package com.myblog.controller;

import com.myblog.dto.ApiResponse;
import com.myblog.dto.ArticleResponse;
import com.myblog.dto.ScrollResult;
import com.myblog.entity.Tag;
import com.myblog.entity.User;
import com.myblog.service.FeedService;
import com.myblog.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Feed 流 + 标签关注控制器
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;
    private final FollowService followService;

    // ========== 标签关注 ==========

    /**
     * 关注标签
     */
    @PostMapping("/tags/{tagId}/follow")
    public ResponseEntity<ApiResponse<Void>> followTag(
            @PathVariable Long tagId,
            @AuthenticationPrincipal User user) {
        followService.followTag(user.getId(), tagId);
        return ResponseEntity.ok(ApiResponse.success("关注成功", null));
    }

    /**
     * 取消关注
     */
    @DeleteMapping("/tags/{tagId}/follow")
    public ResponseEntity<ApiResponse<Void>> unfollowTag(
            @PathVariable Long tagId,
            @AuthenticationPrincipal User user) {
        followService.unfollowTag(user.getId(), tagId);
        return ResponseEntity.ok(ApiResponse.success("已取消关注", null));
    }

    /**
     * 查询某标签关注状态
     */
    @GetMapping("/tags/{tagId}/follow/status")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> getFollowStatus(
            @PathVariable Long tagId,
            @AuthenticationPrincipal User user) {
        boolean followed = user != null && followService.isFollowed(user.getId(), tagId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("followed", followed)));
    }

    /**
     * 我关注的标签列表
     */
    @GetMapping("/tags/followed")
    public ResponseEntity<ApiResponse<List<Tag>>> getFollowedTags(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(followService.getFollowedTags(user.getId())));
    }

    // ========== Feed 流 ==========

    /**
     * Feed 流（滚动分页）
     *
     * @param lastTimestamp 上一批的最小时间戳，首次传当前时间戳
     * @param offset        上一批中与 lastTimestamp 相同 score 的数量，首次传 0
     * @param count         每页数量，默认 10
     */
    @GetMapping("/feed")
    public ResponseEntity<ApiResponse<ScrollResult<ArticleResponse>>> getFeed(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") Long lastTimestamp,
            @RequestParam(defaultValue = "0") Integer offset,
            @RequestParam(defaultValue = "10") Integer count) {
        if (lastTimestamp == 0) lastTimestamp = System.currentTimeMillis();
        return ResponseEntity.ok(ApiResponse.success(
                feedService.queryFeed(user.getId(), lastTimestamp, offset, count)
        ));
    }
}
