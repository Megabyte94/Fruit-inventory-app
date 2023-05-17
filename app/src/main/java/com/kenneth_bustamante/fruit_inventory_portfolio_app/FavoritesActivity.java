package com.kenneth_bustamante.fruit_inventory_portfolio_app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class FavoritesActivity extends AppCompatActivity implements FruitAdapter.OnItemClickListener {
    // Set RecyclerView vars
    RecyclerView recyclerView;
    FruitAdapter fruitAdapter;
    List<Fruit> fruitList;

    // Initialize Firebase Firestore database
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        // Define views and variables
        recyclerView = findViewById(R.id.fruitsRV);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Retrieve fruits and show them in the RecyclerView
        retrieveFruitsAndSetAdapter();

    }

    public void retrieveFruitsAndSetAdapter() {
        // Retrieve fruit data from Firestore and convert to Fruit objects
        fruitList = new ArrayList<>();
        CollectionReference fruitsRef = db.collection("fruits");

        // Show progress dialog
        ProgressDialog progressDialog = new ProgressDialog(FavoritesActivity.this);
        progressDialog.setMessage("Loading fruits...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Get fruits list from database
        fruitsRef.whereEqualTo("isFav", true).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        String documentId = documentSnapshot.getId(); // Retrieve the document ID

                        String fruitName = documentSnapshot.getString("name");
                        int amount = documentSnapshot.getLong("amount").intValue();
                        String imageUrl = documentSnapshot.getString("image_url");

                        Fruit fruit = new Fruit(documentId, fruitName, amount, imageUrl);
                        fruitList.add(fruit);
                    }

                    // Create an instance of FruitAdapter
                    fruitAdapter = new FruitAdapter(FavoritesActivity.this, fruitList, FavoritesActivity.this::onItemClick);

                    // Set the adapter to the RecyclerView
                    recyclerView.setAdapter(fruitAdapter);
                    
                    // Show Toast if there are no favorites
                    if (fruitList.isEmpty()) {
                        Toast.makeText(this, "No favorites", Toast.LENGTH_SHORT).show();
                    }

                    // Dismiss progress dialog
                    progressDialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(FavoritesActivity.this, "An error has occurred...", Toast.LENGTH_SHORT).show();

                    // Dismiss progress dialog
                    progressDialog.dismiss();
                });
    }

    @Override
    public void onItemClick(int position) {
        // Here we can manage clicks on RecyclerView items
        // Empty content
    }
}

// Copyright © 2023 Kenneth Bustamante Zuluaga