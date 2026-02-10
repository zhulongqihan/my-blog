package com.myblog.common.constant;

import lombok.Getter;

/**
 * 错误码枚举
 * 设计规范：
 * - 2xx: 成功
 * - 4xx: 客户端错误
 * - 5xx: 服务端错误
 * 
 * 业务错误码规则：
 * - 1000-1999: 用户相关
 * - 2000-2999: 文章相关
 * - 3000-3999: 评论相关
 * - 4000-4999: 分类标签相关
 */
@Getter
public enum ErrorCode {
    
    // ========== 通用错误 ==========
    SUCCESS(200, "操作成功"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未授权，请先登录"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    INTERNAL_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务暂时不可用"),
    
    // ========== 用户相关 1000-1999 ==========
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_ALREADY_EXISTS(1002, "用户已存在"),
    USER_DISABLED(1003, "用户已被禁用"),
    USERNAME_OR_PASSWORD_ERROR(1004, "用户名或密码错误"),
    PASSWORD_NOT_MATCH(1005, "两次密码不一致"),
    OLD_PASSWORD_ERROR(1006, "原密码错误"),
    USER_NOT_LOGIN(1007, "用户未登录"),
    TOKEN_EXPIRED(1008, "Token已过期"),
    TOKEN_INVALID(1009, "Token无效"),
    TOKEN_BLACKLISTED(1010, "Token已失效，请重新登录"),
    
    // ========== 文章相关 2000-2999 ==========
    ARTICLE_NOT_FOUND(2001, "文章不存在"),
    ARTICLE_TITLE_EMPTY(2002, "文章标题不能为空"),
    ARTICLE_CONTENT_EMPTY(2003, "文章内容不能为空"),
    ARTICLE_ALREADY_PUBLISHED(2004, "文章已发布"),
    ARTICLE_NOT_PUBLISHED(2005, "文章未发布"),
    ARTICLE_DELETE_FAILED(2006, "文章删除失败"),
    
    // ========== 评论相关 3000-3999 ==========
    COMMENT_NOT_FOUND(3001, "评论不存在"),
    COMMENT_CONTENT_EMPTY(3002, "评论内容不能为空"),
    COMMENT_AUDIT_FAILED(3003, "评论审核失败"),
    COMMENT_DELETE_FAILED(3004, "评论删除失败"),
    
    // ========== 分类标签相关 4000-4999 ==========
    CATEGORY_NOT_FOUND(4001, "分类不存在"),
    CATEGORY_ALREADY_EXISTS(4002, "分类已存在"),
    CATEGORY_HAS_ARTICLES(4003, "分类下有文章，无法删除"),
    TAG_NOT_FOUND(4011, "标签不存在"),
    TAG_ALREADY_EXISTS(4012, "标签已存在"),
    TAG_HAS_ARTICLES(4013, "标签下有文章，无法删除"),
    
    // ========== 权限相关 5000-5999 ==========
    ROLE_NOT_FOUND(5001, "角色不存在"),
    MENU_NOT_FOUND(5002, "菜单不存在"),
    PERMISSION_DENIED(5003, "权限不足"),
    
    // ========== 文件相关 6000-6999 ==========
    FILE_UPLOAD_FAILED(6001, "文件上传失败"),
    FILE_SIZE_EXCEEDED(6002, "文件大小超过限制"),
    FILE_TYPE_NOT_ALLOWED(6003, "文件类型不支持"),
    
    // ========== 限流相关 7000-7999 ==========
    RATE_LIMIT_EXCEEDED(7001, "请求过于频繁，请稍后再试");
    
    private final Integer code;
    private final String message;
    
    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
