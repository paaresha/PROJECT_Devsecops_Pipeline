# =============================================================================
# Dev Environment — Outputs
# =============================================================================
# These values are printed after terraform apply and can be used to
# configure kubectl, GitHub Actions secrets, etc.
# =============================================================================

output "eks_cluster_name" {
  description = "EKS cluster name (used in: aws eks update-kubeconfig --name <this>)"
  value       = module.eks.cluster_name
}

output "eks_cluster_endpoint" {
  description = "EKS API endpoint"
  value       = module.eks.cluster_endpoint
}

output "ecr_repository_urls" {
  description = "ECR repo URLs for each microservice"
  value       = module.ecr.repository_urls
}

output "rds_endpoint" {
  description = "PostgreSQL endpoint"
  value       = module.rds.endpoint
}

output "rds_secret_arn" {
  description = "ARN of DB credentials in Secrets Manager"
  value       = module.rds.secret_arn
}

output "redis_endpoint" {
  description = "Redis endpoint"
  value       = module.elasticache.endpoint
}

output "redis_url" {
  description = "Redis connection URL"
  value       = module.elasticache.redis_url
}

output "sqs_queue_url" {
  description = "SQS order events queue URL"
  value       = module.sqs.queue_url
}

output "sqs_dlq_url" {
  description = "SQS dead-letter queue URL"
  value       = module.sqs.dlq_url
}

# ── Convenience: kubeconfig command ──────────────────────────────────────────
output "configure_kubectl" {
  description = "Run this to configure kubectl"
  value       = "aws eks update-kubeconfig --region ${var.aws_region} --name ${module.eks.cluster_name}"
}
