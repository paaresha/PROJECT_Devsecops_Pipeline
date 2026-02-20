# KubeFlow Ops — Phase-Wise DevOps/SRE Walkthrough

> **Perspective**: Senior DevOps / Site Reliability Engineer (SRE) / Platform Engineer  
> **Project**: `c:\PROJECTS\kubeflow-ops`  
> **Stack**: Python FastAPI → Docker → ECR → Terraform (AWS) → EKS → ArgoCD → Prometheus/Grafana/Loki/Tempo → Kyverno → External Secrets

---

## Why This Architecture? (The "North Star")

Before diving into phases, understand the **problem this architecture solves**:

| Problem | Solution Used |
|---|---|
| "It works on my machine" | Docker containers — identical runtime everywhere |
| Manual infra setup = human error | Terraform IaC — infra is code, versioned, repeatable |
| Manual deployments = risky, slow | GitHub Actions CI + ArgoCD CD — fully automated pipeline |
| Cluster state drift (someone did `kubectl edit`) | ArgoCD `selfHeal: true` — Git is truth, always |
| Secrets in plaintext in Git | External Secrets Operator — pulls from AWS Secrets Manager |
| Anyone can push a bad image | Kyverno policies + Trivy scan — enforced at admission |
| "Is the system healthy?" → check logs manually | Prometheus + Grafana + Loki + Tempo — observe everything |
| App crashes at scale → manual node provision | Karpenter — auto-provisions right-sized EC2 in seconds |

This is not a tutorial project — it mirrors what teams at **Netflix, Airbnb, and Spotify actually run**.

---

## Phase 1 — Microservices (Application Layer)

### What Was Built

Three Python FastAPI services simulating a real e-commerce backend:

```
order-service   ──REST──▶  user-service    (validates user before order creation)
order-service   ──SQS ──▶  notification-service  (event-driven, async)
notification-service ──▶  Redis (deduplication cache)
```

### Why FastAPI?

- **Async by default** → handles concurrent requests without blocking (vital for microservices)
- **Auto-generates OpenAPI docs** at `/docs` → self-documenting, great for interviews
- **Type-safe with Pydantic** → catches schema errors at startup, not at runtime

### Key Files

```
apps/
├── order-service/
│   ├── main.py           # CRUD endpoints + SQS publisher + health probe
│   ├── Dockerfile        # Multi-stage build
│   ├── requirements.txt
│   └── tests/test_main.py
├── user-service/         # CRUD + REST inter-service call
└── notification-service/ # SQS consumer + Redis dedup
```

### DevOps/SRE Perspective: What to Say in Interviews

**"Why three services instead of one monolith?"**
> Each service has its own **failure domain**. If `notification-service` goes down, orders still get created and users still register. With a monolith, one bug in email code can crash your entire API. This is the fundamental reason microservices exist.

**"How do they communicate?"**
>
> - **Synchronous**: `order-service` calls `user-service` via REST (HTTP). If the user doesn't exist, the order is rejected immediately.
> - **Asynchronous**: `order-service` publishes to SQS. `notification-service` polls and processes. This is fire-and-forget — order creation isn't blocked by email sending.

**"What is the health probe at `/health`?"**
> Kubernetes calls this endpoint every N seconds. If it returns non-200, Kubernetes marks the pod as unhealthy and stops routing traffic to it. It's the contract between your app and Kubernetes.

### Multi-Stage Dockerfile — Why It Matters

```dockerfile
# Stage 1: Build stage (has pip, compilers, dev tools)
FROM python:3.12-slim AS builder
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Stage 2: Runtime stage (minimal, no build tools)
FROM python:3.12-slim
COPY --from=builder /app /app
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8001"]
```

**SRE insight**: The final image has NO pip, NO compilers, NO shell tools — dramatically smaller attack surface. An attacker who gets RCE in your container can't install tools easily.

### docker-compose.yml — Local Development Loop

```
Services:   order-service | user-service | notification-service
Backing:    postgres | redis | localstack (fake SQS)
```

- Developers run `docker-compose up` and have the full stack locally in ~30 seconds
- No AWS credentials needed for local dev
- **LocalStack** fakes AWS services (SQS, S3) — this is how AWS-native apps are tested locally

---

## Phase 2 — Terraform (Infrastructure as Code)

### What Was Built

