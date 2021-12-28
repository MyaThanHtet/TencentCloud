package com.tencent.liteav.trtccalling.ui.videocall;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.Settings;

import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Group;

import com.blankj.utilcode.util.ToastUtils;
import com.squareup.picasso.Picasso;
import com.tencent.liteav.trtccalling.R;
import com.tencent.liteav.trtccalling.model.TRTCCalling;
import com.tencent.liteav.trtccalling.model.TRTCCallingDelegate;
import com.tencent.liteav.trtccalling.model.impl.TRTCCallingImpl;
import com.tencent.liteav.trtccalling.ui.FloatingView;
import com.tencent.liteav.trtccalling.ui.FloatingViewForVideo;
import com.tencent.liteav.trtccalling.ui.videocall.videolayout.TRTCVideoLayout;
import com.tencent.liteav.trtccalling.ui.videocall.videolayout.TRTCVideoLayoutManager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用于展示视频通话的主界面，通话的接听和拒绝就是在这个界面中完成的。
 *
 * @author guanyifeng
 */
public class TRTCVideoCallActivity extends AppCompatActivity implements View.OnClickListener{
    public static final int TYPE_BEING_CALLED = 1;
    public static final int TYPE_CALL         = 2;

    public static final  String PARAM_TYPE                = "type";
    public static final  String PARAM_SELF_INFO           = "self_info";
    public static final  String PARAM_USER                = "user_model";
    public static final  String PARAM_BEINGCALL_USER      = "beingcall_user_model";
    public static final  String PARAM_OTHER_INVITING_USER = "other_inviting_user_model";
    private static final int    MAX_SHOW_INVITING_USER    = 4;

    private ImageView              mMuteImg;
    private LinearLayout           mMuteLl;
    private ImageView              mHangupImg;
    private LinearLayout           mHangupLl;
    private ImageView              mHandsfreeImg;
    private LinearLayout           mHandsfreeLl;
    private ImageView              mDialingImg;
    private LinearLayout           mDialingLl;
    private TRTCVideoLayoutManager mLayoutManagerTrtc;
    private TRTCVideoLayoutManager mLayoutManagerTrtcTwo;
    private Group mInvitingGroup;
    private LinearLayout           mImgContainerLl;
    private TextView               mTimeTv;
    private ImageView              mSponsorAvatarImg;
    private TextView               mSponsorUserNameTv;
    private Group                  mSponsorGroup;
    private Runnable               mTimeRunnable;
    private int                    mTimeCount;
    private Handler                mTimeHandler;
    private HandlerThread          mTimeHandlerThread;

    /**
     * 拨号相关成员变量
     */
    private UserInfo              mSelfModel;
    private List<UserInfo>        mCallUserInfoList = new ArrayList<>(); // 呼叫方
    private final Map<String, UserInfo> mCallUserModelMap = new HashMap<>();
    private UserInfo              mSponsorUserInfo;                      // 被叫方
    private List<UserInfo>        mOtherInvitingUserInfoList;
    private int                   mCallType;
    private TRTCCalling           mTRTCCalling;
    private boolean               isHandsFree       = true;
    private boolean               isMuteMic         = false;

    private FloatingViewForVideo videoFloatingView;
    private static final int OVERLAY_PERMISSION_REQ_CODE = 1234;

    /**
     * 拨号的回调
     */
    private final TRTCCallingDelegate mTRTCCallingDelegate = new TRTCCallingDelegate() {
        @Override
        public void onError(int code, String msg) {
            //发生了错误，报错并退出该页面
            ToastUtils.showLong(getString(R.string.trtccalling_toast_call_error_msg, code, msg));
            stopCameraAndFinish();
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
                    //1.先造一个虚拟的用户添加到屏幕上
                    UserInfo model = new UserInfo();
                    model.userId = userId;
                    model.userName = userId;
                    model.userAvatar = "";
                    mCallUserInfoList.add(model);
                    mCallUserModelMap.put(model.userId, model);
                    Log.d("VIDEOCALLCHECK ","addUserToManager 1");
                    TRTCVideoLayout videoLayout = addUserToManager(model);
                    if (videoLayout == null) {
                        return;
                    }
                    videoLayout.setVideoAvailable(false);

                    TRTCVideoLayout videoLayoutTwo = addUserToManagerTwo(model);
                    if (videoLayoutTwo == null) {
                        return;
                    }
                    videoLayoutTwo.setVideoAvailable(false);

//                    her clear share preference
                    SharedPreferences.Editor pfVideofloating=getSharedPreferences("VIDEOFLOATING",MODE_PRIVATE).edit();
                    pfVideofloating.clear();
                    pfVideofloating.apply();
                }
            });
        }

        @Override
        public void onUserLeave(final String userId) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d("VIDEOCALLCHECK ","onUserLeave");
                    //1. 回收界面元素
                    mLayoutManagerTrtc.recyclerCloudViewView(userId);
