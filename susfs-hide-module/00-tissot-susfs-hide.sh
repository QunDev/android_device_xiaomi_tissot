#!/system/bin/sh
# tissot SUSFS hiding — runs at post-fs-data (before zygote) via KernelSU-Next.
# susfs v2.1.0 CLI. uname/proc-version are cleaned natively in the kernel build
# (.scmversion + KBUILD_BUILD_*), so NO set_uname here.
SUSFS=/data/adb/ksu/bin/ksu_susfs
[ -x "$SUSFS" ] || exit 0

# susfs kernel logging off (set 1 only when debugging).
$SUSFS enable_log 0

# 1. Hide KSU/susfs dirs from non-root apps (sus_path).
#    Effective only on app processes uid>=10000 NOT granted root, and only for
#    paths that already exist (susfs needs a realpath).
for p in /data/adb/ksu /data/adb/ksud /data/adb/modules /data/adb; do
    [ -e "$p" ] && $SUSFS add_sus_path "$p"
done

# 2. Spoof SELinux AVC denial logs for the ksu domain (hide from logcat scanners).
$SUSFS enable_avc_log_spoofing 1
