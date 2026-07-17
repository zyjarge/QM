#!/bin/bash
# QM 数据库备份脚本
# 用法: ./backup.sh [backup_name]

set -e

BACKUP_DIR="$(cd "$(dirname "$0")" && pwd)"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_NAME="${1:-qm_backup_$TIMESTAMP}"
BACKUP_FILE="$BACKUP_DIR/$BACKUP_NAME.sql"

echo "=== QM 数据库备份 ==="
echo "备份文件: $BACKUP_FILE"

# pg_dump 导出
docker exec qm-postgres pg_dump -U qm -d qm --clean --if-exists > "$BACKUP_FILE"

if [ $? -eq 0 ]; then
    SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
    echo "✅ 备份成功: $SIZE"
    echo "$BACKUP_FILE"
else
    echo "❌ 备份失败"
    exit 1
fi
