# tissot New Google Device ID — install (KernelSU-Next / Magisk).
SKIPUNZIP=0
PFDD=/data/adb/post-fs-data.d
CFG=/data/adb/tissot_newid

ui_print "- tissot New Google Device ID v1.0.0"

if [ "$ARCH" != "arm64" ] && [ "$ARCH" != "arm" ]; then
  ui_print "! Only arm/arm64 supported"; abort
fi

# need resetprop (installed by tissot_susfs_hide). Warn if missing.
if [ ! -x /data/adb/ksu/bin/resetprop ]; then
  ui_print "! /data/adb/ksu/bin/resetprop not found"
  ui_print "! Install the tissot_susfs_hide module first (it ships resetprop)."
fi

mkdir -p "$CFG"

# seed the boot re-apply script into post-fs-data.d (reliable on this build)
ui_print "- Seeding boot script -> $PFDD/05-tissot-newid.sh"
mkdir -p "$PFDD"
cp "$MODPATH/05-tissot-newid.sh" "$PFDD/05-tissot-newid.sh"
chmod 755 "$PFDD/05-tissot-newid.sh"

chmod 755 "$MODPATH/action.sh"

ui_print "- Installed."
ui_print "- HOW TO USE: open KernelSU-Next manager -> Modules ->"
ui_print "  'tissot New Google Device ID' -> tap Action."
ui_print "  It signs you out of Google + resets the device identity."
ui_print "  Then REBOOT and sign in -> Google sees a NEW device."

# we don't need the seeded copy duplicated in the module payload at runtime
rm -f "$MODPATH/05-tissot-newid.sh"
