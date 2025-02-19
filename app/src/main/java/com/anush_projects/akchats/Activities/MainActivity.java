package com.anush_projects.akchats.Activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.airbnb.lottie.LottieAnimationView;
import com.anush_projects.akchats.Adapters.UserAdapter;
import com.anush_projects.akchats.Database.DatabaseHelper;
import com.anush_projects.akchats.Models.User;
import com.anush_projects.akchats.R;
import com.anush_projects.akchats.utils.FirebaseUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener{
    private RecyclerView usersRecyclerView;
    private LottieAnimationView progressBar;
    private FloatingActionButton fab;
    private LinearLayout progressLay;
    private ImageView settings;
    private ShapeableImageView profileImage;
    private SwipeRefreshLayout swipeLayout;
    private UserAdapter userAdapter;
    private List<User> userList = new ArrayList<>();
    private final Handler handler = new Handler();
    private SharedPreferences preferences;
    private FirebaseFirestore db;
    private boolean isFirstLoad = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupRecyclerView();
        loadUserProfile();
        initialLoadUsers();
    }


    private void initViews() {
        preferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        db = FirebaseFirestore.getInstance();
        usersRecyclerView = findViewById(R.id.usersRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        progressLay = findViewById(R.id.progressLay);
        settings = findViewById(R.id.settings);
        profileImage = findViewById(R.id.profileImage);
        fab = findViewById(R.id.getContacts);

        swipeLayout = findViewById(R.id.swipeLayout);
        swipeLayout.setColorSchemeColors(getResources().getColor(R.color.dark_blue));
        swipeLayout.setOnRefreshListener(this);

        settings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        fab.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ContactsActivity.class);
            startActivity(intent);
        });
    }


    private void setupRecyclerView() {
        userAdapter = new UserAdapter(userList, user -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("receiverId", user.getUserId());
            intent.putExtra("receiverName", user.getName());
            intent.putExtra("chatId", FirebaseUtils.getChatId(FirebaseUtils.getCurrentUserId(), user.getUserId())); // Add chatId
            startActivity(intent);
        });
        usersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        usersRecyclerView.setAdapter(userAdapter);
    }



    private void loadUserProfile() {
        db.collection("users").document(FirebaseAuth.getInstance().getCurrentUser().getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String profileImageBase64 = documentSnapshot.getString("profileImageBase64");
                    if (profileImageBase64 != null && !profileImageBase64.isEmpty()) {
                        byte[] decodedString = Base64.decode(profileImageBase64, Base64.DEFAULT);
                        profileImage.setImageBitmap(BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length));
                    } else {
                        profileImage.setImageResource(R.drawable.app_icon);
                    }
                }).addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load profile image", Toast.LENGTH_SHORT).show()
                );
    }

    private void initialLoadUsers() {
        progressLay.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        List<String> chattedUserIds = dbHelper.getChattedUserIds();

        if (chattedUserIds.isEmpty()) {
            progressLay.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            swipeLayout.setRefreshing(false);
            return;
        }

        db.collection("users").whereIn("userId", chattedUserIds).get().addOnSuccessListener(value -> {
            userList.clear();
            for (DocumentSnapshot doc : value.getDocuments()) {
                User user = doc.toObject(User.class);
                userList.add(user);
            }
            userAdapter.notifyDataSetChanged();
            progressLay.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            swipeLayout.setRefreshing(false);
        }).addOnFailureListener(e -> {
            progressLay.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            swipeLayout.setRefreshing(false);
        });
    }


    private void updateUsers() {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        List<String> chattedUserIds = dbHelper.getChattedUserIds();

        if (chattedUserIds.isEmpty()) {
            if (!userList.isEmpty()) {
                userList.clear();
                userAdapter.notifyDataSetChanged();
            }
            return;
        }

        db.collection("users").whereIn("userId", chattedUserIds).get().addOnSuccessListener(value -> {
            List<User> newList = new ArrayList<>();
            for (DocumentSnapshot doc : value.getDocuments()) {
                User user = doc.toObject(User.class);
                newList.add(user);
            }

            if (!userList.equals(newList)) {
                userList.clear();
                userList.addAll(newList);
                userAdapter.notifyDataSetChanged();
            }
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Failed to update users", Toast.LENGTH_SHORT).show()
        );
    }



    private void startMessageCheck() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isFirstLoad) {
                    updateUsers();
                }
                handler.postDelayed(this, 1000); // 5 seconds interval
            }
        }, 1000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isFirstLoad) {
            initialLoadUsers();
        }
        loadUserProfile();
        startMessageCheck();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onRefresh() {
        initialLoadUsers();
    }
}
