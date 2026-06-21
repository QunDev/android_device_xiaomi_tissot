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

## Optional: appear as a specific model
Create `/data/adb/tissot_newid/profile.prop` with `prop=value` lines, e.g.:

```
ro.product.model=Pixel 8
ro.product.manufacturer=Google
ro.product.brand=google
```

The boot script re-applies these every boot. Keep them **consistent with the
fingerprint PIF/autopif sets**. Leave the file absent to NOT touch build props
(safe default). Editing `ro.product.*` is the riskier part — test stability.

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
