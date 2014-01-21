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

public class StatusBarNetworkStateStyle extends SettingsPreferenceFragment
    implements OnPreferenceChangeListener {

    private static final String TAG = "StatusBarNetworkStateStyle";

    private static final String STATUS_BAR_NETWORK_STATS = "status_bar_show_network_stats";
    private static final String STATUS_BAR_NETWORK_STATS_UPDATE = "status_bar_network_status_update";
    private static final String STATUS_BAR_NETWORK_STATS_COLOR = "status_bar_network_status_color";
    
    private static final int MENU_RESET = Menu.FIRST;

    private ListPreference mNetStatsUpdate;
    private ColorPickerPreference mNetStatsColorPicker;
    private CheckBoxPreference mNetworkStats;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createCustomView();
    }

    private PreferenceScreen createCustomView() {
        PreferenceScreen prefSet = this.getPreferenceScreen();
        if (prefSet != null) {
            prefSet.removeAll();
        }

        addPreferencesFromResource(R.xml.status_bar_network_style);
        prefSet = getPreferenceScreen();

        mNetworkStats = (CheckBoxPreference) findPreference(STATUS_BAR_NETWORK_STATS);
        mNetworkStats.setChecked((Settings.System.getInt(getActivity().getApplicationContext()
                .getContentResolver(), Settings.System.STATUS_BAR_NETWORK_STATS, 0) == 1));

        mNetStatsColorPicker = (ColorPickerPreference) findPreference(STATUS_BAR_NETWORK_STATS_COLOR);
        mNetStatsColorPicker.setOnPreferenceChangeListener(this);

        mNetStatsUpdate = (ListPreference) findPreference(STATUS_BAR_NETWORK_STATS_UPDATE);
        long statsUpdate = Settings.System.getInt(getActivity().getApplicationContext()
                .getContentResolver(),
        Settings.System.STATUS_BAR_NETWORK_STATS_UPDATE_INTERVAL, 500);
        mNetStatsUpdate.setValue(String.valueOf(statsUpdate));
        mNetStatsUpdate.setSummary(mNetStatsUpdate.getEntry());
        mNetStatsUpdate.setOnPreferenceChangeListener(this);

        int intColor;
        String hexColor;

        intColor = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_NETWORK_STATS_TEXT_COLOR, -2);
        if (intColor == -2) {
            intColor = 0xffffffff;
            mNetStatsColorPicker.setSummary(getResources().getString(R.string.default_string));
        } else {
            hexColor = String.format("#%08x", (0xffffffff & intColor));
            mNetStatsColorPicker.setSummary(hexColor);
        }
        mNetStatsColorPicker.setNewPreviewColor(intColor);

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
        alertDialog.setMessage(R.string.status_bar_network_stats_color_reset_message);
        alertDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                iconColorReset();
                createCustomView();
            }
        });
        alertDialog.setNegativeButton(R.string.cancel, null);
        alertDialog.create().show();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        
        if (preference == mNetworkStats) {
            boolean value = mNetworkStats.isChecked();
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.STATUS_BAR_NETWORK_STATS, value ? 1 : 0);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mNetStatsUpdate) {
            long updateInterval = Long.valueOf((String) newValue);
            int index = mNetStatsUpdate.findIndexOfValue((String) newValue);
            Settings.System.putLong(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.STATUS_BAR_NETWORK_STATS_UPDATE_INTERVAL, updateInterval);
            mNetStatsUpdate.setSummary(mNetStatsUpdate.getEntries()[index]);
            return true;
        } else if (preference == mNetStatsColorPicker) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            preference.setSummary(hex);

            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.STATUS_BAR_NETWORK_STATS_TEXT_COLOR, intHex);
            return true;
        }
        return false;
    }

    private void iconColorReset() {
        Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.STATUS_BAR_NETWORK_STATS_TEXT_COLOR, -2);
    }
}
