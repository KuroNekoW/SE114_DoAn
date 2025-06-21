package com.example.SE114_DoAn;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ChatLobbyAdapter extends RecyclerView.Adapter<ChatLobbyAdapter.ViewHolder> {

    private List<GroupChatInfo> groupChatInfos;
    private final OnGroupClickListener listener;

    public interface OnGroupClickListener {
        void onGroupClick(Group group);
    }

    public ChatLobbyAdapter(List<GroupChatInfo> groupChatInfos, OnGroupClickListener listener) {
        this.groupChatInfos = groupChatInfos;
        this.listener = listener;
    }

    public void updateGroupChatInfos(List<GroupChatInfo> newGroupChatInfos) {
        this.groupChatInfos = newGroupChatInfos;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_lobby, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GroupChatInfo info = groupChatInfos.get(position);
        holder.bind(info, listener);
    }

    @Override
    public int getItemCount() {
        return groupChatInfos.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvGroupName;
        TextView tvLastMessage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGroupName = itemView.findViewById(R.id.tvGroupName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
        }

        public void bind(final GroupChatInfo info, final OnGroupClickListener listener) {
            Group group = info.getGroup();
            ChatMessage lastMessage = info.getLastMessage();

            tvGroupName.setText(group.getName());

            if (lastMessage != null) {
                String displayMessage = (lastMessage.getUsername() != null ? lastMessage.getUsername() : "")
                        + ": " + lastMessage.getMessage();
                tvLastMessage.setText(displayMessage);
            } else {
                tvLastMessage.setText("Chưa có tin nhắn");
            }

            itemView.setOnClickListener(v -> listener.onGroupClick(group));
        }
    }
}