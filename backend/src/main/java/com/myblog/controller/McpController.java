package com.myblog.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.myblog.common.annotation.RateLimit;
import com.myblog.service.BlogContentToolService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 面向外部智能体的最小 MCP Streamable HTTP JSON-RPC 工具端点。 */
@RestController
@RequestMapping("/api/mcp")
@RequiredArgsConstructor
public class McpController {

    private final ObjectMapper objectMapper;
    private final BlogContentToolService contentToolService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @RateLimit(maxRequests = 60, timeWindow = 60, limitType = RateLimit.LimitType.IP_AND_API, prefix = "mcp")
    public Object handle(@RequestBody JsonNode request) {
        String method = request.path("method").asText();
        JsonNode id = request.get("id");
        return switch (method) {
            case "initialize" -> result(id, initialize());
            case "notifications/initialized" -> null;
            case "tools/list" -> result(id, tools());
            case "tools/call" -> result(id, call(request.path("params")));
            default -> error(id, -32601, "Method not found: " + method);
        };
    }

    private ObjectNode initialize() {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("protocolVersion", "2025-06-18");
        result.putObject("capabilities").putObject("tools");
        result.putObject("serverInfo").put("name", "myblog-content-tools").put("version", "1.0.0");
        return result;
    }

    private ObjectNode tools() {
        ObjectNode result = objectMapper.createObjectNode();
        ArrayNode tools = result.putArray("tools");
        tools.add(tool("search_articles", "搜索公开博客文章并返回可供模型引用的标题与上下文。",
                Map.of("query", schema("string", "搜索关键词", true))));
        tools.add(tool("get_article", "按文章 ID 获取一篇已发布文章的上下文。",
                Map.of("articleId", schema("integer", "文章 ID", true))));
        return result;
    }

    private ObjectNode call(JsonNode params) {
        String name = params.path("name").asText();
        JsonNode arguments = params.path("arguments");
        String text;
        if ("search_articles".equals(name)) {
            String query = arguments.path("query").asText("").trim();
            text = contentToolService.retrieve(query, null, 5).context();
        } else if ("get_article".equals(name)) {
            long articleId = arguments.path("articleId").asLong(-1);
            text = contentToolService.getPublishedArticle(articleId)
                    .map(article -> "标题：" + article.getTitle() + "\n\n" + article.getContent())
                    .orElse("未找到已发布文章");
        } else {
            return error(null, -32602, "Unknown tool: " + name);
        }
        ObjectNode result = objectMapper.createObjectNode();
        ArrayNode content = result.putArray("content");
        content.addObject().put("type", "text").put("text", text);
        result.put("isError", false);
        return result;
    }

    private ObjectNode tool(String name, String description, Map<String, ObjectNode> properties) {
        ObjectNode tool = objectMapper.createObjectNode();
        tool.put("name", name).put("description", description);
        ObjectNode inputSchema = tool.putObject("inputSchema");
        inputSchema.put("type", "object");
        ObjectNode propertyNode = inputSchema.putObject("properties");
        properties.forEach(propertyNode::set);
        inputSchema.putArray("required").addAll(properties.keySet().stream().map(objectMapper.getNodeFactory()::textNode).toList());
        return tool;
    }

    private ObjectNode schema(String type, String description, boolean required) {
        return objectMapper.createObjectNode().put("type", type).put("description", description);
    }

    private ObjectNode result(JsonNode id, JsonNode result) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.set("id", id);
        response.set("result", result);
        return response;
    }

    private ObjectNode error(JsonNode id, int code, String message) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.set("id", id);
        response.set("error", objectMapper.createObjectNode().put("code", code).put("message", message));
        return response;
    }
}
