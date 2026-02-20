# =============================================================================
# ECR Module — Container Registry
# =============================================================================
# Creates one ECR repository per microservice with lifecycle policies to
# automatically delete old images and keep costs down.
# =============================================================================

variable "project_name" {
  type = string
}

variable "environment" {
  type = string
}

variable "service_names" {
  description = "List of microservice names to create repos for"
  type        = list(string)
  default     = ["order-service", "user-service", "notification-service"]
}

# ── ECR Repositories ─────────────────────────────────────────────────────────
resource "aws_ecr_repository" "services" {
  for_each = toset(var.service_names)

  name                 = "${var.project_name}-${each.value}"
  image_tag_mutability = "IMMUTABLE"  # Prevents overwriting tags (security best practice)

  image_scanning_configuration {
    scan_on_push = true  # Automatically scan images for vulnerabilities on push
  }

  encryption_configuration {
    encryption_type = "AES256"
  }

  tags = {
    Name        = "${var.project_name}-${each.value}"
    Environment = var.environment
    ManagedBy   = "terraform"
  }
}

# ── Lifecycle Policy ─────────────────────────────────────────────────────────
# Keep only the last 10 images per repo. Old images are automatically deleted.
# This saves storage costs and keeps the repo clean.
resource "aws_ecr_lifecycle_policy" "services" {
  for_each = aws_ecr_repository.services

  repository = each.value.name

  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Keep only last 10 images"
      selection = {
        tagStatus   = "any"
        countType   = "imageCountMoreThan"
        countNumber = 10
      }
      action = {
        type = "expire"
      }
    }]
  })
}

# ── Outputs ──────────────────────────────────────────────────────────────────
output "repository_urls" {
  description = "Map of service name to ECR repository URL"
  value       = { for k, v in aws_ecr_repository.services : k => v.repository_url }
}

output "registry_id" {
  description = "The registry ID (AWS account ID)"
  value       = values(aws_ecr_repository.services)[0].registry_id
}