//                    trtc_layout_manager_two.recyclerCloudViewView(userId);
                    mLayoutManagerTrtcTwo.recyclerCloudViewView(userId);
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
                        Log.d("VIDEOCALLCHECK ","onReject");
                        mLayoutManagerTrtc.recyclerCloudViewView(userId);
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
                        // 进入无响应环节 Enter the non-response link
                        //1. 回收界面元素
                        Log.d("VIDEOCALLCHECK ","onNoResp");
                        mLayoutManagerTrtc.recyclerCloudViewView(userId);
//                        trtc_layout_manager_two.recyclerCloudViewView(userId);
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
                // 进入无响应环节 Enter the non-response link
                //1. 回收界面元素
                Log.d("VIDEOCALLCHECK ","onLineBusy");
                mLayoutManagerTrtc.recyclerCloudViewView(userId);
//                trtc_layout_manager_two.recyclerCloudViewView(userId);
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
            stopCameraAndFinish();
        }

        @Override
        public void onCallingTimeout() {
            if (mSponsorUserInfo != null) {
                ToastUtils.showLong(getString(R.string.trtccalling_toast_user_timeout, mSponsorUserInfo.userName));
            }
            stopCameraAndFinish();
        }

        @Override
        public void onCallEnd() {
            if (mSponsorUserInfo != null) {
                ToastUtils.showLong(getString(R.string.trtccalling_toast_user_end, mSponsorUserInfo.userName));
            }
            stopCameraAndFinish();
        }

        @Override
        public void onUserVideoAvailable(final String userId, final boolean isVideoAvailable) {
            //有用户的视频开启了 A user’s video is turned on
            Log.d("VIDEOCALLCHECK ","onUserVideoAvailable");
            TRTCVideoLayout layout = mLayoutManagerTrtc.findCloudViewView(userId);
            if (layout != null) {
                Log.d("VIDEOCALLCHECK l","layout 1");
                layout.setVideoAvailable(isVideoAvailable);
                if (isVideoAvailable) {
                    mTRTCCalling.startRemoteView(userId, layout.getVideoView());
                } else {
                    mTRTCCalling.stopRemoteView(userId);
                }
            } else {

            }
            SharedPreferences.Editor passwordPref=getSharedPreferences("VIDEOFLOATING",MODE_PRIVATE).edit();
            passwordPref.putBoolean("isVideoAvailable",isVideoAvailable);
            passwordPref.putString("userId",userId);
            passwordPref.apply();
//            heren
//            if (layout2 != null) {
//                Log.d("VIDEOCALLCHECK l","layout 2");
//                layout2.setVideoAvailable(isVideoAvailable);
//                if (isVideoAvailable) {
//                    layout2.getVideoViewTWO().setVisibility(View.VISIBLE);
//                    mTRTCCalling.startRemoteView(userId, layout2.getVideoViewTWO());
//                } else {
//                    mTRTCCalling.stopRemoteView(userId);
//                }
//            } else {
//
//            }
        }

        @Override
        public void onUserAudioAvailable(String userId, boolean isVideoAvailable) {

        }

        @Override
        public void onUserVoiceVolume(Map<String, Integer> volumeMap) {
            for (Map.Entry<String, Integer> entry : volumeMap.entrySet()) {
                String          userId = entry.getKey();
                Log.d("VIDEOCALLCHECK ","onUserVoiceVolume");
                TRTCVideoLayout layout = mLayoutManagerTrtc.findCloudViewView(userId);
//                TRTCVideoLayout layout_two = trtc_layout_manager_two.findCloudViewView(userId);
                if (layout != null) {
                    layout.setAudioVolumeProgress(entry.getValue());
                }
//                if (layout_two != null) {
//                    layout_two.setAudioVolumeProgress(entry.getValue());
//                }
            }
        }
    };

    /**
     * 主动拨打给某个用户
     *
     * @param context
     * @param selfInfo
     * @param callUserInfoList
     */
    public static void startCallSomeone(Context context, UserInfo selfInfo, List<UserInfo> callUserInfoList) {
        Intent starter = new Intent(context, TRTCVideoCallActivity.class);
        starter.putExtra(PARAM_TYPE, TYPE_CALL);
        starter.putExtra(PARAM_SELF_INFO, selfInfo);
        starter.putExtra(PARAM_USER, new IntentParams(callUserInfoList));
        context.startActivity(starter);
    }

    /**
     * 作为用户被叫
     *
     * @param context
     * @param beingCallUserInfo
     */
    public static void startBeingCall(Context context, UserInfo selfUserInfo, UserInfo beingCallUserInfo, List<UserInfo> otherInvitingUserInfo) {
        Intent starter = new Intent(context, TRTCVideoCallActivity.class);
        starter.putExtra(PARAM_TYPE, TYPE_BEING_CALLED);
        starter.putExtra(PARAM_SELF_INFO, selfUserInfo);
        starter.putExtra(PARAM_BEINGCALL_USER, beingCallUserInfo);
        starter.putExtra(PARAM_OTHER_INVITING_USER, new IntentParams(otherInvitingUserInfo));
        starter.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 应用运行时，保持不锁屏、全屏化
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.trtccalling_videocall_activity_call_main);
        initStatusBar();
        initView();
        initData();
        initListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
