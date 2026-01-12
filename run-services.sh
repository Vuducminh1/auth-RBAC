#!/bin/bash
# Script to run all services for EMR Auth RBAC System
# Usage: ./run-services.sh [option]
# Options:
#   docker    - Run with Docker Compose
#   local     - Run services locally
#   stop      - Stop Docker services

case "$1" in
    docker)
        echo "========================================"
        echo "Starting all services with Docker..."
        echo "========================================"
        docker-compose up -d
        echo ""
        echo "Services started:"
        echo "- PostgreSQL:  http://localhost:5432"
        echo "- AI Service:  http://localhost:8000"
        echo "- Backend:     http://localhost:8080"
        echo "- pgAdmin:     docker-compose --profile tools up pgadmin"
        echo ""
        echo "Use 'docker-compose logs -f' to view logs"
        ;;
    local)
        echo "========================================"
        echo "Starting services locally..."
        echo "========================================"
        echo ""
        echo "Step 1: Start PostgreSQL with Docker"
        docker-compose up -d postgres
        echo ""
        echo "Step 2: Start AI Service (in a new terminal)"
        echo "  cd ../PoweredAI-RBAC"
        echo "  python -m venv .venv"
        echo "  source .venv/bin/activate"
        echo "  pip install -r requirements.txt"
        echo "  uvicorn api:app --reload --port 8000"
        echo ""
        echo "Step 3: Start Backend (in a new terminal)"
        echo "  ./mvnw spring-boot:run"
        ;;
    stop)
        echo "========================================"
        echo "Stopping Docker services..."
        echo "========================================"
        docker-compose down
        echo "Services stopped."
        ;;
    *)
        echo "========================================"
        echo "EMR Auth RBAC - Run Services Script"
        echo "========================================"
        echo ""
        echo "Usage: ./run-services.sh [option]"
        echo ""
        echo "Options:"
        echo "  docker    - Run all services with Docker Compose"
        echo "  local     - Show instructions to run locally"
        echo "  stop      - Stop all Docker services"
        echo ""
        echo "Examples:"
        echo "  ./run-services.sh docker"
        echo "  ./run-services.sh stop"
        ;;
esac


