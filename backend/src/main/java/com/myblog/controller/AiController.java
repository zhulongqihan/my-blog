package com.myblog.controller;

import com.myblog.common.annotation.RateLimit;
import com.myblog.dto.AiChatRequest;
import com.myblog.service.AiGatewayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiGatewayService aiGatewayService;

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RateLimit(maxRequests = 20, timeWindow = 60, limitType = RateLimit.LimitType.IP_AND_API, prefix = "ai-chat")
    public SseEmitter chat(@Valid @RequestBody AiChatRequest request) {
        return aiGatewayService.stream(request);
    }
}
