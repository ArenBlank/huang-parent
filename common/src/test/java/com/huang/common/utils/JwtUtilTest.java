package com.huang.common.utils;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    @Test
    void refreshAccessToken_shouldPreservePlatformAndTokenVersion() {
        String refreshToken = JwtUtil.generateAdminRefreshToken(7L, "ops_admin", 3);

        String newAccessToken = JwtUtil.refreshAccessToken(refreshToken);
        Claims claims = JwtUtil.parseToken(newAccessToken);

        assertThat(newAccessToken).isNotBlank();
        assertThat(JwtUtil.isAccessToken(claims)).isTrue();
        assertThat(JwtUtil.getPlatformFromClaims(claims)).isEqualTo(JwtUtil.PLATFORM_ADMIN);
        assertThat(JwtUtil.getTokenVersionFromClaims(claims)).isEqualTo(3);
        assertThat(JwtUtil.getUserIdFromToken(newAccessToken)).isEqualTo(7L);
    }

    @Test
    void expireWindow_shouldFollowPlatformDefaults() {
        assertThat(JwtUtil.accessTokenExpireMs(JwtUtil.PLATFORM_APP)).isEqualTo(2 * 60 * 60 * 1000L);
        assertThat(JwtUtil.refreshTokenExpireMs(JwtUtil.PLATFORM_APP)).isEqualTo(15L * 24 * 60 * 60 * 1000L);
        assertThat(JwtUtil.accessTokenExpireMs(JwtUtil.PLATFORM_ADMIN)).isEqualTo(30 * 60 * 1000L);
        assertThat(JwtUtil.refreshTokenExpireMs(JwtUtil.PLATFORM_ADMIN)).isEqualTo(7L * 24 * 60 * 60 * 1000L);
    }
}
