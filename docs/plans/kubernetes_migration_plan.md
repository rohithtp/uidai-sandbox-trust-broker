# Kubernetes Migration Plan — UIDAI Sandbox Trust Broker

## 1. Current State Analysis

### What Exists Today

| Component | How It Runs | Configuration |
|---|---|---|
| **Zookeeper** | Docker Compose (`confluentinc/cp-zookeeper:7.5.0`) | Port 2181 |
| **Kafka** | Docker Compose (`confluentinc/cp-kafka:7.5.0`) | Port 9092, depends on Zookeeper |
| **Redis** | Docker Compose (`redis:7.2-alpine`) | Port 6379, AOF persistence |
| **Gateway Service** | `mvn spring-boot:run` (bare metal) | Port 8081, connects to Kafka @ `localhost:9092` |
| **Token Service** | `mvn spring-boot:run` (bare metal) | Port 8082, connects to Kafka + Redis @ `localhost` |

### Key Observation
The Java application services are **not containerised** — only the infrastructure (Kafka, Zookeeper, Redis) runs in Docker. The migration therefore has two parts:
1. **Containerise** the two Spring Boot services (Dockerfiles).
2. **Orchestrate everything** in Kubernetes manifests.

---

## 2. Target Architecture

```
┌─────────────────────────────── Kubernetes Cluster ───────────────────────────────┐
│                                                                                  │
│  Namespace: uidai-sandbox                                                        │
│                                                                                  │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐                         │
│  │  Zookeeper   │   │    Kafka     │   │    Redis     │                         │
│  │  StatefulSet │◄──│  StatefulSet │   │  Deployment  │                         │
│  │  Port: 2181  │   │  Port: 9092  │   │  Port: 6379  │                         │
│  │  + PVC       │   │  + PVC       │   │  + PVC       │                         │
│  └──────┬───────┘   └──────┬───────┘   └──────┬───────┘                         │
│         │                  │                   │                                  │
│         │    ClusterIP Services (internal DNS)  │                                 │
│         │                  │                   │                                  │
│  ┌──────┴──────────────────┴───────────────────┴──────┐                          │
│  │                  ConfigMap                         │                          │
│  │  KAFKA_BOOTSTRAP=kafka-svc:9092                    │                          │
│  │  REDIS_HOST=redis-svc  REDIS_PORT=6379             │                          │
│  └────────────────────┬───────────────────────────────┘                          │
│                       │                                                          │
│  ┌────────────────────┴────────────────────┐                                     │
│  │          Gateway Service                │                                     │
│  │          Deployment (replicas: 2)       │                                     │
│  │          Port: 8081                     │                                     │
│  └─────────────────┬──────────────────────┘                                     │
│                    │                                                             │
│  ┌─────────────────┴──────────────────────┐                                     │
│  │     Token Verification Service         │                                     │
│  │     Deployment (replicas: 2)           │                                     │
│  │     Port: 8082                         │                                     │
│  └────────────────────────────────────────┘                                     │
│                                                                                  │
│  ┌────────────────────────────────────────┐                                      │
│  │          Ingress Controller            │                                      │
│  │  /api/v1/gateway/* → gateway-svc:8081  │                                      │
│  │  /api/v1/token/*   → token-svc:8082    │                                      │
│  └────────────────────────────────────────┘                                      │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. New Files & Directory Structure

```
k8s/                                          ← NEW top-level directory
├── namespace.yaml
├── configmap.yaml
├── infrastructure/
│   ├── zookeeper-statefulset.yaml
│   ├── zookeeper-service.yaml
│   ├── kafka-statefulset.yaml
│   ├── kafka-service.yaml
│   ├── redis-deployment.yaml
│   └── redis-service.yaml
├── services/
│   ├── gateway-deployment.yaml
│   ├── gateway-service.yaml
│   ├── token-deployment.yaml
│   └── token-service.yaml
├── ingress.yaml
└── kustomization.yaml                        ← Ties everything together

interoperability-gateway-service/
└── Dockerfile                                ← NEW

token-verification-and-translation-service/
└── Dockerfile                                ← NEW
```

---

## 4. Changes Required — File by File

### Phase 1: Containerise the Spring Boot Services

#### 4.1 `interoperability-gateway-service/Dockerfile` (NEW)

```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/interoperability-gateway-service-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### 4.2 `token-verification-and-translation-service/Dockerfile` (NEW)

