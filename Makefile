# =============================================================================
# Makefile — KubeFlow Ops
# =============================================================================
# Shortcuts for common operations. Run: make <target>
# =============================================================================

.PHONY: help local-up local-down deploy plan teardown

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

# ── Local Development ────────────────────────────────────────────────────────

local-up: ## Start all services locally with Docker Compose
	docker-compose up --build -d
	@echo ""
	@echo "✅ Services running:"
	@echo "   Order Service:        http://localhost:8001/docs"
	@echo "   User Service:         http://localhost:8002/docs"
	@echo "   Notification Service: http://localhost:8003/docs"

local-down: ## Stop all local services
	docker-compose down -v
	@echo "✅ All services stopped and volumes removed"

local-logs: ## View live logs from all services
	docker-compose logs -f

# ── Terraform ────────────────────────────────────────────────────────────────

init: ## Initialize Terraform
	cd terraform/environments/dev && terraform init

plan: ## Run Terraform plan
	cd terraform/environments/dev && terraform plan

deploy: ## Deploy infrastructure with Terraform
	cd terraform/environments/dev && terraform apply

# ── Teardown (DESTROYS everything!) ──────────────────────────────────────────

teardown: ## ⚠️  DESTROY all AWS infrastructure
	@echo "⚠️  WARNING: This will destroy ALL AWS resources!"
	@echo "Press Ctrl+C to cancel, or wait 5 seconds..."
	@sleep 5
	cd terraform/environments/dev && terraform destroy
	@echo ""
	@echo "✅ All infrastructure destroyed"
	@echo ""
	@echo "Optional cleanup:"
	@echo "  aws s3 rb s3://kubeflow-ops-terraform-state --force"
	@echo "  aws dynamodb delete-table --table-name kubeflow-ops-terraform-lock"

# ── Testing ──────────────────────────────────────────────────────────────────

test-order: ## Run order-service tests
	cd apps/order-service && pip install -r requirements.txt && pytest tests/ -v

test-user: ## Run user-service tests
	cd apps/user-service && pip install -r requirements.txt && pytest tests/ -v

test-notification: ## Run notification-service tests
	cd apps/notification-service && pip install -r requirements.txt && pytest tests/ -v

test-all: test-order test-user test-notification ## Run all tests

# ── Validation ───────────────────────────────────────────────────────────────

validate-tf: ## Validate Terraform configs
	cd terraform/environments/dev && terraform init -backend=false && terraform validate

validate-docker: ## Build all Docker images locally
	docker build -t order-service:test apps/order-service/
	docker build -t user-service:test apps/user-service/
	docker build -t notification-service:test apps/notification-service/
	@echo "✅ All Docker images built successfully"
