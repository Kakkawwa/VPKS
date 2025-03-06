package com.example.diplom;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private TextView nameTextView, surnameTextView, emailTextView;
    private ImageButton logoutButton;
    private Button deleteAccountButton;
    private CircleImageView avatarImageView; // Аватарка
    private String currentAvatarUrl; // Для хранения URL аватарки

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        nameTextView = view.findViewById(R.id.nameTextView);
        surnameTextView = view.findViewById(R.id.surnameTextView);
        emailTextView = view.findViewById(R.id.emailTextView);
        logoutButton = view.findViewById(R.id.imageButton);
        deleteAccountButton = view.findViewById(R.id.button2);
        avatarImageView = view.findViewById(R.id.avatarImageView);

        // При нажатии на аватарку открываем полноэкранный просмотр
        avatarImageView.setOnClickListener(v -> {
            if (currentAvatarUrl != null && !currentAvatarUrl.isEmpty()) {
                Intent intent = new Intent(getActivity(), FullscreenImageActivity.class);
                intent.putExtra("imageUrl", currentAvatarUrl);
                startActivity(intent);
            }
        });

        loadUserProfile();

        // Выход из аккаунта
        logoutButton.setOnClickListener(v -> logoutUser());

        // Удаление аккаунта
        deleteAccountButton.setOnClickListener(v -> reauthenticateAndDelete());

        Button editProfileButton = view.findViewById(R.id.editProfileButton);
        editProfileButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), EditProfileActivity.class);
            startActivity(intent);
        });

        // После инициализации других кнопок в onCreate():
        Button myBookingsButton = view.findViewById(R.id.myBookingsButton);
        myBookingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), MyBookingsActivity.class);
            startActivity(intent);
        });


        Button myCoworkingsButton = view.findViewById(R.id.myCoworkingsButton);
        myCoworkingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), MyCoworkingsActivity.class);
            startActivity(intent);
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserProfile(); // Обновляем данные при каждом появлении фрагмента
    }

    private void loadUserProfile() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();

            db.collection("Users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            String surname = documentSnapshot.getString("surname");
                            String email = documentSnapshot.getString("email");
                            String avatarUrl = documentSnapshot.getString("avatarUrl");

                            nameTextView.setText("Имя: " + name);
                            surnameTextView.setText("Фамилия: " + surname);
                            emailTextView.setText("Email: " + email);

                            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                                currentAvatarUrl = avatarUrl; // сохраняем для полноэкранного просмотра
                                Glide.with(getContext()).load(avatarUrl).into(avatarImageView);
                            } else {
                                avatarImageView.setImageResource(R.drawable.img);
                            }
                        }
                    })
                    .addOnFailureListener(e -> nameTextView.setText("Ошибка загрузки профиля"));
        }
    }

    private void logoutUser() {
        auth.signOut();

        // Удаляем состояние авторизации
        requireActivity().getSharedPreferences("AppPrefs", getContext().MODE_PRIVATE)
                .edit()
                .putBoolean("isLoggedIn", false)
                .apply();

        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void reauthenticateAndDelete() {
        new AlertDialog.Builder(getActivity())
                .setTitle("Удаление аккаунта")
                .setMessage("Вы уверены, что хотите удалить аккаунт? Это действие нельзя отменить.")
                .setPositiveButton("Удалить", (dialog, which) -> deleteUserAccount())
                .setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void deleteUserAccount() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();

            db.collection("Users").document(userId).delete()
                    .addOnSuccessListener(aVoid -> {
                        user.delete()
                                .addOnSuccessListener(aVoid1 -> {
                                    Toast.makeText(getActivity(), "Аккаунт удален", Toast.LENGTH_SHORT).show();
                                    auth.signOut();
                                    requireActivity().getSharedPreferences("AppPrefs", getContext().MODE_PRIVATE)
                                            .edit()
                                            .putBoolean("isLoggedIn", false)
                                            .apply();

                                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                })
                                .addOnFailureListener(e -> Toast.makeText(getActivity(), "Ошибка удаления аккаунта", Toast.LENGTH_SHORT).show());
                    })
                    .addOnFailureListener(e -> Toast.makeText(getActivity(), "Ошибка удаления данных", Toast.LENGTH_SHORT).show());
        }
    }
}
