package com.tencent.liteav.meeting.ui.remote;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatImageButton;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;
import com.tencent.liteav.demo.trtc.R;
import com.tencent.liteav.meeting.ui.MemberEntity;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class RemoteUserListAdapter extends
        RecyclerView.Adapter<RemoteUserListAdapter.ViewHolder> {

    private static final String TAG = RemoteUserListAdapter.class.getSimpleName();

    private final Context context;
    private List<MemberEntity> list;
    private final OnItemClickListener onItemClickListener;

    public RemoteUserListAdapter(Context context,
                                 OnItemClickListener onItemClickListener) {
        this.context = context;
        this.onItemClickListener = onItemClickListener;
    }

    public void setMemberList(List<MemberEntity> list) {
        this.list = list;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View view = inflater.inflate(R.layout.item_meeting_remote_user_list, parent, false);

        ViewHolder viewHolder = new ViewHolder(view);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        MemberEntity item = list.get(position);
        //第一个是自己
        holder.bind(item, position == 0, onItemClickListener);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }


    public interface OnItemClickListener {
        void onMuteAudioClick(int position);

        void onMuteVideoClick(int position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private CircleImageView mHeadImg;
        private TextView mUserNameTv;
        private AppCompatImageButton mAudioImg;
        private AppCompatImageButton mVideoImg;

        public ViewHolder(View itemView) {
            super(itemView);
            initView(itemView);
        }

        private void initView(final View itemView) {
            mHeadImg = itemView.findViewById(R.id.img_head);
            mUserNameTv = itemView.findViewById(R.id.tv_user_name);
            mAudioImg = itemView.findViewById(R.id.img_audio);
            mVideoImg = itemView.findViewById(R.id.img_video);
        }

        public void bind(final MemberEntity model,
                         final boolean isSelf,
                         final OnItemClickListener listener) {
            if (!TextUtils.isEmpty(model.getUserAvatar())) {
                Picasso.get().load(model.getUserAvatar()).into(mHeadImg);
            } else {
                mHeadImg.setImageResource(R.drawable.meeting_head);
            }
            if (isSelf) {
                mUserNameTv.setText(mUserNameTv.getResources().getString(R.string.meeting_tv_me, model.getUserName()));
                mAudioImg.setVisibility(View.GONE);
                mVideoImg.setVisibility(View.GONE);
            } else {
                mUserNameTv.setText(model.getUserName());
                mAudioImg.setSelected(!model.isMuteAudio());
                mVideoImg.setSelected(!model.isMuteVideo());
                mAudioImg.setVisibility(View.VISIBLE);
                mVideoImg.setVisibility(View.VISIBLE);
                mAudioImg.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        listener.onMuteAudioClick(getLayoutPosition());
                    }
                });
                mVideoImg.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        listener.onMuteVideoClick(getLayoutPosition());
                    }
                });
            }
        }
    }

}