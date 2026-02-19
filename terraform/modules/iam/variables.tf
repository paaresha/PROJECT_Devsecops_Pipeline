# =============================================================================
# IAM Module — Variables
# =============================================================================

variable "project_name" {
  description = "Project name for resource naming"
  type        = string
}

variable "environment" {
  description = "Environment name"
  type        = string
}

variable "eks_oidc_issuer_url" {
  description = "OIDC issuer URL from the EKS cluster"
  type        = string
}

variable "eks_oidc_provider_arn" {
  description = "ARN of the OIDC provider (used internally for dependency ordering)"
  type        = string
  default     = ""
}

variable "app_namespace" {
  description = "Kubernetes namespace for the application"
  type        = string
  default     = "vprofile"
}

variable "app_service_account" {
  description = "Kubernetes service account name for the application"
  type        = string
  default     = "vprofile-sa"
}
