package com.yunos.tv.alitvasr.account;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alibaba.ailabs.custom.account.QRCodeManager;
import com.alibaba.ailabs.geniesdk.AiLabsCore;
import com.alibaba.ailabs.geniesdk.util.LogUtils;
import com.alibaba.sdk.aligeniesdkdemo.R;
import com.bumptech.glide.Glide;
import com.yunos.tv.alitvasr.NearFieldDemoActivity;
import com.yunos.tv.alitvasr.model.binder.UserData;
import com.yunos.tv.alitvasr.ui.interfaces.BindDeviceListener;

import java.lang.ref.WeakReference;

//设备绑定页面
public class BindDeviceGuideActivity extends Activity implements BindDeviceListener {
    private static final String TAG = "BindDeviceGuideActivity";
    private static final int GUEST_USER = 0;
    private static final int MEMBER_USER = 1;
    private ImageView mDownAppQrcode;
    private ImageView mBindDeviceQrcode;
    private Handler mMainHandler = new Handler(Looper.getMainLooper());
    private LinearLayout mBindPageLayout;
    private LinearLayout mBindSuccessLayout;
    private Bitmap userIcon;
    private String userName;

    private ImageView mUserIcon;
    private TextView mUserName, mTitleText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bind_device_guide);
        mDownAppQrcode = (ImageView) findViewById(R.id.down_app_qrcode);
        mBindDeviceQrcode = (ImageView) findViewById(R.id.bind_device_qrcode);
        mBindPageLayout = (LinearLayout) findViewById(R.id.bind_page_id);
        mBindSuccessLayout = (LinearLayout) findViewById(R.id.bind_success_id);
    }


    @Override
    protected void onResume() {
        super.onResume();
        NearFieldDemoActivity.registerBindDeviceListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        NearFieldDemoActivity.unregisterBindDeviceListener();
    }

    //显示绑定的二维码
    @Override
    protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        if(intent != null && intent.getAction()!= null){
            if (intent.getAction().equals(NearFieldDemoActivity.SHOW_QRCODE)) {
                String qrcode = intent.getStringExtra(NearFieldDemoActivity.QRCODE_KEY);
                bindUser(0, qrcode);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(getIntent() != null && getIntent().getAction()!= null){
            if (getIntent().getAction().equals(NearFieldDemoActivity.SHOW_QRCODE)) {
                showQrcode();
                String qrcode = getIntent().getStringExtra(NearFieldDemoActivity.QRCODE_KEY);
                bindUser(0, qrcode);
            } else if (getIntent().getAction().equals(NearFieldDemoActivity.BIND_USER)) {
                final UserData userData = AiLabsCore.getInstance().getUserData();
                LogUtils.d("get userData=" + userData.toString());
                if (userData != null && userData.getmUserType() == MEMBER_USER) {
                    //已经绑定过，则直接显示 用户头像浮层
                    //打开一个透明activity，然后显示dialog
                    showBindSuccess(userData);
                } else {
                    showQrcode();
                    //拉取绑定二维码
                    AiLabsCore.getInstance().bindUser();
                }
            }
        }else{
            LogUtils.d("get intent action is null ");
            final UserData userData = AiLabsCore.getInstance().getUserData();
            LogUtils.d("call get userData=" + userData.toString());
            if (userData != null && userData.getmUserType() == MEMBER_USER) {
                //已经绑定过，则直接显示 用户头像浮层
                //打开一个透明activity，然后显示dialog
                showBindSuccess(userData);
            } else {
                showQrcode();
                //拉取绑定二维码
                AiLabsCore.getInstance().bindUser();
            }
        }
    }

    private void showBindSuccess(UserData userData) {
        if (userData == null) return;
        //已经绑定过，则直接显示 用户头像浮层
        //打开一个透明activity，然后显示dialog
        mBindPageLayout.setVisibility(View.GONE);
        mBindSuccessLayout.setVisibility(View.VISIBLE);
        //init view
        mUserIcon = (ImageView) findViewById(R.id.login_user_icon);
        mUserName = (TextView) findViewById(R.id.login_user_name);
        mTitleText = (TextView) findViewById(R.id.login_success_title);
//        mTitleText.setTypeface(ResUtils.getFZCYSJWRegular());
        if (userData != null) {
            String pic = userData.getmAvatar();
            if(!TextUtils.isEmpty(pic)){
                if (pic.startsWith("data:image/png;base64,")) {
                    BitmapWorkerTask bitmapWorkerTask =  new BitmapWorkerTask(mUserIcon);
                    bitmapWorkerTask.execute("0", pic);
                }else{
                    Glide.with(this).load(pic).asBitmap().placeholder(R.drawable.binduser_default)
                            .error(R.drawable.binduser_default).into(mUserIcon);
                }
            }else{
                mUserIcon.setImageResource(R.drawable.binduser_default);
            }

            userName = userData.getmNickName();
        }

        mUserName.setText(TextUtils.isEmpty(userName) ? BindDeviceGuideActivity.this.getResources().getString(R.string.login_success_page_username_defort) : userName);

    }

    //显示下载app二维码
    private void showQrcode() {
        mBindPageLayout.setVisibility(View.VISIBLE);
        mBindSuccessLayout.setVisibility(View.GONE);
        //未绑定 显示绑定页面
        String downappurll = "https://app-aicloud.alibaba.com/download";
        BitmapWorkerTask bitmapWorkerTask =  new BitmapWorkerTask(mDownAppQrcode);
        bitmapWorkerTask.execute("1", downappurll);
    }

    /**
     * 绑定用户
     *
     * @param sessionID 当前sessionID
     * @param content   生成二维码的字符串
     */
    public void bindUser(int sessionID, String content) {
        LogUtils.d("bindUser start.");
        if (!TextUtils.isEmpty(content)) {
            LogUtils.d("bindUser start." + content);

            BitmapWorkerTask bitmapWorkerTask =  new BitmapWorkerTask(mBindDeviceQrcode);
            bitmapWorkerTask.execute("1", content);

            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    mBindPageLayout.setVisibility(View.VISIBLE);
                    mBindSuccessLayout.setVisibility(View.GONE);
                }
            });
        }
    }

    /**
     * 绑定成功回调
     *
     * @param sessionID
     * @param isSuccess true 绑定成功  false 绑定失败
     */
    @Override
    public void bindResponse(int sessionID, boolean isSuccess, final UserData userData) {
        LogUtils.d("bindResponse start.");
        if (isSuccess && userData != null && userData.getmUserType() == 1) {
            LogUtils.d("bindResponse start.data=" + userData.toString());
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    showBindSuccess(userData);
                }
            });
        }
    }

    class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {
        private final WeakReference<View> imageViewReference;
        //type=0  string为图片本身数据，type=1 要显示二维码
        private String type = "0";

        public BitmapWorkerTask(View imageView) {
            imageViewReference = new WeakReference<View>(imageView);
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(String... params) {
            type = params[0];
            if(!TextUtils.isEmpty(type)){
                if(type.equals("0")){
                    return decodeSampledBitmapFromString(params[1], 240, 240);
                }else if(type.equals("1")){
                    try {
                        return QRCodeManager.create2DCode(params[1], 120, 120, null);
                    } catch (com.google.zxing.WriterException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            //当你background线程跑完以后 先看看imageview还在不在，不在 就什么也不做 等着系统回收他的资源
            //在的话 再赋值
            if (imageViewReference != null && bitmap != null) {

                ImageView imageView = (ImageView)imageViewReference.get();
                if (imageView != null) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }

        public Bitmap decodeSampledBitmapFromString(String pic, int reqWidth, int reqHeight) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            Bitmap retBitmap = null;
            if (!TextUtils.isEmpty(pic) && pic.startsWith("data:image/png;base64,")) {
                pic = pic.replace("data:image/png;base64,", "");
                byte[] picBytes = Base64.decode(pic, Base64.DEFAULT);
                if (picBytes != null) {
                    // 先把inJustDecodeBounds设置为true 取得原始图片的属性
                    options.inJustDecodeBounds = true;//不加载bitmap到内存中
                    BitmapFactory.decodeByteArray(picBytes, 0, picBytes.length, options);

                    int outWidth = options.outWidth;
                    int outHeight = options.outHeight;

                    options.inDither = false;
                    options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                    options.inSampleSize = 1;
                    LogUtils.d("outWidth = " + outWidth+"; outHeight="+outHeight+"; reqWidth="+reqWidth+"; reqHeight="+reqHeight);
                    if (outWidth != 0 && outHeight != 0 && reqWidth != 0 && reqHeight != 0)
                    {
                        int sampleSize = (outWidth/reqWidth + outHeight/reqHeight)/2;

                        LogUtils.d("sampleSize = " + sampleSize);

                        options.inSampleSize = sampleSize;
                    }

                    // 在decode的时候 别忘记直接 把这个属性改为false 否则decode出来的是null
                    options.inJustDecodeBounds = false;

                    retBitmap = BitmapFactory.decodeByteArray(picBytes, 0, picBytes.length, options);
                }
            }

            return retBitmap;
        }

    }
}

