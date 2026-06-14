#!/usr/bin/env bash
#
# apply-release-version.sh <version>
#
# 把指定版本号写回整个 reactor:
#   - 根 version、各模块 parent/自身 version(processAllModules);
#   - 两个版本属性 sip-proxy-common.version / gb28181-proxy.version。
#
# 单点维护"版本写回"这件事,供两处复用:
#   1. resolve-release-version.sh —— 解析出新版本后写回;
#   2. CI 第二个发布 job(GitHub Packages)—— 用第一个 job 解析出的版本
#      对齐自己工作区(不同 job 不共享文件系统,且 checkout 的是触发 SHA,
#      拿不到第一个 job bump 后 push 的 pom)。

set -euo pipefail

target="${1:?usage: apply-release-version.sh <version>}"

mvn -q versions:set -DnewVersion="$target" -DprocessAllModules=true -DgenerateBackupPoms=false
mvn -q versions:set-property -Dproperty=sip-proxy-common.version -DnewVersion="$target" -DgenerateBackupPoms=false
mvn -q versions:set-property -Dproperty=gb28181-proxy.version  -DnewVersion="$target" -DgenerateBackupPoms=false
