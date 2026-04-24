# LogSentry 🛡️

**Real-time log anomaly detection system** that ingests 5,000+ events/min via Kafka, applies statistical baseline anomaly detection, and surfaces alerts through a React dashboard and Grafana.

![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green?style=flat-square)
![React](https://img.shields.io/badge/React-18-blue?style=flat-square)
![Kafka](https://img.shields.io/badge/Kafka-7.5-black?style=flat-square)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?style=flat-square)

## Architecture

```
┌──────────────┐    ┌─────────┐    ┌──────────────────┐    ┌────────────┐
│  Log Sources │───▶│  Kafka  │───▶│  Spring Boot     │───▶│ PostgreSQL │
│  (Simulator) │    │ 3 parts │    │  Consumer (x3)   │    │            │
└──────────────┘    └─────────┘    │  ┌──────────────┐ │    └────────────┘
                                   │  │ Anomaly      │ │
                                   │  │ Detection    │ │    ┌────────────┐
                                   │  │ Engine       │─│───▶│ Alerts DB  │
                                   │  └──────────────┘ │    └────────────┘
                                   │  ┌──────────────┐ │
                                   │  │ Prometheus   │ │    ┌────────────┐
                                   │  │ /actuator    │─│───▶│ Grafana    │
                                   │  └──────────────┘ │    └────────────┘
                                   └──────────┬─────────┘
                                              │ REST API
                                   ┌──────────▼─────────┐
                                   │   React Dashboard  │
                                   │   (Vite + Recharts)│
                                   └────────────────────┘
```

## Features

- **High-throughput ingestion**: 5,000+ log events/min via Kafka with 3-partition parallelism
- **Statistical anomaly detection**: Sliding-window baseline with 4 detection strategies:
  - **Spike detection**: Z-score > 3σ from baseline
  - **Gradual drift**: Rolling mean shift > 2σ from historical baseline
  - **Cold-start suppression**: No false alerts during learning phase
  - **Noisy baseline filtering**: Requires sustained anomaly for N consecutive windows
- **React dashboard**: Real-time stat cards, throughput/anomaly charts, log stream, alert management
- **Prometheus metrics**: Custom gauges (throughput, lag, active anomalies) + counters (events, alerts)
- **Grafana dashboard**: Pre-provisioned panels for throughput, lag, and anomaly rate
- **REST API**: Paginated logs, alert CRUD, dashboard stats

## Quick Start

### Option 1: Local Development (H2 in-memory DB)

**Prerequisites**: Java 17+, Maven 3.8+, Node.js 18+, Kafka running locally

```bash
# 1. Start Kafka (if using Docker)
docker-compose up -d kafka zookeeper

# 2. Start Spring Boot backend
cd logsentry-server
mvn spring-boot:run

# 3. Start React frontend (in another terminal)
cd logsentry-ui
npm install
npm run dev
```

- **Backend:** http://localhost:8080
- **Dashboard:** http://localhost:5173
- **H2 Console:** http://localhost:8080/h2-console
- **Prometheus metrics:** http://localhost:8080/actuator/prometheus

### Option 2: Full Docker Stack

```bash
docker-compose up --build -d
```

- **Grafana:** http://localhost:3000 (admin/admin)
- **Prometheus:** http://localhost:9090

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/dashboard/stats` | Real-time dashboard statistics |
| GET | `/api/logs?page=0&size=50` | Paginated log events |
| POST | `/api/logs` | Manually ingest a log event |
| GET | `/api/logs/sources` | List distinct log sources |
| GET | `/api/alerts?page=0&size=50` | Paginated alerts |
| GET | `/api/alerts/active` | Active (unresolved) alerts |
| PUT | `/api/alerts/{id}/resolve` | Resolve an alert |
| GET | `/actuator/prometheus` | Prometheus metrics scrape endpoint |

## Anomaly Detection Engine

The engine uses a **sliding-window statistical approach**:

1. Events are aggregated per source in configurable time windows (default 10s)
2. After a learning phase (6 windows), baseline mean and standard deviation are computed
3. Each new window is compared against the baseline using Z-score analysis
4. Alerts are raised for spikes (>3σ), drift (>2σ shift), and sustained anomalies

Configuration (`application.yml`):
```yaml
logsentry:
  detection:
    window-size-seconds: 10
    learning-windows: 6
    spike-threshold-sigma: 3.0
    drift-threshold-sigma: 2.0
    sustained-windows: 3
```

## Project Structure

```
logsentry/
├── logsentry-server/          # Spring Boot backend
│   └── src/main/java/com/logsentry/
│       ├── api/               # REST controllers
│       ├── config/            # Kafka, Metrics, CORS config
│       ├── detection/         # Anomaly detection engine
│       ├── dto/               # Data transfer objects
│       ├── kafka/             # Producer & consumer
│       ├── model/             # JPA entities & enums
│       └── repository/        # Spring Data repos
├── logsentry-ui/              # React frontend (Vite)
│   └── src/
│       ├── components/        # Dashboard, LogStream, Alerts
│       └── api.js             # Axios API client
├── monitoring/
│   ├── prometheus/            # Prometheus config
│   └── grafana/               # Grafana provisioning
├── .github/workflows/ci.yml   # GitHub Actions CI
└── docker-compose.yml         # Full stack orchestration
```

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Backend | Java 17, Spring Boot 3.2 |
| Messaging | Apache Kafka (Confluent 7.5) |
| Database | PostgreSQL 16 / H2 (dev) |
| Frontend | React 18, Vite, Recharts |
| Metrics | Micrometer + Prometheus |
| Dashboards | Grafana 10.3 |
| CI/CD | GitHub Actions |
| Containers | Docker Compose |

## License

MIT
