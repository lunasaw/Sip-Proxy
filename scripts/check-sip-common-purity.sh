#!/usr/bin/env bash
#
# 校验 sip-common 模块协议纯净性：禁止出现 GB28181 专用关键词。
# CI 在 mvn verify 阶段调用；任意命中即让构建失败。
#
# 边界规则参考：doc/PROTOCOL-DECOUPLING-PLAN.md §五

set -euo pipefail

PATTERN='gb28181|GB28181|gbproxy|Catalog|MobilePosition|GbSession|GbSip|GbUtil'

# 排除注释行（行首可有空白 + // 或 *）
hits=$(grep -rEn "$PATTERN" sip-common/src/main/java --include="*.java" \
       | grep -vE '^\s*//|^\s*\*' || true)

if [ -n "$hits" ]; then
  echo "❌ sip-common 存在 GB28181 耦合："
  echo "$hits"
  exit 1
fi
echo "✅ sip-common 协议纯净"
