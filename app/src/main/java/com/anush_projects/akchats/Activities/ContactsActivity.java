package com.anush_projects.akchats.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.airbnb.lottie.LottieAnimationView;
import com.anush_projects.akchats.Adapters.ContactsAdapter;
import com.anush_projects.akchats.Models.ContactsModel;
import com.anush_projects.akchats.R;
import com.anush_projects.akchats.utils.FirebaseUtils;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ContactsActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener{

    private RecyclerView contactsRecyclerView;
    private ImageView backBtn;
    private ContactsAdapter contactsAdapter;
    private List<ContactsModel> contactList = new ArrayList<>();
    private FirebaseFirestore db;
    private LinearLayout progressLay;
    private LottieAnimationView progressBar;
    private SwipeRefreshLayout swipeLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        db = FirebaseFirestore.getInstance();
        backBtn = findViewById(R.id.backBtn);

        progressBar = findViewById(R.id.progressBar);
        progressLay = findViewById(R.id.progressLay);

        swipeLayout = findViewById(R.id.swipeLayout);
        swipeLayout.setColorSchemeColors(getResources().getColor(R.color.dark_blue));
        swipeLayout.setOnRefreshListener(this);

        backBtn.setOnClickListener(v -> finish());

        contactsRecyclerView = findViewById(R.id.contactsRecyclerView);
        contactsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        contactsAdapter = new ContactsAdapter(contactList, contact -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("receiverId", contact.getUserId());
            intent.putExtra("receiverName", contact.getName());
            intent.putExtra("chatId", FirebaseUtils.getChatId(FirebaseUtils.getCurrentUserId(), contact.getUserId()));
            startActivity(intent);
        });

        contactsRecyclerView.setAdapter(contactsAdapter);
        loadAllUsers();
    }

    private void loadAllUsers() {
        progressLay.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        db.collection("users").get().addOnSuccessListener(queryDocumentSnapshots -> {
            contactList.clear();
            queryDocumentSnapshots.forEach(doc -> {
                ContactsModel user = doc.toObject(ContactsModel.class);
                if (!user.getUserId().equals(FirebaseUtils.getCurrentUserId())) {
                    contactList.add(user);
                }
            });

            progressLay.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            swipeLayout.setRefreshing(false);

            contactsAdapter.notifyDataSetChanged();
        }).addOnFailureListener(e ->{
            progressLay.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            swipeLayout.setRefreshing(false);
        });

    }

    @Override
    public void onRefresh() {
        loadAllUsers();
    }
}