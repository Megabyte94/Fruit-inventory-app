package com.kenneth_bustamante.fruit_inventory_portfolio_app;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.Image;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.core.OrderBy;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements FruitAdapter.OnItemClickListener {

    private static final int GALLERY_REQUEST_CODE = 100;
    private Uri selectedImage;

    // Set RecyclerView vars
    RecyclerView recyclerView;
    FruitAdapter fruitAdapter;
    List<Fruit> fruitList;

    // Initialize activity views
    ConstraintLayout addNewFruitCL;
    ImageView favoritesIV;
    Dialog dialog;

    // Initialize Firebase Firestore database
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Activity result launcher for gallery intent
    private ActivityResultLauncher<Intent> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set activity views
        addNewFruitCL = findViewById(R.id.addNewFruitCL);
        favoritesIV = findViewById(R.id.favoritesIV);
        dialog = new Dialog(MainActivity.this);
        recyclerView = findViewById(R.id.fruitsRV);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        retrieveFruitsAndSetAdapter();

        // Set onClick listeners
        addNewFruitCL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Set dialog params
                dialog.setContentView(R.layout.add_new_fruit_dialog_layout);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                // Set dialog width programmatically
                setDialogWidth();

                // Define dialog views
                EditText amountET = dialog.findViewById(R.id.amountET);
                EditText fruitNameET = dialog.findViewById(R.id.fruitNameET);
                Button addBTN = dialog.findViewById(R.id.addBTN);
                Button uploadImageBTN = dialog.findViewById(R.id.uploadImageBTN);

                //-----
                // Set onClick listeners

                addBTN.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String fruitName = fruitNameET.getText().toString().trim();
                        int amount = !amountET.getText().toString().trim().isEmpty() ? Integer.parseInt(amountET.getText().toString().trim()) : 0;

                        if (fruitName.isEmpty() || amount <= 0) {
                            Toast.makeText(MainActivity.this, "Please enter valid fruit information", Toast.LENGTH_SHORT).show();
                        } else {
                            // Check if an image is selected
                            if (selectedImage != null) {
                                uploadImageToFirestore(selectedImage, fruitName, amount);

                                // Clear the selectedImage variable
                                selectedImage = null;
                            } else {
                                // Generate a unique document ID
                                String randomDocId = UUID.randomUUID().toString();

                                // Upload fruit information to Firestore database
                                uploadFruitInfoToFirestore(randomDocId, fruitName, amount);
                            }
                        }

                        // Retrieve fruits and show them in the RecyclerView
                        retrieveFruitsAndSetAdapter();
                        dialog.dismiss();
                    }
                });
                uploadImageBTN.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Check if permission to read external storage is granted
                        if (Build.VERSION.SDK_INT <= 32) {
                            // For API level 32 or lower, request READ_EXTERNAL_STORAGE permission
                            if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                                    != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                                        1);
                                Toast.makeText(MainActivity.this, "Please accept the permissions and try again", Toast.LENGTH_LONG).show();
                            } else {
                                launchGalleryIntent();
                            }
                        } else {
                            // For API level 33 or higher, request READ_MEDIA_IMAGES permission
                            if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.READ_MEDIA_IMAGES)
                                    != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{android.Manifest.permission.READ_MEDIA_IMAGES},
                                        1);
                                Toast.makeText(MainActivity.this, "Please accept the permissions and try again", Toast.LENGTH_LONG).show();
                            } else {
                                launchGalleryIntent();
                            }
                        }

                    }
                });

                // Show dialog
                dialog.show();
            }
        });

        favoritesIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, FavoritesActivity.class);
                startActivity(intent);
            }
        });

        // Initialize the activity result launcher for gallery intent
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            Intent data = result.getData();
                            if (data != null) {
                                selectedImage = data.getData();
                                String[] filePathColumn = {MediaStore.Images.Media.DATA};
                                Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                                if (cursor != null && cursor.moveToFirst()) {
                                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                                    String picturePath = cursor.getString(columnIndex);
                                    cursor.close();
                                }
                            }
                        }
                    }
                });

    }

    public void retrieveFruitsAndSetAdapter() {
        // Retrieve fruit data from Firestore and convert to Fruit objects
        fruitList = new ArrayList<>();
        CollectionReference fruitsRef = db.collection("fruits");

        // Show progress dialog
        ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setMessage("Loading fruits...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        fruitsRef.orderBy("amount", Query.Direction.DESCENDING).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            String documentId = documentSnapshot.getId(); // Retrieve the document ID

                            String fruitName = documentSnapshot.getString("name");
                            int amount = documentSnapshot.getLong("amount").intValue();
                            String imageUrl = documentSnapshot.getString("image_url");

                            Fruit fruit = new Fruit(documentId, fruitName, amount, imageUrl);
                            fruitList.add(fruit);
                        }

                        // Create an instance of FruitAdapter
                        fruitAdapter = new FruitAdapter(MainActivity.this, fruitList, MainActivity.this::onItemClick);

                        // Set the adapter to the RecyclerView
                        recyclerView.setAdapter(fruitAdapter);

                        // Show Toast message to the user if there are no fruits in inventory
                        if (fruitList.isEmpty()) {
                            Toast.makeText(MainActivity.this, "No fruits in inventory", Toast.LENGTH_SHORT).show();
                        }

                        // Dismiss progress dialog
                        progressDialog.dismiss();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, "An error has occurred...", Toast.LENGTH_SHORT).show();

                        // Dismiss progress dialog
                        progressDialog.dismiss();
                    }
                });
    }

    // Method to check if there is an active internet connection
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void launchGalleryIntent() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(galleryIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String picturePath = cursor.getString(columnIndex);
                cursor.close();
            }
        }
    }

    private void uploadFruitInfoToFirestore(String docId, String fruitName, int amount) {
        // Create a new fruit document in the "fruits" collection
        Map<String, Object> fruit = new HashMap<>();
        fruit.put("name", fruitName);
        fruit.put("amount", amount);
        fruit.put("isFav", false);

        // Add the fruit document to Firestore
        db.collection("fruits")
                .document(docId)
                .set(fruit)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(MainActivity.this, "Fruit added successfully", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, "Failed to add fruit", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void uploadImageToFirestore(Uri imageUri, String fruitName, int amount) {
        // Show a progress dialog while uploading the image
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Adding fruit to Firebase Firestore database...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Generate a unique image filename
        String imageName = UUID.randomUUID().toString() + ".jpg";

        // Get a reference to the Firebase Storage
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();

        // Create a reference to the image file in Firebase Storage
        StorageReference imageRef = storageRef.child("images/" + imageName);

        // Upload the image file to Firebase Storage
        UploadTask uploadTask = imageRef.putFile(imageUri);

        // Listen for the upload task completion
        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // Get the download URL of the uploaded image
                imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri downloadUri) {
                        // Create a new fruit document in the "fruits" collection
                        Map<String, Object> fruit = new HashMap<>();
                        fruit.put("name", fruitName);
                        fruit.put("amount", amount);
                        fruit.put("image_url", downloadUri.toString());
                        fruit.put("isFav", false);

                        // Generate a unique document ID
                        String documentId = UUID.randomUUID().toString();

                        // Add the fruit document to Firestore
                        db.collection("fruits")
                                .document(documentId)
                                .set(fruit)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Toast.makeText(MainActivity.this, "Fruit added successfully", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(MainActivity.this, "Failed to add fruit", Toast.LENGTH_SHORT).show();
                                    }
                                });

                        retrieveFruitsAndSetAdapter();

                        // Dismiss the progress dialog
                        progressDialog.dismiss();
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
            }
        });
    }

    // Set dialog width
    public void setDialogWidth() {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int pxW = displayMetrics.widthPixels;
        ConstraintLayout mainConstraintLayout = dialog.findViewById(R.id.mainConstraintLayout);
        ViewGroup.LayoutParams layoutParams = mainConstraintLayout.getLayoutParams();
        layoutParams.width = pxW - pxW/20;
        mainConstraintLayout.setLayoutParams(layoutParams);
    }

    // Manage clicks on items in the RecyclerView
    @Override
    public void onItemClick(int position) {
        Fruit item = fruitList.get(position);
        
        String documentId = item.getDocumentId();
        String fruitName = item.getName();
        boolean hasImage = true;
        int fruitAmount = item.getAmount();

        // Set dialog params
        dialog.setContentView(R.layout.edit_fruit_info_dialog_layout);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // Set the dialog width
        setDialogWidth();

        // Define dialog views
        EditText amountET = dialog.findViewById(R.id.amountET);
        EditText fruitNameET = dialog.findViewById(R.id.fruitNameET);
        Button saveBTN = dialog.findViewById(R.id.saveBTN);
        ImageButton favBTN = dialog.findViewById(R.id.favBTN);
        ImageButton deleteBTN = dialog.findViewById(R.id.deleteBTN);

        // Set texts
        fruitNameET.setText(fruitName);
        amountET.setText(String.valueOf(fruitAmount));

        // Get document reference
        DocumentReference docRef = db.collection("fruits").document(documentId);
        docRef.get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            boolean isFavField = documentSnapshot.getBoolean("isFav");

                            if (isFavField) {
                                favBTN.setImageResource(R.drawable.round_star_24);
                            }
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, "Error retrieving favorites info.", Toast.LENGTH_SHORT).show();
                    }
                });

        saveBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String fruitName = fruitNameET.getText().toString().trim();
                int amount = !amountET.getText().toString().trim().isEmpty() ? Integer.parseInt(amountET.getText().toString().trim()) : 0;

                if (fruitName.isEmpty() || amount <= 0) {
                    Toast.makeText(MainActivity.this, "Please enter valid fruit information", Toast.LENGTH_SHORT).show();
                } else {
                    // Get document reference
                    DocumentReference docRef = db.collection("fruits").document(documentId);

                    docRef.get()
                            .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                @Override
                                public void onSuccess(DocumentSnapshot documentSnapshot) {
                                    if (documentSnapshot.exists()) {
                                        Map<String, Object> updates = new HashMap<>();
                                        updates.put("name", fruitNameET.getText().toString().trim());
                                        updates.put("amount", Integer.parseInt(amountET.getText().toString().trim()));

                                        docRef.update(updates)
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        retrieveFruitsAndSetAdapter();
                                                        Toast.makeText(MainActivity.this, "Fruit information updated successfully", Toast.LENGTH_SHORT).show();
                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        Toast.makeText(MainActivity.this, "Sorry... An error has occurred", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    }
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(MainActivity.this, "Error updating fruit", Toast.LENGTH_SHORT).show();
                                }
                            });


                }

                // Retrieve fruits from database and show them in the RecyclerView
                retrieveFruitsAndSetAdapter();
                dialog.dismiss();
            }
        });
        favBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                docRef.get()
                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                if (documentSnapshot.exists()) {
                                    boolean isFavField = documentSnapshot.getBoolean("isFav");

                                    if (isFavField) {
                                        favBTN.setImageResource(R.drawable.round_star_border_24);

                                        Map<String, Object> updates = new HashMap<>();
                                        updates.put("isFav", false);

                                        docRef.update(updates)
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        Toast.makeText(MainActivity.this, "Fruit deleted from favorites list", Toast.LENGTH_SHORT).show();
                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        Toast.makeText(MainActivity.this, "Sorry... An error has occurred", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    }
                                    else {
                                        favBTN.setImageResource(R.drawable.round_star_24);

                                        Map<String, Object> updates = new HashMap<>();
                                        updates.put("isFav", true);

                                        docRef.update(updates)
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        Toast.makeText(MainActivity.this, "Fruit added to favorites list", Toast.LENGTH_SHORT).show();
                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        Toast.makeText(MainActivity.this, "Sorry... An error has occurred", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    }
                                }
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(MainActivity.this, "Error retrieving favorites info.", Toast.LENGTH_SHORT).show();
                            }
                        });

            }
        });
        deleteBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                docRef.delete()
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(MainActivity.this, "Fruit deleted successfully from database", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(MainActivity.this, "Error deleting fruit from database", Toast.LENGTH_SHORT).show();
                            }
                        });

                retrieveFruitsAndSetAdapter();
                dialog.dismiss();
            }
        });

        // Show dialog
        dialog.show();

    }

}

// Copyright © 2023 Kenneth Bustamante Zuluaga