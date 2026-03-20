@echo off
echo Running predeploy commands for Firebase Functions...

REM Navigate to the functions directory using the passed argument
REM %1 will be the RESOURCE_DIR from firebase.json
cd %1

echo Running npm run lint...
call npm run lint

IF %ERRORLEVEL% NEQ 0 (
    echo npm run lint failed! Exiting.
    exit /b %ERRORLEVEL%
)

echo Running npm run build...
call npm run build

IF %ERRORLEVEL% NEQ 0 (
    echo npm run build failed! Exiting.
    exit /b %ERRORLEVEL%
)

echo Predeploy commands completed successfully.
exit /b 0