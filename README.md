# Try AWS (LocalStack) — Microservicios EDA con SQS

Proyecto demo de **arquitectura EDA (Event-Driven Architecture)** con **microservicios** en Spring Boot.
Se utiliza **SQS** como cola de eventos, **S3** como almacenamiento de artefactos (PDF), **SES** para notificaciones (simuladas) y **PostgreSQL** para persistencia.
Todo corre en **Docker** y se apoya en **LocalStack** para simular AWS de forma local.

## Arquitectura

**Flujo principal (EDA):**

1. `api-service` expone un endpoint REST para crear una tarea.
2. Al crear la tarea:
   - Persiste la entidad `Task` en Postgres (estado inicial `PENDING`).
   - Publica un evento en **SQS** (`task-queue`) con el payload `{"taskId": <id>}`.
3. `worker-service` realiza *polling* a la cola SQS:
   - Consume el mensaje y recupera la tarea desde Postgres.
   - Genera un **PDF** con **OpenPDF** (LibrePDF) con el detalle de la tarea.
   - Sube el PDF a **S3** (bucket `task-bucket`).
   - Envía una notificación por **SES** (simulada en LocalStack).
   - Actualiza la tarea (`COMPLETED` + `resultUrl`) y elimina el mensaje de la cola.

**Observabilidad:**
- Ambos servicios exponen métricas vía **Spring Boot Actuator** en `/actuator/prometheus`.
- **Prometheus** las scrapea y **Grafana** permite visualizarlas.

## Servicios (Docker Compose)

Definidos en `docker-compose.yml`:

- `localstack` (AWS local): expone `4566`
  - Servicios habilitados: `s3`, `sqs`, `ses`
  - Inicialización automática con `infra/init-aws.sh`:
    - Crea bucket S3: `task-bucket`
    - Crea cola SQS: `task-queue`
    - Verifica identidad SES: `noreply@example.com`
- `postgres` (DB): expone `5432`
- `api-service` (REST): expone `8080`
- `worker-service` (worker): escucha `8081` dentro de la red Docker
- `prometheus`: expone `9090`
- `grafana`: expone `3000`

## Tecnologías

- Java 21 + Spring Boot 4.0.1
- Spring WebMVC + Spring Data JPA + Validation
- AWS SDK v2 (SQS, S3, SES)
- OpenPDF (`com.github.librepdf:openpdf`)
- Actuator + Micrometer Prometheus
- Docker / Docker Compose
- LocalStack (simulación AWS)

## Requisitos

- Docker Desktop (con Docker Compose)

Opcional (si querés inspeccionar LocalStack desde tu host):
- `aws` CLI o `awslocal` (awscli-local)

## Configuración

El `docker-compose.yml` utiliza variables de entorno. Creá un archivo `.env` en la raíz del repo (junto al compose) con valores mínimos

Notas:
- Los servicios usan `DB_HOST` y `AWS_HOST` para apuntar a `postgres` y `localstack` dentro de Docker.
- Los endpoints AWS apuntan a LocalStack vía `http://<AWS_HOST>:4566`.

## Levantar el stack

Desde la raíz del proyecto:

```bash
docker compose up --build
```

Para bajar y limpiar volúmenes:

```bash
docker compose down -v
```

## Uso

### 1) Crear una tarea

`api-service` expone:

- `POST /tasks`

Ejemplo:

```bash
curl -X POST http://localhost:8080/tasks \
  -H "Content-Type: application/json" \
  -d "{\"description\": \"Generar reporte PDF\"}"
```

Respuesta: devuelve la entidad `Task` creada (con `status=PENDING`).

### 2) Procesamiento asíncrono

`worker-service` procesa la cola periódicamente (scheduler cada ~5s) y:

- Genera un PDF `report_task_<id>.pdf`
- Lo sube al bucket `task-bucket`
- Actualiza la tarea (`COMPLETED`) con `resultUrl`
- Envia un email simulado via SES de AWS

La URL que se guarda actualmente tiene formato:

- `http://localhost:4566/task-bucket/report_task_<id>.pdf`

(En LocalStack, el puerto `4566` está publicado hacia tu host, por lo que debería ser accesible desde tu máquina).

## Monitoreo

### Actuator / Prometheus (por servicio)

- API: `http://localhost:8080/actuator/prometheus`
- Worker: `http://localhost:8081/actuator/prometheus`

### Prometheus

- UI: `http://localhost:9090`

Prometheus está configurado en `monitoring/prometheus.yml` para scrapear:
- `api-service:8080/actuator/prometheus`
- `worker-service:8081/actuator/prometheus`

### Grafana

- UI: `http://localhost:3000`
- Usuario: `admin`
- Password: `admin` (configurado por `GF_SECURITY_ADMIN_PASSWORD`)

Luego podés agregar Prometheus como datasource apuntando a:
- `http://prometheus:9090`

## Estructura del repositorio

- `api-service/`: Microservicio REST que crea tareas y publica eventos a SQS.
- `worker-service/`: Worker que consume SQS, genera PDF, sube a S3 y notifica por SES.
- `infra/init-aws.sh`: Script de inicialización de LocalStack (bucket, queue, SES).
- `monitoring/prometheus.yml`: Configuración de scraping de métricas.

## Troubleshooting rápido

- Si `api-service` o `worker-service` fallan al arrancar, revisá que `.env` tenga `DB_NAME/DB_USER/DB_PASS`.
- Para ver logs:

```bash
docker compose logs -f api-service
docker compose logs -f worker-service
```

- Para verificar recursos en LocalStack (desde el contenedor):

```bash
docker exec -it localstack_main awslocal sqs list-queues
docker exec -it localstack_main awslocal s3 ls
```

Todo esto se desplegará en una computadora personal, utilizandola como servidor local y futuramente integraremos GitHub Actions para flujos de CI/CD