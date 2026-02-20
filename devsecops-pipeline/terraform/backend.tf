# =============================================================================
# Remote State Configuration — S3 Backend with DynamoDB Locking
# =============================================================================
# This prevents concurrent Terraform runs from corrupting your state file.
# Before using this, create the S3 bucket and DynamoDB table:
#
#   aws s3api create-bucket --bucket devsecops-pipeline-tfstate --region us-east-1
#   aws dynamodb create-table \
#     --table-name devsecops-pipeline-tflock \
#     --attribute-definitions AttributeName=LockID,AttributeType=S \
#     --key-schema AttributeName=LockID,KeyType=HASH \
#     --billing-mode PAY_PER_REQUEST \
#     --region us-east-1
# =============================================================================

terraform {
  backend "s3" {
    bucket         = "devsecops-pipeline-tfstate"
    key            = "eks/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "devsecops-pipeline-tflock"
    encrypt        = true
  }
}
