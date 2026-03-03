package com.myblog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 点赞响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikeResponseDTO {
    /** 当前用户是否已点赞 */
    private Boolean liked;
    /** 当前总点赞数 */
    private Integer likeCount;
}
