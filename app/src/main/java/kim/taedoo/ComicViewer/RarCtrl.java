package kim.taedoo.ComicViewer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.innosystec.unrar.Archive;
import de.innosystec.unrar.exception.RarException;
import de.innosystec.unrar.rarfile.FileHeader;

/**
 * Rarファイル関連の処理をするためのクラス
 * @author Kim
 * @since  2014.05.22
 *
 */
public class RarCtrl {
	
	public			List<String>		raredFileList=null;	//rarFile内のファイルリストを保持する
	private			BufferedInputStream rarF = null;	//Rarファイルのインプットバッファ

	private 		Archive				rarFile = null;	//Rarファイル
	private			FileHeader 			rarFh[] = null; //Rarファイルのヘッダ情報
	private			String				tempDir = null;	//Cacheファイルのパス

	
	public RarCtrl(String fileName, String tempPath) throws IOException,RarException{
		rarFile = new Archive(new File(fileName));
		tempDir = tempPath + "/";
	}
	
	public void rarFileClose() throws IOException{
		if( rarF != null ){
			rarF.close();
			rarF=null;
		}
		if( rarFile != null ){
			rarFile.close();
			rarFile=null;
		}
		File cacheD = new File(tempDir);
		for( File f : cacheD.listFiles() ){
			if( f.canRead() && f.canWrite() ){
				f.delete();
			}
		}
		raredFileList=null;
	}
	
	/***
	 * コンストラクタで指定されたRarFile内に保存されているファイルのリストを、
	 *  public static	List<String> raredFileList  に設定する。
	 * @return 保存されていたファイルの数を返す。
	 */
	public int setInrarFileList(){
		int fileNum=0;
        rarFh=new FileHeader[rarFile.getFileHeaders().size()];
        rarFile.getFileHeaders().toArray(rarFh);
        
        String fileName="";

		raredFileList=new ArrayList<String>();
		for( int fileC=0; fileC<rarFh.length ;fileC++ ){
			
//****			日本語対応の為、getFileNameStringかgetFileNameWかを事前に判定	2015/03/20
			if( rarFh[fileC].getFileNameW()!= ""){
				fileName=rarFh[fileC].getFileNameW();
			}else{
				fileName=rarFh[fileC].getFileNameString();
			}
			
			//以後、ファイル名はfileNameを使って判定する。

			if(rarFh[fileC].isDirectory()){
				continue;
			}else{
				// とりあえず、rarファイル内に保存されているファイルのうち、jpgとpng,gif以外は無視する
/**
				if(	!rarFh[fileC].getFileNameString().toLowerCase(Locale.getDefault()).endsWith(".jpg")
						&& !rarFh[fileC].getFileNameString().toLowerCase(Locale.getDefault()).endsWith(".png")
						&& !rarFh[fileC].getFileNameString().toLowerCase(Locale.getDefault()).endsWith(".gif")
						)
**/
				if(	!fileName.toLowerCase(Locale.getDefault()).endsWith(".jpg")
						&& !fileName.toLowerCase(Locale.getDefault()).endsWith(".png")
						&& !fileName.toLowerCase(Locale.getDefault()).endsWith(".gif")
						)
					continue;
				int i=0;
				for( ; i < raredFileList.size() ; i++ ){
//					if( rarFh[fileC].getFileNameString().compareTo(raredFileList.get(i)) < 0 ){
//						raredFileList.add(i,rarFh[fileC].getFileNameString());
					if( fileName.compareTo(raredFileList.get(i)) < 0 ){
						raredFileList.add(i,fileName);
						fileNum++;
						break;
					}
				}
				if( i==raredFileList.size() ){
//					raredFileList.add(rarFh[fileC].getFileNameString());
					raredFileList.add(fileName);
					fileNum++;
				}
			}
		}
		return fileNum;
	}

	/***
	 * getFileで指定された、rarFile内に保存されているファイルのストリームを
	 *  public static BufferedInputStream rarF にセットする。
	 * @param getFile 取得したいファイル名
	 * @throws IOException
	 */
	public BufferedInputStream getInrarFileStream(String getFile) throws IOException,RarException{
		for(int i=0; i<rarFh.length;i++)
		{
			String fileName="";
			if(	rarFh[i].isDirectory())
				continue;
			//****			日本語対応の為、getFileNameStringかgetFileNameWかを事前に判定	2015/03/20
			if( rarFh[i].getFileNameW()!= ""){
				fileName=rarFh[i].getFileNameW();
			}else{
				fileName=rarFh[i].getFileNameString();
			}
			
//			if(	rarFh[i].getFileNameString().equalsIgnoreCase(getFile) ){	//ファイル名が一致したらバッファを返す
			if(	fileName.equalsIgnoreCase(getFile) ){	//ファイル名が一致したらバッファを返す
				//RARファイルの処理では、まず、RARからFileを取得したうえで、
				//新たに生成したインプットストリームにバッファを渡す。
//				String fName =tempDir + rarFh[i].getFileNameString().trim();
				String fName =tempDir + fileName.trim();
				FileOutputStream fout = new FileOutputStream(new File(fName));
				rarFile.extractFile(rarFh[i], fout);
				fout.close();
				
				rarF = new BufferedInputStream(new FileInputStream(new File(fName)));
				
				break;
			}
		}
		return rarF;
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
		if( raredFileList==null )
			return null;
		else if( raredFileList.size() < position )
			return raredFileList.get(raredFileList.size()-1);
		else if( position < 0 )
			return raredFileList.get(0);
		return raredFileList.get(position);
	}
	
}
