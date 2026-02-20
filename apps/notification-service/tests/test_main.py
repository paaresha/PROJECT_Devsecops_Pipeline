# =============================================================================
# Unit Tests — Notification Service
# =============================================================================
from fastapi.testclient import TestClient
from unittest.mock import patch, MagicMock

mock_redis = MagicMock()

with patch("main.redis") as mock_redis_module:
    mock_redis_module.from_url.return_value = mock_redis
    from main import app

client = TestClient(app)


def test_liveness():
    """Test that the liveness probe returns 200."""
    response = client.get("/healthz")
    assert response.status_code == 200
    assert response.json()["status"] == "alive"


def test_list_notifications_empty():
    """Test that notifications list starts empty."""
    response = client.get("/notifications")
    assert response.status_code == 200
    assert isinstance(response.json(), list)


def test_notification_stats():
    """Test notification stats endpoint."""
    response = client.get("/notifications/stats")
    assert response.status_code == 200
    data = response.json()
    assert "total_processed" in data
    assert "actions" in data
