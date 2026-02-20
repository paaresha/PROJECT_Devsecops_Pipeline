# =============================================================================
# Unit Tests — Order Service
# =============================================================================
from fastapi.testclient import TestClient
from unittest.mock import patch, MagicMock


# Mock database and external services before importing the app
with patch("main.create_engine"), \
     patch("main.SessionLocal"), \
     patch("main.Base"):
    from main import app

client = TestClient(app)


def test_liveness():
    """Test that the liveness probe returns 200."""
    response = client.get("/healthz")
    assert response.status_code == 200
    assert response.json()["status"] == "alive"


def test_create_order_missing_user():
    """Test that creating an order for a non-existent user returns an error."""
    with patch("main.httpx.AsyncClient") as mock_client:
        mock_response = MagicMock()
        mock_response.status_code = 404
        mock_client.return_value.__aenter__.return_value.get.return_value = mock_response

        response = client.post("/orders", json={
            "user_id": 999,
            "product": "Test Product",
            "quantity": 1,
            "total_price": 29.99,
        })
        assert response.status_code in [404, 503]
