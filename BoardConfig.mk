#
# Copyright (C) 2017-2021 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0
#

TARGET_KERNEL_VERSION := 4.9

# Inherit from common msm8953-common
include device/xiaomi/msm8953-common/BoardConfigCommon.mk

DEVICE_PATH := device/xiaomi/tissot

# Camera
TARGET_SUPPORT_HAL1 := false

# Display
TARGET_SCREEN_DENSITY := 440

# Filesystem
BOARD_BUILD_SYSTEM_ROOT_IMAGE := true
BOARD_USES_RECOVERY_AS_BOOT := true
TARGET_NO_RECOVERY := true

# HIDL
DEVICE_MANIFEST_FILE += $(DEVICE_PATH)/manifest.xml

# Kernel
TARGET_KERNEL_CONFIG := tissot_defconfig

# Partitions
BOARD_USERDATAIMAGE_PARTITION_SIZE := 55087422464 # 25765059584 - 16384

# Power
TARGET_TAP_TO_WAKE_NODE := "/proc/touchpanel/enable_dt2w"

# RIL
ENABLE_VENDOR_RIL_SERVICE := true

# Security Patch Level
# Align the vendor SPL with the system SPL (ro.build.version.security_patch,
# set by LineageOS to a recent date). The stock value (2020-05-05, from the
# 2020-era blobs) drifts ~6 years from the system SPL and trips coherence
# detectors ("cross-source drift" between ro.vendor.build.security_patch and
# ro.build.version.security_patch). Cosmetic only — doesn't change the blobs.
VENDOR_SECURITY_PATCH := 2026-02-01

# Allow duplicate sysprop assignments (user build only surfaces this).
# AOSP core (build/make/core/main.mk) appends `ro.adb.secure=1` for the `user`
# variant, while LineageOS WITH_ADB_INSECURE appends `ro.adb.secure=0` afterwards
# to keep adb-on-by-default. With dups allowed, the LAST assignment wins (=0),
# so adb stays enabled; without this flag post_process_props aborts the user
# build on the conflicting pair.
BUILD_BROKEN_DUP_SYSPROP := true

# Emit ro.build.tags=release-keys (instead of dev-keys). This build is inline
# release-signed with our own private keys (PRODUCT_DEFAULT_DEV_CERTIFICATE ->
# vendor/lineage-priv/keys/releasekey), so the release-keys tag is accurate and
# makes ro.build.tags COHERENT with the release-keys fingerprint. Consumed by
# build/make/core/sysprop.mk (patched). See note there about repo sync.
TARGET_USES_RELEASE_KEYS_TAG := true

# Inherit the proprietary files
include vendor/xiaomi/tissot/BoardConfigVendor.mk
