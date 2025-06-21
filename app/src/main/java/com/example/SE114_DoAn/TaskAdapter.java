package com.example.SE114_DoAn;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.function.Consumer;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> tasks;
    private Context context;
    private Consumer<Task> onTaskClickListener;
    private Consumer<String> onTaskDeleteListener;

    public TaskAdapter(Context context, List<Task> tasks, Consumer<Task> onTaskClickListener, Consumer<String> onTaskDeleteListener) {
        this.context = context;
        this.tasks = tasks;
        this.onTaskClickListener = onTaskClickListener;
        this.onTaskDeleteListener = onTaskDeleteListener;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_card, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);
        holder.tvTaskTitle.setText(task.getTitle());

        holder.itemView.setOnClickListener(v -> {
            if (onTaskClickListener != null) {
                onTaskClickListener.accept(task);
            }
        });

        holder.btnDeleteTask.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Xác nhận xóa")
                    .setMessage("Bạn có chắc muốn xóa task '" + task.getTitle() + "' không?")
                    .setPositiveButton("Xóa", (dialog, which) -> {
                        if (onTaskDeleteListener != null) {
                            // Truyền task_id (String) thay vì position (int)
                            onTaskDeleteListener.accept(task.getTask_id());
                        }
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return tasks != null ? tasks.size() : 0;
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTaskTitle;
        ImageButton btnDeleteTask;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            // Đảm bảo ID trong item_card.xml khớp
            tvTaskTitle = itemView.findViewById(R.id.etCardTitle);
            btnDeleteTask = itemView.findViewById(R.id.btnDeleteCard);
        }
    }
}