package com.example.diplom;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class EmailVerificationDialogFragment extends DialogFragment {

    private FirebaseAuth auth;
    private Handler handler = new Handler();
    private boolean isChecking = false;
    private boolean canResendEmail = false; // Флаг для повторной отправки

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            dismiss();
            return null;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setTitle("Подтверждение Email")
                .setMessage("Мы отправили вам письмо с подтверждением. Перейдите по ссылке в письме, и окно закроется автоматически.")
                .setCancelable(false)
                .setPositiveButton("Ок", (dialog, which) -> { })
                .setNegativeButton("Повторно отправить", (dialog, which) -> {
                    if (canResendEmail) {
                        resendVerificationEmail();
                    } else {
                        Toast.makeText(getContext(), "Подождите немного перед повторной отправкой", Toast.LENGTH_SHORT).show();
                    }
                });

        startEmailVerificationCheck();
        enableResendAfterDelay(); // Включаем возможность повторной отправки через 30 сек
        return builder.create();
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
                        if (user.isEmailVerified()) {
                            isChecking = false;
                            Toast.makeText(getContext(), "Email подтвержден!", Toast.LENGTH_SHORT).show();
                            dismiss();
                        } else {
                            handler.postDelayed(this, 5000); // Проверяем каждые 5 секунд
                        }
                    });
                }
            }
        }, 5000);
    }

    private void enableResendAfterDelay() {
        handler.postDelayed(() -> canResendEmail = true, 30000); // Разрешаем повторную отправку через 30 секунд
    }

    private void resendVerificationEmail() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            user.sendEmailVerification().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(getContext(), "Письмо повторно отправлено!", Toast.LENGTH_SHORT).show();
                    canResendEmail = false;
                    enableResendAfterDelay(); // Снова запретим повторную отправку на 30 секунд
                } else {
                    Toast.makeText(getContext(), "Ошибка повторной отправки: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        isChecking = false;
    }
}
