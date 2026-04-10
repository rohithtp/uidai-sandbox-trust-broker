# Infrastructure Management Guide

This document provides instructions on how to run and manage the core infrastructure components required for the UIDAI Sandbox Trust Broker.

## Core Services

The following services are configured in the `docker/docker-compose.yml` file:

| Service | Image | Internal Port | External Port | Description |
|---------|-------|---------------|---------------|-------------|
| **Zookeeper** | `confluentinc/cp-zookeeper:7.5.0` | 2181 | 2181 | Distributed configuration service for Kafka. |
| **Kafka** | `confluentinc/cp-kafka:7.5.0` | 9092 | 9092 | Event streaming platform for inter-service communication. |
| **Redis** | `redis:7.2-alpine` | 6379 | 6379 | High-performance caching and state management. |

## Prerequisites

- **Option A: Docker Desktop** (Recommended for Ease of Use)
    - [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running.
- **Option B: Colima** (Recommended for Performance/CLI focus)
    - [Colima](https://github.com/abiosoft/colima) and the `docker` / `docker-compose` CLI tools installed (e.g., via `brew install colima docker docker-compose`).

## Running the Infrastructure

Navigate to the project root directory and use the following commands:

### 1. Start Infrastructure Environment
- **If using Docker Desktop**: Just ensure the application is running.
- **If using Colima**: Start the Colima VM first:
  ```bash
  colima start
  ```

### 2. Start Services
To start all services in the background (detached mode):
```bash
docker compose -f docker/docker-compose.yml up -d
```
*Note: Depending on your installation, you may need to use `docker-compose` instead of `docker compose`.*

### 3. Check Status
Verify that all containers are running and healthy:
```bash
docker compose -f docker/docker-compose.yml ps
```

### 4. View Logs
To follow the logs of all services:
```bash
docker compose -f docker/docker-compose.yml logs -f
```
To view logs for a specific service (e.g., Kafka):
```bash
docker compose -f docker/docker-compose.yml logs -f kafka
```

### 5. Stop Services
To stop and remove the containers (keeps volumes/data intact):
```bash
docker compose -f docker/docker-compose.yml stop
```
To shut down everything completely (removes containers and internal network):
```bash
docker compose -f docker/docker-compose.yml down
```

### 6. Clean Data (Reset)
To stop services and remove all associated volumes (warning: this deletes all persistent data in Kafka/Redis):
```bash
docker compose -f docker/docker-compose.yml down -v
```

## Management & Troubleshooting

### Connection Strings
- **Kafka**: `localhost:9092`
- **Redis**: `localhost:6379`
- **Zookeeper**: `localhost:2181`

### Verifying Service Health

#### Redis
Check if Redis is responsive:
```bash
docker exec -it docker-redis-1 redis-cli ping
```
*(Expected output: `PONG`)*

#### Kafka
List topics to verify Kafka is functional:
```bash
docker exec -it docker-kafka-1 kafka-topics --list --bootstrap-server localhost:9092
```

### Common Issues
- **Command Not Found**: On macOS, if `docker` or `colima` are not in your path, ensure `/opt/homebrew/bin` is in your `$PATH` or use absolute paths.
- **Context Mismatch**: If you have both Docker Desktop and Colima, ensure you are using the correct context:
  ```bash
  docker context use colima
  ```
- **Zookeeper Dependency**: Kafka will fail to start if Zookeeper is not reachable on port 2181. If Kafka logs show connection errors, check the Zookeeper container logs first.
