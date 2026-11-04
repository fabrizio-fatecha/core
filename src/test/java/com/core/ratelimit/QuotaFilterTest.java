package com.core.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class QuotaFilterTest {

    private static final String PATH = "/api/v1/products/1";

    private final TestClock clock = new TestClock();
    private final QuotaFilter filter = new QuotaFilter(10, Duration.ofMinutes(30), clock);

    @Test
    void firstTenRequestsAreAdmittedAndTheEleventhIsRejected() throws Exception {
        for (int i = 1; i <= 10; i++) {
            MockHttpServletResponse response = call("203.0.113.5", PATH);
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("10");
            assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo(String.valueOf(10 - i));
        }

        MockHttpServletResponse rejected = call("203.0.113.5", PATH);

        assertThat(rejected.getStatus()).isEqualTo(429);
        assertThat(rejected.getHeader("Retry-After")).isEqualTo("1800");
        assertThat(rejected.getHeader("X-RateLimit-Remaining")).isEqualTo("0");
        assertThat(rejected.getContentType()).isEqualTo("application/problem+json");
        assertThat(rejected.getContentAsString()).contains("Too Many Requests").contains("1800 seconds");
    }

    @Test
    void theBlockLastsThirtyMinutesFromTheTenthRequest() throws Exception {
        useUpTheQuota("203.0.113.5");

        clock.advance(Duration.ofMinutes(29).plusSeconds(59));
        MockHttpServletResponse stillBlocked = call("203.0.113.5", PATH);
        assertThat(stillBlocked.getStatus()).isEqualTo(429);
        assertThat(stillBlocked.getHeader("Retry-After")).isEqualTo("1");

        clock.advance(Duration.ofSeconds(1));
        MockHttpServletResponse freshQuota = call("203.0.113.5", PATH);
        assertThat(freshQuota.getStatus()).isEqualTo(200);
        assertThat(freshQuota.getHeader("X-RateLimit-Remaining")).isEqualTo("9");
    }

    @Test
    void rejectedRequestsDoNotExtendTheBlock() throws Exception {
        useUpTheQuota("203.0.113.5");

        clock.advance(Duration.ofMinutes(10));
        assertThat(call("203.0.113.5", PATH).getStatus()).isEqualTo(429);
        clock.advance(Duration.ofMinutes(10));
        assertThat(call("203.0.113.5", PATH).getStatus()).isEqualTo(429);
        clock.advance(Duration.ofMinutes(10));

        assertThat(call("203.0.113.5", PATH).getStatus()).isEqualTo(200);
    }

    @Test
    void eachAddressHasItsOwnQuota() throws Exception {
        useUpTheQuota("203.0.113.5");

        assertThat(call("203.0.113.5", PATH).getStatus()).isEqualTo(429);
        assertThat(call("198.51.100.7", PATH).getStatus()).isEqualTo(200);
    }

    @Test
    void onlyApiRequestsCountTowardsTheQuota() throws Exception {
        for (int i = 0; i < 30; i++) {
            assertThat(call("203.0.113.5", "/swagger-ui/index.html").getStatus()).isEqualTo(200);
            assertThat(call("203.0.113.5", "/v3/api-docs").getStatus()).isEqualTo(200);
        }

        MockHttpServletResponse first = call("203.0.113.5", PATH);

        assertThat(first.getStatus()).isEqualTo(200);
        assertThat(first.getHeader("X-RateLimit-Remaining")).isEqualTo("9");
    }

    @Test
    void theCountStartsOverWhenTheLimitIsNeverReached() throws Exception {
        for (int i = 0; i < 5; i++) {
            call("203.0.113.5", PATH);
        }

        clock.advance(Duration.ofMinutes(30));
        MockHttpServletResponse response = call("203.0.113.5", PATH);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("9");
    }

    private void useUpTheQuota(String address) throws Exception {
        for (int i = 0; i < 10; i++) {
            assertThat(call(address, PATH).getStatus()).isEqualTo(200);
        }
    }

    private MockHttpServletResponse call(String address, String path) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRemoteAddr(address);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    private static final class TestClock extends Clock {

        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
