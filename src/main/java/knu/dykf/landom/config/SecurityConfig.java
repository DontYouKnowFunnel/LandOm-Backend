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
import org.springframework.http.HttpMethod;
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
import java.util.stream.Stream;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;

    @Value("${server.url:}")
    private String serverUrl;

    @Value("${frontend.url:}")
    private String frontendUrl;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @SneakyThrows
    public SecurityFilterChain filterChain(HttpSecurity http) {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) ->
                                writeErrorResponse(res, ErrorCode.UNAUTHORIZED))
                        .accessDeniedHandler((req, res, ex) ->
                                writeErrorResponse(res, ErrorCode.HANDLE_ACCESS_DENIED))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/events").permitAll()
                        .requestMatchers("/api/v1/events/**").permitAll()
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/index.html",
                                "/landom-sdk.umd.js",
                                "/api/v1/projects/*/analytics/section"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration events = new CorsConfiguration();

        events.setAllowedOriginPatterns(List.of("*"));
        events.setAllowedMethods(List.of("POST", "OPTIONS"));
        events.setAllowedHeaders(List.of("Content-Type", "X-Project-Key"));
        events.setAllowCredentials(true);
        events.setAllowPrivateNetwork(true);

        CorsConfiguration defaults = new CorsConfiguration();
        defaults.setAllowedOrigins(resolveAllowedOrigins());
        defaults.setAllowedMethods(List.of("*"));
        defaults.setAllowedHeaders(List.of("*"));
        defaults.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/v1/events", events);
        source.registerCorsConfiguration("/api/v1/events/**", events);
        source.registerCorsConfiguration("/**", defaults);
        return source;
    }

    private List<String> resolveAllowedOrigins() {
        return Stream.of(
                        "http://localhost:8080",
                        "http://localhost:5173",
                        frontendUrl,
                        serverUrl
                )
                .filter(origin -> origin != null && !origin.isBlank())
                .distinct()
                .toList();
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
