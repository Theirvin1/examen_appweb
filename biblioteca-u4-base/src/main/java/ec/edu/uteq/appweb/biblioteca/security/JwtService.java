package ec.edu.uteq.appweb.biblioteca.security;

import ec.edu.uteq.appweb.biblioteca.domain.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final String secretoBase64;
    private final long duracionMinutos;

    public JwtService(@Value("${app.jwt.secreto}") String secretoBase64,
                      @Value("${app.jwt.expiracion-minutos}") long duracionMinutos) {
        this.secretoBase64 = secretoBase64;
        this.duracionMinutos = duracionMinutos;
    }

    private SecretKey getClaveFirma() {
        byte[] keyBytes = Decoders.BASE64.decode(secretoBase64);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generar(Usuario usuario) {
        Date ahora = new Date();
        Date expiracion = new Date(ahora.getTime() + (duracionMinutos * 60 * 1000));
        return Jwts.builder()
                .subject(usuario.getUsername())
                .claim("rol", usuario.getRol().name())
                .id(UUID.randomUUID().toString())
                .issuedAt(ahora)
                .expiration(expiracion)
                .signWith(getClaveFirma())
                .compact();
    }

    private Claims extraerTodosLosClaims(String token) {
        return Jwts.parser()
                .verifyWith(getClaveFirma())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extraerUsername(String token) {
        return extraerTodosLosClaims(token).getSubject();
    }

    public String extraerRol(String token) {
        return extraerTodosLosClaims(token).get("rol", String.class);
    }

    public boolean esValido(String token) {
        try {
            extraerTodosLosClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
