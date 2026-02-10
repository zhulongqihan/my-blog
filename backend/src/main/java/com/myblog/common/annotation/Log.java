package com.myblog.common.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * 设计目标：通过注解标记需要记录日志的方法，AOP自动记录
 * 
 * 使用示例：
 * @Log(module = "文章管理", operationType = "CREATE", description = "创建文章")
 * public Result<Article> createArticle(@RequestBody ArticleRequest request) {
 *     // ...
 * }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {
    
    /**
     * 操作模块
     * 示例：文章管理、用户管理、评论管理
     */
    String module() default "";
    
    /**
     * 操作类型
     * 示例：CREATE、UPDATE、DELETE、QUERY
     */
    String operationType() default "";
    
    /**
     * 操作描述
     * 示例：创建文章、删除评论、修改用户信息
     */
    String description() default "";
    
    /**
     * 是否保存请求参数
     * 默认true，某些敏感接口可设置为false
     */
    boolean saveRequestParams() default true;
    
    /**
     * 是否保存响应结果
     * 默认false，避免日志过大
     */
    boolean saveResponseResult() default false;
}
