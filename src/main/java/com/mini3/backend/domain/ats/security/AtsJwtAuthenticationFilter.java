package com.mini3.backend.domain.ats.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * management-back 로그인에서 발급한 Bearer JWT 를 동일 규칙으로 검사한다.
 */
@RequiredArgsConstructor
public class AtsJwtAuthenticationFilter extends OncePerRequestFilter {

    private final AtsJwtValidator jwtValidator;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            String token = bearer.substring(7).trim();
            if (jwtValidator.validateToken(token)) {
                Claims claims = jwtValidator.getClaims(token);
                String subject = claims.getSubject();
                Object roleClaim = claims.get("role");
                String role = roleClaim != null ? roleClaim.toString() : "ROLE_EMPLOYEE";
                List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                        new SimpleGrantedAuthority(role.startsWith("ROLE_") ? role : "ROLE_" + role)
                );
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        subject,
                        null,
                        authorities
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }
}
