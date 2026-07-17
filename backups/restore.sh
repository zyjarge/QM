#!/bin/bash
# QM 数据库恢复脚本
# 用法: ./restore.sh <backup_file.sql>

set -e

BACKUP_FILE="$1"
if [ -z "$BACKUP_FILE" ] || [ ! -f "$BACKUP_FILE" ]; then
    echo "❌ 备份文件不存在: $BACKUP_FILE"
    exit 1
fi
# 恢复
docker exec -i qm-postgres psql -U qm -d qm < "$BACKUP_FILE"

if [ $? -eq 0 ]; then
    echo "✅ 恢复成功"
else
    echo "❌ 恢复失败"
    exit 1
fi
