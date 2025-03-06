package com.example.diplom;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.cloudinary.Cloudinary;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.cloudinary.utils.ObjectUtils;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditCoworkingActivity extends AppCompatActivity {

    private EditText etName, etAddress, etPlaces, etDescription, etPrice;
    private RecyclerView recyclerViewImages;
    private Button btnAddImages, btnSave;
    private List<String> imageUrls = new ArrayList<>();
    private List<String> imagePublicIds = new ArrayList<>();
    private List<Uri> newImageUris = new ArrayList<>();
    private List<String> deletedPublicIds = new ArrayList<>();
    private FirebaseFirestore db;
    private String documentId;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private EditableImageAdapter imageAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_coworking);

        initializeViews();
        setupRecyclerView();
        setupGalleryLauncher();
        setupButtons();
        loadCoworkingData();
    }

    private void initializeViews() {
        etName = findViewById(R.id.etName);
        etAddress = findViewById(R.id.etAddress);
        etPlaces = findViewById(R.id.etPlaces);
        etDescription = findViewById(R.id.etDescription);
        etPrice = findViewById(R.id.etPrice);
        recyclerViewImages = findViewById(R.id.recyclerViewImages);
        btnAddImages = findViewById(R.id.btnAddImages);
        btnSave = findViewById(R.id.btnSave);

        db = FirebaseFirestore.getInstance();
        documentId = getIntent().getStringExtra("documentId");
        if (documentId == null || documentId.isEmpty()) {
            Toast.makeText(this, "Ошибка: не передан идентификатор коворкинга", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupRecyclerView() {
        recyclerViewImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        imageAdapter = new EditableImageAdapter(
                this,
                imageUrls,
                imagePublicIds,
                newImageUris,
                position -> {
                    // Проверяем, что позиция существует в imagePublicIds
                    if (position < imagePublicIds.size()) {
                        deletedPublicIds.add(imagePublicIds.get(position));
                        imagePublicIds.remove(position);
                    }
                    // Аналогично для imageUrls
                    if (position < imageUrls.size()) {
                        imageUrls.remove(position);
                    }
                    imageAdapter.notifyDataSetChanged();
                }
        );
        recyclerViewImages.setAdapter(imageAdapter);
    }



    private void setupGalleryLauncher() {
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        handleImageSelection(result.getData());
                    }
                }
        );
    }

    private void handleImageSelection(Intent data) {
        if (data.getClipData() != null) {
            int count = data.getClipData().getItemCount();
            for (int i = 0; i < count; i++) {
                Uri uri = data.getClipData().getItemAt(i).getUri();
                newImageUris.add(uri);
            }
        } else if (data.getData() != null) {
            newImageUris.add(data.getData());
        }
        imageAdapter.notifyDataSetChanged();
        Toast.makeText(this, "Новые фото добавлены", Toast.LENGTH_SHORT).show();
    }

    private void setupButtons() {
        btnAddImages.setOnClickListener(v -> openGallery());
        btnSave.setOnClickListener(v -> validateAndSave());
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        galleryLauncher.launch(intent);
    }

    private void validateAndSave() {
        String name = etName.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String places = etPlaces.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String price = etPrice.getText().toString().trim();

        if (name.isEmpty() || address.isEmpty() || places.isEmpty() || description.isEmpty() || price.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        deleteRemovedImages(() -> {
            if (!newImageUris.isEmpty()) {
                uploadNewImages(name, address, places, description, price);
            } else {
                updateFirestoreData(name, address, places, description, price);
            }
        });
    }

    private void deleteRemovedImages(Runnable onComplete) {
        if (deletedPublicIds.isEmpty()) {
            onComplete.run();
            return;
        }

        // Счетчик для отслеживания завершенных операций
        final int[] completed = {0};
        final int total = deletedPublicIds.size();
        final Cloudinary cloudinary = new Cloudinary(MyApp.CLOUDINARY_CONFIG);

        for (String publicId : deletedPublicIds) {
            new Thread(() -> {
                try {
                    Map response = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                    Log.d("Cloudinary", "Deleted: " + publicId + " response: " + response);
                    synchronized (completed) {
                        completed[0]++;
                        if (completed[0] == total) {
                            runOnUiThread(onComplete);
                        }
                    }
                } catch (Exception e) {
                    Log.e("Cloudinary", "Error deleting: " + publicId, e);
                    synchronized (completed) {
                        completed[0]++;
                        if (completed[0] == total) {
                            runOnUiThread(onComplete);
                        }
                    }
                }
            }).start();
        }
    }

    private void uploadNewImages(String name, String address, String places, String description, String price) {
        List<String> newUrls = new ArrayList<>();
        List<String> newPublicIds = new ArrayList<>();
        int total = newImageUris.size();
        int[] completed = {0};

        for (Uri uri : newImageUris) {
            MediaManager.get().upload(uri)
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {
                            Log.d("Cloudinary", "Началась загрузка: " + requestId);
                        }

                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {}

                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            newUrls.add(resultData.get("secure_url").toString());
                            newPublicIds.add(resultData.get("public_id").toString());
                            if (++completed[0] == total) {
                                updateFirestoreData(
                                        name,
                                        address,
                                        places,
                                        description,
                                        price,
                                        newUrls,
                                        newPublicIds
                                );
                            }
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            Toast.makeText(EditCoworkingActivity.this, "Ошибка загрузки фото", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {}
                    })
                    .dispatch();
        }
    }

    private void updateFirestoreData(String name, String address, String places, String description, String price) {
        updateFirestoreData(name, address, places, description, price, new ArrayList<>(), new ArrayList<>());
    }

    private void updateFirestoreData(String name, String address, String places, String description, String price,
                                     List<String> newUrls, List<String> newPublicIds) {
        List<String> finalUrls = new ArrayList<>(imageUrls);
        List<String> finalPublicIds = new ArrayList<>(imagePublicIds);

        finalUrls.addAll(newUrls);
        finalPublicIds.addAll(newPublicIds);

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("address", address);
        updates.put("places", places);
        updates.put("description", description);
        updates.put("price", price);
        updates.put("images", finalUrls);
        updates.put("imagesPublicIds", finalPublicIds);

        db.collection("coworking_spaces").document(documentId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Данные обновлены", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Ошибка обновления: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void loadCoworkingData() {
        db.collection("coworking_spaces").document(documentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        etName.setText(documentSnapshot.getString("name"));
                        etAddress.setText(documentSnapshot.getString("address"));
                        etPlaces.setText(documentSnapshot.getString("places"));
                        etDescription.setText(documentSnapshot.getString("description"));
                        etPrice.setText(documentSnapshot.getString("price"));

                        imageUrls = (List<String>) documentSnapshot.get("images");
                        imagePublicIds = (List<String>) documentSnapshot.get("imagesPublicIds");

                        if (imageUrls == null) imageUrls = new ArrayList<>();
                        if (imagePublicIds == null) imagePublicIds = new ArrayList<>();

                        imageAdapter.updateData(imageUrls, imagePublicIds);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}