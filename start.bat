@echo off
REM CTF竞赛系统 - Windows启动脚本

echo ==========================================
echo 🚀 校园 CTF 在线竞赛平台 - 启动脚本
echo ==========================================
echo.

REM 检查Docker
docker --version >nul 2>&1
if errorlevel 1 (
    echo ❌ 未检测到Docker，请先安装Docker Desktop
    echo 📥 下载地址: https://www.docker.com/products/docker-desktop
    pause
    exit /b 1
)

REM 检查Docker Compose
docker-compose --version >nul 2>&1
if errorlevel 1 (
    echo ❌ 未检测到Docker Compose，请检查Docker安装
    pause
    exit /b 1
)

echo ✅ Docker环境检查完毕
echo.

echo 📂 项目目录: %CD%
echo.

echo 🔄 启动所有服务（这可能需要1-2分钟）...
echo 💡 提示: 首次启动会下载镜像并编译代码，请耐心等待...
echo.

docker-compose up --build

echo.
echo ==========================================
echo ✅ 所有服务已启动
echo ==========================================
echo.
echo 📱 访问地址:
echo    前端: http://localhost:3615
echo    后端API: http://localhost:8615/api
echo    MySQL: localhost:3306
echo.
echo 🔑 测试账号:
echo    管理员: root / admin123
echo    用户: 2024001 / 123456
echo.
echo 📖 查看日志:
echo    docker-compose logs [service_name]
echo.
echo 🛑 停止服务:
echo    docker-compose down
echo.
pause
