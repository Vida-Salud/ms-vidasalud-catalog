# VidaSalud — Evaluación Parcial 1 (EP1)

**Plataforma para gestión de atenciones en centros de salud**

---

## 📋 Estado de entrega

| Componente | Responsable | Estado | Repositorio |
|---|---|---|---|
| **Frontend Angular + MSAL** | (Tu compañero/a) | ✅ Completo | `frontend-vidasalud` |
| **BFF (JWT Validator)** | Pablo | ✅ Completo | `ms-vidasalud-bff` |
| **Appointments (CRUD Atenciones)** | (Compañero) | ✅ Compilado | `ms-vidasalud-appointments` |
| **Catalog (CRUD Servicios/Boxes/Cupos)** | Tú | ✅ Compilado | `ms-vidasalud-catalog` |

---

## 🏗️ Componentes nuevos en esta entrega

### ms-vidasalud-catalog

Microservicio de gestión del catálogo: servicios médicos, boxes de atención y cupos disponibles.

**Ubicación:** `ms-vidasalud-catalog/`

**Responsabilidades:**
- ✅ CRUD servicios (prestaciones con precio)
- ✅ CRUD boxes (espacios de atención)
- ✅ Gestión cupos disponibles por día
- ✅ Validación JWT con Azure AD
- ✅ Autorización por rol

**Entidades:**
- `Servicio`: nombre, descripción, precio, activo
- `Box`: nombre, servicio, capacidad diaria, activo
- `CupoDisponible`: box, fecha, cupos (auto-inicializado a 30 días)

**Endpoints:**
```
POST   /api/catalog/services
GET    /api/catalog/services
GET    /api/catalog/services/{id}
PUT    /api/catalog/services/{id}

POST   /api/catalog/boxes
GET    /api/catalog/boxes
GET    /api/catalog/boxes/{id}
GET    /api/catalog/boxes?servicioId={id}

GET    /api/catalog/cupos
GET    /api/catalog/cupos/box/{boxId}/fecha/{fecha}
PUT    /api/catalog/cupos/{id}
```

**Stack:**
- Spring Boot 4.0.8, Java 17
- Spring Data JPA, Oracle
- Azure AD OAuth2 Resource Server
- Jakarta Validation

**Compilación:**
```bash
cd ms-vidasalud-catalog
mvn clean package
# Output: target/ms-vidasalud-catalog-0.0.1-SNAPSHOT.jar
```

**Ejecución:**
```bash
mvn spring-boot:run
# Escucha en puerto 8091
```

**Variables de entorno:**
```
DB_HOST=localhost
DB_PORT=1522
DB_SERVICE=XEPDB1
DB_USERNAME=vidasalud
DB_PASSWORD=admin123
AZURE_TENANT_ID=245aec22-d743-4c13-a6e6-dce0a19bc1cf
AZURE_CLIENT_ID=fd189494-2ea3-4c54-91ed-5b5ddd644163
AZURE_APP_ID_URI=api://fd189494-2ea3-4c54-91ed-5b5ddd644163
```

---

### BFF — Actualización con catalog

Se agregó soporte para catalog al BFF existente de Pablo.

**Cambios:**
- ✅ Agregado `RestClient catalogRestClient` en `RestClientConfig`
- ✅ Nuevo `CatalogController` proxy hacia ms-vidasalud-catalog
- ✅ Actualizado `application.yaml` con `catalog-url`
- ✅ SecurityConfig ya incluye reglas de autorización para catalog

**Autorización en BFF:**
```
GET  /api/catalog/** → APPROLE_Admin, APPROLE_Operador
POST /api/catalog/** → APPROLE_Admin
PUT  /api/catalog/** → APPROLE_Admin
```

---

## 🔄 Flujo de autenticación (Indicador 1 — 60%)

1. **Usuario abre Frontend** (`http://localhost:4200`)
2. **Click "Login con Microsoft"**
3. **MSAL redirige a Azure AD** (OAuth2 / OpenID Connect)
4. **Usuario se autentica**
5. **Azure AD devuelve JWT** + claims (roles, scopes)
6. **MSAL almacena token** en sessionStorage
7. **MsalInterceptor adjunta** `Authorization: Bearer <token>` en cada request
8. **Frontend accede a BFF** con JWT

---

## 🔐 Validación JWT en BFF (Indicador 2 — 40%)

