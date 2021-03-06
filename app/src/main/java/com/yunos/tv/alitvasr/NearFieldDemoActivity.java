package com.yunos.tv.alitvasr;

import android.content.Intent;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.alibaba.ailabs.custom.audio.IMediaOutputListener;
import com.alibaba.ailabs.custom.audio.MediaOutputBridge;
import com.alibaba.ailabs.custom.bean.GrantDialog;
import com.alibaba.ailabs.custom.command.IPreOnCommand;
import com.alibaba.ailabs.custom.core.AliGenieSDK;
import com.alibaba.ailabs.custom.util.SystemInfo;
import com.alibaba.ailabs.geniesdk.audioin.recorder.NearFieldRecorder;
import com.alibaba.ailabs.geniesdk.audioin.recorder.RecorderManager;
import com.alibaba.ailabs.geniesdk.util.LogUtils;
import com.alibaba.ailabs.geniesdk_adapter.audioin.RecorderFactory;
import com.alibaba.ailabs.geniesdk_adapter.core.ActionConstant;
import com.alibaba.ailabs.geniesdk_adapter.core.RemoteServiceManager;
import com.alibaba.sdk.aligeniesdkdemo.R;
import com.yunos.tv.alitvasr.account.BindDeviceGuideActivity;
import com.yunos.tv.alitvasr.controller.IUIListener;
import com.yunos.tv.alitvasr.controller.protocol.ProtocolData;
import com.yunos.tv.alitvasr.controller.protocol.ReturnCode;
import com.yunos.tv.alitvasr.controller.session.IPreOnNLPResult;
import com.yunos.tv.alitvasr.model.binder.UserData;
import com.yunos.tv.alitvasr.ui.interfaces.BindDeviceListener;
import com.yunos.tv.alitvasr.ui.interfaces.IBaseView;
import com.yunos.tv.alitvasr.ui.interfaces.IUiManager;

import org.json.JSONObject;

import java.io.DataOutputStream;
import java.lang.ref.WeakReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by miyang on 2018/8/7.
 */

public class NearFieldDemoActivity extends AppCompatActivity implements IUiManager, IPreOnCommand {
    private static String TAG = "geniesdk";
    private static WeakReference<BindDeviceListener> bindDeviceListener;

    private Button wakeup, startTalk, stopTalk, showLogin, stopMedia;
    private TextView asrResult;
    private TextView nluResult;

    private boolean mIsWakeup = false;
    private NearFieldRecorder recorder;
    private DataOutputStream dos;
    private boolean save = true;

    public static final String BIND_RESPONSE = "com.yunos.tv.altvasr.bindResponse";
    public static final String BIND_USER = "com.yunos.tv.altvasr.bindUser";
    public static final String SHOW_QRCODE = "com.yunos.tv.altvasr.showQrCode";
    public static final String QRCODE_KEY = "__qrcode_key";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AliGenieSDK.getInstance(this).init(null, this, RecorderFactory.getNearFieldRecorder(16000, 1, MediaRecorder.AudioSource.MIC, AudioFormat.ENCODING_PCM_16BIT), this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.near_field);
        wakeup = findViewById(R.id.wake);
        startTalk = findViewById(R.id.startTalk);
        stopTalk = findViewById(R.id.stopTalk);
        showLogin = findViewById(R.id.showLogin);
        asrResult = findViewById(R.id.asr_result);
        nluResult = findViewById(R.id.nlu_result);
        recorder = RecorderManager.getInstance().getRecorder();
        //recorder.enableDefaultVAD(false);  禁用默认的VAD

