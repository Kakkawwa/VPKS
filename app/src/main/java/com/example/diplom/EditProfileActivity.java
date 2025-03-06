package com.example.diplom;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.util.Map;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.cloudinary.Cloudinary;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.cloudinary.api.ApiResponse;
import com.cloudinary.utils.ObjectUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditProfileActivity extends AppCompatActivity {
    private EditText etFirstName, etLastName;
    private Button btnSave;
    private CircleImageView avatarImageView;
    private Uri avatarUri;
    private String currentAvatarPublicId;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private static final int PICK_AVATAR_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        btnSave = findViewById(R.id.btnSave);
        avatarImageView = findViewById(R.id.avatarImageView);

        loadUserData();

        // При нажатии на аватарку открываем выбор изображения
        avatarImageView.setOnClickListener(v -> openImageChooser());

        btnSave.setOnClickListener(v -> saveChanges());
    }

    private void loadUserData() {
        String userId = auth.getCurrentUser().getUid();
        db.collection("Users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentAvatarPublicId = documentSnapshot.getString("avatarPublicId");
                        etFirstName.setText(documentSnapshot.getString("name"));
                        etLastName.setText(documentSnapshot.getString("surname"));
                        String avatarUrl = documentSnapshot.getString("avatarUrl");
                        if (avatarUrl != null && !avatarUrl.isEmpty()) {
                            Glide.with(this).load(avatarUrl).into(avatarImageView);
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(EditProfileActivity.this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show());
    }

    private void openImageChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Выберите аватарку"), PICK_AVATAR_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_AVATAR_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            avatarUri = data.getData();
            Glide.with(this).load(avatarUri).into(avatarImageView);
        }
    }

    private void saveChanges() {
        String newName = etFirstName.getText().toString().trim();
        String newSurname = etLastName.getText().toString().trim();

        if (newName.isEmpty() || newSurname.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        if (avatarUri != null) {
            if (currentAvatarPublicId != null && !currentAvatarPublicId.isEmpty()) {
                deleteImageFromCloudinary(
                        currentAvatarPublicId,
                        () -> uploadAvatarToCloudinary(avatarUri),
                        () -> uploadAvatarToCloudinary(avatarUri) // Продолжаем даже при ошибке удаления
                );
            } else {
                uploadAvatarToCloudinary(avatarUri);
            }
        } else {
            updateProfileData(null, null);
        }
    }

    private void deleteImageFromCloudinary(String publicId, Runnable onSuccess, Runnable onError) {
        new Thread(() -> {
            try {
                Cloudinary cloudinary = new Cloudinary(MyApp.CLOUDINARY_CONFIG);
                Map response = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                Log.d("Cloudinary", "Delete response: " + response.toString());
                runOnUiThread(onSuccess);
            } catch (Exception e) {
                // обработка ошибок
            }
        }).start();
    }

    private void uploadAvatarToCloudinary(Uri avatarUri) {
        MediaManager.get().upload(avatarUri)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                        Log.d("Cloudinary", "Началась загрузка аватарки: " + requestId);
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                        Log.d("Cloudinary", "Загрузка аватарки " + requestId + ": " + (bytes * 100 / totalBytes) + "%");
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String avatarUrl = resultData.get("secure_url").toString();
                        String publicId = resultData.get("public_id").toString();
                        updateProfileData(avatarUrl, publicId);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Log.e("Cloudinary", "Ошибка загрузки аватарки: " + error.getDescription());
                        Toast.makeText(EditProfileActivity.this, "Ошибка загрузки аватарки", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                        Log.w("Cloudinary", "Загрузка аватарки отложена: " + requestId);
                    }
                }).dispatch();
    }

    private void updateProfileData(String avatarUrl, String avatarPublicId) {
        String newName = etFirstName.getText().toString().trim();
        String newSurname = etLastName.getText().toString().trim();

        String userId = auth.getCurrentUser().getUid();
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", newName);
        updates.put("surname", newSurname);
        if (avatarUrl != null) {
            updates.put("avatarUrl", avatarUrl);
            updates.put("avatarPublicId", avatarPublicId);
        }

        db.collection("Users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(EditProfileActivity.this, "Данные обновлены", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(EditProfileActivity.this, "Ошибка сохранения", Toast.LENGTH_SHORT).show());
    }
}
