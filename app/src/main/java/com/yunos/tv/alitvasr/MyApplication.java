package com.yunos.tv.alitvasr;

import android.app.Application;
import android.media.AudioFormat;
import android.media.MediaRecorder;

import com.alibaba.ailabs.custom.core.AliGenieSDK;
import com.alibaba.ailabs.geniesdk.audioin.recorder.BaseRecorder;
import com.alibaba.ailabs.geniesdk_adapter.audioin.RecorderFactory;
import com.alibaba.ailabs.geniesdk_adapter.core.MainApplication;
import com.yunos.tv.alitvasr.controller.session.IPreOnNLPResult;
import com.yunos.tv.alitvasr.ui.interfaces.IUiManager;


/**
 * Created by majun on 2018/4/18.
 */

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
    }

}
