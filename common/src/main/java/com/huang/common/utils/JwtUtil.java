package com.huang.common.utils;

import com.huang.common.exception.HuangException;
import com.huang.common.result.ResultCodeEnum;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 * @author system
 * @since 2025-01-24
 */
@Slf4j
public class JwtUtil {

    private static final SecretKey secretKey = Keys.hmacShaKeyFor("CY29Eb04RPNyQPxACH2jBNWFGn0ypMhc".getBytes());
    
    /**
     * Access Token过期时间：7天
     */
    private static final long ACCESS_TOKEN_EXPIRE_TIME = 7 * 24 * 60 * 60 * 1000L;
    
    /**
     * Refresh Token过期时间：30天
     */
    private static final long REFRESH_TOKEN_EXPIRE_TIME = 30 * 24 * 60 * 60 * 1000L;

    /**
     * 创建Token（原有方法，保持向后兼容）
     */
    public static String createToken(Long userId, String username) {
        return Jwts.builder()
                .setSubject("LOGIN_USER")
                .setExpiration(new Date(System.currentTimeMillis() + 3600000 * 24 * 365L))
                .claim("userId", userId)
                .claim("username", username)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 生成App端Access Token
     */
    public static String generateAppAccessToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("type", "access_token");
        claims.put("platform", "app");
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRE_TIME))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 生成App端Refresh Token
     */
    public static String generateAppRefreshToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("type", "refresh_token");
        claims.put("platform", "app");
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRE_TIME))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析Token（原有方法）
     */
    public static Claims parseToken(String token) {
        if (token == null) {
            throw new HuangException(ResultCodeEnum.APP_LOGIN_AUTH);
        }

        try {
            Jws<Claims> claimsJws = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);
            return claimsJws.getBody();
        } catch (ExpiredJwtException e) {
            throw new HuangException(ResultCodeEnum.TOKEN_EXPIRED);
        } catch (JwtException e) {
            throw new HuangException(ResultCodeEnum.TOKEN_INVALID);
        }
    }

    /**
     * 安全解析Token（不抛出异常）
     */
    public static Claims parseTokenSafely(String token) {
        try {
            return parseToken(token);
        } catch (Exception e) {
            log.warn("解析Token失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从Token中获取用户ID
     */
    public static Long getUserIdFromToken(String token) {
        try {
            Claims claims = parseTokenSafely(token);
            if (claims == null) {
                return null;
            }
            Object userId = claims.get("userId");
            if (userId instanceof Integer) {
                return ((Integer) userId).longValue();
            }
            return (Long) userId;
        } catch (Exception e) {
            log.error("从Token中获取用户ID失败", e);
            return null;
        }
    }

    /**
     * 从Token中获取用户名
     */
    public static String getUsernameFromToken(String token) {
        try {
            Claims claims = parseTokenSafely(token);
            return claims != null ? (String) claims.get("username") : null;
        } catch (Exception e) {
            log.error("从Token中获取用户名失败", e);
            return null;
        }
    }

    /**
     * 验证Token是否有效
     */
    public static boolean isTokenValid(String token) {
        try {
            Claims claims = parseToken(token);
            return claims != null && !isTokenExpired(claims);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断Token是否过期
     */
    public static boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }

    /**
     * 刷新Access Token
     */
    public static String refreshAccessToken(String refreshToken) {
        try {
            Claims claims = parseTokenSafely(refreshToken);
            if (claims == null || isTokenExpired(claims)) {
                return null;
            }
            
            // 验证是否为refresh token
            String type = (String) claims.get("type");
            if (!"refresh_token".equals(type)) {
                return null;
            }
            
            Long userId = getUserIdFromToken(refreshToken);
            String username = getUsernameFromToken(refreshToken);
            
            if (userId == null || username == null) {
                return null;
            }
            
            return generateAppAccessToken(userId, username);
        } catch (Exception e) {
            log.error("刷新Token失败", e);
            return null;
        }
    }

    public static void main(String[] args) {
        System.out.println(createToken(1L, "13888888888"));
    }
}
