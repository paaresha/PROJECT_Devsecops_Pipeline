# =============================================================================
# RDS Module — PostgreSQL Database
# =============================================================================
# Creates a PostgreSQL instance in private subnets with security group
# that only allows traffic from the EKS cluster.
# =============================================================================

variable "project_name" {
  type = string
}

variable "environment" {
  type = string
}

variable "vpc_id" {
  type = string
}

variable "private_subnet_ids" {
  type = list(string)
}

variable "eks_security_group_id" {
  description = "SG of EKS cluster — only this SG can access RDS"
  type        = string
}

variable "instance_class" {
  type    = string
  default = "db.t3.micro"  # Budget-friendly
}

variable "db_name" {
  type    = string
  default = "kubeflow"
}

variable "db_username" {
  type    = string
  default = "kubeflow_admin"
}

# ── DB Subnet Group ─────────────────────────────────────────────────────────
resource "aws_db_subnet_group" "main" {
  name       = "${var.project_name}-${var.environment}-db-subnet"
  subnet_ids = var.private_subnet_ids

  tags = {
    Name        = "${var.project_name}-${var.environment}-db-subnet"
    Environment = var.environment
  }
}

# ── Security Group ───────────────────────────────────────────────────────────
# Only allows PostgreSQL traffic (port 5432) from the EKS cluster SG.
# No public access — this is the principle of least privilege.
resource "aws_security_group" "rds" {
  name_prefix = "${var.project_name}-${var.environment}-rds-"
  vpc_id      = var.vpc_id
  description = "Allow PostgreSQL access from EKS only"

  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [var.eks_security_group_id]
    description     = "PostgreSQL from EKS"
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name        = "${var.project_name}-${var.environment}-rds-sg"
    Environment = var.environment
  }

  lifecycle {
    create_before_destroy = true
  }
}

# ── Generate Random Password ────────────────────────────────────────────────
resource "random_password" "db_password" {
  length  = 24
  special = false  # Some drivers have issues with special characters
}

# ── Store Password in AWS Secrets Manager ────────────────────────────────────
# External Secrets Operator in Kubernetes will read this and create a K8s Secret
resource "aws_secretsmanager_secret" "db_credentials" {
  name = "${var.project_name}/${var.environment}/db-credentials"

  tags = {
    Environment = var.environment
    ManagedBy   = "terraform"
  }
}

resource "aws_secretsmanager_secret_version" "db_credentials" {
  secret_id = aws_secretsmanager_secret.db_credentials.id
  secret_string = jsonencode({
    username = var.db_username
    password = random_password.db_password.result
    host     = aws_db_instance.main.address
    port     = 5432
    dbname   = var.db_name
    url      = "postgresql://${var.db_username}:${random_password.db_password.result}@${aws_db_instance.main.address}:5432/${var.db_name}"
  })
}

# ── RDS Instance ─────────────────────────────────────────────────────────────
resource "aws_db_instance" "main" {
  identifier = "${var.project_name}-${var.environment}-postgres"

  engine         = "postgres"
  engine_version = "16.3"
  instance_class = var.instance_class

  allocated_storage     = 20
  max_allocated_storage = 50  # Autoscale storage up to 50GB

  db_name  = var.db_name
  username = var.db_username
  password = random_password.db_password.result

  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]

  multi_az            = false  # Set true for production (costs 2x)
  publicly_accessible = false  # Private subnets only
  skip_final_snapshot = true   # For dev — set false in prod

  backup_retention_period = 7
  backup_window           = "03:00-04:00"
  maintenance_window      = "mon:04:00-mon:05:00"

  tags = {
    Name        = "${var.project_name}-${var.environment}-postgres"
    Environment = var.environment
    ManagedBy   = "terraform"
  }
}

# ── Outputs ──────────────────────────────────────────────────────────────────
output "endpoint" {
  description = "RDS instance endpoint"
  value       = aws_db_instance.main.address
}

output "port" {
  value = aws_db_instance.main.port
}

output "db_name" {
  value = aws_db_instance.main.db_name
}

output "secret_arn" {
  description = "ARN of the Secrets Manager secret containing DB credentials"
  value       = aws_secretsmanager_secret.db_credentials.arn
}
