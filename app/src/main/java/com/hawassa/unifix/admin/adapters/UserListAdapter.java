package com.hawassa.unifix.admin.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.hawassa.unifix.R;
import com.hawassa.unifix.models.User;
import java.util.ArrayList;
import java.util.List;

public class UserListAdapter extends RecyclerView.Adapter<UserListAdapter.UserViewHolder> {

    private List<User> userList;
    private OnUserActionListener listener;

    public interface OnUserActionListener {
        void onDeleteUser(User user);
        void onEditUser(User user);
    }

    public UserListAdapter(List<User> userList, OnUserActionListener listener) {
        this.userList = userList != null ? userList : new ArrayList<>();
        this.listener = listener;
        Log.d("UserListAdapter", "Adapter initialized with " + this.userList.size() + " users");
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        Log.d("UserListAdapter", "getItemCount: " + userList.size());
        return userList.size();
    }

    public void updateData(List<User> newList) {
        Log.d("UserListAdapter", "updateData called with " + (newList != null ? newList.size() : 0) + " users");
        if (newList != null) {
            userList.clear();
            userList.addAll(newList);
        } else {
            userList.clear();
        }
        notifyDataSetChanged();
        Log.d("UserListAdapter", "After updateData: " + userList.size() + " users");
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvUserId, tvUserRole, tvUserEmail, tvUserStatus;
        ImageButton btnDelete, btnEdit;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserId = itemView.findViewById(R.id.tvUserId);
            tvUserRole = itemView.findViewById(R.id.tvUserRole);
            tvUserEmail = itemView.findViewById(R.id.tvUserEmail);
            tvUserStatus = itemView.findViewById(R.id.tvUserStatus);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnEdit = itemView.findViewById(R.id.btnEdit);

            Log.d("UserViewHolder", "ViewHolder created");
        }

        public void bind(User user) {
            Log.d("UserViewHolder", "Binding user: " + (user != null ? user.getName() : "null"));

            if (user == null) {
                return;
            }

            // Display user name
            tvUserName.setText(user.getName() != null ? user.getName() : "Unknown");

            // Display Firebase UID (important for deletion)
            String firebaseUid = user.getFirebaseUid();
            String userId = user.getUserId();

            // Show both IDs for clarity, but the Firebase UID is what we need
            String displayText = "UID: ";
            if (firebaseUid != null && firebaseUid.length() > 6) {
                displayText += firebaseUid.substring(0, 6) + "...";
            } else {
                displayText += (firebaseUid != null ? firebaseUid : "N/A");
            }

            // Add custom user ID if available
            if (userId != null && !userId.isEmpty()) {
                displayText += " | ID: " + userId;
            }

            tvUserId.setText(displayText);

            // Display role with color coding
            String role = user.getRole() != null ? user.getRole() : "unknown";
            tvUserRole.setText("Role: " + role.toUpperCase());

            // Set role color
            int roleColor = R.color.text_primary;
            if ("staff".equalsIgnoreCase(role) || "technician".equalsIgnoreCase(role)) {
                roleColor = R.color.primary_blue;
            } else if ("student".equalsIgnoreCase(role)) {
                roleColor = R.color.green;
            } else if ("admin".equalsIgnoreCase(role)) {
                roleColor = R.color.accent;
            }
            tvUserRole.setTextColor(itemView.getContext().getResources().getColor(roleColor));

            // Display email
            String email = user.getEmail() != null ? user.getEmail() : "No email";
            tvUserEmail.setText(email);

            // Display phone if available
            String phone = user.getPhone();
            if (phone != null && !phone.isEmpty() && tvUserEmail != null) {
                tvUserEmail.setText(email + "\nPhone: " + phone);
            }

            // Display status
            String status = user.getStatus() != null ? user.getStatus() : "active";
            if (tvUserStatus != null) {
                tvUserStatus.setText("Status: " + status.toUpperCase());

                // Set status color
                int statusColor = R.color.green;
                if ("inactive".equalsIgnoreCase(status)) {
                    statusColor = R.color.red;
                } else if ("pending".equalsIgnoreCase(status)) {
                    statusColor = R.color.orange;
                } else if ("suspended".equalsIgnoreCase(status)) {
                    statusColor = R.color.red;
                }
                tvUserStatus.setTextColor(itemView.getContext().getResources().getColor(statusColor));
            }

            // Set click listeners with null checks
            btnDelete.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    User clickedUser = userList.get(position);

                    Log.d("UserAdapter",
                            "Delete clicked for: " + clickedUser.getName() +
                                    ", FirebaseUID: " + clickedUser.getFirebaseUid());

                    listener.onDeleteUser(clickedUser);
                }
            });

            btnEdit.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onEditUser(userList.get(position));
                }
            });

            Log.d("UserAdapter",
                    "User: " + user.getName() +
                            ", FirebaseUID: " + user.getFirebaseUid() +
                            ", UserID: " + user.getUserId());
        }
    }
}