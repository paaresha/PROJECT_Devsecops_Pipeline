# =============================================================================
# VPC Module — Variables
# =============================================================================
# This module creates a production-ready VPC with public and private subnets
# across 2 Availability Zones. The private subnets host EKS nodes, RDS, and
# ElastiCache. The public subnets host NAT Gateway and load balancers.
# =============================================================================

variable "project_name" {
  description = "Project name used for tagging and naming resources"
  type        = string
}

variable "environment" {
  description = "Environment name (dev, staging, prod)"
  type        = string
}

variable "vpc_cidr" {
  description = "CIDR block for the VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "availability_zones" {
  description = "List of AZs to deploy into (2 minimum for EKS)"
  type        = list(string)
  default     = ["us-east-1a", "us-east-1b"]
}

variable "public_subnet_cidrs" {
  description = "CIDR blocks for public subnets"
  type        = list(string)
  default     = ["10.0.1.0/24", "10.0.2.0/24"]
}

variable "private_subnet_cidrs" {
  description = "CIDR blocks for private subnets"
  type        = list(string)
  default     = ["10.0.10.0/24", "10.0.20.0/24"]
}
