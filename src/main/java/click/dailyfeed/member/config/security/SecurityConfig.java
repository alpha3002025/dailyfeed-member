package click.dailyfeed.member.config.security;

import click.dailyfeed.member.config.security.filter.JwtAuthenticationFilter;
import click.dailyfeed.member.config.security.userdetails.CustomUserDetailsService;
import click.dailyfeed.member.domain.jwt.service.JwtKeyHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.security.Key;
import java.util.List;

@RequiredArgsConstructor
@EnableWebSecurity(debug = true)  // Security 디버그 활성화
@Configuration
public class SecurityConfig {
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtKeyHelper jwtKeyHelper;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("http://localhost:3000", "https://yourdomain.com"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity httpSecurity,
            AuthenticationManager authenticationManager
    ) throws Exception {
        Key currentJwtKey = jwtKeyHelper.getCurrentJwtKey();
        try{
            return httpSecurity
                    .csrf(AbstractHttpConfigurer::disable)
                    .formLogin(AbstractHttpConfigurer::disable)
                    .httpBasic(AbstractHttpConfigurer::disable)
                    .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                    .addFilterBefore(
                            new JwtAuthenticationFilter(authenticationManager, currentJwtKey),
                            UsernamePasswordAuthenticationFilter.class
                    )
                    .sessionManagement(configurer -> configurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .headers(configurer -> configurer.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                    .authorizeHttpRequests(
                            configurer -> configurer
                                    .requestMatchers(
                                            "/",
                                            "/welcome",
                                            "/welcome/**",
                                            "/img/**",
                                            "/css/**",
                                            "/api/authentication/login",
                                            "/api/authentication/signup",
//                                            "/api/healthcheck/**",      // istio 및 kubernetes 에서 Rule 적용
                                            "/swagger-ui/**",       // istio 및 kubernetes 에서 Rule 적용
                                            "/swagger-example/**",  // istio 및 kubernetes 에서 Rule 적용
                                            "/swagger-ui.html",     // istio 및 kubernetes 에서 Rule 적용
                                            "/api-docs", "/api-docs/**", "/v3/api-docs/**"  // istio 및 kubernetes 에서 Rule 적용
                                    ).permitAll()
                                    .requestMatchers(
                                            "/api/members/**"  // 임시로 members만 테스트
                                    ).permitAll()
                                    .requestMatchers(
                                            "/api/authentication/logout",
                                            "/api/members/follow/**",
                                            "/api/token/**"
                                    )
                                    .hasAnyRole("MEMBER", "MANAGER", "ADMIN")
                                    .anyRequest().authenticated()
                    )
                    .userDetailsService(customUserDetailsService)
                    .build();
        }
        catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}
