# tissot SUSFS hide module

A KernelSU/Magisk-format flashable module that configures SUSFS root-hiding on
tissot (KernelSU-Next, susfs **v2.1.0**). Flash it once via the KernelSU-Next
manager (Modules → Install from storage) after each clean flash / data wipe.

## What it does (at flash time, and every boot after)
- Installs the susfs v2.1.0 CLI to `/data/adb/ksu/bin/ksu_susfs`.
- Seeds `00-tissot-susfs-hide.sh` into `/data/adb/post-fs-data.d/` — KernelSU-Next
  runs this on every boot (reliable on this non-GKI 4.9 build, where module
  `post-fs-data.sh` may not run due to the 5.2+ mount syscalls returning ENOSYS).
- The script: hides `/data/adb*` from non-root apps (`add_sus_path`) and enables
  AVC-log spoofing. It does **not** spoof uname — the kernel build already cleans
  uname/`/proc/version` natively (`.scmversion` + `KBUILD_BUILD_*` in the kernel
  Makefile), so they look like a stock release with no cross-check mismatch.

## Build / rebuild the zip
The prebuilt `tissot-susfs-hide.zip` is committed here. To rebuild after editing
the script:

    cd device/xiaomi/tissot/susfs-hide-module
    rm -f tissot-susfs-hide.zip
    zip -r9 tissot-susfs-hide.zip . -x ".*" -x "tissot-susfs-hide.zip" -x "README.md"

## Notes
- The `ksu_susfs_arm64` binary MUST match the kernel susfs version (v2.1.0). If
  the kernel susfs version changes, replace it from the matching susfs4ksu
  `ksu_module_susfs/tools/`.
- sus_path only hides from app processes (uid>=10000) that are NOT granted root.
  Test with a root-detector app that you have NOT given root to — not adb shell.
- To customise hiding, edit `00-tissot-susfs-hide.sh`, rebuild the zip, reflash.
  Full CLI help: `su -c /data/adb/ksu/bin/ksu_susfs` (no args).
