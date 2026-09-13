# Arquitectura EP1 — VidaSalud

---

## Visión general

```
┌─────────────────────────────────────────────────────────────────┐
│                      Azure AD (Entra ID)                        │
│  tenant-id: 245aec22-d743-4c13-a6e6-dce0a19bc1cf               │
└──────────────┬────────────────────────────────────────┬─────────┘
               │ MSAL OAuth2 / OpenID Connect           │
               │ (issuer, audience, claims)             │
     ┌─────────▼──────────────┐                    ┌────▼──────────┐
     │  Frontend Angular      │                    │   Access Token │
     │  (localhost:4200)      │◄───────Bearer JWT──┤ + Roles/Scopes │
     │  - Login UI            │                    └────────────────┘
     │  - Dashboard           │
     │  - Atenciones          │
     │  - Catálogo            │
     └──────────┬─────────────┘
                │ Authorization: Bearer <token>
     ┌─────────▼──────────────────────────────────────────────────┐
     │         BFF — Backend for Frontend (8089)                  │
     │         Spring Boot + Spring Security + Azure AD          │
     │  ┌────────────────────────────────────────────────────┐   │
     │  │ SecurityConfig:                                     │   │
     │  │ - Valida JWT (issuer, audience, firma, vigencia)  │   │
     │  │ - Autoriza por rol (APPROLE_Admin, etc)           │   │
     │  │ - CORS para frontend en 4200                       │   │
     │  │                                                     │   │
     │  │ Controllers:                                        │   │
     │  │ - AppointmentsController → proxy a 8090            │   │
     │  │ - CatalogController → proxy a 8091                 │   │
     │  │                                                     │   │
     │  │ RestClientConfig:                                  │   │
     │  │ - RestClient para appointments                     │   │
     │  │ - RestClient para catalog                          │   │
     │  └────────────────────────────────────────────────────┘   │
     └────────┬──────────────────────┬──────────────────────────┘
              │ internal             │ internal
     ┌────────▼──────────────┐    ┌──▼─────────────────────┐
     │ Appointments (8090)   │    │ Catalog (8091)         │
     │ Spring Boot + JPA     │    │ Spring Boot + JPA      │
     │ Oracle DB             │    │ Oracle DB              │
     │                       │    │                        │
     │ ┌─────────────────┐   │    │ ┌──────────────────┐   │
     │ │ Controllers:    │   │    │ │ Controllers:     │   │
     │ │ - /api/atenciones    │    │ │ - /api/catalog   │   │
     │ │                 │   │    │ │ /services        │   │
     │ │ Entities:       │   │    │ │ /boxes           │   │
     │ │ - Atencion      │   │    │ │ /cupos           │   │
     │ │ - EstadoAtencion│   │    │ │                  │   │
     │ │                 │   │    │ │ Entities:        │   │
     │ │ Service:        │   │    │ │ - Servicio       │   │
     │ │ - Validaciones  │   │    │ │ - Box            │   │
     │ │ - Transiciones  │   │    │ │ - CupoDisponible │   │
     │ │                 │   │    │ │                  │   │
     │ │ SecurityConfig: │   │    │ │ Service:         │   │
     │ │ - JWT validator │   │    │ │ - CRUD           │   │
     │ └─────────────────┘   │    │ │ - Validaciones   │   │
     └───────────────────────┘    └──────────────────────┘
```

---

## Flujo de autenticación y autorización

### 1. Login (Usuario navega a http://localhost:4200)

```
User clicks "Login con Microsoft"
        ↓
Angular MsalService inicia OAuth2 flow
        ↓
Redirige a: https://login.microsoftonline.com/<tenant-id>/oauth2/v2.0/authorize?
  client_id=<client-id>&
  scope=<scopes>&
  response_type=code&
  redirect_uri=http://localhost:4200/auth/callback
        ↓
Usuario se autentica en Azure AD
        ↓
Azure AD redirige a callback con authorization code
        ↓
MSAL intercambia code → access token + ID token
        ↓
access_token se almacena en localStorage/sessionStorage
        ↓
Frontend obtiene claims:
  - oid (object ID)
  - upn (user principal name)
  - roles ([] → APPROLE_Admin, APPROLE_Operador, etc)
  - scp (scopes)
  - exp (expiration)
```

