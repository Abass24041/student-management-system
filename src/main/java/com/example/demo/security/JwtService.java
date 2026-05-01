package com.example.demo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtService {

    private final SecretKey signingKey;
    private final String issuer;
    private final Duration tokenTtl;
    private final JwtParser jwtParser;

    public JwtService(
            @Value("${application.security.jwt.secret}") String secret,
            @Value("${application.security.jwt.issuer:demo-app}") String issuer,
            @Value("${application.security.jwt.ttl-minutes:30}") long ttlMinutes) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.issuer = issuer;
        this.tokenTtl = Duration.ofMinutes(ttlMinutes);
        this.jwtParser = Jwts.parser()
                .verifyWith(signingKey)
                .clockSkewSeconds(30)
                .build();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return jwtParser.parseSignedClaims(token).getPayload();
    }

    private boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final Claims claims = extractAllClaims(token);
        final String username = claims.getSubject();
        return username.equals(userDetails.getUsername())
                && issuer.equals(claims.getIssuer())
                && !isTokenExpired(claims);
    }

    public String generateToken(String userName) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, userName);
    }

    private String createToken(Map<String, Object> claims, String userName) {
        Date issuedAt = Date.from(Instant.now());
        Date expiresAt = Date.from(issuedAt.toInstant().plus(tokenTtl));
        Map<String, Object> tokenClaims = Collections.unmodifiableMap(new HashMap<>(claims));
        return Jwts.builder()
                .claims(tokenClaims)
                .subject(userName)
                .issuer(issuer)
                .issuedAt(issuedAt)
                .expiration(expiresAt)
                .signWith(signingKey)
                .compact();
    }
}
