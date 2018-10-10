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
import android.widget.TextView;

import com.alibaba.ailabs.custom.core.AliGenieSDK;
import com.alibaba.ailabs.custom.core.Constants;
import com.alibaba.ailabs.custom.util.SystemInfo;
import com.alibaba.ailabs.geniesdk.audioin.recorder.BaseRecorder;
import com.alibaba.ailabs.geniesdk.audioin.recorder.IMicController;
import com.alibaba.ailabs.geniesdk.audioin.recorder.OutAudioDataFarFieldRecorder;
import com.alibaba.ailabs.geniesdk.audioin.recorder.RecorderManager;
import com.alibaba.ailabs.geniesdk.util.LogUtils;
import com.alibaba.ailabs.geniesdk_adapter.audioin.RecorderFactory;
import com.alibaba.ailabs.geniesdk_adapter.core.ActionConstant;
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

public class ProcessedAudioDataDemoActivity extends AppCompatActivity implements IUiManager {
    private static String TAG = "geniesdk";
    private Button wakeup;
    private TextView asrResult;
    private TextView nluResult;

    private boolean mIsRecording = true;
    private boolean mIsWakeup = false;
    private boolean mCallWakeup = false;
    private boolean mCallStop = true;
    private OutAudioDataFarFieldRecorder recorder;
    private DataOutputStream dos;
    private boolean save = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AliGenieSDK.getInstance(this).init(null,this, RecorderFactory.getOutAudioDataFarFieldRecorder(16000, 2, MediaRecorder.AudioSource.VOICE_RECOGNITION, AudioFormat.ENCODING_PCM_16BIT), null);
        //AliGenieSDK.getInstance(this).setUseThirdPartyMediaController(true);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.processed_audio_data);
        wakeup = findViewById(R.id.wakeup_press);
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

        wakeup.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_UP://松开事件发生后执行代码的区域
                        Log.e(TAG,"wakeup released");
                        stopPcmSave();
                        mCallStop = true;
                        mIsWakeup = false;
                        mCallWakeup = false;
                        break;
                    case MotionEvent.ACTION_DOWN://按住事件发生后执行代码的区域
                        Log.e(TAG,"wakeup pressed");
                        if (!mIsWakeup) {
                            mIsWakeup = true;
                            mCallWakeup = true;
                            mCallStop = false;
                            startPcmSave();
                        }
                        break;
                    default:
                        break;
                }
                return true;
            }
        });


        wakeup.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if (keyEvent.getKeyCode() == 23) {
                    if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                        Log.e(TAG,"wakeup pressed");
                        if (!mIsWakeup) {
                            mIsWakeup = true;
                            mCallWakeup = true;
                            mCallStop = false;
                            startPcmSave();
                        }
                    } else if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
                        Log.e(TAG,"wakeup released");
                        stopPcmSave();
                        mCallStop = true;
                        mIsWakeup = false;
                        mCallWakeup = false;
                    }
                    return true;
                }
                return false;
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        mIsRecording = true;
        testRawDataFarfieldRecord();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mIsRecording = false;
    }

    private void testRawDataFarfieldRecord() {
        new Thread() {
            public void run() {
                int sampleRate = recorder.getSampleRate(SessionID.INPUT_GLOBAL_DEFAULT);
                int channel = recorder.getRealSampleChannel(SessionID.INPUT_GLOBAL_DEFAULT);
                int format = recorder.getSampleBytes(SessionID.INPUT_GLOBAL_DEFAULT);
                int source = recorder.getAudioSource(SessionID.INPUT_GLOBAL_DEFAULT);
                int bufferSize = recorder.getBufferSize();
                byte[] buffer = new byte[bufferSize];

                // /实例化AudioRecord
                AudioRecord record = new AudioRecord(source, sampleRate, channel, format, AudioRecord.getMinBufferSize(sampleRate, channel, format) * 8);
                LogUtils.d("channels="+ record.getChannelCount());
                //开始录制
                record.startRecording();
                try {
                    //定义循环，根据isRecording的值来判断是否继续录制
                    while (mIsRecording) {
                        //读取录音流的数据
                        int bufferReadResult = record.read(buffer, 0, buffer.length);
                        //LogUtils.d("bufferReadResult=" + bufferReadResult, BaseRecorder.class);
                        if (mIsWakeup) {
                            if (mCallWakeup) {
                                mCallWakeup = false;
                                recorder.wakeup(1.0, "ni hao tian mao");
                            }
                            recorder.receiveVadData(buffer);
                            writePcmData(dos, buffer);
                        } else {
                            if (mCallStop) {
                                mCallStop = false;
                                recorder.vadEnd(false);
                            }
                        }
                    }
                    //录制结束
                    record.stop();
                    record.release();
                } catch (Exception e) {
                    LogUtils.e("error occured in recodeRunning, e="+e, BaseRecorder.class);
                    e.printStackTrace();
                }
            }
        }.start();
    }

    //=====TEST CODE START=====
    private DataOutputStream savePcm() {
        DataOutputStream dos;
        File file = new File(SystemInfo.getContext().getFilesDir().getAbsolutePath() + "/test_pcm.pcm");
        LogUtils.d("generate >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> file.");
        //如果存在，就先删除再创建
        if (file.exists())
            file.delete();

        try {
            file.createNewFile();

            //输出流
            OutputStream os = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(os);
            dos = new DataOutputStream(bos);

        } catch (IOException e) {
            throw new IllegalStateException("未能创建" + file.toString());
        }
        return dos;
    }

    private void writePcmData(DataOutputStream dos, byte[] dataBuffer) {
        try {
            dos.write(dataBuffer);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void startPcmSave() {
        if (dos != null) {
            try {
                dos.flush();
                dos.close();
                dos = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        dos = savePcm();
        save = true;
    }

    public void stopPcmSave() {
        save = false;
    }

    //=====TEST CODE END=====

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
        } catch (Exception e) {
            LogUtils.e("Call remote function failed, e="+e);
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