6 modular Terraform modules provisioning the entire AWS foundation:

```
VPC (network boundary)
 └─ EKS (Kubernetes control plane + worker nodes)
 └─ RDS (PostgreSQL — persistent storage)
 └─ ElastiCache (Redis — caching layer)
 └─ SQS (async message queue)
 └─ ECR (Docker image registry)
```

### The Module Pattern — Why It's Important

```
terraform/
├── modules/           # Reusable, environment-agnostic
│   ├── vpc/
│   ├── eks/
│   ├── ecr/
│   ├── rds/
│   ├── elasticache/
│   └── sqs/
└── environments/
    └── dev/           # Calls modules with dev-specific sizes
        ├── main.tf
        ├── backend.tf
        ├── variables.tf
        └── outputs.tf
```

**The discipline**: Modules have zero hardcoded values. Everything is a variable. You can call the same `eks` module with `instance_type = "t3.medium"` for dev and `instance_type = "m5.xlarge"` for prod. One codebase, multiple environments.

### Module: VPC (The Network Foundation)

```
VPC (10.0.0.0/16)
├── Public Subnets:  10.0.1.0/24, 10.0.2.0/24  (us-east-1a, us-east-1b)
│   └── NAT Gateway (outbound internet for private subnets)
└── Private Subnets: 10.0.3.0/24, 10.0.4.0/24
    ├── EKS Worker Nodes  (never directly exposed to internet)
    ├── RDS PostgreSQL
    └── ElastiCache Redis
```

**SRE insight — Why private subnets?**
> Your databases and compute MUST NOT be reachable from the internet directly. Public subnets are only for resources that NEED to receive inbound internet traffic (load balancers). Everything else lives in private subnets. This is Defense in Depth — an attacker must breach multiple network layers.

**NAT Gateway**: Allows private subnet resources to make OUTBOUND calls (pull Docker images, call AWS APIs) without being directly reachable from the internet. Single NAT Gateway is used here for cost (a 3-AZ production setup would have one per AZ for HA).

### Module: EKS — The Brain

**Key concepts built in:**

1. **OIDC Provider for IRSA** (IAM Roles for Service Accounts)
   - This is the **most important security concept** in EKS
   - Without IRSA: You give each EC2 node an IAM role → every pod on that node inherits ALL permissions
   - With IRSA: Each Kubernetes ServiceAccount gets its OWN IAM role with ONLY the permissions it needs
   - `order-service` can write to SQS but NOT read RDS credentials. `notification-service` can read SQS but NOT touch ECR.
   - **This is Principle of Least Privilege applied to Kubernetes**

2. **Managed Node Group**
   - AWS manages the EC2 lifecycle (patching, replacement)
   - You define: instance type, min/max/desired count, disk size
   - `t3.medium` nodes for dev — 2 vCPU, 4GB RAM, ~$0.04/hour

3. **EKS Control Plane Logging**
   - API server, authenticator, scheduler logs → CloudWatch
   - You can audit: who deployed what, when, from where

### Module: RDS

```hcl
engine                  = "postgres"
engine_version          = "15.4"
instance_class          = "db.t3.micro"
multi_az                = false          # dev; true for prod
deletion_protection     = false          # dev; true for prod
backup_retention_period = 7              # 7 days of automated backups
```

**SRE insight**: The `deletion_protection = true` in production means even `terraform destroy` won't delete the database without manual intervention. This has saved countless production databases from accidental destruction.

### Module: SQS — Message Queue + Dead Letter Queue

```
order-queue (main queue)
  └── max_receive_count = 3
  └── After 3 failed processing attempts → DLQ

order-dlq (dead letter queue)
  └── Holds failed messages for inspection/replay
  └── retention: 14 days
```

**SRE insight**: The DLQ is your safety net. Without it, a bad message causes infinite retries, burning processing capacity. With DLQ, bad messages are quarantined. You can inspect them, fix the bug, and replay messages from DLQ to the main queue.

### Remote State Backend

```hcl
# backend.tf
terraform {
  backend "s3" {
    bucket         = "kubeflow-ops-terraform-state"
    key            = "dev/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "kubeflow-ops-terraform-lock"
    encrypt        = true
  }
}
```

**Why this is critical:**

