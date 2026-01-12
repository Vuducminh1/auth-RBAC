@echo off
REM Script to run all services for EMR Auth RBAC System
REM Usage: run-services.bat [option]
REM Options:
REM   docker    - Run with Docker Compose
REM   local     - Run services locally
REM   stop      - Stop Docker services

IF "%1"=="" GOTO help
IF "%1"=="docker" GOTO docker
IF "%1"=="local" GOTO local
IF "%1"=="stop" GOTO stop
GOTO help

:docker
echo ========================================
echo Starting all services with Docker...
echo ========================================
docker-compose up -d
echo.
echo Services started:
echo - PostgreSQL:  http://localhost:5432
echo - AI Service:  http://localhost:8000
echo - Backend:     http://localhost:8080
echo - pgAdmin:     docker-compose --profile tools up pgadmin
echo.
echo Use 'docker-compose logs -f' to view logs
GOTO end

:local
echo ========================================
echo Starting services locally...
echo ========================================
echo.
echo Step 1: Start PostgreSQL with Docker
docker-compose up -d postgres
echo.
echo Step 2: Start AI Service (in a new terminal)
echo   cd ..\PoweredAI-RBAC
echo   python -m venv .venv
echo   .venv\Scripts\activate
echo   pip install -r requirements.txt
echo   uvicorn api:app --reload --port 8000
echo.
echo Step 3: Start Backend (in a new terminal)
echo   .\mvnw spring-boot:run
echo.
echo Or run these commands manually in separate terminals.
GOTO end

:stop
echo ========================================
echo Stopping Docker services...
echo ========================================
docker-compose down
echo Services stopped.
GOTO end

:help
echo ========================================
echo EMR Auth RBAC - Run Services Script
echo ========================================
echo.
echo Usage: run-services.bat [option]
echo.
echo Options:
echo   docker    - Run all services with Docker Compose
echo   local     - Show instructions to run locally
echo   stop      - Stop all Docker services
echo.
echo Examples:
echo   run-services.bat docker
echo   run-services.bat stop
GOTO end

:end

