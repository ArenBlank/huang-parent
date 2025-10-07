package com.huang.web.app.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huang.common.result.Result;
import com.huang.common.result.ResultCodeEnum;
import com.huang.common.service.TokenBlacklistService;
import com.huang.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * JWT认证过滤器
 * 拦截所有请求，检查JWT token是否有效且未被列入黑名单
 * 
 * @author system
 * @since 2025-01-24
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 无需认证的接口路径（白名单）
     */
    private static final List<String> WHITELIST_PATHS = Arrays.asList(
            "/app/auth/login",
            "/app/auth/register", 
            "/app/auth/sms-code/send",
            "/app/auth/forget-password",
            "/app/auth/refresh-token",
            "/app/simple/login",
            "/app/role/list",           // 角色查询不需要认证
            "/app/role/available",      // 可用角色查询不需要认证
            "/doc.html",                // Swagger文档
            "/swagger-resources",       // Swagger资源
            "/v3/api-docs",            // OpenAPI文档
            "/webjars",                // 静态资源
            "/favicon.ico"             // 图标
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        
        // 跳过白名单路径
        if (isWhitelistPath(requestURI)) {
            log.debug("白名单路径，跳过JWT验证: {} {}", method, requestURI);
            filterChain.doFilter(request, response);
            return;
        }
        
        // 跳过OPTIONS请求（CORS预检请求）
        if ("OPTIONS".equalsIgnoreCase(method)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // 获取Authorization头
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("接口需要认证但未提供有效token: {} {}", method, requestURI);
            writeUnauthorizedResponse(response, "未提供有效的访问令牌");
            return;
        }
        
        String token = authHeader.substring(7);
        
        // 验证token格式和有效性
        Claims claims = JwtUtil.parseTokenSafely(token);
        if (claims == null) {
            log.warn("token解析失败: {} {}", method, requestURI);
            writeUnauthorizedResponse(response, "访问令牌无效或已过期");
            return;
        }
        
        // 对于退出登录接口，跳过黑名单检查（允许已在黑名单的token执行退出操作）
        if ("/app/auth/logout".equals(requestURI)) {
            log.debug("退出登录接口，跳过黑名单检查: {} {}", method, requestURI);
        } else {
            // 检查token是否在黑名单中
            if (tokenBlacklistService.isBlacklisted(token)) {
                log.warn("token已在黑名单中: {} {}", method, requestURI);
                writeUnauthorizedResponse(response, "令牌已失效，请重新登录");
                return;
            }
        }
        
        // token有效，继续处理请求
        log.debug("JWT验证通过: {} {}", method, requestURI);
        
        // 可以在这里设置用户信息到SecurityContext或者Request Attribute
        Long userId = JwtUtil.getUserIdFromToken(token);
        String username = JwtUtil.getUsernameFromToken(token);
        if (userId != null && username != null) {
            request.setAttribute("currentUserId", userId);
            request.setAttribute("currentUsername", username);
        }
        
        filterChain.doFilter(request, response);
    }

    /**
     * 检查路径是否在白名单中
     */
    private boolean isWhitelistPath(String requestURI) {
        return WHITELIST_PATHS.stream().anyMatch(requestURI::startsWith);
    }

    /**
     * 写入401未授权响应
     */
    private void writeUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        
        Result<Void> result = Result.fail(ResultCodeEnum.APP_LOGIN_AUTH.getCode(), message);
        String jsonResponse = objectMapper.writeValueAsString(result);
        
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }
}