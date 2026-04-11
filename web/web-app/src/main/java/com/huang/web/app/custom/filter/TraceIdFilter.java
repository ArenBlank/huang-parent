package com.huang.web.app.custom.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.huang.common.utils.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class TraceIdFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TraceIdFilter.class);
    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String INSTANCE_HEADER = "X-App-Instance";
    private static final String MDC_KEY = "traceId";
    private static final String MDC_USER_KEY = "userId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (!StringUtils.hasText(traceId)) {
            traceId = request.getHeader("trace-id");
        }
        if (!StringUtils.hasText(traceId)) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }

        MDC.put(MDC_KEY, traceId);
        Long userId = resolveUserId(request);
        if (userId != null) {
            MDC.put(MDC_USER_KEY, String.valueOf(userId));
        }
        response.setHeader(TRACE_ID_HEADER, traceId);
        response.setHeader(INSTANCE_HEADER, "web-app:" + request.getLocalPort());
        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long costMs = System.currentTimeMillis() - start;
            int status = response.getStatus();
            log.info("APP {} {} {} {}ms traceId={} userId={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    status,
                    costMs,
                    traceId,
                    userId == null ? "-" : userId);
            MDC.remove(MDC_KEY);
            MDC.remove(MDC_USER_KEY);
        }
    }

    private Long resolveUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (!StringUtils.hasText(token)) {
            token = request.getHeader("access-token");
        }
        if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (!StringUtils.hasText(token)) {
            return null;
        }
        return JwtUtil.getUserIdFromToken(token);
    }
}
