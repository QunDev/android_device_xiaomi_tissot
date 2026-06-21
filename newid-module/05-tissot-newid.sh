#!/system/bin/sh
# tissot New Google Device ID — boot re-apply (post-fs-data).
# resetprop does not persist ro.* across reboot, so re-apply every boot. Seeded
# into post-fs-data.d by customize.sh (a module's own post-fs-data.sh is
# unreliable on this non-GKI 4.9 build).
RP=/data/adb/ksu/bin/resetprop
CFG=/data/adb/tissot_newid
PIF=/data/adb/modules/playintegrityfix/pif.prop
[ -x "$RP" ] || exit 0

# 1) re-apply the random serial chosen by the Action
if [ -f "$CFG/serial" ]; then
    v=$(cat "$CFG/serial")
    [ -n "$v" ] && { "$RP" -n ro.serialno "$v"; "$RP" -n ro.boot.serialno "$v"; }
fi

# Apply a full device build-identity (model/brand/device/fingerprint + all
# partition variants). Helper:
apply_profile() {  # $1 brand  $2 manufacturer  $3 model  $4 device  $5 fingerprint
    b=$1; m=$2; mo=$3; d=$4; fp=$5
    for part in "" .system .vendor .odm .product .system_ext; do
        "$RP" -n "ro.product${part}.brand"        "$b"
        "$RP" -n "ro.product${part}.manufacturer" "$m"
        "$RP" -n "ro.product${part}.model"        "$mo"
        "$RP" -n "ro.product${part}.device"       "$d"
        "$RP" -n "ro.product${part}.name"         "$d"
    done
    "$RP" -n ro.build.product     "$d"
    "$RP" -n ro.build.flavor      "${d}-user"
    "$RP" -n ro.build.description  "${d}-user 13 TQ3A.230901.001 10750268 release-keys"
    for k in ro.build.fingerprint ro.system.build.fingerprint ro.vendor.build.fingerprint \
             ro.product.build.fingerprint ro.odm.build.fingerprint \
             ro.system_ext.build.fingerprint ro.bootimage.build.fingerprint; do
        "$RP" -n "$k" "$fp"
    done
}

# 2a) MANUAL OVERRIDE: $CFG/profile.prop ("prop=value" lines) wins if present.
if [ -f "$CFG/profile.prop" ]; then
    while IFS='=' read -r k v; do
        case "$k" in ''|\#*) continue;; esac
        [ -n "$k" ] && "$RP" -n "$k" "$v"
    done < "$CFG/profile.prop"

# 2b) AUTO-SYNC (default): derive the global device identity from PIF's pif.prop
#     so global props ALWAYS match what PlayIntegrityFix feeds DroidGuard, even
#     when autopif changes the spoofed device. The fingerprint is rebuilt with
#     the REAL ROM version (Android 13 / build TQ3A.230901.001) so apps don't see
#     a version mismatch; only brand/device/model follow PIF.
elif [ -f "$PIF" ]; then
    FP=$(grep -i '^FINGERPRINT=' "$PIF" | head -1 | cut -d= -f2-)
    MODEL=$(grep -i '^MODEL=' "$PIF" | head -1 | cut -d= -f2-)
    MFG=$(grep -i '^MANUFACTURER=' "$PIF" | head -1 | cut -d= -f2-)
    if [ -n "$FP" ]; then
        brand=$(echo "$FP"  | cut -d/ -f1)
        device=$(echo "$FP" | cut -d/ -f3 | cut -d: -f1)
        [ -z "$MODEL" ] && MODEL="$device"
        [ -z "$MFG" ]   && MFG="$brand"
        FPNEW="${brand}/${device}/${device}:13/TQ3A.230901.001/10750268:user/release-keys"
        apply_profile "$brand" "$MFG" "$MODEL" "$device" "$FPNEW"
    fi
fi
