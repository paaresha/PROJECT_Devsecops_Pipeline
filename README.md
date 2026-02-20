# KubeFlow Ops — Production GitOps Platform

A **production-grade GitOps platform** demonstrating senior-level DevOps, SRE, and Platform Engineering skills. Built with real-world tools and patterns used by companies like Netflix, Spotify, and Airbnb.

## Architecture

```
Developer Push → GitHub Actions (CI) → ECR → ArgoCD (CD) → EKS Cluster
                    │                                          │
                    ├─ Lint, Test, Scan                        ├─ 3 Microservices
                    ├─ Build Docker Image                      ├─ Prometheus + Grafana
                    ├─ Push to ECR                             ├─ Loki (Logs)
                    └─ Update GitOps Manifest                  ├─ Tempo (Traces)
                                                               ├─ Kyverno (Policies)
                                                               ├─ External Secrets
                                                               └─ Karpenter (Autoscaling)
```

## Tech Stack

| Layer | Tool | Purpose |
|---|---|---|
| **CI** | GitHub Actions | Build, test, scan, push |
| **CD** | ArgoCD | GitOps-based deployment |
| **IaC** | Terraform | AWS infrastructure (modular) |
| **Container** | Docker | Multi-stage builds |
| **Orchestration** | Amazon EKS | Managed Kubernetes |
| **Metrics** | Prometheus + Grafana | SLIs, SLOs, dashboards |
| **Logs** | Loki + Promtail | Centralized log aggregation |
| **Traces** | Tempo | Distributed tracing |
| **Alerts** | Alertmanager | Slack/SNS notifications |
| **Secrets** | External Secrets Operator | AWS Secrets Manager → K8s |
| **Policy** | Kyverno | Security guardrails |
| **Security** | Trivy | CVE scanning (code + images) |

## Microservices

| Service | Port | Tech | Description |
|---|---|---|---|
| `order-service` | 8001 | Python FastAPI | CRUD + SQS publisher |
| `user-service` | 8002 | Python FastAPI | CRUD + inter-service validation |
| `notification-service` | 8003 | Python FastAPI | SQS consumer + Redis caching |

## Quick Start (Local Development)

```bash
# Start everything locally (3 services + Postgres + Redis + LocalStack)
docker-compose up --build

# Access the services:
# Order Service:        http://localhost:8001/docs
# User Service:         http://localhost:8002/docs
# Notification Service: http://localhost:8003/docs
```

## Deployment to AWS

### Prerequisites

- AWS CLI configured
- Terraform >= 1.7.0
- kubectl installed

### Step 1: Create Terraform Backend

```bash
aws s3 mb s3://kubeflow-ops-terraform-state --region us-east-1
aws dynamodb create-table \
  --table-name kubeflow-ops-terraform-lock \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema AttributeName=LockID,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --region us-east-1
```

### Step 2: Deploy Infrastructure

```bash
cd terraform/environments/dev
terraform init
terraform plan
terraform apply
```

### Step 3: Configure kubectl

```bash
aws eks update-kubeconfig --region us-east-1 --name kubeflow-ops-dev
```

### Step 4: Install ArgoCD

```bash
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# Get ArgoCD admin password
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d

# Port forward to access ArgoCD UI
kubectl port-forward svc/argocd-server -n argocd 8080:443
# Open: https://localhost:8080 (username: admin)
```

### Step 5: Deploy App-of-Apps

```bash
kubectl apply -f gitops/platform/argocd/app-of-apps.yaml
```

ArgoCD will now automatically deploy all microservices and platform tools!

## Teardown (Destroy Everything)

```bash
# Delete ArgoCD apps (prevents orphaned resources)
kubectl delete -f gitops/platform/argocd/app-of-apps.yaml

# Destroy all AWS infrastructure
cd terraform/environments/dev
terraform destroy

# Clean up Terraform backend (optional)
aws s3 rb s3://kubeflow-ops-terraform-state --force
aws dynamodb delete-table --table-name kubeflow-ops-terraform-lock
```

## Project Structure

```
kubeflow-ops/
├── apps/                           # Microservice source code
│   ├── order-service/              # CRUD + SQS producer
│   ├── user-service/               # CRUD + REST validation
│   └── notification-service/       # SQS consumer + Redis
├── terraform/                      # Infrastructure as Code
│   ├── modules/                    # Reusable Terraform modules
│   │   ├── vpc/                    # VPC, subnets, NAT GW
│   │   ├── eks/                    # EKS cluster, IRSA, node group
│   │   ├── ecr/                    # Container registries
│   │   ├── rds/                    # PostgreSQL
│   │   ├── elasticache/            # Redis
│   │   └── sqs/                    # Message queues + DLQ
│   └── environments/dev/           # Dev environment config
├── gitops/                         # ArgoCD watches this directory
│   ├── apps/                       # Application manifests
│   │   ├── common/                 # Namespace, SA, ConfigMap, Ingress
│   │   ├── order-service/base/     # Deployment, Service, HPA
│   │   ├── user-service/base/
│   │   └── notification-service/base/
│   └── platform/                   # Platform tool configs
│       ├── argocd/                 # App-of-Apps + child apps
│       ├── prometheus/             # Alert rules
│       ├── kyverno/                # Security policies
│       └── external-secrets/       # AWS Secrets Manager sync
├── .github/workflows/              # CI/CD pipelines
│   ├── ci.yml                      # Build, test, scan, push
│   └── terraform.yml               # IaC pipeline
├── docs/                           # Documentation
│   ├── slo-definitions.md
│   └── runbook.md
└── docker-compose.yml              # Local development
```

## Understanding ArgoCD (For Beginners)

**What it does:** ArgoCD watches a Git repo. When you change a YAML file in Git, ArgoCD automatically applies that change to the cluster.

**How the deployment flow works:**

1. You push code to `apps/order-service/`
2. GitHub Actions builds, tests, and pushes the Docker image to ECR
3. GitHub Actions updates the image tag in `gitops/apps/order-service/base/deployment.yaml`
4. ArgoCD detects the change and syncs it to the cluster
5. Your new code is live — **zero manual steps!**

**Self-Heal:** If someone manually changes something in the cluster (e.g., `kubectl edit`), ArgoCD detects the "drift" and reverts it to match Git.

## License

MIT
