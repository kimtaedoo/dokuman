package kim.taedoo.ComicViewer;

import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class ComicPreferenceActivity extends PreferenceActivity {
	public static String defPath="";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public void onBuildHeaders(List<Header> target) {
		super.onBuildHeaders(target);

		//引数として渡されたパスを、デフォルトパスの設定候補として保持
		Intent data=getIntent();
		defPath = data.getStringExtra("DefaultPath");

		loadHeadersFromResource(R.xml.cv_setting_fragment, target);
	}

}
