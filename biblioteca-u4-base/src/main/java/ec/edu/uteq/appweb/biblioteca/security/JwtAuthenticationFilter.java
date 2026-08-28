package ec.edu.uteq.appweb.biblioteca.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion,
                                    HttpServletResponse respuesta,
                                    FilterChain cadena) throws ServletException, IOException {
        String authHeader = peticion.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            cadena.doFilter(peticion, respuesta);
            return;
        }

        String token = authHeader.substring(7);
        if (jwtService.esValido(token)) {
            String username = jwtService.extraerUsername(token);
            String rol = jwtService.extraerRol(token);

            SimpleGrantedAuthority autoridad = new SimpleGrantedAuthority("ROLE_" + rol);
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    username, null, List.of(autoridad));

            SecurityContextHolder.getContext().setAuthentication(auth);
        } else {
            SecurityContextHolder.clearContext();
        }

        cadena.doFilter(peticion, respuesta);
    }
}
