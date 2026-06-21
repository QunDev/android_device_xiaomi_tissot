# tissot New Google Device ID

On-demand tool to make **Google register this phone as a brand-new device**
(for privacy / fresh device separation). For your own device.

## Why model/fingerprint alone is not enough
Google identifies a device mainly by its **GSF ID** (Google Services Framework
ID) + the **checkin** identity — not by `Build.MODEL`. Changing the model while
keeping the same GSF ID, Google still recognises the same device. So this tool
resets the **identity**, not just the props.

## What the Action does
KSU-Next manager → Modules → *tissot New Google Device ID* → **Action**:
1. Stops GMS / GSF / Play Store.
2. `pm clear` GSF + GMS + Play Store → new GSF ID + Android device id on next
   checkin (this **signs you out of Google**).
3. Resets the Android ID (SSAID) store.
4. Generates a new random **serial** (`ro.serialno`), persisted in
   `/data/adb/tissot_newid/serial` and re-applied every boot.
5. Runs **autopif** (if PlayIntegrityFix autopif variant is installed) to pull a
   fresh **Pixel fingerprint from Google**.

Then **reboot and sign in** → Google sees a new device.

## Device profile = AUTO-SYNC with PlayIntegrityFix (default)
Every boot the script reads PIF's `pif.prop` and sets the **global** device
identity (brand / manufacturer / model / device / name + all partition variants
+ all `*.build.fingerprint`) to **match whatever PIF feeds DroidGuard**. So if
autopif changes the spoofed Pixel, the global props follow it automatically —
no more global-vs-PlayIntegrity mismatch. The fingerprint is rebuilt with the
**real ROM version (Android 13 / build TQ3A.230901.001)** so apps don't see a
version mismatch; only brand/device/model track PIF. Also overrides
`ro.product.*.name=lineage_tissot` (hides the LineageOS tell).

Left untouched on purpose: `ro.build.version.*` (release 13 / sdk 33 = real ROM)
and `ro.hardware`/`ro.board.platform` (real SoC msm8953 — changing risks HALs).

### Pin a fixed device instead (manual override)
If you want a FIXED device regardless of PIF, create
`/data/adb/tissot_newid/profile.prop` (`prop=value` lines — see
`profile.prop.example`). When present it **overrides** auto-sync. Keep all values
consistent (fingerprint = `brand/product/device:release/id/incremental:type/tags`).

### Caveats
- autopif may pick a Pixel that never shipped Android 13 (e.g. Pixel 8) → a deep
  check could spot "that model never ran A13". Prefer constraining autopif to
  A13-era Pixels, or pin via profile.prop.
- `ro.hardware=msm8953` vs a Pixel device is a deep-cross-check mismatch.

## Requirements / notes
- Needs `resetprop` from the **tissot_susfs_hide** module (ships it). Install
  that first.
- Pairs with **PlayIntegrityFix (autopif)** + **TrickyStore**. The Action does
  NOT inject into GMS, so it does not break Play Integrity / DroidGuard.
- **Limitation:** TrickyStore keeps the **same keybox**, so the hardware
  key-attestation identity is identical across "devices" — Google *may* still
  correlate them via the attestation key. For full separation you need a
  different keybox per identity.
- Does NOT change `ro.product.*.name` (=`lineage_tissot`) by default; the
  tissot_susfs_hide module hides the other LineageOS tells.