//here still error
        SharedPreferences prefs = getSharedPreferences("VIDEOFLOATING", MODE_PRIVATE);
        Boolean isVideoAvailable=prefs.getBoolean("isVideoAvailable",false);
        String userId=prefs.getString("userId",null);
        if(isVideoAvailable && !userId.equals(null)){
//            mTRTCCalling.accept();
//            showCallingView();
            Log.d("VIDEOCALLCHECK","onResume");
            //mLayoutManagerTrtc.recyclerCloudViewView(userId);
            TRTCVideoLayout layout = mLayoutManagerTrtc.findCloudViewView(userId);
            if (layout != null) {
                Log.d("VIDEOCALLCHECK R", "layout 2");
                layout.setVideoAvailable(isVideoAvailable);
                if (isVideoAvailable) {
                    layout.getVideoView().setVisibility(View.GONE);
                    mTRTCCalling.startRemoteView(userId, layout.getVideoView());
                } else {
                    mTRTCCalling.stopRemoteView(userId);
                }
            } else {

            }
        }
        if (videoFloatingView != null && videoFloatingView.isShown()) {
            videoFloatingView.dismiss();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        SharedPreferences prefs = getSharedPreferences("VIDEOFLOATING", MODE_PRIVATE);
        Boolean isVideoAvailable=prefs.getBoolean("isVideoAvailable",false);
        String userId=prefs.getString("userId","null");
        //mLayoutManagerTrtc.recyclerCloudViewView(userId);
//        TRTCVideoLayout layout2 = trtc_layout_manager_two.findCloudViewView(userId);
        TRTCVideoLayout layout2 = mLayoutManagerTrtcTwo.findCloudViewView(userId);
        if (layout2 != null) {
            Log.d("VIDEOCALLCHECK p", "layout 2");
            layout2.setVideoAvailable(isVideoAvailable);
            if (isVideoAvailable) {
                layout2.getVideoView().setVisibility(View.VISIBLE);
                mTRTCCalling.startRemoteView(userId, layout2.getVideoView());
            } else {
                mTRTCCalling.stopRemoteView(userId);
            }
        } else {

        }
        if (videoFloatingView != null && videoFloatingView.isShown()) {
            videoFloatingView.dismiss();
        }
    }

    public void requestDrawOverLays() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + this.getPackageName()));
            startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE);
        } else {
            showFloatingView();
        }
    }

    private void showFloatingView() {
        if (videoFloatingView != null && !videoFloatingView.isShown()) {
            if ((null != videoFloatingView)) {
                videoFloatingView.show();
                videoFloatingView.setOnPopupItemClickListener(this);
            }
        }
    }

    private void initStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }

    @Override
    public void onBackPressed() {
        mTRTCCalling.hangup();
        stopCameraAndFinish();
        super.onBackPressed();
    }

    private void stopCameraAndFinish() {
        mTRTCCalling.closeCamera();
        mTRTCCalling.removeDelegate(mTRTCCallingDelegate);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimeCount();
        mTimeHandlerThread.quit();
        if (videoFloatingView != null && videoFloatingView.isShown()) {
            videoFloatingView.dismiss();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        requestDrawOverLays();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N && !Settings.canDrawOverlays(this)) {
                Toast.makeText(getApplicationContext(), "hello sdk", Toast.LENGTH_SHORT).show();
            } else {
                showFloatingView();
            }
        }
    }

    private void initListener() {
        mMuteLl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isMuteMic = !isMuteMic;
                mTRTCCalling.setMicMute(isMuteMic);
                mMuteImg.setActivated(isMuteMic);
                ToastUtils.showLong(isMuteMic ? R.string.trtccalling_toast_enable_mute : R.string.trtccalling_toast_disable_mute);
            }
        });
        mHandsfreeLl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isHandsFree = !isHandsFree;
                mTRTCCalling.setHandsFree(isHandsFree);
                mHandsfreeImg.setActivated(isHandsFree);
                ToastUtils.showLong(isHandsFree ? R.string.trtccalling_toast_use_speaker : R.string.trtccalling_toast_use_handset);
            }
        });
        mMuteImg.setActivated(isMuteMic);
        mHandsfreeImg.setActivated(isHandsFree);
    }

    private void initData() {
        // 初始化成员变量
        mTRTCCalling = TRTCCallingImpl.sharedInstance(this);
        mTRTCCalling.addDelegate(mTRTCCallingDelegate);
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
        List<String> list = new ArrayList<>();
        for (UserInfo userInfo : mCallUserInfoList) {
            list.add(userInfo.userId);
        }
        mTRTCCalling.groupCall(list, TRTCCalling.TYPE_VIDEO_CALL, "");
    }

    private void initView() {
        mMuteImg = findViewById(R.id.iv_mute);
        mMuteLl = findViewById(R.id.ll_mute);
        mHangupImg = findViewById(R.id.iv_hangup);
        mHangupLl = findViewById(R.id.ll_hangup);
        mHandsfreeImg = findViewById(R.id.iv_handsfree);
        mHandsfreeLl = findViewById(R.id.ll_handsfree);
        mDialingImg = findViewById(R.id.iv_dialing);
        mDialingLl = findViewById(R.id.ll_dialing);
        mLayoutManagerTrtc = findViewById(R.id.trtc_layout_manager);
        mInvitingGroup = findViewById(R.id.group_inviting);
        mImgContainerLl = findViewById(R.id.ll_img_container);
        mTimeTv = findViewById(R.id.tv_time);
        mSponsorAvatarImg = findViewById(R.id.iv_sponsor_avatar);
        mSponsorUserNameTv = findViewById(R.id.tv_sponsor_user_name);
        mSponsorGroup = findViewById(R.id.group_sponsor);


//        modify
        videoFloatingView = new FloatingViewForVideo(getApplicationContext(), R.layout.video_window_floating);
        videoFloatingView.setPopupWindow(R.layout.video_popup_layout);
        mLayoutManagerTrtcTwo=videoFloatingView.floatingTRTCLayoutManager();
        videoFloatingView.setOnPopupItemClickListener(this);

    }


    /**
     * 等待接听界面
     */
    public void showWaitingResponseView() {
        //1. 展示自己的画面
        Log.d("VIDEOCALLCHECK ","showWaitingResponseView");
        mLayoutManagerTrtc.setMySelfUserId(mSelfModel.userId);
//        trtc_layout_manager_two.setMySelfUserId(mSelfModel.userId);
        Log.d("VIDEOCALLCHECK ","addUserToManager 2");
        TRTCVideoLayout videoLayout = addUserToManager(mSelfModel);
        if (videoLayout == null) {
            return;
        }
        videoLayout.setVideoAvailable(true);
//        TRTCVideoLayout videoLayoutTwo = addUserToManagerTwo(mSelfModel);
//        if (videoLayoutTwo == null) {
//            return;
//        }
//        videoLayoutTwo.setVideoAvailable(true);

        mTRTCCalling.openCamera(true, videoLayout.getVideoView());

        //2. 展示对方的头像和蒙层
        mSponsorGroup.setVisibility(View.VISIBLE);
        Picasso.get().load(mSponsorUserInfo.userAvatar).into(mSponsorAvatarImg);
        mSponsorUserNameTv.setText(mSponsorUserInfo.userName);

        //3. 展示电话对应界面
        mHangupLl.setVisibility(View.VISIBLE);
        mDialingLl.setVisibility(View.VISIBLE);
        mHandsfreeLl.setVisibility(View.GONE);
        mMuteLl.setVisibility(View.GONE);
        //3. 设置对应的listener
        mHangupLl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTRTCCalling.reject();
                stopCameraAndFinish();
            }
        });
        mDialingLl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //2.接听电话
                mTRTCCalling.accept();
                showCallingView();
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
        Log.d("VIDEOCALLCHECK ","showInvitingView");
        mLayoutManagerTrtc.setMySelfUserId(mSelfModel.userId);
