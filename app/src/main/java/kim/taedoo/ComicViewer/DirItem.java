package kim.taedoo.ComicViewer;

import android.graphics.Bitmap;

public class DirItem {
	public Bitmap fIcon;
	public String fName;
	public String fInfo;

	/***
	 *  DirItemに変数をセットする関数
	 * @param icon
	 * @param fileName
	 * @param info
	 */
	public void setDirItem( Bitmap icon, String fileName, String info ){
		fIcon = icon;
		fName = fileName;
		fInfo = info;
	}
}