### 2. Request al API (Frontend → BFF)

```
Frontend: GET /api/appointments
Header: Authorization: Bearer eyJ0eXA...
        ↓
BFF SecurityConfig:
  1. Extrae token del header
  2. Valida JWT (firma RSASSA-PKCS1-v1_5 con clave pública de Azure AD)
  3. Verifica claims:
     - iss (issuer) = https://login.microsoftonline.com/<tenant-id>/v2.0
     - aud (audience) = api://<client-id>
     - exp (expiration) > ahora
  4. Lee roles del token → mapea a Spring Authority (APPROLE_Admin, etc)
        ↓
BFF @RequestMatchers autorizan:
  GET /api/appointments → hasAnyAuthority("APPROLE_Admin", "APPROLE_Operador", "APPROLE_Cliente")
        ↓
  Si tiene rol → permite pasar
  Si no → responde 403 Forbidden
        ↓
BFF Controller (AppointmentsController) recibe request
        ↓
RestClient llama internamente a http://localhost:8090/api/atenciones
Header: Authorization: Bearer eyJ0eXA...
        ↓
Appointments SecurityConfig valida JWT nuevamente
        ↓
Appointments Controller procesa, responde
        ↓
BFF reenvía response exacta (status, content-type, body)
        ↓
Frontend recibe y renderiza
```

---

## Componentes por responsabilidad

### Frontend (Angular + MSAL)
**Archivo:** frontend-vidasalud/

**Responsabilidades:**
- Login/logout con Azure AD
- Almacenar y renovar tokens
- Adjuntar JWT en headers (via MsalInterceptor)
- Proteger rutas con guards
- Leer roles/scopes del token
- Mostrar UI según rol

**Tecnología:**
- Angular 16+
- @azure/msal-browser
- @azure/msal-angular
- RxJS, HttpClient

---

### BFF (Backend For Frontend)
**Archivo:** ms-vidasalud-bff/

**Responsabilidades:**
- Validar JWT de Azure AD
- Autorizar por rol
- Actuar como proxy/agregador
- Manejar CORS
- Traducir entre frontend y microservicios

**Componentes:**
```
SecurityConfig.java
├── securityFilterChain()
│   ├── csrf: disabled (stateless API)
│   ├── cors: permite origen localhost:4200
│   ├── authorizeHttpRequests
│   │   ├── /api/appointments/** → varios roles
│   │   ├── /api/catalog/** → Admin, Operador
│   │   ├── /api/report/** → Admin
│   │   └── /api/audit/** → Admin, Auditor
│   └── oauth2ResourceServer: Azure AD JWT
└── corsConfigurationSource()
    └── Allow-Origin: http://localhost:4200

RestClientConfig.java
├── appointmentsRestClient() → http://localhost:8090
└── catalogRestClient() → http://localhost:8091

AppointmentsController.java
├── GET /api/appointments → GET /api/atenciones
├── POST /api/appointments → POST /api/atenciones
├── GET /api/appointments/{id} → GET /api/atenciones/{id}
├── PUT /api/appointments/{id} → PUT /api/atenciones/{id}
└── PUT /api/appointments/{id}/status → PUT /api/atenciones/{id}/estado

CatalogController.java
├── GET /api/catalog/services → GET /api/catalog/services
├── POST /api/catalog/services → POST /api/catalog/services
├── GET /api/catalog/boxes → GET /api/catalog/boxes
├── POST /api/catalog/boxes → POST /api/catalog/boxes
├── GET /api/catalog/cupos → GET /api/catalog/cupos
└── PUT /api/catalog/cupos/{id} → PUT /api/catalog/cupos/{id}
```

