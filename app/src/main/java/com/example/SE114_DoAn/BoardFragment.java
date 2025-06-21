package com.example.SE114_DoAn;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.function.BiConsumer;

public class BoardFragment extends Fragment {

    private BoardListAdapter boardListAdapter;
    private BoardViewModel boardViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_board, container, false);

        boardViewModel = new ViewModelProvider(this).get(BoardViewModel.class);
        RecyclerView boardRecyclerView = view.findViewById(R.id.boardRecyclerView);

        setupRecyclerView(boardRecyclerView);
        observeViewModel();

        return view;
    }

    private void setupRecyclerView(RecyclerView recyclerView) {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        // Sử dụng BiConsumer để truyền groupId và taskTitle
        BiConsumer<String, String> onTaskAddListener = (groupId, taskTitle) -> boardViewModel.addTaskToGroup(groupId, taskTitle);

        boardListAdapter = new BoardListAdapter(
                getContext(),
                this::showAddGroupDialog, // onAddGroupListener
                onTaskAddListener,
                this::onTaskClicked, // onTaskClickListener
                taskId -> boardViewModel.deleteTask(taskId) // onTaskDeleteListener
        );
        recyclerView.setAdapter(boardListAdapter);
    }

    private void observeViewModel() {
        boardViewModel.getGroups().observe(getViewLifecycleOwner(), groups -> {
            if (groups != null) {
                boardListAdapter.setGroups(groups);
            }
        });

        boardViewModel.getGroupTasks().observe(getViewLifecycleOwner(), groupTasks -> {
            if (groupTasks != null) {
                boardListAdapter.setGroupTasks(groupTasks);
            }
        });

        boardViewModel.getOperationStatus().observe(getViewLifecycleOwner(), status -> {
            if (status != null && !status.isEmpty()) {
                Toast.makeText(getContext(), status, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Cập nhật để chỉ truyền taskId sang TaskDetailActivity.
     * @param task Đối tượng Task được click.
     */
    private void onTaskClicked(Task task) {
        Intent intent = new Intent(getActivity(), TaskDetailActivity.class);
        intent.putExtra("taskId", task.getTask_id());
        startActivity(intent);
    }

    private void showAddGroupDialog() {
        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        new AlertDialog.Builder(getContext())
                .setTitle("Thêm Nhóm Mới")
                .setMessage("Nhập tên cho nhóm mới")
                .setView(input)
                .setPositiveButton("Thêm", (dialog, which) -> {
                    String groupName = input.getText().toString().trim();
                    if (!groupName.isEmpty()) {
                        boardViewModel.addGroup(groupName);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}