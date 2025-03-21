package com.privatedomain.chat.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * MyBatis Plus Configuration
 * This class configures MyBatis Plus for our application
 * It enables the mapper scanning and pagination features
 */
@Configuration
@EnableTransactionManagement
@MapperScan("com.privatedomain.chat.mapper")
public class MyBatisPlusConfig {
    
    /**
     * Configure MyBatis Plus interceptors
     * Currently adding the pagination interceptor to support paging queries
     *
     * @return the configured interceptor
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // Add pagination support
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));
        return interceptor;
    }
}