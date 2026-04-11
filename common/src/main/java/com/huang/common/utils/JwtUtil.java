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
 * @since 2026-02-25
 */
@Slf4j
public class JwtUtil {

    private static final SecretKey secretKey = Keys.hmacShaKeyFor("CY29Eb04RPNyQPxACH2jBNWFGn0ypMhc".getBytes());

    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_USERNAME = "username";
    public static final String CLAIM_TYPE = "type";
    public static final String CLAIM_PLATFORM = "platform";
    public static final String CLAIM_TOKEN_VERSION = "tokenVersion";
    public static final String TOKEN_TYPE_ACCESS = "access_token";
    public static final String TOKEN_TYPE_REFRESH = "refresh_token";
    public static final String PLATFORM_APP = "app";
    public static final String PLATFORM_ADMIN = "admin";

    private static final long APP_ACCESS_TOKEN_EXPIRE_TIME = 2 * 60 * 60 * 1000L;
    private static final long APP_REFRESH_TOKEN_EXPIRE_TIME = 15L * 24 * 60 * 60 * 1000L;
    private static final long ADMIN_ACCESS_TOKEN_EXPIRE_TIME = 30 * 60 * 1000L;
    private static final long ADMIN_REFRESH_TOKEN_EXPIRE_TIME = 7L * 24 * 60 * 60 * 1000L;

    /**
     * 创建Token（原有方法，保持向后兼容）
     */
    public static String createToken(Long userId, String username) {
        return generateAppAccessToken(userId, username, 0);
    }

    /**
     * 生成App端Access Token
     */
    public static String generateAppAccessToken(Long userId, String username) {
        return generateAppAccessToken(userId, username, 0);
    }

    public static String generateAppAccessToken(Long userId, String username, Integer tokenVersion) {
        return generateToken(userId, username, TOKEN_TYPE_ACCESS, PLATFORM_APP, tokenVersion, APP_ACCESS_TOKEN_EXPIRE_TIME);
    }

    /**
     * 生成App端Refresh Token
     */
    public static String generateAppRefreshToken(Long userId, String username) {
        return generateAppRefreshToken(userId, username, 0);
    }

    public static String generateAppRefreshToken(Long userId, String username, Integer tokenVersion) {
        return generateToken(userId, username, TOKEN_TYPE_REFRESH, PLATFORM_APP, tokenVersion, APP_REFRESH_TOKEN_EXPIRE_TIME);
    }

    public static String generateAdminAccessToken(Long userId, String username) {
        return generateAdminAccessToken(userId, username, 0);
    }

    public static String generateAdminAccessToken(Long userId, String username, Integer tokenVersion) {
        return generateToken(userId, username, TOKEN_TYPE_ACCESS, PLATFORM_ADMIN, tokenVersion, ADMIN_ACCESS_TOKEN_EXPIRE_TIME);
    }

    public static String generateAdminRefreshToken(Long userId, String username) {
        return generateAdminRefreshToken(userId, username, 0);
    }

    public static String generateAdminRefreshToken(Long userId, String username, Integer tokenVersion) {
        return generateToken(userId, username, TOKEN_TYPE_REFRESH, PLATFORM_ADMIN, tokenVersion, ADMIN_REFRESH_TOKEN_EXPIRE_TIME);
    }

    public static String generateAccessToken(Long userId, String username, String platform, Integer tokenVersion) {
        return generateToken(
                userId,
                username,
                TOKEN_TYPE_ACCESS,
                platform,
                tokenVersion,
                PLATFORM_ADMIN.equalsIgnoreCase(platform) ? ADMIN_ACCESS_TOKEN_EXPIRE_TIME : APP_ACCESS_TOKEN_EXPIRE_TIME
        );
    }

    public static String generateRefreshToken(Long userId, String username, String platform, Integer tokenVersion) {
        return generateToken(
                userId,
                username,
                TOKEN_TYPE_REFRESH,
                platform,
                tokenVersion,
                PLATFORM_ADMIN.equalsIgnoreCase(platform) ? ADMIN_REFRESH_TOKEN_EXPIRE_TIME : APP_REFRESH_TOKEN_EXPIRE_TIME
        );
    }

    private static String generateToken(Long userId,
                                        String username,
                                        String type,
                                        String platform,
                                        Integer tokenVersion,
                                        long expireMs) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, userId);
        claims.put(CLAIM_USERNAME, username);
        claims.put(CLAIM_TYPE, type);
        claims.put(CLAIM_PLATFORM, platform);
        claims.put(CLAIM_TOKEN_VERSION, normalizeTokenVersion(tokenVersion));

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expireMs))
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
            Object userId = claims.get(CLAIM_USER_ID);
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
            return claims != null ? (String) claims.get(CLAIM_USERNAME) : null;
        } catch (Exception e) {
            log.error("从Token中获取用户名失败", e);
            return null;
        }
    }

    public static String getPlatformFromClaims(Claims claims) {
        return claims == null ? null : claims.get(CLAIM_PLATFORM, String.class);
    }

    public static String getTokenTypeFromClaims(Claims claims) {
        return claims == null ? null : claims.get(CLAIM_TYPE, String.class);
    }

    public static Integer getTokenVersionFromClaims(Claims claims) {
        if (claims == null) {
            return 0;
        }
        Object tokenVersion = claims.get(CLAIM_TOKEN_VERSION);
        if (tokenVersion instanceof Integer integer) {
            return integer;
        }
        if (tokenVersion instanceof Long longValue) {
            return longValue.intValue();
        }
        return 0;
    }

    public static boolean isRefreshToken(Claims claims) {
        return TOKEN_TYPE_REFRESH.equalsIgnoreCase(getTokenTypeFromClaims(claims));
    }

    public static boolean isAccessToken(Claims claims) {
        return TOKEN_TYPE_ACCESS.equalsIgnoreCase(getTokenTypeFromClaims(claims));
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

            if (!isRefreshToken(claims)) {
                return null;
            }

            Long userId = getUserIdFromToken(refreshToken);
            String username = getUsernameFromToken(refreshToken);
            if (userId == null || username == null) {
                return null;
            }

            return generateAccessToken(
                    userId,
                    username,
                    getPlatformFromClaims(claims),
                    getTokenVersionFromClaims(claims)
            );
        } catch (Exception e) {
            log.error("刷新Token失败", e);
            return null;
        }
    }

    public static long accessTokenExpireMs(String platform) {
        return PLATFORM_ADMIN.equalsIgnoreCase(platform) ? ADMIN_ACCESS_TOKEN_EXPIRE_TIME : APP_ACCESS_TOKEN_EXPIRE_TIME;
    }

    public static long refreshTokenExpireMs(String platform) {
        return PLATFORM_ADMIN.equalsIgnoreCase(platform) ? ADMIN_REFRESH_TOKEN_EXPIRE_TIME : APP_REFRESH_TOKEN_EXPIRE_TIME;
    }

    private static int normalizeTokenVersion(Integer tokenVersion) {
        return tokenVersion == null || tokenVersion < 0 ? 0 : tokenVersion;
    }

    public static void main(String[] args) {
        System.out.println(createToken(1L, "13888888888"));
    }
}
