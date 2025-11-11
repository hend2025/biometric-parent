@echo off
echo ========== 人脸特征加载功能测试 ==========

echo.
echo 1. 检查算法服务状态...
curl -s http://localhost:7081/api/algo/face/feature/count
echo.

echo.
echo 2. 查询数据库中的人脸特征总数...
curl -s http://localhost:7082/api/face/load/count
echo.

echo.
echo 3. 手动触发加载...
curl -s -X POST http://localhost:7082/api/face/load/all
echo.

echo.
echo 4. 等待 10 秒，让加载完成...
timeout /t 10 /nobreak

echo.
echo 5. 查询 Hazelcast 中的人脸特征总数...
curl -s http://localhost:7081/api/algo/face/feature/count
echo.

echo.
echo ========== 测试完成 ==========
pause

