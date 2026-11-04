package com.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

@Configuration
@EnableWebSecurity
class SecurityConfig {

    private static final String API_POLICY = "default-src 'none'; frame-ancestors 'none'";
    private static final String DOCS_POLICY = "default-src 'none'; script-src 'self'; style-src 'self' 'unsafe-inline'; "
            + "img-src 'self' data:; font-src 'self' data:; connect-src 'self'; frame-ancestors 'none'";
    private static final String[] DOCS_PATHS = {"/swagger", "/swagger-ui/**", "/v3/api-docs/**"};

    @Bean
    @Order(1)
    SecurityFilterChain docs(HttpSecurity http) throws Exception {
        hardened(http, DOCS_POLICY)
                .securityMatcher(DOCS_PATHS)
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.GET, DOCS_PATHS).permitAll()
                        .anyRequest().denyAll());
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain api(HttpSecurity http) throws Exception {
        hardened(http, API_POLICY)
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.GET, "/api/v1/products", "/api/v1/products/{rowNumber}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health/**").permitAll()
                        .anyRequest().denyAll());
        return http.build();
    }

    private static HttpSecurity hardened(HttpSecurity http, String contentSecurityPolicy) throws Exception {
        return http
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(rc -> rc.disable())
                .headers(h -> h
                        .contentSecurityPolicy(c -> c.policyDirectives(contentSecurityPolicy))
                        .httpStrictTransportSecurity(x -> x.includeSubDomains(true).maxAgeInSeconds(31_536_000))
                        .referrerPolicy(r -> r.policy(ReferrerPolicy.NO_REFERRER))
                        .frameOptions(f -> f.deny()));
    }
}
