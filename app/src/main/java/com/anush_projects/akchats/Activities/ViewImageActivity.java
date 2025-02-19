package com.anush_projects.akchats.Activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.anush_projects.akchats.R;
import com.jsibbold.zoomage.ZoomageView;

public class ViewImageActivity extends AppCompatActivity {

    ImageView backBtn;
    ZoomageView fullscreenImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_image);

        backBtn = findViewById(R.id.backBtn);
        fullscreenImageView = findViewById(R.id.fullscreenImageView);

        backBtn.setOnClickListener(v -> finish());

        byte[] imageBytes = getIntent().getByteArrayExtra("imageBytes");
        if (imageBytes != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            fullscreenImageView.setImageBitmap(bitmap);
        }
    }
}