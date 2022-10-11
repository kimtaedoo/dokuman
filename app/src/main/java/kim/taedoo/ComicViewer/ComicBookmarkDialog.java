package kim.taedoo.ComicViewer;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ComicBookmarkDialog extends DialogFragment {
	private Dialog dialog=null;
	private	final	int REFLESH_TIME = 200;

	@Override
	public Dialog onCreateDialog(Bundle saveInstanceState) {
		dialog = new Dialog(getActivity());
		dialog.setTitle(R.string.bookMarks);
		dialog.setContentView(R.layout.cv_bookmark_dialog);
		// Close ボタンのリスナ
		dialog.findViewById(R.id.closeBtn).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
		dialog.findViewById(R.id.delBtn).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if( BookmarkCtl.delBookmarkFile() ){
					Toast.makeText(getActivity(),
							getResources().getString(R.string.msg_del_allmark),
							Toast.LENGTH_LONG
							).show();
					LinearLayout bmList = (LinearLayout)dialog.findViewById(R.id.searchDialogList);
					bmList.removeAllViews();
				}else{
					Toast.makeText(getActivity(),
							getResources().getString(R.string.err_msg_del_bookmark),
							Toast.LENGTH_LONG
							).show();
				}
			}
		});
		
		BookmarkCtl.execBookmarkSort();
		makeBookmarkList();
	        
		return dialog;
	}
	
	private void makeBookmarkList(){
		LinearLayout bmList = (LinearLayout)dialog.findViewById(R.id.searchDialogList);
		bmList.removeAllViews();
		bmList.setBackgroundColor(Color.DKGRAY);
		String markedList = BookmarkCtl.getBookmarkList();
		String[] markItem = markedList.split("\n");
		
		if( markItem[0].equals("") )
			return;
		
		for(int i=0 ; i < markItem.length ; i++){
			String filePath = markItem[i].split("#")[0];
			String fileName = markItem[i].split("#")[1];
			String filePage = markItem[i].split("#")[2];
			TextView listText = new TextView(getActivity());
			File fileCheck = new File(filePath+fileName);
			listText.setText( filePath+fileName+"(Page:"+filePage+")" );
			listText.setTag(R.string.dialog_tag_dir, filePath);
			listText.setTag(R.string.dialog_tag_file, fileName);
			listText.setTag(R.string.dialog_tag_page,Integer.parseInt(filePage));
			listText.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT));
			listText.setTextSize(getResources().getInteger(R.integer.text_size_bookmark));
			if( fileCheck.canRead() ){
				listText.setTextColor(Color.WHITE);
				listText.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						String filePath = (String) v.getTag(R.string.dialog_tag_dir);
						String fileName = (String) v.getTag(R.string.dialog_tag_file);
						int filePage = (Integer) v.getTag(R.string.dialog_tag_page);
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
			}else{
				listText.setTextColor(Color.RED);
			}
			listText.setOnLongClickListener(new OnLongClickListener() {
				
				@Override
				public boolean onLongClick(View v) {
					String filePath = (String) v.getTag(R.string.dialog_tag_dir);
					String fileName = (String) v.getTag(R.string.dialog_tag_file);
					if( BookmarkCtl.delBookmarkFile(filePath,fileName) ){
						Toast t = Toast.makeText(
								getActivity()
								, String.format(getString(R.string.msg_del_bookmark),fileName)
								, Toast.LENGTH_SHORT
								);
						t.setGravity(Gravity.CENTER , 0, 0 );
						t.show();
					}else{
						Toast.makeText(getActivity(),
								getResources().getString(R.string.err_msg_del_bookmark),
								Toast.LENGTH_LONG
								).show();
					}

					Timer listTimer = new Timer();
					listTimer.schedule(new TimerTask(){
						private Handler handler = new Handler();

						@Override
						public void run() {
							handler.post(new Runnable() {
								
								@Override
								public void run() {
									makeBookmarkList();
								}
							});
						}
					}, REFLESH_TIME);

					return false;
				}
			});
			listText.setMaxLines(2);
			listText.setPadding(10, 10, 10, 10);
			bmList.addView(listText);
		}
		
	}

}
