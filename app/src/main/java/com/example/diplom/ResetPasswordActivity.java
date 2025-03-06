package com.example.diplom;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import com.example.diplom.databinding.ActivityResetPasswordBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class ResetPasswordActivity extends AppCompatActivity {

    private ActivityResetPasswordBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityResetPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance(); // Инициализируем Firestore

        binding.ResetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkEmailInFirestore();
            }
        });

        binding.BackToLoginTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Закрывает активность и возвращает на LoginActivity
            }
        });
    }

    private void checkEmailInFirestore() {
        String email = binding.EmailEditText.getText().toString().trim();

        if (email.isEmpty()) {
            Toast.makeText(this, "Введите email", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("Users") // Имя коллекции в Firestore
                .whereEqualTo("email", email) // Ищем пользователя по email
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        boolean emailExists = false;

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            emailExists = true;
                            break;
                        }

                        if (emailExists) {
                            sendPasswordResetEmail(email);
                        } else {
                            Toast.makeText(this, "Эта почта не подтверждена", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(this, "Ошибка запроса к базе данных", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void sendPasswordResetEmail(String email) {
        auth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Письмо для сброса пароля отправлено", Toast.LENGTH_LONG).show();
                finish();
            } else {
                Toast.makeText(this, "Ошибка: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}

