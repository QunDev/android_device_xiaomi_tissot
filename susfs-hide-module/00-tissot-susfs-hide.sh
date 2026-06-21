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

# Hide the LSPosed zygisk agent .so from /proc/<pid>/[maps|smaps|mem|...] so apps
# scanning their own memory map don't see a /data/adb/modules/* library. Requires
# CONFIG_KSU_SUSFS_SUS_MAP=y + the task_mmu.c maps/smaps skip (tissot kernel patch).
# ReZygisk's own libzygisk.so is already memfd-loaded (no file path in maps).
for so in /data/adb/modules/zygisk_lsposed/zygisk/arm64-v8a.so \
          /data/adb/modules/zygisk_lsposed/zygisk/armeabi-v7a.so; do
    [ -e "$so" ] && $SUSFS add_sus_map "$so"
done

# Block the SELinux policy ORACLE. app_zygote ships with selinuxfs:file
# read/write/open, which lets a detector spawn an app-zygote and probe whether
# custom types exist (e.g. lsposed_file/zygisk_file/ksu_file) via writes to
# /sys/fs/selinux/{context,access}. untrusted_app/isolated_app/priv_app already
# lack this (stock neverallow); gmscore_app has read-only (can't probe).
# NOTE: webview_zygote is intentionally NOT denied — denying it breaks the
# WebView renderer used by the Google account sign-in flow ("checking info"
# then fail). The detector uses app_zygote, so denying app_zygote alone blocks
# the oracle while leaving Google login working. getattr kept; gmscore_app
# untouched (GMS / Play Integrity keep working). Hides lsposed_file/zygisk_file
# WITHOUT removing them (which breaks LSPosed/ReZygisk — they need their types).
KSUD=/data/adb/ksud
if [ -x "$KSUD" ]; then
    "$KSUD" sepolicy patch "deny app_zygote selinuxfs file { read write open append ioctl lock map watch watch_reads }" 2>/dev/null
fi

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
