# =============================================================================
# VPC Module — Main Configuration
# =============================================================================
# Creates: VPC → Internet Gateway → Public Subnets → Private Subnets →
#          NAT Gateway → Route Tables
#
# Architecture:
#   Public Subnets  → Internet Gateway (for ALB, NAT Gateway)
#   Private Subnets → NAT Gateway (for EKS nodes to pull images, etc.)
# =============================================================================

# ── VPC ──────────────────────────────────────────────────────────────────────
resource "aws_vpc" "main" {
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = true  # Required for EKS
  enable_dns_support   = true  # Required for EKS

  tags = {
    Name        = "${var.project_name}-${var.environment}-vpc"
    Environment = var.environment
    ManagedBy   = "terraform"
  }
}

# ── Internet Gateway ────────────────────────────────────────────────────────
# Allows resources in public subnets to reach the internet
resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id

  tags = {
    Name        = "${var.project_name}-${var.environment}-igw"
    Environment = var.environment
  }
}

# ── Public Subnets ───────────────────────────────────────────────────────────
# These host the NAT Gateway and any public-facing load balancers.
# EKS requires subnets in at least 2 AZs.
resource "aws_subnet" "public" {
  count = length(var.public_subnet_cidrs)

  vpc_id                  = aws_vpc.main.id
  cidr_block              = var.public_subnet_cidrs[count.index]
  availability_zone       = var.availability_zones[count.index]
  map_public_ip_on_launch = true

  tags = {
    Name                                           = "${var.project_name}-${var.environment}-public-${var.availability_zones[count.index]}"
    Environment                                    = var.environment
    "kubernetes.io/role/elb"                        = "1"       # Tells AWS LB Controller to use these for public LBs
    "kubernetes.io/cluster/${var.project_name}-${var.environment}" = "shared"
  }
}

# ── Private Subnets ──────────────────────────────────────────────────────────
# EKS worker nodes, RDS, and ElastiCache live here (no direct internet access).
# They reach the internet via NAT Gateway for pulling images, etc.
resource "aws_subnet" "private" {
  count = length(var.private_subnet_cidrs)

  vpc_id            = aws_vpc.main.id
  cidr_block        = var.private_subnet_cidrs[count.index]
  availability_zone = var.availability_zones[count.index]

  tags = {
    Name                                           = "${var.project_name}-${var.environment}-private-${var.availability_zones[count.index]}"
    Environment                                    = var.environment
    "kubernetes.io/role/internal-elb"               = "1"       # For internal LBs
    "kubernetes.io/cluster/${var.project_name}-${var.environment}" = "shared"
  }
}

# ── NAT Gateway ──────────────────────────────────────────────────────────────
# Single NAT Gateway (budget-friendly). In production, you'd have one per AZ.
# Allows private subnet resources to reach the internet (outbound only).
resource "aws_eip" "nat" {
  domain = "vpc"

  tags = {
    Name        = "${var.project_name}-${var.environment}-nat-eip"
    Environment = var.environment
  }
}

resource "aws_nat_gateway" "main" {
  allocation_id = aws_eip.nat.id
  subnet_id     = aws_subnet.public[0].id  # NAT GW lives in a public subnet

  tags = {
    Name        = "${var.project_name}-${var.environment}-nat"
    Environment = var.environment
  }

  depends_on = [aws_internet_gateway.main]
}

# ── Route Tables ─────────────────────────────────────────────────────────────

# Public route table: routes traffic to Internet Gateway
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }

  tags = {
    Name        = "${var.project_name}-${var.environment}-public-rt"
    Environment = var.environment
  }
}

# Private route table: routes traffic to NAT Gateway
resource "aws_route_table" "private" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.main.id
  }

  tags = {
    Name        = "${var.project_name}-${var.environment}-private-rt"
    Environment = var.environment
  }
}

# Associate public subnets with public route table
resource "aws_route_table_association" "public" {
  count = length(aws_subnet.public)

  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

# Associate private subnets with private route table
resource "aws_route_table_association" "private" {
  count = length(aws_subnet.private)

  subnet_id      = aws_subnet.private[count.index].id
  route_table_id = aws_route_table.private.id
}
