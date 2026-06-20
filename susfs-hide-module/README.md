# tissot SUSFS hide module

A KernelSU flashable module that configures SUSFS root-hiding.
Flash once via KSU Manager → Modules → Install from storage.

## What it does
- Installs susfs v2.1.0 CLI + resetprop to /data/adb/ksu/bin/
- Hides /data/adb/* from non-root apps (add_sus_path)
- Spoofs AVC denial logs (enable_avc_log_spoofing)
- Hides /sys/fs/selinux stat info (add_sus_kstat + update_sus_kstat)
- Spoofs boot-state props (ro.boot.verifiedbootstate=green, etc.)
- Deletes custom ROM version props (ro.modversion, ro.lineage.*)
- Auto-runs every boot via post-fs-data.d

## Rebuild
    cd device/xiaomi/tissot/susfs-hide-module
    rm -f tissot-susfs-hide.zip
    zip -r9 tissot-susfs-hide.zip . -x ".*" -x "tissot-susfs-hide.zip" -x "README.md"

## Notes
- sus_path/kstat only hide from app processes (uid>=10000, no root).
  Test with a root-detector app NOT granted root — not adb shell.
- ksu_susfs_arm64 must match kernel susfs version (v2.1.0).
