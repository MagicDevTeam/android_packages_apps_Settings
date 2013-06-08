LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_JAVA_LIBRARIES := bouncycastle telephony-common telephony-msim
LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4 android-support-v13 jsr305 libGoogleAnalyticsV2

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_SRC_FILES += $(call all-java-files-under, ../PerformanceControl/src)

LOCAL_PACKAGE_NAME := Settings
LOCAL_CERTIFICATE := platform

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

LOCAL_AAPT_INCLUDE_ALL_RESOURCES := true
LOCAL_AAPT_FLAGS := --extra-packages com.koushikdutta.superuser:com.koushikdutta.widgets \
                    --extra-packages com.brewcrewfoo.performance \
                    --auto-add-overlay

LOCAL_SRC_FILES += $(call all-java-files-under,../../../external/koush/Superuser/Superuser/src) $(call all-java-files-under,../../../external/koush/Widgets/Widgets/src)
LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res 
LOCAL_RESOURCE_DIR += $(LOCAL_PATH)/../../../external/koush/Widgets/Widgets/res 
LOCAL_RESOURCE_DIR += $(LOCAL_PATH)/../../../external/koush/Superuser/Superuser/res
LOCAL_RESOURCE_DIR += $(LOCAL_PATH)/../PerformanceControl/res

include $(BUILD_PACKAGE)

# Use the folloing include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))
