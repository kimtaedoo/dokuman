package kim.taedoo.ComicViewer;

//import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import de.innosystec.unrar.exception.RarException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ViewSwitcher.ViewFactory;

public class ComicImageActivity extends Activity implements ViewFactory,OnTouchListener {
	
	private	final	String				TAG;
	private 		String				openFileName = null;	//これから開くファイル名
	private 		String				openDirName = null;		//今開いているディレクトリ名
	private 		ArrayList<String>	fileNameLists = null;	//現在ディレクトリ内のファイル名
	private 		int					filePosition = 0;		//現在ディレクトリの中で、開くファイルが何番目にあるか
	private 		int					pagePosition = 0;		//これから開くファイルの何ページ目を開くか
	private			FileCtrl			fileC = null;	//File コントロール用のクラス
	private			ImageSwitcher imgSwitcher = null;	//画像表示View
	private 		Bitmap				images = null;	//画像イメージ
	private			Bitmap				scaleimage = null;	//拡大縮小時のイメージ
	private			int					maxFileCount = 0;	//ZipFile内に保存されている画像枚数を保持
	private			Toast 				t = null;
	private	final	int					TOAST_OFFSET = 50;

	private			int					rightArea = 0;		//ページめくりの境界位置右側
	private			int					leftArea = 0;		//ページめくりの境界位置左側
	private			int					dispX = 0;
	private			int					dispY = 0;
	private	final	int					MAX_IMG_SIZE = 2048;
	private			Rect				dispRect = null;	//画面サイズ
	
	private	static	long				eventTime = 0;		//前回イベントの時間保持
	private			Timer				slideTimer = null;	//イベントのループ処理
					long				slideTime;
	private	static	boolean				slideFlag = false;	//スライドショーを実行中か否か
	private	final	int					NEXT = 1;		//autoSlideの引数。次ののページへ
	private	final	int					BACK = -1;		//autoSlideの引数。前ののページへ
	private	final	long				EVENT_TIME = 600;		//イベント関連で、次の処理を実行するまで待つ時間
	private	final	int					EVENT_SLIDE_MINIMUM = 400;		//最速スライド時間
	private	final	int					EVENT_TIME_MINIMUM = 150;		//イベントが重複しないようにチェックする時間
	private			int					tapRight;			//右をタップした時のページ遷移方向
	private			int					tapLeft;			//左をタップした時のページ遷移方向
	private			int					sideRight;			//見開きの時、右側のページ方向
	private			int					sideLeft;			//見開きの時、左側のページ方向
	private			Timer				markTimer = null;	//長押しブックマーク時の処理
	private			boolean				filechangeFlag = false;	//ファイル変更中フラグ
	
	private			boolean				spreadFlag = false;	//見開き表示の場合true
	private			boolean				spreadOneFlag = false;	//見開きの時、1ページだけずらすフラグ

	private			ProgressDialog 		progressD = null;
	
	public ComicImageActivity() {
		TAG="ComicImageActivity";
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if( t==null ){
			t = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT);
			t.setGravity(Gravity.CENTER | Gravity.BOTTOM , 0, TOAST_OFFSET );
/****ディスプレイサイズの取得を関数化	2013.11.21
			// ウィンドウマネージャのインスタンス取得
			WindowManager wm = (WindowManager)getSystemService(WINDOW_SERVICE);
			// ディスプレイのインスタンス生成
			Display disp = wm.getDefaultDisplay();
			Point dispSize = new Point();
			disp.getSize(dispSize);
			//画像の描画サイズは、ディスプレイサイズまたは2048の小さい方
			dispX=Math.min(dispSize.x , MAX_IMG_SIZE);
			dispY=Math.min(dispSize.y , MAX_IMG_SIZE);
***/
			setDispsize();
			dispRect = new Rect(0, 0, dispX , dispY);
			if( dispX > dispY && ComicViewerActivity.spread )
				spreadFlag = true;
			//タップ位置を取得する境界線を設定
			rightArea = (dispX*2)/3;
			leftArea = (dispX*1)/3;

			SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
			String prefstr = pref.getString(
					getString( R.string.pre_key_slidetime ),
					String.valueOf(getResources().getInteger(R.integer.default_slide_time )));
			slideTime = Long.parseLong(prefstr);

			Boolean tapNext = pref.getBoolean( getString(R.string.pre_key_nextpage), false);
			if( tapNext ){	//trueは右側タップで次のページへ
				tapRight=NEXT;
				tapLeft=BACK;
			}else{
				tapRight=BACK;
				tapLeft=NEXT;
			}
			
			progressD = new ProgressDialog(ComicImageActivity.this);
			progressD.setTitle(R.string.dialog_title_filechange);
			progressD.setMessage(getResources().getString(R.string.dialog_msg_filechange));
			progressD.setIndeterminate(true);
			progressD.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			progressD.setCancelable(false);
			progressD.setMax(30);

			//Intent情報のゲット
			/***
			 * 引数の書式
			 * openFileName = ディレクトリパス＋ファイルパス（ファイルの絶対パス）
			 * openDirName = ディレクトリパス
			 * fileNameLists = 同じディレクトリ内のファイル名リスト
			 * filePosition = 現在開いているファイルの番号
			 * pagePosition = 開くファイルのページ番号
			 */
			Intent	data=getIntent();
			openFileName = data.getStringExtra(getString(R.string.intent_open_filename));
			openDirName =  data.getStringExtra(getString(R.string.intent_open_dirname));
			fileNameLists = data.getStringArrayListExtra(getString(R.string.intent_file_lists));
			filePosition = data.getIntExtra(getString(R.string.intent_file_position),0);
			pagePosition = data.getIntExtra(getString(R.string.intent_page_position),0);
		}
		
