package com.mth.tencentcloud;

import static com.tencent.liteav.login.model.ProfileManager.ERROR_CODE_NEED_REGISTER;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.blankj.utilcode.util.ToastUtils;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.tencent.liteav.login.model.ProfileManager;

public class MainLoginActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int RC_SIGN_IN = 1;
    private final String TAG = "SIGNGOOGLE";
    Button loginBtn;
    GoogleSignInClient mGoogleSignInClient;
    private EditText fromUserID;
    private EditText fromUserName;
    private EditText toUserID;
    private EditText toUserName;
    private SignInButton signInButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_login_layout);
        fromUserID = findViewById(R.id.edt_from_user_id);
        fromUserName = findViewById(R.id.edt_from_user_name);
        toUserID = findViewById(R.id.edt_to_user_id);
        toUserName = findViewById(R.id.edt_to_user_name);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        SignInButton signInButton = findViewById(R.id.sign_in_button);
        signInButton.setSize(SignInButton.SIZE_STANDARD);


        signInButton.setOnClickListener(this);
        findViewById(R.id.login_btn).setOnClickListener(view -> {
            //saveUserData();

            SpUtils spUtils = new SpUtils(MainLoginActivity.this);
            spUtils.saveData(
                    fromUserID.getText().toString().trim(),
                    fromUserName.getText().toString().trim(),
                    "",
                    toUserID.getText().toString().trim(),
                    toUserName.getText().toString().trim(),
                    ""
            );

            login();
            initData();
        });
    }

   /* @Override
    protected void onStart() {
        super.onStart();
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        gotoProfile(account);
    }*/

    private void initData() {
        String userId = ProfileManager.getInstance().getUserId();
        String token = ProfileManager.getInstance().getToken();
        if (!TextUtils.isEmpty(userId)) {
            //   mEditUserId.setText(userId);
            if (!TextUtils.isEmpty(token)) {
                //   handleLoginStatus(STATUS_LOGGING_IN);
                ProfileManager.getInstance().autoLogin(userId, token, new ProfileManager.ActionCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(getApplicationContext(), "Success", Toast.LENGTH_SHORT).show();
                        //    handleLoginStatus(STATUS_LOGIN_SUCCESS);
                        //   startMainActivity();
                    }

                    @Override
                    public void onFailed(int code, String msg) {
                        //  handleLoginStatus(STATUS_LOGIN_FAIL);
                        ToastUtils.showLong(com.tencent.liteav.login.R.string.login_tips_auto_login_fail);
                    }
                });
            }
        }
    }

    private void login() {
        //Toast.makeText(MainLoginActivity.this, "Logging in", Toast.LENGTH_SHORT).show();

        String userId = fromUserID.getText().toString().trim();
        Toast.makeText(MainLoginActivity.this, userId, Toast.LENGTH_SHORT).show();

        if (TextUtils.isEmpty(userId)) {
            ToastUtils.showLong(R.string.login_tips_input_correct_info);
            Toast.makeText(getApplicationContext(), "Null", Toast.LENGTH_SHORT).show();
            return;
        }
        // handleLoginStatus(STATUS_LOGGING_IN);


        ProfileManager.getInstance().login(userId, "", new ProfileManager.ActionCallback() {
            @Override
            public void onSuccess() {
                //  handleLoginStatus(STATUS_LOGIN_SUCCESS);

                Toast.makeText(MainLoginActivity.this, "Logging OK", Toast.LENGTH_SHORT).show();

                startActivity(new Intent(MainLoginActivity.this, MainActivity.class));
                finish();
            }

            @Override
            public void onFailed(int code, String msg) {

                Toast.makeText(MainLoginActivity.this, "Login Fail ", Toast.LENGTH_SHORT).show();
                if (code == ERROR_CODE_NEED_REGISTER) {
                    //  handleLoginStatus(STATUS_LOGIN_SUCCESS);
                    ToastUtils.showLong(R.string.login_tips_register);
                    //do register

                    ProfileManager.getInstance().setNicknameAndAvatar(fromUserName.getText().toString().trim(), "https://liteav.sdk.qcloud.com/app/res/picture/voiceroom/avatar/user_avatar1.png", new ProfileManager.ActionCallback() {
                        @Override
                        public void onSuccess() {
                            ToastUtils.showLong(getString(R.string.login_toast_register_success_and_logging_in));
                            //startMainActivity();
                            finish();
                        }

                        @Override
                        public void onFailed(int code, String msg) {
                            ToastUtils.showLong(getString(R.string.login_toast_failed_to_set_username, msg));
                        }
                    });
                } else {
                    //handleLoginStatus(STATUS_LOGIN_FAIL);
                }
            }
        });
    }

    public void saveUserData() {
        SharedPreferences.Editor editor = this.getSharedPreferences("UserData", MODE_PRIVATE).edit();
        editor.putString("fromUserID", fromUserID.getText().toString().trim());
        editor.putString("fromUserName", fromUserName.getText().toString().trim());
        editor.putString("toUserID", toUserID.getText().toString().trim());
        editor.putString("toUserName", toUserName.getText().toString().trim());
        editor.apply();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.sign_in_button:
                signIn();
                break;
        }
    }

    public void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            // Signed in successfully, show authenticated UI.
            //  updateUI(account);

            gotoProfile(account);

        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
            //updateUI(null);
        }
    }

    private void gotoProfile(GoogleSignInAccount account) {
        Intent intent = new Intent(MainLoginActivity.this, ProfileActivity.class);
        intent.putExtra("profileImage", account.getPhotoUrl());
        intent.putExtra("name", account.getDisplayName());
        intent.putExtra("email", account.getEmail());
        startActivity(intent);
    }
}
