package cn.cloudwalk.libproject;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.camera.authenticationlibrary.Bulider;

import cn.cloudwalk.FaceInterface.CW_LivenessCode;
import cn.cloudwalk.libproject.util.NullUtils;
import cn.cloudwalk.libproject.view.RoundProgressBar;

/**
 * 结果提示页面
 *
 * @author ysyhpc
 */
public class LiveResultActivity extends Activity {
    public static final String FACEDECT_RESULT_TYPE = "facedect_result_type";
    public static final String FACEDECT_RESULT_MSG = "facedect_result_msg";
    public static final String ISLIVEPASS = "islivepass";
    public static final String ISVERFYPASS = "isverfypass";
    public static final String FACESCORE = "facescore";
    public static final String SESSIONID = "sessionid";

    int type;
    boolean islivepass, isverfypass;
    double facescore;
    String sessionid;

    Button bt_restart, bt_ok;
    TextView tv_tip, tv_title;
    SoundPool sndPool;

    RoundProgressBar pb_circle;
    private int progress = 0;
    ImageView iv_result;
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            if (msg.what == 1) {
                pb_circle.setProgress(progress);
                if (Math.abs(progress) <= pb_circle.getMax()) {
                    progress = progress - 2;
                    mHandler.sendEmptyMessageDelayed(1, 5);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.cloudwalk_activity_facedect_result);

        getIntentData();
        initViews();

        if (type == Bulider.FACE_VERFY_PASS || type == CW_LivenessCode.CW_FACE_LIVENESS_FACEDEC_OK) {
            pb_circle.setArcColor(getResources().getColor(R.color.face_result_ok));
            iv_result.setImageResource(R.drawable.cloudwalk_gou);
            bt_restart.setVisibility(View.GONE);
            bt_ok.setText(R.string.commit);
        } else {
            pb_circle.setArcColor(getResources().getColor(R.color.face_result_fail));
            iv_result.setImageResource(R.drawable.cloudwalk_fail);
        }
        changeCircle();

        changeUI(type);

        bt_restart.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                startActivity(new Intent(LiveResultActivity.this, Bulider.startCls));
                finish();

            }

        });
        bt_ok.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                if (type == Bulider.FACE_VERFY_PASS || type == CW_LivenessCode.CW_FACE_LIVENESS_FACEDEC_OK) {
                    if (Bulider.mResultCallBack != null)
                        Bulider.mResultCallBack.result(islivepass, isverfypass, sessionid, facescore, type,
                                Bulider.bestFaceData, Bulider.liveDatas);
                    sendBroadcast(new Intent("iw_sendok"));
                    finish();
                } else {
                    finish();
                }

            }
        });

        String msg = getIntent().getStringExtra(FACEDECT_RESULT_MSG);
        if (NullUtils.isNotEmpty(msg)) {
            tv_tip.setText(msg);
        }

    }

    private void changeCircle() {
        pb_circle.setMax(100);
        progress--;
        mHandler.sendEmptyMessageDelayed(1, 500);
    }

    private void initViews() {
        bt_restart = (Button) findViewById(R.id.bt_restart);
        bt_ok = (Button) findViewById(R.id.bt_ok);
        iv_result = (ImageView) findViewById(R.id.iv_result);
        tv_tip = (TextView) findViewById(R.id.tv_tip);
        tv_title = (TextView) findViewById(R.id.tv_title);
        pb_circle = (RoundProgressBar) findViewById(R.id.pb_circle);
    }

    private void getIntentData() {
        type = getIntent().getIntExtra(FACEDECT_RESULT_TYPE, CW_LivenessCode.CW_FACE_LIVENESS_FACEDEC_OK);
        islivepass = getIntent().getBooleanExtra(ISLIVEPASS, false);
        isverfypass = getIntent().getBooleanExtra(ISVERFYPASS, false);
        facescore = getIntent().getDoubleExtra(FACESCORE, 0d);
        sessionid = getIntent().getStringExtra(SESSIONID);
    }

    private void changeUI(int type) {

        sndPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 100);
        switch (type) {
            case Bulider.FACE_VERFY_PASS:// 人脸比对通过
                int streamID = sndPool.load(this, R.raw.cloudwalk_verfy_suc, 1);

                tv_title.setText(R.string.faceverifysuc);
                tv_tip.setText(R.string.face_verfy_ok_tip);

                break;
            case CW_LivenessCode.CW_FACE_LIVENESS_FACEDEC_OK:// 活体检测通过
                streamID = sndPool.load(this, R.raw.cloudwalk_success, 1);

                tv_tip.setText(R.string.facedect_ok_tip);
                tv_title.setText(R.string.facedectsuc);

                break;

            case Bulider.FACE_VERFY_FAIL:// 人脸比对不通过
                streamID = sndPool.load(this, R.raw.cloudwalk_verfy_fail, 1);

                tv_title.setText(R.string.faceverifyfail);
                tv_tip.setText(R.string.face_verfy_fail_tip);
                break;
            case CW_LivenessCode.CW_FACE_LIVENESS_MULTIPERSONE:// 检测到多个人
                streamID = sndPool.load(this, R.raw.cloudwalk_failed_actionblend, 1);

                tv_tip.setText(R.string.facedectfail_moreface);
                break;
            case CW_LivenessCode.CW_FACE_LIVENESS_NOPEPOLE:// 没有检测到人脸
                streamID = sndPool.load(this, R.raw.cloudwalk_failed_actionblend, 1);

                tv_tip.setText(R.string.facedectfail_noface);
                break;
            case CW_LivenessCode.CW_FACE_LIVENESS_PEPOLECHANGED:// 检测到换人
                streamID = sndPool.load(this, R.raw.cloudwalk_failed_actionblend, 1);

                tv_tip.setText(R.string.facedectfail_changeface);
                break;
            case CW_LivenessCode.CW_FACE_LIVENESS_ATTACK_SHAKE:// 检测到攻击-图像抖动
            case CW_LivenessCode.CW_FACE_LIVENESS_ATTACK_MOUTH:// 检测到攻击-嘴被扣取
            case CW_LivenessCode.CW_FACE_LIVENESS_ATTACK_RIGHTEYE:// 检测到攻击-右眼被扣取
            case CW_LivenessCode.CW_FACE_LIVENESS_ATTACK_PICTURE:// 检测到攻击-图片攻击
            case CW_LivenessCode.CW_FACE_LIVENESS_ATTACK_UNSTABLE:// 检测到攻击-人脸不稳定
            case CW_LivenessCode.CW_FACE_LIVENESS_ATTACK_PAD:// 检测到攻击-方框(如纸片、pad)攻击
            case CW_LivenessCode.CW_FACE_LIVENESS_ATTACK_VIDEO:// 检测到攻击-视频攻击
                streamID = sndPool.load(this, R.raw.cloudwalk_failed_actionblend, 1);
                tv_tip.setText(R.string.facedectfail_fakeface);
                break;
            case CW_LivenessCode.CW_FACE_LIVENESS_WRONGACTION:// 错误动作
                streamID = sndPool.load(this, R.raw.cloudwalk_failed_actionblend, 1);

                break;
            case CW_LivenessCode.CW_FACE_LIVENESS_NOT_ACTIONBLEND:// 动作不符合规范
                streamID = sndPool.load(this, R.raw.cloudwalk_failed_actionblend, 1);

                break;
            case CW_LivenessCode.CW_FACE_LIVENESS_OVERTIME:// 活体超时
                streamID = sndPool.load(this, R.raw.cloudwalk_failed_timeout, 1);

                tv_tip.setText(R.string.facedectfail_timeout);

                break;

            case Bulider.FACE_VERFY_NETFAIL:// 网络异常
                streamID = sndPool.load(this, R.raw.cloudwalk_net_fail, 1);

                tv_tip.setText(R.string.facedec_net_fail);
                break;
            case CW_LivenessCode.CW_FACE_LIVENESS_AUTH_ERROR:// 授权失败
                tv_tip.setText(R.string.facedectfail_appid);
                bt_restart.setVisibility(View.GONE);

                break;
            case Bulider.BESTFACE_FAIL:// 最佳人脸获取失败
                streamID = sndPool.load(this, R.raw.cloudwalk_failed, 1);

                tv_title.setText(R.string.facedect_fail);
                tv_tip.setText(R.string.bestface_fail);

                break;
        }
        sndPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {

            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {

                int playId = sndPool.play(sampleId, 1.0f, 1.0f, 0, 0, 1.0f);

            }
        });
    }

    @Override
    protected void onStop() {

        sndPool.stop(1);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        sndPool.release();
        super.onDestroy();
    }

}