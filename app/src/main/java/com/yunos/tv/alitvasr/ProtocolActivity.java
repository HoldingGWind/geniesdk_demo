package com.yunos.tv.alitvasr;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.alibaba.ailabs.custom.core.Constants;
import com.alibaba.ailabs.custom.util.DateUtils;
import com.alibaba.ailabs.custom.util.LogUtils;
import com.alibaba.ailabs.custom.util.StringUtil;
import com.alibaba.ailabs.custom.util.SystemInfo;
import com.alibaba.sdk.aligeniesdkdemo.R;

import java.util.Date;

public class ProtocolActivity extends Activity implements View.OnClickListener, View.OnFocusChangeListener {

    private String TAG = "ProtocolActivity";
    private Button agree;
    private boolean PROTOCOL_SIGNED = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.protocol_layout);

        agree = (Button) findViewById(R.id.protocol_content_agree);
        agree.setOnFocusChangeListener(this);
        agree.requestFocus();

        if (isProtocolSigned()) {
            LogUtils.d("protocol already sign!");
            setAgreeState();
        } else {
            LogUtils.d("protocol not sign");
            agree.setOnClickListener(this);
            agree.setOnFocusChangeListener(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        LogUtils.d("onBackPressed()");
        if (!isProtocolSigned()) {
            // 创建退出对话框
            AlertDialog isExit = new AlertDialog.Builder(this).create();
            isExit.setTitle("用户协议还未签署");
            // 设置对话框消息
            isExit.setMessage("确定要退出吗？");
            // 添加选择按钮并注册监听
            isExit.setButton("确定", listener);
            isExit.setButton2("取消", listener);
            // 显示对话框
            isExit.show();
        } else {
            super.onBackPressed();

        }
    }


    DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case AlertDialog.BUTTON_POSITIVE:// "确认"按钮退出程序
                    finish();
                    break;
                case AlertDialog.BUTTON_NEGATIVE:// "取消"第二个按钮取消对话框
                    break;
                default:
                    break;
            }
        }
    };


    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.protocol_content_agree) {
            setAgreeState();
            commitProtocolSignTime(DateUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        }
        finish();
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        int id = v.getId();
        if (id == R.id.protocol_content_agree) {
            if (hasFocus) {
                agree.setBackgroundResource(R.drawable.agree_btn_focus);
            } else {
                agree.setBackgroundResource(R.drawable.agree_btn_unfocus);
            }
        }
    }

    private void commitProtocolSignTime(String time) {
        SharedPreferences mSharedPreferences = getSharedPreferences("ProtocolSignTime", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString("sign_time", time);
        editor.commit();
    }

    private boolean isProtocolSigned() {
        SharedPreferences mSharedPreferences = getSharedPreferences("ProtocolSignTime", Context.MODE_PRIVATE);
        String time = mSharedPreferences.getString("sign_time", "");
        if (StringUtil.isNotEmpty(time)) {
            return true;
        }

        String signTime = SystemInfo.getSystemProperty(Constants.PROPERTY_CONFIRM_LICENSE, null);
        if (StringUtil.isNotEmpty(signTime)) {
            LogUtils.i("commit protocol sign time to local:" + signTime);
            commitProtocolSignTime(signTime);
            return true;
        }

        return false;
    }


    private void setAgreeState() {
        agree.setBackgroundResource(R.drawable.agree_btn_focus);
        agree.setText(R.string.protocol_signed);
    }
}

