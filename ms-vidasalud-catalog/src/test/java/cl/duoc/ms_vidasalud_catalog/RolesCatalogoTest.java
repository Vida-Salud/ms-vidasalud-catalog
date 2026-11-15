package cl.duoc.ms_vidasalud_catalog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RolesCatalogoTest {

	@Autowired
	MockMvc mockMvc;

	@Test
	void clienteNoCreaPrestaciones() throws Exception {
		mockMvc.perform(post("/api/catalog/services")
						.with(jwt().authorities(new SimpleGrantedAuthority("APPROLE_Cliente")))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\": \"Dental\", \"precio\": 15000}"))
				.andExpect(status().isForbidden());
	}
}
