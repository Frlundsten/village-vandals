package com.villagevandals.vandals.web.jwt;

import com.villagevandals.vandals.web.UserInfoService;
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

  private final UserInfoService userInfoService;
  private final JwtService jwtService;

  public JwtAuthFilter(UserInfoService userInfoService, JwtService jwtService) {
    this.userInfoService = userInfoService;
    this.jwtService = jwtService;
  }

  /**
   * Validate potential bearer in the request. This will fail if the jwt is malformed/invalid.
   *
   * @param request
   * @param response
   * @param filterChain
   * @throws ServletException
   * @throws IOException
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
