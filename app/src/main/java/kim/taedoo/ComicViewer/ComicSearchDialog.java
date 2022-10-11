package kim.taedoo.ComicViewer;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class ComicSearchDialog extends DialogFragment {
	private Dialog dialog=null;
	public String dirName="";

	@Override
	public Dialog onCreateDialog(Bundle saveInstanceState) {
		dialog = new Dialog(getActivity());
		dialog.setTitle(R.string.fileSearch);
		dialog.setContentView(R.layout.cv_search_dialog);
		// Close ボタンのリスナ
		dialog.findViewById(R.id.closeBtn).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
		
		EditText searchKeyword = (EditText)dialog.findViewById(R.id.searchKeyword); 
		searchKeyword.setOnEditorActionListener(new OnEditorActionListener() {
			
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if(event == null && actionId == 6 || 
						event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER){
	
					// 検索してリストを作成する処理を設定する
					LinearLayout searchList = (LinearLayout)dialog.findViewById(R.id.searchDialogList);
					searchList.setBackgroundColor(Color.DKGRAY);
					searchList.removeAllViews();
					File dir = new File(dirName);
					if( !dir.isDirectory() ){
						Log.e("SearchDialogError","Not Dir!");
						return false;
					}
					String[] fileItems = dir.list();
					ArrayList<String> fileSort = new ArrayList<String>();
					for(int i=0 ; i < fileItems.length ; i++){
						int j=0;
						for( ; j < i ; j++){
							if( fileSort.get(j).compareTo(fileItems[i]) > 0 ){
								fileSort.add(j,fileItems[i]);
								break;
							}
						}
						if( j == fileSort.size() ){
							fileSort.add(fileItems[i]);
						}
					}
					for(int i=0 ; i < fileSort.size() ; i++){
						if( ( fileSort.get(i).toLowerCase(Locale.getDefault()).endsWith(".zip")
								||  fileSort.get(i).toLowerCase(Locale.getDefault()).endsWith(".cbz") )
								&& fileSort.get(i).toLowerCase(Locale.getDefault()).indexOf(
										v.getText().toString().toLowerCase(Locale.getDefault())
										) > 0 ){
							File fileCheck = new File(dirName+fileSort.get(i));
							if( fileCheck.canRead() ){
								TextView listText = new TextView(getActivity());
								listText.setText( fileSort.get(i) );
								listText.setTextSize(getResources().getInteger(R.integer.text_size_bookmark));
								listText.setTag(R.string.dialog_tag_dir, dirName);
								listText.setTag(R.string.dialog_tag_file, fileCheck.getName());
								listText.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT));
								listText.setTextColor(Color.WHITE);
								listText.setOnClickListener(new View.OnClickListener() {
									
									@Override
									public void onClick(View v) {
										String filePath = (String) v.getTag(R.string.dialog_tag_dir);
										String fileName = (String) v.getTag(R.string.dialog_tag_file);
										int filePage = BookmarkCtl.getMarkingPage(filePath, fileName);
										ArrayList<String> fileNameList = new ArrayList<String>();
										int position = 0;
										
										File dirFile= new File(filePath);
										File[] fileLists = dirFile.listFiles();
										for( int i=0; i < fileLists.length ; i++ ){
											if(!fileLists[i].isDirectory()){
												if( !fileLists[i].getName().toLowerCase(Locale.getDefault()).endsWith(".zip") ){
													continue;
												}
												int j=0;
												for( ; j < fileNameList.size() ; j++){
													if( fileLists[i].getName().compareTo( fileNameList.get(j) ) < 0 ){
														fileNameList.add(j,fileLists[i].getName());
														break;
													}
												}
												if(j==fileNameList.size())
													fileNameList.add(fileLists[i].getName());
											}
										}
										for( int i=0; i < fileNameList.size() ;i++){
											if(fileNameList.get(i).equals(fileName)){
												position=i;
												break;
											}
										}
										
										Intent pageIntent = new Intent(v.getContext(),ComicImageActivity.class);
										pageIntent.putExtra(
												getString(R.string.intent_open_filename),
												filePath+fileName
												);	//ファイル名をセット
										pageIntent.putExtra(getString(R.string.intent_open_dirname), filePath);
										pageIntent.putExtra(getString(R.string.intent_file_lists), fileNameList);
										pageIntent.putExtra(getString(R.string.intent_file_position), position);
										pageIntent.putExtra(getString(R.string.intent_page_position), filePage);
										pageIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
										startActivity(pageIntent);
										
										dialog.dismiss();
										
									}
								});
								listText.setMaxLines(2);
								listText.setPadding(10, 10, 10, 10);
								searchList.addView(listText);
							}
						}
					}
					searchList.refreshDrawableState();
				}
				return false;
			}
		});
		
	        
		return dialog;
	}

}
