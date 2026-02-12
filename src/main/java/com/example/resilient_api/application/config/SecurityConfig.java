package com.example.resilient_api.application.config;

import com.example.resilient_api.infrastructure.adapters.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        // Actuator - público
                        .pathMatchers("/actuator/**").permitAll()

                        // Swagger/OpenAPI - público
                        .pathMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/webjars/**").permitAll()

                        // ===== ENDPOINTS PÚBLICOS (sin autenticación) =====
                        // Verificar existencia de capacidades
                        .pathMatchers(HttpMethod.POST, "/capacity/check-exists").permitAll()
                        // Obtener capacidades con tecnologías
                        .pathMatchers(HttpMethod.POST, "/capacity/with-technologies").permitAll()

                        // ===== ENDPOINTS ADMIN (solo isAdmin = true) =====
                        // Listar capacidades
                        .pathMatchers(HttpMethod.GET, "/capacity").hasRole("ADMIN")
                        // Listar bootcamps
                        .pathMatchers(HttpMethod.GET, "/capacity/bootcamp").hasRole("ADMIN")
                        // Crear capacidad
                        .pathMatchers(HttpMethod.POST, "/capacity").hasRole("ADMIN")
                        // Crear bootcamp
                        .pathMatchers(HttpMethod.POST, "/capacity/bootcamp").hasRole("ADMIN")
                        // Eliminar bootcamp
                        .pathMatchers(HttpMethod.DELETE, "/capacity/bootcamp/**").hasRole("ADMIN")
                        // Eliminar capacidades por IDs
                        .pathMatchers(HttpMethod.POST, "/capacity/delete-by-ids").hasRole("ADMIN")

                        // ===== ENDPOINTS USER (solo isAdmin = false) =====
                        // Inscribirse en bootcamp
                        .pathMatchers(HttpMethod.POST, "/capacity/bootcamp/enroll").hasRole("USER")
                        // Desinscribirse de bootcamp
                        .pathMatchers(HttpMethod.DELETE, "/capacity/bootcamp/*/unenroll").hasRole("USER")
                        // Ver mis bootcamps
                        .pathMatchers(HttpMethod.GET, "/capacity/bootcamp/my-bootcamps").hasRole("USER")

                        // Por defecto: permitir todo lo demás (para endpoints no especificados)
                        .anyExchange().permitAll()
                )
                .addFilterAt(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .build();
    }
}
