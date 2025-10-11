package com.huang.web.admin.custom.interceptor;

import com.huang.common.login.LoginUser;
import com.huang.common.login.LoginUserHolder;
import com.huang.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthenticationInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        System.out.println("=== 认证拦截器被调用: " + request.getRequestURI() + " ===");
        
        String token = request.getHeader("access-token");
        System.out.println("Token: " + token);
        
        // 如果没有token，使用测试模式
        if (token == null || token.trim().isEmpty()) {
            // 测试模式：使用admin用户（用户ID=1）
            LoginUser testUser = new LoginUser(1L, "admin");
            LoginUserHolder.setLoginUser(testUser);
            System.out.println("设置测试用户: " + testUser.getUserId() + ", " + testUser.getUsername());
            return true;
        }
        
        try {
            Claims claims = JwtUtil.parseToken(token);
            Long userId = claims.get("userId", Long.class);
            String username = claims.get("username", String.class);
            LoginUserHolder.setLoginUser(new LoginUser(userId, username));
        } catch (Exception e) {
            // Token解析失败时，也使用测试模式
            LoginUserHolder.setLoginUser(new LoginUser(1L, "admin"));
        }
        
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        LoginUserHolder.clear();
    }
}
