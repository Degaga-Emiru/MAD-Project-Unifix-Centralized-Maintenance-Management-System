package com.hawassa.unifix.shared.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.hawassa.unifix.R;
import com.hawassa.unifix.models.Notification;
import java.util.ArrayList;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
    private List<Notification> notificationList;
    private OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    public NotificationAdapter(List<Notification> notificationList, OnNotificationClickListener listener) {
        this.notificationList = notificationList != null ? notificationList : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (notificationList == null || position < 0 || position >= notificationList.size()) {
            return;
        }

        Notification notification = notificationList.get(position);
        if (notification == null) {
            return;
        }

        // Set basic info
        holder.tvTitle.setText(notification.getTitle() != null ? notification.getTitle() : "");
        holder.tvMessage.setText(notification.getMessage() != null ? notification.getMessage() : "");

        // Use your model's getFormattedTime() method
        holder.tvTime.setText(notification.getFormattedTime());

        // Set sender name (if available)
        if (notification.getSenderName() != null && !notification.getSenderName().isEmpty()) {
            holder.tvSender.setText("From: " + notification.getSenderName());
            holder.tvSender.setVisibility(View.VISIBLE);
        } else {
            holder.tvSender.setVisibility(View.GONE);
        }
        String message = notification.getMessage();
        if (message != null && !message.isEmpty()) {
            holder.tvMessage.setText(message);
            holder.tvMessage.setVisibility(View.VISIBLE);
        } else {
            holder.tvMessage.setVisibility(View.GONE);
        }

        // Set icon based on type
        int iconRes = getIconForType(notification.getType());
        holder.ivIcon.setImageResource(iconRes);

        // Set icon color
        int colorRes = getColorForType(notification.getType());
        holder.ivIcon.setColorFilter(holder.itemView.getContext().getResources().getColor(colorRes));

        // Show unread indicator
        boolean isRead = notification.isRead();
        holder.viewUnreadIndicator.setVisibility(isRead ? View.GONE : View.VISIBLE);
        holder.itemView.setAlpha(isRead ? 0.7f : 1.0f);
        holder.ivIcon.setAlpha(isRead ? 0.7f : 1.0f);

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotificationClick(notification);
            }
        });
    }

    private int getIconForType(String type) {
        if (type == null) return R.drawable.ic_notifications;

        switch (type) {
            case "new_report":
                return R.drawable.ic_report;
            case "task_assigned":
                return R.drawable.ic_assign_task;
            case "status_update":
                return R.drawable.ic_update;
            case "feedback":
                return R.drawable.ic_feedback;
            case "report_confirmation":
                return R.drawable.ic_check_circle;
            case "admin_alert":
                return R.drawable.ic_warning;
            default:
                return R.drawable.ic_notifications;
        }
    }

    private int getColorForType(String type) {
        if (type == null) return R.color.primary_blue;

        switch (type) {
            case "new_report":
                return R.color.notification_new;
            case "task_assigned":
                return R.color.notification_assigned;
            case "status_update":
                return R.color.notification_update;
            case "feedback":
                return R.color.notification_feedback;
            case "admin_alert":
                return R.color.notification_alert;
            default:
                return R.color.primary_blue;
        }
    }

    @Override
    public int getItemCount() {
        return notificationList != null ? notificationList.size() : 0;
    }

    public void updateData(List<Notification> newList) {
        if (notificationList == null) {
            notificationList = new ArrayList<>();
        }
        notificationList.clear();
        if (newList != null) {
            notificationList.addAll(newList);
        }
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvTitle, tvMessage, tvTime, tvSender;
        View viewUnreadIndicator;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvSender = itemView.findViewById(R.id.tvSender); // Now this exists!
            viewUnreadIndicator = itemView.findViewById(R.id.viewUnreadIndicator);
        }
    }
}