**Puertos:**
- Escucha: 8089
- Llama: appointments (8090), catalog (8091)

**Variables de entorno:**
```
APPOINTMENTS_URL=http://localhost:8090 (default)
CATALOG_URL=http://localhost:8091 (default)
AZURE_TENANT_ID=245aec22-d743-4c13-a6e6-dce0a19bc1cf
AZURE_CLIENT_ID=fd189494-2ea3-4c54-91ed-5b5ddd644163
AZURE_APP_ID_URI=api://fd189494-2ea3-4c54-91ed-5b5ddd644163
```

---

### Appointments Microservice
**Archivo:** ms-vidasalud-appointments/

**Responsabilidades:**
- CRUD de atenciones médicas
- Gestionar estados (SOLICITADA, CONFIRMADA, EN_ESPERA, EN_ATENCIÓN, CERRADA, CANCELADA)
- Validar transiciones de estado
- Validar JWT

**Entidades:**
```
Atencion
├── id (PK, IDENTITY)
├── paciente (String, 100)
├── servicio (String, 100)
├── box (String, 50, nullable)
├── fechaHora (LocalDateTime)
├── estado (Enum: EstadoAtencion)
└── fechaCreacion (LocalDateTime, @PrePersist)

EstadoAtencion (Enum)
├── SOLICITADA
├── CONFIRMADA
├── EN_ESPERA
├── EN_ATENCIÓN
├── CERRADA
└── CANCELADA
```

**Reglas de negocio:**
- No se puede agendar hacia el pasado
- Estados editables solo en SOLICITADA, CONFIRMADA, EN_ESPERA
- Transiciones válidas definidas en EnumMap
- Auditoría: fechaCreacion asignada automáticamente por @PrePersist

**Endpoints:**
```
POST   /api/atenciones                     → 201 Created + Location
GET    /api/atenciones                     → 200 OK (lista)
GET    /api/atenciones/{id}                → 200 OK o 404
PUT    /api/atenciones/{id}                → 200 OK (actualiza)
PUT    /api/atenciones/{id}/estado         → 200 OK (transición)
```

**Puertos:**
- Escucha: 8090
- BD: Oracle (localhost:1522/XEPDB1)

**Variables de entorno:**
```
DB_HOST=localhost
DB_PORT=1522
DB_SERVICE=XEPDB1
DB_USERNAME=vidasalud
DB_PASSWORD=admin123
AZURE_TENANT_ID=...
AZURE_CLIENT_ID=...
AZURE_APP_ID_URI=...
```

---

### Catalog Microservice (Nuevo)
**Archivo:** ms-vidasalud-catalog/

**Responsabilidades:**
- CRUD de servicios (prestaciones)
- CRUD de boxes (espacios de atención)
- Gestionar cupos diarios disponibles
- Validar JWT

**Entidades:**
```
Servicio
├── id (PK, IDENTITY)
├── nombre (String, 100)
├── descripcion (String, 500, nullable)
├── precio (BigDecimal, 10,2)
├── activo (Boolean)
└── fechaCreacion (LocalDateTime, @PrePersist)

Box
├── id (PK, IDENTITY)
├── nombre (String, 100)
├── servicio (FK → Servicio)
├── capacidadDiaria (Integer)
├── activo (Boolean)
└── fechaCreacion (LocalDateTime, @PrePersist)

CupoDisponible
├── id (PK, IDENTITY)
├── box (FK → Box)
├── fecha (LocalDate)
├── cuposDisponibles (Integer)
└── UK(box_id, fecha)
```

**Reglas de negocio:**
- Al crear box: inicializar 30 días de cupos (cantidad = capacidadDiaria)
- Cupos únicos por (box, fecha)
- Decrementar cupo al confirmar atención (future integration)
- String normalizados: trim() + null si vacío

