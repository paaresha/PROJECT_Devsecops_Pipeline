# SLO/SLI Definitions — KubeFlow Ops

## What are SLIs and SLOs?

- **SLI (Service Level Indicator):** A measurable metric about your service (e.g., latency, error rate)
- **SLO (Service Level Objective):** A target value for an SLI (e.g., "99.9% of requests succeed")
- **Error Budget:** The allowed amount of unreliability (e.g., 0.1% downtime = ~43 min/month)

## Order Service

| SLI | Measurement | SLO Target | Error Budget |
|---|---|---|---|
| Availability | `rate(http_requests_total{status!~"5.."}[30d]) / rate(http_requests_total[30d])` | 99.9% | 43 min/month |
| Latency (P95) | `histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))` | < 500ms | N/A |
| Error Rate | `rate(http_requests_total{status=~"5.."}[5m]) / rate(http_requests_total[5m])` | < 1% | N/A |

## User Service

| SLI | Measurement | SLO Target |
|---|---|---|
| Availability | Same formula as above | 99.9% |
| Latency (P95) | Same | < 300ms |

## Notification Service

| SLI | Measurement | SLO Target |
|---|---|---|
| Availability | Health check success rate | 99.9% |
| Processing Lag | Time from SQS publish to notification processed | < 30 seconds |
| DLQ Rate | Messages in DLQ / total messages | < 0.1% |

## Alerting Thresholds

| Alert | Threshold | Severity | Action |
|---|---|---|---|
| HighErrorRate | > 5% for 5 min | Critical | Page on-call → immediate investigation |
| HighLatency | P95 > 2s for 5 min | Warning | Check logs → scale up if needed |
| PodCrashLoop | > 3 restarts in 10 min | Critical | Check logs → rollback deployment |
| DiskPressure | Node disk pressure | Critical | Expand EBS volume or add nodes |
