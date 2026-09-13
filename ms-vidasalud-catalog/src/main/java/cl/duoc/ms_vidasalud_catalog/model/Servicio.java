package cl.duoc.ms_vidasalud_catalog.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "SERVICIO")
public class Servicio {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID")
	private Long id;

	@Column(name = "NOMBRE", nullable = false, length = 100)
	private String nombre;

	@Column(name = "DESCRIPCION", length = 500)
	private String descripcion;

	@Column(name = "PRECIO", nullable = false, precision = 10, scale = 2)
	private BigDecimal precio;

	@Column(name = "ACTIVO", nullable = false)
	private Boolean activo;

	@Column(name = "FECHA_CREACION", nullable = false, updatable = false)
	private LocalDateTime fechaCreacion;

	protected Servicio() {
	}

	public Servicio(String nombre, String descripcion, BigDecimal precio) {
		this.nombre = nombre;
		this.descripcion = descripcion;
		this.precio = precio;
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

	public String getDescripcion() {
		return descripcion;
	}

	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}

	public BigDecimal getPrecio() {
		return precio;
	}

	public void setPrecio(BigDecimal precio) {
		this.precio = precio;
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
		if (!(o instanceof Servicio otro)) {
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
		return "Servicio{id=%d, nombre='%s', precio=%s}".formatted(id, nombre, precio);
	}
}
