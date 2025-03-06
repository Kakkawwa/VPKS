package com.example.diplom;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

public class EditBookingActivity extends AppCompatActivity {

    private EditText etDate, etHours, etPeople, etComment;
    private Button btnSave;
    private String bookingId;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_booking);

        etDate = findViewById(R.id.etDate);
        etHours = findViewById(R.id.etHours);
        etPeople = findViewById(R.id.etPeople);
        etComment = findViewById(R.id.etComment);
        btnSave = findViewById(R.id.btnSave);

        db = FirebaseFirestore.getInstance();
        bookingId = getIntent().getStringExtra("bookingId");

        // Загрузите данные бронирования из Firestore и заполните поля (пример)
        db.collection("Bookings").document(bookingId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        etDate.setText(documentSnapshot.getString("date"));
                        etHours.setText(String.valueOf(documentSnapshot.getLong("hours")));
                        etPeople.setText(String.valueOf(documentSnapshot.getLong("people")));
                        etComment.setText(documentSnapshot.getString("comment"));
                    }
                });

        btnSave.setOnClickListener(v -> {
            String newDate = etDate.getText().toString().trim();
            int newHours = Integer.parseInt(etHours.getText().toString().trim());
            int newPeople = Integer.parseInt(etPeople.getText().toString().trim());
            String newComment = etComment.getText().toString().trim();

            db.collection("Bookings").document(bookingId)
                    .update("date", newDate, "hours", newHours, "people", newPeople, "comment", newComment)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(EditBookingActivity.this, "Бронирование обновлено", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(EditBookingActivity.this, "Ошибка обновления: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        });
    }
}