//        trtc_layout_manager_two.setMySelfUserId(mSelfModel.userId);
        Log.d("VIDEOCALLCHECK ","addUserToManager 3");
        TRTCVideoLayout videoLayout = addUserToManager(mSelfModel);
        if (videoLayout == null) {
            return;
        }
        videoLayout.setVideoAvailable(true);

        mTRTCCalling.openCamera(true, videoLayout.getVideoView());
//        here conflict
        mTRTCCalling.openCamera(true, videoLayout.getVideoView());

        //2. 设置底部栏
        mHangupLl.setVisibility(View.VISIBLE);
        mHangupLl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTRTCCalling.hangup();
                stopCameraAndFinish();
            }
        });
        mDialingLl.setVisibility(View.GONE);
        mHandsfreeLl.setVisibility(View.VISIBLE);
        mMuteLl.setVisibility(View.VISIBLE);
        //3. 隐藏中间他们也在界面
        hideOtherInvitingUserView();
        //4. sponsor画面也隐藏
        mSponsorGroup.setVisibility(View.GONE);
    }

    /**
     * 展示通话中的界面
     */
    public void showCallingView() {
        //1. 蒙版消失
        mSponsorGroup.setVisibility(View.GONE);
        //2. 底部状态栏
        mHangupLl.setVisibility(View.VISIBLE);
        mDialingLl.setVisibility(View.GONE);
        mHandsfreeLl.setVisibility(View.VISIBLE);
        mMuteLl.setVisibility(View.VISIBLE);

        mHangupLl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTRTCCalling.hangup();
                stopCameraAndFinish();
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
        mTimeTv.setText(getShowTime(mTimeCount));
        if (mTimeRunnable == null) {
            mTimeRunnable = new Runnable() {
                @Override
                public void run() {
                    mTimeCount++;
                    if (mTimeTv != null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mTimeTv.setText(getShowTime(mTimeCount));
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
        if (mOtherInvitingUserInfoList == null || mOtherInvitingUserInfoList.size() == 0) {
            return;
        }
        mInvitingGroup.setVisibility(View.VISIBLE);
        int squareWidth = getResources().getDimensionPixelOffset(R.dimen.trtccalling_small_image_size);
        int leftMargin  = getResources().getDimensionPixelOffset(R.dimen.trtccalling_small_image_left_margin);
        for (int index = 0; index < mOtherInvitingUserInfoList.size() && index < MAX_SHOW_INVITING_USER; index++) {
            UserInfo                  userInfo     = mOtherInvitingUserInfoList.get(index);
            ImageView                 imageView    = new ImageView(this);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(squareWidth, squareWidth);
            if (index != 0) {
                layoutParams.leftMargin = leftMargin;
            }
            imageView.setLayoutParams(layoutParams);
            Picasso.get().load(userInfo.userAvatar).into(imageView);
            mImgContainerLl.addView(imageView);
        }
    }

    private void hideOtherInvitingUserView() {
        mInvitingGroup.setVisibility(View.GONE);
    }

    private TRTCVideoLayout addUserToManager(UserInfo userInfo) {
//        here

        TRTCVideoLayout layout = mLayoutManagerTrtc.allocCloudVideoView(userInfo.userId);

        if (layout == null) {
            return null;
        }
        layout.getUserNameTv().setText(userInfo.userName);
        if (!TextUtils.isEmpty(userInfo.userAvatar)) {
            Picasso.get().load(userInfo.userAvatar).into(layout.getHeadImg());
        }
        return layout;
    }

    private TRTCVideoLayout addUserToManagerTwo(UserInfo userInfo) {
//        here
        TRTCVideoLayout layout = mLayoutManagerTrtcTwo.allocCloudVideoView(userInfo.userId);
        if (layout == null) {
            return null;
        }
        layout.getUserNameTv().setText(userInfo.userName);
        if (!TextUtils.isEmpty(userInfo.userAvatar)) {
            Picasso.get().load(userInfo.userAvatar).into(layout.getHeadImg());
        }
        return layout;
    }

    @Override
    public void onClick(View v) {
        int id=v.getId();
        if (id == R.id.btn_return) {
//            here
            Toast.makeText(TRTCVideoCallActivity.this,"Do ",Toast.LENGTH_SHORT).show();
            Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            try {
                PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
                pendingIntent.send();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

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