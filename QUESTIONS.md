# KubeFlow Ops — Interview Questions & Answers

## 🟢 Junior Level (0–3 YoE)

1. Describe this project in 2–3 sentences.
2. What is GitOps and how does ArgoCD implement it?
3. What is the difference between GitHub Actions and Jenkins?
4. Why do you use multi-stage Docker builds?
5. What is the purpose of the `.dockerignore` file?
6. Explain the difference between a Kubernetes Deployment and a Pod.
7. What are the 3 types of Kubernetes health probes and when does each run?
8. What is a Kubernetes Service and why is it needed?
9. What is a Kubernetes Namespace and why don't you use `default`?
10. What is ECR and why not just use Docker Hub?
11. What does `docker-compose up` do in this project?
12. What is LocalStack and why is it used in local development?
13. What is `requirements.txt` and why does the Dockerfile copy it before the code?
14. What is the difference between `ClusterIP` and `LoadBalancer` service types?
15. What does `kubectl rollout status` do?

## 🟡 Mid Level (3–8 YoE)

1. Explain IRSA (IAM Roles for Service Accounts) and why it's better than node-level IAM.
2. How does your CI pipeline authenticate to AWS without static access keys?
3. What is a dead-letter queue (DLQ) and why is it important in your SQS design?
4. Explain the App-of-Apps pattern in ArgoCD.
5. What happens if someone manually edits a deployment in the cluster while ArgoCD is running?
6. How does External Secrets Operator work? Walk through the flow from AWS Secrets Manager to pod env var.
7. What is Kyverno and what policies are you enforcing?
8. Explain the HPA (Horizontal Pod Autoscaler) configuration. What is the stabilization window?
9. Why do you use Kustomize over Helm for application manifests?
10. How does Terraform remote state work? Why do you need DynamoDB?
11. What is the difference between `terraform plan` and `terraform apply`?
12. How do you handle Terraform state locking?
13. What is pod anti-affinity and why is it configured here?
14. Explain the Prometheus scraping model — how does it discover your pods?
15. What is the difference between Loki and ELK/EFK stack?
16. What is distributed tracing and how does OpenTelemetry work?
17. How would you handle a database migration in this setup?
18. What is idempotency and how does the notification service achieve it?
19. Explain NAT Gateway — why is it in the public subnet, and why do private subnets need it?
20. What is the security group chain from EKS → RDS? How do you restrict access?

## 🔴 Senior Level (8+ YoE)

1. You said this is "production-grade." What would you change for actual production?
2. How would you implement canary deployments with ArgoCD?
3. How would you design this for multi-region/disaster recovery?
4. Explain SLIs and SLOs. What are your service's SLOs and how do you measure them?
5. What is an error budget and how does it influence deployment decisions?
6. How would you implement rate limiting at the application level vs. infrastructure level?
7. Your notification service uses polling. How would you redesign it with AWS Lambda + SQS trigger?
8. How would you handle zero-downtime database migrations with rolling deployments?
9. What are the cost optimization strategies you'd apply to this architecture?
10. How would you implement mutual TLS (mTLS) between services?
11. Design an alerting strategy — what alerts are actionable vs. just noise?
12. How would you debug a latency spike that appears in Grafana?
13. Explain Karpenter vs. Cluster Autoscaler. When would you choose each?
14. How would you implement GitOps for secrets rotation?
15. What is your rollback strategy if ArgoCD deploys a broken version?
