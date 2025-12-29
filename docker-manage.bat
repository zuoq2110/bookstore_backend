@echo off
echo ========================================
echo     Web Ban Sach Docker Management
echo ========================================

:menu
echo.
echo 1. Start all services (build if needed)
echo 2. Stop all services
echo 3. Rebuild and restart
echo 4. View logs
echo 5. Clean all (remove containers, volumes, networks)
echo 6. Check services status
echo 7. Exit
echo.
set /p choice="Choose an option (1-7): "

if "%choice%"=="1" goto start
if "%choice%"=="2" goto stop
if "%choice%"=="3" goto rebuild
if "%choice%"=="4" goto logs
if "%choice%"=="5" goto clean
if "%choice%"=="6" goto status
if "%choice%"=="7" goto exit
echo Invalid choice!
goto menu

:start
echo Starting all services...
docker-compose up -d --build
goto menu

:stop
echo Stopping all services...
docker-compose down
goto menu

:rebuild
echo Rebuilding and restarting all services...
docker-compose down
docker-compose build --no-cache
docker-compose up -d
goto menu

:logs
echo Viewing logs...
docker-compose logs -f
goto menu

:clean
echo WARNING: This will remove all containers, volumes, and networks!
set /p confirm="Are you sure? (y/n): "
if /i "%confirm%"=="y" (
    echo Cleaning up...
    docker-compose down -v --remove-orphans
    docker system prune -f
    echo Cleanup completed!
) else (
    echo Cleanup cancelled.
)
goto menu

:status
echo Checking services status...
docker-compose ps
docker-compose logs --tail=10
goto menu

:exit
echo Goodbye!
exit /b 0