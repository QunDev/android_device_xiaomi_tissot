#!/system/bin/sh
# tissot SUSFS hiding — runs at post-fs-data (before zygote) via KernelSU-Next.
# susfs v2.1.0 CLI. uname/proc-version are cleaned natively in the kernel build
# (.scmversion + KBUILD_BUILD_*), so NO set_uname here.
SUSFS=/data/adb/ksu/bin/ksu_susfs
[ -x "$SUSFS" ] || exit 0

$SUSFS enable_log 0

for p in /data/adb/ksu /data/adb/ksud /data/adb/modules /data/adb; do
    [ -e "$p" ] && $SUSFS add_sus_path "$p"
done

$SUSFS enable_avc_log_spoofing 1

# Hide /sys/fs/selinux stat info from non-root apps (uid>=10000).
# add_sus_kstat + update_sus_kstat only spoofs stat() — safe for boot.
if [ -e /sys/fs/selinux ]; then
    $SUSFS add_sus_kstat /sys/fs/selinux
    $SUSFS update_sus_kstat /sys/fs/selinux
fi

RP=/data/adb/ksu/bin/resetprop
if [ -x "$RP" ]; then
    "$RP" -n ro.boot.verifiedbootstate green
    "$RP" -n ro.boot.flash.locked 1
    "$RP" -n ro.boot.vbmeta.device_state locked
    "$RP" -n ro.boot.veritymode enforcing
    "$RP" -n ro.build.tags release-keys
    for p in ro.modversion ro.lineage.version ro.lineage.releasetype \
             ro.lineage.build.version ro.lineage.display.version; do
        "$RP" --delete "$p"
    done
fi
