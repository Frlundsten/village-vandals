package com.villagevandals.vandals.web.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

  public static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);

  private final JwtService jwtService;

  public JwtAuthFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  /**
   * Extracts and validates the Bearer JWT from the {@code Authorization} header.
   * Populates the {@link SecurityContextHolder} on success so downstream filters and
   * controllers can resolve the authenticated principal. Responds with 401 and short-circuits
   * the filter chain if the token is expired or malformed; passes through silently if no
   * {@code Authorization} header is present (letting Spring Security handle it).
   */
  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String authHeader = request.getHeader("Authorization");

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    String token = authHeader.substring(7);

    try {
      if (jwtService.isTokenExpired(token)) {
        logger.debug("Token is expired");
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Login again");
        return;
      }

      String username = jwtService.extractUsername(token);

      if (SecurityContextHolder.getContext().getAuthentication() == null) {

        List<SimpleGrantedAuthority> authorities =
            jwtService.extractRoles(token).stream().map(SimpleGrantedAuthority::new).toList();

        UsernamePasswordAuthenticationToken authToken =
            new UsernamePasswordAuthenticationToken(username, null, authorities);

        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authToken);
      }

    } catch (Exception e) {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT");
      return;
    }

    filterChain.doFilter(request, response);
  }
}
