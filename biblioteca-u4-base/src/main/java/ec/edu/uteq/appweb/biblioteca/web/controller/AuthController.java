package ec.edu.uteq.appweb.biblioteca.web.controller;

import ec.edu.uteq.appweb.biblioteca.domain.Usuario;
import ec.edu.uteq.appweb.biblioteca.repository.UsuarioRepository;
import ec.edu.uteq.appweb.biblioteca.security.JwtService;
import ec.edu.uteq.appweb.biblioteca.web.dto.ApiResponse;
import ec.edu.uteq.appweb.biblioteca.web.dto.LoginRequest;
import ec.edu.uteq.appweb.biblioteca.web.dto.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Optional;
import java.net.URI;
import org.springframework.http.ProblemDetail;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final long duracionMinutos;

    public AuthController(UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService,
                          @Value("${app.jwt.expiracion-minutos}") long duracionMinutos) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.duracionMinutos = duracionMinutos;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsernameAndActivoTrue(request.username());

        if (usuarioOpt.isEmpty() || !passwordEncoder.matches(request.password(), usuarioOpt.get().getPasswordHash())) {
            ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Credenciales invalidas");
            problema.setTitle("Error de autenticacion");
            problema.setType(URI.create("https://uteq.edu.ec/errores/autenticacion"));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problema);
        }

        Usuario usuario = usuarioOpt.get();
        String token = jwtService.generar(usuario);
        LoginResponse response = new LoginResponse(
                usuario.getUsername(),
                usuario.getRol().name(),
                "Bearer",
                duracionMinutos * 60
        );

        return ResponseEntity.ok(ApiResponse.ok(response, "Login exitoso"));
    }
}
