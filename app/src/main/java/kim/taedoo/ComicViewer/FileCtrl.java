package kim.taedoo.ComicViewer;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import de.innosystec.unrar.exception.RarException;

/**
 * ファイル種別ごとに処理を振り分けるクラス
 * @author Kim
 * @since  2014.05.23
 *
 */
public class FileCtrl {
	
	public			List<String>		zipedFileList=null;	//zipFile内のファイルリストを保持する

	private	static	int					kind = 0;	//ファイルの種別を保持する
	private	static	final	int			FILE_ZIP = 1;
	private	static	final	int			FILE_RAR = 2;

	private			ZipCtrl				zipC = null;	//ZipFile コントロール用のクラス
	private			RarCtrl				rarC = null;	//RarFile コントロール用のクラス

	public FileCtrl(String fileName, String tempDir) throws IOException,RarException{
		if( fileName.toLowerCase(Locale.getDefault()).endsWith(".zip")
				|| fileName.toLowerCase(Locale.getDefault()).endsWith(".cbz") ){
			zipC = new ZipCtrl(fileName);
			kind = FILE_ZIP;
		}else if(fileName.toLowerCase(Locale.getDefault()).endsWith(".rar")
				|| fileName.toLowerCase(Locale.getDefault()).endsWith(".cbr") ){
			rarC = new RarCtrl(fileName, tempDir);
			kind = FILE_RAR;
		}
	}
	
	public void allFileClose() throws IOException,RarException{
		switch(kind){
		case FILE_ZIP:
			zipC.zipFileClose();
			break;
		case FILE_RAR:
			rarC.rarFileClose();
			break;
		}
	}
	
	/***
	 * コンストラクタで指定されたFile内に保存されているファイルのリストを、
	 *  public static	List<String> zipedFileList  に設定する。
	 * @return 保存されていたファイルの数を返す。
	 */
	public int setInFileList(){
		switch(kind){
		case FILE_ZIP:
			return zipC.setInzipFileList();
		case FILE_RAR:
			return rarC.setInrarFileList();
		}
		return 0;
	}

	/***
	 * getFileで指定された、File内に保存されているファイルのストリームを
	 *  返す
	 * @param getFile 取得したいファイル名
	 * @throws IOException
	 */
	public BufferedInputStream getInFileStream(String getFile) throws IOException,RarException{
		switch(kind){
		case FILE_ZIP:
			return zipC.getInzipFileStream(getFile);
		case FILE_RAR:
			return rarC.getInrarFileStream(getFile);
		}
		return null;
	}
	
	/***
	 * public static List<String> zipedFileList に記録されている
	 * Zipファイル内のファイル名リストから、positionの位置にセットされている
	 * ファイル名を返す。
	 * @param position	取得したいファイル名の位置
	 * @return 指定位置のファイル名。
	 * 但し、zipedFileListが作成されていない場合はnull、positionがファイル数を超えている場合は最終要素
	 * positionが0未満の場合は最初の要素を返す。
	 */
	public String getPositionFileName(int position){
		switch(kind){
		case FILE_ZIP:
			return zipC.getPositionFileName(position);
		case FILE_RAR:
			return rarC.getPositionFileName(position);
		}
		return "";
	}
	
}
