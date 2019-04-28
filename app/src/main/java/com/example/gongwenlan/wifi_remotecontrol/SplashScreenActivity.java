package com.example.gongwenlan.wifi_remotecontrol;

import android.app.Activity;
import android.app.Dialog;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import java.lang.ref.WeakReference;

public class SplashScreenActivity extends AppCompatActivity {
    private static Dialog mSplashDialog;
    private static WeakReference<Activity> mActivity;
    private static ProgressBar pb;

//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_splash_screen);
//    }

    public static void show(final Activity activity, final int themeResId) {
        if (activity == null) return;
        mActivity = new WeakReference<Activity>(activity);
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!activity.isFinishing()) {
                    mSplashDialog = new Dialog(activity, themeResId);
                    mSplashDialog.setContentView(R.layout.activity_splash_screen);
                    mSplashDialog.setCancelable(false);
                    pb = (ProgressBar) mSplashDialog.findViewById(R.id.progressBar);

                    if (!mSplashDialog.isShowing()) {
                        mSplashDialog.show();
                    }
                }
            }
        });
    }

    public static void setProgressBar(int progress){
        if(pb != null){
            pb.setProgress(progress);
        }
    }

    public static void hideProgressBar(){
        if(pb != null){
            pb.setVisibility(View.INVISIBLE);
        }
    }

    public static void show(final Activity activity, final boolean fullScreen) {
        int resourceId = fullScreen ? R.style.SplashScreen_Fullscreen : R.style.SplashScreen_SplashTheme;

        show(activity, resourceId);
    }

    public static void show(final Activity activity) {
        show(activity, false);
    }

    public static void hide(Activity activity) {
        if (activity == null) {
            if (mActivity == null) {
                return;
            }
            activity = mActivity.get();
        }

        if (activity == null) return;

        final Activity _activity = activity;

        _activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mSplashDialog != null && mSplashDialog.isShowing()) {
                    boolean isDestroyed = false;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        isDestroyed = _activity.isDestroyed();
                    }

                    if (!_activity.isFinishing() && !isDestroyed) {
                        mSplashDialog.dismiss();
                    }
                    mSplashDialog = null;
                }
            }
        });
    }
}
