package com.fund.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fund.util.JwtUtil;
import com.fund.vo.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        
        // 跳过不需要认证的路径
        if (isPublicPath(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (jwtUtil.validateToken(token)) {
                Long userId = jwtUtil.getUserIdFromToken(token);
                String email = jwtUtil.getEmailFromToken(token);

                request.setAttribute("userId", userId);
                request.setAttribute("email", email);

                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, new ArrayList<>());
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                filterChain.doFilter(request, response);
            } else {
                // Token 无效，返回 401
                sendErrorResponse(response, "登录已过期，请重新登录");
            }
        } else {
            // 没有 Token，返回 401
            sendErrorResponse(response, "未登录，请先登录");
        }
    }

    private boolean isPublicPath(String uri) {
        return uri.startsWith("/api/auth/") ||
               uri.startsWith("/api/ocr/") ||
               uri.startsWith("/api/market/") ||
               uri.equals("/api/fund/search") ||
               uri.equals("/api/fund/data") ||
               uri.equals("/api/fund/performance") ||
               uri.startsWith("/css/") ||
               uri.startsWith("/js/") ||
               uri.startsWith("/images/") ||
               uri.equals("/") ||
               uri.equals("/favicon.ico");
    }

    private void sendErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        
        ApiResponse<Object> apiResponse = ApiResponse.error(message);
        ObjectMapper mapper = new ObjectMapper();
        response.getWriter().write(mapper.writeValueAsString(apiResponse));
    }
}
