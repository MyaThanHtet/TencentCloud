package com.mth.videocall;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatImageButton;

import com.mth.common.TRTCBaseActivity;
import com.mth.debug.Constant;
import com.mth.debug.GenerateTestUserSig;
import com.tencent.liteav.TXLiteAVCode;
import com.tencent.liteav.device.TXDeviceManager;
import com.tencent.rtmp.ui.TXCloudVideoView;
import com.tencent.trtc.TRTCCloud;
import com.tencent.trtc.TRTCCloudDef;
import com.tencent.trtc.TRTCCloudListener;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * TRTC视频通话的主页面
 * <p>
 * 包含如下简单功能：
 * - 进入视频通话房间{@link VideoCallingActivity#enterRoom()}
 * - 退出视频通话房间{@link VideoCallingActivity#exitRoom()}
 * - 切换前置/后置摄像头{@link VideoCallingActivity#switchCamera()}
 * - 打开/关闭摄像头{@link VideoCallingActivity#muteVideo()}
 * - 打开/关闭麦克风{@link VideoCallingActivity#muteAudio()}
 * - 显示房间内其他用户的视频画面（当前示例最多可显示6个其他用户的视频画面）{@link TRTCCloudImplListener#refreshRemoteVideoViews()}
 * <p>
 * - 详见接入文档{https://cloud.tencent.com/document/product/647/42045}
 */

/**
 * Video Call
 * <p>
 * Features:
 * - Enter a video call room: {@link VideoCallingActivity#enterRoom()}
 * - Exit a video call room: {@link VideoCallingActivity#exitRoom()}
 * - Switch between the front and rear cameras: {@link VideoCallingActivity#switchCamera()}
 * - Turn on/off the camera: {@link VideoCallingActivity#muteVideo()}
 * - Turn on/off the mic: {@link VideoCallingActivity#muteAudio()}
 * - Display the video of other users (max. 6) in the room: {@link TRTCCloudImplListener#refreshRemoteVideoViews()}
 * <p>
 * - For more information, please see the integration document {https://cloud.tencent.com/document/product/647/42045}.
 */
public class VideoCallingActivity extends TRTCBaseActivity implements View.OnClickListener {

    private static final String TAG = "VideoCallingActivity";
    private static final int OVERLAY_PERMISSION_REQ_CODE = 1234;
    private final int mUserCount = 0;
    private TextView mTextTitle;
    private TXCloudVideoView mTXCVVLocalPreviewView;
    private AppCompatImageButton mButtonMuteVideo;
    private AppCompatImageButton mButtonMuteAudio;
    private AppCompatImageButton mButtonSwitchCamera;
    private AppCompatImageButton mButtonAudioRoute;
    private AppCompatImageButton mButtonHungup;
    private TRTCCloud mTRTCCloud;
    private TXDeviceManager mTXDeviceManager;
    private boolean mIsFrontCamera = true;
    private List<String> mRemoteUidList;
    private List<TXCloudVideoView> mRemoteViewList;
    private String mRoomId;
    private String mUserId;
    private boolean mAudioRouteFlag = true;
    private FloatingView mFloatingView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.videocall_activity_calling);
        getSupportActionBar().hide();
        handleIntent();

        if (checkPermission()) {
            initView();
            enterRoom();
        }
    }

    private void handleIntent() {
        Intent intent = getIntent();
        if (null != intent) {
            if (intent.getStringExtra(Constant.USER_ID) != null) {
                mUserId = intent.getStringExtra(Constant.USER_ID);
            }
            if (intent.getStringExtra(Constant.ROOM_ID) != null) {
                mRoomId = intent.getStringExtra(Constant.ROOM_ID);
            }
        }
    }

    private void initView() {
        mTextTitle = findViewById(R.id.tv_room_number);

        mTXCVVLocalPreviewView = findViewById(R.id.txcvv_main);
        mButtonMuteVideo = findViewById(R.id.btn_mute_video);
        mButtonMuteAudio = findViewById(R.id.btn_mute_audio);
        mButtonSwitchCamera = findViewById(R.id.btn_switch_camera);
        mButtonAudioRoute = findViewById(R.id.btn_audio_route);
        mButtonHungup = findViewById(R.id.btn_hangup);
        if (!TextUtils.isEmpty(mRoomId)) {
            mTextTitle.setText(getString(R.string.videocall_roomid) + mRoomId);
        }
        mButtonMuteVideo.setOnClickListener(this);
        mButtonMuteAudio.setOnClickListener(this);
        mButtonSwitchCamera.setOnClickListener(this);
        mButtonAudioRoute.setOnClickListener(this);
        mButtonHungup.setOnClickListener(this);
        mRemoteUidList = new ArrayList<>();
        mRemoteViewList = new ArrayList<>();
        mRemoteViewList.add((TXCloudVideoView) findViewById(R.id.trtc_view_1));
        mRemoteViewList.add((TXCloudVideoView) findViewById(R.id.trtc_view_2));
        mRemoteViewList.add((TXCloudVideoView) findViewById(R.id.trtc_view_3));
        mRemoteViewList.add((TXCloudVideoView) findViewById(R.id.trtc_view_4));
        mRemoteViewList.add((TXCloudVideoView) findViewById(R.id.trtc_view_5));
        mRemoteViewList.add((TXCloudVideoView) findViewById(R.id.trtc_view_6));

        mFloatingView = new FloatingView(getApplicationContext(), R.layout.videocall_view_floating_default);
        mFloatingView.setPopupWindow(R.layout.videocall_popup_layout);
        mFloatingView.setOnPopupItemClickListener(this);
    }

    private void enterRoom() {
        mTRTCCloud = TRTCCloud.sharedInstance(getApplicationContext());
        mTRTCCloud.setListener(new TRTCCloudImplListener(VideoCallingActivity.this));
        mTXDeviceManager = mTRTCCloud.getDeviceManager();

        TRTCCloudDef.TRTCParams trtcParams = new TRTCCloudDef.TRTCParams();
        trtcParams.sdkAppId = GenerateTestUserSig.SDKAPPID;
        trtcParams.userId = mUserId;
        trtcParams.roomId = Integer.parseInt(mRoomId);
        trtcParams.userSig = GenerateTestUserSig.genTestUserSig(trtcParams.userId);

        mTRTCCloud.startLocalPreview(mIsFrontCamera, mTXCVVLocalPreviewView);
        mTRTCCloud.startLocalAudio(TRTCCloudDef.TRTC_AUDIO_QUALITY_SPEECH);
        mTRTCCloud.enterRoom(trtcParams, TRTCCloudDef.TRTC_APP_SCENE_VIDEOCALL);
    }

    @Override
    protected void onStop() {
        super.onStop();
        requestDrawOverLays();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mFloatingView != null && mFloatingView.isShown()) {
            mFloatingView.dismiss();
        }
        exitRoom();
    }

    private void exitRoom() {
        if (mTRTCCloud != null) {
            mTRTCCloud.stopLocalAudio();
            mTRTCCloud.stopLocalPreview();
            mTRTCCloud.exitRoom();
            mTRTCCloud.setListener(null);
        }
        mTRTCCloud = null;
        TRTCCloud.destroySharedInstance();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mFloatingView != null && mFloatingView.isShown()) {
            mFloatingView.dismiss();
        }
    }

    @Override
    protected void onPermissionGranted() {
        initView();
        enterRoom();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_mute_video) {
            muteVideo();
        } else if (id == R.id.btn_mute_audio) {
            muteAudio();
        } else if (id == R.id.btn_switch_camera) {
            switchCamera();
        } else if (id == R.id.btn_audio_route) {
            audioRoute();
        } else if (id == R.id.iv_return) {
            floatViewClick();
        } else if (id == R.id.btn_hangup) {
            hungup();
        }
    }

    private void floatViewClick() {
        Intent intent = new Intent(this, VideoCallingActivity.class);
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }
        try {
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void muteVideo() {
        boolean isSelected = mButtonMuteVideo.isSelected();
        if (!isSelected) {
            mTRTCCloud.stopLocalPreview();
            //   mButtonMuteVideo.setText(getString(R.string.videocall_open_camera));
            mButtonMuteVideo.setActivated(true);
        } else {
            mTRTCCloud.startLocalPreview(mIsFrontCamera, mTXCVVLocalPreviewView);

            //  mButtonMuteVideo.setText(getString(R.string.videocall_close_camera));
            mButtonMuteVideo.setActivated(false);
        }
        mButtonMuteVideo.setSelected(!isSelected);
    }

    private void muteAudio() {
        boolean isSelected = mButtonMuteAudio.isSelected();
        if (!isSelected) {
            mTRTCCloud.muteLocalAudio(true);
            mButtonMuteAudio.setActivated(true);
        } else {
            mTRTCCloud.muteLocalAudio(false);
            mButtonMuteAudio.setActivated(false);
        }
        mButtonMuteAudio.setSelected(!isSelected);
    }

    private void switchCamera() {
        mIsFrontCamera = !mIsFrontCamera;
        mTXDeviceManager.switchCamera(mIsFrontCamera);
        if (mIsFrontCamera) {
            mButtonSwitchCamera.setActivated(true);
        } else {
            mButtonSwitchCamera.setActivated(false);
        }
    }

    private void audioRoute() {
        if (mAudioRouteFlag) {
            mAudioRouteFlag = false;
            mTXDeviceManager.setAudioRoute(TXDeviceManager.TXAudioRoute.TXAudioRouteEarpiece);
            mButtonAudioRoute.setActivated(true);
        } else {
            mAudioRouteFlag = true;
            mTXDeviceManager.setAudioRoute(TXDeviceManager.TXAudioRoute.TXAudioRouteSpeakerphone);
            mButtonAudioRoute.setActivated(false);
        }
    }

    private void hungup() {
        finish();
    }

    public void requestDrawOverLays() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N && !Settings.canDrawOverlays(VideoCallingActivity.this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + VideoCallingActivity.this.getPackageName()));
            startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE);
        } else {
            showFloatingView();
        }
    }

    private void showFloatingView() {
        if (mFloatingView != null && !mFloatingView.isShown()) {
            if ((null != mTRTCCloud)) {
                mFloatingView.show();
                mFloatingView.setOnPopupItemClickListener(this);
            }
        }
    }

    private class TRTCCloudImplListener extends TRTCCloudListener {

        private final WeakReference<VideoCallingActivity> mContext;

        public TRTCCloudImplListener(VideoCallingActivity activity) {
            super();
            mContext = new WeakReference<>(activity);
        }

        @Override
        public void onUserVideoAvailable(String userId, boolean available) {
            Log.d(TAG, "onUserVideoAvailable userId " + userId + ", mUserCount " + mUserCount + ",available " + available);
            int index = mRemoteUidList.indexOf(userId);
            if (available) {
                if (index != -1) {
                    return;
                }
                mRemoteUidList.add(userId);
                refreshRemoteVideoViews();
            } else {
                if (index == -1) {
                    return;
                }
                mTRTCCloud.stopRemoteView(userId);
                mRemoteUidList.remove(index);
                refreshRemoteVideoViews();
            }

        }

        private void refreshRemoteVideoViews() {
            for (int i = 0; i < mRemoteViewList.size(); i++) {
                if (i < mRemoteUidList.size()) {
                    String remoteUid = mRemoteUidList.get(i);
                    mRemoteViewList.get(i).setVisibility(View.VISIBLE);
                    mTRTCCloud.startRemoteView(remoteUid, TRTCCloudDef.TRTC_VIDEO_STREAM_TYPE_SMALL, mRemoteViewList.get(i));
                } else {
                    mRemoteViewList.get(i).setVisibility(View.GONE);
                }
            }
        }

        @Override
        public void onError(int errCode, String errMsg, Bundle extraInfo) {
            Log.d(TAG, "sdk callback onError");
            VideoCallingActivity activity = mContext.get();
            if (activity != null) {
                Toast.makeText(activity, "onError: " + errMsg + "[" + errCode + "]", Toast.LENGTH_SHORT).show();
                if (errCode == TXLiteAVCode.ERR_ROOM_ENTER_FAIL) {
                    activity.exitRoom();
                }
            }
        }
    }
}
