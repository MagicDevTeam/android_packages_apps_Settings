package com.android.settings.magicmod;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.widget.DevCard;
import com.android.settings.SettingsPreferenceFragment;

public class MagicModInfo extends SettingsPreferenceFragment {

	Preference m591Site;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.magicmod_info);
		
		m591Site = findPreference("site_591");

	}

	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
			Preference preference) {
		if(preference == m591Site) {
		  gotoUrl("http://www.591fan.com");
		}
		return super.onPreferenceTreeClick(preferenceScreen, preference);
		/*
		 * if (preference == mGummySite) { gotoUrl("http://www.teamgummy.com");
		 * } else if (preference == mGummySource) {
		 * gotoUrl("http://www.github.com/teamgummy"); }
		 */
		//gotoUrl("http://www.591fan.com");
	}

	private void gotoUrl(String uri) {
		Uri page = Uri.parse(uri);
		Intent i = new Intent(Intent.ACTION_VIEW, page);
		getActivity().startActivity(i);
	}
}