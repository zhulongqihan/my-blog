package com.myblog.service;

import cn.hutool.core.util.BooleanUtil;
import com.myblog.common.constant.RedisKeyPrefix;
import com.myblog.common.exception.BusinessException;
import com.myblog.dto.CheckInCalendarDTO;
import com.myblog.dto.CheckInStatsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 读者签到服务 — BitMap 实现
 *
 * 对标黑马点评签到系统：
 * - SETBIT：签到打卡
 * - BITCOUNT：本月签到天数
 * - BITFIELD GET：连续签到天数（逐位右移计数）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckInService {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 执行签到
     */
    public CheckInStatsDTO checkIn(Long userId) {
        LocalDate today = LocalDate.now();
        String key = getSignKey(userId, YearMonth.from(today));
        int dayOfMonth = today.getDayOfMonth();

        // ① 检查今日是否已签到
        Boolean alreadySigned = stringRedisTemplate.opsForValue().getBit(key, dayOfMonth);
        if (BooleanUtil.isTrue(alreadySigned)) {
            throw new BusinessException("今日已签到");
        }

        // ② 执行签到：SETBIT key dayOfMonth 1
        stringRedisTemplate.opsForValue().setBit(key, dayOfMonth, true);

        // ③ 设置 Key 过期时间 90 天
        stringRedisTemplate.expire(key, 90, TimeUnit.DAYS);

        // ④ 统计本月签到天数
        int signCount = countSignDays(key);

        // ⑤ 计算连续签到天数
        int consecutiveDays = countConsecutiveDays(key, dayOfMonth);

        // ⑥ 检查是否触发成就
        String achievement = checkAchievement(consecutiveDays);

        return CheckInStatsDTO.builder()
                .signCount(signCount)
                .consecutiveDays(consecutiveDays)
                .signedToday(true)
                .achievement(achievement)
                .build();
    }

    /**
     * 获取签到统计
     */
    public CheckInStatsDTO getStats(Long userId, YearMonth month) {
        String key = getSignKey(userId, month);
        int signCount = countSignDays(key);

        int consecutiveDays = 0;
        boolean signedToday = false;
        if (month.equals(YearMonth.now())) {
            int dayOfMonth = LocalDate.now().getDayOfMonth();
            consecutiveDays = countConsecutiveDays(key, dayOfMonth);
            Boolean bit = stringRedisTemplate.opsForValue().getBit(key, dayOfMonth);
            signedToday = BooleanUtil.isTrue(bit);
        }

        return CheckInStatsDTO.builder()
                .signCount(signCount)
                .consecutiveDays(consecutiveDays)
                .signedToday(signedToday)
                .build();
    }

    /**
     * 获取某月的签到日历
     */
    public CheckInCalendarDTO getCalendar(Long userId, YearMonth month) {
        String key = getSignKey(userId, month);
        int totalDays = month.lengthOfMonth();

        // 取整月位图
        List<Long> results = stringRedisTemplate.opsForValue().bitField(
                key,
                BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(totalDays))
                        .valueAt(1)
        );

        long num = (results != null && !results.isEmpty()) ? results.get(0) : 0;

        // 解析每一天
        List<Integer> signedDays = new ArrayList<>();
        for (int i = totalDays; i >= 1; i--) {
            if ((num & 1) == 1) {
                signedDays.add(i);
            }
            num >>>= 1;
        }
        Collections.reverse(signedDays);

        int consecutiveDays = 0;
        if (month.equals(YearMonth.now())) {
            consecutiveDays = countConsecutiveDays(key, LocalDate.now().getDayOfMonth());
        }

        return CheckInCalendarDTO.builder()
                .month(month.toString())
                .totalDays(totalDays)
                .signedDays(signedDays)
                .signCount(signedDays.size())
                .consecutiveDays(consecutiveDays)
                .build();
    }

    // ========== 内部方法 ==========

    private String getSignKey(Long userId, YearMonth month) {
        return RedisKeyPrefix.SIGN + userId + ":" + month.format(DateTimeFormatter.ofPattern("yyyyMM"));
    }

    /**
     * BITCOUNT 统计本月签到天数
     */
    private int countSignDays(String key) {
        Long count = stringRedisTemplate.execute(
                (RedisCallback<Long>) connection ->
                        connection.stringCommands().bitCount(key.getBytes())
        );
        return count != null ? count.intValue() : 0;
    }

    /**
     * 连续签到天数算法 — BITFIELD GET + 逐位右移
     *
     * 核心思路：
     * 1. BITFIELD key GET u{dayOfMonth} 1 — 获取从第1天到今天的无符号整数
     * 2. 逐位右移，统计末尾连续 1 的个数
     */
    private int countConsecutiveDays(String key, int dayOfMonth) {
        List<Long> results = stringRedisTemplate.opsForValue().bitField(
                key,
                BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth))
                        .valueAt(1)
        );

        if (results == null || results.isEmpty()) {
            return 0;
        }

        long num = results.get(0);
        if (num == 0) return 0;

        int count = 0;
        while ((num & 1) == 1) {
            count++;
            num >>>= 1;
        }
        return count;
    }

    /**
     * 检查签到成就
     */
    private String checkAchievement(int consecutiveDays) {
        if (consecutiveDays == 7) {
            return "LOYAL_READER";
        } else if (consecutiveDays == 30) {
            return "HARDCORE_FAN";
        }
        return null;
    }
}
