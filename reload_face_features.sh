#!/bin/bash
#
# 人脸特征分批加载脚本
# 用于生产环境分批加载大量人脸特征数据
#

# 配置
SERVICE_URL="http://localhost:7082"
BATCH_SIZE=1000
SLEEP_SECONDS=2
LOG_FILE="/var/log/face_feature_reload.log"

# 记录日志
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

log "========== 开始加载人脸特征数据 =========="

# 获取总数
RESPONSE=$(curl -s "${SERVICE_URL}/api/face/load/count")
TOTAL=$(echo "$RESPONSE" | grep -o '"totalCount":[0-9]*' | grep -o '[0-9]*')

if [ -z "$TOTAL" ] || [ "$TOTAL" -eq 0 ]; then
    log "错误：无法获取数据总数或总数为 0"
    exit 1
fi

log "数据总数: $TOTAL"
log "批次大小: $BATCH_SIZE"

# 计算批次数
BATCH_COUNT=$(( (TOTAL + BATCH_SIZE - 1) / BATCH_SIZE ))
log "总批次数: $BATCH_COUNT"

# 分批加载
OFFSET=0
SUCCESS_COUNT=0
FAIL_COUNT=0

while [ $OFFSET -lt $TOTAL ]; do
    CURRENT_BATCH=$(( OFFSET / BATCH_SIZE + 1 ))
    log "加载第 $CURRENT_BATCH/$BATCH_COUNT 批: offset=$OFFSET, batchSize=$BATCH_SIZE"
    
    RESPONSE=$(curl -s -X POST "${SERVICE_URL}/api/face/load/batch?batchSize=$BATCH_SIZE&offset=$OFFSET")
    
    # 检查响应
    if echo "$RESPONSE" | grep -q '"code":0'; then
        log "  批次 $CURRENT_BATCH 加载成功"
        SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
    else
        log "  批次 $CURRENT_BATCH 加载失败: $RESPONSE"
        FAIL_COUNT=$((FAIL_COUNT + 1))
    fi
    
    OFFSET=$((OFFSET + BATCH_SIZE))
    
    # 避免过度占用资源
    if [ $OFFSET -lt $TOTAL ]; then
        sleep $SLEEP_SECONDS
    fi
done

log "========== 加载完成 =========="
log "总批次: $BATCH_COUNT, 成功: $SUCCESS_COUNT, 失败: $FAIL_COUNT"

# 验证加载结果
log "验证 Hazelcast 中的数据..."
ALGO_RESPONSE=$(curl -s "http://localhost:7081/api/algo/face/feature/count")
LOADED_COUNT=$(echo "$ALGO_RESPONSE" | grep -o '"count":[0-9]*' | grep -o '[0-9]*')

if [ -n "$LOADED_COUNT" ]; then
    log "Hazelcast 中的数据总数: $LOADED_COUNT"
else
    log "警告：无法获取 Hazelcast 中的数据总数"
fi

log "========== 脚本执行完毕 =========="

exit 0

