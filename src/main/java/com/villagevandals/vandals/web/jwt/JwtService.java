package com.villagevandals.vandals.web.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class JwtService {

  @Value("${jwt.secret}")
  private String SECRET;

  private static final long ACCESS_EXP_MS = 1000 * 60 * 30;

  /**
   * Generates a signed JWT for the given user. Embeds a {@code roles} claim and expires
   * after {@value ACCESS_EXP_MS} ms.
   */
  public String generateToken(UserDetails userDetails) {
    return generateTokenWithUsername(userDetails.getUsername());
  }

  /**
   * Generates a signed JWT for the given username, assigning the default ROLE_USER role.
   * Use this when the full {@link org.springframework.security.core.userdetails.UserDetails}
   * object is not available, such as after a Keycloak OAuth callback.
   */
  public String generateTokenWithUsername(String username) {
    Map<String, Object> claims = new HashMap<>();
    // Always assign ROLE_USER for now
    claims.put("roles", List.of("ROLE_USER"));
    return createToken(claims, username);
  }

  private String createToken(Map<String, Object> claims, String username) {
    return Jwts.builder()
        .setClaims(claims)
        .setSubject(username)
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + ACCESS_EXP_MS))
        .signWith(getSignKey(), SignatureAlgorithm.HS256)
        .compact();
  }

  private Key getSignKey() {
    byte[] keyBytes = Decoders.BASE64.decode(SECRET);
    return Keys.hmacShaKeyFor(keyBytes);
  }

  /**
   * Extracts the username ({@code sub} claim) from the token without validating expiry.
   */
  public String extractUsername(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  /**
   * Extracts the expiration date from the token without validating whether it has expired.
   */
  public Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
  }

  /**
   * Applies {@code claimsResolver} to the verified claims of the token.
   * Throws a JJWT exception if the token signature is invalid or the token is malformed.
   */
  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parserBuilder().setSigningKey(getSignKey()).build().parseClaimsJws(token).getBody();
  }

  /**
   * Returns true if the token's expiration timestamp is in the past.
   */
  public Boolean isTokenExpired(String token) {
    return extractExpiration(token).before(new Date());
  }

  @SuppressWarnings("unchecked")
  /**
   * Extracts the {@code roles} claim as a list of role strings (e.g. {@code "ROLE_USER"}).
   */
  public List<String> extractRoles(String token) {
    return extractClaim(token, claims -> (List<String>) claims.get("roles"));
  }
}
