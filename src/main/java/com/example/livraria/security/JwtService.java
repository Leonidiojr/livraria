package com.example.livraria.security;

import com.example.livraria.domain.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/** Geracao e validacao de tokens JWT (HS256). */
@Service
public class JwtService {

    public record TokenGerado(String token, Instant expiraEm) {
    }

    private final Key chave;
    private final Duration validade;
    private final Clock clock;

    @Autowired
    public JwtService(@Value("${app.jwt.secret}") String segredoBase64,
                      @Value("${app.jwt.expiration-minutes:120}") long minutos) {
        this(segredoBase64, Duration.ofMinutes(minutos), Clock.systemUTC());
    }

    JwtService(String segredoBase64, Duration validade, Clock clock) {
        this.chave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(segredoBase64));
        this.validade = validade;
        this.clock = clock;
    }

    public TokenGerado gerarToken(Usuario usuario) {
        Instant agora = clock.instant();
        Instant expira = agora.plus(validade);
        String token = Jwts.builder()
                .setSubject(usuario.getEmail())
                .claim("id", usuario.getId())
                .claim("nome", usuario.getNome())
                .claim("perfil", usuario.getPerfil().getNome())
                .setIssuedAt(Date.from(agora))
                .setExpiration(Date.from(expira))
                .signWith(chave, SignatureAlgorithm.HS256)
                .compact();
        return new TokenGerado(token, expira);
    }

    /** Retorna o e-mail (subject) se o token for valido e nao estiver expirado. */
    public Optional<String> extrairEmail(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(chave)
                    .setClock(() -> Date.from(clock.instant()))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return Optional.ofNullable(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
