#
# Copyright (C) 2017-2021 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0
#

# Enable USB debugging (adb) by default without ADB authentication.
# On non-eng builds this sets ro.adb.secure=0, which makes post_process_props.py
# add "adb" to persist.sys.usb.config so AdbService turns ADB_ENABLED on at every
# boot. Must be defined before vendor/lineage/config/common.mk is inherited below.
WITH_ADB_INSECURE := true

# Inherit from those products. Most specific first.
$(call inherit-product, $(SRC_TARGET_DIR)/product/core_64_bit.mk)
$(call inherit-product, $(SRC_TARGET_DIR)/product/full_base_telephony.mk)

# Inherit some common LineageOS stuff.
$(call inherit-product, vendor/lineage/config/common_full_phone.mk)

# Inherit from tissot device
$(call inherit-product, device/xiaomi/tissot/device.mk)

# MindTheGapps (real Google services: Play Store, Play Services, GSF, sync,
# SetupWizard) embedded at build time. The tree is synced to vendor/gapps via
# .repo/local_manifests/gapps.xml (gitlab MindTheGapps/vendor_gapps, branch tau =
# Android 13). arm64 since tissot inherits core_64_bit. -if-exists so the build
# still works if the gapps tree isn't synced.
$(call inherit-product-if-exists, vendor/gapps/arm64/arm64-vendor.mk)

# Device identifier. This must come after all inclusions
PRODUCT_DEVICE := tissot
PRODUCT_NAME := lineage_tissot
BOARD_VENDOR := Xiaomi
PRODUCT_BRAND := Xiaomi
PRODUCT_MODEL := Mi A1
PRODUCT_MANUFACTURER := Xiaomi
TARGET_VENDOR := Xiaomi

PRODUCT_GMS_CLIENTID_BASE := android-xiaomi

PRODUCT_BUILD_PROP_OVERRIDES += \
    PRIVATE_BUILD_DESC="tissot-user 8.0.0 OPR1.170623.026 8.1.10 release-keys"

# Set BUILD_FINGERPRINT variable to be picked up by both system and vendor build.prop
BUILD_FINGERPRINT := "xiaomi/tissot/tissot_sprout:8.0.0/OPR1.170623.026/8.1.10:user/release-keys"
