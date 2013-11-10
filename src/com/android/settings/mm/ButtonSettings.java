/*
 * Copyright (C) 2012 MagicMod
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

import android.app.ActivityManagerNative;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;
import android.view.IWindowManager;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class ButtonSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "InterfaceSettings";

    private static final String KEY_NAVIGATION_BAR = "navigation_bar_settings";
    private static final String KEY_HARDWARE_KEYS = "hardware_keys";
    private static final String HARDWARE_KEYS_SHOW_OVERFLOW = "hardware_keys_show_overflow";
    private static final String KEY_WAKEUP_CATEGORY = "category_wakeup_options";
    private static final String KEY_BUTTON_WAKE = "pref_wakeon_button";
    private static final String KEY_VOLUME_WAKE = "pref_volume_wake";

    private PreferenceScreen mPhoneDrawer;
    private PreferenceScreen mTabletDrawer;
    private PreferenceScreen mHardwareKeys;
    private CheckBoxPreference mShowActionOverflow;
    private ListPreference mButtonWake;
    private CheckBoxPreference mVolumeWake;
    private PreferenceCategory mWakeUpOptions;
    private PreferenceScreen mNavBar;

    private final Configuration mCurConfig = new Configuration();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.button_settings);

        mWakeUpOptions = (PreferenceCategory) getPreferenceScreen().findPreference(
                KEY_WAKEUP_CATEGORY);

        mHardwareKeys = (PreferenceScreen) findPreference(KEY_HARDWARE_KEYS);
        mNavBar = (PreferenceScreen)findPreference(KEY_NAVIGATION_BAR);

        try {
            boolean hasNavBar = WindowManagerGlobal.getWindowManagerService().hasNavigationBar();
            if (!hasNavBar){
                getPreferenceScreen().removePreference(findPreference(KEY_NAVIGATION_BAR));
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error getting navigation bar status");
        }
        mShowActionOverflow = (CheckBoxPreference) findPreference(HARDWARE_KEYS_SHOW_OVERFLOW);
        if (mShowActionOverflow != null) {
            mShowActionOverflow.setChecked(Settings.System.getInt(getContentResolver(),
                    Settings.System.UI_FORCE_OVERFLOW_BUTTON, 0) == 1);
        }

        mButtonWake = (ListPreference) findPreference(KEY_BUTTON_WAKE);
        if (mButtonWake != null) {
            if (!getResources().getBoolean(R.bool.config_show_homeWake)) {
                // no home button, don't allow user to disable power button
                // either
                mWakeUpOptions.removePreference(mButtonWake);
            } else {
                int buttonWakeValue = Settings.System.getInt(getContentResolver(),
                        Settings.System.BUTTON_WAKE_SCREEN, 2);
                mButtonWake.setValue(String.valueOf(buttonWakeValue));
                mButtonWake.setSummary(getResources().getString(
                        R.string.pref_wakeon_button_summary, mButtonWake.getEntry()));
                mButtonWake.setOnPreferenceChangeListener(this);
            }
        }

        mVolumeWake = (CheckBoxPreference) findPreference(KEY_VOLUME_WAKE);
        if (mVolumeWake != null) {
            if (!getResources().getBoolean(R.bool.config_show_volumeRockerWake)) {
                mWakeUpOptions.removePreference(mVolumeWake);
            } else {
                mVolumeWake.setChecked(Settings.System.getInt(getContentResolver(),
                        Settings.System.VOLUME_WAKE_SCREEN, 0) == 1);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mShowActionOverflow) {
            boolean enabled = mShowActionOverflow.isChecked();
            Settings.System.putInt(getContentResolver(), Settings.System.UI_FORCE_OVERFLOW_BUTTON,
                    enabled ? 1 : 0);
            // Show appropriate
            if (enabled) {
                Toast.makeText(getActivity(), R.string.hardware_keys_show_overflow_toast_enable,
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getActivity(), R.string.hardware_keys_show_overflow_toast_disable,
                        Toast.LENGTH_LONG).show();
            }
            return true;
        }
        if (preference == mVolumeWake) {
            boolean value = mVolumeWake.isChecked();
            Settings.System.putInt(getContentResolver(), Settings.System.VOLUME_WAKE_SCREEN,
                    value ? 1 : 0);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        if (KEY_BUTTON_WAKE.equals(key)) {
            int buttonWakeValue = Integer.parseInt((String) objValue);
            int index = mButtonWake.findIndexOfValue((String) objValue);
            Settings.System.putInt(getContentResolver(), Settings.System.BUTTON_WAKE_SCREEN,
                    buttonWakeValue);
            mButtonWake.setSummary(getResources().getString(R.string.pref_wakeon_button_summary,
                    mButtonWake.getEntries()[index]));
            return true;
        }
        return true;
    }
}
