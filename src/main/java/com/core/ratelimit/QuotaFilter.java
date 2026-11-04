package com.core.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class QuotaFilter extends OncePerRequestFilter {

    private static final int MAX_TRACKED_ADDRESSES = 100_000;

    private final int limit;
    private final Duration block;
    private final Clock clock;
    private final ConcurrentHashMap<String, Quota> quotas = new ConcurrentHashMap<>();

    @Autowired
    public QuotaFilter(@Value("${app.quota.requests}") int limit, @Value("${app.quota.block}") Duration block) {
        this(limit, block, Clock.systemUTC());
    }

    QuotaFilter(int limit, Duration block, Clock clock) {
        this.limit = limit;
        this.block = block;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long now = clock.millis();
        forgetExpiredWhenCrowded(now);
        Quota quota = quotas.compute(request.getRemoteAddr(), (address, current) -> next(current, now));

        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(limit - quota.used()));

        if (quota.admitted()) {
            chain.doFilter(request, response);
            return;
        }
        reject(response, quota, now);
    }

    private Quota next(Quota current, long now) {
        if (current == null || now >= current.expiresAt()) {
            return new Quota(1, now + block.toMillis(), true);
        }
        if (current.used() >= limit) {
            return new Quota(current.used(), current.expiresAt(), false);
        }
        int used = current.used() + 1;
        long expiresAt = used == limit ? now + block.toMillis() : current.expiresAt();
        return new Quota(used, expiresAt, true);
    }

    private void forgetExpiredWhenCrowded(long now) {
        if (quotas.size() > MAX_TRACKED_ADDRESSES) {
            quotas.values().removeIf(quota -> now >= quota.expiresAt());
        }
    }

    private void reject(HttpServletResponse response, Quota quota, long now) throws IOException {
        long seconds = (quota.expiresAt() - now + 999) / 1000;
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(seconds));
        response.setContentType("application/problem+json");
        response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"Too Many Requests\",\"status\":429,"
                + "\"detail\":\"Request limit reached. Retry after " + seconds + " seconds.\"}");
    }

    private record Quota(int used, long expiresAt, boolean admitted) {
    }
}
