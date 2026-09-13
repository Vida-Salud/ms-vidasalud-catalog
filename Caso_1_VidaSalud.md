# Caso VidaSalud: Plataforma para gestión de atenciones en centros de salud

> Caso semestral — Instrucciones de caso

## 1. Contexto

Hoy cada centro agenda por WhatsApp, planilla o un sistema distinto. Se pierden cupos, el paciente no sabe si fue confirmado y no hay una vista única de la sala de espera.

Una red de 20 centros de atención primaria y clínicas dentales necesita una plataforma unificada para:

- Agendar atenciones por la web, administrar cupos de boxes y coordinar la llegada del paciente.
- Notificar al paciente (email/push) y al box clínico (ticket de admisión).
- Generar un panel de operaciones en tiempo real (atenciones por hora, tiempo de espera, estados activos).
- Auditar eventos clínico-administrativos (quién solicitó, confirmó, atendió o cerró una atención).

### Lo que la red exige (stack obligatorio)

- Login corporativo con **Azure AD** (IDaaS).
- Frontend **Angular** con **MSAL** y autorización por rol (Admin, Operador del dominio, Cliente del dominio).
- Backend **Spring Boot** con microservicios detrás de **AWS API Gateway**, protegido con el JWT de Azure AD.
- Despliegue en **AWS EC2** con **Docker / Docker Compose**.
- Mensajería asíncrona con **RabbitMQ** (tareas/colas de trabajo) y streaming con **Kafka** (con Zookeeper) para analítica y auditoría en tiempo real.

## 2. Actores y roles

| Rol | Responsabilidad |
|---|---|
| Admin | Coordina la red, el catálogo de prestaciones y ve KPIs globales. |
| Recepcionista (Operador) | Confirma, llama a box y cierra el flujo de la atención. |
| Paciente (Cliente) | Solicita y sigue sus atenciones. |
| Auditor | Consulta el timeline. Solo lectura. |

## 3. Alcance funcional mínimo

| Módulo | Descripción | Actores | Reglas clave |
|---|---|---|---|
| Gestión de atenciones | CRUD de atenciones y cambio de estado (SOLICITADA → CONFIRMADA → EN_ESPERA → EN_ATENCIÓN → CERRADA / CANCELADA) | Paciente, Recepcionista | No se puede pasar a EN_ATENCIÓN sin CONFIRMAR |
| Catálogo | CRUD de prestaciones, boxes y cupos disponibles | Admin | El cupo del box disminuye al confirmar la atención |
| Notificaciones | Email/push al paciente y ticket de admisión al box | Recepcionista, Paciente | Envío asíncrono (cola) |
| Reportería | Panel de KPIs: atenciones por hora, tiempo de espera, estados activos | Admin | Datos por streaming (Kafka) sin bloquear el core |
| Auditoría | Timeline de eventos de la atención | Auditor | Solo lectura |

## 4. Seguridad e identidad (IDaaS Azure + API Gateway)

- App Registration "vidasalud": `clientId`, `redirectUri`, `authority = https://login.microsoftonline.com/<TENANT_ID>/`
- MSAL Angular: proteger rutas y adjuntar `Bearer <access_token>` a cada request.
- AWS API Gateway (HTTP API) con JWT Authorizer: `issuer = https://login.microsoftonline.com/<TENANT_ID>/v2.0` y `audiences = api://<API_CLIENT_ID>`.
- Spring Security: validar el JWT con `security.oauth2.resourceserver.jwt.issuer-uri` y comprobar que el rol puede usar el endpoint llamado.

## 5. Microservicios de dominio

| Servicio | Dominio | DB | Responsabilidad | Exposición |
|---|---|---|---|---|
| ms-vidasalud-appointments | Atenciones | Oracle | CRUD atenciones, estados, coordinación de cupos y notificación | `/api/appointments/*` |
| ms-vidasalud-catalog | Prestaciones / cupos | Oracle | CRUD prestaciones, boxes, cupos y precios | `/api/catalog/*` |
| ms-vidasalud-notify | Notificaciones | sin DB | Procesa envío email/webpush y ticket de box vía RabbitMQ | no público (consumidor RabbitMQ) |
| ms-vidasalud-audit | Auditoría / timeline | Oracle | Consume Kafka y persiste eventos | `/api/audit/*` (read-only) |
| ms-vidasalud-report | KPIs / analytics | Oracle | Agregaciones y endpoints de lectura (consume Kafka) | `/api/report/*` (read-only) |

Además del listado anterior, debe incluirse:

- **ms-vidasalud-bff** (Spring Boot + Spring Security) como BFF detrás del API Gateway.
- Un microservicio administrador de **RabbitMQ**.
- Un microservicio administrador de **Kafka**.

