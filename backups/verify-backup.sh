#!/bin/bash
# QM 备份恢复验证脚本
# 用法: ./verify-backup.sh

set -e

echo "=== QM 备份恢复验证 (A9) ==="

# 1. 备份
echo "1. 执行备份..."
BACKUP_NAME="verify_backup_$(date +%Y%m%d_%H%M%S)"
./backup.sh "$BACKUP_NAME"
BACKUP_FILE="$BACKUP_NAME.sql"

# 2. 记录当前数据量
echo "2. 记录当前数据..."
REQ_COUNT_BEFORE=$(docker exec qm-postgres psql -U qm -d qm -t -c "SELECT COUNT(*) FROM requirements;")
echo "   需求数: $REQ_COUNT_BEFORE"

# 3. 模拟删库（危险操作，仅测试环境）
echo "3. 模拟删库（DROP requirements 表）..."
docker exec qm-postgres psql -U qm -d qm -c "DROP TABLE requirements CASCADE;"

REQ_COUNT_AFTER_DELETE=$(docker exec qm-postgres psql -U qm -d qm -t -c "SELECT COUNT(*) FROM requirements;" 2>/dev/null || echo "0")
echo "   删除后需求数: $REQ_COUNT_AFTER_DELETE"

# 4. 恢复
echo "4. 执行恢复..."
./restore.sh "$BACKUP_FILE"

# 5. 验证
echo "5. 验证恢复结果..."
REQ_COUNT_RESTORED=$(docker exec qm-postgres psql -U qm -d qm -t -c "SELECT COUNT(*) FROM requirements;")
echo "   恢复后需求数: $REQ_COUNT_RESTORED"

if [ "$REQ_COUNT_BEFORE" = "$REQ_COUNT_RESTORED" ]; then
    echo "✅ 备份恢复验证通过"
    exit 0
else
    echo "❌ 验证失败: 恢复后数据不一致"
    exit 1
fi
