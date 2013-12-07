/*
 * Copyright (C) 2012 Slimroms Project
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

import android.R.string;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.filterpacks.text.StringSource;
import android.graphics.Color;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

import java.util.Date;

public class StatusBarSignalStyle extends SettingsPreferenceFragment
    implements OnPreferenceChangeListener {

    private static final String TAG = "SignalStyle";

    private static final String PREF_STATUS_BAR_SIGNAL_STYLE = "signal_style";
    private static final String PREF_STATUS_BAR_SIGNAL_TEXT_COLOR = "signal_color";
    
    private static final int MENU_RESET = Menu.FIRST;

    private ListPreference mSignalStyle;
    private ColorPickerPreference mSignalColor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createCustomView();
    }

    private PreferenceScreen createCustomView() {
        PreferenceScreen prefSet = getPreferenceScreen();
        if (prefSet != null) {
            prefSet.removeAll();
        }

        addPreferencesFromResource(R.xml.status_bar_signal_style);
        prefSet = getPreferenceScreen();

        PackageManager pm = getPackageManager();
        Resources systemUiResources;
        try {
            systemUiResources = pm.getResourcesForApplication("com.android.systemui");
        } catch (Exception e) {
            Log.e(TAG, "can't access systemui resources",e);
            return null;
        }

        mSignalStyle = (ListPreference) prefSet.findPreference(PREF_STATUS_BAR_SIGNAL_STYLE);
        mSignalStyle.setOnPreferenceChangeListener(this);
               
        int signal = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.STATUS_BAR_SIGNAL_TEXT, 0);
        mSignalStyle.setValue(String.valueOf(signal));
        mSignalStyle.setSummary(mSignalStyle.getEntry());

        mSignalColor = (ColorPickerPreference) findPreference(PREF_STATUS_BAR_SIGNAL_TEXT_COLOR);
        mSignalColor.setOnPreferenceChangeListener(this);
        
        int intColor;
        String hexColor;

        intColor = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_SIGNAL_TEXT_COLOR, -2);
        if (intColor == -2) {
            intColor = systemUiResources.getColor(systemUiResources.getIdentifier(
                    "com.android.systemui:color/batterymeter_charge_color", null, null));
            mSignalColor.setSummary(getResources().getString(R.string.default_string));
        } else {
            hexColor = String.format("#%08x", (0xffffffff & intColor));
            mSignalColor.setSummary(hexColor);
        }
        mSignalColor.setNewPreviewColor(intColor);

        updateSignalColorOption(signal);

        setHasOptionsMenu(true);
        return prefSet;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.reset)
                .setIcon(R.drawable.ic_settings_backup) // use the backup icon
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                resetToDefault();
                return true;
             default:
                return super.onContextItemSelected(item);
        }
    }

    private void resetToDefault() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setTitle(R.string.reset);
        alertDialog.setMessage(R.string.signal_style_reset_message);
        alertDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                iconColorReset();
                createCustomView();
            }
        });
        alertDialog.setNegativeButton(R.string.cancel, null);
        alertDialog.create().show();
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
         if (preference == mSignalStyle) {
            int signalStyle = Integer.valueOf((String) newValue);
            int index = mSignalStyle.findIndexOfValue((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_SIGNAL_TEXT, signalStyle);
            mSignalStyle.setSummary(mSignalStyle.getEntries()[index]);
            createCustomView();
            return true;
        } else if (preference == mSignalColor) {
            String hex = ColorPickerPreference.convertToARGB(Integer
                    .valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_SIGNAL_TEXT_COLOR, intHex);
            return true;
        }
        return false;
    }

    private void iconColorReset() {
        Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.STATUS_BAR_SIGNAL_TEXT, 0);
        Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.STATUS_BAR_SIGNAL_TEXT_COLOR, -2);
    }

    private void updateSignalColorOption(int signalStyle) {
        if (signalStyle == 1) {
            mSignalColor.setEnabled(true);
        } else {
            mSignalColor.setEnabled(false);
        }
    }

}
