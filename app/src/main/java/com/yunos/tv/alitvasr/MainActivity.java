package com.yunos.tv.alitvasr;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.alibaba.sdk.aligeniesdkdemo.R;

public class MainActivity extends AppCompatActivity {
    private static String TAG = "MainActivity";
    private final int requestCode = 1027;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        if (!hasAudioPermission()) {
            requestAudioPermission();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(code, permissions, grantResults);
        if (requestCode == code) {
            for (int i = 0; i < permissions.length; i++) {
                if (Manifest.permission.RECORD_AUDIO.equals(permissions[i]) && grantResults[i] == 0) {
                    Log.i(TAG, "onRequestPermissionsResult RECORD_AUDIO permission GRANTED!");
                    Toast.makeText(this, "permission granted!", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }
        Toast.makeText(this, "permission not granted!", Toast.LENGTH_SHORT).show();
    }


    private boolean hasAudioPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                return false;
            }
        }
        return true;
    }

    private void requestAudioPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
                Toast.makeText(this, "App required access to audio", Toast.LENGTH_SHORT).show();
            }
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, requestCode);
        }
    }

    public void onButton1Click(View view) {
        startActivity(new Intent(this, NearFieldDemoActivity.class));
    }

    public void onButton2Click(View view) {
        startActivity(new Intent(this, RawAudioDataDemoActivity.class));
    }

    public void onButton3Click(View view) {
        startActivity(new Intent(this, ProcessedAudioDataDemoActivity.class));
    }

    public void onButton4Click(View view) {
        startActivity(new Intent(this, TextStreamDataDemoActivity.class));
    }
}
