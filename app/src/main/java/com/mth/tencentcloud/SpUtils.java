package com.mth.tencentcloud;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;

public class SpUtils {
    public final String fromUser_ID = "fromUserID";
    public final String fromUser_Name = "fromUserName";
    public final String fromUser_Avatar = "fromUserAvatar";
    public final String toUser_ID = "toUserID";
    public final String toUser_Name = "toUserName";
    public final String toUser_Avatar = "toUserAvatar";
    Context context;

    public SpUtils(Context context) {
        this.context = context;
    }

    public void saveData(String fromUserID, String fromUserName, String fromUserAvatar, String toUserID, String toUserName, String toUserAvatar) {
        SharedPreferences.Editor editor = context.getSharedPreferences("UserData", MODE_PRIVATE).edit();

        editor.putString(fromUser_ID, fromUserID);
        editor.putString(fromUser_Name, fromUserName);
        editor.putString(fromUser_Avatar, fromUserAvatar);
        editor.putString(toUser_ID, toUserID);
        editor.putString(toUser_Name, toUserName);
        editor.putString(toUser_Avatar, toUserAvatar);
        editor.apply();
    }

    public void clearData() {
        SharedPreferences.Editor editor = context.getSharedPreferences("UserData", MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();
    }
}
