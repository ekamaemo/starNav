package com.example.starnav;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class SessionsAdapter extends RecyclerView.Adapter<SessionsAdapter.ViewHolder> {

    private List<SessionItem> items;
    private final OnItemClickListener listener;

    // Интерфейс для обработки кликов
    public interface OnItemClickListener {
        void onItemClick(SessionItem item);
    }

    public SessionsAdapter(List<SessionItem> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_session, parent, false);
        return new ViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SessionItem item = items.get(position);
        holder.bind(item);

        // Убедитесь, что этот код есть и listener не null!
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item); // Должен вызываться здесь
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView date, status;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.sessionImageView);
            date = itemView.findViewById(R.id.dateTextView);
            status = itemView.findViewById(R.id.statusTextView);
        }

        public void bind(SessionItem item) {
            date.setText(item.date);
            status.setText(item.status);
        }
    }
}