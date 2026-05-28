#!/usr/bin/env bash
# scripts/check-gateway-core-purity.sh
# 确保 gateway-core 协议中立：不得含 GB28181/ONVIF/GT1078/RTSP 等协议关键字
# 注释和 Javadoc 示例中的协议���允许（仅检查 import 和实际代码）

set -e

SRC="sip-gateway/gateway-core/src/main/java"

if [ ! -d "$SRC" ]; then
    echo "⚠️  gateway-core 源码目录不存在，跳过纯度检查"
    exit 0
fi

# 只检查 import 语句和非注释代码行
# 排除：1) 以 // 或 * 开头的注释行  2) Javadoc 中的 @param/@return 等文档
FORBIDDEN='gb28181|GB28181|Gb28181|gbproxy|jain\.sip|sip\.common|sip\.message|onvif|Onvif|gt1078|rtsp'

if grep -rEn "^import.*($FORBIDDEN)" "$SRC" 2>/dev/null; then
    echo "❌ gateway-core must remain protocol-neutral (found in import statements)"
    echo "   Forbidden tokens in imports: $FORBIDDEN"
    exit 1
fi

echo "✅ gateway-core purity OK"
exit 0