```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/token-verification-and-translation-service-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
```

> [!TIP]
> Consider adding a multi-stage build later to compile inside Docker, removing the need for a local Maven install.

---

### Phase 2: Application Configuration Changes

#### 4.3 `interoperability-gateway-service/src/main/resources/application.properties`

```diff
 spring.application.name=interoperability-gateway-service
 server.port=8081

 # Kafka Configuration
-spring.kafka.bootstrap-servers=localhost:9092
+spring.kafka.bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
 spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
 spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
+
+# Redis Configuration (env-driven)
+spring.data.redis.host=${REDIS_HOST:localhost}
+spring.data.redis.port=${REDIS_PORT:6379}
```

#### 4.4 `token-verification-and-translation-service/src/main/resources/application.properties`

```diff
 spring.application.name=token-verification-and-translation-service
 server.port=8082

 # Kafka Configuration
-spring.kafka.bootstrap-servers=localhost:9092
+spring.kafka.bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
 spring.kafka.consumer.group-id=token-verification-group
 spring.kafka.consumer.auto-offset-reset=earliest

 # Redis Configuration
-spring.data.redis.host=localhost
-spring.data.redis.port=6379
+spring.data.redis.host=${REDIS_HOST:localhost}
+spring.data.redis.port=${REDIS_PORT:6379}

 # JWKS Configuration
-app.security.jwks-uri=https://example.com/.well-known/jwks.json
+app.security.jwks-uri=${JWKS_URI:https://example.com/.well-known/jwks.json}
```

> [!IMPORTANT]
> Using `${ENV_VAR:default}` syntax keeps local `mvn spring-boot:run` working while allowing K8s ConfigMaps/env vars to override.

---

### Phase 3: Kubernetes Manifests

#### 4.5 `k8s/namespace.yaml`

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: uidai-sandbox
  labels:
    app.kubernetes.io/part-of: uidai-trust-broker
```

#### 4.6 `k8s/configmap.yaml`

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: trust-broker-config
  namespace: uidai-sandbox
data:
  KAFKA_BOOTSTRAP_SERVERS: "kafka-svc:9092"
  REDIS_HOST: "redis-svc"
  REDIS_PORT: "6379"
  JWKS_URI: "https://example.com/.well-known/jwks.json"
```

#### 4.7 `k8s/infrastructure/zookeeper-statefulset.yaml`

```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: zookeeper
  namespace: uidai-sandbox
spec:
  serviceName: zookeeper-svc
  replicas: 1
  selector:
    matchLabels:
      app: zookeeper
  template:
    metadata:
      labels:
        app: zookeeper
    spec:
      containers:
        - name: zookeeper
          image: confluentinc/cp-zookeeper:7.5.0
          ports:
            - containerPort: 2181
          env:
            - name: ZOOKEEPER_CLIENT_PORT
              value: "2181"
            - name: ZOOKEEPER_TICK_TIME
              value: "2000"
          volumeMounts:
            - name: zk-data
              mountPath: /var/lib/zookeeper/data
  volumeClaimTemplates:
    - metadata:
        name: zk-data
      spec:
        accessModes: ["ReadWriteOnce"]
        resources:
          requests:
            storage: 1Gi
```

#### 4.8 `k8s/infrastructure/zookeeper-service.yaml`

```yaml
apiVersion: v1
kind: Service
metadata:
  name: zookeeper-svc
  namespace: uidai-sandbox
spec:
  clusterIP: None          # Headless for StatefulSet
  selector:
    app: zookeeper
  ports:
    - port: 2181
      targetPort: 2181
```

#### 4.9 `k8s/infrastructure/kafka-statefulset.yaml`

