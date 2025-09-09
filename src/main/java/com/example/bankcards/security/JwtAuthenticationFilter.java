package com.example.bankcards.security;

import com.example.bankcards.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, CustomUserDetailsService userDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Извлекаем токен из заголовка Authorization
        String token = extractTokenFromRequest(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Извлекаем имя пользователя (subject) из токена
        String username = jwtTokenProvider.extractSubject(token);

        // 3. Проверяем, что имя пользователя извлечено и для этого пользователя еще не установлена аутентификация
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Загружаем детали пользователя из базы данных
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            // 4. Проверяем валидность токена (провайдер проверит срок действия и подпись)
            if (jwtTokenProvider.isTokenValid(token)) {

                // Создаем объект аутентификации
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null, // credentials - нам они не нужны, так как мы используем токен
                        userDetails.getAuthorities()
                );

                // Дополняем объект деталями запроса (IP, сессия и т.д.)
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 5. Устанавливаем аутентификацию в SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        filterChain.doFilter(request, response);
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        final String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // Обрезаем "Bearer "
        }
        return null;
    }
}
