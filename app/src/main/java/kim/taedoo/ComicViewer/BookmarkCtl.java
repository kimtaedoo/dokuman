package kim.taedoo.ComicViewer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import android.util.Log;

public class BookmarkCtl {
	public static String bookmarkFilename="";
	
	/***
	 * bookmarkのファイルの中身をStringで返す関数
	 * @return bookmarkファイルの中身をStringで
	 */
	public static String getBookmarkList( ) {
		String TAG="getBookmarkList";
		File markFile = new File(bookmarkFilename);
		String bookMarks = "";
		String bufLine = "";
		BufferedReader fileBuff = null;
		if( !markFile.canRead() ){
			try {
				markFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
				Log.e(TAG,"Bookmark File create Error!");
			}
			return bookMarks;
		}
		try {
			fileBuff=new BufferedReader(new FileReader(markFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		while( bufLine != null ){
			try {
				bufLine = fileBuff.readLine();
			} catch (IOException e) {
				e.printStackTrace();
				Log.e(TAG,"Bookmark File read Error!");
				return "";
			}
			if( bufLine != null )
				bookMarks = bookMarks + bufLine + "\n";
		}
		try {
			fileBuff.close();
		} catch (IOException e) {
			e.printStackTrace();
			Log.e(TAG,"Bookmark File close!");
		}
		return bookMarks;
	}

	/***
	 * ブックアークファイルに、ブックマーク情報を書き込む関数
	 * @param markingFile	ブックマークするファイルの絶対パス
	 * @param pageNum		ブックマークするページ番号
	 * @return				書き込みが正常終了すれば true 、失敗したら false
	 */
	public static boolean writeBookmarkList( String markingFile, int pageNum ) {
		String TAG="writeBookmarkList";
		File markFile = new File(bookmarkFilename);
		File markingFileItem = new File(markingFile); 
		ArrayList <String> bookMarks = new ArrayList<String>();
		String markCheck = markingFileItem.getParent()+"/#"+markingFileItem.getName();
		String newMark = markCheck+"#"+String.valueOf(pageNum);
		BufferedReader infileBuff = null;
		BufferedWriter outfileBuff = null;
		String fileBuf = "";
		boolean setFlag = false;

		if( !markFile.canRead() ){			//ブックマークファイルが無い場合、ファイルを作成して情報を書き込む
			try {
				markFile.createNewFile();
				markFile.setWritable(true);
				outfileBuff = new BufferedWriter( new FileWriter(markFile));
				outfileBuff.write(newMark);
				outfileBuff.newLine();
				outfileBuff.close();
			} catch (IOException e) {
				e.printStackTrace();
				Log.e(TAG,"Bookmark File create Error!");
				return false;
			}
		}else{			//ブックマークファイルがある場合は、現在のブックマークを探して上書きする。
			try {
				infileBuff=new BufferedReader( new FileReader(markFile) );
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			while( fileBuf != null ){
				try {
					fileBuf=infileBuff.readLine();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				if( fileBuf != null )
					if( fileBuf.indexOf( markCheck ) >= 0 ){
						bookMarks.add(newMark);
						setFlag=true;
					}else{
						bookMarks.add(fileBuf);
					}
			}
			if( !setFlag )
				bookMarks.add(newMark);
			try {
				infileBuff.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			if( !markFile.canWrite() ){
				Log.e(TAG,"Bookmark File Can't writable!");
				return false;
			}
			try {
				outfileBuff = new BufferedWriter( new FileWriter(markFile));
			} catch (IOException e) {
				e.printStackTrace();
				Log.e(TAG,"Bookmark File write Error!");
			}
			
			for(int i=0; i < bookMarks.size() ; i++ ){
				try {
					outfileBuff.write(bookMarks.get(i));
					outfileBuff.newLine();
				} catch (IOException e1) {
					e1.printStackTrace();
					Log.e(TAG,"Bookmark File write Error!");
				}
			}
			
			try {
				outfileBuff.close();
			} catch (IOException e) {
				e.printStackTrace();
				Log.e(TAG,"Bookmark File close Error!");
			}
		}
		return true;
	}

	/***
	 * getfileNameで指定されたファイルのブックマークされているページ番号を返す
	 * @param getdirName 指定ファイルのディレクトリパス
	 * @param getfileName 指定ファイル
	 * @return 指定ファイルのブックマークされているページ番号
	 */
	public static int getMarkingPage( String getdirName, String getfileName ) {
		String TAG="getMarkingPage";
		File markFile = new File(bookmarkFilename);
		String bookMarks = "";
		String checkMarks = getdirName+"#"+getfileName;
		BufferedReader fileBuff = null;
		int getPage=0;
		if( !markFile.canRead() ){
			Log.e(TAG,"Bookmark File read Error!");
			return -1;
		}
		try {
			fileBuff=new BufferedReader( new FileReader(markFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		while( bookMarks != null ){
			try {
				bookMarks=fileBuff.readLine();
			} catch (IOException e) {
				e.printStackTrace();
				Log.e(TAG,"Bookmark File read Error!");
				return -1;
			}
			if( bookMarks != null && bookMarks.indexOf(checkMarks) >= 0){
				getPage=Integer.parseInt( bookMarks.split("#")[2] );
				break;
			}
		}
		try {
			fileBuff.close();
		} catch (IOException e) {
			e.printStackTrace();
			Log.e(TAG,"Bookmark File close!");
		}
		
		return getPage;
	}

	/***
	 * Bookmarkファイルの中身を、ファイル名順にソートする関数
	 * @return ソートに成功すればtrue、エラーならfalse
	 */
	public static boolean execBookmarkSort() {
		String TAG="execBookmarkSort";
		File markFile = new File(bookmarkFilename);
		ArrayList<String> bookMarks = new ArrayList<String>();
		String bufLine = "";
		BufferedReader fileBuff = null;
		
		//ファイルを読み込んでファイル名順にソート
		if( !markFile.canRead() ){
			Log.e(TAG,"Bookmark File read Error!");
			return false;
		}
		try {
			fileBuff=new BufferedReader(new FileReader(markFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		while( bufLine != null ){
			try {
				bufLine = fileBuff.readLine();
			} catch (IOException e) {
				e.printStackTrace();
				Log.e(TAG,"Bookmark File read Error!");
				return false;
			}
			if( bufLine != null ){
				int i=0;
				for(  ; i < bookMarks.size() ; i++ ){
					if( bufLine.compareTo(bookMarks.get(i)) < 0 ){
						bookMarks.add( i, bufLine );
						break;
					}
				}
				if( i==bookMarks.size() ){
					bookMarks.add( bufLine );
				}
			}
		}
		try {
			fileBuff.close();
		} catch (IOException e) {
			e.printStackTrace();
			Log.e(TAG,"Bookmark File close!");
		}

		//ソートしたブックマークを書き込み。
		BufferedWriter outfileBuff = null;
		if( !markFile.canWrite() ){
			Log.e(TAG,"Bookmark File Can't writable!");
			return false;
		}
		try {
			outfileBuff = new BufferedWriter( new FileWriter(markFile));
		} catch (IOException e) {
			e.printStackTrace();
			Log.e(TAG,"Bookmark File write Error!");
		}
		
		for(int i=0; i < bookMarks.size() ; i++ ){
			try {
				outfileBuff.write(bookMarks.get(i));
				outfileBuff.newLine();
			} catch (IOException e1) {
				e1.printStackTrace();
				Log.e(TAG,"Bookmark File write Error!");
			}
		}
		
		try {
			outfileBuff.close();
		} catch (IOException e) {
			e.printStackTrace();
			Log.e(TAG,"Bookmark File close Error!");
		}
		return true;
	}

	/***
	 * bookmarkのファイルを削除する関数
	 * @return 削除に成功した時true,失敗した時false
	 */
	public static boolean delBookmarkFile( ) {
		File markFile = new File(bookmarkFilename);
		if( markFile.canRead() ){
			return markFile.delete();
		}
		return true;
	}


	/***
	 * bookmarkファイルの中から、指定したブックマークだけを削除する
	 * @param markingFile	ブックマークするファイルの絶対パス
	 * @param pageNum		ブックマークするページ番号
	 * @return				書き込みが正常終了すれば true 、失敗したら false
	 */
	public static boolean delBookmarkFile( String getdirName, String getfileName ) {
		String TAG="delBookmarkFile";
		File markFile = new File(bookmarkFilename);
		ArrayList <String> bookMarks = new ArrayList<String>();
		String markCheck = getdirName+"#"+getfileName;
		BufferedReader infileBuff = null;
		BufferedWriter outfileBuff = null;
		String fileBuf = "";
		boolean delFlag = false;

		if( !markFile.canRead() ){			//ブックマークファイルが無い場合、ファイルを作成して情報を書き込む
			Log.e(TAG,"Bookmark File Can't Read!");
			return false;
		}else{			//ブックマークファイルがある場合は、現在のブックマークを探して上書きする。
			try {
				infileBuff=new BufferedReader( new FileReader(markFile) );
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			while( fileBuf != null ){
				try {
					fileBuf=infileBuff.readLine();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				if( fileBuf != null )
					if( fileBuf.indexOf( markCheck ) >= 0 ){
						delFlag=true;
					}else{
						bookMarks.add(fileBuf);
					}
			}
			try {
				infileBuff.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if( !delFlag ){
				Log.e(TAG,"Bookmark is Not in Bookmarks File!");
				return false;
			}
			
			if( !markFile.canWrite() ){
				Log.e(TAG,"Bookmark File Can't writable!");
				return false;
			}
			try {
				outfileBuff = new BufferedWriter( new FileWriter(markFile));
			} catch (IOException e) {
				e.printStackTrace();
				Log.e(TAG,"Bookmark File write Error!");
			}
			
			for(int i=0; i < bookMarks.size() ; i++ ){
				try {
					outfileBuff.write(bookMarks.get(i));
					outfileBuff.newLine();
				} catch (IOException e1) {
					e1.printStackTrace();
					Log.e(TAG,"Bookmark File write Error!");
				}
			}
			
			try {
				outfileBuff.close();
			} catch (IOException e) {
				e.printStackTrace();
				Log.e(TAG,"Bookmark File close Error!");
			}
		}
		return true;
	}


}