        wakeup.setOnTouchListener(new View.OnTouchListener() {
            @Override

            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_UP://松开事件发生后执行代码的区域
                        Log.e(TAG, "wakeup released");
                        mIsWakeup = false;
                        break;
                    case MotionEvent.ACTION_DOWN://按住事件发生后执行代码的区域
                        Log.e(TAG, "wakeup pressed");
                        if (!mIsWakeup) {
                            mIsWakeup = true;
                            recorder.wakeup(1.0, "ni hao tian mao");

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
                        Log.e(TAG, "wakeup pressed");
                        if (!mIsWakeup) {
                            mIsWakeup = true;
                            recorder.wakeup(1.0, "ni hao tian mao");
                        }
                    } else if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
                        Log.e(TAG, "wakeup released");
                        mIsWakeup = false;
                    }
                    return true;
                }
                return false;
            }
        });

        startTalk.setOnClickListener(new View.OnClickListener()

        {
            public void onClick(View var1) {
                recorder.wakeup(1.0, "ni hao tian mao");
            }
        });

        stopTalk.setOnClickListener(new View.OnClickListener()

        {
            public void onClick(View var1) {
                recorder.vadEnd(true);
            }
        });

        showLogin.setOnClickListener(new View.OnClickListener() {
            public void onClick(View var1) {
                Intent intent = new Intent();
                intent.setClass(SystemInfo.getContext(), BindDeviceGuideActivity.class);
                startActivity(intent);
            }
        });

        stopMedia = findViewById(R.id.stopMedia);
        stopMedia.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View var1) {
                MediaOutputBridge.getInstance().clean();
            }
        });


        MediaOutputBridge.getInstance().setMediaOutputListener(new IMediaOutputListener() {
            @Override
            public void onAudioPlayStart() {
                LogUtils.d(">>>>> onAudioPlayStart");
            }

            @Override
            public void onAudioPlayStop() {
                LogUtils.d(">>>>> onAudioPlayStop");
            }

            @Override
            public void onAudioPlayComplete() {
                LogUtils.d(">>>>> onAudioPlayComplete");
            }

            @Override
            public void onAudioPlayPause() {
                LogUtils.d(">>>>> onAudioPlayPause");
            }

            @Override
            public void onAudioPlayResume() {
                LogUtils.d(">>>>> onAudioPlayResume");
            }

            @Override
            public void onAudioPlaySuspend() {
                LogUtils.d(">>>>> onAudioPlaySuspend");
            }

            @Override
            public void onAudioPlaySuspend2Resume() {
                LogUtils.d(">>>>> onAudioPlaySuspend2Resume");
            }

            @Override
            public void onAudioSeek(int progress) {
                LogUtils.d(">>>>> onAudioSeek");
            }

            @Override
            public void onOneshotAudioPlayStart() {

            }

            @Override
            public void onOneshotAudioPlayStop() {

            }

            @Override
            public void onTtsStart() {
                LogUtils.d(">>>>> onTtsStart");
            }

            @Override
            public void onTtsStop(boolean expectSpeech) {
                LogUtils.d(">>>>> onTtsStop");
            }
        });
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
     *
     * @param isShow true:出现 false:消失
     */
    @Override
    public void onShow(boolean isShow) {

    }

    /**
     * 开始录音的回调
     *
     * @param sessionId
     */
    @Override
    public void onRecordStart(int sessionId) {

    }

    /**
     * 实时ASR语音转文字
     *
     * @param sessionId
     * @param streamText
     * @param isFinish
     */
    @Override
    public void onStreaming(int sessionId, String streamText, boolean isFinish) {
        Log.e(TAG, "sessionId = " + sessionId + ", streamText= " + streamText
                + ", isFinish=" + isFinish);
        if (!TextUtils.isEmpty(streamText)) {
            StringBuffer sbr = new StringBuffer();
            String rex = "(\\s*)([\\u4e00-\\u9fa5]+)(\\s+)";
            Pattern p = Pattern.compile(rex);
            Matcher m = p.matcher(streamText);
            while (m.find()) {
                m.appendReplacement(sbr, m.group(2));
            }
            m.appendTail(sbr);
            asrResult.setText(sbr);
        }

        Bundle bundle = new Bundle();
        bundle.putString(ActionConstant.KEY_ARGS1, streamText);
        try {
            RemoteServiceManager.getInstance(this).call(ActionConstant.MESSAGE_ASK, bundle);
        } catch (Exception e) {
            LogUtils.e("Call remote function failed, e=" + e);
        }
    }

    /**
     * 音量变更回调
     *
     * @param sessionId
     * @param volume
     */
    @Override
    public void onVolume(int sessionId, int volume) {
        Log.e(TAG, "sessionId = " + sessionId + ", volume= " + volume);
    }

    /**
     * 录音结束回调
     *
     * @param sessionId
     */
    @Override
    public void onRecordStop(int sessionId) {
        Log.e(TAG, "sessionId = " + sessionId);

    }

    /**
     * NLP语义解析结果回调，原始数据
     *
     * @param sessionId
     * @param protocolData
     */
    @Override
    public void onRecognizeResult(int sessionId, ProtocolData protocolData) {
        LogUtils.d("onRecognizeResult: sessionId = " + sessionId + ",data = " + protocolData.toString());
        nluResult.setText(protocolData.toString());
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
     *
     * @return
     */
    @Override
    public boolean isUiShowing() {
        return true;
    }

    /**
     * NLP语义预处理回调，已废弃
     *
     * @param i
     * @param s
     * @param s1
     * @param jsonObject
     * @param s2
     * @return
     */
    @Override
    public int onPretreatedResult(int i, String s, String s1, JSONObject jsonObject, String s2) {
        return ReturnCode.CONTINUE;
    }

    /**
     * NLP语义预处理回调，将领域，命令名，命令参数预解析出来，方便使用
     *
     * @param sessionId
     * @param data
     * @param commandDomain
     * @param command
     * @param commandParams
     * @param question
     * @return
     */
    @Override
    public int onPretreatedResult(int sessionId, ProtocolData data, String
            commandDomain, String command, JSONObject commandParams, String question) {
        LogUtils.d("onPretreatedResult:sessionId = " + sessionId + ",commandDomain = " + commandDomain + ",command = " + command + ",commandParams = " + commandParams.toString());
        if (commandDomain.equals("AliGenie.System.Control")) {
            if (command.equals("Exit")) {
                LogUtils.d("It is exit command!");
                MediaOutputBridge.getInstance().clean();
            }
        }

        return ReturnCode.CONTINUE;
    }

    /**
     * 底层通知回调，包括账号绑定，音量设置等通知
     *
     * @param type
     * @param data
     * @param arg1
     * @param arg2
     */
    @Override
    public void onNotify(int type, Object data, int arg1, int arg2) {
        LogUtils.d("type=" + type + ",Object=" + data + ",arg1=" + arg1 + ",arg2=" + arg2);
        switch (type) {
            case IBaseView.NOTIFY_QRCODE_MESSAGE: {
                //显示二维码
                if (data != null) {
                    try {
                        Intent intent = new Intent();
                        intent.setAction(SHOW_QRCODE);
                        intent.setPackage(getPackageName());
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra(QRCODE_KEY, data.toString());
                        startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    hideUi();
                }
            }
            break;
            case IBaseView.NOTIFY_BINDER_USER: {
                //绑定设备
                if (bindDeviceListener != null && bindDeviceListener.get() != null) {
                    BindDeviceListener listener = bindDeviceListener.get();
                    listener.bindResponse(0, arg1 == 0 ? false : true, (UserData) data);
                }
            }
            case NOTIFY_TVASSIST_BIND:
                showGrantDialog(arg1);
                break;
            case NOTIFY_TVASSIST_BIND_SUCCESS:
            case NOTIFY_TVASSIST_SCREEN_CLEAN:
                dismissGrantDialog();
                break;
        }
    }

    /**
     * IUIListener只有onShow一个接口，用于给其他类提供一个面板弹出时的回调入口
     *
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
     *
     * @param isScreen
     * @param thridContext
     * @return
     */
    @Override
    public String getTvContext(boolean isScreen, String thridContext) {
        return "{\"systemInfo\":{\"area_code\":\"\",\"uuid\":\"928911AD5D41A7FD9BFCE90E0A86C96D\",\"device_model\":\"MagicBox_M18S\",\"device_firmware_version\":\"6.1.0-R-20180417.0354\",\"firmware\":\"6.1.0-R-20180417.0354\",\"edu_version_code\":2100405003,\"device_sn\":\"928911AD5D41A7FD9BFCE90E0A86C96D\",\"bcp\":\"1\",\"charge_type\":\"2,3,5\",\"from\":\"0,7\",\"device_media\":\"s265_1080p\",\"sw\":\"sw1080\",\"version_code\":2120511020},\"sceneInfo\":\"{\\\"appPackage\\\":\\\"com.alibaba.sdk.aligeniesdkdemo\\\",\\\"clientVersion\\\":2100607006,\\\"clientVersionName\\\":\\\"7.0.06\\\",\\\"clientTime\\\":1524575642306,\\\"system_locale\\\":\\\"浙江省 杭州市 市辖区\\\",\\\"deviceMode\\\":0,\\\"location\\\":\\\"浙江省 杭州市 市辖区\\\",\\\"city\\\":\\\"浙江省\\\",\\\"useApp\\\":\\\"com.yunos.tv.homeshell\\\",\\\"useAppClientVersion\\\":\\\"2100590020\\\",\\\"timezone\\\":\\\"Asia\\\\\\/Shanghai\\\",\\\"micType\\\":0}\",\"sceneExtInfo\":\"{\\\"soundType\\\":10,\\\"soundData\\\":\\\"{\\\\\\\"pageNum\\\\\\\":1,\\\\\\\"pageSize\\\\\\\":5,\\\\\\\"pageCount\\\\\\\":4}\\\",\\\"context\\\":\\\"{\\\\\\\"pageSizeMax\\\\\\\":20}\\\"}\",\"packageInfo\":\"{\\\"com.yunos.tv.appstore\\\":\\\"2101407000\\\",\\\"com.yunos.tv.yingshi.boutique\\\":\\\"2120506118\\\"}\",\"memory\":\"1991\",\"clientVersion\":\"2100607006\",\"localeInfo\":\"{\\\"language\\\":\\\"zh\\\",\\\"country\\\":\\\"CN\\\"}\",\"protocolVersion\":2}";
    }

    /**
     * 业务方预处理各个命令，由于服务端下发的多为组合命令，注意处理islast的属性
     *
     * @param sessionId
     * @param command
     * @return 参考 ReturnCode
     * CONTINUE = 0;    //数据继续向下分发
     * STOP = 1;    //数据停止向下分发，并关闭界面
     * STOP_NO_HIDE = 2;    //数据停止向下分发，保留界面
     */
    @Override
    public int pretreatedNLPResult(int sessionId, String command) {
        LogUtils.d(">>>>>pretreatedNLPResult : command=" + command,NearFieldDemoActivity.class);
        return ReturnCode.CONTINUE;
    }

    public static void registerBindDeviceListener(BindDeviceListener listener) {
        if (bindDeviceListener == null) {
            bindDeviceListener = new WeakReference<BindDeviceListener>(listener);
        }
    }

    public static void unregisterBindDeviceListener() {
        bindDeviceListener = null;
    }



    //授权验证的dialog
    private GrantDialog mGrantDialog;

    //显示授权dialog
    public void showGrantDialog(int cid) {
        LogUtils.d("showGrantDialog.cid = " + cid,NearFieldDemoActivity.class);
        if (mGrantDialog != null) {
            mGrantDialog.dismiss();
            mGrantDialog = null;
        }
        mGrantDialog = new GrantDialog(this);
        mGrantDialog.setCid(cid);
        mGrantDialog.show();
    }

    //销毁授权窗口的dialog
    public void dismissGrantDialog() {
        if (mGrantDialog != null) {
            mGrantDialog.dismiss();
            mGrantDialog = null;
        }
    }

}