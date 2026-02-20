# =============================================================================
# User Service — FastAPI Microservice
# =============================================================================
# Handles CRUD operations for users.
# - Stores user data in PostgreSQL
# - Called by the order-service to validate user existence before creating orders
# - This demonstrates inter-service REST communication in a microservices arch
# =============================================================================

import os
import logging
from datetime import datetime
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException, status
from pydantic import BaseModel, Field, EmailStr
from sqlalchemy import create_engine, Column, Integer, String, DateTime, text
from sqlalchemy.orm import declarative_base, sessionmaker
from prometheus_fastapi_instrumentator import Instrumentator

# ── OpenTelemetry Tracing ────────────────────────────────────────────────────
from opentelemetry import trace
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor
from opentelemetry.exporter.otlp.proto.grpc.trace_exporter import OTLPSpanExporter
from opentelemetry.instrumentation.fastapi import FastAPIInstrumentor
from opentelemetry.sdk.resources import Resource

# ── Configuration ────────────────────────────────────────────────────────────
DATABASE_URL  = os.getenv("DATABASE_URL", "postgresql://postgres:postgres@localhost:5432/kubeflow")
OTLP_ENDPOINT = os.getenv("OTLP_ENDPOINT", "localhost:4317")
SERVICE_NAME  = "user-service"

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(SERVICE_NAME)

# ── Database Setup ───────────────────────────────────────────────────────────
engine = create_engine(DATABASE_URL, pool_size=5, max_overflow=10)
SessionLocal = sessionmaker(bind=engine)
Base = declarative_base()


class UserModel(Base):
    """Database table for users."""
    __tablename__ = "users"

    id         = Column(Integer, primary_key=True, index=True, autoincrement=True)
    name       = Column(String(255), nullable=False)
    email      = Column(String(255), unique=True, nullable=False, index=True)
    role       = Column(String(50), default="customer")
    created_at = Column(DateTime, default=datetime.utcnow)


# ── Pydantic Schemas ─────────────────────────────────────────────────────────
class UserCreate(BaseModel):
    name: str = Field(..., min_length=1, max_length=255)
    email: str = Field(..., min_length=5, max_length=255)
    role: str = Field("customer", max_length=50)


class UserResponse(BaseModel):
    id: int
    name: str
    email: str
    role: str
    created_at: datetime

    class Config:
        from_attributes = True


# ── App Lifecycle ────────────────────────────────────────────────────────────
@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Starting user-service...")
    Base.metadata.create_all(bind=engine)
    logger.info("Database tables created/verified.")
    yield
    logger.info("Shutting down user-service...")


# ── FastAPI App ──────────────────────────────────────────────────────────────
app = FastAPI(
    title="User Service",
    description="Manages users — part of KubeFlow Ops platform",
    version="1.0.0",
    lifespan=lifespan,
)

# Prometheus metrics endpoint at /metrics
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
    """Readiness probe — Kubernetes stops sending traffic if this fails."""
    try:
        db = SessionLocal()
        db.execute(text("SELECT 1"))
        db.close()
        return {"status": "ready", "database": "connected"}
    except Exception as e:
        raise HTTPException(status_code=503, detail=f"Not ready: {e}")


# ── CRUD Endpoints ───────────────────────────────────────────────────────────

@app.post("/users", response_model=UserResponse, status_code=status.HTTP_201_CREATED, tags=["Users"])
def create_user(user: UserCreate):
    """Create a new user."""
    db = SessionLocal()
    try:
        # Check for duplicate email
        existing = db.query(UserModel).filter(UserModel.email == user.email).first()
        if existing:
            raise HTTPException(status_code=409, detail="Email already registered")

        db_user = UserModel(**user.model_dump())
        db.add(db_user)
        db.commit()
        db.refresh(db_user)
        logger.info(f"User {db_user.id} created: {db_user.email}")
        return db_user
    finally:
        db.close()


@app.get("/users", response_model=list[UserResponse], tags=["Users"])
def list_users(limit: int = 50):
    """List all users."""
    db = SessionLocal()
    try:
        return db.query(UserModel).order_by(UserModel.created_at.desc()).limit(limit).all()
    finally:
        db.close()


@app.get("/users/{user_id}", response_model=UserResponse, tags=["Users"])
def get_user(user_id: int):
    """
    Get a specific user by ID.
    This endpoint is called by the order-service to validate user existence
    before creating an order (inter-service communication via REST).
    """
    db = SessionLocal()
    try:
        user = db.query(UserModel).filter(UserModel.id == user_id).first()
        if not user:
            raise HTTPException(status_code=404, detail="User not found")
        return user
    finally:
        db.close()


@app.put("/users/{user_id}", response_model=UserResponse, tags=["Users"])
def update_user(user_id: int, user_update: UserCreate):
    """Update a user's details."""
    db = SessionLocal()
    try:
        user = db.query(UserModel).filter(UserModel.id == user_id).first()
        if not user:
            raise HTTPException(status_code=404, detail="User not found")
        user.name = user_update.name
        user.email = user_update.email
        user.role = user_update.role
        db.commit()
        db.refresh(user)
        logger.info(f"User {user.id} updated")
        return user
    finally:
        db.close()


@app.delete("/users/{user_id}", status_code=status.HTTP_204_NO_CONTENT, tags=["Users"])
def delete_user(user_id: int):
    """Delete a user."""
    db = SessionLocal()
    try:
        user = db.query(UserModel).filter(UserModel.id == user_id).first()
        if not user:
            raise HTTPException(status_code=404, detail="User not found")
        db.delete(user)
        db.commit()
        logger.info(f"User {user.id} deleted")
    finally:
        db.close()
