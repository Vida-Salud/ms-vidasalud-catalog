package cl.duoc.ms_vidasalud_catalog.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "BOX")
public class Box {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID")
	private Long id;

	@Column(name = "NOMBRE", nullable = false, length = 100)
	private String nombre;

	@ManyToOne
	@JoinColumn(name = "SERVICIO_ID", nullable = false, foreignKey = @ForeignKey(name = "FK_BOX_SERVICIO"))
	private Servicio servicio;

	@Column(name = "CAPACIDAD_DIARIA", nullable = false)
	private Integer capacidadDiaria;

	@Column(name = "ACTIVO", nullable = false)
	private Boolean activo;

	@Column(name = "FECHA_CREACION", nullable = false, updatable = false)
	private LocalDateTime fechaCreacion;

	protected Box() {
	}

	public Box(String nombre, Servicio servicio, Integer capacidadDiaria) {
		this.nombre = nombre;
		this.servicio = servicio;
		this.capacidadDiaria = capacidadDiaria;
		this.activo = true;
	}

	@PrePersist
	private void alCrear() {
		this.fechaCreacion = LocalDateTime.now();
		if (this.activo == null) {
			this.activo = true;
		}
	}

	public Long getId() {
		return id;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public Servicio getServicio() {
		return servicio;
	}

	public void setServicio(Servicio servicio) {
		this.servicio = servicio;
	}

	public Integer getCapacidadDiaria() {
		return capacidadDiaria;
	}

	public void setCapacidadDiaria(Integer capacidadDiaria) {
		this.capacidadDiaria = capacidadDiaria;
	}

	public Boolean getActivo() {
		return activo;
	}

	public void setActivo(Boolean activo) {
		this.activo = activo;
	}

	public LocalDateTime getFechaCreacion() {
		return fechaCreacion;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Box otro)) {
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
		return "Box{id=%d, nombre='%s', servicio=%s, capacidad=%d}".formatted(id, nombre, servicio, capacidadDiaria);
	}
}
