package ec.edu.uteq.appweb.biblioteca.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ec.edu.uteq.appweb.biblioteca.BaseIntegracionTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

class LibroControllerIT extends BaseIntegracionTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "LECTOR")
    @DisplayName("GET /api/v1/libros responde 200 y el cuerpo trae las cinco claves del envoltorio, con meta.page y meta.size correctos")
    void listarLibrosDevuelveEnvoltorio() throws Exception {
        mockMvc.perform(get("/api/v1/libros").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.meta").exists())
                .andExpect(jsonPath("$.meta.page").isNumber())
                .andExpect(jsonPath("$.meta.size").value(5));
    }

    @Test
    @WithMockUser(roles = "LECTOR")
    @DisplayName("GET /api/v1/libros/999999 responde 404 y el cuerpo trae title, status y detail del formato Problem Details")
    void libroInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/api/v1/libros/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/v1/libros con titulo vacio responde 400 y el arreglo errors no esta vacio")
    void crearLibroSinTituloDevuelve400() throws Exception {
        String payload = "{ \"isbn\": \"1234567890\", \"titulo\": \"\", \"anioPublicacion\": 2020, \"ejemplaresTotales\": 5, \"autorId\": 1, \"editorialId\": 1, \"categoriaId\": 1 }";
            
        mockMvc.perform(post("/api/v1/libros")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }
}
