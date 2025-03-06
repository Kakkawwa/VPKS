package com.example.diplom;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Toast;

import com.example.diplom.databinding.ActivityRegisterBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();

        // Кнопка регистрации
        binding.RegisterButton.setOnClickListener(v -> registerUser());

        // Переход на страницу входа
        binding.LoginTextView.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
        });

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

    private void registerUser() {
        String email = binding.EmailEditText.getText().toString().trim();
        String password = binding.PasswordEditText.getText().toString().trim();
        String name = binding.NameEditText.getText().toString().trim();
        String surname = binding.SurnameEditText.getText().toString().trim();

        boolean isValid = true;

        // Очистка прошлых ошибок
        binding.nameInputLayout.setError(null);
        binding.surnameInputLayout.setError(null);
        binding.emailInputLayout.setError(null);
        binding.passwordInputLayout.setError(null);

        // Проверка имени
        if (name.isEmpty()) {
            binding.nameInputLayout.setError("Введите имя");
            isValid = false;
        }

        // Проверка фамилии
        if (surname.isEmpty()) {
            binding.surnameInputLayout.setError("Введите фамилию");
            isValid = false;
        }

        // Проверка email
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInputLayout.setError("Введите корректный email");
            isValid = false;
        }

        // Проверка пароля
        if (password.isEmpty() || password.length() < 6) {
            binding.passwordInputLayout.setError("Пароль должен содержать минимум 6 символов");
            isValid = false;
        }

        if (!isValid) {
            return;
        }

        // Регистрация в Firebase
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            // Используем уникальное имя файла, включающее UID
                            SharedPreferences registrationPrefs = getSharedPreferences("RegistrationData_" + user.getUid(), MODE_PRIVATE);
                            registrationPrefs.edit()
                                    .putString("name", name)
                                    .putString("surname", surname)
                                    .putString("email", email)
                                    .apply();

                            user.sendEmailVerification().addOnCompleteListener(emailTask -> {
                                if (emailTask.isSuccessful()) {
                                    Intent intent = new Intent(RegisterActivity.this, EmailVerificationActivity.class);
                                    intent.putExtra("userId", user.getUid());
                                    intent.putExtra("email", email);
                                    intent.putExtra("name", name);
                                    intent.putExtra("surname", surname);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    binding.emailInputLayout.setError("Ошибка отправки email: " + emailTask.getException().getMessage());
                                }
                            });
                        }
                    } else {
                        Toast.makeText(RegisterActivity.this, "Ошибка регистрации: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }


}
