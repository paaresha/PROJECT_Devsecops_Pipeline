#!/bin/bash
# =============================================================================
# LocalStack Initialization Script
# =============================================================================
# This runs automatically when LocalStack starts, creating the SQS queues
# we need for local development.
# =============================================================================

echo "Creating SQS queues in LocalStack..."

# Create the dead-letter queue first (DLQ)
# Messages that fail processing 3 times get moved here for investigation
awslocal sqs create-queue \
    --queue-name order-events-dlq \
    --attributes '{"MessageRetentionPeriod":"1209600"}'

# Get the DLQ ARN (needed for the main queue's redrive policy)
DLQ_ARN=$(awslocal sqs get-queue-attributes \
    --queue-url http://localhost:4566/000000000000/order-events-dlq \
    --attribute-names QueueArn \
    --query 'Attributes.QueueArn' \
    --output text)

# Create the main order events queue with a redrive policy
# maxReceiveCount=3 means: after 3 failed processing attempts, send to DLQ
awslocal sqs create-queue \
    --queue-name order-events \
    --attributes "{\"RedrivePolicy\":\"{\\\"deadLetterTargetArn\\\":\\\"${DLQ_ARN}\\\",\\\"maxReceiveCount\\\":\\\"3\\\"}\"}"

echo "✅ SQS queues created successfully"
awslocal sqs list-queues
