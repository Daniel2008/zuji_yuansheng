@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ========================================
echo 环境切换工具 Environment Switch Tool
echo ========================================
echo.

if "%1"=="" (
    echo 用法 Usage: switch_env.bat [dev^|prod]
    echo   dev  - 切换到开发环境 Switch to development environment
    echo   prod - 切换到正式环境 Switch to production environment
    echo.
    goto :end
)

set "target_env=%1"

if not "%target_env%"=="dev" if not "%target_env%"=="prod" (
    echo 错误 Error: 无效的环境参数 Invalid environment parameter: %target_env%
    echo 有效值 Valid values: dev, prod
    goto :end
)

set "config_dir=%~dp0config"
set "app_config=%config_dir%\app.properties"

if not exist "%config_dir%" (
    echo 错误 Error: 配置目录不存在 Config directory does not exist: %config_dir%
    goto :end
)

if not exist "%app_config%" (
    echo 错误 Error: 配置文件不存在 Config file does not exist: %app_config%
    goto :end
)

echo 正在切换到 %target_env% 环境... Switching to %target_env% environment...
echo.

rem 创建临时文件来更新配置
set "temp_file=%config_dir%\app.properties.tmp"

rem 读取原文件并更新 current.environment
for /f "usebackq delims=" %%a in ("%app_config%") do (
    set "line=%%a"
    if "!line:~0,19!"=="current.environment" (
        echo current.environment=%target_env% >> "%temp_file%"
    ) else (
        echo !line! >> "%temp_file%"
    )
)

rem 替换原文件
move "%temp_file%" "%app_config%" >nul

if %target_env%==dev (
    echo ✓ 已切换到开发环境 Successfully switched to development environment
) else (
    echo ✓ 已切换到正式环境 Successfully switched to production environment
)

echo.
echo ========================================
echo 当前环境配置 Current Environment Configuration
echo ========================================
echo 配置文件 Config File: app.properties
echo 当前环境 Current Environment: %target_env%
echo ========================================

rem 显示当前环境的配置
for /f "tokens=1,2 delims==" %%a in ('type "%app_config%" 2^>nul ^| findstr /v "^#" ^| findstr /v "^$" ^| findstr "^%target_env%\."') do (
    set "key=%%a"
    set "value=%%b"
    if defined value (
        set "display_key=!key:%target_env%.=!"
        echo !display_key! = !value!
    )
)

echo ========================================

:end
pause