package com.huang.web.app.custom.interceptor;

import com.huang.common.exception.HuangException;
import com.huang.common.login.LoginUser;
import com.huang.common.login.LoginUserHolder;
import com.huang.common.result.ResultCodeEnum;
import com.huang.common.utils.JwtUtil;
import com.huang.web.app.service.biz.auth.AppAuthCacheService;
import com.huang.web.app.service.biz.auth.AppAuthSnapshot;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Objects;

@Slf4j
@Component
public class AuthenticationInterceptor implements HandlerInterceptor {

    private final AppAuthCacheService appAuthCacheService;

    public AuthenticationInterceptor(AppAuthCacheService appAuthCacheService) {
        this.appAuthCacheService = appAuthCacheService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String requestURI = request.getRequestURI();
        log.info("App JWT authentication intercepting request: {}", requestURI);

        String token = request.getHeader("Authorization");
        if (!StringUtils.hasText(token)) {
            token = request.getHeader("access-token");
        }
        if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (!StringUtils.hasText(token)) {
            log.warn("Request missing JWT token: {}", requestURI);
            throw new HuangException(ResultCodeEnum.APP_LOGIN_AUTH);
        }

        try {
            Claims claims = JwtUtil.parseToken(token);
            String platform = JwtUtil.getPlatformFromClaims(claims);
            if (!JwtUtil.PLATFORM_APP.equalsIgnoreCase(platform)) {
                throw new HuangException(ResultCodeEnum.TOKEN_INVALID);
            }
            Long userId = claims.get(JwtUtil.CLAIM_USER_ID, Long.class);
            String username = claims.get(JwtUtil.CLAIM_USERNAME, String.class);
            if (userId == null || !StringUtils.hasText(username)) {
                log.warn("JWT token missing required user info: {}", requestURI);
                throw new HuangException(ResultCodeEnum.TOKEN_INVALID);
            }

            AppAuthSnapshot snapshot = appAuthCacheService.getOrLoad(userId);
            if (snapshot == null || snapshot.status() == null || snapshot.status() != 1) {
                log.warn("JWT auth failed because user is missing or disabled: userId={}", userId);
                throw new HuangException(ResultCodeEnum.APP_LOGIN_AUTH);
            }
            if (!Objects.equals(normalizeTokenVersion(snapshot.tokenVersion()), JwtUtil.getTokenVersionFromClaims(claims))) {
                log.warn("JWT auth failed because tokenVersion does not match: userId={}", userId);
                throw new HuangException(ResultCodeEnum.TOKEN_INVALID);
            }

            LoginUserHolder.setLoginUser(new LoginUser(userId, username));
            log.debug("JWT auth succeeded, userId={}, username={}", userId, username);
            return true;
        } catch (HuangException e) {
            log.warn("JWT auth failed: {} - {}", requestURI, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("JWT auth error: {} - {}", requestURI, e.getMessage(), e);
            throw new HuangException(ResultCodeEnum.TOKEN_INVALID);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        LoginUserHolder.clear();
    }

    private int normalizeTokenVersion(Integer tokenVersion) {
        return tokenVersion == null || tokenVersion < 0 ? 0 : tokenVersion;
    }
}