- `terraform.tfstate` contains EVERY resource ID, IP, and secret Terraform manages
- If stored locally: team members overwrite each other's state → catastrophic state corruption
- **S3**: Versioned, encrypted, durable storage for state file
- **DynamoDB**: Provides **state locking** — only one person/pipeline can run `terraform apply` at a time. If two people apply simultaneously, the second waits (or is rejected). This prevents race conditions.

### Terraform Workflow (The Discipline)

```bash
terraform init     # Download providers, set up backend
terraform plan     # Show what WILL change — ALWAYS review this
terraform apply    # Actually make changes
terraform destroy  # Tear everything down (for dev environments)
```

**The immutable infrastructure principle**: You never SSH into a server to fix something. You change the Terraform code and re-apply. The old resource is destroyed and a new one is created. This eliminates "snowflake servers" — servers that are alive so long nobody knows what's on them.

---

## Phase 3 — Kubernetes Manifests (GitOps Layer)

### What Was Built

Full Kubernetes manifests using **Kustomize** with base + overlays pattern:

```
gitops/apps/order-service/
├── base/
│   ├── deployment.yaml   # Core workload definition
│   ├── service.yaml      # ClusterIP for internal traffic
│   ├── hpa.yaml          # Horizontal Pod Autoscaler
│   └── kustomization.yaml
└── overlays/
    └── dev/
        └── patch-replicas.yaml  # Dev-specific overrides
```

### The Deployment: Every Field Explained

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
  namespace: kubeflow-ops
  labels:
    app: order-service       # Required by Kyverno policy
    team: platform           # Required by Kyverno policy
    env: dev                 # Required by Kyverno policy
spec:
  replicas: 2                # Always run 2+ for HA (not 1)
  selector:
    matchLabels:
      app: order-service
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: 0      # Never have 0 pods during update (zero downtime)
      maxSurge: 1            # May temporarily have 3 pods during rollout
  template:
    spec:
      affinity:
        podAntiAffinity:     # Force pods onto DIFFERENT nodes
          ...                # If node dies, you don't lose ALL replicas
      containers:
      - name: order-service
        resources:
          requests:          # Kubernetes scheduler uses this to PLACE the pod
            cpu: "100m"      # 0.1 CPU cores
            memory: "128Mi"
          limits:            # Hard ceiling — pod is OOMKilled if exceeded
            cpu: "500m"
            memory: "512Mi"
```

**SRE insight — Resources requests vs limits:**
> `requests` = what you BID for — Kubernetes uses this for scheduling decisions. `limits` = the hard cap. Setting requests too low causes pods to be scheduled onto full nodes → OOMKilled. Setting requests too high wastes capacity. Finding the right values requires load testing and observing actual usage in Prometheus.

### Three Health Probe Types — Why All Three?

```yaml
livenessProbe:           # Is the app alive? If NO → kill and restart
  httpGet:
    path: /health
    port: 8001
  initialDelaySeconds: 30   # Wait 30s before first check (app needs time to start)
  periodSeconds: 10

readinessProbe:          # Is the app READY to receive traffic? If NO → remove from Service
  httpGet:
    path: /health
    port: 8001
  initialDelaySeconds: 10
  periodSeconds: 5

startupProbe:            # Gives app time to start (overrides liveness during startup)
  httpGet:
    path: /health
    port: 8001
  failureThreshold: 30   # 30 * 10s = 5 minutes to start (for slow-starting apps)
  periodSeconds: 10
```

**The critical difference**:

- `liveness` failure → pod RESTARTS (fixes deadlock, memory corruption)
- `readiness` failure → pod stays alive but gets NO traffic (deploys complete but don't send bad traffic)
- `startup` → prevents liveness from killing a slow-starting pod prematurely

### HPA — Horizontal Pod Autoscaler

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
spec:
  scaleTargetRef:
    kind: Deployment
    name: order-service
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70     # Scale up when avg CPU > 70%
```

**SRE insight**: HPA watches live CPU/memory metrics from the Metrics Server. When average CPU across all pods > 70%, it adds pods. When load decreases, it scales back down. This handles traffic spikes automatically. Combined with **Karpenter** (Phase 7), new pods AND new nodes are provisioned automatically.

### Kustomize — Why Not Plain YAML?

```bash
# Without Kustomize: copy-paste 300 lines, change 3 values, forget one → prod bug
# With Kustomize:
kubectl kustomize gitops/apps/order-service/overlays/dev/ | kubectl apply -f -
```