(según la pauta de cada evaluación)

### Endpoints esenciales (ejemplos)

**ms-vidasalud-appointments**
```
POST /api/appointments                     # crear atención
GET  /api/appointments/{id}
PUT  /api/appointments/{id}/status
     body: { "status": "SOLICITADA|CONFIRMADA|EN_ESPERA|EN_ATENCIÓN|CERRADA|CANCELADA" }
GET  /api/appointments?status=...&from=...&to=...
```

**ms-vidasalud-catalog**
```
GET  /api/catalog/services
POST /api/catalog/services
PUT  /api/catalog/services/{id}            # precio/cupo
```

**ms-vidasalud-report**
```
GET /api/report/kpis?range=last24h
GET /api/report/top-services?range=last7d
```

## 6. Pantallas propuestas

| Pantalla | Ruta | Roles | Función |
|---|---|---|---|
| Login | `/login` | público | MSAL. Botón «Iniciar sesión con Microsoft». |
| Dashboard | `/dashboard` | todos los autenticados | Admin: KPIs de atenciones. Recepcionista: sala de espera y pendientes. Paciente: próximas atenciones y estado. |
| Atenciones | `/appointments` | Admin, Recepcionista, Paciente | Listar, crear (paciente o recepcionista) y cambiar estado (recepcionista/admin). |
| Catálogo de prestaciones | `/catalog` | Admin, Recepcionista | Prestaciones, boxes y cupos. |
| Reportería | `/reports` | Admin | Atenciones por hora, tiempo de espera, prestaciones más demandadas. |
| Auditoría | `/audit` | Admin, Auditor | Trazabilidad de la atención. Filtros: usuario, fechas, tipo de evento. |

**Flujo de llamadas seguras:**
```
JWT → API Gateway → ms-vidasalud-bff → microservicio de dominio
```

## 7. Despliegue (EC2 + Docker Compose)

- **ec2-apps**: appointments-svc, catalog-svc, notify-svc, report-svc, audit-svc
- **ec2-mq**: RabbitMQ (clúster de 2 nodos para las evaluaciones) con Management UI
- **ec2-kafka**: Zookeeper (3 nodos) + Kafka (3 brokers) + Kafka UI
- Un `compose.yml` para apps, otro para mq y otro para kafka.
- Security Groups: abrir solo los puertos necesarios (AMQP/5672, Kafka/9092, HTTP APIs internas).

### Sugerencia de repositorios GitHub

```
/frontend-vidasalud            (Angular + MSAL)
/ms-vidasalud-bff               (Spring Boot, Spring Security)
/ms-vidasalud-appointments      (Spring Boot)
/ms-vidasalud-catalog           (Spring Boot)
/ms-vidasalud-notify            (Spring Boot, consumer RabbitMQ)
/ms-vidasalud-report            (Spring Boot, consumer Kafka)
/ms-vidasalud-audit             (Spring Boot, consumer Kafka)
/infra                          → /apps/compose.yml · /mq/compose.yml · /kafka/compose.yml
/docs
```

## 8. Topología RabbitMQ (6 colas: 3 flujos + 3 DLQ)

| Cola principal | Propósito | DLQ | Binding direct | Binding topic |
|---|---|---|---|---|
| q.cmd.email | Email/push al paciente (cita confirmada, llamado a box, cierre) | q.cmd.email.dlq | email.send | email.* |
| q.cmd.admission | Ticket de admisión / llamado a box clínico | q.cmd.admission.dlq | admission.ticket | admission.# |
| q.cmd.record | Generación de PDF (comprobante o resumen de atención) | q.cmd.record.dlq | record.gen | record.* |

**Exchanges:** `cmd.direct` (direct), `cmd.topic` (topic) y `cmd.dead.dlx` (direct, para DLQ).

**Buenas prácticas:** envelope común (`type`, `eventId`, `timestamp`, `traceId`, `correlationId`), ACK/NACK explícitos, idempotencia y métricas de tasa de DLQ.

## 9. Topología Kafka

| Tópico | Particiones | Réplicas | Política | Retención | Propósito |
|---|---|---|---|---|---|
| appointments.events | 3 | 3 | delete | 3–7 días | Fuente de verdad de eventos de la atención. Alimenta reportería y auditoría. |
| audit.timeline | 3 | 3 | compact, delete | 14–30 días | Historial quién / qué / cuándo / desde dónde. |
| *.DLT (por consumidor) | 3 | 3 | delete | 7–14 días | Mensajes que fallaron tras N reintentos, con metadatos de error. |
