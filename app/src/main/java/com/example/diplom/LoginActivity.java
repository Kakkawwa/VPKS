package com.example.diplom;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.diplom.databinding.ActivityLoginBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth auth;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Проверяем, авторизован ли пользователь
        preferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        if (preferences.getBoolean("isLoggedIn", false)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();

        binding.LoginButton.setOnClickListener(v -> loginUser());

        binding.RegisterTextView.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class))
        );

        binding.ForgotPasswordTextView.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, ResetPasswordActivity.class))
        );
    }

    @Override
    public void onBackPressed() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Выйти из приложения?")
                .setMessage("Вы уверены, что хотите выйти?")
                .setPositiveButton("Да", (dialog, which) -> {
                    dialog.dismiss();
                    super.onBackPressed(); // Завершает активность
                })
                .setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void loginUser() {
        String email = binding.EmailEditText.getText().toString().trim();
        String password = binding.PasswordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Поля не могут быть пустыми", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null && user.isEmailVerified()) {
                            saveUserDataToFirestore(user.getUid());

                            // Сохраняем состояние авторизации
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putBoolean("isLoggedIn", true);
                            editor.apply();

                            Toast.makeText(LoginActivity.this, "Вход выполнен успешно!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Убираем из стека
                            startActivity(intent);
                        } else {
                            Toast.makeText(LoginActivity.this, "Подтвердите email! Проверьте почту.", Toast.LENGTH_LONG).show();
                            auth.signOut();
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "Ошибка входа: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserDataToFirestore(String userId) {
        SharedPreferences registrationPrefs = getSharedPreferences("RegistrationData_" + userId, MODE_PRIVATE);
        String name = registrationPrefs.getString("name", null);
        String surname = registrationPrefs.getString("surname", null);
        String email = registrationPrefs.getString("email", null);

        if (name != null && surname != null && email != null) {
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("name", name);
            userInfo.put("surname", surname);
            userInfo.put("email", email);

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("Users").document(userId)
                    .set(userInfo)
                    .addOnSuccessListener(unused -> {
                        // Очистка данных после успешного сохранения
                        registrationPrefs.edit().clear().apply();
                    })
                    .addOnFailureListener(e -> {
                    });
        }
    }


}
