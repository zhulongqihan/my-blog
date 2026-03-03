package com.myblog.task;

import com.myblog.common.constant.RedisKeyPrefix;
import com.myblog.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 热门榜定时刷新任务
 *
 * 每小时扫描所有文章的 HyperLogLog UV 数，
 * 写入 ZSet 排行榜（article:hot:weekly），
 * 供 GET /api/articles/hot/weekly 实时查询。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HotRankTask {

    private final StringRedisTemplate stringRedisTemplate;
    private final ArticleRepository articleRepository;

    /**
     * 每小时整点刷新本周热门榜
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void refreshWeeklyHotRank() {
        List<Long> articleIds = articleRepository.findAllPublishedArticleIds();
        if (articleIds.isEmpty()) return;

        String hotKey = RedisKeyPrefix.ARTICLE_HOT_WEEKLY;

        // 清空旧排行
        stringRedisTemplate.delete(hotKey);

        int updated = 0;
        for (Long articleId : articleIds) {
            Long uvCount = stringRedisTemplate.opsForHyperLogLog().size(
                    RedisKeyPrefix.ARTICLE_UV + articleId
            );
            if (uvCount != null && uvCount > 0) {
                stringRedisTemplate.opsForZSet().add(
                        hotKey,
                        articleId.toString(),
                        uvCount.doubleValue()
                );
                updated++;
            }
        }

        log.info("[HotRank] 热门榜刷新完成: {} 篇文章参与排名", updated);
    }
}
