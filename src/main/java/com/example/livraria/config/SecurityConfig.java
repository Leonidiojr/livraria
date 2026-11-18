package com.example.livraria.config;

import com.example.livraria.domain.Perfil;
import com.example.livraria.security.JwtAuthenticationFilter;
import com.example.livraria.security.JwtService;
import com.example.livraria.security.UsuarioDetailsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class SecurityConfig {

    private static final String ADMIN = Perfil.ADMIN;
    private static final String VENDEDOR = Perfil.VENDEDOR;

    private static final String[] LEITURA_CATALOGO = {
            "/api/livros/**", "/api/autores/**", "/api/categorias/**", "/api/editoras/**"
    };

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService,
                                           UsuarioDetailsService usuarioDetailsService) throws Exception {
        var jwtFilter = new JwtAuthenticationFilter(jwtService, usuarioDetailsService);
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .httpBasic(b -> b.disable())
                .formLogin(f -> f.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // front-end estatico, documentacao e erros
                        .requestMatchers("/", "/index.html", "/app.html", "/css/**", "/js/**", "/img/**", "/favicon.ico", "/error").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // autenticacao
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/registro").permitAll()
                        // imagens de capa/autor: leitura publica (usadas em tags img, sem token)
                        .requestMatchers(HttpMethod.GET, "/api/livros/*/capa", "/api/autores/*/foto").permitAll()
                        // catalogo: leitura para qualquer usuario autenticado, escrita apenas ADMIN
                        .requestMatchers(HttpMethod.GET, LEITURA_CATALOGO).authenticated()
                        .requestMatchers(LEITURA_CATALOGO).hasRole(ADMIN)
                        // operacao da loja: ADMIN e VENDEDOR
                        .requestMatchers("/api/estoque/**", "/api/clientes/**", "/api/pagamentos/**").hasAnyRole(ADMIN, VENDEDOR)
                        // pedidos: todos consultam/criam/cancelam (CLIENTE apenas os proprios, regra no servico);
                        // alterar itens e excluir apenas ADMIN e VENDEDOR
                        .requestMatchers(HttpMethod.GET, "/api/pedidos/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/pedidos").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/pedidos/*/status").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/pedidos/*/pagamento").authenticated()
                        .requestMatchers("/api/pedidos/**").hasAnyRole(ADMIN, VENDEDOR)
                        // administracao: usuarios, perfis e auditoria apenas ADMIN
                        .requestMatchers("/api/usuarios/**", "/api/perfis/**", "/api/auditoria/**").hasRole(ADMIN)
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:5500,http://127.0.0.1:5500,http://localhost:3000,http://localhost:5173}") String origens) {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(Arrays.stream(origens.split(",")).map(String::trim).toList());
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cfg);
        return source;
    }
}
