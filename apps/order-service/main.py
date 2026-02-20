# =============================================================================
# Order Service — FastAPI Microservice
# =============================================================================
# Handles CRUD operations for orders.
# - Validates user existence by calling the user-service via REST
# - Publishes order events to AWS SQS for the notification-service to consume
# =============================================================================

import os
import json
import logging
from datetime import datetime
from contextlib import asynccontextmanager
from typing import Optional

import boto3
import httpx
from fastapi import FastAPI, HTTPException, status
from pydantic import BaseModel, Field
from sqlalchemy import create_engine, Column, Integer, String, Float, DateTime, text
from sqlalchemy.orm import declarative_base, sessionmaker
from prometheus_fastapi_instrumentator import Instrumentator

# ── OpenTelemetry Tracing Setup ──────────────────────────────────────────────
# This sends trace data to Tempo (via the OTLP exporter) so you can see
# the full lifecycle of a request across all 3 microservices in Grafana.
from opentelemetry import trace
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor
from opentelemetry.exporter.otlp.proto.grpc.trace_exporter import OTLPSpanExporter
from opentelemetry.instrumentation.fastapi import FastAPIInstrumentor
from opentelemetry.sdk.resources import Resource

# ── Configuration via Environment Variables ──────────────────────────────────
# In Kubernetes, these come from ConfigMaps / External Secrets — never hardcoded.
DATABASE_URL     = os.getenv("DATABASE_URL", "postgresql://postgres:postgres@localhost:5432/kubeflow")
SQS_QUEUE_URL    = os.getenv("SQS_QUEUE_URL", "http://localhost:4566/000000000000/order-events")
USER_SERVICE_URL = os.getenv("USER_SERVICE_URL", "http://localhost:8002")
AWS_REGION       = os.getenv("AWS_REGION", "us-east-1")
AWS_ENDPOINT_URL = os.getenv("AWS_ENDPOINT_URL", None)  # For LocalStack in local dev
OTLP_ENDPOINT    = os.getenv("OTLP_ENDPOINT", "localhost:4317")
SERVICE_NAME     = "order-service"

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(SERVICE_NAME)

# ── Database Setup (SQLAlchemy) ──────────────────────────────────────────────
engine = create_engine(DATABASE_URL, pool_size=5, max_overflow=10)
SessionLocal = sessionmaker(bind=engine)
Base = declarative_base()


class OrderModel(Base):
    """Database table for orders."""
    __tablename__ = "orders"

    id          = Column(Integer, primary_key=True, index=True, autoincrement=True)
    user_id     = Column(Integer, nullable=False, index=True)
    product     = Column(String(255), nullable=False)
    quantity    = Column(Integer, nullable=False, default=1)
    total_price = Column(Float, nullable=False)
    status      = Column(String(50), default="PENDING")
    created_at  = Column(DateTime, default=datetime.utcnow)


# ── Pydantic Schemas (request/response validation) ──────────────────────────
class OrderCreate(BaseModel):
    user_id: int = Field(..., description="ID of the user placing the order")
    product: str = Field(..., min_length=1, max_length=255)
    quantity: int = Field(1, ge=1)
    total_price: float = Field(..., gt=0)


class OrderResponse(BaseModel):
    id: int
    user_id: int
    product: str
    quantity: int
    total_price: float
    status: str
    created_at: datetime

    class Config:
        from_attributes = True


# ── SQS Client ──────────────────────────────────────────────────────────────
def get_sqs_client():
    """Returns a boto3 SQS client. Uses LocalStack endpoint in local dev."""
    kwargs = {"region_name": AWS_REGION}
    if AWS_ENDPOINT_URL:
        kwargs["endpoint_url"] = AWS_ENDPOINT_URL
    return boto3.client("sqs", **kwargs)


def publish_order_event(order_id: int, user_id: int, product: str, action: str):
    """
    Publishes an event to SQS so the notification-service can pick it up.
    This is an asynchronous, decoupled communication pattern.
    If SQS is down, we log the error but don't fail the order creation —
    this is a common resilience pattern in production.
    """
    try:
        sqs = get_sqs_client()
        message = {
            "order_id": order_id,
            "user_id": user_id,
            "product": product,
            "action": action,
            "timestamp": datetime.utcnow().isoformat(),
        }
        sqs.send_message(
            QueueUrl=SQS_QUEUE_URL,
            MessageBody=json.dumps(message),
        )
        logger.info(f"Published {action} event for order {order_id} to SQS")
    except Exception as e:
        # Don't crash the request if SQS publish fails — log and move on
        logger.error(f"Failed to publish SQS event: {e}")


# ── App Lifecycle ────────────────────────────────────────────────────────────
@asynccontextmanager
async def lifespan(app: FastAPI):
    """Runs on startup: creates DB tables if they don't exist."""
    logger.info("Starting order-service...")
    Base.metadata.create_all(bind=engine)
    logger.info("Database tables created/verified.")
    yield
    logger.info("Shutting down order-service...")


