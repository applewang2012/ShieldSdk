/**
 * Project Name:cwFaceForDev3
 * File Name:LiveStartActivity.java
 * Package Name:cn.cloudwalk.dev.mobilebank
 * Date:2016-5-16上午9:17:24
 * Copyright @ 2010-2016 Cloudwalk Information Technology Co.Ltd All Rights Reserved.
 */

package cn.cloudwalk.libproject;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import cn.cloudwalk.libproject.callback.NoDoubleClickListener;
import cn.cloudwalk.libproject.util.LogUtils;

/**
 * ClassName: LiveStartActivity <br/>
 * Description: <br/>
 * date: 2016-5-16 上午9:17:24 <br/>
 *
 * @author 284891377
 * @since JDK 1.7
 */
public class LiveStartActivity extends TemplatedActivity {
    private final String TAG = LogUtils.makeLogTag("LiveActivity");

    Button mBt_startdect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.cloudwalk_layout_facedect_start);
        setTitle(R.string.cloudwalk_live_title);



        mBt_startdect = (Button) findViewById(R.id.bt_startdect);
        mBt_startdect.setOnClickListener(new NoDoubleClickListener() {

            @Override
            public void onNoDoubleClick(View v) {
                if (v.getId() == R.id.bt_startdect) {


                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {//6.0以上
                        //拍照权限申请
                        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                                != PackageManager.PERMISSION_GRANTED
                                && checkSelfPermission(Manifest.permission.CAMERA)
                                == PackageManager.PERMISSION_GRANTED) {

                            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 3);
                        } else if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                                == PackageManager.PERMISSION_GRANTED
                                && checkSelfPermission(Manifest.permission.CAMERA)
                                != PackageManager.PERMISSION_GRANTED) {
                            requestPermissions(new String[]{Manifest.permission.CAMERA}, 3);
                        } else if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                                != PackageManager.PERMISSION_GRANTED
                                && checkSelfPermission(Manifest.permission.CAMERA)
                                != PackageManager.PERMISSION_GRANTED) {
                            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.CAMERA}, 3);
                        } else {

                            startActivity(new Intent(LiveStartActivity.this, LiveActivity.class));
                            finish();
                        }
                    } else {

                        startActivity(new Intent(LiveStartActivity.this, LiveActivity.class));
                        finish();
                    }


                }

            }
        });

    }

    @Override
    protected void onResume() {

        super.onResume();
    }

    @Override
    protected void onStop() {

        super.onStop();
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 3) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[grantResults.length - 1] == PackageManager.PERMISSION_GRANTED) {
                //用户同意使用write
                startActivity(new Intent(LiveStartActivity.this, LiveActivity.class));
                finish();
            } else {
                //用户不同意，向用户展示该权限作用
//                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)
//                        || !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
//                    AlertDialog dialog = new AlertDialog.Builder(this)
//                            .setMessage("您禁止了拍照或访问存储的权限，请在手机设置中开启权限！")
//                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
//                                @Override
//                                public void onClick(DialogInterface dialog, int which) {
//
//                                }
//                            })
//                            .setNegativeButton("取消", new DialogInterface.OnClickListener() {
//                                @Override
//                                public void onClick(DialogInterface dialog, int which) {
//
//                                }
//                            }).create();
//                    dialog.show();
//                } else {
                    Toast.makeText(this, "您禁止了此权限！请选择允许", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }
