# =============================================================================
# Unit Tests — User Service
# =============================================================================
from fastapi.testclient import TestClient
from unittest.mock import patch


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


def test_get_user_not_found():
    """Test that requesting a non-existent user returns 404."""
    from unittest.mock import MagicMock

    mock_session = MagicMock()
    mock_session.query.return_value.filter.return_value.first.return_value = None

    with patch("main.SessionLocal", return_value=mock_session):
        response = client.get("/users/999")
        assert response.status_code == 404
