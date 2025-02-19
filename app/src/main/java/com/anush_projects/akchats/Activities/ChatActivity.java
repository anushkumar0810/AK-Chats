package com.anush_projects.akchats.Activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.anush_projects.akchats.Adapters.ChatAdapter;
import com.anush_projects.akchats.Database.DatabaseHelper;
import com.anush_projects.akchats.Models.Message;
import com.anush_projects.akchats.R;
import com.anush_projects.akchats.Services.NotificationHelper;
import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ChatAdapter chatAdapter;
    private List<Message> messageList;
    private EditText messageInput;
    private ImageView sendButton, pickImages, backBtn, camera;

    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private DatabaseHelper dbHelper;

    private ShapeableImageView receiverImage;
    private TextView receiverName;
    private String receiverFCMToken = "", name = "";

    private String chatId, senderId, receiverId;

    private static final int PICK_IMAGE_REQUEST = 100;
    public static final int CAMERA_REQUEST = 102;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        dbHelper = new DatabaseHelper(this);

        chatId = getIntent().getStringExtra("chatId");
        senderId = auth.getCurrentUser().getUid();
        receiverId = getIntent().getStringExtra("receiverId");

        recyclerView = findViewById(R.id.chatRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        pickImages = findViewById(R.id.pickImages);
        backBtn = findViewById(R.id.backBtn);
        camera = findViewById(R.id.camera);
        receiverImage = findViewById(R.id.receiverImage);
        receiverName = findViewById(R.id.receiverName);

        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messageList, senderId);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(chatAdapter);

        loadMessages();

        getReceiverDetails();

        listenForMessages();

        backBtn.setOnClickListener(v -> finish());

        sendButton.setOnClickListener(v -> sendTextMessage());

        pickImages.setOnClickListener(v -> openGallery());

        camera.setOnClickListener(v -> openCamera());
    }

    private void getReceiverDetails() {
        FirebaseFirestore.getInstance().collection("users")
                .document(receiverId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        name = documentSnapshot.getString("name");
                        String profileImageBase64 = documentSnapshot.getString("profileImageBase64");
                        receiverFCMToken = documentSnapshot.getString("fcmToken");

                        receiverName.setText(name);

                        if (profileImageBase64 != null && !profileImageBase64.isEmpty()) {
                            try {
                                byte[] imageBytes = Base64.decode(profileImageBase64, Base64.DEFAULT);
                                Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                                receiverImage.setImageBitmap(bitmap);
                            } catch (IllegalArgumentException e) {
                                receiverImage.setImageResource(R.drawable.app_icon);
                                Log.e("AKChats", "Base64 decoding error", e);
                            }
                        } else {
                            receiverImage.setImageResource(R.drawable.app_icon);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load receiver details", Toast.LENGTH_SHORT).show();
                    Log.e("AKChats", "Error fetching receiver details", e);
                });
    }



    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (photoFile != null) {
                imageUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", photoFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "IMG_" + timeStamp;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            try {
                byte[] imageBytes = getCompressedImageBytes(imageUri);
                sendImageMessage(imageBytes);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Image processing failed", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
            if (imageUri != null) {
                try {
                    byte[] imageBytes = getCompressedImageBytes(imageUri);
                    sendImageMessage(imageBytes);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Image processing failed", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Camera image not found", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private byte[] getCompressedImageBytes(Uri imageUri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(imageUri);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(inputStream, null, options);
        inputStream.close();

        options.inSampleSize = calculateInSampleSize(options, 720, 1280); // Target size
        options.inJustDecodeBounds = false;

        inputStream = getContentResolver().openInputStream(imageUri);
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
        inputStream.close();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 30, outputStream); // Lower quality for faster compression
        bitmap.recycle();

        return outputStream.toByteArray();
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        while ((height / inSampleSize) > reqHeight && (width / inSampleSize) > reqWidth) {
            inSampleSize *= 2;
        }

        return inSampleSize;
    }



    private void sendImageMessage(byte[] imageBytes) {
        long timestamp = System.currentTimeMillis();
        String messageId = chatId + "_" + timestamp;

        String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);

        Message imageMessage = new Message(
                messageId, chatId, senderId, receiverId,
                null, imageBytes, "image", timestamp
        );
        imageMessage.setStatus("uploading");

        dbHelper.insertMessage(imageMessage, this);

        messageList.add(imageMessage);
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        recyclerView.scrollToPosition(messageList.size() - 1);

        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("messageId", messageId);
        messageMap.put("chatId", chatId);
        messageMap.put("senderId", senderId);
        messageMap.put("receiverId", receiverId);
        messageMap.put("chatImage", base64Image);
        messageMap.put("type", "image");
        messageMap.put("timestamp", timestamp);

        firestore.collection("chats").document(chatId)
                .collection("messages").document(messageId)
                .set(messageMap)
                .addOnSuccessListener(aVoid -> {
                    imageMessage.setStatus("sent");
                    dbHelper.updateMessageStatus(messageId, "sent");
                    chatAdapter.notifyItemChanged(messageList.indexOf(imageMessage));
                    Log.d("AKChats", "Image sent to Firestore");
                    NotificationHelper.sendNotification(this,receiverFCMToken, name, "you received a image",senderId,chatId, "chatImage");

                })
                .addOnFailureListener(e -> {
                    imageMessage.setStatus("failed");
                    dbHelper.updateMessageStatus(messageId, "failed");
                    chatAdapter.notifyItemChanged(messageList.indexOf(imageMessage));
                    Log.e("AKChats", "Failed to send image to Firestore", e);
                });
    }


    private void sendTextMessage() {
        String text = messageInput.getText().toString().trim();
        if (text.isEmpty()) {
            return;
        }

        long timestamp = System.currentTimeMillis();
        String messageId = chatId + "_" + timestamp;


        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("messageId", messageId);
        messageMap.put("chatId", chatId);
        messageMap.put("senderId", senderId);
        messageMap.put("receiverId", receiverId);
        messageMap.put("text", text);
        messageMap.put("type", "text");
        messageMap.put("timestamp", timestamp);


        firestore.collection("chats").document(chatId)
                .collection("messages").document(messageId)
                .set(messageMap)
                .addOnSuccessListener(aVoid -> {
                    NotificationHelper.sendNotification(this,receiverFCMToken, name, text, senderId, chatId,"message");
                })
                .addOnFailureListener(e -> Log.e("AKChats", "Failed to send message", e));

        Message textMessage = new Message(
                messageId, chatId, senderId, receiverId,
                text, null, "text", timestamp
        );
        dbHelper.insertMessage(textMessage, this);

        messageList.add(textMessage);
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        recyclerView.scrollToPosition(messageList.size() - 1);

        messageInput.setText("");
    }

    private void loadMessages() {
        messageList.clear();
        messageList.addAll(dbHelper.getMessagesByChatId(chatId));
        chatAdapter.notifyDataSetChanged();
        if (!messageList.isEmpty()) {
            recyclerView.scrollToPosition(messageList.size() - 1);
        }
    }

    private void listenForMessages() {
        firestore.collection("chats").document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("AKChats", "Firestore Listen Failed: " + error.getMessage());
                        return;
                    }
                    if (value != null && !value.isEmpty()) {
                        for (DocumentChange change : value.getDocumentChanges()) {
                            DocumentSnapshot document = change.getDocument();
                            String messageId = document.getString("messageId");
                            String messageType = document.getString("type");
                            long timestamp = document.getLong("timestamp");

                            Log.d("AKChats", "Received Message: " + messageId + ", Type: " + messageType);

                            if (!dbHelper.isMessageExists(messageId)) {
                                Message newMessage;
                                if ("image".equals(messageType)) {
                                    String base64Image = document.getString("chatImage");
                                    byte[] imageBytes = decodeBase64Image(base64Image);
                                    newMessage = new Message(
                                            messageId, chatId, document.getString("senderId"),
                                            document.getString("receiverId"), null,
                                            imageBytes, "image", timestamp
                                    );
                                } else {
                                    String text = document.getString("text");
                                    newMessage = new Message(
                                            messageId, chatId, document.getString("senderId"),
                                            document.getString("receiverId"), text,
                                            null, "text", timestamp
                                    );
                                }

                                long result = dbHelper.insertMessage(newMessage, this);
                                if (result != -1) {
                                    Log.d("AKChats", "Message Inserted to SQLite: " + messageId);
                                    messageList.add(newMessage);
                                    chatAdapter.notifyItemInserted(messageList.size() - 1);
                                    recyclerView.scrollToPosition(messageList.size() - 1);
                                } else {
                                    Log.e("AKChats", "Failed to Insert Message to SQLite");
                                }
                            }
                        }
                    } else {
                        Log.d("AKChats", "No New Messages in Firestore");
                    }
                });
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
