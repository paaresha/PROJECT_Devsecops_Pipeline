# =============================================================================
# SQS Module — Message Queue
# =============================================================================
# Creates an order events queue with a dead-letter queue (DLQ).
# The order-service publishes events, the notification-service consumes them.
# DLQ catches messages that fail processing 3 times for investigation.
# =============================================================================

variable "project_name" {
  type = string
}

variable "environment" {
  type = string
}

# ── Dead Letter Queue ────────────────────────────────────────────────────────
# Messages that fail processing go here. You can set up CloudWatch alarms
# on DLQ message count to know when something is broken.
resource "aws_sqs_queue" "order_events_dlq" {
  name                      = "${var.project_name}-${var.environment}-order-events-dlq"
  message_retention_seconds = 1209600  # 14 days

  tags = {
    Name        = "${var.project_name}-${var.environment}-order-events-dlq"
    Environment = var.environment
    ManagedBy   = "terraform"
  }
}

# ── Main Queue ───────────────────────────────────────────────────────────────
resource "aws_sqs_queue" "order_events" {
  name                       = "${var.project_name}-${var.environment}-order-events"
  visibility_timeout_seconds = 30
  message_retention_seconds  = 345600  # 4 days
  receive_wait_time_seconds  = 10      # Long polling (reduces API calls & cost)

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.order_events_dlq.arn
    maxReceiveCount     = 3  # After 3 failed attempts → send to DLQ
  })

  tags = {
    Name        = "${var.project_name}-${var.environment}-order-events"
    Environment = var.environment
    ManagedBy   = "terraform"
  }
}

# ── IAM Policy (for IRSA) ───────────────────────────────────────────────────
# This policy is attached to Kubernetes ServiceAccounts via IRSA, giving
# specific pods (and only those pods) access to SQS.
resource "aws_iam_policy" "sqs_access" {
  name        = "${var.project_name}-${var.environment}-sqs-access"
  description = "Allow send/receive on order events SQS queue"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "sqs:SendMessage",
          "sqs:ReceiveMessage",
          "sqs:DeleteMessage",
          "sqs:GetQueueAttributes",
          "sqs:GetQueueUrl",
        ]
        Resource = [
          aws_sqs_queue.order_events.arn,
          aws_sqs_queue.order_events_dlq.arn,
        ]
      }
    ]
  })
}

# ── Outputs ──────────────────────────────────────────────────────────────────
output "queue_url" {
  description = "URL of the order events SQS queue"
  value       = aws_sqs_queue.order_events.url
}

output "queue_arn" {
  description = "ARN of the order events SQS queue"
  value       = aws_sqs_queue.order_events.arn
}

output "dlq_url" {
  description = "URL of the dead-letter queue"
  value       = aws_sqs_queue.order_events_dlq.url
}

output "sqs_policy_arn" {
  description = "ARN of the IAM policy for SQS access (attach via IRSA)"
  value       = aws_iam_policy.sqs_access.arn
}
