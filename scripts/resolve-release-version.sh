#!/usr/bin/env bash
#
# resolve-release-version.sh
#
# 解析"下一个可发布版本",供 CI 发布流程使用:
#   1. 读取聚合根 pom 的当前 <version>;
#   2. 若该版本已存在于 Maven 中央仓库,则 patch 段 +1,循环直到找到一个未被占用的版本;
#   3. 若发生了版本变更,则把新版本写回整个 reactor
#      (根 version、各模块 parent/自身 version,以及两个版本属性
#       sip-proxy-common.version / gb28181-proxy.version)。
#
# 判定"是否已发布"以中央仓库实际产物为准(repo1.maven.org 上的 .pom 是否 200),
# 这是 Sonatype Central 同步后的最终落地位置,也是同版本不可重复发布的事实边界。
#
# 输出:
#   - stderr 打印过程日志;
#   - stdout 打印 "VERSION=<resolved>" 与 "BUMPED=true|false";
#   - 若存在 $GITHUB_OUTPUT,则追加 version=<resolved> / bumped=<bool> 供后续 step 引用。
#
# 注意:中央仓库同步有延迟(发布成功后 repo1 可能数十分钟才可见)。
# 因此"刚发布后立刻再次 push"这一窗口内可能误判为未发布并触发重发,
# 此时 Central Portal 会以 duplicate 拒绝——属可接受的边界情形。

set -euo pipefail

GROUP_PATH="io/github/lunasaw/sip-proxy"
CENTRAL_BASE="https://repo1.maven.org/maven2/${GROUP_PATH}"
HELP_PLUGIN="org.apache.maven.plugins:maven-help-plugin:3.4.0"

log() { echo "$@" >&2; }

current="$(mvn -q -N "${HELP_PLUGIN}:evaluate" -Dexpression=project.version -DforceStdout)"
log "当前 pom 版本: ${current}"

is_published() {
  local v="$1" code
  code="$(curl -s -o /dev/null -w '%{http_code}' "${CENTRAL_BASE}/${v}/sip-proxy-${v}.pom")"
  [ "$code" = "200" ]
}

bump_patch() {
  # 仅对 x.y.z 数值 patch 段 +1;若带 -SNAPSHOT/qualifier 则先剥离再 bump
  local base="${1%%-*}" major minor patch
  IFS='.' read -r major minor patch <<<"$base"
  echo "${major}.${minor}.$((patch + 1))"
}

resolved="$current"
bumped=false
while is_published "$resolved"; do
  log "版本 ${resolved} 已在中央仓库,递增 patch"
  resolved="$(bump_patch "$resolved")"
  bumped=true
done
log "解析结果: ${resolved} (bumped=${bumped})"

if [ "$resolved" != "$current" ]; then
  log "写回版本号到整个 reactor: ${current} -> ${resolved}"
  mvn -q versions:set -DnewVersion="$resolved" -DprocessAllModules=true -DgenerateBackupPoms=false
  mvn -q versions:set-property -Dproperty=sip-proxy-common.version -DnewVersion="$resolved" -DgenerateBackupPoms=false
  mvn -q versions:set-property -Dproperty=gb28181-proxy.version  -DnewVersion="$resolved" -DgenerateBackupPoms=false
fi

echo "VERSION=${resolved}"
echo "BUMPED=${bumped}"
if [ -n "${GITHUB_OUTPUT:-}" ]; then
  {
    echo "version=${resolved}"
    echo "bumped=${bumped}"
  } >> "$GITHUB_OUTPUT"
fi