```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: kafka
  namespace: uidai-sandbox
spec:
  serviceName: kafka-svc
  replicas: 1
  selector:
    matchLabels:
      app: kafka
  template:
    metadata:
      labels:
        app: kafka
    spec:
      containers:
        - name: kafka
          image: confluentinc/cp-kafka:7.5.0
          ports:
            - containerPort: 9092
          env:
            - name: KAFKA_BROKER_ID
              value: "1"
            - name: KAFKA_ZOOKEEPER_CONNECT
              value: "zookeeper-svc:2181"
            - name: KAFKA_ADVERTISED_LISTENERS
              value: "PLAINTEXT://kafka-svc:9092"
            - name: KAFKA_LISTENER_SECURITY_PROTOCOL_MAP
              value: "PLAINTEXT:PLAINTEXT"
            - name: KAFKA_INTER_BROKER_LISTENER_NAME
              value: "PLAINTEXT"
            - name: KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR
              value: "1"
          volumeMounts:
            - name: kafka-data
              mountPath: /var/lib/kafka/data
  volumeClaimTemplates:
    - metadata:
        name: kafka-data
      spec:
        accessModes: ["ReadWriteOnce"]
        resources:
          requests:
            storage: 5Gi
```

> [!NOTE]
> Key difference from Docker Compose: `KAFKA_ADVERTISED_LISTENERS` changes from `localhost:9092` → `kafka-svc:9092` so services resolve via K8s DNS.

#### 4.10 `k8s/infrastructure/kafka-service.yaml`

```yaml
apiVersion: v1
kind: Service
metadata:
  name: kafka-svc
  namespace: uidai-sandbox
spec:
  clusterIP: None
  selector:
    app: kafka
  ports:
    - port: 9092
      targetPort: 9092
```

#### 4.11 `k8s/infrastructure/redis-deployment.yaml`

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: redis
  namespace: uidai-sandbox
spec:
  replicas: 1
  selector:
    matchLabels:
      app: redis
  template:
    metadata:
      labels:
        app: redis
    spec:
      containers:
        - name: redis
          image: redis:7.2-alpine
          command: ["redis-server", "--save", "60", "1", "--loglevel", "warning"]
          ports:
            - containerPort: 6379
          volumeMounts:
            - name: redis-data
              mountPath: /data
      volumes:
        - name: redis-data
          persistentVolumeClaim:
            claimName: redis-pvc
---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: redis-pvc
  namespace: uidai-sandbox
spec:
  accessModes: ["ReadWriteOnce"]
  resources:
    requests:
      storage: 1Gi
```

#### 4.12 `k8s/infrastructure/redis-service.yaml`

```yaml
apiVersion: v1
kind: Service
metadata:
  name: redis-svc
  namespace: uidai-sandbox
spec:
  selector:
    app: redis
  ports:
    - port: 6379
      targetPort: 6379
```

#### 4.13 `k8s/services/gateway-deployment.yaml`

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: gateway
  namespace: uidai-sandbox
spec:
  replicas: 2
  selector:
    matchLabels:
      app: gateway
  template:
    metadata:
      labels:
        app: gateway
    spec:
      containers:
        - name: gateway
          image: uidai/interoperability-gateway-service:1.0.0-SNAPSHOT
          ports:
            - containerPort: 8081
          envFrom:
            - configMapRef:
                name: trust-broker-config
          readinessProbe:
            httpGet:
              path: /api/v1/gateway/health
              port: 8081
            initialDelaySeconds: 30
            periodSeconds: 10
          livenessProbe:
            httpGet:
              path: /api/v1/gateway/health
              port: 8081
            initialDelaySeconds: 60
            periodSeconds: 15
          resources:
            requests:
              memory: "256Mi"
              cpu: "250m"
            limits:
              memory: "512Mi"
              cpu: "500m"
```

#### 4.14 `k8s/services/gateway-service.yaml`

```yaml
apiVersion: v1
kind: Service
metadata:
  name: gateway-svc
  namespace: uidai-sandbox
spec:
  selector:
    app: gateway
  ports:
    - port: 8081
      targetPort: 8081
```

#### 4.15 `k8s/services/token-deployment.yaml`

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: token-service
  namespace: uidai-sandbox
spec:
  replicas: 2
  selector:
    matchLabels:
      app: token-service
  template:
    metadata:
      labels:
        app: token-service
    spec:
      containers:
        - name: token-service
          image: uidai/token-verification-service:1.0.0-SNAPSHOT
          ports:
            - containerPort: 8082
          envFrom:
            - configMapRef:
                name: trust-broker-config
          readinessProbe:
            httpGet:
              path: /actuator/health
              port: 8082
            initialDelaySeconds: 30
            periodSeconds: 10
          livenessProbe:
            httpGet:
              path: /actuator/health
              port: 8082
            initialDelaySeconds: 60
            periodSeconds: 15
          resources:
            requests:
              memory: "256Mi"
              cpu: "250m"
            limits:
              memory: "512Mi"
              cpu: "500m"