# ── FastAPI App ──────────────────────────────────────────────────────────────
app = FastAPI(
    title="Order Service",
    description="Manages orders — part of KubeFlow Ops platform",
    version="1.0.0",
    lifespan=lifespan,
)

# Prometheus metrics — automatically exposes /metrics endpoint for Prometheus to scrape
Instrumentator().instrument(app).expose(app)

# OpenTelemetry tracing — sends spans to Tempo
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

tracer = trace.get_tracer(SERVICE_NAME)


# ── Health Probes ────────────────────────────────────────────────────────────
# Kubernetes uses these to determine if your pod is alive and ready to serve traffic.
# - /healthz → liveness probe (is the process alive?)
# - /readyz  → readiness probe (is the app ready to accept requests?)

@app.get("/healthz", tags=["Health"])
def liveness():
    """Liveness probe — Kubernetes restarts the pod if this fails."""
    return {"status": "alive"}


@app.get("/readyz", tags=["Health"])
def readiness():
    """Readiness probe — Kubernetes stops sending traffic if this fails."""
    try:
        db = SessionLocal()
        db.execute(text("SELECT 1"))
        db.close()
        return {"status": "ready", "database": "connected"}
    except Exception as e:
        raise HTTPException(status_code=503, detail=f"Not ready: {e}")


# ── CRUD Endpoints ───────────────────────────────────────────────────────────

@app.post("/orders", response_model=OrderResponse, status_code=status.HTTP_201_CREATED, tags=["Orders"])
async def create_order(order: OrderCreate):
    """
    Create a new order.
    1. Validates that the user exists by calling user-service (inter-service communication)
    2. Saves the order to PostgreSQL
    3. Publishes an event to SQS (async notification)
    """
    # Step 1: Validate user exists via REST call to user-service
    with tracer.start_as_current_span("validate-user"):
        try:
            async with httpx.AsyncClient() as client:
                resp = await client.get(f"{USER_SERVICE_URL}/users/{order.user_id}", timeout=5.0)
                if resp.status_code == 404:
                    raise HTTPException(status_code=404, detail=f"User {order.user_id} not found")
                resp.raise_for_status()
        except httpx.RequestError as e:
            logger.error(f"User service unreachable: {e}")
            raise HTTPException(status_code=503, detail="User service unavailable")

    # Step 2: Save order to database
    with tracer.start_as_current_span("save-order"):
        db = SessionLocal()
        try:
            db_order = OrderModel(**order.model_dump())
            db.add(db_order)
            db.commit()
            db.refresh(db_order)
        finally:
            db.close()

    # Step 3: Publish event to SQS
    publish_order_event(db_order.id, db_order.user_id, db_order.product, "ORDER_CREATED")

    logger.info(f"Order {db_order.id} created for user {db_order.user_id}")
    return db_order


@app.get("/orders", response_model=list[OrderResponse], tags=["Orders"])
def list_orders(user_id: Optional[int] = None, limit: int = 50):
    """List all orders, optionally filtered by user_id."""
    db = SessionLocal()
    try:
        query = db.query(OrderModel)
        if user_id:
            query = query.filter(OrderModel.user_id == user_id)
        return query.order_by(OrderModel.created_at.desc()).limit(limit).all()
    finally:
        db.close()


@app.get("/orders/{order_id}", response_model=OrderResponse, tags=["Orders"])
def get_order(order_id: int):
    """Get a specific order by ID."""
    db = SessionLocal()
    try:
        order = db.query(OrderModel).filter(OrderModel.id == order_id).first()
        if not order:
            raise HTTPException(status_code=404, detail="Order not found")
        return order
    finally:
        db.close()


@app.patch("/orders/{order_id}/status", response_model=OrderResponse, tags=["Orders"])
def update_order_status(order_id: int, new_status: str):
    """Update an order's status (e.g., PENDING → SHIPPED → DELIVERED)."""
    db = SessionLocal()
    try:
        order = db.query(OrderModel).filter(OrderModel.id == order_id).first()
        if not order:
            raise HTTPException(status_code=404, detail="Order not found")
        order.status = new_status
        db.commit()
        db.refresh(order)
        publish_order_event(order.id, order.user_id, order.product, f"ORDER_{new_status}")
        return order
    finally:
        db.close()


@app.delete("/orders/{order_id}", status_code=status.HTTP_204_NO_CONTENT, tags=["Orders"])
def delete_order(order_id: int):
    """Delete an order."""
    db = SessionLocal()
    try:
        order = db.query(OrderModel).filter(OrderModel.id == order_id).first()
        if not order:
            raise HTTPException(status_code=404, detail="Order not found")
        db.delete(order)
        db.commit()
    finally:
        db.close()
