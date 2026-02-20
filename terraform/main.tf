# =============================================================================
# Root Module — Wires VPC, IAM, and EKS modules + ECR Repository
# =============================================================================
# Dependency order (no circular references):
#   1. VPC (standalone)
#   2. IAM base roles (standalone — cluster & node group roles only)
#   3. EKS (depends on VPC subnets + IAM base roles)
#   4. IAM OIDC/IRSA (depends on EKS — needs OIDC issuer URL)
# =============================================================================

# ---- Data Sources ----
data "aws_availability_zones" "available" {
  state = "available"
}

# ---- Step 1: VPC Module ----
module "vpc" {
  source = "./modules/vpc"

  vpc_cidr     = var.vpc_cidr
  project_name = var.project_name
  environment  = var.environment
  azs          = slice(data.aws_availability_zones.available.names, 0, 3)
}

# ---- Step 2: IAM Base Roles (no EKS dependency) ----
# These roles are needed BEFORE creating the EKS cluster.
resource "aws_iam_role" "eks_cluster" {
  name = "${var.project_name}-eks-cluster-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = { Service = "eks.amazonaws.com" }
        Action = "sts:AssumeRole"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "eks_cluster_policy" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSClusterPolicy"
  role       = aws_iam_role.eks_cluster.name
}

resource "aws_iam_role_policy_attachment" "eks_vpc_resource_controller" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSVPCResourceController"
  role       = aws_iam_role.eks_cluster.name
}

resource "aws_iam_role" "eks_nodes" {
  name = "${var.project_name}-eks-node-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = { Service = "ec2.amazonaws.com" }
        Action = "sts:AssumeRole"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "eks_worker_node_policy" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSWorkerNodePolicy"
  role       = aws_iam_role.eks_nodes.name
}

resource "aws_iam_role_policy_attachment" "eks_cni_policy" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKS_CNI_Policy"
  role       = aws_iam_role.eks_nodes.name
}

resource "aws_iam_role_policy_attachment" "ecr_read_only" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
  role       = aws_iam_role.eks_nodes.name
}

# ---- Step 3: EKS Module (depends on VPC + base IAM roles) ----
module "eks" {
  source = "./modules/eks"

  cluster_name        = "${var.project_name}-${var.environment}"
  cluster_version     = var.eks_cluster_version
  subnet_ids          = module.vpc.private_subnet_ids
  cluster_role_arn    = aws_iam_role.eks_cluster.arn
  node_role_arn       = aws_iam_role.eks_nodes.arn
  node_instance_types = var.eks_node_instance_types
  desired_size        = var.eks_desired_capacity
  min_size            = var.eks_min_size
  max_size            = var.eks_max_size

  depends_on = [
    module.vpc,
    aws_iam_role_policy_attachment.eks_cluster_policy,
    aws_iam_role_policy_attachment.eks_vpc_resource_controller,
    aws_iam_role_policy_attachment.eks_worker_node_policy,
    aws_iam_role_policy_attachment.eks_cni_policy,
    aws_iam_role_policy_attachment.ecr_read_only
  ]
}

# ---- Step 4: IAM OIDC/IRSA Module (depends on EKS) ----
module "iam" {
  source = "./modules/iam"

  project_name        = var.project_name
  environment         = var.environment
  eks_oidc_issuer_url = module.eks.oidc_issuer_url
  app_namespace       = "cloudpulse"
  app_service_account = "cloudpulse-sa"

  depends_on = [module.eks]
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
