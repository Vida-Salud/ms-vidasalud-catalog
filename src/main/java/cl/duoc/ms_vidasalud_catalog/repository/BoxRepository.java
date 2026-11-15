package cl.duoc.ms_vidasalud_catalog.repository;

import cl.duoc.ms_vidasalud_catalog.model.Box;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoxRepository extends JpaRepository<Box, Long> {
	List<Box> findByServicioId(Long servicioId);
}
