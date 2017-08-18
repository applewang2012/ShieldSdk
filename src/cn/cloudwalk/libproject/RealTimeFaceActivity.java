/**
 * Project Name:cwFaceForDev3
 * File Name:LiveActivity.java
 * Package Name:cn.cloudwalk.dev.mobilebank
 * Date:2016-5-16上午9:17:24
 * Copyright @ 2010-2016 Cloudwalk Information Technology Co.Ltd All Rights Reserved.
 */

package cn.cloudwalk.libproject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.DisplayMetrics;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.camera.authenticationlibrary.Bulider;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import cn.cloudwalk.CloudwalkSDK;
import cn.cloudwalk.FaceInterface;
import cn.cloudwalk.FaceInterface.CW_LivenessCode;
import cn.cloudwalk.callback.FaceInfoCallback;
import cn.cloudwalk.jni.FaceInfo;
import cn.cloudwalk.libproject.camera.CameraPreview;
import cn.cloudwalk.libproject.progressHUD.CwProgressHUD;
import cn.cloudwalk.libproject.util.CameraUtil;
import cn.cloudwalk.libproject.util.DisplayUtil;
import cn.cloudwalk.libproject.util.LogUtils;
import cn.cloudwalk.libproject.util.NullUtils;
import cn.cloudwalk.libproject.util.Util;

/**
 * ClassName: RealTimeFaceActivity <br/>
 * Description: 实时人脸<br/>
 * date: 2016-5-16 上午9:17:24 <br/>
 *
 * @author 284891377
 * @since JDK 1.7
 */
public class RealTimeFaceActivity extends TemplatedActivity implements FaceInfoCallback {
	private final String TAG = LogUtils.makeLogTag("BestFaceActivity");

	final static int BESTFACE = 101, SET_RESULT = 122, FACE_TIMEOUT = 123, PLAYMAIN_END = 125;
	boolean isStop;// 界面遮盖
	boolean isLivePass;// 活体是否通过
	boolean isSetResult = false;// 跳转页面

	boolean isStartDetectFace;// 开始检测

	// 活体声音资源初始化
	public SoundPool sndPool;
	public Map<String, Integer> poolMap;
	int currentStreamID;
	boolean isLoadmain;
	boolean isPlayMain = true;

	CameraPreview mPreview;
	FrameLayout mFl_top;
	LinearLayout mLl_bottom;

	MainHandler mMainHandler;

	// 版权图片
	ImageView mIv_copyright;
	Bitmap mCopyright;

	public CloudwalkSDK cloudwalkSDK;
	public int initRet;

	int orientation;
	public CwProgressHUD processDialog;// 进度框
	// 使用广播来传递数据
	LocalBroadcastManager localBroadcastManager;
	LiveBroadcastReceiver liveBroadcastReceiver;