```

#### 4.16 `k8s/services/token-service.yaml`

```yaml
apiVersion: v1
kind: Service
metadata:
  name: token-svc
  namespace: uidai-sandbox
spec:
  selector:
    app: token-service
  ports:
    - port: 8082
      targetPort: 8082
```

#### 4.17 `k8s/ingress.yaml`

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: trust-broker-ingress
  namespace: uidai-sandbox
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
spec:
  ingressClassName: nginx
  rules:
    - host: trust-broker.local
      http:
        paths:
          - path: /api/v1/gateway
            pathType: Prefix
            backend:
              service:
                name: gateway-svc
                port:
                  number: 8081
          - path: /api/v1/token
            pathType: Prefix
            backend:
              service:
                name: token-svc
                port:
                  number: 8082
```

#### 4.18 `k8s/kustomization.yaml`

```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
namespace: uidai-sandbox

resources:
  - namespace.yaml
  - configmap.yaml
  - infrastructure/zookeeper-statefulset.yaml
  - infrastructure/zookeeper-service.yaml
  - infrastructure/kafka-statefulset.yaml
  - infrastructure/kafka-service.yaml
  - infrastructure/redis-deployment.yaml
  - infrastructure/redis-service.yaml
  - services/gateway-deployment.yaml
  - services/gateway-service.yaml
  - services/token-deployment.yaml
  - services/token-service.yaml
  - ingress.yaml
```

---

## 5. Summary of Changes to Existing Files

| File | Change Type | What Changes |
|---|---|---|
| `interoperability-gateway-service/application.properties` | **Modify** | Externalise Kafka/Redis hosts via `${ENV_VAR:default}` |
| `token-verification-and-translation-service/application.properties` | **Modify** | Externalise Kafka/Redis/JWKS hosts via `${ENV_VAR:default}` |
| `docker/docker-compose.yml` | **Keep** | Retain for local dev; no deletion needed |
| `docs/infrastructure-management.md` | **Update** | Add Kubernetes section alongside Docker instructions |
| `README.md` | **Update** | Add K8s deployment instructions |
| `e2e_test.sh` | **Update** | Add K8s-aware base URL option (via env var or port-forward) |

---

## 6. Deployment Commands

```bash
# Build JARs
mvn clean package -DskipTests

# Build Docker images
docker build -t uidai/interoperability-gateway-service:1.0.0-SNAPSHOT \
  interoperability-gateway-service/

docker build -t uidai/token-verification-service:1.0.0-SNAPSHOT \
  token-verification-and-translation-service/

# Deploy to K8s (using Kustomize)
kubectl apply -k k8s/

# Verify
kubectl -n uidai-sandbox get pods
kubectl -n uidai-sandbox get svc

# Port-forward for local testing
kubectl -n uidai-sandbox port-forward svc/gateway-svc 8081:8081
kubectl -n uidai-sandbox port-forward svc/token-svc 8082:8082
```

---

## 7. Phased Execution Roadmap

| Phase | Tasks | Effort |
|---|---|---|
| **1 — Containerise** | Create Dockerfiles, externalise config, verify images build | ~2 hrs |
| **2 — K8s Infrastructure** | Write Zookeeper/Kafka/Redis manifests, test on Minikube | ~3 hrs |
| **3 — K8s Services** | Write Gateway/Token Deployment manifests, wire ConfigMap | ~2 hrs |
| **4 — Networking** | Add Ingress, test E2E via port-forward | ~1 hr |
| **5 — Polish** | Update docs, add health probes, resource limits, Kustomize | ~2 hrs |

**Total estimated effort: ~10 hours**

---

## 8. Open Questions for You

1. **Local K8s runtime** — Do you want to use **Minikube**, **kind**, or **Colima with K8s**?
2. **Container registry** — Push images to Docker Hub, a private registry, or load directly into the local cluster?
3. **Helm charts** — Want me to structure this as a Helm chart instead of raw manifests + Kustomize?
4. **KRaft mode** — Kafka 3.5+ supports KRaft (no Zookeeper). Want to drop Zookeeper entirely?
5. **Secrets** — Any sensitive config (JWKS keys, Redis passwords) that should use K8s Secrets instead of ConfigMap?