1. **BFF recibe request** con `Authorization: Bearer <token>`
2. **SecurityConfig extrae JWT**
3. **Valida claims:**
   - ✅ `iss` (issuer) = `https://login.microsoftonline.com/<tenant>/v2.0`
   - ✅ `aud` (audience) = `api://<client-id>`
   - ✅ Firma RSASSA-PKCS1-v1_5 (JWK de Azure AD)
   - ✅ `exp` (expiration) < ahora → rechaza
4. **Mapea roles** del token a Spring Authority (`APPROLE_Admin`, etc)
5. **@RequestMatchers autoriza** según endpoint:
   - GET /api/appointments → Admin, Operador, Cliente
   - POST /api/appointments → Admin, Operador, Cliente
   - PUT /api/appointments/{id}/status → Admin, Operador
   - POST /api/catalog/** → Admin
   - PUT /api/catalog/** → Admin
6. **Si válido:** permite request, llama microservicio
7. **Si inválido:** responde 401/403 con mensaje de error

---

## 🧪 Pruebas — Ver documentación completa

**Archivo:** `PRUEBA_FLUJO_EP1.md`

**Resumen:**
- ✅ Iniciación de servicios (3 terminales)
- ✅ Pruebas curl sin MSAL (rápido)
- ✅ Prueba con MSAL en navegador (completa)
- ✅ Validaciones críticas (checklist)
- ✅ Troubleshooting

**Validar indicadores:**
| Indicador | Test | Ubicación |
|---|---|---|
| MSAL login/logout | Chrome DevTools Console | PRUEBA_FLUJO_EP1.md §5 |
| JWT obtenido | sessionStorage verification | PRUEBA_FLUJO_EP1.md §4.2 |
| JWT adjuntado | Network tab Authorization | PRUEBA_FLUJO_EP1.md §4.2 |
| BFF valida | Logs: "Authorization: validando..." | PRUEBA_FLUJO_EP1.md §7 |
| Autorización por rol | 403 Forbidden test | PRUEBA_FLUJO_EP1.md §6 |

---

## 📐 Arquitectura — Ver documentación completa

**Archivo:** `ARQUITECTURA_EP1.md`

**Contenidos:**
- Diagrama de flujo completo
- Responsabilidades por componente
- Flujo detallado de autenticación/autorización
- Entidades y relaciones
- Endpoints y reglas de negocio
- Decisiones de diseño
- Seguridad (autenticación, autorización, CORS, CSRF)
- Checklists de validación
- Próximos pasos

---

## 📦 Compilación y despliegue

### Compilar todos los servicios

```bash
# Appointments
cd ms-vidasalud-appointments
mvn clean package -DskipTests
cd ..

# Catalog
cd ms-vidasalud-catalog
mvn clean package -DskipTests
cd ..

# BFF
cd ms-vidasalud-bff
mvn clean package -DskipTests
cd ..
```

**Resultado:** 3 JARs en respectivas carpetas `target/`

### Ejecutar localmente (3 terminales)

**Terminal 1 — Appointments (8090):**
```bash
cd ms-vidasalud-appointments
mvn spring-boot:run
```

**Terminal 2 — Catalog (8091):**
```bash
cd ms-vidasalud-catalog
mvn spring-boot:run
```

**Terminal 3 — BFF (8089):**
```bash
cd ms-vidasalud-bff
mvn spring-boot:run
```

**Terminal 4 — Frontend (4200):**
```bash
cd frontend-vidasalud
npm install
ng serve
```

**Verificar:**
- BFF: `curl http://localhost:8089/api/appointments` → 401 (requiere JWT) ✅
- Frontend: `http://localhost:4200` → login button visible ✅

---

## 🔗 Integración con BD Oracle

**Requisitos:**
- Oracle 12c+ corriendo en `localhost:1522`
- Usuario: `vidasalud`, password: `admin123`
- Service: `XEPDB1`
- (O Docker: `docker run -d -p 1522:1521 -e ORACLE_PWD=admin123 gvenzl/oracle-xe`)

**Tablas creadas automáticamente:**
- `SERVICIO` (Catalog)
- `BOX` (Catalog)
- `CUPO_DISPONIBLE` (Catalog)
- `ATENCION` (Appointments)

**Hibernate:** `ddl-auto: update` (crea/actualiza automáticamente)

---

## 📄 Archivos de documentación

```
Desarrollo Nativo Cloud 1/
├── README_EP1.md ← Estás aquí
├── ARQUITECTURA_EP1.md ← Documentación completa
├── PRUEBA_FLUJO_EP1.md ← Guía de pruebas
├── Caso_1_VidaSalud.md ← Especificación del caso
├── EP1_DSY1107_Estudiante_encargo.md ← Pauta de evaluación
├── ms-vidasalud-appointments/
├── ms-vidasalud-catalog/ ← Nuevo en EP1
├── ms-vidasalud-bff/
└── frontend-vidasalud/
```

---

## ✅ Checklist de entrega

- [ ] Todos los servicios compilan sin errores
- [ ] BD Oracle iniciada y accesible
- [ ] 3 servicios backend iniciados en puertos correctos (8089, 8090, 8091)
- [ ] Frontend iniciado en puerto 4200
- [ ] MSAL login funciona
- [ ] JWT se obtiene y almacena
- [ ] POST /api/appointments con JWT → 201 Created
- [ ] POST /api/catalog/services con JWT + APPROLE_Admin → 201 Created
- [ ] POST /api/catalog/services con JWT + APPROLE_Cliente → 403 Forbidden
- [ ] GET /api/appointments sin JWT → 401 Unauthorized
- [ ] Documentación completada y adjunta
- [ ] Repositorios en GitHub (links en AVA)

---

## 🚀 Entrega

**Formato:**
- Frontend: repositorio GitHub (enlace en AVA + correo docente)
- Backend: repositorio GitHub (enlace en AVA + correo docente)

**Contenido por repositorio:**
```
frontend-vidasalud/
├── README.md
├── src/
├── .gitignore
└── angular.json

ms-vidasalud-bff/
├── README.md (existente)
├── pom.xml
├── src/
├── .gitignore
└── target/ (generado, en .gitignore)

ms-vidasalud-appointments/
├── README.md (existente)
├── pom.xml
├── src/
├── .gitignore
└── target/ (generado, en .gitignore)

ms-vidasalud-catalog/
├── README.md
├── pom.xml
├── src/
├── .gitignore
└── target/ (generado, en .gitignore)
```

**Nota:** No commitear `target/`, `node_modules/`, `dist/`, `.env` con credenciales reales.

---

## 📞 Contacto

- **Pauta:** `EP1_DSY1107_Estudiante_encargo.md`
- **Caso:** `Caso_1_VidaSalud.md`
- **Arquitectura:** `ARQUITECTURA_EP1.md`
- **Pruebas:** `PRUEBA_FLUJO_EP1.md`

---

## 📊 Indicadores de evaluación

### Indicador 1: MSAL en Angular (60%)

**Muy buen desempeño (100%):**
- ✅ MSAL integrado y operativo
- ✅ Login/logout funcionan
- ✅ Guards y MsalInterceptor sin fallas
- ✅ Tokens obtenidos para consumir API Gateway
- ✅ Roles y scopes leídos desde claims

**Validar en:** PRUEBA_FLUJO_EP1.md §5

### Indicador 2: Validación JWT en BFF (40%)

**Muy buen desempeño (100%):**
- ✅ BFF valida issuer y audience correctamente
- ✅ Verifica firma y vigencia del token
- ✅ Aplica autorización por rol
- ✅ Responde con códigos de error adecuados (401, 403)

**Validar en:** PRUEBA_FLUJO_EP1.md §6, §7

---

## 🎯 Resultado esperado

Al completar todas las pruebas:

1. **Frontend:**
   - Usuario hace login con Azure AD
   - Token se obtiene y almacena
   - MsalInterceptor adjunta Authorization header
   - Guards protegen rutas

2. **BFF:**
   - Recibe JWT
   - Valida issuer, audience, firma, vigencia
   - Autoriza por rol (@RequestMatchers)
   - Reenvía a microservicios

3. **Microservicios:**
   - Validan JWT nuevamente
   - Procesan request
   - Devuelven respuesta

4. **Integration:**
   - Request completo: Frontend → BFF → Appointments/Catalog → BD
   - Response completo: BD → Microservicio → BFF → Frontend

---

**Fecha de generación:** 2026-09-13  
**Versión:** EP1 — Evaluación Parcial 1  
**Asignatura:** DSY1107 — Desarrollo Cloud Native I  
**Institución:** DuocUC
