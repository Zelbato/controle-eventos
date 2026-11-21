package br.edu.exemplo.controleeventos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Partes 08 a 12: autenticação, perfis e regras de acesso.
 *
 * A autenticação usa o UsuarioDetailsService (Parte 09), que busca o
 * usuário no PostgreSQL. O usuário fixo em memória da Parte 08 foi removido.
 *
 * A proteção CSRF permanece ATIVADA: todos os formulários que alteram dados
 * usam POST com th:action, e o Thymeleaf inclui o token automaticamente.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Páginas públicas (Partes 08 e 10)
                .requestMatchers("/css/**", "/login", "/cadastro", "/error", "/acesso-negado")
                    .permitAll()

                // Área do participante (Partes 11 e 12)
                .requestMatchers(
                        "/catalogo",
                        "/catalogo/**",
                        "/meus-eventos",
                        "/inscricoes/minha",
                        "/minhas-inscricoes/*/cancelar")
                    .hasRole("PARTICIPANTE")

                // Área administrativa (Partes 09 e 11)
                .requestMatchers(
                        "/eventos/**",
                        "/participantes/**",
                        "/inscricoes/**",
                        "/relatorios/**")
                    .hasRole("ADMIN")

                // Demais rotas (ex.: página inicial) exigem apenas login
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/acesso-negado")
            );

        return http.build();
    }
}
