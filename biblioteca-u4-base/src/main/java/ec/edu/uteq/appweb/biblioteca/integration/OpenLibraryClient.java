package ec.edu.uteq.appweb.biblioteca.integration;

import ec.edu.uteq.appweb.biblioteca.config.CacheConfig;
import ec.edu.uteq.appweb.biblioteca.exception.ServicioExternoException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * Consume la API publica de Open Library para enriquecer los libros del catalogo
 * con metadatos externos (portada, paginas, fecha de publicacion).
 *
 * Patrones aplicados:
 *  - Cache-aside sobre Redis (namespace CacheConfig.CACHE_OPENLIBRARY, TTL 24 h).
 *  - Manejo diferenciado de fallos: 404 -> null (dato ausente), resto -> 502.
 *  - No se cachean los fallos (unless = "#result == null" excluye null del cache).
 */
@Component
public class OpenLibraryClient {

    private final RestClient restClient;

    public OpenLibraryClient(RestClient restClientExterno) {
        this.restClient = restClientExterno;
    }

    @Cacheable(value = CacheConfig.CACHE_OPENLIBRARY, key = "#isbn", unless = "#result == null")
    public OpenLibraryResponse consultarPorIsbn(String isbn) {
        try {
            return restClient.get()
                    .uri("/isbn/{isbn}.json", isbn)
                    .retrieve()
                    .onStatus(
                            estado -> estado.value() == 404,
                            (peticion, respuesta) -> { /* 404 no es error, devolvemos null */ })
                    .onStatus(
                            HttpStatusCode::isError,
                            (peticion, respuesta) -> {
                                throw new ServicioExternoException(
                                        "Open Library respondio con error HTTP " + respuesta.getStatusCode().value());
                            })
                    .body(OpenLibraryResponse.class);
        } catch (ServicioExternoException e) {
            throw e;
        } catch (ResourceAccessException e) {
            throw new ServicioExternoException("Timeout o fallo de red al consultar Open Library", e);
        } catch (Exception e) {
            throw new ServicioExternoException("Error inesperado al consultar Open Library", e);
        }
    }
}
