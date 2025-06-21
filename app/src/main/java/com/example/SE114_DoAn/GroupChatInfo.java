package com.example.SE114_DoAn;

// Lớp này dùng để kết hợp thông tin của Group và tin nhắn cuối cùng
public class GroupChatInfo {
    private Group group;
    private ChatMessage lastMessage;

    public GroupChatInfo(Group group) {
        this.group = group;
        this.lastMessage = null; // Ban đầu chưa có tin nhắn
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public ChatMessage getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(ChatMessage lastMessage) {
        this.lastMessage = lastMessage;
    }
}