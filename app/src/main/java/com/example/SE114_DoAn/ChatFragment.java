package com.example.SE114_DoAn;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Date;

public class ChatFragment extends Fragment {

    private RecyclerView rvChatLobby;
    private ChatLobbyAdapter adapter;
    private BoardViewModel boardViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        rvChatLobby = view.findViewById(R.id.rvChatLobby);
        boardViewModel = new ViewModelProvider(requireActivity()).get(BoardViewModel.class);

        setupRecyclerView();
        observeViewModel();

        return view;
    }

    private void setupRecyclerView() {
        adapter = new ChatLobbyAdapter(new ArrayList<>(), group -> {
            Bundle args = new Bundle();
            args.putString("GROUP_ID", group.getGroup_id());
            args.putString("GROUP_NAME", group.getName());
            NavHostFragment.findNavController(this).navigate(R.id.action_chatFragment_to_groupChatFragment, args);
        });
        rvChatLobby.setLayoutManager(new LinearLayoutManager(getContext()));
        rvChatLobby.setAdapter(adapter);
    }

    private void observeViewModel() {
        // Lắng nghe LiveData chứa cả Group và tin nhắn cuối cùng
        boardViewModel.getGroupChatInfos().observe(getViewLifecycleOwner(), groupChatInfos -> {
            if (groupChatInfos != null) {
                // Sắp xếp danh sách để group có tin nhắn mới nhất lên đầu
                groupChatInfos.sort((o1, o2) -> {
                    Date d1 = (o1.getLastMessage() != null && o1.getLastMessage().getTimestamp() != null) ? o1.getLastMessage().getTimestamp() : new Date(0);
                    Date d2 = (o2.getLastMessage() != null && o2.getLastMessage().getTimestamp() != null) ? o2.getLastMessage().getTimestamp() : new Date(0);
                    return d2.compareTo(d1);
                });
                adapter.updateGroupChatInfos(groupChatInfos);
            }
        });
    }
}