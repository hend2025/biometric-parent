#!/bin/bash

echo "========== 人脸特征加载功能测试 =========="

# 1. 检查算法服务状态
echo ""
echo "1. 检查算法服务状态..."
curl -s http://localhost:7081/api/algo/face/feature/count
echo ""

# 2. 查询数据库中的人脸特征总数
echo ""
echo "2. 查询数据库中的人脸特征总数..."
curl -s http://localhost:7082/api/face/load/count
echo ""

# 3. 手动触发加载
echo ""
echo "3. 手动触发加载..."
curl -s -X POST http://localhost:7082/api/face/load/all
echo ""

# 4. 等待 10 秒
echo ""
echo "4. 等待 10 秒，让加载完成..."
sleep 10

# 5. 再次查询 Hazelcast 中的数量
echo ""
echo "5. 查询 Hazelcast 中的人脸特征总数..."
curl -s http://localhost:7081/api/algo/face/feature/count
echo ""

echo ""
echo "========== 测试完成 =========="

