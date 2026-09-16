# VidaSalud - Microservicio de Catálogo

Microservicio encargado de administrar el catálogo de servicios médicos, boxes de atención y disponibilidad de cupos de la plataforma **VidaSalud**.

Forma parte de la arquitectura de microservicios del proyecto y es consumido principalmente a través de `ms-vidasalud-bff`.

## Funcionalidades

El microservicio permite administrar tres elementos principales:

- **Servicios médicos:** creación, consulta y actualización de prestaciones.
- **Boxes:** espacios físicos asociados a un servicio médico.
- **Cupos disponibles:** disponibilidad diaria de cada box.

Cuando se crea un nuevo box, el sistema genera automáticamente disponibilidad para los próximos **30 días**, utilizando la capacidad diaria configurada.

## Tecnologías

- Java 17+
- Spring Boot 4.0.8
- Spring Web MVC
- Spring Data JPA
- Spring Security
- OAuth2 Resource Server
- Microsoft Entra ID / Azure AD
- Jakarta Validation
- Oracle Database
- Maven
- Docker

> Las imágenes Docker utilizan Eclipse Temurin JDK/JRE 21.

## Arquitectura

El proyecto utiliza una arquitectura por capas:

```text
Controller
    │
    ▼
Service
    │
    ▼
Repository
    │
    ▼
Oracle Database
```

Estructura principal:

```text
ms-vidasalud-catalog/
├── config/
│   └── SecurityConfig.java
├── controller/
│   ├── CatalogController.java
│   └── ManejadorErrores.java
├── dto/
├── model/
├── repository/
├── service/
│   └── CatalogService.java
└── resources/
    └── application.yaml
```

## Puerto

El servicio se ejecuta por defecto en:

```text
8091
```

Base URL local:

```text
http://localhost:8091
```

## Seguridad

Todos los endpoints requieren un **JWT válido emitido por Microsoft Entra ID / Azure AD**.

El token debe enviarse mediante:

```http
Authorization: Bearer <access_token>
```

Aunque este microservicio valida la autenticación, la autorización por roles de la aplicación se centraliza principalmente en el **BFF**.

## Variables de entorno

| Variable | Descripción | Valor local por defecto |
|---|---|---|
| `AZURE_TENANT_ID` | Tenant de Microsoft Entra ID | Configuración local |
| `AZURE_CLIENT_ID` | Client ID de la API | Configuración local |
| `AZURE_APP_ID_URI` | Application ID URI | `api://<client-id>` |
| `DB_HOST` | Host de Oracle | `localhost` |
| `DB_PORT` | Puerto de Oracle | `1522` |
| `DB_SERVICE` | Service Name de Oracle | `XEPDB1` |
| `DB_USERNAME` | Usuario Oracle | `vidasalud` |
| `DB_PASSWORD` | Contraseña Oracle | Configuración local |

Para ambientes productivos se recomienda definir estas variables externamente y no almacenar credenciales en el repositorio.

## Endpoints

### Servicios médicos

```http
POST /api/catalog/services
GET  /api/catalog/services
GET  /api/catalog/services/{id}
PUT  /api/catalog/services/{id}
```

Ejemplo para crear un servicio:

```json
{
  "nombre": "Consulta Medicina General",
  "descripcion": "Consulta médica general",
  "precio": 25000
}
```

### Boxes

```http
POST /api/catalog/boxes
GET  /api/catalog/boxes
GET  /api/catalog/boxes?servicioId={id}
GET  /api/catalog/boxes/{id}
```

Ejemplo:

```json
{
  "nombre": "Box 01",
  "servicioId": 1,
  "capacidadDiaria": 10
}
```

Al crear un box se inicializan automáticamente sus cupos para los próximos 30 días.

### Cupos

```http
GET /api/catalog/cupos
GET /api/catalog/cupos?fecha=2026-09-20
GET /api/catalog/cupos/box/{boxId}/fecha/{fecha}
PUT /api/catalog/cupos/{id}
```

Ejemplo para modificar disponibilidad:

```json
{
  "cuposDisponibles": 8
}
```

Los cupos disponibles nunca pueden ser negativos.

## Ejecución local

El proyecto Spring Boot se encuentra dentro de la carpeta:

```text
ms-vidasalud-catalog/
```

Ingresar a ella:

```bash
cd ms-vidasalud-catalog
```

Compilar:

```bash
mvn clean package
```

Ejecutar:

```bash
mvn spring-boot:run
```

## Docker

Construir imagen:

```bash
docker build -t vidasalud-catalog .
```

Ejecutar:

```bash
docker run -p 8091:8091 vidasalud-catalog
```

En un ambiente real se deben proporcionar también las variables de Oracle y Microsoft Entra ID.

## Base de datos

El servicio utiliza **Oracle Database** mediante Spring Data JPA.

Actualmente Hibernate está configurado con:

```yaml
ddl-auto: update
```

Las principales entidades son:

```text
Servicio
Box
CupoDisponible
```

Relación general:

```text
Servicio
   │
   └── Box
         │
         └── CupoDisponible
```

## Integración con VidaSalud

El flujo esperado es:

```text
Frontend VidaSalud
        │
        ▼
ms-vidasalud-bff
        │
        ▼
ms-vidasalud-catalog
        │
        ▼
Oracle Database
```

El frontend no debería consumir directamente este microservicio; las solicitudes se canalizan a través del BFF.

## Proyecto VidaSalud

Este repositorio forma parte de la solución **VidaSalud**, desarrollada utilizando una arquitectura basada en microservicios, autenticación con Microsoft Entra ID y separación entre frontend, BFF y servicios de dominio.
