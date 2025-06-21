package com.example.SE114_DoAn;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class BoardListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_LIST = 0;
    private static final int TYPE_ADD = 1;

    private Context context;
    private List<Group> groups = new ArrayList<>();
    private Map<String, List<Task>> groupTasks = new HashMap<>();

    // Listeners for Fragment
    private final Runnable onAddGroupListener;
    private final Consumer<Task> onTaskClickListener;
    private final Consumer<String> onTaskDeleteListener;
    private final BiConsumer<String, String> onTaskAddListener; // Consumer<groupId, taskTitle>

    public BoardListAdapter(Context context, Runnable onAddGroupListener, BiConsumer<String, String> onTaskAddListener, Consumer<Task> onTaskClickListener, Consumer<String> onTaskDeleteListener) {
        this.context = context;
        this.onAddGroupListener = onAddGroupListener;
        this.onTaskAddListener = onTaskAddListener;
        this.onTaskClickListener = onTaskClickListener;
        this.onTaskDeleteListener = onTaskDeleteListener;
    }

    public void setGroups(List<Group> newGroups) {
        this.groups = newGroups;
        notifyDataSetChanged();
    }

    public void setGroupTasks(Map<String, List<Task>> newTasks) {
        this.groupTasks = newTasks;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return position < groups.size() ? TYPE_LIST : TYPE_ADD;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_LIST) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list, parent, false);
            return new ListViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_add_list, parent, false);
            return new AddListViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == TYPE_LIST) {
            ListViewHolder listViewHolder = (ListViewHolder) holder;
            Group currentGroup = groups.get(position);
            listViewHolder.tvListTitle.setText(currentGroup.getName());

            listViewHolder.cardRecyclerView.setLayoutManager(new LinearLayoutManager(context));
            List<Task> tasks = groupTasks.getOrDefault(currentGroup.getGroup_id(), new ArrayList<>());

            TaskAdapter taskAdapter = new TaskAdapter(context, tasks, onTaskClickListener, onTaskDeleteListener);
            listViewHolder.cardRecyclerView.setAdapter(taskAdapter);

            listViewHolder.tvAddCard.setOnClickListener(v -> {
                final EditText input = new EditText(context);
                new AlertDialog.Builder(context)
                        .setTitle("Thêm Task Mới")
                        .setMessage("Nhập tiêu đề cho task mới")
                        .setView(input)
                        .setPositiveButton("Thêm", (dialog, which) -> {
                            String taskTitle = input.getText().toString().trim();
                            if (!taskTitle.isEmpty() && onTaskAddListener != null) {
                                onTaskAddListener.accept(currentGroup.getGroup_id(), taskTitle);
                            }
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            });

        } else if (holder.getItemViewType() == TYPE_ADD) {
            AddListViewHolder addListViewHolder = (AddListViewHolder) holder;
            addListViewHolder.btnAddList.setOnClickListener(v -> {
                if (onAddGroupListener != null) {
                    onAddGroupListener.run();
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return groups.size() + 1; // +1 for the "Add List" button
    }

    // ViewHolders
    static class ListViewHolder extends RecyclerView.ViewHolder {
        TextView tvListTitle;
        RecyclerView cardRecyclerView;
        TextView tvAddCard;
        public ListViewHolder(@NonNull View itemView) {
            super(itemView);
            tvListTitle = itemView.findViewById(R.id.listTitle);
            cardRecyclerView = itemView.findViewById(R.id.cardRecyclerView);
            tvAddCard = itemView.findViewById(R.id.tvAddCard);
        }
    }

    static class AddListViewHolder extends RecyclerView.ViewHolder {
        Button btnAddList;
        public AddListViewHolder(@NonNull View itemView) {
            super(itemView);
            btnAddList = itemView.findViewById(R.id.btnAddList);
        }
    }
}