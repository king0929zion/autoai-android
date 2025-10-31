@echo off
echo ================================================
echo   Push Fixed Code to GitHub
echo ================================================
echo.
echo I've removed the token from the files.
echo Now pushing the corrected version...
echo.
pause
echo.

git push -f origin main

if %errorlevel% equ 0 (
    echo.
    echo ================================================
    echo   SUCCESS! Code pushed to GitHub!
    echo ================================================
    echo.
    echo Next steps:
    echo 1. Visit: https://github.com/king0929zion/autoai-android
    echo 2. Click "Actions" tab
    echo 3. Wait 3-5 minutes for build
    echo 4. Download APK from "Artifacts"
    echo.
    echo WARNING: The old token was exposed and should be revoked!
    echo Please visit: https://github.com/settings/tokens
    echo.
) else (
    echo.
    echo ================================================
    echo   PUSH FAILED
    echo ================================================
    echo.
    echo Please try again or contact support.
    echo.
)

pause
