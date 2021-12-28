package com.tencent.liteav.trtccalling.ui.audiocall;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Group;

import com.blankj.utilcode.util.CollectionUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.squareup.picasso.Picasso;
import com.tencent.liteav.trtccalling.R;
import com.tencent.liteav.trtccalling.model.TRTCCalling;
import com.tencent.liteav.trtccalling.model.TRTCCallingDelegate;
import com.tencent.liteav.trtccalling.ui.FloatingView;
import com.tencent.liteav.trtccalling.ui.audiocall.audiolayout.TRTCAudioLayout;
import com.tencent.liteav.trtccalling.ui.audiocall.audiolayout.TRTCAudioLayoutManager;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用于展示语音通话的主界面，通话的接听和拒绝就是在这个界面中完成的。
 */
public class TRTCAudioCallActivity extends AppCompatActivity {
    public static final String PARAM_TYPE = "type";
    public static final String PARAM_USER = "user_model";
    public static final String PARAM_SELF_INFO = "self_info";
    public static final String PARAM_BEINGCALL_USER = "beingcall_user_model";
    public static final String PARAM_OTHER_INVITING_USER = "other_inviting_user_model";
    public static final int TYPE_BEING_CALLED = 1;
    public static final int TYPE_CALL = 2;
    private static final String TAG = TRTCAudioCallActivity.class.getName();
    private static final int MAX_SHOW_INVITING_USER = 2;
    private static final int OVERLAY_PERMISSION_REQ_CODE = 1234;
    private static final int OVERLAY_PERMISSION_SHARE_REQ_CODE = 1235;
    private FloatingView mFloatingView;
    private ImageView mImageMute;
    private ImageView mImageHangup;
    private LinearLayout mLayoutMute;
    private LinearLayout mLayoutHangup;
    private ImageView mImageHandsFree;
    private LinearLayout mLayoutHandsFree;
    private ImageView mImageDialing;
    private LinearLayout mLayoutDialing;
    private TRTCAudioLayoutManager mLayoutManagerTRTC;
    private Group mGroupInviting;
    private LinearLayout mLayoutImgContainer;
    private TextView mTextTime;
    private Runnable mTimeRunnable;
    private int mTimeCount;
    private Handler mTimeHandler;
    private HandlerThread mTimeHandlerThread;
    private UserInfo mSelfModel;
    private List<UserInfo> mCallUserInfoList = new ArrayList<>(); // 呼叫方
    private final Map<String, UserInfo> mCallUserModelMap = new HashMap<>();
    private UserInfo mSponsorUserInfo;                      // 被叫方
    private List<UserInfo> mOtherInvitingUserInfoList;
    private int mCallType;
    private TRTCCalling mTRTCCalling;
    private boolean isHandsFree = true;
    private boolean isMuteMic = false;
    private AssetFileDescriptor mAssetFileDescriptor;
    private MediaPlayer mediaPlayer;
    /**
     * 拨号的回调
     */
    private final TRTCCallingDelegate mTRTCAudioCallListener = new TRTCCallingDelegate() {
        @Override
        public void onError(int code, String msg) {
            //发生了错误，报错并退出该页面
            ToastUtils.showLong(getString(R.string.trtccalling_toast_call_error_msg, code, msg));
            finish();
        }

        @Override
        public void onInvited(String sponsor, List<String> userIdList, boolean isFromGroup, int callType) {
        }

        @Override
        public void onGroupCallInviteeListUpdate(List<String> userIdList) {
        }

        @Override
        public void onUserEnter(final String userId) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showCallingView();
                    TRTCAudioLayout layout = mLayoutManagerTRTC.findAudioCallLayout(userId);
                    if (layout != null) {
                        layout.stopLoading();
                    } else {
                        UserInfo model = new UserInfo();
                        model.userId = userId;
                        model.userName = userId;
                        model.userAvatar = "";
                        mCallUserInfoList.add(model);
                        mCallUserModelMap.put(model.userId, model);
                        addUserToManager(model);
                    }
                }
            });
        }

        @Override
        public void onUserLeave(final String userId) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //1. 回收界面元素
                    mLayoutManagerTRTC.recyclerAudioCallLayout(userId);
                    //2. 删除用户model
                    UserInfo userInfo = mCallUserModelMap.remove(userId);
                    if (userInfo != null) {
                        mCallUserInfoList.remove(userInfo);
                    }
                }
            });
        }

        @Override
        public void onReject(final String userId) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mCallUserModelMap.containsKey(userId)) {
                        // 进入拒绝环节
                        //1. 回收界面元素
                        mLayoutManagerTRTC.recyclerAudioCallLayout(userId);
                        //2. 删除用户model
                        UserInfo userInfo = mCallUserModelMap.remove(userId);
                        if (userInfo != null) {
                            mCallUserInfoList.remove(userInfo);
                            ToastUtils.showLong(getString(R.string.trtccalling_toast_user_reject_call, userInfo.userName));
                        }
                    }
                }
            });
        }

        @Override
        public void onNoResp(final String userId) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mCallUserModelMap.containsKey(userId)) {
                        // 进入无响应环节
                        //1. 回收界面元素
                        mLayoutManagerTRTC.recyclerAudioCallLayout(userId);
                        //2. 删除用户model
                        UserInfo userInfo = mCallUserModelMap.remove(userId);
                        if (userInfo != null) {
                            mCallUserInfoList.remove(userInfo);
                            ToastUtils.showLong(getString(R.string.trtccalling_toast_user_not_response, userInfo.userName));
                        }
                    }
                }
            });
        }

        @Override
        public void onLineBusy(String userId) {
            if (mCallUserModelMap.containsKey(userId)) {
                // 进入无响应环节
                //1. 回收界面元素
                mLayoutManagerTRTC.recyclerAudioCallLayout(userId);
                //2. 删除用户model
                UserInfo userInfo = mCallUserModelMap.remove(userId);
                if (userInfo != null) {
                    mCallUserInfoList.remove(userInfo);
                    ToastUtils.showLong(getString(R.string.trtccalling_toast_user_busy, userInfo.userName));
                }
            }
        }

        @Override
        public void onCallingCancel() {
            if (mSponsorUserInfo != null) {
                ToastUtils.showLong(getString(R.string.trtccalling_toast_user_cancel_call, mSponsorUserInfo.userName));
            }
            abort();
            finish();
        }

        @Override
        public void onCallingTimeout() {
            if (mSponsorUserInfo != null) {
                ToastUtils.showLong(getString(R.string.trtccalling_toast_user_timeout, mSponsorUserInfo.userName));
            }
            abort();
            finish();
        }

        @Override
        public void onCallEnd() {
            if (mSponsorUserInfo != null) {
                ToastUtils.showLong(getString(R.string.trtccalling_toast_user_end, mSponsorUserInfo.userName));
            }
            finish();
        }

        @Override
        public void onUserVideoAvailable(String userId, boolean isVideoAvailable) {
        }

        @Override
        public void onUserAudioAvailable(String userId, boolean isVideoAvailable) {

        }

        @Override
        public void onUserVoiceVolume(Map<String, Integer> volumeMap) {
            for (Map.Entry<String, Integer> entry : volumeMap.entrySet()) {
                String userId = entry.getKey();
                TRTCAudioLayout layout = mLayoutManagerTRTC.findAudioCallLayout(userId);
                if (layout != null) {
                    layout.setAudioVolume(entry.getValue());
                }
            }
        }
    };

    /**
     * 主动拨打给某个用户
     *
     * @param context
     * @param selfModel
     * @param callUserInfoList
     */
    public static void startCallSomeone(Context context, UserInfo selfModel, List<UserInfo> callUserInfoList) {
        Intent starter = new Intent(context, TRTCAudioCallActivity.class);
        starter.putExtra(PARAM_TYPE, TYPE_CALL);
        starter.putExtra(PARAM_SELF_INFO, selfModel);
        starter.putExtra(PARAM_USER, new IntentParams(callUserInfoList));
        context.startActivity(starter);
    }

    /**
     * 作为用户被叫
     */
    public static void startBeingCall(Context context, UserInfo selfUserInfo, UserInfo beingCallUserInfo, List<UserInfo> otherInvitingUserInfo) {
        Intent starter = new Intent(context, TRTCAudioCallActivity.class);
        starter.putExtra(PARAM_TYPE, TYPE_BEING_CALLED);
        starter.putExtra(PARAM_BEINGCALL_USER, beingCallUserInfo);
        starter.putExtra(PARAM_SELF_INFO, selfUserInfo);
        starter.putExtra(PARAM_OTHER_INVITING_USER, new IntentParams(otherInvitingUserInfo));
        starter.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 应用运行时，保持不锁屏、全屏化
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.trtccalling_audiocall_activity_call_main);
        initStatusBar();
        initView();
        initData();
        initListener();
        mFloatingView = new FloatingView(getApplicationContext(), R.layout.audio_call_window_floating);
        // mFloatingView.setPopupWindow(R.layout.screenshare_popup_layout);

        mFloatingView.setOnPopupItemClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(TRTCAudioCallActivity.this, TRTCAudioCallActivity.class);
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    //intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                }
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }

    public void requestDrawOverLays() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N && !Settings.canDrawOverlays(TRTCAudioCallActivity.this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + TRTCAudioCallActivity.this.getPackageName()));
            startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE);
        } else {
            showFloatingView();
        }
    }

    private void showFloatingView() {
        if (mFloatingView != null && !mFloatingView.isShown()) {
            mFloatingView.show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N && !Settings.canDrawOverlays(TRTCAudioCallActivity.this)) {
                Toast.makeText(getApplicationContext(), "Error", Toast.LENGTH_SHORT).show();
            } else {
                showFloatingView();
            }
        } else if (resultCode == OVERLAY_PERMISSION_SHARE_REQ_CODE) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N && !Settings.canDrawOverlays(TRTCAudioCallActivity.this)) {
                Toast.makeText(getApplicationContext(), "Error", Toast.LENGTH_SHORT).show();
            } else {
                //screenCapture();
            }
        }
    }


    private void bell() {
        try {
            mAssetFileDescriptor = getAssets().openFd("dial.mp3");
            mediaPlayer = new MediaPlayer();
            mediaPlayer.reset();
            mediaPlayer.setDataSource(mAssetFileDescriptor.getFileDescriptor(), mAssetFileDescriptor.getStartOffset(), mAssetFileDescriptor.getLength());
            mediaPlayer.prepare();
            mediaPlayer.start();
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer arg0) {
                    mediaPlayer.start();
                    mediaPlayer.setLooping(true);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void abort() {
        try {
            mediaPlayer.stop();
        } catch (Exception e) {

        }
        mediaPlayer.release();
    }

    private void initStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }

    @Override
    public void onBackPressed() {
        // 退出这个界面的时候，需要挂断
        //  mTRTCCalling.hangup();
        requestDrawOverLays();
        // moveTaskToBack(true);
        Log.d("CDA", "onBackPressed Called");
        Intent setIntent = new Intent("com.tencent.liteav.action.portals");
        setIntent.addCategory(Intent.CATEGORY_DEFAULT);
        setIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(setIntent);
    }

    @Override
    protected void onStop() {
        super.onStop();
        requestDrawOverLays();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mTRTCCalling.removeDelegate(mTRTCAudioCallListener);
        stopTimeCount();
        mTimeHandlerThread.quit();
        try {
            mAssetFileDescriptor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (mFloatingView != null && mFloatingView.isShown()) {
            mFloatingView.dismiss();
        }
    }

    @Override
    protected void onResume() {

        if (mFloatingView != null && mFloatingView.isShown()) {
            mFloatingView.dismiss();
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void initListener() {
        mLayoutMute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isMuteMic = !isMuteMic;
                mTRTCCalling.setMicMute(isMuteMic);
                mImageMute.setActivated(isMuteMic);
                ToastUtils.showLong(isMuteMic ? R.string.trtccalling_toast_enable_mute : R.string.trtccalling_toast_disable_mute);
            }
        });
        mLayoutHandsFree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isHandsFree = !isHandsFree;
                mTRTCCalling.setHandsFree(isHandsFree);
                mImageHandsFree.setActivated(isHandsFree);
                ToastUtils.showLong(isHandsFree ? R.string.trtccalling_toast_use_speaker : R.string.trtccalling_toast_use_handset);
            }
        });
        mImageMute.setActivated(isMuteMic);
        mImageHandsFree.setActivated(isHandsFree);
    }

    private void initData() {
        // 初始化成员变量
        mTRTCCalling = TRTCCalling.sharedInstance(this);
        mTRTCCalling.addDelegate(mTRTCAudioCallListener);
        mTimeHandlerThread = new HandlerThread("time-count-thread");
        mTimeHandlerThread.start();
        mTimeHandler = new Handler(mTimeHandlerThread.getLooper());
        // 初始化从外界获取的数据
        Intent intent = getIntent();
        //自己的资料
        mSelfModel = (UserInfo) intent.getSerializableExtra(PARAM_SELF_INFO);
        mCallType = intent.getIntExtra(PARAM_TYPE, TYPE_BEING_CALLED);
        if (mCallType == TYPE_BEING_CALLED) {
            // 作为被叫
            mSponsorUserInfo = (UserInfo) intent.getSerializableExtra(PARAM_BEINGCALL_USER);
            IntentParams params = (IntentParams) intent.getSerializableExtra(PARAM_OTHER_INVITING_USER);
            if (params != null) {
                mOtherInvitingUserInfoList = params.mUserInfos;
            }
            showWaitingResponseView();
        } else {
            // 主叫方
            IntentParams params = (IntentParams) intent.getSerializableExtra(PARAM_USER);
            if (params != null) {
                mCallUserInfoList = params.mUserInfos;
                for (UserInfo userInfo : mCallUserInfoList) {
                    mCallUserModelMap.put(userInfo.userId, userInfo);
                }
                startInviting();
                showInvitingView();
            }
        }
    }

    private void startInviting() {
        for (UserInfo userInfo : mCallUserInfoList) {
            mTRTCCalling.call(userInfo.userId, TRTCCalling.TYPE_AUDIO_CALL);
        }
    }

    private void initView() {
        mImageMute = findViewById(R.id.img_mute);
        mLayoutMute = findViewById(R.id.ll_mute);
        mImageHangup = findViewById(R.id.img_hangup);
        mLayoutHangup = findViewById(R.id.ll_hangup);
        mImageHandsFree = findViewById(R.id.img_handsfree);
        mLayoutHandsFree = findViewById(R.id.ll_handsfree);
        mImageDialing = findViewById(R.id.img_dialing);
        mLayoutDialing = findViewById(R.id.ll_dialing);
        mLayoutManagerTRTC = findViewById(R.id.trtc_layout_manager);
        mGroupInviting = findViewById(R.id.group_inviting);
        mLayoutImgContainer = findViewById(R.id.ll_img_container);
        mTextTime = findViewById(R.id.tv_time);

    }


    /**
     * 等待接听界面
     */

    //incoming call
    public void showWaitingResponseView() {
        bell();
        //1. 展示对方的画面
        TRTCAudioLayout layout = mLayoutManagerTRTC.allocAudioCallLayout(mSponsorUserInfo.userId);
        layout.setUserId(mSponsorUserInfo.userName);
        Picasso.get().load(mSponsorUserInfo.userAvatar).into(layout.getRippleImageView());
        layout.getRippleBackground().startRippleAnimation();

        //2. 展示电话对应界面
        mLayoutHangup.setVisibility(View.VISIBLE);
        mLayoutDialing.setVisibility(View.VISIBLE);
        mLayoutHandsFree.setVisibility(View.GONE);
        mLayoutMute.setVisibility(View.GONE);
        //3. 设置对应的listener
        mLayoutHangup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTRTCCalling.reject();
                mTRTCCalling.removeDelegate(mTRTCAudioCallListener);
                stopTimeCount();
                abort();
                finish();
            }
        });
        mLayoutDialing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                abort();

                //1.分配自己的画面
                mLayoutManagerTRTC.setMySelfUserId(mSelfModel.userId);
                // addUserToManager(mSelfModel);
                //2.接听电话
                mTRTCCalling.accept();
                TRTCAudioLayout layout = mLayoutManagerTRTC.findAudioCallLayout(mSponsorUserInfo.userId);
                layout.setUserId(mSponsorUserInfo.userName);
                Picasso.get().load(mSponsorUserInfo.userAvatar).into(layout.getImageView());
                layout.getRippleImageView().setVisibility(View.GONE);
                layout.getRippleBackground().stopRippleAnimation();
                showCallingView();
                /*TRTCAudioLayout layout = mLayoutManagerTRTC.findAudioCallLayout(mSelfModel.userId);
                layout.getRippleImageView().setVisibility(View.GONE);
                layout.getImageView().setVisibility(View.VISIBLE);*/
            }
        });
        //4. 展示其他用户界面
        showOtherInvitingUserView();
    }

    /**
     * 展示邀请列表
     */
    public void showInvitingView() {
        //1. 展示自己的界面
        mLayoutManagerTRTC.setMySelfUserId(mSelfModel.userId);

/*
     // to hide self avatar in calling
       addUserToManager(mSelfModel);
*/

        //2. 展示对方的画面
        for (UserInfo userInfo : mCallUserInfoList) {
            TRTCAudioLayout layout = addUserToManager(userInfo);
            layout.startLoading();
        }
        //3. 设置底部栏
        mLayoutHangup.setVisibility(View.VISIBLE);
        mLayoutHangup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTRTCCalling.hangup();
                mTRTCCalling.removeDelegate(mTRTCAudioCallListener);
                stopTimeCount();
                finish();
            }
        });
        mLayoutDialing.setVisibility(View.GONE);
        mLayoutHandsFree.setVisibility(View.GONE);
        mLayoutMute.setVisibility(View.GONE);
        //4. 隐藏中间他们也在界面
        hideOtherInvitingUserView();
    }

    /**
     * 展示通话中的界面
     */
    public void showCallingView() {
        mLayoutHangup.setVisibility(View.VISIBLE);
        mLayoutDialing.setVisibility(View.GONE);
        mLayoutHandsFree.setVisibility(View.VISIBLE);
        mLayoutMute.setVisibility(View.VISIBLE);

        mLayoutHangup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTRTCCalling.hangup();
                mTRTCCalling.removeDelegate(mTRTCAudioCallListener);
                stopTimeCount();
                finish();
            }
        });
        showTimeCount();
        hideOtherInvitingUserView();
    }

    private void showTimeCount() {
        if (mTimeRunnable != null) {
            return;
        }
        mTimeCount = 0;
        mTextTime.setText(getShowTime(mTimeCount));
        if (mTimeRunnable == null) {
            mTimeRunnable = new Runnable() {
                @Override
                public void run() {
                    mTimeCount++;
                    if (mTextTime != null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mTextTime.setText(getShowTime(mTimeCount));

                                if (mFloatingView != null && mFloatingView.isShown()) {
                                    mFloatingView.setTimer(getShowTime(mTimeCount));
                                }

                            }
                        });
                    }
                    mTimeHandler.postDelayed(mTimeRunnable, 1000);
                }
            };
        }
        mTimeHandler.postDelayed(mTimeRunnable, 1000);
    }

    private void stopTimeCount() {
        mTimeHandler.removeCallbacks(mTimeRunnable);
        mTimeRunnable = null;
    }

    private String getShowTime(int count) {
        return getString(R.string.trtccalling_called_time_format, count / 60, count % 60);
    }

    private void showOtherInvitingUserView() {
        if (CollectionUtils.isEmpty(mOtherInvitingUserInfoList)) {
            return;
        }
        mGroupInviting.setVisibility(View.VISIBLE);
        int squareWidth = getResources().getDimensionPixelOffset(R.dimen.trtccalling_small_image_size);
        int leftMargin = getResources().getDimensionPixelOffset(R.dimen.trtccalling_small_image_left_margin);
        for (int index = 0; index < mOtherInvitingUserInfoList.size() && index < MAX_SHOW_INVITING_USER; index++) {
            UserInfo userInfo = mOtherInvitingUserInfoList.get(index);
            ImageView imageView = new ImageView(this);
            //  CircleImageView circleImageView = new CircleImageView(this);

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(squareWidth, squareWidth);
            if (index != 0) {
                layoutParams.leftMargin = leftMargin;
            }
            imageView.setLayoutParams(layoutParams);
            Picasso.get().load(userInfo.userAvatar).into(imageView);
            mLayoutImgContainer.addView(imageView);
        }
    }

    private void hideOtherInvitingUserView() {
        mGroupInviting.setVisibility(View.GONE);
    }


    private TRTCAudioLayout addUserToManager(UserInfo userInfo) {
        TRTCAudioLayout layout = mLayoutManagerTRTC.allocAudioCallLayout(userInfo.userId);
        if (layout == null) {
            return null;
        }
        layout.setUserId(userInfo.userName);
        Picasso.get().load(userInfo.userAvatar).into(layout.getImageView());
        return layout;
    }


    private static class IntentParams implements Serializable {
        public List<UserInfo> mUserInfos;

        public IntentParams(List<UserInfo> userInfos) {
            mUserInfos = userInfos;
        }
    }

    public static class UserInfo implements Serializable {
        public String userId;
        public String userAvatar;
        public String userName;
    }
}
