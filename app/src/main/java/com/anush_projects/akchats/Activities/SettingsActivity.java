package com.anush_projects.akchats.Activities;

import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.anush_projects.akchats.Database.DatabaseHelper;
import com.anush_projects.akchats.R;
import com.anush_projects.akchats.utils.FirebaseUtils;
import com.anush_projects.akchats.utils.ThemeHelper;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity {

    private ImageView backBtn, profileImage, save;
    private EditText userName;
    private TextView userMail, switchTheme, forgotPassword, deleteChat, deleteAllChat, deleteAccount, logout;
    private FirebaseFirestore db;
    private static final int PICK_IMAGE_REQUEST = 1;
    public static final int CAMERA_REQUEST = 102;
    private String profileImageBase64 = "";
    private LinearLayout progressLay;
    private LottieAnimationView progressBar;
    private String userEmailTxt, userNameTxt;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initViewsAndListeners();
        loadUserProfile();
    }

    private void initViewsAndListeners() {
        db = FirebaseFirestore.getInstance();
        backBtn = findViewById(R.id.backBtn);
        profileImage = findViewById(R.id.profileImage);
        save = findViewById(R.id.save);
        userName = findViewById(R.id.userName);
        userMail = findViewById(R.id.userMail);
        switchTheme = findViewById(R.id.switchTheme);
        forgotPassword = findViewById(R.id.forgotPassword);
        deleteChat = findViewById(R.id.deleteChat);
        deleteAllChat = findViewById(R.id.deleteAllChat);
        deleteAccount = findViewById(R.id.deleteAccount);
        logout = findViewById(R.id.logout);
        progressBar = findViewById(R.id.progressBar);
        progressLay = findViewById(R.id.progressLay);

        backBtn.setOnClickListener(v -> finish());

        profileImage.setOnClickListener(v -> openBottomDialog());

        save.setOnClickListener(v -> updateUserProfile());

        switchTheme.setOnClickListener(v -> showThemeDialog());

        logout.setOnClickListener(v -> {
            logoutDialog(getString(R.string.logout));
        });

        deleteAccount.setOnClickListener(v -> {
            logoutDialog(getString(R.string.delete_account));
        });

        deleteAllChat.setOnClickListener(v -> {
            logoutDialog(getString(R.string.delete_all_chats));
        });

    }

    private void loadUserProfile() {
        progressLay.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        db.collection("users").document(FirebaseAuth.getInstance().getCurrentUser().getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    profileImageBase64 = documentSnapshot.getString("profileImageBase64");
                    userNameTxt = documentSnapshot.getString("name");
                    userEmailTxt = documentSnapshot.getString("email");
                    if (profileImageBase64 != null && !profileImageBase64.isEmpty()) {
                        byte[] decodedString = Base64.decode(profileImageBase64, Base64.DEFAULT);
                        profileImage.setImageBitmap(BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length));
                    } else {
                        profileImage.setImageResource(R.drawable.app_icon);
                    }

                    if (userNameTxt != null && !userNameTxt.isEmpty()){
                        userName.setText(userNameTxt);
                    } else {
                        userName.setText(getString(R.string.kindly_contact_admin));
                    }

                    if (userEmailTxt != null && !userEmailTxt.isEmpty()){
                        userMail.setText(userEmailTxt);
                    } else {
                        userMail.setText(getString(R.string.kindly_contact_admin));
                    }

                    progressLay.setVisibility(View.GONE);
                    progressBar.setVisibility(View.GONE);

                }).addOnFailureListener(e ->{
                    progressLay.setVisibility(View.GONE);
                    progressBar.setVisibility(View.GONE);
                });
    }

    private void openBottomDialog() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_dialog_layout, null);
        bottomSheetDialog.setContentView(view);

        LinearLayout cameraOption = view.findViewById(R.id.option_camera);
        LinearLayout galleryOption = view.findViewById(R.id.option_gallery);

        cameraOption.setOnClickListener(v -> {
            openCamera();
            bottomSheetDialog.dismiss();
        });

        galleryOption.setOnClickListener(v -> {
            openGallery();
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
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
                profileImageBase64 = Base64.encodeToString(imageBytes, Base64.DEFAULT);

                Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                profileImage.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
            }
        }  else if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
            if (imageUri != null) {
                processImage(imageUri);
            } else {
                Toast.makeText(this, "Camera image not found", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void processImage(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            options.inSampleSize = calculateInSampleSize(options, 480, 480);
            options.inJustDecodeBounds = false;

            inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream);
            byte[] imageBytes = outputStream.toByteArray();
            profileImageBase64 = Base64.encodeToString(imageBytes, Base64.DEFAULT);

            profileImage.setImageBitmap(bitmap);

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
        }
    }

    private byte[] getCompressedImageBytes(Uri imageUri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(imageUri);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(inputStream, null, options);
        inputStream.close();

        options.inSampleSize = calculateInSampleSize(options, 480, 480); // Profile picture size
        options.inJustDecodeBounds = false;

        inputStream = getContentResolver().openInputStream(imageUri);
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
        inputStream.close();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 40, outputStream);
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

    private void updateUserProfile() {
        String updatedName = userName.getText().toString().trim();
        if (updatedName.isEmpty()) {
            Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", updatedName);
        if (!profileImageBase64.isEmpty()) {
            updates.put("profileImageBase64", profileImageBase64);
        }

        db.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> finish())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show());
    }

    private void showThemeDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(SettingsActivity.this, R.style.ThemeAlertDialog);
        alertDialog.setTitle("Choose Theme");
        String[] items = {"Light", "Dark", "System default"};
        SharedPreferences themePref = PreferenceManager.getDefaultSharedPreferences(this);
        String theme = themePref.getString("themePref", ThemeHelper.DEFAULT_MODE);
        int checkedItem = 0;
        if (theme.equals(ThemeHelper.LIGHT_MODE)) {
            checkedItem = 0;
        } else if (theme.equals(ThemeHelper.DARK_MODE)) {
            checkedItem = 1;
        } else {
            checkedItem = 2;
        }
        SharedPreferences.Editor prefEditor = themePref.edit();
        alertDialog.setSingleChoiceItems(items, checkedItem, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        prefEditor.putString("themePref", ThemeHelper.LIGHT_MODE);
                        prefEditor.commit();
                        ThemeHelper.applyTheme(ThemeHelper.LIGHT_MODE);
                        break;
                    case 1:
                        prefEditor.putString("themePref", ThemeHelper.DARK_MODE);
                        prefEditor.commit();
                        ThemeHelper.applyTheme(ThemeHelper.DARK_MODE);
                        break;
                    case 2:
                        prefEditor.putString("themePref", ThemeHelper.DEFAULT_MODE);
                        prefEditor.commit();
                        ThemeHelper.applyTheme(ThemeHelper.DEFAULT_MODE);
                        break;
                }
            }
        });
        AlertDialog alert = alertDialog.create();
        alert.setCanceledOnTouchOutside(true);
        alert.show();
    }

    private void logoutDialog(String from) {
        final Dialog dialog = new Dialog(SettingsActivity.this);
        Display display = getWindowManager().getDefaultDisplay();
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setContentView(R.layout.logout_dialog);
        dialog.getWindow().setLayout(display.getWidth() * 80 / 100, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);

        TextView title = dialog.findViewById(R.id.headerTxt);
        TextView subTxt = dialog.findViewById(R.id.subTxt);
        TextView deleteAccountTxt = dialog.findViewById(R.id.deleteAccountTxt);
        TextView userEmail = dialog.findViewById(R.id.userEmail);
        EditText userPassword = dialog.findViewById(R.id.userPassword);
        TextView yes = dialog.findViewById(R.id.yes);
        TextView no = dialog.findViewById(R.id.no);

        if (from.equalsIgnoreCase(getString(R.string.logout))) {

            title.setText(getString(R.string.logout));
            subTxt.setText(getString(R.string.reallySignOut));

            yes.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FirebaseAuth.getInstance().signOut();
                    dialog.dismiss();
                    startActivity(new Intent(getApplicationContext(), SignupActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                }
            });

            no.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
        } else if (from.equalsIgnoreCase(getString(R.string.delete_account))) {

            title.setText(getString(R.string.delete_account));
            subTxt.setText(getString(R.string.delete_your_account));
            userEmail.setText(userEmailTxt);

            deleteAccountTxt.setVisibility(View.VISIBLE);
            userPassword.setVisibility(View.VISIBLE);
            userEmail.setVisibility(View.VISIBLE);

            yes.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!userPassword.getText().toString().isEmpty()) {
                        deleteAccount(userEmailTxt, userPassword.getText().toString().trim());
                        userPassword.setError(null);
                        dialog.dismiss();
                    } else {
                        userPassword.setError(getString(R.string.enter_your_password));
                    }
                }
            });

            no.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
        } else if (from.equalsIgnoreCase(getString(R.string.delete_all_chats))) {

            title.setText(getString(R.string.delete_all_chats));
            subTxt.setText(getString(R.string.delete_your_chats));

            yes.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clearAllChats();
                    dialog.dismiss();
                }
            });

            no.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
        }
        dialog.show();
    }

    public void clearAllChats() {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        String currentUserId = FirebaseUtils.getCurrentUserId();

        // Fetch all chats of the user
        firestore.collection("chats")
                .whereArrayContains("userIds", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot chatDoc : queryDocumentSnapshots) {
                        String chatId = chatDoc.getId();

                        firestore.collection("chats").document(chatId)
                                .collection("messages")
                                .get()
                                .addOnSuccessListener(messages -> {
                                    for (DocumentSnapshot messageDoc : messages) {
                                        messageDoc.getReference().delete();
                                    }
                                })
                                .addOnFailureListener(e -> Log.e("AKChats", "Failed to delete messages from Firestore", e));
                    }
                })
                .addOnFailureListener(e -> Log.e("AKChats", "Failed to fetch chats", e));

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        dbHelper.clearAllChats();
    }


    private void deleteAccount(String userEmail, String userPassword) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(userEmail, userPassword);
        user.reauthenticate(credential).addOnCompleteListener(reauthTask -> {
            if (reauthTask.isSuccessful()) {

                Task<Void> deleteChatsTask = db.collection("chats")
                        .whereEqualTo("participants." + user.getUid(), true)
                        .get()
                        .continueWithTask(task -> {
                            WriteBatch batch = db.batch();
                            for (DocumentSnapshot doc : task.getResult()) {
                                batch.delete(doc.getReference());
                            }
                            return batch.commit();
                        });

                Task<Void> deleteProfileTask = db.collection("users")
                        .document(user.getUid())
                        .delete();

                Tasks.whenAllComplete(deleteChatsTask, deleteProfileTask).addOnCompleteListener(allTasks -> {

                    user.delete().addOnCompleteListener(deleteTask -> {
                        if (deleteTask.isSuccessful()) {
                            dbHelper.clearAllChats();

                            auth.signOut();
                            Intent intent = new Intent(SettingsActivity.this, SignupActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Failed to delete account", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
            } else {
                Toast.makeText(this, "Reauthentication failed. Check your password", Toast.LENGTH_SHORT).show();
            }
        });
    }
}