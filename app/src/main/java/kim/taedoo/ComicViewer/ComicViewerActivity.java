package kim.taedoo.ComicViewer;

import com.amazon.device.ads.AdError;
import com.amazon.device.ads.AdLayout;
import com.amazon.device.ads.AdListener;
import com.amazon.device.ads.AdProperties;
import com.amazon.device.ads.AdRegistration;
import com.amazon.device.ads.AdTargetingOptions;
import com.google.ads.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.TextUtils.TruncateAt;
//import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ComicViewerActivity extends Activity 
		implements OnItemClickListener,OnClickListener {
	
	/*** 自動ブックマーク ON/OFF ***/
	public static boolean autoMark;
	/*** スライドショー ON/OFF ***/
	public static boolean slideShow;
	/*** 見開き表示 ON/OFF ***/
	public static boolean spread;
	/*** 見開き方向 true=右から左 false=左から右 ***/
	public static boolean direction;
	public String bookMarks = "";
	public String[] bookMarkList = null;
	
	private	final String TAG;
	private final int	MIN_LIST_HEIGHT = 48;
	
	/** 広告をGoogleを使うか、Amazonを使うか。trueだとGoogle／falseだとAmazon **/
//	private static final boolean ADS_GOOLE = true;
	/** 広告がテストかどうか。 trueだとテスト／falseだと本番 **/
//	private static final boolean ADS_TEST = false;

	private ListView fileList = null;
	public ArrayList<DirItem> dirItems;
	
	private AdView adView;	//GoogleAds
	private AdLayout adLayout;	//AmazonAds

	public ComicViewerActivity() {
		TAG = "ComicViewerActivity";
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.cv_main_activity);

		ImageButton updirBtn = (ImageButton)findViewById(R.id.upDir);
		updirBtn.setOnClickListener(this);
		
		ImageButton setBtn = (ImageButton)findViewById(R.id.setUp);
		setBtn.setOnClickListener(this);
		
		ImageButton resumeBtn = (ImageButton)findViewById(R.id.resume);
		resumeBtn.setOnClickListener(this);
		
		ImageButton bookmarkBtn = (ImageButton)findViewById(R.id.bookMark);
		bookmarkBtn.setOnClickListener(this);

		ImageButton searchBtn = (ImageButton)findViewById(R.id.fileSearch);
		searchBtn.setOnClickListener(this);

		adView = (AdView)findViewById(R.id.adView);
		adView.setGravity(Gravity.CENTER_HORIZONTAL);
		adView.loadAd(new AdRequest());
/* Amazon mobileAdsがまだ日本非対応なのでコメント化
		if(ADS_GOOLE){
			// GoogleのadView を作成する
			adView = (AdView)findViewById(R.id.adView);
			adView.setGravity(Gravity.CENTER_HORIZONTAL);
			adView.loadAd(new AdRequest());
		}else{
			// AmazonのAdLayoutを作成する
			adLayout = (AdLayout)findViewById(R.id.adLayout);
			AdRegistration.enableLogging(ADS_TEST);
			AdRegistration.enableTesting(ADS_TEST);
			AdRegistration.setAppKey("89f2a011df444fc5a6c5520b0ae23c98");
			//adLayout.setTimeout(20000);
			adLayout.loadAd(new AdTargetingOptions()); // async task to retrieve an ad
		}
*/
		BookmarkCtl.bookmarkFilename=getCacheDir().getAbsolutePath()+"/bookmarks.txt";
		//とりあえずデフォルトディレクトリから開く
		EditText pwd = (EditText)findViewById(R.id.pwdDir);
		fileList = (ListView)findViewById(R.id.fileList);
		
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		String str = pref.getString(
				getString( R.string.pre_key_defpath ), Environment.getExternalStorageDirectory().getPath()+"/" );
		pwd.setText(str);	//取得したデフォルトディレクトリをセット
		autoMark = pref.getBoolean(getString(R.string.pre_key_automark), true);
		slideShow = pref.getBoolean(getString(R.string.pre_key_doubletap), true);
		spread =  pref.getBoolean(getString(R.string.pre_key_spread), true);
		direction =  pref.getBoolean(getString(R.string.pre_key_direction), true);
		
		String dirName = pwd.getText().toString();
		
		dirListSet(dirName);
	}
	
	@Override
	protected void onDestroy() {
		BookmarkCtl.execBookmarkSort();

		adView.destroy();
/* Amazon mobileAdsがまだ日本非対応なのでコメント化
		if(ADS_GOOLE){
			adView.destroy();
		}else{
			adLayout.destroy();
		}
*/

		super.onDestroy();
	}
	
	/***
	 * 指定されたディレクトリパス内のファイル一覧を取得する関数
	 * @author Kim
	 * @param dirPath ***/
	private void dirListSet(String dirPath) {
		EditText pwd = (EditText)findViewById(R.id.pwdDir);

		File dir = new File(dirPath);
		File[] fileLists = dir.listFiles();
		dirItems = new ArrayList<DirItem>();
		ArrayList<DirItem> dirItemdirs = new ArrayList<DirItem>();
		ArrayList<DirItem> dirItemfiles = new ArrayList<DirItem>();
		Bitmap folderIcon = BitmapFactory.decodeResource(getResources(), R.drawable.folder_icon);
		Bitmap zipIcon = BitmapFactory.decodeResource(getResources(), R.drawable.zip_icon);
		Bitmap cbzIcon = BitmapFactory.decodeResource(getResources(), R.drawable.cbz_icon);
		Bitmap rarIcon = BitmapFactory.decodeResource(getResources(), R.drawable.rar_icon);
		Bitmap cbrIcon = BitmapFactory.decodeResource(getResources(), R.drawable.cbr_icon);
		Bitmap upIcon = BitmapFactory.decodeResource(getResources(), R.drawable.up_folder_icon);
		String dirMsg = getResources().getString(R.string.msg_directory);
		String zipMsg = getResources().getString(R.string.msg_zip_file);
		String cbzMsg = getResources().getString(R.string.msg_cbz_file);
		String rarMsg = getResources().getString(R.string.msg_rar_file);
		String cbrMsg = getResources().getString(R.string.msg_cbr_file);
		String upMsg = getResources().getString(R.string.msg_up_directory);

		for( int i=0; i < fileLists.length ; i++ ){
			DirItem fileAdapter = new DirItem();
			if(fileLists[i].isDirectory()){
				fileAdapter.setDirItem(
						folderIcon,
						fileLists[i].getName()+"/",
						dirMsg
						);
				int j=0;
				for( ; j < dirItemdirs.size() ; j++){
					if( fileAdapter.fName.compareTo(dirItemdirs.get(j).fName) < 0 ){
						dirItemdirs.add(j,fileAdapter);
						break;
					}
				}
				if(j==dirItemdirs.size())
					dirItemdirs.add(j,fileAdapter);
			}else{
				if( fileLists[i].getName().toLowerCase(Locale.getDefault()).endsWith(".zip") ){
					fileAdapter.setDirItem(
							zipIcon,
							fileLists[i].getName(),
							zipMsg);
				}else if( fileLists[i].getName().toLowerCase(Locale.getDefault()).endsWith(".cbz")){
					fileAdapter.setDirItem(
							cbzIcon,
							fileLists[i].getName(),
							cbzMsg);
				}else if( fileLists[i].getName().toLowerCase(Locale.getDefault()).endsWith(".rar")){
					fileAdapter.setDirItem(
							rarIcon,
							fileLists[i].getName(),
							rarMsg);
				}else if( fileLists[i].getName().toLowerCase(Locale.getDefault()).endsWith(".cbr")){
					fileAdapter.setDirItem(
							cbrIcon,
							fileLists[i].getName(),
							cbrMsg);
				}else{		//フォルダでもzipファイルでもない場合は無視。
					continue;
				}
				int j=0;
				for( ; j < dirItemfiles.size() ; j++){
					if( fileAdapter.fName.compareTo(dirItemfiles.get(j).fName) < 0 ){
						dirItemfiles.add(j,fileAdapter);
						break;
					}
				}
				if(j==dirItemfiles.size())
					dirItemfiles.add(j,fileAdapter);
			}
		}
		dirItems.addAll(dirItemdirs);
		dirItems.addAll(dirItemdirs.size(),dirItemfiles);
		if( !dirPath.equals("/") ){
			DirItem fileAdapter = new DirItem();
			fileAdapter.setDirItem(
					upIcon,
					"../",
					upMsg);
			dirItems.add(0,fileAdapter);
		}
		fileList.setAdapter(new DirArrayAdapter());
		fileList.setOnItemClickListener(this);
		pwd.setText(dirPath);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		
		ArrayList<String>	fileNameList = null;
		
		EditText pwd = (EditText)findViewById(R.id.pwdDir);
		if( !v.getTag().toString().endsWith("/") ){
			fileNameList = new ArrayList<String>();
			//zipファイルを対象にした処理 Intentをコール
			//前のファイル・次のファイルへの移動の為に、ディレクトリ以外のファイルのリストを作り、Intent に設定
			for(int i=0; i < dirItems.size() ; i++){
				DirItem item=dirItems.get(i);
				if( !item.fName.endsWith("/") ){
					fileNameList.add(item.fName);
				}else{
					position--;
				}
			}
			
			int pageNum = 0;
			if( autoMark )
				pageNum = BookmarkCtl.getMarkingPage( pwd.getText().toString(),v.getTag().toString() );
					
			Intent pageIntent = new Intent(this,ComicImageActivity.class);
			pageIntent.putExtra(
					getString(R.string.intent_open_filename),
					pwd.getText().toString()+v.getTag().toString()
					);	//ファイル名をセット
			pageIntent.putExtra(getString(R.string.intent_open_dirname), pwd.getText().toString());
			pageIntent.putExtra(getString(R.string.intent_file_lists), fileNameList);
			pageIntent.putExtra(getString(R.string.intent_file_position), position);
			pageIntent.putExtra(getString(R.string.intent_page_position), pageNum);
			pageIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(pageIntent);
			
		}else{
			if( v.getTag().equals("../")){
				String pwdPath = pwd.getText().toString();
				String[] dirNames = pwdPath.split("/");
				pwdPath="";
				for(int i=0;i<dirNames.length-1;i++)
					pwdPath=pwdPath+dirNames[i]+"/";
				pwd.setText(pwdPath);
			}else{
				pwd.setText(pwd.getText().toString()+v.getTag().toString());
			}
			dirListSet(pwd.getText().toString());
		}
	}
	
	@Override
	public void onClick(View v) {
		EditText pwd = (EditText)findViewById(R.id.pwdDir);
		switch( v.getId() ){
		case R.id.upDir:	//上位ディレクトリへの移動ボタンクリック
			if( pwd.getText().toString().equals("/") ){
				Toast.makeText(
						getApplicationContext(),
						getResources().getString(R.string.err_msg_root_dir),
						Toast.LENGTH_SHORT).show();
			}else{
				String pwdPath = pwd.getText().toString();
				String[] dirNames = pwdPath.split("/");
				pwdPath="";
				for(int i=0;i<dirNames.length-1;i++)
					pwdPath=pwdPath+dirNames[i]+"/";
				pwd.setText(pwdPath);
				dirListSet(pwd.getText().toString());
			}
			break;
		case R.id.setUp :	//設定ボタンクリック
			Intent preIntent = new Intent(this,ComicPreferenceActivity.class);
			preIntent.putExtra("DefaultPath", pwd.getText().toString());
			startActivity(preIntent);
			break;
		case R.id.resume :	//前回ファイル呼び出しボタンクリック
			int position=0;
			SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
			String str = pref.getString(
					getString( R.string.pre_key_resume ), "" );
			if( str.equals("") ){
				Toast.makeText(getApplicationContext(), R.string.err_msg_no_resume, Toast.LENGTH_SHORT).show();
				return;
			}
			String dirName = str.split("#")[0];
			String fileName = str.split("#")[1];
			String pageNumstr = str.split("#")[2];
			if( !str.equals("") ){
				dirListSet( dirName );
				ArrayList<String>	fileNameList = null;
				fileNameList = new ArrayList<String>();
				//zipファイルを対象にした処理 Intentをコール
				//前のファイル・次のファイルへの移動の為に、ファイルのリストを作り、Intent に設定
				for(int i=0; i < dirItems.size() ; i++){
					DirItem item=dirItems.get(i);
					fileNameList.add(item.fName);
					if( item.fName.equals(fileName) )
						position += i;
				}
				
				int pageNum = Integer.parseInt( pageNumstr );
						
				Intent pageIntent = new Intent(this,ComicImageActivity.class);
				pageIntent.putExtra(getString(R.string.intent_open_filename),dirName+fileName);	//ファイル名をセット
				pageIntent.putExtra(getString(R.string.intent_open_dirname), dirName );
				pageIntent.putExtra(getString(R.string.intent_file_lists), fileNameList);
				pageIntent.putExtra(getString(R.string.intent_file_position), position);
				pageIntent.putExtra(getString(R.string.intent_page_position), pageNum);
				pageIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(pageIntent);
				Toast.makeText( getApplicationContext(), getString(R.string.msg_open_resume), Toast.LENGTH_SHORT).show();
			}else{
				Toast.makeText( getApplicationContext(), getString(R.string.msg_no_resume), Toast.LENGTH_SHORT).show();
			}
			break;
		case R.id.bookMark:	//ブックマークボタンクリック
			showBookmarkDialog();
			break;
		case R.id.fileSearch:	//ブックマークボタンクリック
			showSearchDialog( pwd.getText().toString() );
			break;
		}
	}

	/***
	 * FileSearchDialogを表示する関数
	 */
	private void showSearchDialog(String dirName) {
		final ComicSearchDialog searchDialog = new ComicSearchDialog();

//		EditText searchKeyword = (EditText)findViewById(R.id.searchKeyword);
//		searchKeyword.setTag(R.string.dialog_tag_dir, dirName);
		searchDialog.dirName = dirName;
		
		searchDialog.show(getFragmentManager(), TAG);
	}

	/***
	 * BookmarkDialogを表示する関数
	 */
	private void showBookmarkDialog() {
		final ComicBookmarkDialog bookmarkDialog = new ComicBookmarkDialog();

		bookmarkDialog.show(getFragmentManager(), TAG);
	}

	/***
	 * ListViewの中身を構成するクラス
	 * @author Kin
	 *
	 */
	public class DirArrayAdapter extends BaseAdapter implements ListAdapter {

		@Override
		public int getCount() {
			return dirItems.size();
		}

		@Override
		public DirItem getItem(int position) {
			return dirItems.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
			Context context=ComicViewerActivity.this;
			DirItem item=dirItems.get(position);
			if(convertView == null){
				LinearLayout layout=new LinearLayout(context);
				layout.setPadding(10, 5, 10, 5);
				layout.setGravity(Gravity.CENTER_VERTICAL);
				convertView=layout;
				
				ImageView iconImg=new ImageView(context);
				iconImg.setTag("icon");
				iconImg.setPadding(0, 0, 10, 0);
				
				TextView mainText = new TextView(context);
				mainText.setTag("main");
				mainText.setTextSize(getResources().getInteger(R.integer.text_size_title));
				mainText.setMaxLines(2);
				mainText.setEllipsize(TruncateAt.END);
				mainText.setTextColor(getResources().getColor(R.color.main_text_color));

			    TextView subText = new TextView(context);
				subText.setTag("sub");
				subText.setTextSize(getResources().getInteger(R.integer.text_size_sub));
				subText.setMaxLines(2);
				subText.setPadding(10, 0, 5, 0);
				subText.setTextColor(getResources().getColor(R.color.sub_text_color));

				LinearLayout.LayoutParams lp=new
						LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
						LayoutParams.WRAP_CONTENT);
				//weightを5に設定
				lp.weight=5;
				layout.addView(iconImg);
				layout.addView(mainText,lp);
				layout.addView(subText);
			}
			ImageView icon=(ImageView)convertView.findViewWithTag("icon");
			icon.setImageBitmap(item.fIcon);
			TextView main=(TextView)convertView.findViewWithTag("main");
			main.setText(item.fName);
			TextView sub=(TextView)convertView.findViewWithTag("sub");
			sub.setText(item.fInfo);
			convertView.setTag(item.fName);
			if( item.fIcon.getHeight()/getBaseContext().getResources().getDisplayMetrics().density < MIN_LIST_HEIGHT )
				convertView.setMinimumHeight( (int)(MIN_LIST_HEIGHT * getBaseContext().getResources().getDisplayMetrics().density) );
			return convertView;
		}

	}

}
