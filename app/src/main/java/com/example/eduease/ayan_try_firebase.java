package com.example.eduease;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ayan_try_firebase {

    // Initialize the secondary Firebase App
    public static void initSecondaryFirebase(Context context) {
        try {
            InputStream serviceAccount = context.getAssets().open("google-services-ni-ayan.json");

            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setApplicationId("1:882141634417:android:ac69b51d83d01def3460d0")
                    .setApiKey("AIzaSyBlECTZf28SbEc4xHsz7JnH99YtTw6T58I")
                    .setProjectId("edu-ease-ni-ayan")
                    .setDatabaseUrl("https://edu-ease-ni-ayan-default-rtdb.firebaseio.com/")  // <-- ADD THIS
                    .build();

            if (FirebaseApp.getApps(context).size() < 2) {
                FirebaseApp.initializeApp(context, options, "Secondary");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // Create and fetch hierarchical data
    public static void createAndFetchStudent(Context context) {
        initSecondaryFirebase(context);

        Log.d("FIREBASETEST", "Using FirebaseApp: " + FirebaseApp.getInstance("Secondary").getName());


        DatabaseReference db = FirebaseDatabase.getInstance(FirebaseApp.getInstance("Secondary")).getReference();



        // Create sample student data
        Map<String, Object> studentData = new HashMap<>();
        studentData.put("name", "Ayan");
        studentData.put("age", 21);
        studentData.put("grade", "A");

        // Write to students/student1
        db.child("students").child("student1").setValue(studentData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FIREBASETEST", "Student data created successfully.");

                    // Now fetch it
                    db.child("students").child("student1")
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.exists()) {
                                        String name = dataSnapshot.child("name").getValue(String.class);
                                        Long age = dataSnapshot.child("age").getValue(Long.class);
                                        String grade = dataSnapshot.child("grade").getValue(String.class);

                                        Log.d("FIREBASE_TEST", "Fetched student info: Name: " + name + ", Age: " + age + ", Grade: " + grade);
                                    } else {
                                        Log.d("FIREBASE_TEST", "No data found for student1");
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError error) {
                                    Log.e("FIREBASETEST", "Data fetch cancelled", error.toException());
                                }
                            });

                })
                .addOnFailureListener(e -> {
                    Log.e("FIREBASETEST", "Failed to create student data", e);
                });
    }
}
