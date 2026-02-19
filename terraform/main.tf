# =============================================================================
# Root Module — Wires VPC, IAM, and EKS modules + ECR Repository
# =============================================================================

# ---- Data Sources ----
data "aws_availability_zones" "available" {
  state = "available"
}

# ---- VPC Module ----
module "vpc" {
  source = "./modules/vpc"

  vpc_cidr     = var.vpc_cidr
  project_name = var.project_name
  environment  = var.environment
  azs          = slice(data.aws_availability_zones.available.names, 0, 3)
}

# ---- IAM Module ----
module "iam" {
  source = "./modules/iam"

  project_name          = var.project_name
  environment           = var.environment
  eks_oidc_issuer_url   = module.eks.oidc_issuer_url
  eks_oidc_provider_arn = module.iam.oidc_provider_arn
  app_namespace         = "vprofile"
  app_service_account   = "vprofile-sa"

  depends_on = [module.eks]
}

# ---- EKS Module ----
module "eks" {
  source = "./modules/eks"

  cluster_name        = "${var.project_name}-${var.environment}"
  cluster_version     = var.eks_cluster_version
  subnet_ids          = module.vpc.private_subnet_ids
  cluster_role_arn    = module.iam.cluster_role_arn
  node_role_arn       = module.iam.node_role_arn
  node_instance_types = var.eks_node_instance_types
  desired_size        = var.eks_desired_capacity
  min_size            = var.eks_min_size
  max_size            = var.eks_max_size

  depends_on = [module.vpc]
}

# ---- ECR Repository ----
resource "aws_ecr_repository" "app" {
  name                 = "${var.project_name}-app"
  image_tag_mutability = "IMMUTABLE"
  force_delete         = true

  image_scanning_configuration {
    scan_on_push = true
  }

  encryption_configuration {
    encryption_type = "AES256"
  }

  tags = {
    Name = "${var.project_name}-app"
  }
}

# ---- ECR Lifecycle Policy (keep last 30 images) ----
resource "aws_ecr_lifecycle_policy" "app" {
  repository = aws_ecr_repository.app.name

  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Keep last 30 images"
        selection = {
          tagStatus   = "any"
          countType   = "imageCountMoreThan"
          countNumber = 30
        }
        action = {
          type = "expire"
        }
      }
    ]
  })
}
