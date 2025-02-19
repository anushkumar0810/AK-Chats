package com.anush_projects.akchats.Adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.anush_projects.akchats.Models.User;
import com.anush_projects.akchats.R;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
    private List<User> userList;
    private OnUserClickListener listener;
    private Map<String, ListenerRegistration> listenerMap = new HashMap<>();

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    public UserAdapter(List<User> userList, OnUserClickListener listener) {
        this.userList = userList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.username.setText(user.getName());

        if (user.getProfileImageBase64() != null && !user.getProfileImageBase64().isEmpty()) {
            byte[] decodedString = Base64.decode(user.getProfileImageBase64(), Base64.DEFAULT);
            Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            holder.userImage.setImageBitmap(decodedBitmap);
        } else {
            holder.userImage.setImageResource(R.drawable.app_icon);
        }

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String chatId = currentUserId.compareTo(user.getUserId()) < 0 ?
                currentUserId + "_" + user.getUserId() :
                user.getUserId() + "_" + currentUserId;

        Log.d("ChatID", "Chat ID for " + user.getName() + ": " + chatId);

        if (listenerMap.containsKey(chatId)) {
            listenerMap.get(chatId).remove();
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        ListenerRegistration registration = db.collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (error != null) {
                        Log.e("FirestoreError", "Error getting recent messages", error);
                        holder.recentChat.setText("Say Hi! 👋");
                        holder.lastMessageTime.setText("");
                        return;
                    }

                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        String lastMessage = document.getString("text");
                        String chatImage = document.getString("chatImage");
                        Long timestamp = document.getLong("timestamp");

                        if (chatImage != null && !chatImage.isEmpty()) {
                            holder.recentChat.setText("Image 📷");
                        } else {
                            holder.recentChat.setText(lastMessage != null ? lastMessage : "Say Hi! 👋");
                        }

                        holder.lastMessageTime.setText(timestamp != null ? getFormattedTime(timestamp) : "");
                    } else {
                        holder.recentChat.setText("Say Hi! 👋");
                        holder.lastMessageTime.setText("");
                    }
                });

        listenerMap.put(chatId, registration);

        holder.itemView.setOnClickListener(v -> listener.onUserClick(user));
    }

    private String getFormattedTime(Long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        for (ListenerRegistration registration : listenerMap.values()) {
            registration.remove();
        }
        listenerMap.clear();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView username, recentChat, lastMessageTime;
        ShapeableImageView userImage;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.username);
            userImage = itemView.findViewById(R.id.userImage);
            recentChat = itemView.findViewById(R.id.recentChat);
            lastMessageTime = itemView.findViewById(R.id.timeText);
        }
    }
}
