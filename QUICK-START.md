# 🚀 Quick Start Guide

## Prerequisites

- Java 21 JDK
- Docker and Docker Compose
- Git

## 1. Clone and Setup

```bash
git clone <your-repo-url>
cd political-accountability-app
```

## 2. Start Development Environment

```bash
# Start all services (PostgreSQL, Elasticsearch, Kafka, MinIO)
make dev

# Or manually:
docker-compose up -d
```

## 3. Build and Run

```bash
# Build the application
make build

# Run locally
make run

# Or run in Docker
make docker-run
```

## 4. Verify Everything Works

```bash
# Run health checks
make health-check

# Run tests
make test

# Check services
curl http://localhost:8080/api/health
```

## 5. Development Workflow

```bash
# Make changes to your code
# Run tests
make test

# Check code quality
make security-scan

# Deploy to staging
make deploy
```

## 🎯 Key Commands

| Command | Description |
|---------|-------------|
| `make dev` | Start development environment |
| `make build` | Build application |
| `make test` | Run all tests |
| `make run` | Run application locally |
| `make deploy` | Deploy to staging |
| `make health-check` | Check service health |
| `make logs` | View application logs |

## 🔧 Troubleshooting

### Services won't start?
```bash
make stop
make docker-clean
make dev
```

### Tests failing?
```bash
make health-check
docker-compose logs
```

### Build issues?
```bash
make clean
make build
```

## 📊 Monitoring

- **Application**: http://localhost:8080
- **Health Check**: http://localhost:8081/healthcheck
- **Metrics**: http://localhost:8081/metrics
- **MinIO Console**: http://localhost:9001
- **Elasticsearch**: http://localhost:9200

## 🚀 Next Steps

1. Read the [GamePlan](GamePlan) for project roadmap
2. Check [CI-CD-README.md](CI-CD-README.md) for detailed pipeline info
3. Explore the [API documentation](README.md#api-endpoints)
4. Start developing your political accountability features!

---

**Need help?** Check the main [README.md](README.md) or create an issue in the repository.
