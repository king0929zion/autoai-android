@echo off
echo ========================================
echo   AutoAI Android - GitHub Push Script
echo ========================================
echo.

REM Check if Git is installed
git --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Git is not installed
    echo Please install Git: https://git-scm.com/download/win
    pause
    exit /b 1
)

echo [1/5] Checking Git configuration...
git config --global user.name >nul 2>&1
if %errorlevel% neq 0 (
    echo [WARNING] Git username not configured
    set /p username="Enter your GitHub username: "
    git config --global user.name "%username%"
)

git config --global user.email >nul 2>&1
if %errorlevel% neq 0 (
    echo [WARNING] Git email not configured
    set /p email="Enter your email: "
    git config --global user.email "%email%"
)

echo Username:
git config --global user.name
echo Email:
git config --global user.email
echo.

REM Check if already initialized
if not exist .git (
    echo [2/5] Initializing Git repository...
    git init
    git branch -M main
) else (
    echo [2/5] Git repository already exists
)

REM Check if remote is configured
git remote get-url origin >nul 2>&1
if %errorlevel% neq 0 (
    echo [INFO] Remote repository not configured
    echo.
    echo Please create a repository on GitHub first
    echo Format: https://github.com/YOUR_USERNAME/autoai-android.git
    echo.
    set /p repo_url="Repository URL: "
    git remote add origin %repo_url%
    echo Remote repository added: %repo_url%
) else (
    echo Remote repository:
    git remote get-url origin
)
echo.

echo [3/5] Adding files to staging area...
git add .
echo Files added
echo.

echo [4/5] Creating commit...
set /p commit_msg="Enter commit message (press Enter for default): "
if "%commit_msg%"=="" (
    set commit_msg=Update: Fix compilation issues and UI optimization
)
git commit -m "%commit_msg%"
echo.

echo [5/5] Pushing to GitHub...
echo Pushing, please wait...
git push -u origin main
if %errorlevel% equ 0 (
    echo.
    echo ========================================
    echo   SUCCESS! Push completed
    echo ========================================
    echo.
    echo Next steps:
    echo 1. Visit your GitHub repository to view the code
    echo 2. Go to Actions tab to view automatic build
    echo 3. Wait 3-5 minutes, then download compiled APK
    echo.
    echo For details, see: GITHUB_DEPLOY_GUIDE.md
    echo ========================================
) else (
    echo.
    echo ========================================
    echo   FAILED! Push failed
    echo ========================================
    echo.
    echo Possible reasons:
    echo 1. Incorrect repository URL
    echo 2. No push permission
    echo 3. Network connection issue
    echo 4. Need to pull remote updates first
    echo.
    echo Solutions:
    echo 1. Check if repository URL is correct
    echo 2. Confirm you have write access to the repository
    echo 3. Try manually: git push -u origin main
    echo 4. If using HTTPS, you may need Personal Access Token
    echo    Visit: https://github.com/settings/tokens
    echo.
)

pause
