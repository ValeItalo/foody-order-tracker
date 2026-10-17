package com.foody.ordertracker.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtService")
class JwtServiceTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-0123456789";

    private final JwtService jwtService = new JwtService(SECRET, 60);

    @Test
    @DisplayName("token gerado é válido e devolve o e-mail do subject")
    void shouldGenerateAndValidateToken() {
        String token = jwtService.generateToken("ana@foody.com");

        assertThat(jwtService.extractEmailIfValid(token)).contains("ana@foody.com");
    }

    @Test
    @DisplayName("token adulterado é rejeitado")
    void shouldRejectTamperedToken() {
        String token = jwtService.generateToken("ana@foody.com");
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThat(jwtService.extractEmailIfValid(tampered)).isEmpty();
    }

    @Test
    @DisplayName("token assinado com outro segredo é rejeitado")
    void shouldRejectTokenSignedWithAnotherSecret() {
        JwtService other = new JwtService("another-secret-another-secret-another-0123456789", 60);
        String foreign = other.generateToken("ana@foody.com");

        assertThat(jwtService.extractEmailIfValid(foreign)).isEmpty();
    }

    @Test
    @DisplayName("token expirado é rejeitado")
    void shouldRejectExpiredToken() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Instant past = Instant.now().minusSeconds(3600);
        String expired = Jwts.builder()
                .subject("ana@foody.com")
                .issuedAt(Date.from(past.minusSeconds(60)))
                .expiration(Date.from(past))
                .signWith(key)
                .compact();

        assertThat(jwtService.extractEmailIfValid(expired)).isEmpty();
    }

    @Test
    @DisplayName("token nulo ou vazio é rejeitado")
    void shouldRejectBlankToken() {
        assertThat(jwtService.extractEmailIfValid("")).isEmpty();
        assertThat(jwtService.extractEmailIfValid(null)).isEmpty();
    }

    @Test
    @DisplayName("segredo curto falha na criação do serviço")
    void shouldFailFastWithWeakSecret() {
        assertThatThrownBy(() -> new JwtService("curto", 60))
                .isInstanceOf(io.jsonwebtoken.security.WeakKeyException.class);
    }
}