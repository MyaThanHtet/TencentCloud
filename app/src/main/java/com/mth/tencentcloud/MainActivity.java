package com.mth.tencentcloud;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.mth.audiocall.AudioCallingEnterActivity;
import com.mth.screenshare.ScreenEntranceActivity;
import com.mth.videocall.VideoCallingEnterActivity;
import com.tencent.liteav.login.model.ProfileManager;
import com.tencent.liteav.meeting.ui.CreateMeetingActivity;
import com.tencent.liteav.trtccalling.model.TRTCCalling;
import com.tencent.liteav.trtccalling.ui.TRTCCallingEntranceActivity;
import com.tencent.liteav.trtcvoiceroom.ui.list.VoiceRoomListActivity;


/**
 * TRTC API-Example 主页面
 * <p>
 * 其中包含
 * 基础功能模块如下：
 * - 语音通话模块{@link .AudioCallingEnterActivity}
 */
public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().hide();


        /* new Handler().postDelayed(() -> findViewById(R.id.launch_view).setVisibility(View.GONE), 1000);
         */
        findViewById(R.id.ll_audio_call).setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, AudioCallingEnterActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.ll_sshare).setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, ScreenEntranceActivity.class);
            startActivity(intent);
        });
        findViewById(R.id.ll_vcall).setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, VideoCallingEnterActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.ll_audio_room).setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, VoiceRoomListActivity.class);
            startActivity(intent);
        });
        findViewById(R.id.ll_calling_video).setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, TRTCCallingEntranceActivity.class);
            intent.putExtra("TYPE", TRTCCalling.TYPE_VIDEO_CALL);
            startActivity(intent);
        });
        findViewById(R.id.ll_calling_audio).setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, TRTCCallingEntranceActivity.class);
            intent.putExtra("TYPE", TRTCCalling.TYPE_AUDIO_CALL);
            startActivity(intent);
        });
        findViewById(R.id.ll_video_room).setOnClickListener(view ->{
            Intent intent = new Intent(MainActivity.this, CreateMeetingActivity.class);
            intent.putExtra("TYPE", TRTCCalling.TYPE_AUDIO_CALL);
            startActivity(intent);
        });

        findViewById(R.id.logout_btn).setOnClickListener(view -> {

            ProfileManager.getInstance().logout(new ProfileManager.ActionCallback() {
                @Override
                public void onSuccess() {
                    CallService.stop(getApplicationContext());
                    // 退出登录
                    SpUtils spUtils = new SpUtils(MainActivity.this);
                    spUtils.clearData();

                    startActivity(new Intent(MainActivity.this, MainLoginActivity.class));
                    finish();
                }

                @Override
                public void onFailed(int code, String msg) {
                }
            });

        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // CallService.start(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        CallService.start(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CallService.stop(MainActivity.this);
    }
}
