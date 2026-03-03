package com.myblog.service;

import com.myblog.common.constant.RedisKeyPrefix;
import com.myblog.common.exception.BusinessException;
import com.myblog.entity.Tag;
import com.myblog.entity.UserTagFollow;
import com.myblog.repository.TagRepository;
import com.myblog.repository.UserTagFollowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 标签关注服务 — 双写 Redis + DB
 *
 * 关注关系持久化到 MySQL（user_tag_follow 表），
 * 同时写入 Redis SET（双向索引），供 FeedService 实时查询。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FollowService {

    private final StringRedisTemplate stringRedisTemplate;
    private final UserTagFollowRepository followRepository;
    private final TagRepository tagRepository;

    /**
     * 关注标签
     */
    @Transactional
    public void followTag(Long userId, Long tagId) {
        // 验证标签存在
        tagRepository.findById(tagId)
                .orElseThrow(() -> new BusinessException("标签不存在"));

        boolean exists = followRepository.existsByUserIdAndTagId(userId, tagId);
        if (exists) {
            throw new BusinessException("已关注该标签");
        }

        // 写入 DB
        UserTagFollow follow = UserTagFollow.builder()
                .userId(userId)
                .tagId(tagId)
                .build();
        followRepository.save(follow);

        // 写入 Redis（双向索引）
        stringRedisTemplate.opsForSet().add(
                RedisKeyPrefix.FOLLOW_TAGS + userId, tagId.toString()
        );
        stringRedisTemplate.opsForSet().add(
                RedisKeyPrefix.TAG_FOLLOWERS + tagId, userId.toString()
        );

        log.info("[Follow] 用户 {} 关注标签 {}", userId, tagId);
    }

    /**
     * 取消关注
     */
    @Transactional
    public void unfollowTag(Long userId, Long tagId) {
        followRepository.deleteByUserIdAndTagId(userId, tagId);

        stringRedisTemplate.opsForSet().remove(
                RedisKeyPrefix.FOLLOW_TAGS + userId, tagId.toString()
        );
        stringRedisTemplate.opsForSet().remove(
                RedisKeyPrefix.TAG_FOLLOWERS + tagId, userId.toString()
        );

        log.info("[Follow] 用户 {} 取关标签 {}", userId, tagId);
    }

    /**
     * 查询是否已关注
     */
    public boolean isFollowed(Long userId, Long tagId) {
        return Boolean.TRUE.equals(
                stringRedisTemplate.opsForSet().isMember(
                        RedisKeyPrefix.FOLLOW_TAGS + userId, tagId.toString()
                )
        );
    }

    /**
     * 获取用户关注的标签 ID 集合
     */
    public Set<Long> getFollowedTagIds(Long userId) {
        Set<String> members = stringRedisTemplate.opsForSet().members(
                RedisKeyPrefix.FOLLOW_TAGS + userId
        );
        if (members == null || members.isEmpty()) return Collections.emptySet();
        return members.stream().map(Long::parseLong).collect(Collectors.toSet());
    }

    /**
     * 获取用户关注的标签实体列表
     */
    public List<Tag> getFollowedTags(Long userId) {
        Set<Long> tagIds = getFollowedTagIds(userId);
        if (tagIds.isEmpty()) return Collections.emptyList();
        return tagRepository.findAllById(tagIds);
    }
}
