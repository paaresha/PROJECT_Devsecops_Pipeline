# =============================================================================
# IAM Module — Outputs
# =============================================================================

output "oidc_provider_arn" {
  description = "ARN of the IAM OIDC provider"
  value       = aws_iam_openid_connect_provider.eks.arn
}

output "app_role_arn" {
  description = "ARN of the application IRSA role"
  value       = aws_iam_role.app_irsa.arn
}

output "alb_controller_role_arn" {
  description = "ARN of the ALB controller IRSA role"
  value       = aws_iam_role.alb_controller.arn
}
