package com.myblog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CommentRequest {
    @NotBlank(message = "评论内容不能为空")
    private String content;
    
    private Long parentId;
    
    // 游客评论
    private String guestName;
    private String guestEmail;
}
