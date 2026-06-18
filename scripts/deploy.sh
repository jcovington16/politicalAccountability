#!/bin/bash

# Deployment script for Political Accountability App
set -e

# Configuration
ENVIRONMENT=${1:-staging}
VERSION=${2:-latest}
DOCKER_IMAGE="political-accountability-app:${VERSION}"

echo "🚀 Deploying Political Accountability App to ${ENVIRONMENT} environment..."

# Function to check if a command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Check prerequisites
if ! command_exists docker; then
    echo "❌ Docker is not installed. Please install Docker first."
    exit 1
fi

if ! command_exists docker-compose; then
    echo "❌ Docker Compose is not installed. Please install Docker Compose first."
    exit 1
fi

# Build the application
echo "📦 Building application..."
./gradlew clean build shadowJar

# Build Docker image
echo "🐳 Building Docker image..."
docker build -t ${DOCKER_IMAGE} .

# Stop existing containers
echo "🛑 Stopping existing containers..."
docker-compose down

# Start infrastructure services
echo "🏗️ Starting infrastructure services..."
docker-compose up -d postgres elasticsearch kafka zookeeper minio

# Wait for services to be ready
echo "⏳ Waiting for services to be ready..."
sleep 30

# Health check for services
echo "🔍 Checking service health..."

# Check PostgreSQL
until docker-compose exec -T postgres pg_isready -U postgres; do
    echo "Waiting for PostgreSQL..."
    sleep 2
done

# Check Elasticsearch
until curl -f http://localhost:9200/_cluster/health; do
    echo "Waiting for Elasticsearch..."
    sleep 2
done

# Check Kafka
until docker-compose exec -T kafka kafka-topics.sh --bootstrap-server localhost:9092 --list; do
    echo "Waiting for Kafka..."
    sleep 2
done

# Check MinIO
until curl -f http://localhost:9000/minio/health/live; do
    echo "Waiting for MinIO..."
    sleep 2
done

echo "✅ All services are healthy!"

# Start the application
echo "🚀 Starting application..."
docker-compose up -d api-gateway

# Wait for application to start
echo "⏳ Waiting for application to start..."
sleep 60

# Health check for application
echo "🔍 Checking application health..."
for i in {1..30}; do
    if curl -f http://localhost:8081/healthcheck; then
        echo "✅ Application is healthy!"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "❌ Application health check failed after 30 attempts"
        exit 1
    fi
    echo "Attempt $i/30: Application not ready yet..."
    sleep 5
done

# Run smoke tests
echo "🧪 Running smoke tests..."
./scripts/api-smoke-test.sh
REQUESTS=10 ./scripts/search-load-smoke.sh

echo "🎉 Deployment completed successfully!"
echo "📊 Application is running at: http://localhost:8080"
echo "🔍 Health check endpoint: http://localhost:8081/healthcheck"
echo "📈 Monitoring dashboard: http://localhost:8081/metrics"

# Show running containers
echo "📋 Running containers:"
docker-compose ps
