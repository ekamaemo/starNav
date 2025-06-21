package com.example.starnav;

import android.content.Context;
import android.content.Intent;
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
    private final Context context;  // Добавляем контекст

    public interface OnItemClickListener {
        void onItemClick(SessionItem item);
    }

    public SessionsAdapter(Context context, List<SessionItem> items, OnItemClickListener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
    }

    public void updateItems(List<SessionItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
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
        holder.bind(item, listener);
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

        public void bind(SessionItem item, OnItemClickListener listener) {
            date.setText(item.getDate());
            status.setText(item.getStatus());


            // Обработка клика
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(item);
                }

                // Альтернативный вариант - прямая навигация
                Context context = itemView.getContext();
                Intent intent = new Intent(context, ItemActivity.class);
                intent.putExtra("latitude", item.getLatitude());
                intent.putExtra("longitude", item.getLongitude());
                context.startActivity(intent);
            });
        }
    }
}