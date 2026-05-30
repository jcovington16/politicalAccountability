# CI/CD Pipeline Documentation

## 🚀 Overview

This document describes the comprehensive CI/CD pipeline setup for the Political Accountability App. The pipeline is designed to automate testing, building, and deployment processes to ensure reliable and fast development cycles.

## 📋 Pipeline Components

### 1. GitHub Actions Workflow (`.github/workflows/ci-cd.yml`)

The main CI/CD pipeline includes:

- **Build and Test**: Compiles code, runs unit tests with coverage
- **Integration Tests**: Tests with real services using TestContainers
- **Security Scanning**: Vulnerability assessment with OWASP Dependency Check
- **Docker Build**: Multi-stage Docker image creation
- **Deployment**: Automated deployment to staging and production
- **Health Checks**: Post-deployment validation

### 2. Testing Framework

#### Unit Tests
- JUnit 5 with Kotlin support
- MockK for mocking
- AssertJ for assertions
- JaCoCo for code coverage

#### Integration Tests
- TestContainers for real service testing
- PostgreSQL, Elasticsearch, Kafka, MinIO containers
- End-to-end service connectivity validation

#### Test Commands
```bash
# Run all tests
make test

# Run integration tests only
make test-integration

# Generate coverage report
make test-coverage
```

### 3. Docker Optimization

#### Multi-stage Build
- **Builder stage**: Compiles application with full JDK
- **Runtime stage**: Lightweight JRE with security hardening
- Non-root user execution
- Health checks included

#### Docker Commands
```bash
# Build optimized image
make docker-build

# Run in development
make docker-run

# Clean up resources
make docker-clean
```

### 4. Deployment Automation

#### Deployment Scripts
- `scripts/deploy.sh`: Automated deployment with health checks
- `scripts/health-check.sh`: Comprehensive service validation
- Environment-specific configurations

#### Deployment Commands
```bash
# Deploy to staging
make deploy

# Deploy to production
make deploy-prod

# Run health checks
make health-check
```

## 🛠️ Development Workflow

### Local Development
```bash
# Start development environment
make dev

# Run application locally
make run

# Stop services
make stop
```

### Code Quality
```bash
# Run security scan
make security-scan

# Format code
make format

# Run linting
make lint
```

## 🔧 Configuration

### Environment Variables

#### Required for CI/CD
- `DOCKER_USERNAME`: Docker Hub username
- `DOCKER_PASSWORD`: Docker Hub password
- `DATABASE_URL`: PostgreSQL connection string
- `ELASTICSEARCH_URL`: Elasticsearch endpoint
- `KAFKA_BOOTSTRAP_SERVERS`: Kafka broker addresses
- `MINIO_ENDPOINT`: MinIO S3-compatible storage endpoint

#### GitHub Secrets Setup
1. Go to your repository settings
2. Navigate to "Secrets and variables" > "Actions"
3. Add the following secrets:
   - `DOCKER_USERNAME`
   - `DOCKER_PASSWORD`

### Service Health Checks

All services include health check endpoints:
- **Application**: `http://localhost:8081/healthcheck`
- **API Gateway**: `http://localhost:8080/api/health`
- **Metrics**: `http://localhost:8081/metrics`

## 📊 Monitoring and Observability

### Health Checks
- Service connectivity validation
- Database connection testing
- Application endpoint verification
- Resource usage monitoring

### Logging
- Structured logging with SLF4J/Logback
- Centralized log collection
- Error tracking and alerting

### Metrics
- JVM metrics (memory, CPU, GC)
- HTTP request metrics
- Database connection pool metrics
- Custom business metrics

## 🚦 Pipeline Stages

### 1. Build Stage
- Code compilation
- Dependency resolution
- Artifact generation
- Docker image building

### 2. Test Stage
- Unit test execution
- Integration test validation
- Code coverage reporting
- Security vulnerability scanning

### 3. Deploy Stage
- Environment-specific deployment
- Service health validation
- Rollback capability
- Post-deployment verification

## 🔒 Security Features

### Vulnerability Scanning
- OWASP Dependency Check integration
- Automated security report generation
- CVSS score-based failure thresholds
- Suppression file for false positives

### Container Security
- Non-root user execution
- Minimal base images
- Security scanning in CI
- Regular dependency updates

## 📈 Performance Optimization

### Build Optimization
- Gradle build caching
- Docker layer caching
- Parallel test execution
- Incremental compilation

### Runtime Optimization
- JVM tuning for containers
- Connection pooling
- Caching strategies
- Resource limits

## 🐛 Troubleshooting

### Common Issues

#### Build Failures
```bash
# Clean and rebuild
make clean
make build
```

#### Test Failures
```bash
# Check service connectivity
make health-check

# View application logs
make logs
```

#### Deployment Issues
```bash
# Check service status
docker-compose ps

# View service logs
docker-compose logs [service-name]
```

### Debug Commands
```bash
# Check system resources
docker stats

# View detailed logs
docker-compose logs -f --tail=100

# Access service shell
docker-compose exec [service-name] /bin/bash
```

## 🔄 Continuous Improvement

### Pipeline Metrics
- Build success rate
- Test execution time
- Deployment frequency
- Mean time to recovery

### Optimization Opportunities
- Parallel job execution
- Caching improvements
- Test suite optimization
- Deployment automation

## 📚 Additional Resources

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [TestContainers Documentation](https://www.testcontainers.org/)
- [Docker Best Practices](https://docs.docker.com/develop/dev-best-practices/)
- [OWASP Dependency Check](https://owasp.org/www-project-dependency-check/)

## 🤝 Contributing

When contributing to the CI/CD pipeline:

1. Test changes locally first
2. Update documentation
3. Ensure backward compatibility
4. Add appropriate tests
5. Follow security best practices

---

For questions or issues with the CI/CD pipeline, please create an issue in the repository or contact the development team.
