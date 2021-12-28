package com.mth.tencentcloud;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

public class ProfileActivity extends AppCompatActivity {
    GoogleSignInClient mGoogleSignInClient;
    private ImageView profileImageView;
    private TextView userNameTextView;
    private TextView userGmailTextView;
    private String photoUrl;
    private String displayName;
    private String email;
    private Button logoutBtn;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_profile);
        profileImageView = findViewById(R.id.profile_image);
        userNameTextView = findViewById(R.id.user_name);
        userGmailTextView = findViewById(R.id.gmail_tv);
        
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        Intent intent = getIntent();
        if (!(intent == null)) {

            photoUrl = intent.getStringExtra("profileImage");
            displayName = intent.getStringExtra("name");
            email = intent.getStringExtra("email");

        }
        userNameTextView.setText(displayName);
        userGmailTextView.setText(email);

        logoutBtn = findViewById(R.id.logout_btn);
        logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signOut();
            }
        });
    }

    private void signOut() {
        mGoogleSignInClient.signOut()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        finish();
                    }
                });
    }
}
