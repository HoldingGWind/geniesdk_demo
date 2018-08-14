package com.yunos.tv.alitvasr;

import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.alibaba.ailabs.custom.audio.input.RecorderManager;
import com.alibaba.ailabs.custom.core.AliGenieSDK;
import com.alibaba.ailabs.custom.core.Constants;
import com.alibaba.ailabs.custom.util.SystemInfo;
import com.alibaba.ailabs.geniesdk.audioin.recorder.BaseRecorder;
import com.alibaba.ailabs.geniesdk.audioin.recorder.IMicController;
import com.alibaba.ailabs.geniesdk.audioin.recorder.OutAudioDataFarFieldRecorder;
import com.alibaba.ailabs.geniesdk.util.LogUtils;
import com.alibaba.ailabs.geniesdk_adapter.audioin.RecorderFactory;
import com.alibaba.ailabs.geniesdk_adapter.core.ActionConstant;
import com.alibaba.ailabs.geniesdk_adapter.core.AliGenieSDKAdapter;
import com.alibaba.ailabs.geniesdk_adapter.core.RemoteServiceManager;
import com.alibaba.sdk.aligeniesdkdemo.R;
import com.yunos.tv.alitvasr.controller.IUIListener;
import com.yunos.tv.alitvasr.controller.protocol.ProtocolData;
import com.yunos.tv.alitvasr.controller.session.SessionID;
import com.yunos.tv.alitvasr.ui.interfaces.IBaseView;
import com.yunos.tv.alitvasr.ui.interfaces.IUiManager;

import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by miyang on 2018/8/7.
 */

public class TextStreamDataDemoActivity extends AppCompatActivity implements IUiManager {

