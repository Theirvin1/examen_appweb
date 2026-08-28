package ec.edu.uteq.appweb.biblioteca.web.controller;

import ec.edu.uteq.appweb.biblioteca.domain.Libro;
import ec.edu.uteq.appweb.biblioteca.integration.OpenLibraryClient;
import ec.edu.uteq.appweb.biblioteca.integration.OpenLibraryResponse;
import ec.edu.uteq.appweb.biblioteca.service.LibroService;
import ec.edu.uteq.appweb.biblioteca.web.dto.ApiResponse;
import ec.edu.uteq.appweb.biblioteca.web.dto.LibroEnriquecidoResponse;
import ec.edu.uteq.appweb.biblioteca.web.dto.LibroRequest;
import ec.edu.uteq.appweb.biblioteca.web.dto.LibroResponse;
import ec.edu.uteq.appweb.biblioteca.web.dto.PageMeta;
import ec.edu.uteq.appweb.biblioteca.web.mapper.LibroMapper;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/libros")
public class LibroController {

    private final LibroService servicio;
    private final LibroMapper mapper;
    private final OpenLibraryClient openLibraryClient;

    public LibroController(LibroService servicio, LibroMapper mapper, OpenLibraryClient openLibraryClient) {
        this.servicio = servicio;
        this.mapper = mapper;
        this.openLibraryClient = openLibraryClient;
    }

    // B1: GET /api/v1/libros con filtros opcionales y paginacion
    @GetMapping
    public ApiResponse<List<LibroResponse>> buscar(
            @RequestParam(required = false) String titulo,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Integer anioDesde,
            @PageableDefault(size = 20) Pageable paginacion) {
        Page<Libro> pagina = servicio.buscar(titulo, categoriaId, anioDesde, paginacion);
        List<LibroResponse> datos = pagina.getContent().stream().map(mapper::aRespuesta).toList();
        return ApiResponse.ok(datos, "Libros listados", PageMeta.de(pagina));
    }

    // GET /api/v1/libros/{id}
    @GetMapping("/{id}")
    public ApiResponse<LibroResponse> buscarPorId(@PathVariable Long id) {
        return ApiResponse.ok(mapper.aRespuesta(servicio.buscarPorId(id)), "Libro encontrado");
    }

    // B2: POST /api/v1/libros - solo ADMIN
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LibroResponse>> crear(@Valid @RequestBody LibroRequest solicitud) {
        Libro creado = servicio.crear(solicitud);
        LibroResponse cuerpo = mapper.aRespuesta(creado);
        return ResponseEntity
                .created(URI.create("/api/v1/libros/" + creado.getId()))
                .body(ApiResponse.ok(cuerpo, "Libro creado"));
    }

    // PUT /api/v1/libros/{id} - solo ADMIN
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<LibroResponse> actualizar(@PathVariable Long id,
                                                  @Valid @RequestBody LibroRequest solicitud) {
        Libro actualizado = servicio.actualizar(id, solicitud);
        return ApiResponse.ok(mapper.aRespuesta(actualizado), "Libro actualizado");
    }

    // DELETE /api/v1/libros/{id} - borrado logico, solo ADMIN
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        servicio.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    // B3: GET /api/v1/libros/{id}/enriquecido - combina libro local con Open Library
    @GetMapping("/{id}/enriquecido")
    public ApiResponse<LibroEnriquecidoResponse> enriquecido(@PathVariable Long id) {
        Libro libro = servicio.buscarPorId(id);
        OpenLibraryResponse externo = openLibraryClient.consultarPorIsbn(libro.getIsbn());
        LibroEnriquecidoResponse respuesta = new LibroEnriquecidoResponse(
                mapper.aRespuesta(libro),
                externo != null ? externo.title() : null,
                externo != null ? externo.urlPortada() : null,
                externo != null ? externo.number_of_pages() : null,
                externo != null ? externo.publish_date() : null);
        return ApiResponse.ok(respuesta, "Libro enriquecido");
    }
}
