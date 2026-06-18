# Political Accountability App - Development Makefile

.PHONY: help build test clean run deploy health-check api-smoke load-smoke release-check launch-monitor docker-build docker-run db-status db-migrate db-validate db-history db-rollback db-new db-tag ingest-local ingest-dry-run ingest-congress-bills ingest-congress-members ingest-govinfo-packages ingest-state-civic ingest-federal-executives ingest-media refresh-live-data profile-completeness kafka-health kafka-recover kafka-raw-log dashboard-install dashboard-dev dashboard-build mobile-install mobile-start mobile-typecheck

# Default target
help:
	@echo "Political Accountability App - Development Commands"
	@echo "=================================================="
	@echo ""
	@echo "Build & Test:"
	@echo "  build          - Build the application"
	@echo "  test           - Run unit tests"
	@echo "  test-integration - Run integration tests"
	@echo "  test-coverage  - Generate test coverage report"
	@echo "  clean          - Clean build artifacts"
	@echo ""
	@echo "Development:"
	@echo "  run            - Run the application locally"
	@echo "  dev            - Start development environment"
	@echo "  stop           - Stop development environment"
	@echo "  ingest-local dir=x - Import local CSV/JSON files"
	@echo "  ingest-dry-run - Validate sample CSV/JSON files without writing to the database"
	@echo "  ingest-congress-bills - Fetch recent Congress.gov bill events"
	@echo "  ingest-congress-members - Fetch Congress.gov member profiles"
	@echo "  ingest-govinfo-packages - Fetch recent GovInfo official document events"
	@echo "  ingest-state-civic - Fetch Open States and Google Civic records"
	@echo "  ingest-federal-executives - Seed recent U.S. President profiles"
	@echo "  ingest-media - Fetch public media discovery records from GDELT, RSS, and YouTube"
	@echo "  refresh-live-data - Refresh primary official sources and normalize into PostgreSQL"
	@echo "  profile-completeness names='Name One,Name Two' - Report stored profile gaps"
	@echo "  kafka-health - Verify Kafka metadata, produce, and consume"
	@echo "  kafka-recover - Reset only local Kafka/ZooKeeper data and restart them"
	@echo "  kafka-raw-log - Print raw-content Kafka events"
	@echo "  dashboard-dev  - Run React dashboard"
	@echo "  dashboard-build - Build React dashboard"
	@echo "  mobile-start   - Run React Native/Expo mobile app"
	@echo "  mobile-typecheck - Type-check mobile app"
	@echo ""
	@echo "Docker:"
	@echo "  docker-build   - Build Docker image"
	@echo "  docker-run     - Run application in Docker"
	@echo "  docker-clean   - Clean Docker images and containers"
	@echo ""
	@echo "Deployment:"
	@echo "  deploy         - Deploy to staging"
	@echo "  deploy-prod    - Deploy to production"
	@echo "  health-check   - Run health checks"
	@echo "  api-smoke      - Run public/admin API smoke checks"
	@echo "  load-smoke     - Run small search load smoke test"
	@echo "  release-check  - Run release-candidate checks"
	@echo "  launch-monitor - Print launch monitoring snapshot"
	@echo ""
	@echo "Quality:"
	@echo "  security-scan  - Run security vulnerability scan"
	@echo "  lint           - Run code linting"
	@echo "  format         - Format code"
	@echo ""
	@echo "Database:"
	@echo "  db-status      - Show pending Liquibase migrations"
	@echo "  db-migrate     - Apply pending Liquibase migrations"
	@echo "  db-validate    - Validate Liquibase changelogs"
	@echo "  db-history     - Show applied migration history"
	@echo "  db-rollback    - Roll back the most recent migration"
	@echo "  db-new name=x  - Create a new timestamped migration"
	@echo "  db-tag tag=x   - Tag the current database state"

# Build targets
build:
	@echo "🔨 Building application..."
	./gradlew clean build shadowJar

test:
	@echo "🧪 Running unit tests..."
	./gradlew test

test-integration:
	@echo "🧪 Running integration tests..."
	./gradlew integrationTest

test-coverage:
	@echo "📊 Generating test coverage report..."
	./gradlew testCoverage
	@echo "Coverage report available at: build/reports/jacoco/test/html/index.html"

clean:
	@echo "🧹 Cleaning build artifacts..."
	./gradlew clean
	docker system prune -f

# Development targets
run:
	@echo "🚀 Starting application..."
	@set -a; [ ! -f .env ] || . ./.env; set +a; ./gradlew :api-gateway:runApiGateway

dev:
	@echo "🏗️ Starting development environment..."
	docker-compose up -d
	@echo "⏳ Waiting for services to start..."
	sleep 30
	@echo "✅ Development environment ready!"
	@echo "📊 Services:"
	@echo "  - API Gateway: http://localhost:8080"
	@echo "  - PostgreSQL: localhost:5432"
	@echo "  - Elasticsearch: http://localhost:9200"
	@echo "  - Kafka: localhost:9092"
	@echo "  - MinIO: http://localhost:9000"

stop:
	@echo "🛑 Stopping development environment..."
	docker-compose down

# Docker targets
docker-build:
	@echo "🐳 Building Docker image..."
	docker build -t political-accountability-app:latest .

docker-run:
	@echo "🐳 Running application in Docker..."
	docker-compose up -d

