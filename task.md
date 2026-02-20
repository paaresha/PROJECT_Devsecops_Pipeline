# KubeFlow Ops — Task Checklist

## Phase 1: Project Setup & Microservices (Python FastAPI)
- [/] Create implementation plan and get user approval
- [ ] Scaffold project directory structure
- [ ] Build `order-service` (FastAPI — CRUD + SQS producer)
- [ ] Build `user-service` (FastAPI — CRUD + REST calls)
- [ ] Build `notification-service` (FastAPI — SQS consumer)
- [ ] Dockerfiles (multi-stage) for all 3 services
- [ ] `docker-compose.yml` for local development

## Phase 2: Terraform — AWS Infrastructure (Modular)
- [ ] VPC module (public/private subnets, NAT Gateway)
- [ ] EKS module (cluster, managed node group, IRSA)
- [ ] ECR module (3 repos, lifecycle policies)
- [ ] RDS module (PostgreSQL, private subnet)
- [ ] ElastiCache module (Redis, private subnet)
- [ ] SQS module (order queue + dead-letter queue)
- [ ] Environment configs (`dev/` and `prod/`)
- [ ] S3 + DynamoDB backend for remote state

## Phase 3: Kubernetes Manifests (Helm/Kustomize)
- [ ] Namespace, ServiceAccount, RBAC
- [ ] Deployments + Services for all 3 microservices
- [ ] HPA (Horizontal Pod Autoscaler)
- [ ] Ingress (NGINX Ingress Controller)
- [ ] Health probes (liveness, readiness, startup)
- [ ] Resource requests/limits on all pods

## Phase 4: GitHub Actions — CI Pipelines
- [ ] CI workflow for each microservice (lint, test, build, scan, push to ECR)
- [ ] Terraform workflow (fmt, validate, plan, apply with approval)
- [ ] Auto-update image tag in GitOps repo after CI passes

## Phase 5: ArgoCD — GitOps CD
- [ ] ArgoCD installation manifests
- [ ] Application CRDs for each microservice
- [ ] App-of-Apps pattern for platform tools
- [ ] Sync policies (auto-sync, self-heal, prune)

## Phase 6: Observability Stack
- [ ] Prometheus + Grafana (kube-prometheus-stack)
- [ ] Loki + Promtail (log aggregation)
- [ ] Tempo (distributed tracing)
- [ ] Grafana dashboards (SRE overview, per-service, infra)
- [ ] Alertmanager rules → Slack/SNS

## Phase 7: Platform Tools
- [ ] Karpenter (node autoscaling)
- [ ] Kyverno (policy enforcement)
- [ ] External Secrets Operator → AWS Secrets Manager
- [ ] Cert-Manager + External DNS

## Phase 8: Documentation & Runbooks
- [ ] Architecture diagram and README
- [ ] SLO/SLI definitions
- [ ] Incident response runbook
- [ ] Teardown instructions (`terraform destroy` commands)
- [ ] QUESTIONS file (interview prep)
