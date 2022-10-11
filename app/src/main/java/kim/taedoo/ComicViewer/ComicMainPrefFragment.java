package kim.taedoo.ComicViewer;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.Preference.OnPreferenceChangeListener;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

public class ComicMainPrefFragment extends PreferenceFragment {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.cv_setting_main);

		final Preference defDir=findPreference(getString(R.string.pre_key_defpath));
		defDir.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				SharedPreferences.Editor pref = preference.getEditor();
				pref.putString(
						getString( R.string.pre_key_defpath ), ComicPreferenceActivity.defPath);
				pref.commit();
				defDir.setSummary(getResources().getString(R.string.pre_msg_defpath)
						+ComicPreferenceActivity.defPath);
				return false;
			}
			
		});
		
		final Preference automark=findPreference(getString(R.string.pre_key_automark));
		automark.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				ComicViewerActivity.autoMark=(Boolean) newValue;
				return true;
			}
		});
		
		final Preference spread=findPreference(getString(R.string.pre_key_spread));
		spread.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				ComicViewerActivity.spread=(Boolean) newValue;
				return true;
			}
		});
		
		final Preference direction=findPreference(getString(R.string.pre_key_direction));
		direction.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				ComicViewerActivity.direction = (Boolean)newValue;
				return true;
			}
		});
		
		final Preference howto = findPreference(getString(R.string.pre_key_howto));
		howto.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				
				TextView tv = new TextView(getActivity());
				tv.setText(Html.fromHtml( getString(R.string.howtoText)) );
				tv.setMovementMethod(LinkMovementMethod.getInstance());
				
				AlertDialog.Builder howtoDialog = new AlertDialog.Builder(getActivity());
				howtoDialog.setTitle(R.string.pre_title_howto);
				howtoDialog.setView(tv);
				howtoDialog.setPositiveButton("OK", null);
				howtoDialog.show();
				return false;
			}
			
		});
		
		final Preference about = findPreference(getString(R.string.pre_key_about));
		about.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				
				TextView tv = new TextView(getActivity());
				tv.setText(Html.fromHtml( getString(R.string.aboutText)) );
				tv.setMovementMethod(LinkMovementMethod.getInstance());
				
				AlertDialog.Builder aboutDialog = new AlertDialog.Builder(getActivity());
				aboutDialog.setTitle(R.string.pre_title_about);
				aboutDialog.setView(tv);
				aboutDialog.setPositiveButton("OK", null);
				aboutDialog.show();
				return false;
			}
			
		});
		
	}

}