docker-clean:
	@echo "🧹 Cleaning Docker resources..."
	docker-compose down -v
	docker system prune -af
	docker volume prune -f

# Deployment targets
deploy:
	@echo "🚀 Deploying to staging..."
	./scripts/deploy.sh staging

deploy-prod:
	@echo "🚀 Deploying to production..."
	./scripts/deploy.sh production

health-check:
	@echo "🔍 Running health checks..."
	./scripts/health-check.sh

api-smoke:
	./scripts/api-smoke-test.sh

load-smoke:
	./scripts/search-load-smoke.sh

release-check:
	./scripts/release-candidate-check.sh

launch-monitor:
	./scripts/launch-monitor.sh

# Quality targets
security-scan:
	@echo "🔒 Running security vulnerability scan..."
	./gradlew dependencyCheckAnalyze
	@echo "Security report available at: build/reports/dependency-check-report.html"

lint:
	@echo "🔍 Running code linting..."
	./gradlew checkstyleMain checkstyleTest

format:
	@echo "✨ Formatting code..."
	./gradlew ktlintFormat

# CI/CD targets
ci-build:
	@echo "🔨 CI Build..."
	./gradlew clean build shadowJar --no-daemon

ci-test:
	@echo "🧪 CI Tests..."
	./gradlew test integrationTest --no-daemon

ci-security:
	@echo "🔒 CI Security Scan..."
	./gradlew dependencyCheckAnalyze --no-daemon

# Database targets
db-status:
	@echo "Showing database migration status..."
	./scripts/db/liquibase.sh status --verbose

db-migrate:
	@echo "Running database migrations..."
	./scripts/db/liquibase.sh update

db-validate:
	@echo "Validating database changelogs..."
	./scripts/db/liquibase.sh validate

db-history:
	@echo "Showing database migration history..."
	./scripts/db/liquibase.sh history

db-rollback:
	@echo "Rolling back the most recent database migration..."
	./scripts/db/liquibase.sh rollbackCount 1

db-new:
	@test -n "$(name)" || (echo "Usage: make db-new name=add_short_description" && exit 1)
	./scripts/db/new-migration.sh "$(name)"

db-tag:
	@test -n "$(tag)" || (echo "Usage: make db-tag tag=before_content_refactor" && exit 1)
	./scripts/db/liquibase.sh tag "$(tag)"

db-seed:
	@echo "🌱 Seeding database..."
	# Add your database seeding commands here

ingest-local:
	@test -n "$(dir)" || (echo "Usage: make ingest-local dir=data/ingestion" && exit 1)
	./gradlew :ingestion-service:runLocalFileIngestion -PinputDir="$(dir)"

ingest-dry-run:
	node scripts/validate-sample-data.mjs data/templates

ingest-congress-bills:
	@set -a; [ ! -f .env ] || . ./.env; set +a; ./gradlew :ingestion-service:runCongressGovBills

ingest-congress-members:
	@set -a; [ ! -f .env ] || . ./.env; set +a; ./gradlew :ingestion-service:runCongressGovMembers

ingest-govinfo-packages:
	@set -a; [ ! -f .env ] || . ./.env; set +a; ./gradlew :ingestion-service:runGovInfoPackages

kafka-raw-log:
	@set -a; [ ! -f .env ] || . ./.env; set +a; ./gradlew :ingestion-service:runRawContentLogger

kafka-health:
	./scripts/kafka-health.sh

kafka-recover:
	@echo "Resetting local Kafka/ZooKeeper metadata. PostgreSQL and application data are preserved."
	docker compose stop api-gateway kafka zookeeper
	docker compose rm -sfv kafka zookeeper
	docker volume rm -f political-accountability-app_kafka_data political-accountability-app_zookeeper_data political-accountability-app_zookeeper_logs
	docker compose up -d zookeeper kafka

refresh-live-data:
	./scripts/refresh-live-data.sh

profile-completeness:
	@test -n "$(names)" || (echo "Usage: make profile-completeness names='Name One,Name Two'" && exit 1)
	@set -a; [ ! -f .env ] || . ./.env; set +a; node scripts/profile-completeness-report.mjs --names "$(names)"

ingest-official-normalized:
	@set -a; [ ! -f .env ] || . ./.env; set +a; ./gradlew :ingestion-service:runOfficialDataNormalization

ingest-state-civic:
	@set -a; [ ! -f .env ] || . ./.env; set +a; ./gradlew :ingestion-service:runStateCivicIngestion

ingest-federal-executives:
	@set -a; [ ! -f .env ] || . ./.env; set +a; ./gradlew :ingestion-service:runFederalExecutiveSeed

ingest-media:
	@set -a; [ ! -f .env ] || . ./.env; set +a; ./gradlew :ingestion-service:runMediaIngestion

dashboard-install:
	cd dashboard && npm install

dashboard-dev:
	cd dashboard && npm run dev

dashboard-build:
	cd dashboard && npm run build

mobile-install:
	cd mobile && npm install

mobile-start:
	cd mobile && npm start

mobile-typecheck:
	cd mobile && npm run typecheck

# Monitoring targets
logs:
	@echo "📋 Showing application logs..."
	docker-compose logs -f api-gateway

metrics:
	@echo "📊 Application metrics..."
	curl -s http://localhost:8081/metrics | grep -E "(jvm_|http_|database_)" | head -20
