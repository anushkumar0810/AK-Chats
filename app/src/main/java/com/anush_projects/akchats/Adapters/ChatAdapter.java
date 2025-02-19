package com.anush_projects.akchats.Adapters;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.anush_projects.akchats.Activities.ViewImageActivity;
import com.anush_projects.akchats.Models.Message;
import com.anush_projects.akchats.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_TEXT_SENT = 0;
    private static final int TYPE_TEXT_RECEIVED = 1;
    private static final int TYPE_IMAGE_SENT = 2;
    private static final int TYPE_IMAGE_RECEIVED = 3;

    private List<Message> messages;
    private String senderId;

    public ChatAdapter(List<Message> messages, String senderId) {
        this.messages = messages;
        this.senderId = senderId;
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        String type = message.getMessageType() != null ? message.getMessageType() : "text";

        if ("image".equals(message.getMessageType())) {
            return message.getSenderId().equals(senderId) ? TYPE_IMAGE_SENT : TYPE_IMAGE_RECEIVED;
        } else {
            return message.getSenderId().equals(senderId) ? TYPE_TEXT_SENT : TYPE_TEXT_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_IMAGE_SENT) {
            return new ImageViewHolder(inflater.inflate(R.layout.item_image_sent, parent, false));
        } else if (viewType == TYPE_IMAGE_RECEIVED) {
            return new ImageViewHolder(inflater.inflate(R.layout.item_image_received, parent, false));
        } else if (viewType == TYPE_TEXT_SENT) {
            return new TextViewHolder(inflater.inflate(R.layout.item_message_sent, parent, false));
        } else {
            return new TextViewHolder(inflater.inflate(R.layout.item_message_received, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);
        String formattedTime = getFormattedTime(message.getTimestamp());

        if (holder instanceof TextViewHolder) {
            TextViewHolder textHolder = (TextViewHolder) holder;
            textHolder.messageText.setVisibility(View.VISIBLE);
            textHolder.messageText.setText(message.getText());
            textHolder.chatTime.setText(formattedTime);

        } else if (holder instanceof ImageViewHolder) {
            ImageViewHolder imageHolder = (ImageViewHolder) holder;
            imageHolder.imageView.setVisibility(View.VISIBLE);

            byte[] imageBytes = message.getImageBytes();
            if (imageBytes == null && message.getText() != null) {
                imageBytes = decodeBase64Image(message.getText());
            }

            if ("uploading".equals(message.getStatus())) {
                if (imageHolder.progressBarBar != null) {
                    imageHolder.progressBarBar.setVisibility(View.VISIBLE);
                }
            } else {
                if (imageHolder.progressBarBar != null) {
                    imageHolder.progressBarBar.setVisibility(View.GONE);
                }
            }

            if (imageBytes != null) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 2;
                Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);
                imageHolder.imageView.setImageBitmap(bitmap);
            } else {
                imageHolder.imageView.setImageResource(R.drawable.app_icon);
            }

            imageHolder.imageView.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), ViewImageActivity.class);
                intent.putExtra("imageBytes", message.getImageBytes());
                v.getContext().startActivity(intent);
            });

            imageHolder.chatTime.setText(formattedTime);
        }
    }



    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class TextViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, chatTime;

        TextViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            chatTime = itemView.findViewById(R.id.chatTime);
        }
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView chatTime;
        ProgressBar progressBarBar;

        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageMessage);
            chatTime = itemView.findViewById(R.id.chatTime);
            progressBarBar = itemView.findViewById(R.id.progressBarBar);
        }
    }

    private String getFormattedTime(Long timestamp) {
        if (timestamp == null || timestamp == 0) {
            return "Now";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    private byte[] decodeBase64Image(String base64) {
        try {
            return Base64.decode(base64, Base64.DEFAULT);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
    }

}
