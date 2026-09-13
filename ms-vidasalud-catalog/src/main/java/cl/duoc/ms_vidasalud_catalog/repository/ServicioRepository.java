package cl.duoc.ms_vidasalud_catalog.repository;

import cl.duoc.ms_vidasalud_catalog.model.Servicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServicioRepository extends JpaRepository<Servicio, Long> {
}
