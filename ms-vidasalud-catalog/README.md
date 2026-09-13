# ms-vidasalud-catalog

Microservicio de catálogo de prestaciones, boxes y cupos para la plataforma VidaSalud.

## Dominio

- **Servicios (prestaciones)**: CRUD de servicios médicos con precio
- **Boxes**: Espacios de atención asignados a un servicio con capacidad diaria
- **Cupos**: Inventario diario de cupos disponibles por box

## Stack

- Spring Boot 4.0.8
- Java 21
- Oracle Database
- Spring Data JPA
- Azure AD (OAuth2 Resource Server)
- Jakarta Validation

## Compilar

```bash
mvn clean package
```

## Ejecutar

```bash
mvn spring-boot:run
```

## Variables de entorno

- `AZURE_TENANT_ID`: Azure AD tenant
- `AZURE_CLIENT_ID`: Application ID
- `DB_HOST`: Oracle host (default: localhost)
- `DB_PORT`: Oracle port (default: 1522)
- `DB_SERVICE`: Oracle service name (default: XEPDB1)
- `DB_USERNAME`: Oracle user (default: vidasalud)
- `DB_PASSWORD`: Oracle password (default: admin123)

## Endpoints

### Servicios

```
POST   /api/catalog/services              → crear servicio
GET    /api/catalog/services              → listar servicios
GET    /api/catalog/services/{id}         → obtener servicio
PUT    /api/catalog/services/{id}         → actualizar servicio
```

### Boxes

```
POST   /api/catalog/boxes                 → crear box
GET    /api/catalog/boxes                 → listar boxes
GET    /api/catalog/boxes?servicioId=...  → boxes de un servicio
GET    /api/catalog/boxes/{id}            → obtener box
```

### Cupos

```
GET    /api/catalog/cupos?fecha=...       → cupos disponibles (hoy por defecto)
GET    /api/catalog/cupos/box/{boxId}/fecha/{fecha} → cupo específico
PUT    /api/catalog/cupos/{id}            → ajustar cupo
```

## Arquitectura

Patrón por capas:
- **Controller**: HTTP, validación @Valid, mapeo status codes
- **Service**: lógica de negocio, validaciones
- **Repository**: Spring Data JPA
- **Model**: entidades JPA con @PrePersist para auditoría
- **DTO**: transferencia de datos

## Seguridad

Requiere JWT válido de Azure AD en `Authorization: Bearer <token>`.

All endpoints are protected unless stated otherwise.
