package com.astral.express.pccms.identity.security;

import com.astral.express.pccms.identity.service.CustomUserDetails;
import com.astral.express.pccms.user.entity.Users;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Getter
@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64URL.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractJti(String token) {
        return extractAllClaims(token).get("jti", String.class);
    }

    public String extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        String userId = claims.get("userId", String.class);
        return userId != null ? userId : claims.getSubject();
    }

    public String extractEmail(String token) {
        return extractAllClaims(token).get("email", String.class);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
//        return Jwts.parserBuilder().setSigningKey(getSigningKey()).build()
//                .parseClaimsJws(token).getBody();
        return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateToken(Users user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("jti", UUID.randomUUID().toString());
        claims.put("userId", user.getId().toString());
        claims.put("role", user.getRole().getCode());
        claims.put("email", user.getEmail());
        return Jwts.builder()
                .claims(claims)
                .subject(user.getEmail())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), Jwts.SIG.HS512)
                .compact();
    }

    public String generateRefreshToken(Users user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("jti", UUID.randomUUID().toString());
        claims.put("tokenType", "REFRESH");

        return createToken(claims, user.getEmail(), refreshExpiration);
    }

    private String createToken(Map<String, Object> claims, String subject, Long expirationTime) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(getSigningKey(), Jwts.SIG.HS512)
                .compact();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String subject = extractUsername(token);
        boolean matchesUsername = subject.equals(userDetails.getUsername());
        boolean matchesUserId = userDetails instanceof CustomUserDetails customUserDetails
                && subject.equals(customUserDetails.getId().toString());
        return (matchesUsername || matchesUserId) && !isTokenExpired(token);
    }
}
