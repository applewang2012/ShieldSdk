package cn.cloudwalk.libproject.camera;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import cn.cloudwalk.CloudwalkSDK;
import cn.cloudwalk.FaceInterface;
import cn.cloudwalk.FaceInterface.ImageForm;
import cn.cloudwalk.libproject.Contants;
import cn.cloudwalk.libproject.util.LogUtils;

/**
 * 实时预览帧 setPreviewCallback
 * 
 * @author yusr
 *
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, PreviewCallback {

	private static final String TAG = LogUtils.makeLogTag("CameraPreview");

	private Camera mCamera;

	Delegate mDelegate;
	private int orientation;
	/**
	 * 设置屏幕方向
	 *
	 * @param orientation
	 *            Configuration.ORIENTATION_LANDSCAPE 或者
	 *            Configuration.ORIENTATION_PORTRAIT
	 */
	public void setScreenOrientation(int orientation) {
		this.orientation = orientation;

	}

	//摄像头id
	int caremaId=Camera.CameraInfo.CAMERA_FACING_FRONT;
	public int getCaremaId() {
		return caremaId;
	}
	public void setCaremaId(int caremaId) {
		this.caremaId = caremaId;
	}

	private boolean mPreviewing = true;
	private boolean mSurfaceCreated = false;
	private CameraConfigurationManager mCameraConfigurationManager;
	Context context;
    /**
     * setReqPrevWH:设置希望的预览分辨率. <br/>
     * @author:284891377   Date: 2016/10/25 0025 10:50
     *
     * @since JDK 1.7
     */
	public void setReqPrevWH(int reqPrevW,int reqPrevH) {
		this.reqPrevW = reqPrevW;
		this. reqPrevH= reqPrevH;
	}

	int reqPrevW=Contants.PREVIEW_W,reqPrevH=Contants.PREVIEW_H;


	public CameraPreview(Context context) {
		super(context);
		this.context = context;
	}

	public CameraPreview(Context context, AttributeSet attrs, int defStyle) {

		super(context, attrs, defStyle);
		this.context = context;
	}

	public CameraPreview(Context context, AttributeSet attrs) {

		super(context, attrs);
		this.context = context;
	}

	public void setCamera(Camera camera) {
		mCamera = camera;
		if (mCamera != null) {
			mCameraConfigurationManager = new CameraConfigurationManager(getContext());

			getHolder().addCallback(this);
			if (mPreviewing) {
				requestLayout();
			} else {
				showCameraPreview();
			}
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder surfaceHolder) {
		mSurfaceCreated = true;
	}

	@Override
	public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
		if (surfaceHolder.getSurface() == null) {
			return;
		}
		stopCameraPreview();
		showCameraPreview();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
		mSurfaceCreated = false;
		stopCameraPreview();
	}

	public void showCameraPreview() {
		if (mCamera != null) {
			try {
				mPreviewing = true;
				mCamera.setPreviewDisplay(getHolder());

				mCameraConfigurationManager.setCameraParametersForPreviewCallBack(mCamera, caremaId, reqPrevW,
						reqPrevH);
				mCamera.startPreview();
				mCamera.setPreviewCallback(CameraPreview.this);
			} catch (Exception e) {
				LogUtils.LOGE(TAG, e.toString());
			}
		}
	}

	public void stopCameraPreview() {
		if (mCamera != null) {
			try {

				mPreviewing = false;
				mCamera.cancelAutoFocus();
				mCamera.setPreviewCallback(null);
				mCamera.stopPreview();
			} catch (Exception e) {
				LogUtils.LOGE(TAG, e.toString());
			}
		}
	}

	public void openFlashlight() {
		if (flashLightAvaliable()) {
			mCameraConfigurationManager.openFlashlight(mCamera);
		}
	}

	public void closeFlashlight() {
		if (flashLightAvaliable()) {
			mCameraConfigurationManager.closeFlashlight(mCamera);
		}
	}

	private boolean flashLightAvaliable() {
		return mCamera != null && mPreviewing && mSurfaceCreated
				&& getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
	}

	/******************************************************************/
	public Size getPreviewSize() {
		Camera.Parameters parameters = mCamera.getParameters();
		return parameters.getPreviewSize();
	}

	public void setDelegate(Delegate mDelegate) {
		this.mDelegate = mDelegate;
	}

	/**
	 * 打开摄像头开始预览，但是并未开始识别
	 */
	public void cwStartCamera() {
		if (mCamera != null) {
			return;
		}

		try {
			mCamera = Camera.open(caremaId);
		} catch (Exception e) {
			if (mDelegate != null) {
				mDelegate.onOpenCameraError();
			}
		}
		setCamera(mCamera);
	}

	/**
	 * 关闭摄像头预览，并且隐藏扫描框
	 */
	public void cwStopCamera() {
		if (mCamera != null) {
			stopCameraPreview();

			mCamera.release();
			mCamera = null;
		}
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {

		if (caremaId == Camera.CameraInfo.CAMERA_FACING_FRONT) {// 前置
			if (Configuration.ORIENTATION_PORTRAIT == orientation) {// 竖屏
																	// 水平镜像+旋转90
				CloudwalkSDK.getInstance(context).cwPushFrame(data, reqPrevW, reqPrevH,
						ImageForm.CW_IMG_NV21, FaceInterface.CaremaType.FRONT_PORTRAIT);
			} else {// 横屏 水平镜像

				CloudwalkSDK.getInstance(context).cwPushFrame(data, reqPrevW, reqPrevH,
						ImageForm.CW_IMG_NV21, FaceInterface.CaremaType.FRONT_LANDSCAPE);
			}

		} else {// 后置
			if (Configuration.ORIENTATION_PORTRAIT == orientation) {// 竖屏 旋转90
				CloudwalkSDK.getInstance(context).cwPushFrame(data, reqPrevW, reqPrevH,
						ImageForm.CW_IMG_NV21, FaceInterface.CaremaType.BACK_PORTRAIT);

			} else {
				// 横屏不做处理
				CloudwalkSDK.getInstance(context).cwPushFrame(data, reqPrevW, reqPrevH,
						ImageForm.CW_IMG_NV21, FaceInterface.CaremaType.BACK_LANDSCAPE);
			}

		}

	}


	/**
	 * 切换摄像头
	 */
	public int switchCarema() {
		cwStopCamera();
		if (caremaId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			caremaId = Camera.CameraInfo.CAMERA_FACING_BACK;
		} else {
			caremaId = Camera.CameraInfo.CAMERA_FACING_FRONT;
		}
		cwStartCamera();
		return caremaId;
	}
}