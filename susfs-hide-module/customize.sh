# tissot SUSFS hide — KSU/Magisk customize.sh, runs at flash time.
# Installs the susfs v2.1.0 CLI binary and seeds the hide script into
# /data/adb/post-fs-data.d/ (which KernelSU-Next ksud runs on every boot —
# reliable on this non-GKI 4.9 build where module post-fs-data.sh may not run).

SKIPUNZIP=0
DEST_BIN_DIR=/data/adb/ksu/bin
PFDD=/data/adb/post-fs-data.d

ui_print "- tissot SUSFS hide (susfs v2.1.0)"

# arch guard
if [ "$ARCH" != "arm64" ]; then
  ui_print "! Only arm64 is supported"
  abort
fi

# 1. install ksu_susfs CLI binary
if [ ! -d "$DEST_BIN_DIR" ]; then
  ui_print "! $DEST_BIN_DIR missing — is KernelSU installed? aborting"
  abort
fi
ui_print "- Installing ksu_susfs -> $DEST_BIN_DIR/ksu_susfs"
cp "$MODPATH/tools/ksu_susfs_arm64" "$DEST_BIN_DIR/ksu_susfs"
chmod 755 "$DEST_BIN_DIR/ksu_susfs"

# 1b. install resetprop CLI (Magisk applet, runs standalone). Needed because
#     `setprop ro.*` is rejected by init after first set; resetprop writes the
#     property area directly to spoof ro.boot.* boot-state props (#2).
ui_print "- Installing resetprop -> $DEST_BIN_DIR/resetprop"
cp "$MODPATH/tools/resetprop_arm64" "$DEST_BIN_DIR/resetprop"
chmod 755 "$DEST_BIN_DIR/resetprop"

# 2. seed the hide script into post-fs-data.d (runs every boot)
ui_print "- Seeding hide script -> $PFDD/00-tissot-susfs-hide.sh"
mkdir -p "$PFDD"
cp "$MODPATH/00-tissot-susfs-hide.sh" "$PFDD/00-tissot-susfs-hide.sh"
chmod 755 "$PFDD/00-tissot-susfs-hide.sh"

# 3. run it once now so hiding is active without a reboot
ui_print "- Applying hide config now"
sh "$PFDD/00-tissot-susfs-hide.sh"

# clean up the module payload we no longer need on disk
rm -rf "$MODPATH/tools"
rm -f "$MODPATH/customize.sh"

ui_print "- Done. Hiding will auto-apply on every boot."
