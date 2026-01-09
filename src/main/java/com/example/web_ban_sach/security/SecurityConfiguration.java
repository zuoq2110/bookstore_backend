package com.example.web_ban_sach.security;

import com.example.web_ban_sach.Filter.JwtFiler;
import com.example.web_ban_sach.Service.UserService;
import com.example.web_ban_sach.util.UserSecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.Arrays;

@Configuration
public class SecurityConfiguration {
    @Autowired
    private JwtFiler jwtFiler;
    @Bean
    public BCryptPasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserSecurityService userSecurityService){
        DaoAuthenticationProvider dap = new DaoAuthenticationProvider();
        dap.setUserDetailsService(userSecurityService);
        dap.setPasswordEncoder(passwordEncoder());
        return dap;
    }
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{
        http.authorizeHttpRequests(
                config -> config
                        // Add explicit bypass for OTP auth endpoints FIRST
                        .requestMatchers("/api/auth/**").permitAll()
                        // Add 2FA endpoints - require authentication
                        .requestMatchers("/api/2fa/**").authenticated()
                        // Add specific bypass for chat testing
                        .requestMatchers("/api/chat/**").permitAll()
                        // Add bypass for test pagination endpoints
                        .requestMatchers("/test-pagination/**").permitAll()
                        .requestMatchers(HttpMethod.GET, Endpoints.PUBLIC_GET_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.POST, Endpoints.PUBLIC_POST_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.PUT, Endpoints.PUBLIC_PUT_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.DELETE, Endpoints.PUBLIC_DELETE_ENDPOINTS).permitAll()
                        // Order Tracking endpoints - Đã include trong /don-hang/** ở PUBLIC_GET_ENDPOINTS
                        // Seller endpoints - Yêu cầu authentication (sẽ check isSeller trong controller)
                        .requestMatchers(HttpMethod.GET, Endpoints.SELLER_GET_ENDPOINTS).authenticated()
                        .requestMatchers(HttpMethod.POST, Endpoints.SELLER_POST_ENDPOINTS).authenticated()
                        .requestMatchers(HttpMethod.PUT, Endpoints.SELLER_PUT_ENDPOINTS).authenticated()
                        .requestMatchers(HttpMethod.DELETE, Endpoints.SELLER_DELETE_ENDPOINTS).authenticated()
                        // Admin endpoints
                        .requestMatchers(HttpMethod.GET, Endpoints.ADMIN_GET_ENDPOINTS).hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.POST, Endpoints.ADMIN_POST_ENDPOINTS).hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.PUT, Endpoints.ADMIN_PUT_ENDPOINTS).hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, Endpoints.ADMIN_DELETE_ENDPOINTS).hasAuthority("ADMIN")
                        // Default rule for all other endpoints
                        .anyRequest().authenticated()
        );
        http.cors(cors -> {
            cors.configurationSource(request -> {
                CorsConfiguration corsConfig = new CorsConfiguration();
                corsConfig.addAllowedOrigin("*");
                corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
                corsConfig.addAllowedHeader("*");
                return corsConfig;
            });
        });
        http.addFilterBefore(jwtFiler, UsernamePasswordAuthenticationFilter.class);
        http.sessionManagement((session)->session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.httpBasic(Customizer.withDefaults());
        http.csrf(csrf -> csrf.disable());
        return http.build();
    }
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
