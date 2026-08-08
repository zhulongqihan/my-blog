package com.myblog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** AI Gateway 请求。上下文由服务端按 articleId 检索，避免客户端伪造来源。 */
@Data
public class AiChatRequest {

    @NotBlank
    @Size(max = 4000)
    private String query;

    @Size(max = 3000)
    private String systemPrompt;

    private Long articleId;

    @Size(max = 40)
    private String mode;
}
