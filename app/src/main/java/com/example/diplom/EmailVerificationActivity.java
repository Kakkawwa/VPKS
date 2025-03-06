package com.example.diplom;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.diplom.databinding.ActivityEmailVerificationBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EmailVerificationActivity extends AppCompatActivity {

    private ActivityEmailVerificationBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private Handler handler = new Handler();
    private static final long RESEND_TIMEOUT_MS = 60000;
    private long lastResendTime = 0;
    private boolean isChecking = false;
    private static final long RESEND_TIMEOUT_NORMAL = 60000; // 60 секунд
    private static final long RESEND_TIMEOUT_BLOCKED = 600000; // 10 минут
    private long currentResendTimeout = RESEND_TIMEOUT_NORMAL;
    private String email, userId, name, surname;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEmailVerificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Получаем данные из Intent
        email = getIntent().getStringExtra("email");
        userId = getIntent().getStringExtra("userId");
        name = getIntent().getStringExtra("name");
        surname = getIntent().getStringExtra("surname");

        binding.verificationInfo.setText("Мы отправили вам письмо на " + email + ". Перейдите по ссылке в письме и вернитесь в приложение.");

        SpannableString resendText = new SpannableString("На вашей почте или в папке 'Спам' нет письма? Давайте отправим его повторно.");
        ForegroundColorSpan purpleColor = new ForegroundColorSpan(Color.parseColor("#800080")); // Фиолетовый цвет
        StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);
        ClickableSpan resendClick = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
            }
            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
            }
        };

        SpannableString wrongEmailText = new SpannableString("Ввели неверную почту? Нажмите сюда, чтобы ввести другую почту.");
        ClickableSpan wrongEmailClick = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                restartRegistration();
            }
            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
            }
        };
        int startWrongEmail = wrongEmailText.toString().indexOf("Нажмите сюда, чтобы ввести другую почту");
        wrongEmailText.setSpan(wrongEmailClick, startWrongEmail, startWrongEmail + 36, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        wrongEmailText.setSpan(boldSpan, startWrongEmail, startWrongEmail + 36, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        wrongEmailText.setSpan(purpleColor, startWrongEmail, startWrongEmail + 36, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        binding.wrongEmailText.setText(wrongEmailText);
        binding.wrongEmailText.setMovementMethod(LinkMovementMethod.getInstance());

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            goToLogin();
            return;
        }
        startEmailVerificationCheck();
    }

    private void startEmailVerificationCheck() {
        isChecking = true;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isChecking) return;

                FirebaseUser user = auth.getCurrentUser();
                if (user != null) {
                    user.reload().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser updatedUser = auth.getCurrentUser(); // Получаем обновленного пользователя
                            if (updatedUser != null && updatedUser.isEmailVerified()) {
                                isChecking = false;
                                saveUserToFirestore(updatedUser.getUid(), updatedUser.getEmail(), name, surname);
                            } else {
                                handler.postDelayed(this, 5000);
                            }
                        }
                    });
                }
                else {
                    isChecking = false;
                    goToLogin();
                }
            }
        }, 5000);
    }
    private void restartRegistration() {
        auth.signOut();
        Intent intent = new Intent(this, RegisterActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void saveUserToFirestore(String userId, String email, String name, String surname) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("email", email);
        userInfo.put("name", name);
        userInfo.put("surname", surname);

        db.collection("Users").document(userId)
                .set(userInfo)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(EmailVerificationActivity.this, "Email подтвержден, аккаунт создан!", Toast.LENGTH_SHORT).show();
                    goToLogin();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EmailVerificationActivity.this, "Ошибка сохранения данных: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void goToLogin() {
        startActivity(new Intent(EmailVerificationActivity.this, LoginActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isChecking = false;
    }
}
