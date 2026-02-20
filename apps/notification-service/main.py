# =============================================================================
# Notification Service — FastAPI Microservice
# =============================================================================
# Consumes order events from AWS SQS and processes notifications.
# - Runs a background worker that polls SQS for messages
# - Uses Redis to cache processed message IDs (idempotency / deduplication)
# - This demonstrates asynchronous, event-driven communication between services
# =============================================================================

import os
import json
import logging
import threading
import time
from datetime import datetime
from contextlib import asynccontextmanager
from typing import Optional

import boto3
import redis
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from prometheus_fastapi_instrumentator import Instrumentator

# ── OpenTelemetry Tracing ────────────────────────────────────────────────────
from opentelemetry import trace
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor
from opentelemetry.exporter.otlp.proto.grpc.trace_exporter import OTLPSpanExporter
from opentelemetry.instrumentation.fastapi import FastAPIInstrumentor
from opentelemetry.sdk.resources import Resource

# ── Configuration ────────────────────────────────────────────────────────────
SQS_QUEUE_URL    = os.getenv("SQS_QUEUE_URL", "http://localhost:4566/000000000000/order-events")
REDIS_URL        = os.getenv("REDIS_URL", "redis://localhost:6379/0")
AWS_REGION       = os.getenv("AWS_REGION", "us-east-1")
AWS_ENDPOINT_URL = os.getenv("AWS_ENDPOINT_URL", None)  # For LocalStack
OTLP_ENDPOINT    = os.getenv("OTLP_ENDPOINT", "localhost:4317")
POLL_INTERVAL    = int(os.getenv("POLL_INTERVAL", "5"))  # seconds between SQS polls
SERVICE_NAME     = "notification-service"

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(SERVICE_NAME)

# ── Redis Client (for deduplication) ─────────────────────────────────────────
# Redis stores the IDs of messages we've already processed.
# If the same message is delivered twice (SQS "at-least-once" guarantee),
# we skip it instead of sending a duplicate notification.
redis_client = redis.from_url(REDIS_URL, decode_responses=True)

# ── In-memory notification log (for demo/API visibility) ────────────────────
# In production, you'd store these in a database. For this demo project,
# an in-memory list lets us show processed notifications via the API.
notifications: list[dict] = []
MAX_NOTIFICATIONS = 1000  # Keep only the last 1000 in memory


# ── SQS Client ──────────────────────────────────────────────────────────────
def get_sqs_client():
    kwargs = {"region_name": AWS_REGION}
    if AWS_ENDPOINT_URL:
        kwargs["endpoint_url"] = AWS_ENDPOINT_URL
    return boto3.client("sqs", **kwargs)


# ── SQS Background Worker ───────────────────────────────────────────────────
# This runs in a separate thread, continuously polling SQS for new messages.
# It demonstrates the "consumer" side of an event-driven architecture.

def sqs_worker():
    """Background thread that polls SQS and processes order events."""
    logger.info("SQS worker started — polling for messages...")
    sqs = get_sqs_client()

    while True:
        try:
            # Long polling (WaitTimeSeconds=10) reduces API calls and cost
            response = sqs.receive_message(
                QueueUrl=SQS_QUEUE_URL,
                MaxNumberOfMessages=10,
                WaitTimeSeconds=10,
                MessageAttributeNames=["All"],
            )

            messages = response.get("Messages", [])
            for msg in messages:
                message_id = msg["MessageId"]

                # ── Idempotency Check ────────────────────────────────────
                # Check Redis to see if we've already processed this message
                if redis_client.get(f"processed:{message_id}"):
                    logger.info(f"Skipping duplicate message: {message_id}")
                    sqs.delete_message(QueueUrl=SQS_QUEUE_URL, ReceiptHandle=msg["ReceiptHandle"])
                    continue

                # ── Process the message ──────────────────────────────────
                try:
                    body = json.loads(msg["Body"])
                    notification = {
                        "message_id": message_id,
                        "order_id": body.get("order_id"),
                        "user_id": body.get("user_id"),
                        "product": body.get("product"),
                        "action": body.get("action"),
                        "processed_at": datetime.utcnow().isoformat(),
                    }

                    # In production: send email, push notification, Slack message, etc.
                    logger.info(f"📬 Notification: {body.get('action')} for order {body.get('order_id')}")

                    # Store in memory for API visibility
                    notifications.append(notification)
                    if len(notifications) > MAX_NOTIFICATIONS:
                        notifications.pop(0)  # Remove oldest

                    # Mark as processed in Redis (TTL: 24 hours)
                    redis_client.setex(f"processed:{message_id}", 86400, "1")

                    # Delete message from SQS (so it's not reprocessed)
                    sqs.delete_message(QueueUrl=SQS_QUEUE_URL, ReceiptHandle=msg["ReceiptHandle"])

                except Exception as e:
                    logger.error(f"Failed to process message {message_id}: {e}")
                    # Don't delete — SQS will retry or send to DLQ

        except Exception as e:
            logger.error(f"SQS polling error: {e}")
            time.sleep(POLL_INTERVAL)


# ── App Lifecycle ────────────────────────────────────────────────────────────
@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Starting notification-service...")
    # Start SQS worker in a background daemon thread
    worker = threading.Thread(target=sqs_worker, daemon=True)
    worker.start()
    logger.info("SQS background worker launched.")
    yield
    logger.info("Shutting down notification-service...")


# ── FastAPI App ──────────────────────────────────────────────────────────────
app = FastAPI(
    title="Notification Service",
    description="Consumes order events from SQS — part of KubeFlow Ops platform",
    version="1.0.0",
    lifespan=lifespan,
)

# Prometheus metrics
Instrumentator().instrument(app).expose(app)

# OpenTelemetry tracing
try:
    resource = Resource.create({"service.name": SERVICE_NAME})
    provider = TracerProvider(resource=resource)
    processor = BatchSpanProcessor(OTLPSpanExporter(endpoint=OTLP_ENDPOINT, insecure=True))
    provider.add_span_processor(processor)
    trace.set_tracer_provider(provider)
    FastAPIInstrumentor.instrument_app(app)
    logger.info(f"OpenTelemetry tracing enabled → {OTLP_ENDPOINT}")
except Exception as e:
    logger.warning(f"OpenTelemetry setup failed (non-fatal): {e}")


# ── Health Probes ────────────────────────────────────────────────────────────

@app.get("/healthz", tags=["Health"])
def liveness():
    """Liveness probe — Kubernetes restarts the pod if this fails."""
    return {"status": "alive"}


@app.get("/readyz", tags=["Health"])
def readiness():
    """Readiness probe — checks Redis connectivity."""
    try:
        redis_client.ping()
        return {"status": "ready", "redis": "connected"}
    except Exception as e:
        raise HTTPException(status_code=503, detail=f"Not ready: {e}")


# ── API Endpoints ────────────────────────────────────────────────────────────

@app.get("/notifications", tags=["Notifications"])
def list_notifications(limit: int = 50):
    """List recently processed notifications (most recent first)."""
    return notifications[-limit:][::-1]


@app.get("/notifications/stats", tags=["Notifications"])
def notification_stats():
    """Get notification processing statistics."""
    return {
        "total_processed": len(notifications),
        "actions": _count_by_action(),
        "last_processed": notifications[-1]["processed_at"] if notifications else None,
    }


def _count_by_action() -> dict:
    """Count notifications by action type."""
    counts: dict[str, int] = {}
    for n in notifications:
        action = n.get("action", "UNKNOWN")
        counts[action] = counts.get(action, 0) + 1
    return counts
