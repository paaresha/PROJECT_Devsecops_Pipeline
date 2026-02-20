# =============================================================================
# Remote State Backend — S3 + DynamoDB
# =============================================================================
# Stores Terraform state in S3 (encrypted) with DynamoDB locking.
# This prevents two people from running terraform apply at the same time.
#
# IMPORTANT: You must create the S3 bucket and DynamoDB table FIRST:
#   aws s3 mb s3://kubeflow-ops-terraform-state --region us-east-1
#   aws dynamodb create-table \
#     --table-name kubeflow-ops-terraform-lock \
#     --attribute-definitions AttributeName=LockID,AttributeType=S \
#     --key-schema AttributeName=LockID,KeyType=HASH \
#     --billing-mode PAY_PER_REQUEST \
#     --region us-east-1
# =============================================================================

terraform {
  backend "s3" {
    bucket         = "kubeflow-ops-terraform-state"
    key            = "dev/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "kubeflow-ops-terraform-lock"
    encrypt        = true
  }
}
