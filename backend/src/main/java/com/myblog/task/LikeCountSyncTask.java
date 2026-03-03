package com.myblog.task;

import com.myblog.common.constant.RedisKeyPrefix;
import com.myblog.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 点赞数定时同步任务 — Write-Behind 模式
 *
 * 复用 ViewCountSyncTask 的 Write-Behind 模式：
 * 用户点赞写 Redis（实时响应），定时同步到 MySQL（最终一致）
 *
 * 对标黑马点评：秒杀订单异步写入数据库
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LikeCountSyncTask {

    private final StringRedisTemplate stringRedisTemplate;
    private final ArticleRepository articleRepository;

    /**
     * 每 5 分钟同步一次 Redis 点赞数到 MySQL
     * initialDelay = 2 分钟，避免启动时与预热任务冲突
     */
    @Scheduled(fixedRate = 300000, initialDelay = 120000)
    public void syncLikeCounts() {
        Set<String> keys = stringRedisTemplate.keys(RedisKeyPrefix.ARTICLE_LIKE_COUNT + "*");
        if (keys == null || keys.isEmpty()) return;

        int synced = 0;
        for (String key : keys) {
            try {
                String value = stringRedisTemplate.opsForValue().get(key);
                if (value == null) continue;

                Long articleId = Long.parseLong(
                        key.replace(RedisKeyPrefix.ARTICLE_LIKE_COUNT, "")
                );
                int likeCount = Integer.parseInt(value);

                articleRepository.findById(articleId).ifPresent(article -> {
                    article.setLikeCount(likeCount);
                    articleRepository.save(article);
                });
                synced++;
            } catch (Exception e) {
                log.error("同步点赞数失败: key={}", key, e);
            }
        }

        if (synced > 0) {
            log.info("[LikeSync] 点赞数同步完成: {} 篇文章", synced);
        }
    }
}
