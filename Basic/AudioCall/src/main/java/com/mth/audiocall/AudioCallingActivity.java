package com.mth.audiocall;

import static com.tencent.liteav.trtccalling.ui.audiocall.TRTCAudioCallActivity.PARAM_BEINGCALL_USER;
import static com.tencent.liteav.trtccalling.ui.audiocall.TRTCAudioCallActivity.PARAM_OTHER_INVITING_USER;
import static com.tencent.liteav.trtccalling.ui.audiocall.TRTCAudioCallActivity.PARAM_SELF_INFO;
import static com.tencent.liteav.trtccalling.ui.audiocall.TRTCAudioCallActivity.PARAM_TYPE;
import static com.tencent.liteav.trtccalling.ui.audiocall.TRTCAudioCallActivity.TYPE_BEING_CALLED;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatImageButton;

import com.mth.common.TRTCBaseActivity;
import com.mth.debug.Constant;
import com.mth.debug.GenerateTestUserSig;
import com.tencent.liteav.TXLiteAVCode;
import com.tencent.liteav.trtccalling.model.TRTCCalling;
import com.tencent.liteav.trtccalling.model.TRTCCallingDelegate;
import com.tencent.liteav.trtccalling.ui.audiocall.TRTCAudioCallActivity;
import com.tencent.trtc.TRTCCloud;
import com.tencent.trtc.TRTCCloudDef;
import com.tencent.trtc.TRTCCloudListener;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * TRTC 语音通话的主页面
 * <p>
 * 包含如下简单功能：
 * - 进入语音通话房间{@link AudioCallingActivity#enterRoom()}
 * - 退出语音通话房间{@link AudioCallingActivity#exitRoom()}
 * - 关闭/打开麦克风{@link AudioCallingActivity#muteAudio()}
 * - 免提(听筒/扬声器切换){@link AudioCallingActivity#audioRoute()}
 * <p>
 * - 详见接入文档{https://cloud.tencent.com/document/product/647/42047}
 */

/**
 * Audio Call
 * <p>
 * Features:
 * - Enter an audio call room: {@link .AudioCallingActivity#enterRoom()}
 * - Exit an audio call room: {@link .AudioCallingActivity#exitRoom()}
 * - Turn on/off the mic: {@link .AudioCallingActivity#muteAudio()}
 * - Switch between the speaker (hands-free mode) and receiver: {@link .audiocall.AudioCallingActivity#audioRoute()}
 * <p>
 * - For more information, please see the integration document {https://cloud.tencent.com/document/product/647/42047}.
 */
public class AudioCallingActivity extends TRTCBaseActivity implements View.OnClickListener {

    private static final String TAG = "AudioCallingActivity";
    private final List<String> mRemoteUserList = new ArrayList<>();
    AnimationDrawable animationDrawable;
    private TextView mTextTitle;
    private List<LinearLayout> mListUserView;
    private List<TextView> mListUserIdView;
    private List<ImageView> mListUserAvatar;
    private AppCompatImageButton mButtonMuteAudio;
    private AppCompatImageButton mButtonAudioRoute;
    private AppCompatImageButton mButtonHangUp;
    private TRTCCloud mTRTCCloud;
    private String mRoomId;
    private String mUserId;
    private boolean mAudioRouteFlag = true;
    private TRTCCalling mTRTCCalling;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.audiocall_activity_calling);
        getSupportActionBar().hide();
        handleIntent();

        if (checkPermission()) {
            initView();
            enterRoom();
        }

        RelativeLayout relativeLayout = findViewById(R.id.main_content_relativelayout);
        animationDrawable = (AnimationDrawable) relativeLayout.getBackground();
        animationDrawable.setEnterFadeDuration(2000);
        animationDrawable.setExitFadeDuration(4000);
        animationDrawable.start();
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

    protected void initView() {
        mTextTitle = findViewById(R.id.tv_room_number);
        mButtonMuteAudio = findViewById(R.id.btn_mute_audio);
        mButtonAudioRoute = findViewById(R.id.btn_audio_route);
        mButtonHangUp = findViewById(R.id.btn_hangup);

        mListUserView = new ArrayList<>();
        mListUserIdView = new ArrayList<>();
        mListUserAvatar = new ArrayList<>();

        mListUserView.add(findViewById(R.id.ll_user1));
        mListUserView.add(findViewById(R.id.ll_user2));
        mListUserView.add(findViewById(R.id.ll_user3));
        mListUserView.add(findViewById(R.id.ll_user4));
        mListUserView.add(findViewById(R.id.ll_user5));
        mListUserView.add(findViewById(R.id.ll_user6));
        mListUserView.add(findViewById(R.id.ll_user7));
        mListUserView.add(findViewById(R.id.ll_user8));
        mListUserView.add(findViewById(R.id.ll_user9));


        mListUserIdView.add(findViewById(R.id.tv_user1));
        mListUserIdView.add(findViewById(R.id.tv_user2));
        mListUserIdView.add(findViewById(R.id.tv_user3));
        mListUserIdView.add(findViewById(R.id.tv_user4));
        mListUserIdView.add(findViewById(R.id.tv_user5));
        mListUserIdView.add(findViewById(R.id.tv_user6));
        mListUserIdView.add(findViewById(R.id.tv_user7));
        mListUserIdView.add(findViewById(R.id.tv_user8));
        mListUserIdView.add(findViewById(R.id.tv_user9));

        mListUserAvatar.add(findViewById(R.id.iv_user1));
        mListUserAvatar.add(findViewById(R.id.iv_user2));
        mListUserAvatar.add(findViewById(R.id.iv_user3));
        mListUserAvatar.add(findViewById(R.id.iv_user4));
        mListUserAvatar.add(findViewById(R.id.iv_user5));
        mListUserAvatar.add(findViewById(R.id.iv_user6));
        mListUserAvatar.add(findViewById(R.id.iv_user7));
        mListUserAvatar.add(findViewById(R.id.iv_user8));
        mListUserAvatar.add(findViewById(R.id.iv_user9));

        mButtonAudioRoute.setSelected(mAudioRouteFlag);
        if (!TextUtils.isEmpty(mRoomId)) {
            mTextTitle.setText(getString(R.string.audiocall_roomid) + mRoomId);
        }

        mButtonMuteAudio.setOnClickListener(this);
        mButtonAudioRoute.setOnClickListener(this);
        mButtonHangUp.setOnClickListener(this);

        refreshUserView();
    }

    @Override
    protected void onPermissionGranted() {
        initView();
        enterRoom();
    }

    protected void enterRoom() {
        mTRTCCloud = TRTCCloud.sharedInstance(getApplicationContext());
        mTRTCCloud.setListener(new TRTCCloudImplListener(AudioCallingActivity.this));
        TRTCCloudDef.TRTCParams trtcParams = new TRTCCloudDef.TRTCParams();
        trtcParams.sdkAppId = GenerateTestUserSig.SDKAPPID;
        trtcParams.userId = mUserId;
        trtcParams.roomId = Integer.parseInt(mRoomId);
        trtcParams.userSig = GenerateTestUserSig.genTestUserSig(trtcParams.userId);
        mTRTCCloud.enableAudioVolumeEvaluation(2000);
        mTRTCCloud.startLocalAudio(TRTCCloudDef.TRTC_AUDIO_QUALITY_SPEECH);
        mTRTCCloud.enterRoom(trtcParams, TRTCCloudDef.TRTC_APP_SCENE_AUDIOCALL);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        animationDrawable.stop();
        exitRoom();
    }

    private void exitRoom() {
        if (mTRTCCloud != null) {
            mTRTCCloud.stopLocalAudio();
            mTRTCCloud.exitRoom();
            mTRTCCloud.setListener(null);
        }
        mTRTCCloud = null;
        TRTCCloud.destroySharedInstance();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_mute_audio) {
            muteAudio();
        } else if (id == R.id.btn_audio_route) {
            audioRoute();
        } else if (id == R.id.btn_hangup) {
            hangUp();
        }

    }

    private void hangUp() {
        finish();
    }

    private void audioRoute() {
        mAudioRouteFlag = !mAudioRouteFlag;
        mButtonAudioRoute.setSelected(mAudioRouteFlag);
        if (mAudioRouteFlag) {
            mTRTCCloud.setAudioRoute(TRTCCloudDef.TRTC_AUDIO_ROUTE_SPEAKER);
            mButtonAudioRoute.setActivated(true);

        } else {
            mTRTCCloud.setAudioRoute(TRTCCloudDef.TRTC_AUDIO_ROUTE_EARPIECE);
            mButtonAudioRoute.setActivated(false);

        }
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

    private void refreshUserView() {
        if (mRemoteUserList != null) {
            for (int i = 0; i < 9; i++) {
                if (i < mRemoteUserList.size()) {
                    mListUserView.get(i).setVisibility(View.VISIBLE);
                    //mListUserAvatar.get(i)---> Avatar

                    //friend list come from 99 Chat
          /*    String avatar_url = friendList.getAvatarUrl(mListUserIdView.get(i).toString().trim());
                    Picasso.get().load(avatar_url).into(mListUserAvatar.get(i));*/

                    mListUserIdView.get(i).setText(mRemoteUserList.get(i).toString());
                } else {
                    mListUserView.get(i).setVisibility(View.GONE);
                }
            }
        }
    }


    private class TRTCCloudImplListener extends TRTCCloudListener {

        private final WeakReference<AudioCallingActivity> mContext;

        public TRTCCloudImplListener(AudioCallingActivity activity) {
            super();
            mContext = new WeakReference<>(activity);
        }

        @Override
        public void onUserVoiceVolume(ArrayList<TRTCCloudDef.TRTCVolumeInfo> arrayList, int i) {
            Log.d(TAG, "onUserVoiceVolume:i = " + i);
            if (arrayList != null && arrayList.size() > 0) {
                Log.d(TAG, "onUserVoiceVolume:arrayList.size = " + arrayList.size());
                int index = 0;
                for (TRTCCloudDef.TRTCVolumeInfo info : arrayList) {
                    if (info != null && !mUserId.equals(info.userId)) {
                        Log.d(TAG, "onUserVoiceVolume:userId = " + info.userId + ", volume = " + info.volume);
                        // mListVoiceInfo.get(index).setVisibility(View.VISIBLE);
                        //mListVoiceInfo.get(index).setText(info.userId + ":" + info.volume);
                        index++;
                    }
                }
                for (int j = index; j < 6; j++) {
                    //  mListVoiceInfo.get(j).setVisibility(View.GONE);
                }
            }
        }


        @Override
        public void onRemoteUserEnterRoom(String s) {
            Log.d(TAG, "onRemoteUserEnterRoom userId " + s);
            mRemoteUserList.add(s);
            refreshUserView();
        }

        @Override
        public void onRemoteUserLeaveRoom(String s, int i) {
            Log.d(TAG, "onRemoteUserLeaveRoom userId " + s);
            mRemoteUserList.remove(s);
            refreshUserView();
        }

        @Override
        public void onError(int errCode, String errMsg, Bundle extraInfo) {
            Log.d(TAG, "sdk callback onError");
            AudioCallingActivity activity = mContext.get();
            if (activity != null) {
                Toast.makeText(activity, "onError: " + errMsg + "[" + errCode + "]", Toast.LENGTH_SHORT).show();
                if (errCode == TXLiteAVCode.ERR_ROOM_ENTER_FAIL) {
                    activity.exitRoom();
                }
            }
        }
    }

}