**Endpoints:**
```
POST   /api/catalog/services               → 201 Created + Location
GET    /api/catalog/services               → 200 OK (lista)
GET    /api/catalog/services/{id}          → 200 OK o 404
PUT    /api/catalog/services/{id}          → 200 OK

POST   /api/catalog/boxes                  → 201 Created + Location
GET    /api/catalog/boxes                  → 200 OK (lista)
GET    /api/catalog/boxes?servicioId={id}  → 200 OK (filtrado)
GET    /api/catalog/boxes/{id}             → 200 OK o 404

GET    /api/catalog/cupos?fecha={fecha}    → 200 OK (hoy por defecto)
GET    /api/catalog/cupos/box/{boxId}/fecha/{fecha} → 200 OK o 404
PUT    /api/catalog/cupos/{id}             → 200 OK
```

**Puertos:**
- Escucha: 8091
- BD: Oracle (localhost:1522/XEPDB1)

---

## Flujo de datos (ejemplo: crear atención)

```
User en Frontend:
  Input: paciente="Juan", servicio="Odontología", fechaHora="2026-09-15T10:00"
  Button: POST /api/appointments
         ↓
    Header: Authorization: Bearer <JWT>
    Body: { "paciente": "Juan", ... }

         ↓ [CORS permitido en BFF]

BFF (8089):
  1. SecurityConfig extrae JWT
  2. Valida issuer, audience, firma, exp
  3. Lee rol del token (APPROLE_Cliente)
  4. @RequestMatchers: POST /api/appointments
     → hasAnyAuthority("APPROLE_Admin", "APPROLE_Operador", "APPROLE_Cliente") ✓
  5. AppointmentsController.crear() ejecuta
  6. RestClient llama: POST http://localhost:8090/api/atenciones
     Header: Authorization: Bearer <JWT>
     Body: { "paciente": "Juan", ... }

         ↓ [Internal call, no CORS]

Appointments (8090):
  1. SecurityConfig valida JWT nuevamente
  2. AtencionController.crear() ejecuta
  3. AtencionService.crear() valida:
     - fechaHora es futura ✓
  4. Crea Atencion, guarda en BD
  5. @PrePersist asigna fechaCreacion
  6. Responde: 201 Created, Location header, body con Atencion

         ↓

BFF recibe respuesta
  - Status: 201
  - Content-Type: application/json
  - Body: { "id": 1, "paciente": "Juan", "estado": "SOLICITADA", ... }

         ↓

BFF reenvía exacto al Frontend
  201 Created, mismo body

         ↓

Frontend (4200):
  - Almacena respuesta
  - Renderiza: "Atención creada con éxito, ID: 1"
  - Redirige a vista de detalles
```

---

## Decisiones de diseño

| Aspecto | Decisión | Razón |
|---|---|---|
| **JWT validation en BFF + microservicios** | Doble validación | Defensa en profundidad. BFF controla acceso, pero microservicios también validan. |
| **RestClient sin token en llamadas internas** | El BFF NO pasa el JWT al microservicio internamente | Las llamadas internas son dentro de la VPC/localhost. Tokens no necesarios. |
| **Roles en Spring Authority** | Mapea APPROLE_* de Azure AD a Spring ROLE_* | Unifica verificación en @RequestMatchers. |
| **String normalizados** | trim() + null si vacío | Evita strings con solo espacios o inconsistencias. |
| **@PrePersist para auditoria** | Fecha de creación automática | No depende del cliente, inmutable después. |
| **Enum para estados de Atencion** | EnumType.STRING, no ORDINAL | Reordenar enum no corrompe BD. |
| **Única tabla de Atencion por BD** | No replicar en catalog | Single Source of Truth. Catalog solo consulta cupos. |
| **Cupos inicializados a 30 días** | Hardcoded en servicio | Configurable luego. Para EP1, simple. |

