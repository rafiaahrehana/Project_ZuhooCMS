package com.businessos.config;

import com.businessos.auth.user.UserRepository;
import com.businessos.security.JwtAuthFilter;
import com.businessos.security.SubscriptionEnforcementFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpStatus;
import org.springframework.web.cors.CorsConfigurationSource;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final SubscriptionEnforcementFilter subscriptionEnforcementFilter;
    private final com.businessos.modules.demo.DemoReadOnlyFilter demoReadOnlyFilter;
    private final UserRepository userRepository;
    private final CorsConfigurationSource corsConfigurationSource;

    private static final String[] PUBLIC_ENDPOINTS = {
        "/api/auth/**",
        "/api/companies/public/**",
        "/api/clients/public/**",
        "/uploads/**",
        "/api/payments/sslcommerz/callback/**",
        "/api/payments/sslcommerz/ipn",
        // Public company portal content (anonymous visitors browsing /portal/:subdomain)
        "/api/website/**",
        // Anonymous lead capture from the marketing site and tenant portal
        // contact forms. Honeypot + dedupe inside; rate-limit at the edge.
        "/api/public/crm/**",
        // "See Demo" session minting - the token it returns is read-only
        // enforced by DemoReadOnlyFilter.
        "/api/public/demo/**",
        // Public careers pages (candidates browsing /careers/:slug and
        // applying). Slug-scoped, OPEN postings only; honeypot in the
        // apply endpoint.
        "/api/public/careers/**",
        // Landing-page live-traffic SSE stream (anonymous, unauthenticated /home)
        "/api/v1/metrics/**",
        // WebSocket handshake - a browser's native WebSocket transport can't send an
        // Authorization header, so this can't be gated by JwtAuthFilter like a normal
        // endpoint. WebSocketAuthInterceptor authenticates the handshake itself via a
        // ?token= query param and refuses the upgrade if it's missing/invalid - see
        // WebSocketConfig.
        "/ws/**"
    };

    private static final String[] PUBLIC_GET_ENDPOINTS = {
        "/api/locations/**",
        // Plan name/price/description only - nothing sensitive - needed so the public
        // marketing homepage's pricing section can show real, current platform prices
        // instead of hardcoded numbers that drift from what SslCommerzServiceImpl
        // actually charges. Mutating endpoints on this controller stay SUPER_ADMIN-only.
        "/api/subscription-plans"
    };

    /**
     * Uploaded files (avatars, documents) - permitAll like the rest of PUBLIC_ENDPOINTS,
     * but on its own chain so it can opt out of the main chain's default Cache-Control:
     * no-store header. Without this, every uploaded image is re-downloaded in full on
     * every render (no browser caching at all) - fine for a small icon, but a multi-MB
     * avatar effectively never finishes loading in time, showing blank.
     * Filenames are content-hashed/unique per upload, so aggressive caching is safe.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain uploadsFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/uploads/**")
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(AbstractHttpConfigurer::disable)
            .headers(headers -> headers.cacheControl(cache -> cache.disable()))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Without this, Spring Security's default entry point returns 403 for a
            // missing/expired/invalid token, indistinguishable from a real "no permission"
            // 403 - the frontend's silent-refresh-on-401 logic never fires and every
            // module 403s once the access token expires. Return 401 so the interceptor
            // can refresh and retry instead.
            .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/swagger-ui/**",
                            "/v3/api-docs/**",
                            "/swagger-ui.html").permitAll()
                    .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                    .requestMatchers(HttpMethod.GET, PUBLIC_GET_ENDPOINTS).permitAll()
                    .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(subscriptionEnforcementFilter, JwtAuthFilter.class)
            // After JwtAuthFilter because it needs the authenticated user to
            // know whether this is the demo account.
            .addFilterAfter(demoReadOnlyFilter, SubscriptionEnforcementFilter.class);

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return email -> userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService());
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
