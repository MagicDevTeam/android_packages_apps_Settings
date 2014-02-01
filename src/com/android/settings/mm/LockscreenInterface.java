/*
 * Copyright (C) 2014 The MagicMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.mm;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.internal.util.mm.DeviceUtils;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.ChooseLockSettingsHelper;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.R;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

import java.io.File;

public class LockscreenInterface extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "LockscreenInterface";

    private static final String KEY_ENABLE_WIDGETS = "keyguard_enable_widgets";
    private static final String KEY_QUICK_UNLOCK_CONTROL = "quick_unlock_control";
    private static final String KEY_LOCKSCREEN_CAMERA_WIDGET = "lockscreen_camera_widget";
    private static final String KEY_LOCKSCREEN_MAXIMIZE_WIDGETS = "lockscreen_maximize_widgets";
    private static final String KEY_LOCKSCREEN_DISABLE_HINTS = "lockscreen_disable_hints";
    private static final String KEY_LOCKSCREEN_USE_CAROUSEL = "lockscreen_use_widget_container_carousel";
    private static final String KEY_LOCK_BEFORE_UNLOCK = "lock_before_unlock";
    private static final String KEY_LOCKSCREEN_COLORIZE_ICON = "lockscreen_colorize_icon";
    private static final String KEY_LOCKSCREEN_LOCK_ICON = "lockscreen_lock_icon";
    private static final String KEY_LOCKSCREEN_FRAME_COLOR = "lockscreen_frame_color";
    private static final String KEY_LOCKSCREEN_LOCK_COLOR = "lockscreen_lock_color";
    private static final String KEY_LOCKSCREEN_DOTS_COLOR = "lockscreen_dots_color";

    private CheckBoxPreference mEnableKeyguardWidgets;
    private CheckBoxPreference mQuickUnlockScreen;
    private CheckBoxPreference mCameraWidget;
    private CheckBoxPreference mMaximizeWidgets;
    private CheckBoxPreference mLockscreenHints;
    private CheckBoxPreference mLockscreenUseCarousel;
    private CheckBoxPreference mLockBeforeUnlock;
    private CheckBoxPreference mColorizeCustom;
    private ColorPickerPreference mFrameColor;
    private ColorPickerPreference mLockColor;
    private ColorPickerPreference mDotsColor;
    private ListPreference mLockIcon;
    
    private ChooseLockSettingsHelper mChooseLockSettingsHelper;
    private LockPatternUtils mLockUtils;
    private DevicePolicyManager mDPM;
    private Resources mKeyguardResources;
    private File mLockImage;

    private String mDefault;

    private static final int MENU_RESET = Menu.FIRST;
    private static final int DLG_RESET = 0;
    private static final int REQUEST_PICK_LOCK_ICON = 100;

    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.mm_lockscreen_interface_settings);
        PreferenceScreen prefSet = getPreferenceScreen();

        mChooseLockSettingsHelper = new ChooseLockSettingsHelper(getActivity());
        mLockUtils = mChooseLockSettingsHelper.utils();
        mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        mKeyguardResources = null;
        PackageManager pm = this.getPackageManager();
        try {
            mKeyguardResources = pm.getResourcesForApplication("com.android.keyguard");
        } catch (Exception e) {
            e.printStackTrace();
        }

        mDefault = getResources().getString(R.string.default_string);
        mLockImage = new File(getActivity().getFilesDir() + "/lock_icon.tmp");

        // Find preferences
        mEnableKeyguardWidgets = (CheckBoxPreference) findPreference(KEY_ENABLE_WIDGETS);
        mQuickUnlockScreen = (CheckBoxPreference) findPreference(KEY_QUICK_UNLOCK_CONTROL);
        mCameraWidget = (CheckBoxPreference) findPreference(KEY_LOCKSCREEN_CAMERA_WIDGET);
        mMaximizeWidgets = (CheckBoxPreference) findPreference(KEY_LOCKSCREEN_MAXIMIZE_WIDGETS);
        mLockscreenHints = (CheckBoxPreference) findPreference(KEY_LOCKSCREEN_DISABLE_HINTS);
        mLockscreenUseCarousel = (CheckBoxPreference) findPreference(KEY_LOCKSCREEN_USE_CAROUSEL);
        mLockBeforeUnlock = (CheckBoxPreference) findPreference(KEY_LOCK_BEFORE_UNLOCK);
        mLockIcon = (ListPreference) findPreference(KEY_LOCKSCREEN_LOCK_ICON);
        mLockIcon.setOnPreferenceChangeListener(this);
        mColorizeCustom = (CheckBoxPreference) findPreference(KEY_LOCKSCREEN_COLORIZE_ICON);
        mFrameColor = (ColorPickerPreference) findPreference(KEY_LOCKSCREEN_FRAME_COLOR);
        mFrameColor.setOnPreferenceChangeListener(this);
        mLockColor = (ColorPickerPreference) findPreference(KEY_LOCKSCREEN_LOCK_COLOR);
        mLockColor.setOnPreferenceChangeListener(this);
        mDotsColor = (ColorPickerPreference) findPreference(KEY_LOCKSCREEN_DOTS_COLOR);
        mDotsColor.setOnPreferenceChangeListener(this);

        // Remove/disable custom widgets based on device RAM and policy
        if (ActivityManager.isLowRamDeviceStatic()) {
            // Widgets take a lot of RAM, so disable them on low-memory devices
            prefSet.removePreference(findPreference(KEY_ENABLE_WIDGETS));
            mEnableKeyguardWidgets = null;
        } else {
            checkDisabledByPolicy(mEnableKeyguardWidgets,
                    DevicePolicyManager.KEYGUARD_DISABLE_WIDGETS_ALL);
        }

        if (mDPM.getCameraDisabled(null)
                || (mDPM.getKeyguardDisabledFeatures(null) & DevicePolicyManager.KEYGUARD_DISABLE_SECURE_CAMERA) != 0) {
            prefSet.removePreference(mCameraWidget);
            mCameraWidget = null;
        }
        if (!DeviceUtils.isPhone(getActivity())) {
            prefSet.removePreference(mMaximizeWidgets);
            mMaximizeWidgets = null;
        }
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePreferencesState();
        updateLockSummary();
    }

    private void updatePreferencesState() {
        boolean dotsDisabled = new LockPatternUtils(getActivity()).isSecure()
                && Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCK_BEFORE_UNLOCK,
                        0) == 0;
        boolean imageExists = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.LOCKSCREEN_LOCK_ICON) != null;

        if (mEnableKeyguardWidgets != null) {
            boolean enable = mLockUtils.getWidgetsEnabled();
            if (enable) {
                mEnableKeyguardWidgets.setSummary(R.string.disabled);
            } else {
                mEnableKeyguardWidgets.setSummary(R.string.enabled);
            }
            mEnableKeyguardWidgets.setChecked(enable);
        }
        if (mQuickUnlockScreen != null) {
            mQuickUnlockScreen.setChecked(Settings.System.getInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_QUICK_UNLOCK_CONTROL, 0) == 1);
        }
        if (mCameraWidget != null) {
            final boolean cameraDefault = mKeyguardResources != null ? mKeyguardResources
                    .getBoolean(mKeyguardResources
                            .getIdentifier(
                                    "com.android.keyguard:bool/kg_enable_camera_default_widget",
                                    null, null)) : false;

            mCameraWidget.setChecked(Settings.System.getInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_CAMERA_WIDGET, cameraDefault ? 1 : 0) == 1);
        }
        if (mMaximizeWidgets != null) {
            mMaximizeWidgets.setChecked(Settings.System.getInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_MAXIMIZE_WIDGETS, 0) == 1);
        }
        if (mLockscreenHints != null) {
            mLockscreenHints.setChecked(Settings.System.getInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_DISABLE_HINTS, 1) == 1);
        }
        if (mLockscreenUseCarousel != null) {
            mLockscreenUseCarousel.setChecked(Settings.System.getInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_USE_WIDGET_CONTAINER_CAROUSEL, 0) == 1);
        }
        if (mLockBeforeUnlock != null) {
            mLockBeforeUnlock.setChecked(Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.LOCK_BEFORE_UNLOCK, 0) == 1);
        }
        if (mColorizeCustom != null) {
            mColorizeCustom.setChecked(Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.LOCKSCREEN_COLORIZE_LOCK, 0) == 1);
            mColorizeCustom.setEnabled(!dotsDisabled && imageExists);
        }
        if (mFrameColor != null) {
            int frameColor = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.LOCKSCREEN_FRAME_COLOR, -2);
            setPreferenceSummary(mFrameColor,
                    getResources().getString(R.string.lockscreen_frame_color_summary), frameColor);
            mFrameColor.setNewPreviewColor(frameColor);
        }
        if (mLockColor != null) {
            int lockColor = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.LOCKSCREEN_LOCK_COLOR, -2);
            setPreferenceSummary(mLockColor,
                    getResources().getString(R.string.lockscreen_lock_color_summary), lockColor);
            mLockColor.setNewPreviewColor(lockColor);
            // Tablets don't have the extended-widget lock icon
            if (DeviceUtils.isTablet(getActivity())) {
                mLockColor.setEnabled(!dotsDisabled);
            }
        }
        if (mDotsColor != null) {
            int dotsColor = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.LOCKSCREEN_DOTS_COLOR, -2);
            setPreferenceSummary(mDotsColor,
                    getResources().getString(R.string.lockscreen_dots_color_summary), dotsColor);
            mDotsColor.setNewPreviewColor(dotsColor);
            mDotsColor.setEnabled(!dotsDisabled);
        }
        if (mLockIcon != null) {
            mLockIcon.setEnabled(!dotsDisabled);
        }
    }

    private void updateLockSummary() {
        int resId;
        String value = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.LOCKSCREEN_LOCK_ICON);
        if (value == null) {
            resId = R.string.lockscreen_lock_icon_default;
            mLockIcon.setValueIndex(1);
        } else {
            resId = R.string.lockscreen_lock_icon_custom;
            mLockIcon.setValueIndex(0);
        }
        mLockIcon.setSummary(getResources().getString(resId));
    }

    private void setPreferenceSummary(Preference preference, String defaultSummary, int value) {
        if (value == -2) {
            preference.setSummary(defaultSummary + " (" + mDefault + ")");
        } else {
            String hexColor = String.format("#%08x", (0xffffffff & value));
            preference.setSummary(defaultSummary + " (" + hexColor + ")");
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_PICK_LOCK_ICON) {

                if (mLockImage.length() == 0 || !mLockImage.exists()) {
                    Toast.makeText(getActivity(),
                            getResources().getString(R.string.shortcut_image_not_valid),
                            Toast.LENGTH_LONG).show();
                    return;
                }

                File image = new File(getActivity().getFilesDir() + File.separator + "lock_icon"
                        + System.currentTimeMillis() + ".png");
                String path = image.getAbsolutePath();
                mLockImage.renameTo(image);
                image.setReadable(true, false);

                Settings.Secure.putString(getContentResolver(),
                        Settings.Secure.LOCKSCREEN_LOCK_ICON, path);

                mColorizeCustom.setEnabled(path != null);
            }
        } else {
            if (mLockImage.exists()) {
                mLockImage.delete();
            }
        }
        updateLockSummary();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.reset).setIcon(R.drawable.ic_settings_backup) // use the backup icon
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                showDialogInner(DLG_RESET);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        final String key = preference.getKey();

        if (KEY_ENABLE_WIDGETS.equals(key)) {
            final boolean enable = mEnableKeyguardWidgets.isChecked();
            if (enable) {
                mEnableKeyguardWidgets.setSummary(R.string.enabled);
            } else {
                mEnableKeyguardWidgets.setSummary(R.string.disabled);
            }
            mLockUtils.setWidgetsEnabled(enable);
            return true;
        } else if (KEY_QUICK_UNLOCK_CONTROL.equals(key)) {
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.LOCKSCREEN_QUICK_UNLOCK_CONTROL,
                    mQuickUnlockScreen.isChecked() ? 1 : 0);
            return true;
        } else if (KEY_LOCKSCREEN_CAMERA_WIDGET.equals(key)) {
            Settings.System.putInt(getContentResolver(), Settings.System.LOCKSCREEN_CAMERA_WIDGET,
                    mCameraWidget.isChecked() ? 1 : 0);
            return true;
        } else if (KEY_LOCKSCREEN_MAXIMIZE_WIDGETS.equals(key)) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_MAXIMIZE_WIDGETS, mMaximizeWidgets.isChecked() ? 1
                            : 0);
            return true;
        } else if (KEY_LOCKSCREEN_DISABLE_HINTS.equals(key)) {
            Settings.System.putInt(getContentResolver(), Settings.System.LOCKSCREEN_DISABLE_HINTS,
                    mLockscreenHints.isChecked() ? 1 : 0);
            return true;
        } else if (KEY_LOCKSCREEN_USE_CAROUSEL.equals(key)) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_USE_WIDGET_CONTAINER_CAROUSEL,
                    mLockscreenUseCarousel.isChecked() ? 1 : 0);
            return true;
        } else if (KEY_LOCK_BEFORE_UNLOCK.equals(key)) {
            Settings.Secure.putInt(getContentResolver(), Settings.Secure.LOCK_BEFORE_UNLOCK,
                    mLockBeforeUnlock.isChecked() ? 1 : 0);
            return true;
        } else if (KEY_LOCKSCREEN_COLORIZE_ICON.equals(key)) {
            Settings.Secure.putInt(getContentResolver(), Settings.Secure.LOCKSCREEN_COLORIZE_LOCK,
                    mColorizeCustom.isChecked() ? 1 : 0);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mLockIcon) {
            int indexOf = mLockIcon.findIndexOfValue(newValue.toString());
            if (indexOf == 0) {
                requestLockImage();
            } else {
                deleteLockIcon();
            }
            return true;
        } else if (preference == mFrameColor) {
            int val = Integer.valueOf(String.valueOf(newValue));
            Settings.Secure.putInt(getContentResolver(), Settings.Secure.LOCKSCREEN_FRAME_COLOR,
                    val);
            setPreferenceSummary(preference,
                    getResources().getString(R.string.lockscreen_frame_color_summary), val);
            return true;
        } else if (preference == mLockColor) {
            int val = Integer.valueOf(String.valueOf(newValue));
            Settings.Secure
                    .putInt(getContentResolver(), Settings.Secure.LOCKSCREEN_LOCK_COLOR, val);
            setPreferenceSummary(preference,
                    getResources().getString(R.string.lockscreen_lock_color_summary), val);
            return true;
        } else if (preference == mDotsColor) {
            int val = Integer.valueOf(String.valueOf(newValue));
            Settings.Secure
                    .putInt(getContentResolver(), Settings.Secure.LOCKSCREEN_DOTS_COLOR, val);
            setPreferenceSummary(preference,
                    getResources().getString(R.string.lockscreen_dots_color_summary), val);
            return true;
        }

        return false;
    }

      /**
     * Checks if a specific policy is disabled by a device administrator, and disables the
     * provided preference if so.
     * @param preference Preference
     * @param feature Feature
     */
    private void checkDisabledByPolicy(Preference preference, int feature) {
        boolean disabled = featureIsDisabled(feature);

        if (disabled) {
            preference.setSummary(R.string.security_enable_widgets_disabled_summary);
        }

        preference.setEnabled(!disabled);
    }

    /**
     * Checks if a specific policy is disabled by a device administrator.
     * @param feature Feature
     * @return Is disabled
     */
    private boolean featureIsDisabled(int feature) {
        return (mDPM.getKeyguardDisabledFeatures(null) & feature) != 0;
    }

    private void requestLockImage() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 144, getResources()
                .getDisplayMetrics());

        intent.setType("image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", px);
        intent.putExtra("aspectY", px);
        intent.putExtra("outputX", px);
        intent.putExtra("outputY", px);
        intent.putExtra("scale", true);
        intent.putExtra("scaleUpIfNeeded", false);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.PNG.toString());

        try {
            mLockImage.createNewFile();
            mLockImage.setWritable(true, false);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mLockImage));
            startActivityForResult(intent, REQUEST_PICK_LOCK_ICON);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private void deleteLockIcon() {
        String path = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.LOCKSCREEN_LOCK_ICON);

        if (path != null) {
            File f = new File(path);

            if (f != null && f.exists()) {
                f.delete();
            }
        }

        Settings.Secure.putString(getContentResolver(), Settings.Secure.LOCKSCREEN_LOCK_ICON, null);

        mColorizeCustom.setEnabled(false);
        updateLockSummary();
    }

    private void showDialogInner(int id) {
        DialogFragment newFragment = MyAlertDialogFragment.newInstance(id);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), "dialog " + id);
    }

    public static class MyAlertDialogFragment extends DialogFragment {

        public static MyAlertDialogFragment newInstance(int id) {
            MyAlertDialogFragment frag = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt("id", id);
            frag.setArguments(args);
            return frag;
        }

        LockscreenInterface getOwner() {
            return (LockscreenInterface) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            switch (id) {
                case DLG_RESET:
                    return new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.reset)
                            .setMessage(R.string.lockscreen_style_reset_message)
                            .setNegativeButton(R.string.cancel, null)
                            .setPositiveButton(R.string.dlg_ok,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            Settings.Secure.putInt(getActivity()
                                                    .getContentResolver(),
                                                    Settings.Secure.LOCKSCREEN_FRAME_COLOR, -2);
                                            Settings.Secure.putInt(getActivity()
                                                    .getContentResolver(),
                                                    Settings.Secure.LOCKSCREEN_LOCK_COLOR, -2);
                                            Settings.Secure.putInt(getActivity()
                                                    .getContentResolver(),
                                                    Settings.Secure.LOCKSCREEN_DOTS_COLOR, -2);
                                            getOwner().updatePreferencesState();
                                            getOwner().updateLockSummary();
                                        }
                                    }).create();
            }
            throw new IllegalArgumentException("unknown id " + id);
        }

        @Override
        public void onCancel(DialogInterface dialog) {

        }
    }
}
