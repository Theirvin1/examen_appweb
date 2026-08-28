package ec.edu.uteq.appweb.biblioteca.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Cliente HTTP del lado del servidor con tiempos de espera acotados.
 *
 * Los timeouts NO son opcionales: sin ellos, una API externa lenta bloquea los
 * hilos del servidor y termina tumbando la aplicacion propia. Es el fallo en
 * cascada que describe Nygard en Release It!.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient restClientExterno(RestClient.Builder builder,
                                        @Value("${app.api-externa.base-url}") String baseUrl,
                                        @Value("${app.api-externa.connect-timeout-ms:3000}") long conexionMs,
                                        @Value("${app.api-externa.read-timeout-ms:5000}") long lecturaMs) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofMillis(conexionMs));
        fabrica.setReadTimeout(Duration.ofMillis(lecturaMs));
        return builder
                .baseUrl(baseUrl)
                .requestFactory(fabrica)
                .build();
    }
}
