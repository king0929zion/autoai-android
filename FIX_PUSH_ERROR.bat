@echo off
echo ================================================
echo   Fix Git Push Error - Add Remote Repository
echo ================================================
echo.
echo Current status:
echo - Git repository: OK (initialized)
echo - Local commits: OK (already committed)
echo - Remote repository: NOT CONFIGURED (this is the problem!)
echo.
echo ================================================
echo.

echo Step 1: Create repository on GitHub
echo -----------------------------------------------
echo 1. Open: https://github.com/new
echo 2. Repository name: autoai-android
echo 3. Visibility: Public
echo 4. Click "Create repository"
echo.
echo Press any key after creating repository...
pause >nul
echo.

echo Step 2: Enter your repository information
echo -----------------------------------------------
set /p username="Enter your GitHub username: "
echo.
echo Your repository URL will be:
echo https://github.com/%username%/autoai-android.git
echo.
set /p confirm="Is this correct? (Y/N): "
if /i not "%confirm%"=="Y" (
    echo Cancelled. Please run the script again.
    pause
    exit /b
)
echo.

echo Step 3: Adding remote repository...
echo -----------------------------------------------
git remote add origin https://github.com/%username%/autoai-android.git
if %errorlevel% equ 0 (
    echo SUCCESS! Remote repository added.
) else (
    echo ERROR! Failed to add remote repository.
    echo Trying to update existing remote...
    git remote set-url origin https://github.com/%username%/autoai-android.git
)
echo.

echo Step 4: Verify remote configuration
echo -----------------------------------------------
git remote -v
echo.

echo Step 5: Ready to push!
echo -----------------------------------------------
echo Now pushing to GitHub...
echo You will be prompted for username and password.
echo.
echo Username: %username%
echo Password: USE YOUR PERSONAL ACCESS TOKEN (not GitHub password)
echo.
echo If you don't have a token:
echo 1. Go to: https://github.com/settings/tokens
echo 2. Generate new token (classic)
echo 3. Check "repo" permission
echo 4. Copy the token
echo.
pause
echo.

echo Pushing...
git push -u origin main

if %errorlevel% equ 0 (
    echo.
    echo ================================================
    echo   SUCCESS! Code pushed to GitHub!
    echo ================================================
    echo.
    echo Next steps:
    echo 1. Visit: https://github.com/%username%/autoai-android
    echo 2. Click "Actions" tab
    echo 3. Wait 3-5 minutes for build
    echo 4. Download APK from "Artifacts"
    echo.
) else (
    echo.
    echo ================================================
    echo   PUSH FAILED
    echo ================================================
    echo.
    echo Common issues:
    echo 1. Authentication failed
    echo    - Make sure you use token as password (not GitHub password)
    echo    - Get token at: https://github.com/settings/tokens
    echo.
    echo 2. Repository not found
    echo    - Make sure you created the repository on GitHub
    echo    - Check the repository name is correct
    echo.
    echo 3. Permission denied
    echo    - Make sure you own the repository
    echo    - Check token has "repo" permission
    echo.
)

echo.
pause
