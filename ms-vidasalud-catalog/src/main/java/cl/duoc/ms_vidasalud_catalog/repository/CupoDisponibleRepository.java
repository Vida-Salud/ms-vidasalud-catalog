package cl.duoc.ms_vidasalud_catalog.repository;

import cl.duoc.ms_vidasalud_catalog.model.CupoDisponible;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CupoDisponibleRepository extends JpaRepository<CupoDisponible, Long> {
	Optional<CupoDisponible> findByBoxIdAndFecha(Long boxId, LocalDate fecha);

	List<CupoDisponible> findByBoxId(Long boxId);

	List<CupoDisponible> findByFecha(LocalDate fecha);
}
