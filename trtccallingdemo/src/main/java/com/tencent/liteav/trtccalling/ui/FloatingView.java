package com.tencent.liteav.trtccalling.ui;


import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.tencent.liteav.trtccalling.R;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class FloatingView extends FrameLayout implements GestureDetector.OnGestureListener {
    private final Context mContext;
    private WindowManager mWindowManager;
    private final GestureDetector mGestureDetector;
    private WindowManager.LayoutParams mLayoutParams;
    private float mLastX;
    private float mLastY;
    private boolean mIsShowing = false;

    public FloatingView(Context context) {
        super(context);
        this.mContext = context;
        this.mGestureDetector = new GestureDetector(context, this);
    }

    public FloatingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        this.mGestureDetector = new GestureDetector(context, this);
    }

    public FloatingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
        this.mGestureDetector = new GestureDetector(context, this);
    }

    public FloatingView(Context context, int viewResId) {
        super(context);
        this.mContext = context;
        View.inflate(context, viewResId, this);
        this.mGestureDetector = new GestureDetector(context, this);
    }

    public void showView(View view) {
        showView(view, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
    }

    public void showView(View view, int width, int height) {
        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        int type = WindowManager.LayoutParams.TYPE_TOAST;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
            type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        mLayoutParams = new WindowManager.LayoutParams(type);
        mLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        mLayoutParams.flags |= WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
        mLayoutParams.width = width;
        mLayoutParams.height = height;
        mLayoutParams.format = PixelFormat.TRANSLUCENT;
        mWindowManager.addView(view, mLayoutParams);
    }

    public void hideView() {
        if (null != mWindowManager) {
            mWindowManager.removeViewImmediate(this);
        }
        mWindowManager = null;
    }

    public void setTimer(String timer) {
        TextView timerTV = findViewById(R.id.time_for_me);
        timerTV.setText(timer);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
    }

    @Override
    public boolean onDown(MotionEvent e) {
        mLastX = e.getRawX();
        mLastY = e.getRawY();
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        float nowX, nowY, tranX, tranY;
        nowX = e2.getRawX();
        nowY = e2.getRawY();
        tranX = nowX - mLastX;
        tranY = nowY - mLastY;
        mLayoutParams.x += tranX;
        mLayoutParams.y += tranY;
        mWindowManager.updateViewLayout(this, mLayoutParams);
        mLastX = nowX;
        mLastY = nowY;

        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }


    public void setOnPopupItemClickListener(OnClickListener listener) {
     /*   ImageView imageView=findViewById(R.id.return_home);
        imageView.setOnClickListener(listener);*/

        RelativeLayout relativeLayout = findViewById(R.id.activity_main);
        relativeLayout.setOnClickListener(listener);
    }

    public void show() {
        if (!mIsShowing) {
            showView(this);
        }
        mIsShowing = true;
    }

    public void dismiss() {
        if (mIsShowing) {
            hideView();
        }
        mIsShowing = false;
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }


}
