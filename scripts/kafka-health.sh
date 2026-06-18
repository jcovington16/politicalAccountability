#!/usr/bin/env bash
set -euo pipefail

service_status="$(docker compose ps --status running --format json kafka)"
if [[ -z "$service_status" ]]; then
  echo "Kafka is not running. Start it with: docker compose up -d zookeeper kafka"
  exit 1
fi

health="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}missing{{end}}' "$(docker compose ps -q kafka)")"
if [[ "$health" != "healthy" ]]; then
  echo "Kafka container health is '$health'."
  docker compose logs --tail=80 kafka
  exit 1
fi

# A broker can answer metadata requests while message flow is still broken. This
# short-lived topic verifies the full producer -> broker -> consumer path.
topic="public-record-health-$(date +%s)-$$"
cleanup() {
  docker compose exec -T kafka kafka-topics \
    --bootstrap-server localhost:9092 --delete --topic "$topic" >/dev/null 2>&1 || true
}
trap cleanup EXIT

docker compose exec -T kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --create --if-not-exists --topic "$topic" --partitions 1 --replication-factor 1 >/dev/null

message="healthy-$(date +%s)-$$"
printf '%s\n' "$message" | docker compose exec -T kafka kafka-console-producer \
  --bootstrap-server localhost:9092 --topic "$topic" >/dev/null

received="$(docker compose exec -T kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 --topic "$topic" --from-beginning \
  --max-messages 1 --timeout-ms 10000 2>/dev/null)"

if [[ "$received" != "$message" ]]; then
  echo "Kafka round-trip failed: produced and consumed messages differ."
  exit 1
fi

echo "Kafka is healthy: metadata, produce, and consume checks passed."