Base has common config. Overlays patch ONLY what differs. Dev uses 1 replica, lower resources. Prod uses 3 replicas, higher limits. One source of truth, no duplication.

---

## Phase 4 — GitHub Actions CI (Continuous Integration)

### What Was Built

Two pipelines in `.github/workflows/`:

1. `ci.yml` — Triggered on every push to `apps/**`
2. `terraform.yml` — Triggered on every push to `terraform/**`

### CI Pipeline: Every Step Explained

```yaml
name: CI Pipeline
on:
  push:
    paths:
      - 'apps/**'          # Only triggers when app code changes
```

**Step 1: Checkout**

```yaml
- uses: actions/checkout@v4
```

Clones the repo into the runner. v4 = latest, pinned for reproducibility.

**Step 2: Run Unit Tests**

```yaml
- name: Run Tests
  run: |
    pip install -r requirements.txt
    pytest tests/ -v --tb=short
  working-directory: apps/order-service
```

If tests fail, the pipeline STOPS. No bad code ever reaches ECR. This is the **first quality gate**.

**Step 3: Trivy Source Scan**

```yaml
- name: Scan Source Code
  uses: aquasecurity/trivy-action@master
  with:
    scan-type: fs
    scan-ref: apps/order-service/
    severity: HIGH,CRITICAL
    exit-code: 1            # Fail pipeline if HIGH/CRITICAL CVE found
```

Trivy scans your `requirements.txt` and `Dockerfile` for known vulnerabilities BEFORE building. **Shift-left security** — catch CVEs at development time, not in production.

**Step 4: Build Docker Image**

```yaml
- name: Build Image
  run: docker build -t order-service:${{ github.sha }} apps/order-service/
```

Tag with `github.sha` (the git commit hash) — every image is perfectly traceable to its source code. If something breaks in production, you know EXACTLY which commit caused it.

**Step 5: Trivy Image Scan**

```yaml
- name: Scan Docker Image
  uses: aquasecurity/trivy-action@master
  with:
    scan-type: image
    image-ref: order-service:${{ github.sha }}
    severity: CRITICAL
    exit-code: 1
```

Scans the BUILT image — catches vulnerabilities in OS packages, base image, runtime deps. A second security gate.

**Step 6: OIDC Authentication to AWS** ← Most important step

```yaml
- name: Configure AWS Credentials
  uses: aws-actions/configure-aws-credentials@v4
  with:
    role-to-assume: arn:aws:iam::${{ secrets.AWS_ACCOUNT_ID }}:role/github-actions-ecr-role
    aws-region: us-east-1
```

**SRE/Security insight — Why OIDC and not Access Keys?**
> Old approach: Store `AWS_ACCESS_KEY_ID` + `AWS_SECRET_ACCESS_KEY` in GitHub Secrets. Problem: Long-lived credentials that can be stolen, rotated manually, accidentally logged.
>
> OIDC approach: GitHub acts as an Identity Provider. AWS trusts GitHub's signed JWT token. The pipeline **assumes an IAM role** for the duration of the job only. No keys stored anywhere. Token expires when the job ends. This is the modern, AWS-recommended approach.

**Step 7: Push to ECR**

```yaml
- name: Login to ECR
  uses: aws-actions/amazon-ecr-login@v2

- name: Push to ECR
  run: |
    docker tag order-service:${{ github.sha }} ${{ env.ECR_REGISTRY }}/order-service:${{ github.sha }}
    docker push ${{ env.ECR_REGISTRY }}/order-service:${{ github.sha }}
```

**Step 8: Update GitOps Manifest** ← The bridge between CI and CD

```yaml
- name: Update Image Tag in GitOps
  run: |
    sed -i "s|image: .*order-service:.*|image: $ECR_REGISTRY/order-service:${{ github.sha }}|g" \
      gitops/apps/order-service/base/deployment.yaml
    git config user.name "github-actions"
    git add gitops/
    git commit -m "ci: update order-service to ${{ github.sha }}"
    git push
```

This is the **critical handoff**: CI pushed an image, now it updates the YAML in Git to point to that new image. ArgoCD (Phase 5) detects this Git change and deploys it to the cluster. **No CI pipeline ever touches the cluster directly**. Git is the only way things change.

### Terraform Pipeline

