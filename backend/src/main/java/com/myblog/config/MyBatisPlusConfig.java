package com.myblog.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus配置
 * 设计目标：
 * 1. 配置分页插件
 * 2. 配置Mapper扫描路径
 * 3. 配置性能分析插件（开发环境）
 * 
 * 设计说明：
 * - 明确指定sqlSessionFactoryRef避免与JPA冲突
 * - 只扫描com.myblog.mapper包（MyBatis-Plus的Mapper）
 * - com.myblog.repository包由JPA管理
 */
@Configuration
@MapperScan(
    basePackages = "com.myblog.mapper",
    sqlSessionFactoryRef = "sqlSessionFactory"
)
public class MyBatisPlusConfig {
    
    /**
     * 配置MyBatis-Plus拦截器
     * 设计亮点：
     * - 分页插件：自动处理分页SQL，支持多种数据库
     * - 性能优化：使用物理分页，避免内存分页
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        
        // 分页插件
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        // 设置最大单页限制数量，防止恶意查询
        paginationInterceptor.setMaxLimit(1000L);
        // 溢出总页数后是否进行处理（默认不处理）
        paginationInterceptor.setOverflow(false);
        
        interceptor.addInnerInterceptor(paginationInterceptor);
        
        return interceptor;
    }
}
