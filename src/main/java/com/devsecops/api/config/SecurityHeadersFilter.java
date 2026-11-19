package com.devsecops.api.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Filtro simples para adicionar cabecalhos HTTP de seguranca em todas as respostas da API.
 * Esses cabecalhos sao validados na etapa de DAST da pipeline DevSecOps.
 */
@Component
public class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (response instanceof HttpServletResponse httpResponse) {
            httpResponse.setHeader("X-Content-Type-Options", "nosniff");
            httpResponse.setHeader("X-Frame-Options", "DENY");
            httpResponse.setHeader("X-XSS-Protection", "0");
            httpResponse.setHeader("Referrer-Policy", "no-referrer");
            httpResponse.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'");
            httpResponse.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
            httpResponse.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
        }
        chain.doFilter(request, response);
    }
}
