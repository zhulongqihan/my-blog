package com.myblog.service;

import com.myblog.common.constant.RedisKeyPrefix;
import com.myblog.dto.ArticleResponse;
import com.myblog.dto.ScrollResult;
import com.myblog.entity.Article;
import com.myblog.entity.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Feed 流服务 — ZSet 推模型 + 滚动分页
 *
 * 对标黑马点评 Feed 流：
 * - 发布文章时推送到关注者的 ZSet 收件箱（Push 模型）
 * - 查询时用 ZREVRANGEBYSCORE + lastTimestamp + offset 做滚动分页
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ArticleService articleService;

    /**
     * 文章发布后推送到关注者的 Feed 收件箱
     */
    public void pushToFollowers(Article article) {
        List<Tag> tags = article.getTags();
        if (tags == null || tags.isEmpty()) return;

        // 收集所有关注了这些标签的用户（去重）
        Set<String> allFollowers = new HashSet<>();
        for (Tag tag : tags) {
            Set<String> followers = stringRedisTemplate.opsForSet().members(
                    RedisKeyPrefix.TAG_FOLLOWERS + tag.getId()
            );
            if (followers != null) {
                allFollowers.addAll(followers);
            }
        }

        if (allFollowers.isEmpty()) return;

        // 批量 ZADD 到每个用户的 Feed ZSet
        long timestamp = article.getPublishedAt() != null
                ? article.getPublishedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                : System.currentTimeMillis();

        String articleId = article.getId().toString();

        for (String userId : allFollowers) {
            stringRedisTemplate.opsForZSet().add(
                    RedisKeyPrefix.FEED + userId,
                    articleId,
                    timestamp
            );
        }

        log.info("[Feed] 推送完成: 文章[{}] → {} 位用户", article.getId(), allFollowers.size());
    }

    /**
     * 滚动分页查询用户 Feed
     *
     * 算法：ZREVRANGEBYSCORE key lastTimestamp 0 LIMIT offset count
     * - 首次请求：lastTimestamp = 当前时间戳, offset = 0
     * - 翻页请求：上一批的 minTime 和 sameScoreCount
     */
    public ScrollResult<ArticleResponse> queryFeed(Long userId, Long lastTimestamp, Integer offset, Integer count) {
        String key = RedisKeyPrefix.FEED + userId;

        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(
                        key,
                        0,              // min
                        lastTimestamp,   // max
                        offset,          // offset
                        count            // count
                );

        if (typedTuples == null || typedTuples.isEmpty()) {
            return new ScrollResult<>(Collections.emptyList(), null, null);
        }

        // 解析结果
        List<Long> articleIds = new ArrayList<>(typedTuples.size());
        long minTime = Long.MAX_VALUE;
        int sameScoreCount = 1;

        for (ZSetOperations.TypedTuple<String> tuple : typedTuples) {
            articleIds.add(Long.parseLong(Objects.requireNonNull(tuple.getValue())));
            long score = Objects.requireNonNull(tuple.getScore()).longValue();

            if (score == minTime) {
                sameScoreCount++;
            } else if (score < minTime) {
                minTime = score;
                sameScoreCount = 1;
            }
        }

        // 查询文章详情（保持顺序）
        List<ArticleResponse> articles = articleIds.stream()
                .map(id -> {
                    try {
                        return articleService.getArticle(id);
                    } catch (Exception e) {
                        return null; // 文章已删除，跳过
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return new ScrollResult<>(articles, minTime, sameScoreCount);
    }
}