```yaml
terraform fmt -check   # Enforce formatting standards
terraform validate     # Syntax and schema check
terraform plan         # Show what will change
# Manual approval required for apply (required_reviewers: 1)
terraform apply
```

**The `plan` step is critical** — it's a dry run that shows every resource to be created, changed, or destroyed. Reviewers must approve the plan before apply runs. This prevents "I didn't know terraform would delete the database."

---

## Phase 5 — ArgoCD (GitOps Continuous Deployment)

### What ArgoCD Does (Mental Model)

```
Git Repo (desired state)  ◄──── GitHub Actions updates image tag
        │
        │ ArgoCD watches every 3 minutes (or webhook)
        ▼
EKS Cluster (actual state)

ArgoCD compares desired vs actual.
If they differ → ArgoCD syncs (applies) the diff.
If someone manually edits the cluster → ArgoCD reverts it.
```

### App-of-Apps Pattern

```yaml
# gitops/platform/argocd/app-of-apps.yaml
# This is ONE ArgoCD Application that manages ALL other Applications

apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: app-of-apps
  namespace: argocd
spec:
  source:
    repoURL: https://github.com/YourOrg/kubeflow-ops
    targetRevision: main
    path: gitops/platform/argocd/applications   # Watches this directory
  destination:
    server: https://kubernetes.default.svc
    namespace: argocd
  syncPolicy:
    automated:
      selfHeal: true      # Revert manual cluster changes
      prune: true         # Delete resources removed from Git
```

**Why App-of-Apps?** You apply ONE manifest to bootstrap everything. ArgoCD discovers ALL other Application manifests in the `applications/` folder and deploys them all. Adding a new service = add one Application YAML to that folder. ArgoCD handles the rest.

### Per-Service Application (Example: order-service)

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: order-service
  namespace: argocd
spec:
  project: default
  source:
    repoURL: https://github.com/YourOrg/kubeflow-ops
    targetRevision: main
    path: gitops/apps/order-service/overlays/dev
  destination:
    server: https://kubernetes.default.svc
    namespace: kubeflow-ops
  syncPolicy:
    automated:
      selfHeal: true
      prune: true
    syncOptions:
      - CreateNamespace=true
      - ApplyOutOfSyncOnly=true
