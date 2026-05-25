#!/usr/bin/env bash
#
# 校验业务模块（gb28181-client / gb28181-server）不直接依赖 RFC 4566 base parser。
# 所有 SDP 解析必须经 Gb28181SdpParser，否则编译期阻塞。
#
# 关联：doc/SDP-PARSER-LAYERING-PLAN.md §4.4
# 触发原因：2026-05-25 InviteResponseProcessor 越层调 SipUtils.parseSdp 导致 ACK 漏发

set -euo pipefail

PATTERN='io\.github\.lunasaw\.sip\.common\.sdp\.Rfc4566SdpParser|io\.github\.lunasaw\.sip\.common\.utils\.SipUtils\.parseSdp'

hits=$(grep -rEn "$PATTERN" \
       gb28181-client/src/main/java \
       gb28181-server/src/main/java \
       --include="*.java" 2>/dev/null \
       | grep -vE '^\s*//|^\s*\*' || true)

if [ -n "$hits" ]; then
  echo "❌ 业务模块禁止直接依赖 Rfc4566SdpParser / SipUtils.parseSdp，请通过 Gb28181SdpParser 解析："
  echo "$hits"
  exit 1
fi
echo "✅ 业务层未越级依赖 base parser"
