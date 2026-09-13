# Prueba de Flujo Completo — EP1 VidaSalud

**Objetivo:** Validar integración Frontend (MSAL) → BFF (JWT) → Microservicios.

---

## 1. Requisitos previos

- Java 17+
- Maven 3.9+
- Node.js + npm (para Angular frontend)
- Azure AD configurado (tenant, client-id, app-id-uri)
- Oracle DB (local o Docker)

**Credenciales Azure AD de prueba:**
```
AZURE_TENANT_ID: 245aec22-d743-4c13-a6e6-dce0a19bc1cf
AZURE_CLIENT_ID: fd189494-2ea3-4c54-91ed-5b5ddd644163
AZURE_APP_ID_URI: api://fd189494-2ea3-4c54-91ed-5b5ddd644163
```

---

## 2. Iniciar servicios backend

### 2.1 Appointments (puerto 8090)

```bash
cd ms-vidasalud-appointments
mvn spring-boot:run
```

**Salida esperada:**
```
Started MsVidasaludAppointmentsApplication in 5.234 seconds
[main] INFO  : Tomcat started on port(s): 8090
```

### 2.2 Catalog (puerto 8091)

```bash
cd ms-vidasalud-catalog
mvn spring-boot:run
```

**Salida esperada:**
```
Started MsVidasaludCatalogApplication in 5.234 seconds
[main] INFO  : Tomcat started on port(s): 8091
```

### 2.3 BFF (puerto 8089)

```bash
cd ms-vidasalud-bff
mvn spring-boot:run
```

**Salida esperada:**
```
Started MsVidasaludBffApplication in 5.234 seconds
[main] INFO  : Tomcat started on port(s): 8089
```

---

## 3. Iniciar Frontend Angular

```bash
cd frontend-vidasalud
npm install
ng serve
```

**Salida esperada:**
```
✔ Compiled successfully.
Application bundle generation complete. [X.XXX seconds]
✔ Build at: 2026-09-13T...
✔ Watch mode enabled. [localhost:4200]
```

---

## 4. Prueba manual (sin MSAL — curl directo)

> **Nota:** Para prueba completa con MSAL, usar navegador en step 5.

### 4.1 Health check (sin autenticación)

```bash
# BFF debe estar UP
curl -i http://localhost:8089/api/appointments
# Respuesta esperada: 401 Unauthorized (requiere JWT)
```

### 4.2 Crear servicio en catalog (requiere JWT)

**Obtener JWT de prueba:**

Para pruebas locales sin Azure AD real, crear JWT mock firmado o usar variable de entorno:

```bash
# Opción A: con cliente REST que tenga MSAL (Postman/Insomnia)
# 1. Login con Azure AD → obtener token
# 2. Copiar access_token

# Opción B: Para pruebas rápidas sin MSAL
# Desactivar JWT validation en SecurityConfig (SOLO PRUEBA LOCAL)
```

**Si se deshabilita temporalmente validación JWT:**

```bash
POST http://localhost:8089/api/catalog/services
Content-Type: application/json

{
  "nombre": "Odontología",
  "descripcion": "Servicio de limpieza dental",
  "precio": 25000
}

# Respuesta esperada: 201 Created
{
  "id": 1,
  "nombre": "Odontología",
  "descripcion": "Servicio de limpieza dental",
  "precio": 25000,
  "activo": true,
  "fechaCreacion": "2026-09-13T19:30:00"
}
```

### 4.3 Listar servicios

```bash
GET http://localhost:8089/api/catalog/services

# Respuesta esperada: 200 OK
[
  {
    "id": 1,
    "nombre": "Odontología",
    "precio": 25000,
    ...
  }
]
```

### 4.4 Crear box

```bash
POST http://localhost:8089/api/catalog/boxes
Content-Type: application/json

{
  "nombre": "Box 101",
  "servicioId": 1,
  "capacidadDiaria": 10
}

# Respuesta esperada: 201 Created
{
  "id": 1,
  "nombre": "Box 101",
  "servicioId": 1,
  "servicioNombre": "Odontología",
  "capacidadDiaria": 10,
  "activo": true,
  "fechaCreacion": "2026-09-13T19:30:00"
}
```

### 4.5 Listar cupos para hoy

```bash
GET http://localhost:8089/api/catalog/cupos

# Respuesta esperada: 200 OK (cupos inicializados para 30 días)
[
  {
    "id": 1,
    "boxId": 1,
    "boxNombre": "Box 101",
    "fecha": "2026-09-13",
    "cuposDisponibles": 10
  },
  ...
]
```

### 4.6 Crear atención en appointments

```bash
POST http://localhost:8089/api/appointments
Content-Type: application/json

{
  "paciente": "Juan Pérez",
  "servicio": "Odontología",
  "box": "Box 101",
  "fechaHora": "2026-09-15T10:00:00"
}

# Respuesta esperada: 201 Created
{
  "id": 1,
  "paciente": "Juan Pérez",
  "servicio": "Odontología",
  "box": "Box 101",
  "fechaHora": "2026-09-15T10:00:00",
  "estado": "SOLICITADA",
  "fechaCreacion": "2026-09-13T19:30:00"
}
```

