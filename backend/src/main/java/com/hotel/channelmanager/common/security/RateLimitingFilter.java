package com.hotel.channelmanager.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.channelmanager.common.exception.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_MINUTE = 30;
    private final ConcurrentHashMap<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        // Protect auth endpoints from brute force
        if (path.startsWith("/api/v1/auth/login") || path.startsWith("/api/v1/auth/refresh")) {
            String clientIp = getClientIp(request);
            long currentMinute = System.currentTimeMillis() / 60000;
            String key = clientIp + ":" + currentMinute;

            RequestCounter counter = requestCounts.computeIfAbsent(key, k -> new RequestCounter(currentMinute));
            if (counter.count.incrementAndGet() > MAX_REQUESTS_PER_MINUTE) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                ErrorResponse error = ErrorResponse.of(
                        "RATE_LIMIT_EXCEEDED",
                        "Too many login attempts.",
                        "Please wait a moment before trying again.",
                        429
                );
                response.getWriter().write(objectMapper.writeValueAsString(error));
                return;
            }

            // Clean up old entries
            requestCounts.entrySet().removeIf(entry -> entry.getValue().minute < currentMinute - 2);
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class RequestCounter {
        final long minute;
        final AtomicInteger count = new AtomicInteger(0);

        RequestCounter(long minute) {
            this.minute = minute;
        }
    }
}