		try {
			fileC = new FileCtrl(openFileName, getExternalCacheDir().getAbsolutePath());
			maxFileCount = fileC.setInFileList();
			if( maxFileCount == 0 ){		//zipファイル内にjpgまたはPNGが1枚もなかったらメッセージを出して処理を終了
				t.setDuration(Toast.LENGTH_SHORT);
				t.setText( getResources().getString(R.string.err_msg_not_image) );
				t.show();
			}
		} catch (IOException e) {
			e.printStackTrace();
			Log.e(TAG, getResources().getString(R.string.err_msg_file_failed));
			Toast.makeText(
					getApplicationContext(),
					this.getResources().getString(R.string.err_msg_file_failed),
					Toast.LENGTH_LONG).show();
			finish();
			return;
		} catch (RarException e){
			e.printStackTrace();
			Log.e(TAG, getResources().getString(R.string.err_msg_file_failed));
			Toast.makeText(
					getApplicationContext(),
					this.getResources().getString(R.string.err_msg_file_failed),
					Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		setInitImages();
		
		requestWindowFeature(Window.FEATURE_CONTEXT_MENU);
		setTitle(openFileName);
		setContentView(R.layout.cv_view_activity);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if( imgSwitcher == null ){
			imgSwitcher = (ImageSwitcher)findViewById(R.id.imageSwitcher);
			imgSwitcher.setFactory(this);
		}
		imgSwitcher.setImageDrawable(
				new BitmapDrawable(getResources(), images)
				);
		
		imgSwitcher.setInAnimation(
				AnimationUtils.loadAnimation(this, R.anim.fade_in));
		imgSwitcher.setOutAnimation(
				AnimationUtils.loadAnimation(this, R.anim.fade_out));

		getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
	    super.onConfigurationChanged(newConfig);
/**ディスプレイサイズの取得方法を変更	2013.11.21
	    int disp1 = dispX;
	    int disp2 = dispY;
**/

		setDispsize();
	    //新しい表示が横画面の場合
	    if( newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE ){
//	    	dispX = Math.max(disp1, disp2);
//			dispY = Math.min(disp1, disp2);
	    	rightArea = (dispX*2)/3;
			leftArea = (dispX*1)/3;
			if( ComicViewerActivity.spread )
				spreadFlag = true;
	    }else{
//	    	dispX = Math.min(disp1, disp2);
//			dispY = Math.max(disp1, disp2);
	    	rightArea = (dispX*2)/3;
			leftArea = (dispX*1)/3;
			spreadFlag = false;
	    }
	    
		setImagesBitmap();
		imgSwitcher.setImageDrawable(
				new BitmapDrawable(getResources(), images)
				);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		slideCancel();
		markCancel();
		try {
			Thread.sleep(EVENT_TIME);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		if( fileC != null){
			try {
				fileC.allFileClose();
				if( images != null ){
					images=null;
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (RarException e) {
				e.printStackTrace();
			}
		}
		
		if( ComicViewerActivity.autoMark ){
			BookmarkCtl.writeBookmarkList( openFileName, pagePosition );
		}
		
		String resumeStr = openDirName + "#" + fileNameLists.get(filePosition) + "#" + pagePosition;
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor prefEdit = pref.edit();
		prefEdit.putString(
				getString( R.string.pre_key_resume ), resumeStr);
		prefEdit.commit();
		
	};
	
	@Override
	public View makeView() {
        ImageView i = new ImageView(this);
        i.setBackgroundColor(0xFF000000);
        i.setScaleType(ImageView.ScaleType.FIT_CENTER);
        i.setLayoutParams(new ImageSwitcher.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        i.setOnTouchListener(this);
        return i;
	}
	
	/***
	 * ZIPのバッファからBitmapを生成してimagesにセットする
	 */
	private void setImagesBitmap(){
		if( maxFileCount != 0 ){
			// Portrait(縦位置)
			BitmapFactory.Options options = new BitmapFactory.Options();

			/**  ここから画像のサイズ変更処理  **/
			options.inJustDecodeBounds = true;
			try {
				BitmapFactory.decodeStream(
						fileC.getInFileStream(fileC.getPositionFileName(pagePosition)),
						dispRect,
						options
						);
			} catch (IOException e) {
				e.printStackTrace();
				Log.e(TAG, getResources().getString(R.string.err_msg_file_failed));
				Toast.makeText(
						getApplicationContext(),
						getResources().getString(R.string.err_msg_file_failed),
						Toast.LENGTH_LONG).show();
			} catch (RarException e) {
				e.printStackTrace();
				Log.e(TAG, getResources().getString(R.string.err_msg_file_failed));
				Toast.makeText(
						getApplicationContext(),
						getResources().getString(R.string.err_msg_file_failed),
						Toast.LENGTH_LONG).show();
			}

			if( options.outHeight > options.outWidth && dispX > dispY && ComicViewerActivity.spread)
				spreadFlag = true;
			else
				spreadFlag = false;
			
			if( spreadFlag
					&& pagePosition <= maxFileCount-2 ) {
				// 見開きの場合の処理
/***
 * 2枚目の画像が横長画像の場合は見開きを辞める処理を入れるなら、ここで処理する。
				int imgR_width=options.outWidth;
				int imgL_width=0;
				int img_height=options.outHeight;
				
				try {
					//2枚目の画像の横幅取得
					BitmapFactory.decodeStream(
							zipC.getInzipFileStream(zipC.getPositionFileName(pagePosition+1)),
							dispRect,
							options
							);
					imgL_width = options.outWidth;
				} catch (IOException e) {
					e.printStackTrace();
					Log.e(TAG, getResources().getString(R.string.err_msg_file_failed));
					Toast.makeText(
							getApplicationContext(),
							getResources().getString(R.string.err_msg_file_failed),
							Toast.LENGTH_LONG).show();
				}
*/
				
				options.inJustDecodeBounds = false;
				//options=null;	削除 2014.05.29
				//画像サイズが大きい3MBときは小さくして取得	2014.05.29
				options.inSampleSize = options.outHeight*options.outWidth >= 3145728 ? 2 : 1;
				/** ここまで画像のサイズ変更処理 **/
				//画像合成用に2枚のBitmapを取得
				Bitmap imageL = null;
				Bitmap imageR = null;
				try {
					if( ComicViewerActivity.direction ){
						sideRight = BACK;
						sideLeft = NEXT;
						imageR = BitmapFactory.decodeStream(
								fileC.getInFileStream(fileC.getPositionFileName(pagePosition)),
								dispRect,
								options
								);
						imageL = BitmapFactory.decodeStream(
								fileC.getInFileStream(fileC.getPositionFileName(pagePosition+1)),
								dispRect,
								options
								);
					}else{
						sideRight = NEXT;
						sideLeft = BACK;
						imageR = BitmapFactory.decodeStream(
								fileC.getInFileStream(fileC.getPositionFileName(pagePosition+1)),
								dispRect,
								options
								);
						imageL = BitmapFactory.decodeStream(
								fileC.getInFileStream(fileC.getPositionFileName(pagePosition)),
								dispRect,
								options
								);
					}
				} catch (IOException e) {
					e.printStackTrace();
					Log.e(TAG, getResources().getString(R.string.err_msg_file_failed));
					Toast.makeText(
							getApplicationContext(),
							getResources().getString(R.string.err_msg_file_failed),
							Toast.LENGTH_LONG).show();
					return;
				} catch (RarException e) {
					e.printStackTrace();
					Log.e(TAG, getResources().getString(R.string.err_msg_file_failed));
					Toast.makeText(
							getApplicationContext(),
							getResources().getString(R.string.err_msg_file_failed),
							Toast.LENGTH_LONG).show();
					return;
				}
				int Rw = imageR.getWidth();
				int Rh = imageR.getHeight();
				int Lw = imageL.getWidth();
				int Lh = imageL.getHeight();
				/***** 結合・描画処理を変更。2013.11.20
				//imagesに対して右画像＋左画像の幅と大きいほうの高さを持ったBitmapを作成しCanvasを設定
				images = Bitmap.createBitmap(Lw + Rw, Math.max(Lh,Rh) , Bitmap.Config.ARGB_8888);
				Canvas canvas = new Canvas(images);
				canvas.drawBitmap(imageR, (float)Lw, 0, (Paint)null); // 画像合成
				canvas.drawBitmap(imageL, 0, 0, (Paint)null); // 画像合成
				
//				if( images.getWidth() > MAX_IMG_SIZE || images.getHeight() > MAX_IMG_SIZE ){
				if( images.getWidth() > dispX || images.getHeight() > dispY ){
					int imagesX = images.getWidth();
					int imagesY = images.getHeight();
				    // 最終的なサイズにするための縮小率を求める
					float scale_min =
							Math.min((float)dispX/imagesX , (float)dispY/imagesY);
//							Math.min((float)MAX_IMG_SIZE/imagesX , (float)MAX_IMG_SIZE/imagesY);
					Matrix matrix = new Matrix();
				    // 画像変形用のオブジェクトに拡大・縮小率をセットし
					matrix.postScale(scale_min, scale_min);
				    // 取得した画像を元にして変形画像を生成・再設定
					images = Bitmap.createBitmap(images, 0, 0, imagesX, imagesY, matrix, true);
				}
				*****/
				//背景Bitmapを、ディスプレイと同じ比率の、画像サイズで作成
				int imagesX = Rw+Lw;
				int imagesY = Math.max(Rh,Lh);
				Bitmap bgImage = null;
				float bgScale = Math.max((float)imagesX/dispX , (float)imagesY/dispY);
				bgImage = Bitmap.createBitmap((int)(dispX*bgScale), (int)(dispY*bgScale), Bitmap.Config.ARGB_8888);
				//Canvasをセットし、中央に2枚の画像を描画
				Canvas canvas = new Canvas(bgImage);
				canvas.drawBitmap(imageR, (float)((bgImage.getWidth()-imagesX)/2+Lw), 0, (Paint)null); // 画像合成
				canvas.drawBitmap(imageL, (float)((bgImage.getWidth()-imagesX)/2), 0, (Paint)null); // 画像合成

			    // 最終的なサイズにするための縮小率を求める
				float scale_min =
						Math.min((float)dispX/imagesX , (float)dispY/imagesY);
				Matrix matrix = new Matrix();
			    // 画像変形用のオブジェクトに拡大・縮小率をセットし
				matrix.postScale(scale_min, scale_min);
				images = Bitmap.createBitmap(bgImage, 0, 0, bgImage.getWidth(), bgImage.getHeight(), matrix, true);

			}else {
				//1枚だけ表示するときの処理
				options.inJustDecodeBounds = false;
				//options=null;	削除	2014.05.29
				
				//画像サイズが大きい3MBときは小さくして取得	2014.05.29
				options.inSampleSize = options.outHeight*options.outWidth >= 3145728 ? 2 : 1;
				/*** 処理の変更テスト
				float scaleX = (float)options.outWidth/dispX;
				float scaleY = (float)options.outHeight/dispY;
				float scale = scaleX > scaleY ? scaleX : scaleY;
				options.inSampleSize = scale >= 2.0 ? 2 : (int)scale;
				try {
					images = BitmapFactory.decodeStream(
							fileC.getInFileStream(fileC.getPositionFileName(pagePosition)),
							dispRect,
							options
							);
				} catch (IOException e) {
					e.printStackTrace();
					Log.e(TAG, getResources().getString(R.string.err_msg_file_failed));
					Toast.makeText(
							getApplicationContext(),
							getResources().getString(R.string.err_msg_file_failed),
							Toast.LENGTH_LONG).show();
				} catch (RarException e) {
					e.printStackTrace();
					Log.e(TAG, getResources().getString(R.string.err_msg_file_failed));
					Toast.makeText(
							getApplicationContext(),
							getResources().getString(R.string.err_msg_file_failed),
							Toast.LENGTH_LONG).show();
				}
				Bitmap bgImage = null;
				//Canvasをセットし、中央に画像を描画
				bgImage = Bitmap.createBitmap((int)(dispX*scale), (int)(dispY*scale), Bitmap.Config.ARGB_8888);
				Canvas canvas = new Canvas(bgImage);
				canvas.drawBitmap(images, 0, (float)((bgImage.getHeight()-images.getHeight())/2), (Paint)null); // 画像合成
				ここまで、処理の変更テスト***/

				/** ここから画像のサイズ変更処理 **/
				/***一時退避 **/
				Bitmap singleImage = null;
				try {
					singleImage = BitmapFactory.decodeStream(
							fileC.getInFileStream(fileC.getPositionFileName(pagePosition)),
							dispRect,
							options
							);
				} catch (IOException e) {
					e.printStackTrace();
					Log.e(TAG, getResources().getString(R.string.err_msg_file_failed));
					Toast.makeText(
							getApplicationContext(),
							getResources().getString(R.string.err_msg_file_failed),
							Toast.LENGTH_LONG).show();
					return;
				} catch (RarException e) {
					e.printStackTrace();
					Log.e(TAG, getResources().getString(R.string.err_msg_file_failed));
					Toast.makeText(
							getApplicationContext(),
							getResources().getString(R.string.err_msg_file_failed),
							Toast.LENGTH_LONG).show();
					return;
				}

				//1枚だけ表示するときも、全画面使用できるように、ディスプレイ比率と同じBitmapを生成
				Bitmap bgImage = null;
				float scaleX = (float)singleImage.getWidth()/dispX;
				float scaleY = (float)singleImage.getHeight()/dispY;
				//Canvasをセットし、中央に画像を描画
				if( scaleX > scaleY ){
					bgImage = Bitmap.createBitmap((int)(dispX*scaleX), (int)(dispY*scaleX), Bitmap.Config.ARGB_8888);
					Canvas canvas = new Canvas(bgImage);
					canvas.drawBitmap(singleImage, 0, (float)((bgImage.getHeight()-singleImage.getHeight())/2), (Paint)null); // 画像合成
				}else{
					bgImage = Bitmap.createBitmap((int)(dispX*scaleY), (int)(dispY*scaleY), Bitmap.Config.ARGB_8888);
					Canvas canvas = new Canvas(bgImage);
					canvas.drawBitmap(singleImage, (float)((bgImage.getWidth()-singleImage.getWidth())/2), 0, (Paint)null); // 画像合成
				}

				// 最終的なサイズにするための縮小率を求める
				int w = bgImage.getWidth();
				int h = bgImage.getHeight();
				float scale_min = Math.min((float)dispX/w, (float)dispY/h);
				// 画像変形用のオブジェクトに拡大・縮小率をセットし
				Matrix matrix = new Matrix();
				matrix.postScale(scale_min, scale_min);
				// 取得した画像を元にして変形画像を生成・再設定
//				images = Bitmap.createBitmap(bgImage, 0, 0, w, h, matrix, true);
				images = Bitmap.createBitmap(bgImage, 0, 0, bgImage.getWidth(), bgImage.getHeight(), matrix, true);
				/**/
			}
		}else{
			images = Bitmap.createBitmap(dispX, dispY, Bitmap.Config.RGB_565); 
		}

		//拡大縮小用の領域に保存
		scaleimage = Bitmap.createBitmap(images);
		scaleChangeFlag = false;
		sScale = 1.0f;

	}
	
	/***
	 * ImageViewに画像をセットする関数
	 * @param zipedFileList
	 * @param pagePosition
	 */
	private void setInitImages() {
		
		//基本的にありえないが、指定されたページ番号がファイルのページ数を超えている場合は、最終ページにセット
		if( maxFileCount == 0 ){
			pagePosition = 0;
		}else if( pagePosition >= maxFileCount ){
			pagePosition = maxFileCount-1;
		}else if( pagePosition < 0 ){
			pagePosition = 0;
		}

		setImagesBitmap();
	}


	/**
	 * 画像を差し替える関数
	 * @param move	次のページは1、前のページは-1を指定
	 */
	void setChangeImages(int move){

		pagePosition += move;
		
		//見開きの時は、2倍動かす
		if( spreadFlag && pagePosition <= maxFileCount-1 && pagePosition!=0 && spreadOneFlag!=true )
			pagePosition += move;
		
		if( maxFileCount == 0){
			pagePosition = 0;
		}else if( pagePosition >= maxFileCount){
			pagePosition = 0;
		}else if( pagePosition < 0){
			pagePosition = maxFileCount-1;
		}
		// 画像の中身をセットする
		setImagesBitmap();

		//これを有効にすると、ハードウェアアクセラレータをオフにして、フルサイズ画像が表示できる代わりに動きが悪くなる。
//		imgSwitcher.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

		imgSwitcher.setImageDrawable(
				new BitmapDrawable(getResources(), images)
				);
	}

	/***
	 * 画像を連続的にスライドさせる関数
	 * @param lr	ページを移動させる方向。次のページに進む場合は1、戻る場合は-1
	 * @param msec	スライドショーの間隔（ミリ秒指定）
	 * @param inAnim	アニメーションID in
	 * @param outAnim	アニメーションID out
	 */
	private void autoSlide(final int lr,long msec, int inAnim, int outAnim){
		slideTimer = new Timer();
		imgSwitcher.setInAnimation(
				AnimationUtils.loadAnimation(this, inAnim));
		imgSwitcher.setOutAnimation(
				AnimationUtils.loadAnimation(this, outAnim));
		slideTimer.scheduleAtFixedRate(new TimerTask(){
			private Handler handler = new Handler();

			@Override
			public void run() {
				handler.post(new Runnable() {
					
					@Override
					public void run() {
						setChangeImages(lr);
					}
				});
			}
		}, EVENT_TIME_MINIMUM, msec);
	}

	/****
	 * 画像の縮小など、タッチ関連処理
	 */
	static float posY = 0;
	static float posX = 0;
	static float multi1X = 0;
	static float multi1Y = 0;
	static float multi2X = 0;
	static float multi2Y = 0;
	static float multiScale = 0;
	private			boolean				scaleChangeFlag = false;	//スケールが変更されている間true
	private			boolean				schangeNowFlag = false;		//スケール変更操作実行中のみtrue
	private			boolean				pchangeNowFlag = false;		//画像移動操作実行中のみtrue
	private			float				sScale = 0.0f;		//表示中の画像スケール
	private			Matrix				imgMatrix = null;
	private			Canvas				scaleCanvas = null;

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		//returnにfalseを設定すると、その後の継続したイベントが取得されなくなる。
		//MOVEなどを使用するため、継続的にイベントを取りたいのであれば、trueを返す。

		int action = event.getAction();
		int count = event.getPointerCount();

		switch( action & MotionEvent.ACTION_MASK ){		//イベントの動作種類による振り分け
		case MotionEvent.ACTION_UP:
			markCancel();
			if( event.getEventTime() - event.getDownTime() < EVENT_TIME
					&& !filechangeFlag && !pchangeNowFlag ){	//タップ時間が短い場合の処理＆MOVEの後ではない
				if( scaleChangeFlag ){		//スケール変更中は、初期値に戻す。
					sScale = 1.0f;
					changeImageScale(sScale);
					scaleChangeFlag = false;
				}else if( event.getEventTime() - eventTime < EVENT_TIME
						&& event.getHistorySize() == 0 
						&& !slideFlag
						&& ComicViewerActivity.slideShow ){	//中央のエリアをダブルタップ
					autoSlide( NEXT, slideTime, R.anim.fade_in, R.anim.fade_out);
					t.setDuration(Toast.LENGTH_LONG);
					t.setText(R.string.msg_slide_start);
					t.show();
					slideFlag = true;
					break;
				}else if( rightArea < event.getX() && event.getHistorySize() == 0 && !slideFlag){		//右のエリアをタップ
					imgSwitcher.setInAnimation(
							AnimationUtils.loadAnimation(this, R.anim.fade_in));
					imgSwitcher.setOutAnimation(
							AnimationUtils.loadAnimation(this, R.anim.fade_out));
					setChangeImages( tapRight );
					t.setDuration(Toast.LENGTH_SHORT);
					t.setText( String.valueOf(pagePosition+1) + "/" + String.valueOf(maxFileCount) );
					t.show();
					slideCancel();
					break;
				}else if( event.getX() < leftArea && event.getHistorySize() == 0  && !slideFlag ){	//左のエリアをタップ
					imgSwitcher.setInAnimation(
							AnimationUtils.loadAnimation(this, R.anim.fade_in));
					imgSwitcher.setOutAnimation(
							AnimationUtils.loadAnimation(this, R.anim.fade_out));
					setChangeImages( tapLeft );
					t.setDuration(Toast.LENGTH_SHORT);
					t.setText( String.valueOf(pagePosition+1) + "/" + String.valueOf(maxFileCount) );
					t.show();
					slideCancel();
					break;
				}
			}

			if( scaleChangeFlag ){		//スケール変更時のX,Y座標をmulti1にセット
				schangeNowFlag = false;
			}

			if( slideFlag ){
				t.setDuration(Toast.LENGTH_SHORT);
				t.setText( R.string.msg_slide_stop );
				t.show();
			}
			slideCancel();
			slideFlag = false;
			filechangeFlag = false;
			pchangeNowFlag = false;
			eventTime = event.getEventTime();
			break;

		case MotionEvent.ACTION_DOWN:		//普通にタップをしたとき
			posY = event.getY();
			posX = event.getX();
			handBookmark();					//手動ブックマークのタイマーをセット
			break;

		case MotionEvent.ACTION_POINTER_DOWN:			//2本目以降の指を置いた時
			if( count == 2){
				multi1Y = event.getY(0);
				multi1X = event.getX(0);
				multi2Y = event.getY(1);
				multi2X = event.getX(1);
				multiScale = (float)Math.sqrt(Math.pow( multi1Y - multi2Y , 2) + Math.pow( multi1X - multi2X , 2));
			}
			posY = event.getY(0);
			posX = event.getX(0);
			filechangeFlag = true;
			markCancel();
			break;
			
		case MotionEvent.ACTION_POINTER_UP:				//2本指を離したとき
			
			slideCancel();
			markCancel();
			progressD.dismiss();
			posX = event.getX() - posX;
			posY = event.getY() - posY;
			if( scaleChangeFlag ){
				return true;
			}
			if( posY > 150 ){
				changeFile( BACK, R.anim.slide_in_up, R.anim.slide_out_down );
				t.setDuration(Toast.LENGTH_SHORT);
				t.setText(String.format(getString(R.string.msg_change_file),fileNameLists.get(filePosition).toString()));
				t.show();
			}else if( posY < -150 ){
				changeFile( NEXT, R.anim.slide_in_down, R.anim.slide_out_up );
				t.setDuration(Toast.LENGTH_SHORT);
				t.setText(String.format(getString(R.string.msg_change_file),fileNameLists.get(filePosition).toString()));
				t.show();
			}else if( posX > 150 ){
				imgSwitcher.setInAnimation(
						AnimationUtils.loadAnimation(this, R.anim.slide_in_left));
				imgSwitcher.setOutAnimation(
						AnimationUtils.loadAnimation(this, R.anim.slide_out_right));
				spreadOneFlag=true;
				setChangeImages( sideLeft );
				spreadOneFlag=false;
				t.setDuration(Toast.LENGTH_SHORT);
				t.setText( String.valueOf(pagePosition+1) + "/" + String.valueOf(maxFileCount) );
				t.show();
			}else if( posX < -150 ){
				imgSwitcher.setInAnimation(
						AnimationUtils.loadAnimation(this, R.anim.slide_in_right));
				imgSwitcher.setOutAnimation(
						AnimationUtils.loadAnimation(this, R.anim.slide_out_left));
				spreadOneFlag=true;
				setChangeImages( sideRight );
				spreadOneFlag=false;
				t.setDuration(Toast.LENGTH_SHORT);
				t.setText( String.valueOf(pagePosition+1) + "/" + String.valueOf(maxFileCount) );
				t.show();
			}
			if( maxFileCount == 0 ){		//zipファイル内にjpgまたはPNGが1枚もなかったらメッセージを出す
				t.setDuration(Toast.LENGTH_SHORT);
				t.setText( getString(R.string.err_msg_not_image)+":"+fileNameLists.get(filePosition).toString() );
				t.show();
			}

			posY=0;
			posX=0;

			break;
			
		case MotionEvent.ACTION_OUTSIDE:
			slideCancel();
			markCancel();
			break;
			
		case MotionEvent.ACTION_MOVE:		//フリックした時
			if( count == 1 ){				//一本指のフリック
				if( scaleChangeFlag &&  !schangeNowFlag ){		//スケール変更中の1本指フリックは、画像の移動
					if(event.getHistorySize()!=0){
						if( !pchangeNowFlag			//指の移動距離が30以上の時だけ、画像の移動処理としてフラグを立てる
								&& Math.abs(event.getX() - posX) > 30
								&& Math.abs(event.getY() - posY) > 30
								)
							pchangeNowFlag = true;
						changeImageposition(
								event.getX() - event.getHistoricalX(0, 0)
								,event.getY() - event.getHistoricalY(0, 0)
								);
					}
				}else if( event.getHistorySize() != 0 ){
					float pos = event.getX() - event.getHistoricalX(event.getHistorySize()-1);
					if( pos > 10 ){
						markCancel();
						if( slideTimer == null ){
							autoSlide( tapRight, EVENT_SLIDE_MINIMUM,
								R.anim.slide_in_right, R.anim.slide_out_left);
							slideFlag = true;
							t.setDuration(Toast.LENGTH_LONG);
							t.setText(R.string.msg_put_off);
							t.show();
						}
					}else if( pos < -10 ){
						markCancel();
						if( slideTimer == null ){
							autoSlide( tapLeft, EVENT_SLIDE_MINIMUM,
								R.anim.slide_in_left, R.anim.slide_out_right);
							slideFlag = true;
							t.setDuration(Toast.LENGTH_LONG);
							t.setText(R.string.msg_put_off);
							t.show();
						}
					}
				}
			}else if( count == 2 ){			//２本指のフリック
				multi1X = event.getX(0);
				multi1Y = event.getY(0);
				multi2X = event.getX(1);
				multi2Y = event.getY(1);
				float checkScale = (float) Math.sqrt(Math.pow( multi1Y - multi2Y , 2) + Math.pow( multi1X - multi2X , 2));
				if( Math.abs(multiScale - checkScale) < 80 ){	//指の間隔が変化ない場合
					// 縦に動いている場合は、プログレスを表示する。
					if( Math.abs(posY-event.getY(0)) > 80)
						progressD.show();
				}else{
					scaleChangeFlag = true;
					schangeNowFlag = true;
					changeImageScale( checkScale / multiScale );
					eventTime = event.getEventTime();
				}
			}
			break;
		}

		return true;

	}

	/***
	 * 画像のスケールを変更する
	 * @param imageScale	画像の拡大・縮小率を指定	
	 */
	private void changeImageScale(float imageScale) {
		imgMatrix = new Matrix();
		imgMatrix.setScale(1/sScale, 1/sScale, (multi1X + multi2X)/2, (multi1Y + multi2Y)/2);
		//拡大・縮小率の調整
		if( imageScale > 1 ){
			sScale = (float) (sScale + 0.04f);
			if( sScale > 1.8 )
				sScale = 1.8f;
		}else if( imageScale < 1 ){
			sScale = (float)( sScale - 0.04f);
			if( sScale < 0.7 )
				sScale = 0.7f;
		}
		imgMatrix.setScale(sScale, sScale, (multi1X + multi2X)/2, (multi1Y + multi2Y)/2);
		
		//imagesにcanvasを設定して、スケールを変更する
		scaleCanvas = new Canvas(images);
		scaleCanvas.drawColor( Color.BLACK );
		scaleCanvas.concat(imgMatrix);
		scaleCanvas.drawBitmap(scaleimage, imgMatrix, null);
		imgSwitcher.invalidate();
	}
	
	/***
	 * スケール変更時に画像の表示位置を変更する
	 * @param imageScale	画像の拡大・縮小率を指定	
	 */
	private void changeImageposition( float X, float Y) {
		scaleCanvas.translate(X, Y);
		scaleCanvas.drawColor( Color.BLACK );
		scaleCanvas.drawBitmap(scaleimage, imgMatrix, null);
		imgSwitcher.invalidate();
	}

	/***
	 * zipファイルを変更する
	 * @param move	1で次のファイルへ、-1で前のファイルへ
	 */
	private void changeFile(int move, int inAnim, int outAnim ) {

		slideCancel();
		try {
			fileC.allFileClose();
			fileC=null;
		} catch (IOException e) {
			e.printStackTrace();
			Log.e(TAG, "zipFile close Error");
		} catch (RarException e) {
			e.printStackTrace();
			Log.e(TAG, "rarFile close Error");
		}
		
		if( ComicViewerActivity.autoMark )
			BookmarkCtl.writeBookmarkList(openFileName, pagePosition);
		
		try {
			filePosition += move;
			if( filePosition >= fileNameLists.size() ){
				filePosition=0;
			}else if( filePosition < 0 ){
				filePosition=fileNameLists.size()-1;
			}
			openFileName = openDirName + fileNameLists.get(filePosition);
			fileC = new FileCtrl(openFileName, getExternalCacheDir().getAbsolutePath());
			maxFileCount = fileC.setInFileList();
		} catch (IOException e) {
			e.printStackTrace();
			Log.e(TAG, getResources().getString(R.string.err_msg_file_failed));
			Toast.makeText(
					getApplicationContext(),
					this.getResources().getString(R.string.err_msg_file_failed),
					Toast.LENGTH_LONG).show();
			return;
		} catch (RarException e) {
			e.printStackTrace();
			Log.e(TAG, getResources().getString(R.string.err_msg_file_failed));
			Toast.makeText(
					getApplicationContext(),
					this.getResources().getString(R.string.err_msg_file_failed),
					Toast.LENGTH_LONG).show();
			return;
		}

		//新しいファイルのページ番号をセット
		if( ComicViewerActivity.autoMark ){
			pagePosition=BookmarkCtl.getMarkingPage(openDirName,fileNameLists.get(filePosition));
		}else{
			pagePosition=0;
		}
		setInitImages();
		imgSwitcher.setInAnimation(
				AnimationUtils.loadAnimation(this, inAnim));
		imgSwitcher.setOutAnimation(
				AnimationUtils.loadAnimation(this, outAnim));
		imgSwitcher.setImageDrawable(
				new BitmapDrawable(getResources(), images)
				);

		getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
	}

	/***
	 * 手動でブックマークするタイマー起動の関数
	 */
	private void handBookmark(){
		if( !ComicViewerActivity.autoMark ){
			markTimer = new Timer();
			markTimer.schedule(new TimerTask(){
				private Handler handler = new Handler();

				@Override
				public void run() {
					handler.post(new Runnable() {
						
						@Override
						public void run() {
							BookmarkCtl.writeBookmarkList(openFileName, pagePosition);
							t.setDuration(Toast.LENGTH_SHORT);
							t.setText( getResources().getString(R.string.msg_do_bookmarking) );
							t.show();
						}
					});
				}
			}, EVENT_TIME);
		}
	}
	
	/***
	 * handBookmark関数のタイマーをキャンセルする
	 */
	private void markCancel(){
		if( markTimer != null ){
			markTimer.purge();
			markTimer.cancel();
			markTimer=null;
		}
	}
	
	/***
	 * 自動スライドをキャンセルする
	 */
	private void slideCancel(){
		if( slideTimer != null ){
			slideTimer.purge();
			slideTimer.cancel();
			slideTimer=null;
		}
	}
	
	/***
	 * ディスプレイサイズを取得し、dispX,dispYにセットする
	 */
	private void setDispsize(){
		// ウィンドウマネージャのインスタンス取得
		WindowManager wm = (WindowManager)getSystemService(WINDOW_SERVICE);
		// ディスプレイのインスタンス生成
		Display disp = wm.getDefaultDisplay();
		Point dispSize = new Point();
		disp.getSize(dispSize);
		//画像の描画サイズは、ディスプレイサイズまたは2048の小さい方
		dispX=Math.min(dispSize.x , MAX_IMG_SIZE);
		dispY=Math.min(dispSize.y , MAX_IMG_SIZE);
	}
	
}
