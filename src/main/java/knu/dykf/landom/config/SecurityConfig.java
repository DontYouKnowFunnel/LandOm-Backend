package knu.dykf.landom.config;

import jakarta.servlet.http.HttpServletResponse;
import knu.dykf.landom.exception.ErrorCode;
import knu.dykf.landom.jwt.JwtAuthenticationFilter;
import knu.dykf.landom.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;

    @Value("${server.url}")
    private String serverUrl;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @SneakyThrows
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) ->
                                writeErrorResponse(response, ErrorCode.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeErrorResponse(response, ErrorCode.HANDLE_ACCESS_DENIED))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**", "/swagger-ui/**", "/v3/api-docs/**", "/api/v1/events", "/api/v1/events/**", "/index.html", "/landom-sdk.umd.js", "/api/v1/projects/{id}/analytics/section").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration eventConfiguration = new CorsConfiguration();
        eventConfiguration.setAllowedOrigins(List.of("*"));
        eventConfiguration.setAllowedMethods(List.of("*"));
        eventConfiguration.setAllowedHeaders(List.of("*"));
        eventConfiguration.setAllowCredentials(false);
        eventConfiguration.setMaxAge(3600L);

        CorsConfiguration defaultConfiguration = new CorsConfiguration();
        defaultConfiguration.setAllowedOrigins(List.of(
                "http://localhost:8080",
                "http://localhost:5173",// 로컬 개발 환경
                serverUrl,  // 실제 운영 환경
                frontendUrl
        ));
        defaultConfiguration.addAllowedMethod("*");        // 모든 HTTP Method 허용
        defaultConfiguration.addAllowedHeader("*");        // 모든 Header 허용
        defaultConfiguration.setAllowCredentials(true);    // 쿠키/인증정보 포함 허용

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/v1/events", eventConfiguration);
        source.registerCorsConfiguration("/**", defaultConfiguration);
        return source;
    }

    @SneakyThrows
    private void writeErrorResponse(HttpServletResponse response, ErrorCode errorCode) {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("""
                {"code":"%s","message":"%s"}
                """.formatted(errorCode.name(), errorCode.getMessage()));
    }
}