---

## Seguridad

### Autenticación (Indicador 2)
- ✅ Azure AD como IdP (issuer)
- ✅ JWT firmado por Azure (RSASSA-PKCS1-v1_5 con JWK)
- ✅ Validación de issuer, audience, firma, vigencia en BFF
- ✅ Validación de issuer, firma en microservicios

### Autorización (Indicador 2)
- ✅ Roles en JWT claims (role, scp)
- ✅ BFF autoriza por rol (@RequestMatchers)
- ✅ Microservicios no publican rutas sensibles sin autenticación
- ✅ 401 sin token, 403 sin rol

### CORS (Seguridad)
- ✅ BFF permite solo http://localhost:4200
- ✅ No permite credenciales en preflight
- ✅ Headers blanqueados (*)

### CSRF
- ✅ Deshabilitado (API stateless, token en header, no cookie)

### Inyección SQL / XSS
- ✅ Spring Data JPA con prepared statements
- ✅ JSON serialization automática (no HTML rendering en controller)
- ✅ Input validation en DTOs (@NotBlank, @Positive)

---

## Testing (checklist de indicadores)

### Indicador 1: MSAL en Angular (60%)

- [ ] MSAL inicializado con credenciales correctas
- [ ] Login redirige a Azure AD y regresa con token
- [ ] Token almacenado en sessionStorage
- [ ] MsalInterceptor adjunta Authorization header
- [ ] Logout limpia token
- [ ] MsalGuard protege rutas (redirige a login si no autenticado)
- [ ] Roles se leen del token (claims)
- [ ] Scopes solicitados incluyen api://... (app-id-uri)

**Comandos de validación en Chrome DevTools:**
```javascript
// Verify token stored
sessionStorage.getItem('msal.idtoken');

// Verify interceptor
// (Network tab → request → Headers → Authorization)

// Verify claims
const token = sessionStorage.getItem('msal.idtoken');
const decoded = jwt_decode(token);
console.log(decoded.roles, decoded.scp);
```

### Indicador 2: JWT en BFF (40%)

- [ ] BFF recibe Authorization header
- [ ] BFF extrae y valida JWT
- [ ] Valida issuer (https://login.microsoftonline.com/<tenant>/v2.0)
- [ ] Valida audience (api://<client-id>)
- [ ] Valida firma (RSASSA-PKCS1-v1_5 con JWK de Azure AD)
- [ ] Valida vigencia (exp)
- [ ] 401 Unauthorized sin token
- [ ] 401 Unauthorized con token expirado
- [ ] 401 Unauthorized con token de otro issuer
- [ ] 403 Forbidden con rol insuficiente
- [ ] 200 OK con JWT válido y rol correcto

**Validar en logs de BFF:**
```
[main] AadResourceServerConfiguration: JWT decoding configurado
[http-nio-8089] Authorization: validando issuer=...
[http-nio-8089] Authorization: validando audience=...
[http-nio-8089] Authorization: scope=SCOPE_api...
[http-nio-8089] Authorization: authority=APPROLE_Admin
```

---

## Próximos pasos (después de EP1)

1. **RabbitMQ**: integración de eventos (atención confirmada → notificación)
2. **Kafka**: streaming de auditoría y reportería
3. **ms-vidasalud-notify**: consumidor RabbitMQ
4. **ms-vidasalud-report**: agregaciones Kafka
5. **ms-vidasalud-audit**: timeline Kafka
6. **Docker Compose**: orquestación local
7. **AWS EC2**: despliegue a producción
8. **API Gateway**: AWS o Nginx como punto único de entrada

---

**Documento generado:** 2026-09-13  
**Versión:** EP1 (Evaluación Parcial 1)  
**Componentes:** Frontend + BFF + Appointments + Catalog  
**Stack:** Angular + Spring Boot 4.0.8 + Java 17 + Oracle + Azure AD
