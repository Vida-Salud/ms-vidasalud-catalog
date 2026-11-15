package cl.duoc.ms_vidasalud_catalog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class RelojConfig {

	@Bean
	Clock clock() {
		return Clock.system(ZoneId.of("America/Santiago"));
	}
}