	public class LiveBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context arg0, Intent intent) {
			int faceCompareType = intent.getIntExtra("isFaceComparePass", Bulider.FACE_VERFY_FAIL);
			String faceCompareSessionId = intent.getStringExtra("faceSessionId");
			String faceCompareTipMsg = intent.getStringExtra("faceCompareTipMsg");
			double faceScore = intent.getDoubleExtra("faceCompareScore", 0d);

			setFaceResult(faceCompareType, faceScore, faceCompareSessionId, faceCompareTipMsg);

		}

	}

	@Override
	protected boolean hasActionBar() {
		orientation = this.getResources().getConfiguration().orientation;
		if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
			return false;
		} else {
			return true;
		}
	}

	private void initcloudwalkSDK() {
		cloudwalkSDK = cloudwalkSDK.getInstance(this);
		// 设置活体等级
		cloudwalkSDK.cwSetLivessLevel(Bulider.liveLevel);

		// 初始化
		initRet = cloudwalkSDK.cwInit(Bulider.licence);
		int op = FaceInterface.FaceDetType.CW_FACE_TRACK | FaceInterface.FaceDetType.CW_FACE_KEYPT
				| FaceInterface.FaceDetType.CW_FACE_QUALITY | FaceInterface.FaceDetType.CW_FACE_LIVENESS;
		cloudwalkSDK.setOperator(op);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.cloudwalk_activity_bestface);
		setTitle(R.string.cloudwalk_live_title);
		initSoundPool(this);
		mMainHandler = new MainHandler();

		initView();
		// FaceRecognize单例实例化
		initcloudwalkSDK();
		initCallBack();

		processDialog = CwProgressHUD.create(this).setStyle(CwProgressHUD.Style.SPIN_INDETERMINATE)
				.setLabel(getString(R.string.cloudwalk_faceverifying)).setCancellable(true).setAnimationSpeed(2)
				.setDimAmount(0.5f);

	}

	private void playMain() {
		if (isPlayMain && isLoadmain) {
			isPlayMain = false;
			currentStreamID = 1;// 第一个加载的语音为1
			sndPool.play(currentStreamID, 1.0f, 1.0f, 0, 0, 1.0f);
			mMainHandler.sendEmptyMessageDelayed(PLAYMAIN_END, 3000);

		}

	}

	public void initSoundPool(Context ctx) {

		poolMap = new HashMap<String, Integer>();
		sndPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 100);
		sndPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {

			@Override
			public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {

				if (1 == sampleId && initRet == 0) {
					isLoadmain = true;
					playMain();
				}

			}
		});

		poolMap.put("main", sndPool.load(ctx, R.raw.cloudwalk_main, 1));//

	}

	public void releaseSoundPool() {
		if (sndPool != null) {
			sndPool.release();
			sndPool = null;
		}

	}

	private void initCallBack() {
		cloudwalkSDK.cwFaceInfoCallback(this);

	}

	private void initView() {

		// 屏幕分辨率

		DisplayMetrics dm = new DisplayMetrics();
		this.getWindowManager().getDefaultDisplay().getMetrics(dm);
		int width = dm.widthPixels;
		int height = dm.heightPixels - DisplayUtil.dip2px(this, 45) - Util.getStatusBarHeight(this);
		// 版权图片
		mIv_copyright = (ImageView) findViewById(R.id.copyright_iv); // 云丛logo
		try {
			mCopyright = BitmapFactory.decodeStream(this.getAssets().open("yc_copyright.png"));
			if (mCopyright != null)
				mIv_copyright.setImageBitmap(mCopyright);
		} catch (IOException e) {

			e.printStackTrace();
		}

		// 根据预览分辨率设置Preview尺寸
		mPreview = (CameraPreview) findViewById(R.id.preview);
		mPreview.setScreenOrientation(orientation);
		if (CameraUtil.isHasCamera(Camera.CameraInfo.CAMERA_FACING_FRONT)) {
			mPreview.setCaremaId(Camera.CameraInfo.CAMERA_FACING_FRONT);
		} else {
			mPreview.setCaremaId(Camera.CameraInfo.CAMERA_FACING_BACK);
		}
		mFl_top = (FrameLayout) findViewById(R.id.top_fl);

		mLl_bottom = (LinearLayout) findViewById(R.id.bottom_rl);

		// 屏幕方向
		int previewW = 0, previewH = 0, flTopW = 0, flTopH = 0, bottomW = 0, bottomH = 0;
		if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
			height = dm.heightPixels - Util.getStatusBarHeight(this);
			previewH = height;
			previewW = previewH * Contants.PREVIEW_W / Contants.PREVIEW_H;
			flTopW = height;
			flTopH = height;
			bottomH = height;
			if (width - height < DisplayUtil.dip2px(this, 185)) {
				bottomW = DisplayUtil.dip2px(this, 185);
			} else {
				bottomW = width - height;
			}
			// 调整布局大小
			RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(previewW, previewH);
			mPreview.setLayoutParams(params);

			params = new RelativeLayout.LayoutParams(flTopW, flTopH);
			params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			mFl_top.setLayoutParams(params);
			mFl_top.setBackgroundResource(R.drawable.cloudwalk_face_main_camera_mask);

			params = new RelativeLayout.LayoutParams(bottomW, bottomH);
			params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
			mLl_bottom.setLayoutParams(params);
		} else if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			height = dm.heightPixels - DisplayUtil.dip2px(this, 45) - Util.getStatusBarHeight(this);
			previewW = width;
			previewH = width * Contants.PREVIEW_W / Contants.PREVIEW_H;
			flTopW = width;
			flTopH = width;
			bottomW = width;
			if (height - width < DisplayUtil.dip2px(this, 185)) {
				bottomH = DisplayUtil.dip2px(this, 185);
			} else {
				bottomH = height - width;
			}

			// 调整布局大小
			RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(previewW, previewH);
			mPreview.setLayoutParams(params);

			params = new RelativeLayout.LayoutParams(flTopW, flTopH);
			params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
			mFl_top.setLayoutParams(params);
			mFl_top.setBackgroundResource(R.drawable.cloudwalk_face_main_camera_mask);

			params = new RelativeLayout.LayoutParams(bottomW, bottomH);
			params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
			mLl_bottom.setLayoutParams(params);
		}

	}

	/**
	 * 获取最佳人脸
	 */
	private void getBestFace() {
		Bulider.bestFaceData = cloudwalkSDK.cwGetBestFace();

	}

	private void doFaceVerify() {
		isLivePass = true;

		if (Bulider.dfvCallBack == null) {

			mMainHandler.obtainMessage(SET_RESULT, CW_LivenessCode.CW_FACE_LIVENESS_FACEDEC_OK).sendToTarget();

			return;
		} else {
			localBroadcastManager = LocalBroadcastManager.getInstance(this);
			IntentFilter filter = new IntentFilter();
			filter.addAction(Contants.ACTION_BROADCAST_LIVE);
			liveBroadcastReceiver = new LiveBroadcastReceiver();
			localBroadcastManager.registerReceiver(liveBroadcastReceiver, filter);
			processDialog.show();
			Bulider.dfvCallBack.OnDefineFaceVerifyResult(Bulider.bestFaceData);
		}

	}

	/**
	 * 返回比对结果
	 *
	 * @param faceCompareType
	 *            人脸比对是否通过
	 * @param faceScore
	 *            比对分数
	 * @param sessionId
	 *            sessionId
	 * @param tipMsg
	 *            自定义提示信息
	 */
	public void setFaceResult(int faceCompareType, double faceScore, String sessionId, String tipMsg) {
		int resultType;
		boolean isVerfyPass = false;
		if (Bulider.FACE_VERFY_PASS == faceCompareType) {
			resultType = Bulider.FACE_VERFY_PASS;
			isVerfyPass = true;
		} else if (Bulider.FACE_VERFY_FAIL == faceCompareType) {
			resultType = Bulider.FACE_VERFY_FAIL;
		} else {
			resultType = Bulider.FACE_VERFY_NETFAIL;
		}
		if (processDialog != null && processDialog.isShowing()) {
			processDialog.dismiss();
		}

		setFaceResult(isVerfyPass, faceScore, sessionId, resultType, tipMsg);

	}

	private void setFaceResult(boolean isVerfyPass, double faceScore, String sessionId, int resultType, String tipMsg) {
		mMainHandler.removeCallbacksAndMessages(null);
		if (isSetResult || isStop)
			return;
		isSetResult = true;

		if (Bulider.isResultPage) {

			Intent mIntent = new Intent(this, LiveResultActivity.class);
			mIntent.putExtra(LiveResultActivity.FACEDECT_RESULT_TYPE, resultType);// 结果类型
			if (NullUtils.isNotEmpty(tipMsg))
				mIntent.putExtra(LiveResultActivity.FACEDECT_RESULT_MSG, tipMsg);// 自定义提示语句
			mIntent.putExtra(LiveResultActivity.ISLIVEPASS, isLivePass);// 活体是否通过
			mIntent.putExtra(LiveResultActivity.ISVERFYPASS, isVerfyPass);// 比对是否通过
			mIntent.putExtra(LiveResultActivity.FACESCORE, faceScore);// 比对分数
			mIntent.putExtra(LiveResultActivity.SESSIONID, sessionId);// 比对日志id

			startActivity(mIntent);
			finish();
		} else {

			if (Bulider.mResultCallBack != null)
				Bulider.mResultCallBack.result(isLivePass, isVerfyPass, sessionId, faceScore, resultType,
						Bulider.bestFaceData, Bulider.liveDatas);
			finish();
		}

	}

	@Override
	protected void onResume() {
		super.onResume();
		// 重置页面状态
		Bulider.bestFaceData = null;
		playMain();
		isLivePass = false;
		isStartDetectFace = false;
		isSetResult = false;

		isStop = false;

		if (initRet == 0) {
			mPreview.cwStartCamera();

		} else {

			mMainHandler.obtainMessage(SET_RESULT, CW_LivenessCode.CW_FACE_LIVENESS_AUTH_ERROR).sendToTarget();

		}

	}

	@Override
	protected void onRestart() {
		super.onRestart();

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		mMainHandler.removeCallbacksAndMessages(null);
		cloudwalkSDK.cwDestory();
		releaseSoundPool();
		if (processDialog != null && processDialog.isShowing()) {
			processDialog.dismiss();
		}
		if (localBroadcastManager != null)
			localBroadcastManager.unregisterReceiver(liveBroadcastReceiver);
	}

	@Override
	protected void onStop() {
		super.onStop();
		mPreview.cwStopCamera();
		isPlayMain = true;
		isStop = true;

		mMainHandler.removeCallbacksAndMessages(null);
		sndPool.stop(currentStreamID);

	}

	public class MainHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {

			switch (msg.what) {

			case SET_RESULT:
				Integer resultCode = (Integer) msg.obj;
				setFaceResult(false, 0d, "", resultCode, null);

				break;

			case PLAYMAIN_END:
				isStartDetectFace = true;//
				mMainHandler.sendEmptyMessageDelayed(BESTFACE, 1000);
				break;
			case FACE_TIMEOUT:
				mMainHandler.obtainMessage(SET_RESULT, Bulider.BESTFACE_FAIL).sendToTarget();
				break;
			case BESTFACE:

				if (Bulider.bestFaceData == null || Bulider.bestFaceData.length == 0) {
					getBestFace();
					mMainHandler.sendEmptyMessageDelayed(BESTFACE, 1000);

				} else {
					mMainHandler.removeCallbacksAndMessages(null);
					doFaceVerify();
				}

				break;

			}

			super.handleMessage(msg);
		}

	}

	@Override
	public void detectFaceInfo(FaceInfo[] faceInfos, int faceNum) {
		if (faceNum > 0) {// 检测到脸, 画脸框

			// boolean isX = faceInfos[0].x > Contants.PREVIEW_H * 0.15;
			// boolean isY = faceInfos[0].y > Contants.PREVIEW_H * 0.05;
			// boolean isW = (faceInfos[0].x + faceInfos[0].width) <
			// Contants.PREVIEW_H * 0.85;
			// boolean isH = (faceInfos[0].y + faceInfos[0].height) <
			// Contants.PREVIEW_W * 0.87;
			//
			// if (isStartDetectFace && isX && isY && isW && isH) {
			//
			// currentStep++;
			// doNextStep();
			// }

		} else {// 未检测到脸,清除脸框

		}

	}

}