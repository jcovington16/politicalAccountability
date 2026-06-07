#!/bin/bash

# Health check script for Political Accountability App
set -e

# Configuration
API_URL="http://localhost:8080"
HEALTH_URL="http://localhost:8081/healthcheck"
METRICS_URL="http://localhost:8081/metrics"

echo "🔍 Running health checks for Political Accountability App..."

# Function to check HTTP endpoint
check_endpoint() {
    local url=$1
    local name=$2
    local expected_status=${3:-200}
    
    echo "Checking $name at $url..."
    
    if response=$(curl -s -w "%{http_code}" -o /tmp/response.json "$url"); then
        if [ "$response" -eq "$expected_status" ]; then
            echo "✅ $name is healthy (HTTP $response)"
            return 0
        else
            echo "❌ $name returned HTTP $response (expected $expected_status)"
            return 1
        fi
    else
        echo "❌ $name is not responding"
        return 1
    fi
}

# Function to check service connectivity
check_service() {
    local service=$1
    local check_command=$2
    
    echo "Checking $service connectivity..."
    
    if eval "$check_command" >/dev/null 2>&1; then
        echo "✅ $service is accessible"
        return 0
    else
        echo "❌ $service is not accessible"
        return 1
    fi
}

# Check application endpoints
echo "🌐 Checking application endpoints..."
check_endpoint "$HEALTH_URL" "Health Check Endpoint"
check_endpoint "$METRICS_URL" "Metrics Endpoint"
check_endpoint "$API_URL/politicians/not-a-uuid/profile" "Profile Endpoint Routing" 400
check_endpoint "$API_URL/politicians/not-a-uuid/votes" "Politician Votes Endpoint Routing" 400
check_endpoint "$API_URL/bills/not-a-uuid/votes" "Bill Votes Endpoint Routing" 400

# Check database connectivity
echo "🗄️ Checking database connectivity..."
check_service "PostgreSQL" "docker-compose exec -T postgres pg_isready -U postgres"

# Check Elasticsearch
echo "🔍 Checking Elasticsearch..."
check_endpoint "http://localhost:9200/_cluster/health" "Elasticsearch Cluster"

# Check Kafka
echo "📨 Checking Kafka..."
check_service "Kafka" "docker-compose exec -T kafka kafka-topics.sh --bootstrap-server localhost:9092 --list"

# Check MinIO
echo "📦 Checking MinIO..."
check_endpoint "http://localhost:9000/minio/health/live" "MinIO Health"

# Check application logs for errors
echo "📋 Checking application logs for errors..."
if docker-compose logs api-gateway | grep -i error | tail -5; then
    echo "⚠️ Found errors in application logs"
else
    echo "✅ No recent errors in application logs"
fi

# Check system resources
echo "💻 Checking system resources..."
echo "CPU Usage:"
docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}"

echo "🎉 Health check completed!"
