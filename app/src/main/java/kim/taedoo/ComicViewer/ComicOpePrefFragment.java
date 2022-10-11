package kim.taedoo.ComicViewer;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.Preference.OnPreferenceChangeListener;

public class ComicOpePrefFragment extends PreferenceFragment {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.cv_setting_operation);
		final Preference slideshow=findPreference(getString(R.string.pre_key_doubletap));
		slideshow.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
//				CheckBoxPreference slideshow = new CheckBoxPreference(preference.getContext());
				if( (Boolean) newValue )
					ComicViewerActivity.slideShow=true;
				else
					ComicViewerActivity.slideShow=false;
				return true;
			}
		});
	}

}
