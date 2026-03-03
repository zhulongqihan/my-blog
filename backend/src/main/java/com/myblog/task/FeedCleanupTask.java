package com.myblog.task;

import com.myblog.common.constant.RedisKeyPrefix;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Feed 清理定时任务
 *
 * 每天凌晨 3 点裁剪 Feed，每个用户只保留最近 500 条
 * 防止 ZSet 无限膨胀占用 Redis 内存
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeedCleanupTask {

    private final StringRedisTemplate stringRedisTemplate;

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupFeeds() {
        Set<String> feedKeys = stringRedisTemplate.keys(RedisKeyPrefix.FEED + "*");
        if (feedKeys == null || feedKeys.isEmpty()) return;

        int cleaned = 0;
        for (String key : feedKeys) {
            Long size = stringRedisTemplate.opsForZSet().size(key);
            if (size != null && size > 500) {
                stringRedisTemplate.opsForZSet().removeRange(key, 0, size - 501);
                cleaned++;
            }
        }

        if (cleaned > 0) {
            log.info("[FeedCleanup] Feed 清理完成: {} 个用户", cleaned);
        }
    }
}