    private static String TAG = "geniesdk";
    private TextView asrResult;
    private TextView nluResult;
    private OutAudioDataFarFieldRecorder recorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AliGenieSDK.getInstance(this).init("db643dcd-b096-43e8-9707-6f34d36a1549"/*"ef785ed9-785e-41e4-8c84-ff82f41528f8"*/,this, RecorderFactory.getTextStreamFarFieldRecorder(), null);
        //AliGenieSDK.getInstance(this).setUseThirdPartyMediaController(true);
        //AliGenieSDKAdapter.init();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.text_stream_data);
        asrResult = findViewById(R.id.asr_result);
        nluResult = findViewById(R.id.nlu_result);
        recorder = RecorderManager.getInstance().getRecorder();
        recorder.setMicController(new IMicController() {
            @Override
            public boolean openMic() {
                LogUtils.d("Open mic here.");
                return true;
            }
        });
    }

    public void onButton1Click(View view) {
        recorder.wakeup(1.0, "tian mao jing ling");
        recorder.receiveTextSteam("我要听歌", true);
        recorder.vadEnd();
    }

    public void onButton2Click(View view) {
        recorder.wakeup(1.0, "tian mao jing ling");
        recorder.receiveTextSteam("今天的天气", true);
        recorder.vadEnd();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    /**
     * 对话面板出现和消失的回调
     * @param isShow true:出现 false:消失
     */
    @Override
    public void onShow(boolean isShow) {

    }

    /**
     * 开始录音的回调
     * @param sessionId
     */
    @Override
    public void onRecordStart(int sessionId) {

    }

    /**
     * 实时ASR语音转文字
     * @param sessionId
     * @param streamText
     * @param isFinish
     */
    @Override
    public void onStreaming(int sessionId, String streamText, boolean isFinish) {
        Log.e(TAG, "sessionId = " + sessionId + ", streamText= " + streamText
                + ", isFinish=" + isFinish);
        asrResult.setText(streamText);
        Bundle bundle = new Bundle();
        bundle.putString(ActionConstant.KEY_ARGS1, streamText);
        try {
            RemoteServiceManager.getInstance(this).call(ActionConstant.MESSAGE_ASK, bundle);
        } catch (RemoteException e) {
            com.alibaba.ailabs.custom.util.LogUtils.e("Call remote function failed, e="+e);
        }
    }

    /**
     * 音量变更回调
     * @param sessionId
     * @param volume
     */
    @Override
    public void onVolume(int sessionId, int volume) {
        Log.e(TAG, "sessionId = " + sessionId + ", volume= " + volume);
    }

    /**
     * 录音结束回调
     * @param sessionId
     */
    @Override
    public void onRecordStop(int sessionId) {
        Log.e(TAG, "sessionId = " + sessionId );

    }

    /**
     * NLP语义解析结果回调，原始数据
     * @param sessionId
     * @param data
     */
    @Override
    public void onRecognizeResult(int sessionId, ProtocolData data) {
        Log.e(TAG, "sessionId = " + sessionId  + ",data = " + data.toString());
        nluResult.setText(data.toString());
    }

    /**
     * 隐藏面板
     */
    @Override
    public void hideUi() {
        Log.e(TAG, "hideUi");
    }

    /**
     * 面板是否显示
     * @return
     */
    @Override
    public boolean isUiShowing() {
        return true;
    }

    /**
     * NLP语义预处理回调，已废弃
     * @param i
     * @param s
     * @param s1
     * @param jsonObject
     * @param s2
     * @return
     */
    @Override
    public int onPretreatedResult(int i, String s, String s1, JSONObject jsonObject, String s2) {
        return 0;
    }

    /**
     * NLP语义预处理回调，将领域，命令名，命令参数预解析出来，方便使用
     * @param sessionId
     * @param data
     * @param commandDomain
     * @param command
     * @param commandParams
     * @param question
     * @return
     */
    @Override
    public int onPretreatedResult(int sessionId, ProtocolData data, String commandDomain, String command, JSONObject commandParams, String question) {
        Log.e(TAG, "sessionId = " + sessionId  + ",commandDomain = " + commandDomain + ",command = " + command + ",commandParams = " + commandParams.toString());
        return 0;
    }

    /**
     * 底层通知回调，包括账号绑定，音量设置等通知
     * @param type
     * @param data
     * @param arg1
     * @param arg2
     */
    @Override
    public void onNotify(int type, Object data, int arg1, int arg2) {
        LogUtils.d("type="+type+",Object="+data+",arg1="+arg1+",arg2="+arg2);
        switch (type) {
            case IBaseView.NOTIFY_QRCODE_MESSAGE: {
                //显示二维码
                if (data != null) {
                    try {
                        Intent intent = new Intent();
                        intent.setAction(Constants.ACTION_SHOW_QRCODE);
                        intent.setPackage(SystemInfo.getContext().getPackageName());
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra(Constants.QRCODE_KEY, data.toString());
                        SystemInfo.getContext().startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            break;
            case IBaseView.NOTIFY_BINDER_USER: {
                //绑定设备
                LocalBroadcastManager.getInstance(SystemInfo.getContext()).sendBroadcast(new Intent(Constants.ACTION_STOP_ACTIVITY));
            }
            break;
        }
    }

    /**
     * IUIListener只有onShow一个接口，用于给其他类提供一个面板弹出时的回调入口
     * @param listener
     */
    @Override
    public void setUIListener(IUIListener listener) {

    }

    /**
     * 低内存回调
     */
    @Override
    public void onLowMemory() {

    }

    /**
     * 获取机顶盒上下文信息
     * @param isScreen
     * @param thridContext
     * @return
     */
    @Override
    public String getTvContext(boolean isScreen, String thridContext) {
        return "{\"systemInfo\":{\"area_code\":\"\",\"uuid\":\"928911AD5D41A7FD9BFCE90E0A86C96D\",\"device_model\":\"MagicBox_M18S\",\"device_firmware_version\":\"6.1.0-R-20180417.0354\",\"firmware\":\"6.1.0-R-20180417.0354\",\"edu_version_code\":2100405003,\"device_sn\":\"928911AD5D41A7FD9BFCE90E0A86C96D\",\"bcp\":\"1\",\"charge_type\":\"2,3,5\",\"from\":\"0,7\",\"device_media\":\"s265_1080p\",\"sw\":\"sw1080\",\"version_code\":2120511020},\"sceneInfo\":\"{\\\"appPackage\\\":\\\"com.alibaba.sdk.aligeniesdkdemo\\\",\\\"clientVersion\\\":2100607006,\\\"clientVersionName\\\":\\\"7.0.06\\\",\\\"clientTime\\\":1524575642306,\\\"system_locale\\\":\\\"浙江省 杭州市 市辖区\\\",\\\"deviceMode\\\":0,\\\"location\\\":\\\"浙江省 杭州市 市辖区\\\",\\\"city\\\":\\\"浙江省\\\",\\\"useApp\\\":\\\"com.yunos.tv.homeshell\\\",\\\"useAppClientVersion\\\":\\\"2100590020\\\",\\\"timezone\\\":\\\"Asia\\\\\\/Shanghai\\\",\\\"micType\\\":0}\",\"sceneExtInfo\":\"{\\\"context\\\":\\\"{\\\\\\\"pageSizeMax\\\\\\\":20}\\\"}\",\"packageInfo\":\"{\\\"com.yunos.tv.appstore\\\":\\\"2101407000\\\",\\\"com.yunos.tv.yingshi.boutique\\\":\\\"2120506118\\\"}\",\"memory\":\"1991\",\"clientVersion\":\"2100607006\",\"localeInfo\":\"{\\\"language\\\":\\\"zh\\\",\\\"country\\\":\\\"CN\\\"}\",\"protocolVersion\":2}";
    }
}
