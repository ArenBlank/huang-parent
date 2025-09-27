package com.huang.web.app.custom.interceptor;

import com.huang.common.exception.HuangException;
import com.huang.common.login.LoginUser;
import com.huang.common.login.LoginUserHolder;
import com.huang.common.result.ResultCodeEnum;
import com.huang.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * App端JWT认证拦截器
 * @author system
 * @since 2025-01-24
 */
@Slf4j
@Component
public class AuthenticationInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        log.info("App端JWT认证拦截器处理请求: {}", requestURI);
        
        // 获取Authorization header或access-token header
        String token = request.getHeader("Authorization");
        if (!StringUtils.hasText(token)) {
            token = request.getHeader("access-token");
        }
        
        // 处理Bearer前缀
        if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        if (!StringUtils.hasText(token)) {
            log.warn("请求缺少JWT token: {}", requestURI);
            throw new HuangException(ResultCodeEnum.APP_LOGIN_AUTH);
        }
        
        try {
            Claims claims = JwtUtil.parseToken(token);
            Long userId = claims.get("userId", Long.class);
            String username = claims.get("username", String.class);
            
            if (userId == null || !StringUtils.hasText(username)) {
                log.warn("JWT token中缺少必要的用户信息: {}", requestURI);
                throw new HuangException(ResultCodeEnum.TOKEN_INVALID);
            }
            
            LoginUserHolder.setLoginUser(new LoginUser(userId, username));
            log.debug("JWT认证成功，用户ID: {}, 用户名: {}", userId, username);
            
            return true;
            
        } catch (HuangException e) {
            log.warn("JWT认证失败: {} - {}", requestURI, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("JWT认证异常: {} - {}", requestURI, e.getMessage(), e);
            throw new HuangException(ResultCodeEnum.TOKEN_INVALID);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        LoginUserHolder.clear();
    }
}
