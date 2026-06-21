#!/system/bin/sh
# ============================================================================
# tissot New Google Device ID — ACTION
# Run from the KernelSU-Next manager: Modules -> "tissot New Google Device ID"
# -> Action button.
#
# Resets this device's Google identity so the NEXT sign-in registers a brand
# new device with Google.
#
# !! WARNING !! This SIGNS YOU OUT of all Google accounts and wipes Play Store /
# GMS / GSF data. You MUST reboot and sign in again afterwards.
# ============================================================================
RP=/data/adb/ksu/bin/resetprop
CFG=/data/adb/tissot_newid
mkdir -p "$CFG"

echo "==========================================="
echo " tissot New Google Device ID"
echo "==========================================="
echo "- Signs you out of Google. Reboot + re-login after."
echo

echo "[1/5] Stopping Google services..."
for p in com.google.android.gms com.google.android.gsf com.android.vending; do
    am force-stop "$p" 2>/dev/null
done

echo "[2/5] Clearing GSF / Play Store / GMS (new GSF + device identity)..."
pm clear com.google.android.gsf  >/dev/null 2>&1 && echo "    gsf cleared"
pm clear com.android.vending     >/dev/null 2>&1 && echo "    vending cleared"
pm clear com.google.android.gms  >/dev/null 2>&1 && echo "    gms cleared (accounts removed)"

echo "[3/5] Resetting Android ID (SSAID) store..."
rm -f /data/system/users/0/settings_ssaid.xml 2>/dev/null && echo "    ssaid reset"

echo "[4/5] Generating a new random serial..."
NEWSER=$(tr -dc 'A-Za-z0-9' </dev/urandom 2>/dev/null | head -c 12)
[ -z "$NEWSER" ] && NEWSER=$(cat /proc/sys/kernel/random/uuid | tr -d '-' | cut -c1-12)
echo "$NEWSER" > "$CFG/serial"
if [ -x "$RP" ]; then
    "$RP" -n ro.serialno      "$NEWSER"
    "$RP" -n ro.boot.serialno "$NEWSER"
fi
echo "    new serial: $NEWSER"

echo "[5/5] Refreshing PIF fingerprint from Google (autopif)..."
AP=/data/adb/modules/playintegrityfix/autopif.sh
if [ -f "$AP" ]; then
    sh "$AP" >/dev/null 2>&1 && echo "    autopif ok" || echo "    autopif failed (run it manually if needed)"
else
    echo "    autopif not found - skip (PlayIntegrityFix autopif variant not installed)"
fi

echo
echo ">>> DONE. Now REBOOT, then sign into Google."
echo ">>> Google will register this as a NEW device."
echo ">>> (If you also want a different MODEL name, edit $CFG/profile.prop"
echo ">>>  - see README - and keep it consistent with PIF.)"
