# Incident Response Runbook — KubeFlow Ops

## Alert: HighErrorRate (> 5% HTTP 5xx)

### Severity: Critical

### Steps

1. **Check which service is affected**

   ```bash
   kubectl get pods -n kubeflow-ops
   kubectl logs -n kubeflow-ops -l app=<service-name> --tail=100
   ```

2. **Check recent deployments** (was something just deployed?)

   ```bash
   kubectl rollout history deployment/<service-name> -n kubeflow-ops
   ```

3. **Rollback if recent deployment caused it**

   ```bash
   kubectl rollout undo deployment/<service-name> -n kubeflow-ops
   ```

4. **Check downstream dependencies**
   - RDS: `aws rds describe-db-instances --db-instance-identifier kubeflow-ops-dev-postgres`
   - Redis: Is ElastiCache healthy?
   - SQS: Are messages piling up? `aws sqs get-queue-attributes --queue-url <url> --attribute-names All`

5. **Check Grafana dashboards**
   - Open SRE Overview dashboard
   - Look at error rate graph — when did it start?
   - Check distributed traces in Tempo for failing requests

---

## Alert: PodCrashLoopBackOff

### Severity: Critical

### Steps

1. **Identify the crashing pod**

   ```bash
   kubectl get pods -n kubeflow-ops | grep CrashLoop
   ```

2. **Check pod logs (including previous crash)**

   ```bash
   kubectl logs <pod-name> -n kubeflow-ops --previous
   ```

3. **Check events for the pod**

   ```bash
   kubectl describe pod <pod-name> -n kubeflow-ops
   ```

4. **Common causes:**
   - OOMKilled → Increase memory limits
   - Config error → Check ConfigMap/Secrets
   - DB connection error → Check RDS security group / credentials
   - Image pull error → Check ECR repository and image tag

---

## Alert: HighLatency (P95 > 2s)

### Severity: Warning

### Steps

1. **Check current pod count and HPA status**

   ```bash
   kubectl get hpa -n kubeflow-ops
   kubectl top pods -n kubeflow-ops
   ```

2. **Scale up if needed**

   ```bash
   kubectl scale deployment/<service-name> -n kubeflow-ops --replicas=5
   ```

3. **Check database performance**
   - Are there slow queries? Check RDS Performance Insights
   - Is there connection pool exhaustion?

4. **Check distributed trace** in Grafana Tempo for slow spans

---

## Alert: NodeDiskPressure

### Severity: Critical

### Steps

1. **Identify the node**

   ```bash
   kubectl get nodes -o wide
   kubectl describe node <node-name> | grep -A5 Conditions
   ```

2. **Clean up disk space**

   ```bash
   # On the node: prune unused Docker images
   docker system prune -af
   ```

3. **If persistent:** Increase EBS volume size or let Karpenter provision new nodes

---

## General Debugging Commands

```bash
# Cluster health
kubectl cluster-info
kubectl get nodes -o wide
kubectl top nodes

# Application status
kubectl get all -n kubeflow-ops
kubectl get events -n kubeflow-ops --sort-by='.lastTimestamp'

# ArgoCD status
kubectl get applications -n argocd
argocd app list

# Check all pod statuses
kubectl get pods -A | grep -v Running | grep -v Completed
```
