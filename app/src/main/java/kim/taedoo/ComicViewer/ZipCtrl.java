package kim.taedoo.ComicViewer;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;

/**
 * Zipファイル関連の処理をするためのクラス
 * @author Kim
 * @since  2013.04.26
 *
 */
public class ZipCtrl {
	
	private			List<String>		zipedFileList=null;	//zipFile内のファイルリストを保持する
	private			BufferedInputStream zipF = null;	//Zipファイルのインプットバッファ

	private 		ZipFile				zipFile = null;	//Zipファイル
	private			Enumeration<?> 		enume = null;	//Zipファイルの中身を読むための Enumeration

	
	public ZipCtrl(String fileName) throws IOException{
		zipFile = new ZipFile(fileName,"MS932");
	}
	
	public void zipFileClose() throws IOException{
		if( zipF != null ){
			zipF.close();
			zipF=null;
		}
		if( zipFile != null ){
			zipFile.close();
			zipFile=null;
		}
		zipedFileList=null;
		enume=null;
	}
	
	/***
	 * コンストラクタで指定されたZipFile内に保存されているファイルのリストを、
	 *  public static	List<String> zipedFileList  に設定する。
	 * @return 保存されていたファイルの数を返す。
	 */
	public int setInzipFileList(){
		int fileNum=0;
		zipedFileList=new ArrayList<String>();
		enume = zipFile.getEntries();
		while( enume.hasMoreElements() )
		{
			ZipEntry	zipEntry =	(ZipEntry)enume.nextElement();
			if(zipEntry.isDirectory())
				continue;
			else{
				// とりあえず、zipファイル内に保存されているファイルのうち、jpgとpng以外は無視する
				if(	!zipEntry.getName().toLowerCase(Locale.getDefault()).endsWith(".jpg")
						&& !zipEntry.getName().toLowerCase(Locale.getDefault()).endsWith(".png")
						&& !zipEntry.getName().toLowerCase(Locale.getDefault()).endsWith(".gif")
						)
					continue;
				int i=0;
				for( ; i < zipedFileList.size() ; i++ ){
					if( zipEntry.getName().compareTo(zipedFileList.get(i)) < 0 ){
						zipedFileList.add(i,zipEntry.getName());
						fileNum++;
						break;
					}
				}
				if( i==zipedFileList.size() ){
					zipedFileList.add(zipEntry.getName());
					fileNum++;
				}
			}
		}
		return fileNum;
	}

	/***
	 * getFileで指定された、zipFile内に保存されているファイルのストリームを
	 *  public static BufferedInputStream zipF にセットする。
	 * @param getFile 取得したいファイル名
	 * @throws IOException
	 */
	public BufferedInputStream getInzipFileStream(String getFile) throws IOException{
		enume = zipFile.getEntries();
		while( enume.hasMoreElements() )
		{
			ZipEntry	zipEntry =	(ZipEntry)enume.nextElement();
			if(zipEntry.isDirectory())
				continue;
			if( zipEntry.getName().equalsIgnoreCase(getFile) ){	//ファイル名が一致したらバッファを返す
				zipF = new BufferedInputStream(zipFile.getInputStream(zipEntry));
				break;
			}
		}
		return zipF;
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
		if( zipedFileList==null )
			return null;
		else if( zipedFileList.size() < position )
			return zipedFileList.get(zipedFileList.size()-1);
		else if( position < 0 )
			return zipedFileList.get(0);
		return zipedFileList.get(position);
	}
	
}
