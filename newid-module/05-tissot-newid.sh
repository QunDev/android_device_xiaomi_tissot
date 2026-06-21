#!/system/bin/sh
# tissot New Google Device ID — boot re-apply (post-fs-data).
# resetprop does not persist ro.* across reboot, so re-apply the spoofed serial
# (and the optional device profile) every boot. Seeded into post-fs-data.d by
# customize.sh because a module's own post-fs-data.sh is unreliable on this
# non-GKI 4.9 build.
RP=/data/adb/ksu/bin/resetprop
CFG=/data/adb/tissot_newid
[ -x "$RP" ] || exit 0

# 1) re-apply the random serial chosen by the Action
if [ -f "$CFG/serial" ]; then
    v=$(cat "$CFG/serial")
    [ -n "$v" ] && { "$RP" -n ro.serialno "$v"; "$RP" -n ro.boot.serialno "$v"; }
fi

# 2) OPTIONAL device profile: "prop=value" lines in $CFG/profile.prop to make the
#    device look like a specific (Google) model. Leave the file ABSENT to NOT
#    touch build props (recommended/safe). Keep values consistent with PIF.
#    Example profile.prop:
#      ro.product.model=Pixel 8
#      ro.product.manufacturer=Google
#      ro.product.brand=google
if [ -f "$CFG/profile.prop" ]; then
    while IFS='=' read -r k v; do
        case "$k" in ''|\#*) continue;; esac
        [ -n "$k" ] && "$RP" -n "$k" "$v"
    done < "$CFG/profile.prop"
fi
