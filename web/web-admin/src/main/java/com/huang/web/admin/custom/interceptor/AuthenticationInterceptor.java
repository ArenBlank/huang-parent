package com.huang.web.admin.custom.interceptor;

import com.huang.common.exception.HuangException;
import com.huang.common.login.LoginUser;
import com.huang.common.login.LoginUserHolder;
import com.huang.common.result.ResultCodeEnum;
import com.huang.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthenticationInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("Authorization");
        if (!StringUtils.hasText(token)) {
            token = request.getHeader("access-token");
        }
        if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (!StringUtils.hasText(token)) {
            throw new HuangException(ResultCodeEnum.ADMIN_LOGIN_AUTH);
        }

        Claims claims = JwtUtil.parseToken(token);
        String platform = claims.get("platform", String.class);
        if (!"admin".equalsIgnoreCase(platform)) {
            throw new HuangException(ResultCodeEnum.ADMIN_ACCESS_FORBIDDEN);
        }
        Long userId = claims.get("userId", Long.class);
        String username = claims.get("username", String.class);
        if (userId == null || !StringUtils.hasText(username)) {
            throw new HuangException(ResultCodeEnum.TOKEN_INVALID);
        }
        LoginUserHolder.setLoginUser(new LoginUser(userId, username));

        return true;

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        LoginUserHolder.clear();
    }
}
