# =============================================================================
# Multi-Stage Dockerfile — Order Service
# =============================================================================
# Stage 1: Install dependencies in a temporary layer
# Stage 2: Copy only the installed packages + app code into a slim final image
# Result: ~120MB image instead of ~900MB. Faster deploys, smaller attack surface.
# =============================================================================

# ── Stage 1: Builder ─────────────────────────────────────────────────────────
# We use the full Python image here because it has gcc and other build tools
# needed to compile some Python packages (like psycopg2-binary).
FROM python:3.12-slim AS builder

WORKDIR /build

# Copy requirements first — Docker caches this layer, so if requirements
# haven't changed, it skips the slow pip install on subsequent builds.
COPY requirements.txt .
RUN pip install --no-cache-dir --prefix=/install -r requirements.txt

# ── Stage 2: Production ─────────────────────────────────────────────────────
# We use the slim image — no gcc, no build tools, no pip cache.
# Only the Python runtime + our installed packages + app code.
FROM python:3.12-slim

# Security: Run as non-root user (Kyverno policy will enforce this)
RUN groupadd -r appuser && useradd -r -g appuser appuser

WORKDIR /app

# Copy installed Python packages from the builder stage
COPY --from=builder /install /usr/local

# Copy application code
COPY main.py .

# Switch to non-root user
USER appuser

# Expose port (documentation + Kubernetes service discovery)
EXPOSE 8001

# Health check — Docker-level (Kubernetes probes are separate)
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8001/healthz')" || exit 1

# Run with uvicorn (production ASGI server)
# --host 0.0.0.0 makes it accessible from outside the container
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8001"]