```

### Full Deployment Flow (End-to-End)

```
1. Developer pushes code to apps/order-service/main.py
2. GitHub Actions triggers (path filter: apps/**)
3. CI runs: pytest → trivy scan → docker build → trivy image scan
4. CI pushes image to ECR: 123456789.dkr.ecr.us-east-1.amazonaws.com/order-service:abc1234
5. CI updates gitops/apps/order-service/base/deployment.yaml with new tag
6. CI commits and pushes this YAML change to Git
7. ArgoCD detects diff between Git (new tag) and cluster (old tag)
8. ArgoCD applies the updated deployment.yaml to EKS
9. Kubernetes performs a RollingUpdate: new pods start → health checks pass → old pods terminate
10. New code is live. Zero manual steps. Zero kubectl commands in CI.
```

**SRE interview statement:**
> "In our GitOps model, Git is the single source of truth for cluster state. The only way to change what runs in production is to merge a PR. This gives us a complete audit trail — every deployment is a Git commit, with a timestamp, author, and description. Rolling back is `git revert`."

---

## Phase 6 — Observability Stack (The Four Pillars)

### What Was Built

```
Metrics:  Prometheus (scrapes) + Grafana (visualizes) + Alertmanager (fires alerts)
Logs:     Loki (stores) + Promtail (collects from pods)
Traces:   Grafana Tempo (stores distributed traces)
Events:   Kubernetes events → also queryable
```

All deployed as ArgoCD Applications → Helm charts → GitOps managed.

### Prometheus — How It Works

```
Prometheus Server
  │
  ├── Scrapes /metrics endpoints every 15s:
  │     ├── kube-state-metrics (pod states, deployment status)
  │     ├── node-exporter (CPU, memory, disk per EC2 node)
  │     ├── FastAPI /metrics (request rate, latency, error count)
  │     └── JVM/Go/custom metrics from any pod
  │
  └── Stores time-series data (15-day retention by default)
         └── Query language: PromQL
```

**PromQL examples you should know:**

```promql
# Error rate for order-service (RED method: Errors)
rate(http_requests_total{service="order-service", status=~"5.."}[5m])
/
rate(http_requests_total{service="order-service"}[5m])

# P99 latency (RED method: Duration)
histogram_quantile(0.99,
  rate(http_request_duration_seconds_bucket{service="order-service"}[5m])
)

# Pod restarts (symptom of crash loops)
increase(kube_pod_container_status_restarts_total[1h]) > 5
```

### Alert Rules — The Five Most Important

```yaml
# gitops/platform/prometheus/alert-rules.yaml

- alert: HighErrorRate
  expr: |
    rate(http_requests_total{status=~"5.."}[5m])
    / rate(http_requests_total[5m]) > 0.05
  for: 5m
  severity: critical
  # If >5% of requests error for >5 minutes → page the on-call

- alert: PodCrashLooping
  expr: increase(kube_pod_container_status_restarts_total[1h]) > 5
  for: 5m
  severity: critical
  # Pod restarted >5 times in an hour → something is broken

- alert: HighLatencyP99
  expr: |
    histogram_quantile(0.99,
      rate(http_request_duration_seconds_bucket[5m])
    ) > 2
  for: 5m
  severity: warning
  # 99th percentile latency > 2 seconds → degraded user experience

- alert: NodeDiskPressure
  expr: (node_filesystem_avail_bytes / node_filesystem_size_bytes) < 0.1
  for: 10m
  severity: warning
  # Less than 10% disk free on any node → will cause pod failures

- alert: DeploymentReplicasMismatch
  expr: |
    kube_deployment_status_replicas_available != kube_deployment_spec_replicas
  for: 5m
  severity: warning
  # Fewer running replicas than desired → check for OOMKills, evictions
```

### SLOs — The SRE Contract with Users

```markdown
# docs/slo-definitions.md

Order Service SLOs:
  Availability: 99.9% of requests return non-5xx (allows 43min downtime/month)
  Latency:      95% of requests complete in <500ms
  Error Budget: 0.1% per month = 43 minutes of downtime allowed

If the error budget is consumed:
  - Freeze feature releases
  - Focus 100% on reliability
  - Conduct thorough blameless postmortem
```

**Why SLOs matter more than "uptime":**
> "Uptime" is binary. SLOs are nuanced. A service can be "up" but so slow it's unusable. SLOs capture: Is the service fast enough? Is it correct enough? This is what users actually care about, not whether a ping returns.

### Loki — Log Aggregation

```
Promtail DaemonSet (runs on EVERY node)
  └── Tails /var/log/pods/**/*.log
  └── Labels logs: {app="order-service", namespace="kubeflow-ops"}
  └── Ships to Loki

Loki stores logs indexed by labels (not content)
  └── Query via LogQL in Grafana
  └── Correlate with Prometheus metrics timestamps
```

**LogQL examples:**

```logql
# All logs from order-service in last 5 minutes
{app="order-service", namespace="kubeflow-ops"} |= "error"

# Error rate from logs (when you don't have metrics)
count_over_time({app="order-service"} |= "ERROR" [5m])
```

### Tempo — Distributed Tracing

```
Request comes in → order-service (trace starts)
  └── Calls user-service (child span)
  └── Publishes to SQS (child span)
       └── notification-service picks up (linked trace)

In Grafana → click on a slow request → see EXACTLY which call was slow
```

**SRE insight**: Metrics tell you WHAT is broken. Logs tell you WHY. Traces tell you WHERE in the call chain. All three are needed for fast incident resolution. The Grafana platform links them: click a spike in a Grafana graph → drill into Loki logs for that time window → see the trace ID in the log → jump to the Tempo trace.

---

## Phase 7 — Platform Tools (Security & Reliability)

### Kyverno — Policy as Code

```yaml
# gitops/platform/kyverno/policies.yaml

# Policy 1: Block 'latest' image tag
apiVersion: kyverno.io/v1
kind: ClusterPolicy
metadata:
  name: disallow-latest-tag
spec:
  validationFailureAction: Enforce   # Block, don't just warn
  rules:
  - name: require-image-tag
    match:
      resources:
        kinds: [Pod]
    validate:
      message: "Image tag 'latest' is not allowed. Use a specific SHA tag."
      pattern:
        spec:
          containers:
          - image: "!*:latest"
```

**Why block `latest`?**
> `latest` is a moving target. `docker pull image:latest` today might pull a different image tomorrow. In production, this means a rollout could pick up an untested image. Using `image:sha256` or `image:abc1234` guarantees the exact binary that was tested is what runs.

```yaml
# Policy 2: Require resource limits
spec:
  rules:
  - validate:
      message: "Resource limits are required on all containers."
      pattern:
        spec:
          containers:
          - resources:
              limits:
                cpu: "?*"
                memory: "?*"
```

**Why?** Without limits, a buggy pod can consume all node CPU/memory, starving other pods. This policy prevents one bad deploy from taking down the entire node.

```yaml
# Policy 3: Block privileged containers
- validate:
    message: "Privileged containers are not allowed."
    pattern:
      spec:
        containers:
        - securityContext:
            privileged: false
```

**Why?** A privileged container has root access to the HOST node. If an attacker breaks out of the container, they own the node. This policy enforces container isolation.

### External Secrets Operator (ESO) — Secret Management

The problem:

```
# BAD: Secret in YAML committed to Git
apiVersion: v1
kind: Secret
data:
  DB_PASSWORD: cGFzc3dvcmQxMjM=  # base64 of "password123" → NOT encrypted, anyone with repo access sees it
```

The ESO solution:

```yaml
# gitops/platform/external-secrets/secrets.yaml

# Step 1: ClusterSecretStore — tell ESO where secrets live
apiVersion: external-secrets.io/v1beta1
kind: ClusterSecretStore
metadata:
  name: aws-secrets-manager
spec:
  provider:
    aws:
      service: SecretsManager
      region: us-east-1
      auth:
        jwt:
          serviceAccountRef:
            name: external-secrets-sa   # Uses IRSA — no AWS keys!

---
# Step 2: ExternalSecret — pull a specific secret into K8s
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: order-service-db
spec:
  refreshInterval: 1h                   # Re-sync from AWS every hour
  secretStoreRef:
    kind: ClusterSecretStore
    name: aws-secrets-manager
  target:
    name: order-service-db-secret       # Creates this Kubernetes Secret
  data:
  - secretKey: DB_PASSWORD
    remoteRef:
      key: kubeflow-ops/order-service/db   # AWS Secrets Manager path
      property: password
```

**Flow**: AWS Secrets Manager → ESO (via IRSA) → Kubernetes Secret → Pod mounts as env var. Actual secret value is NEVER in Git. Only the reference path is.

### Karpenter — Node Autoscaling

**Before Karpenter (Cluster Autoscaler):**

```
1. HPA scales pod from 5 → 10 replicas (load spike)
2. 5 pods are Pending (no node capacity)
3. Cluster Autoscaler detects → asks ASG to add nodes
4. ASG launches EC2 → bootstraps → joins cluster: 3-5 minutes
5. Pending pods are finally scheduled
```

**With Karpenter:**

```
1. HPA scales pod from 5 → 10 replicas
2. 5 pods are Pending
3. Karpenter detects pending pods immediately
4. Karpenter computes EXACT right-sized node (not just "another t3.medium")
5. EC2 launched, joins cluster: 60-90 seconds
6. Pending pods scheduled immediately
```

**Karpenter NodePool:**

```yaml
apiVersion: karpenter.sh/v1beta1
kind: NodePool
spec:
  template:
    spec:
      requirements:
      - key: karpenter.k8s.aws/instance-category
        operator: In
        values: ["c", "m", "r"]            # Compute, Memory, General purpose
      - key: karpenter.sh/capacity-type
        operator: In
        values: ["spot", "on-demand"]       # Prefer spot (80% cheaper)
  limits:
    cpu: "100"                              # Max 100 CPUs in cluster
  disruption:
    consolidationPolicy: WhenUnderutilized  # Bin-pack pods, delete empty nodes
    consolidateAfter: 30s
```

**SRE insight — Spot + Consolidation**: Karpenter prefers Spot instances (up to 80% cheaper). When load decreases, it consolidates pods onto fewer nodes and terminates idle nodes. This alone can reduce EC2 costs by 40-60% compared to a static node group.

---

## Phase 8 — Documentation (The SRE Artifacts)

### SLO Definitions — Engineering Reliability

```markdown
# docs/slo-definitions.md

## Order Service

### SLI (Service Level Indicator) — What we measure
- Availability: HTTP 2xx responses / Total responses
- Latency: P95 response time
- Error Rate: 5xx / Total requests

### SLO (Service Level Objective) — Our target
- Availability: ≥ 99.9% over 30-day rolling window
- Latency P95: ≤ 500ms
- Error Rate: ≤ 0.1%

### Error Budget — How much "failure" we can afford
- Availability budget: 43 minutes/month (0.1% of 30 days)
- When budget is 50% consumed → alert engineering team
- When budget is 100% consumed → freeze all feature work
```

### Runbook — Incident Response

```markdown
# docs/runbook.md

## Alert: PodCrashLooping

### 1. Understand (5 minutes)
   kubectl get pods -n kubeflow-ops
   kubectl logs <pod-name> -n kubeflow-ops --previous
   kubectl describe pod <pod-name> -n kubeflow-ops

### 2. Common Causes
   - OOMKilled: Memory limit too low → increase in deployment.yaml
   - Config error: Wrong env var → check ExternalSecret sync status
   - Dependency down: DB unavailable → check RDS status in console

### 3. Rollback (if new deploy caused it)
   kubectl rollout undo deployment/order-service -n kubeflow-ops
   # Or via ArgoCD: sync to previous commit SHA

### 4. Communicate
   - Post in #incidents Slack channel
   - Update status page
   - Begin blameless postmortem document
```

---

## The Complete Flow: A Developer's Monday Morning

Here's the ENTIRE system in one narrative:

```
1. Developer fixes a bug in order-service/main.py
   └── git push origin main

2. GitHub Actions triggers (path: apps/order-service/**)
   ├── pytest → 47 tests pass ✓
   ├── trivy fs scan → 0 HIGH CVEs ✓
   ├── docker build → image: order-service:3f2a1b8 ✓
   ├── trivy image scan → 0 CRITICAL CVEs ✓
   ├── ECR push → 123456789.dkr.ecr.us-east-1.amazonaws.com/order-service:3f2a1b8 ✓
   └── git commit: "ci: update order-service to 3f2a1b8" → Git ✓

3. ArgoCD detects changed deployment.yaml (3 min poll or webhook)
   └── Applies diff to EKS cluster

4. Kubernetes RollingUpdate begins
   ├── Pod order-service-new-xxx starts
   ├── startupProbe runs (waits up to 5 min for startup)
   ├── readinessProbe passes → pod added to Service endpoints
   ├── Old pod removed from Service endpoints
   └── Old pod gracefully terminates (SIGTERM → 30s → SIGKILL)

5. Prometheus scrapes new pod (after 15s)
   └── Grafana "Request Rate" graph shows traffic on new pod

6. All good → developer's Monday morning coffee still hot ☕
   Alert fired? → Alertmanager → Slack → on-call opens runbook
```

---

## Interview Cheat Sheet

| Question | Answer (Key Points) |
|---|---|
| "Explain your CI/CD pipeline" | GitHub Actions CI (test→scan→build→push→update GitOps) + ArgoCD CD (watches Git→syncs cluster). Git is single source of truth. |
| "How do you handle secrets?" | External Secrets Operator pulls from AWS Secrets Manager via IRSA. No secrets ever in Git. |
| "How does IRSA work?" | EKS OIDC provider maps K8s ServiceAccount → IAM Role. Each pod gets only permissions it needs. No node-level IAM roles. |
| "What are your SLOs?" | 99.9% availability, P95 < 500ms. Error budget = 43 min/month. Budget exhaustion = feature freeze. |
| "How do you handle traffic spikes?" | HPA scales pods (based on CPU/memory). Karpenter scales nodes (right-sized, spot-preferred, < 90 seconds). |
| "How do you rollback?" | `kubectl rollout undo` or `git revert` + ArgoCD syncs. All rollbacks are auditable Git commits. |
| "Why GitOps over push-based CD?" | Full audit trail in Git. Self-healing (ArgoCD reverts drift). No pipeline has cluster credentials. Rollback = git revert. |
| "What is a DLQ?" | Dead Letter Queue — catches messages that fail processing N times. Isolates bad messages for debugging without blocking the main queue. |
| "Why Kyverno?" | Enforce security policies at admission time. Bad deployments are REJECTED before they reach the cluster, not just warned about. |
| "What is the RED method?" | Rate, Errors, Duration — the three golden signals for monitoring service health. |
