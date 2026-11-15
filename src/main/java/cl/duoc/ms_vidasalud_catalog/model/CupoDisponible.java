package cl.duoc.ms_vidasalud_catalog.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "CUPO_DISPONIBLE", uniqueConstraints = {
		@UniqueConstraint(columnNames = {"BOX_ID", "FECHA"}, name = "UK_BOX_FECHA")
})
public class CupoDisponible {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID")
	private Long id;

	@ManyToOne
	@JoinColumn(name = "BOX_ID", nullable = false, foreignKey = @ForeignKey(name = "FK_CUPO_BOX"))
	private Box box;

	@Column(name = "FECHA", nullable = false)
	private LocalDate fecha;

	@Column(name = "CUPOS_DISPONIBLES", nullable = false)
	private Integer cuposDisponibles;

	protected CupoDisponible() {
	}

	public CupoDisponible(Box box, LocalDate fecha, Integer cuposDisponibles) {
		this.box = box;
		this.fecha = fecha;
		this.cuposDisponibles = cuposDisponibles;
	}

	public Long getId() {
		return id;
	}

	public Box getBox() {
		return box;
	}

	public void setBox(Box box) {
		this.box = box;
	}

	public LocalDate getFecha() {
		return fecha;
	}

	public void setFecha(LocalDate fecha) {
		this.fecha = fecha;
	}

	public Integer getCuposDisponibles() {
		return cuposDisponibles;
	}

	public void setCuposDisponibles(Integer cuposDisponibles) {
		this.cuposDisponibles = cuposDisponibles;
	}

	public void decrementarCupo() {
		if (this.cuposDisponibles > 0) {
			this.cuposDisponibles--;
		}
	}

	public boolean tieneCupoDisponible() {
		return this.cuposDisponibles > 0;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof CupoDisponible otro)) {
			return false;
		}
		return id != null && id.equals(otro.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public String toString() {
		return "CupoDisponible{id=%d, box=%d, fecha=%s, cupos=%d}".formatted(id, box.getId(), fecha, cuposDisponibles);
	}
}
