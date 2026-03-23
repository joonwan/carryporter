package com.e101.carryporter.global.config.web;

import com.e101.carryporter.global.filter.AuthorizationFilter;
import com.e101.carryporter.global.filter.CorsFilter;
import com.e101.carryporter.global.filter.JwtAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.servlet.Filter;

@Configuration
public class WebConfig {

    // CorsFilter 등록
    @Bean
    public FilterRegistrationBean<Filter> corsFilterRegistration(CorsFilter corsFilter) {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>(corsFilter);
        registration.setOrder(1); // 가장 먼저 실행
        return registration;
    }

    // JwtAuthenticationFilter 등록
    @Bean
    public FilterRegistrationBean<Filter> jwtFilterRegistration(JwtAuthenticationFilter jwtFilter) {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>(jwtFilter);
        registration.setOrder(2); // CORS 다음에 실행
        return registration;
    }

    @Bean
    public FilterRegistrationBean<Filter> authorizationFilterRegistration(AuthorizationFilter authorizationFilter) {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>(authorizationFilter);
        registration.setOrder(3); // 인증 다음에 실행
        return registration;
    }
}
