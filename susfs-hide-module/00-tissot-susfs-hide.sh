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

# 3. Boot-state prop spoofing (#2: verifiedbootstate=orange -> green).
#    Apps read these via getprop/__system_property_get; `setprop ro.*` is blocked
#    by init, so use resetprop (writes the property area directly). /proc/cmdline
#    is NOT readable by untrusted_app (SELinux proc_cmdline), so no cmdline spoof
#    is needed — the prop is the only app-visible channel.
#    LIMIT: this fools prop reads only. Hardware TEE Key Attestation still reports
#    the true unlocked/unverified state and CANNOT be faked on an unlocked
#    bootloader — if the detector does key attestation, this will not hide it.
RP=/data/adb/ksu/bin/resetprop
if [ -x "$RP" ]; then
    # -n: set the value without firing property-change triggers/init handlers.
    "$RP" -n ro.boot.verifiedbootstate green
    "$RP" -n ro.boot.flash.locked 1
    "$RP" -n ro.boot.vbmeta.device_state locked
    "$RP" -n ro.boot.veritymode enforcing
    # belt-and-suspenders for #3 (build.prop on disk is already release-keys).
    "$RP" -n ro.build.tags release-keys
fi