### 4.7 Cambiar estado de atención (requiere rol APPROLE_Operador)

```bash
PUT http://localhost:8089/api/appointments/1/status
Content-Type: application/json

{
  "status": "CONFIRMADA"
}

# Respuesta esperada: 200 OK
{
  "id": 1,
  "estado": "CONFIRMADA",
  ...
}
```

---

## 5. Prueba completa con Frontend + MSAL

1. Abrir navegador: `http://localhost:4200`
2. Click "Login con Microsoft"
3. Autenticarse con credenciales Azure AD
4. Frontend obtiene JWT → adjunta en Authorization header
5. Hacer requests desde UI
6. Validar que BFF recibe JWT y lo valida
7. Validar que microservicios responden

**Flow esperado:**
```
[Browser] → MSAL Login → Azure AD
    ↓
[Frontend] obtiene JWT
    ↓
[Frontend] POST /api/appointments (+ Bearer JWT)
    ↓
[BFF] recibe JWT → valida con SecurityConfig
    ↓
[BFF] llama internamente → http://localhost:8090/api/atenciones
    ↓
[Appointments] valida JWT, procesa, responde
    ↓
[BFF] devuelve respuesta al Frontend
    ↓
[Frontend] muestra datos en UI
```

---

## 6. Validaciones críticas

| Punto | Prueba | Esperado |
|---|---|---|
| **BFF inicia** | `curl http://localhost:8089` | Status 401 (sin JWT) |
| **Catalog accesible** | POST /api/catalog/services | 201 Created (sin JWT = 401) |
| **Appointments accesible** | POST /api/appointments | 201 Created (sin JWT = 401) |
| **JWT validado** | Headers sin Authorization | 401 Unauthorized |
| **JWT válido + rol Admin** | Bearer token + APPROLE_Admin | 200/201 OK |
| **JWT válido + rol insuficiente** | Bearer token + APPROLE_Cliente en POST /catalog | 403 Forbidden |
| **Cupos inicializados** | GET /api/catalog/cupos | 30 registros (30 días) |
| **BFF reenvía status codes** | GET /api/appointments/999 | 404 Not Found |

---

## 7. Logs a revisar

### BFF (debe validar JWT)
```
[main] SecurityConfig: securityFilterChain configurado
[main] RestClientConfig: appointmentsRestClient = http://localhost:8090
[main] RestClientConfig: catalogRestClient = http://localhost:8091
[http-nio-8089] AadResourceServerHttpSecurityConfigurer: JWT válido, autorización por rol
```

### Appointments/Catalog (deben autenticar)
```
[main] SecurityConfig: validando issuer = https://login.microsoftonline.com/245aec22.../v2.0
[http-nio-8090] Request: Authorization header present → validación JWT OK
```

---

## 8. Troubleshooting

| Problema | Causa | Solución |
|---|---|---|
| BFF no inicia | Puerto 8089 en uso | `lsof -i :8089` y matar proceso |
| 401 Unauthorized | JWT expirado/inválido | Renovar token MSAL |
| 403 Forbidden | Rol insuficiente | Verificar APPROLE_* en token Azure AD |
| Connection refused | Servicio caído | Revisar logs, reiniciar |
| CORS error | Origen no permitido | Verificar corsConfigurationSource en BFF |
| JWT validation fail | Firma inválida | Verificar AZURE_TENANT_ID, client-id |

---

## 9. Checklist de validación

- [ ] BFF compila y arranca
- [ ] Appointments compila y arranca
- [ ] Catalog compila y arranca
- [ ] Frontend inicia en puerto 4200
- [ ] MSAL login funciona
- [ ] JWT obtenido y almacenado
- [ ] POST /api/appointments llega al BFF con JWT
- [ ] BFF reenvía a appointments (internamente)
- [ ] Appointments valida JWT y crea atención
- [ ] Response llega al frontend
- [ ] Roles se aplican (Admin > Operador > Cliente)
- [ ] 404/400/500 se devuelven con status correcto
- [ ] Cupos se inicializan al crear box

---

## 10. Resultado esperado

**Indicador 1 (MSAL — 60%):**
- ✅ MSAL integrado en Angular
- ✅ Login/logout funcionan
- ✅ Guards protegen rutas
- ✅ JWT se adjunta en requests
- ✅ Roles y scopes se leen del token

**Indicador 2 (JWT en BFF — 40%):**
- ✅ BFF valida issuer, audience, firma, vigencia
- ✅ Autorización por rol (@PreAuthorize / requestMatchers)
- ✅ 401 sin token, 403 sin rol
- ✅ Mensajes de error adecuados

---

**Fecha prueba:** 2026-09-13  
**Estado:** ✅ Documentado  
**Siguiente:** Git commit + push a GitHub
