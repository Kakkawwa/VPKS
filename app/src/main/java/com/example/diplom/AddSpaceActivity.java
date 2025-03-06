package com.example.diplom;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.app.TimePickerDialog;
import android.text.TextUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import android.net.Uri;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.example.diplom.databinding.ActivityAddSpaceBinding;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.yandex.mapkit.MapKitFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddSpaceActivity extends AppCompatActivity {
    private Handler inactivityHandler = new Handler();
    private Runnable inactivityRunnable = () -> {
        isUserInteracted = false; // Отключаем API-запросы
    };
    private List<Zone> zonesList = new ArrayList<>();
    private String lastQuery = "";
    private final Handler handler = new Handler();
    private Runnable inputFinishChecker;
    private ActivityAddSpaceBinding binding;
    private FirebaseFirestore db;
    private List<Uri> selectedImages = new ArrayList<>();
    private ImageAdapter imageAdapter;
    private boolean isUserInteracted = false;
    private ActivityResultLauncher<Intent> addressLauncher;

    // Список удобств, выбранных через диалог
    private List<String> selectedAmenitiesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapKitFactory.initialize(this); // Инициализация MapKit
        binding = ActivityAddSpaceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.AdressEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                isUserInteracted = true;
                resetInactivityTimer();
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        binding.AdressEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isUserInteracted && s.length() > 3 && !s.toString().equals(lastQuery)) {
                    handler.removeCallbacks(inputFinishChecker);
                    inputFinishChecker = () -> { lastQuery = s.toString(); };
                    handler.postDelayed(inputFinishChecker, 500);
                    resetInactivityTimer();
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.openingTimeEditText.setOnClickListener(v -> {
            TimePickerDialog timePicker = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                binding.openingTimeEditText.setText(String.format("%02d:%02d", hourOfDay, minute));
            }, 9, 0, true);
            timePicker.show();
        });

        binding.closingTimeEditText.setOnClickListener(v -> {
            TimePickerDialog timePicker = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                binding.closingTimeEditText.setText(String.format("%02d:%02d", hourOfDay, minute));
            }, 18, 0, true);
            timePicker.show();
        });

        // Если выбрано "круглосуточно", отключаем поля времени:
        binding.checkbox24Hours.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                binding.openingTimeEditText.setText("00:00");
                binding.closingTimeEditText.setText("23:59");
                binding.openingTimeEditText.setEnabled(false);
                binding.closingTimeEditText.setEnabled(false);
            } else {
                binding.openingTimeEditText.setEnabled(true);
                binding.closingTimeEditText.setEnabled(true);
            }
        });

        // Обработчик добавления новой зоны
        binding.addZoneButton.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_zone, null);

            // Находим элементы в диалоге
            EditText zoneNameEditText = dialogView.findViewById(R.id.zoneNameEditText);
            EditText zonePlacesEditText = dialogView.findViewById(R.id.zonePlacesEditText);
            RadioGroup priceTypeRadioGroup = dialogView.findViewById(R.id.priceTypeRadioGroup);
            EditText priceEditText = dialogView.findViewById(R.id.priceEditText);

            builder.setView(dialogView)
                    .setTitle("Добавить зону")
                    .setPositiveButton("Добавить", (dialog, which) -> {
                        String zoneName = zoneNameEditText.getText().toString().trim();
                        String zonePlaces = zonePlacesEditText.getText().toString().trim();
                        String price = priceEditText.getText().toString().trim();

                        // Определяем выбранный тип цены
                        String priceType = "";
                        int selectedId = priceTypeRadioGroup.getCheckedRadioButtonId();
                        if (selectedId == R.id.priceHourRadio) {
                            priceType = "час";
                        } else if (selectedId == R.id.priceDayRadio) {
                            priceType = "день";
                        } else if (selectedId == R.id.priceMonthRadio) {
                            priceType = "месяц";
                        }

                        // Проверяем, что все поля заполнены
                        if (!zoneName.isEmpty() && !zonePlaces.isEmpty() && !price.isEmpty() && !priceType.isEmpty()) {
                            addZoneItem(zoneName, zonePlaces, price, priceType);
                        } else {
                            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
        });

        // Выбор удобств через диалог с множественным выбором
        List<String> availableAmenities = Arrays.asList("Скоростной вайфай", "Принтер", "Кофемашина", "Шкафчики", "Парковка", "Микроволновка", "Кухня");
        binding.selectAmenitiesButton.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Выберите удобства");
            boolean[] selectedAmenitiesArray = new boolean[availableAmenities.size()];
            // Передаем текущее состояние, если уже были выбраны удобства
            for (int i = 0; i < availableAmenities.size(); i++) {
                selectedAmenitiesArray[i] = selectedAmenitiesList.contains(availableAmenities.get(i));
            }
            builder.setMultiChoiceItems(availableAmenities.toArray(new String[0]), selectedAmenitiesArray, (dialog, which, isChecked) -> {
                selectedAmenitiesArray[which] = isChecked;
            });
            builder.setPositiveButton("ОК", (dialog, which) -> {
                selectedAmenitiesList.clear();
                for (int i = 0; i < availableAmenities.size(); i++) {
                    if (selectedAmenitiesArray[i]) {
                        selectedAmenitiesList.add(availableAmenities.get(i));
                    }
                }
                // Исправлено: убраны лишние экранирования
                binding.selectedAmenitiesTextView.setText(TextUtils.join(", ", selectedAmenitiesList));
            });
            builder.setNegativeButton("Отмена", null);
            builder.show();
        });

        db = FirebaseFirestore.getInstance();
        imageAdapter = new ImageAdapter(selectedImages);

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.recyclerView.setAdapter(imageAdapter);

        // Callback для выбора адреса
        addressLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String address = result.getData().getStringExtra("address");
                        updateAddressUI(address);
                    }
                });

        setupAddressButtons();

        binding.selectImagesButton.setOnClickListener(v -> openGallery());
        binding.AddCoworkingButton.setOnClickListener(v -> saveData());
    }

    // Добавление зоны с поддержкой редактирования и удаления
    private void addZoneItem(String zoneName, String zonePlaces, String price, String priceType) {
        View zoneItem = getLayoutInflater().inflate(R.layout.item_zone, binding.zonesContainer, false);

        TextView zoneNameTV = zoneItem.findViewById(R.id.zoneNameTV);
        TextView zonePlacesTV = zoneItem.findViewById(R.id.zonePlacesTV);
        TextView priceTV = zoneItem.findViewById(R.id.priceTV);
        Button editZoneButton = zoneItem.findViewById(R.id.editZoneButton);
        Button deleteZoneButton = zoneItem.findViewById(R.id.deleteZoneButton);

        zoneNameTV.setText(zoneName);
        zonePlacesTV.setText("Мест: " + zonePlaces);
        priceTV.setText("Цена: " + price + " ₽/" + priceType);

        binding.zonesContainer.addView(zoneItem);

        // Создаем объект зоны и добавляем его в список
        final Zone newZone = new Zone(zoneName, zonePlaces, price, priceType);
        zonesList.add(newZone);

        // Редактирование зоны
        editZoneButton.setOnClickListener(v -> showEditZoneDialog(newZone, zoneItem));

        // Удаление зоны
        deleteZoneButton.setOnClickListener(v -> {
            binding.zonesContainer.removeView(zoneItem);
            zonesList.remove(newZone);
        });
    }

    private void showEditZoneDialog(final Zone zone, final View zoneItem) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_zone, null);

        // Элементы диалога
        EditText zoneNameEditText = dialogView.findViewById(R.id.zoneNameEditText);
        EditText zonePlacesEditText = dialogView.findViewById(R.id.zonePlacesEditText);
        RadioGroup priceTypeRadioGroup = dialogView.findViewById(R.id.priceTypeRadioGroup);
        EditText priceEditText = dialogView.findViewById(R.id.priceEditText);

        // Предзаполнение значений
        zoneNameEditText.setText(zone.getName());
        zonePlacesEditText.setText(zone.getPlaces());
        priceEditText.setText(zone.getPrice());
        if (zone.getPriceType().equals("час")) {
            priceTypeRadioGroup.check(R.id.priceHourRadio);
        } else if (zone.getPriceType().equals("день")) {
            priceTypeRadioGroup.check(R.id.priceDayRadio);
        } else if (zone.getPriceType().equals("месяц")) {
            priceTypeRadioGroup.check(R.id.priceMonthRadio);
        }

        builder.setView(dialogView)
                .setTitle("Редактировать зону")
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    String newZoneName = zoneNameEditText.getText().toString().trim();
                    String newZonePlaces = zonePlacesEditText.getText().toString().trim();
                    String newPrice = priceEditText.getText().toString().trim();
                    String newPriceType = "";
                    int selectedId = priceTypeRadioGroup.getCheckedRadioButtonId();
                    if (selectedId == R.id.priceHourRadio) {
                        newPriceType = "час";
                    } else if (selectedId == R.id.priceDayRadio) {
                        newPriceType = "день";
                    } else if (selectedId == R.id.priceMonthRadio) {
                        newPriceType = "месяц";
                    }
                    if (!newZoneName.isEmpty() && !newZonePlaces.isEmpty() && !newPrice.isEmpty() && !newPriceType.isEmpty()) {
                        // Обновляем объект зоны
                        zone.setName(newZoneName);
                        zone.setPlaces(newZonePlaces);
                        zone.setPrice(newPrice);
                        zone.setPriceType(newPriceType);
                        // Обновляем UI элемента зоны
                        TextView zoneNameTV = zoneItem.findViewById(R.id.zoneNameTV);
                        TextView zonePlacesTV = zoneItem.findViewById(R.id.zonePlacesTV);
                        TextView priceTV = zoneItem.findViewById(R.id.priceTV);
                        zoneNameTV.setText(newZoneName);
                        zonePlacesTV.setText("Мест: " + newZonePlaces);
                        priceTV.setText("Цена: " + newPrice + " ₽/" + newPriceType);
                    } else {
                        Toast.makeText(AddSpaceActivity.this, "Заполните все поля", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void setupAddressButtons() {
        binding.addAddressButton.setOnClickListener(v -> launchAddressSelection(null));
        binding.changeAddressButton.setOnClickListener(v ->
                launchAddressSelection(binding.AdressEditText.getText().toString()));
    }

    private void launchAddressSelection(String currentAddress) {
        Intent intent = new Intent(this, AddressSelectionActivity.class);
        if (currentAddress != null)
            intent.putExtra("currentAddress", currentAddress);
        addressLauncher.launch(intent);
    }

    private void updateAddressUI(String address) {
        if (address != null && !address.isEmpty()) {
            binding.AdressEditText.setText(address);
            binding.addressInputLayout.setVisibility(View.VISIBLE);
            binding.addAddressButton.setVisibility(View.GONE);
            binding.changeAddressButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isUserInteracted = true; // Включаем API-запросы при возврате
    }

    @Override
    protected void onPause() {
        super.onPause();
        isUserInteracted = false; // Отключаем API-запросы при выходе
    }

    // Метод для сброса таймера неактивности
    private void resetInactivityTimer() {
        inactivityHandler.removeCallbacks(inactivityRunnable);
        inactivityHandler.postDelayed(inactivityRunnable, 120000); // 2 минуты бездействия
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT); // Изменено с ACTION_PICK
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        galleryLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    if (result.getData().getClipData() != null) {
                        int count = result.getData().getClipData().getItemCount();
                        Log.d("ImageSelection", "Выбрано изображений: " + count);
                        for (int i = 0; i < count; i++) {
                            Uri imageUri = result.getData().getClipData().getItemAt(i).getUri();
                            if (imageUri != null) {
                                selectedImages.add(imageUri);
                                Log.d("ImageSelection", "Добавлен URI: " + imageUri.toString());
                            }
                        }
                    } else {
                        Uri imageUri = result.getData().getData();
                        Log.d("ImageSelection", "Добавлен URI: " + imageUri);
                        if (imageUri != null) {
                            selectedImages.add(imageUri);
                        }
                    }
                    // Показываем RecyclerView, когда изображения выбраны
                    if (!selectedImages.isEmpty()) {
                        binding.recyclerView.setVisibility(View.VISIBLE);
                    }
                    imageAdapter.notifyDataSetChanged();
                }
            }
    );

    private void saveData() {
        // Существующие данные:
        String name = binding.NameEditText.getText().toString().trim();
        String address = binding.AdressEditText.getText().toString().trim();
        String description = binding.DescriptionEditText.getText().toString().trim();

        // Новые данные:
        // 1. Зоны – сохраняем список зон как JSON-массив или список Map
        List<Map<String, Object>> zones = new ArrayList<>();
        for (Zone zone : zonesList) {
            Map<String, Object> zoneMap = new HashMap<>();
            zoneMap.put("name", zone.getName());
            zoneMap.put("places", zone.getPlaces());
            zoneMap.put("price", zone.getPrice());
            zoneMap.put("priceType", zone.getPriceType());
            zones.add(zoneMap);
        }

        // 2. Удобства – используем выбранные через диалог
        List<String> amenities = new ArrayList<>(selectedAmenitiesList);

        // 3. Часы работы:
        String openingTime = binding.openingTimeEditText.getText().toString();
        String closingTime = binding.closingTimeEditText.getText().toString();
        boolean is24Hours = binding.checkbox24Hours.isChecked();

        // Валидация полей
        if (name.isEmpty() || address.isEmpty() || description.isEmpty() || selectedImages.isEmpty() || zonesList.isEmpty()) {
            Toast.makeText(this, "Заполните все обязательные поля и выберите фото", Toast.LENGTH_SHORT).show();
            return;
        }

        // Продолжаем с загрузкой изображений и сохранением в Firestore, добавляя новые данные:
        uploadImagesToCloudinary(name, address, description, zones, amenities, openingTime, closingTime, is24Hours);
    }

    // Метод для загрузки изображений и сохранения данных в Firestore
    private void uploadImagesToCloudinary(String name, String address, String description,
                                          List<Map<String, Object>> zones, List<String> amenities,
                                          String openingTime, String closingTime, boolean is24Hours) {
        List<String> imageUrls = new ArrayList<>();
        List<String> imagePublicIds = new ArrayList<>();

        for (Uri imageUri : selectedImages) {
            MediaManager.get().upload(imageUri).callback(new com.cloudinary.android.callback.UploadCallback() {
                @Override
                public void onStart(String requestId) {
                    Log.d("Cloudinary", "Загрузка началась: " + requestId);
                }
                @Override
                public void onProgress(String requestId, long bytes, long totalBytes) {
                    Log.d("Cloudinary", "Загрузка " + requestId + ": " + (bytes * 100 / totalBytes) + "% завершено");
                }
                @Override
                public void onSuccess(String requestId, Map resultData) {
                    String imageUrl = resultData.get("secure_url").toString();
                    String publicId = resultData.get("public_id").toString();
                    imageUrls.add(imageUrl);
                    imagePublicIds.add(publicId);

                    if (imageUrls.size() == selectedImages.size()) {
                        saveDataToFirestore(name, address, description, imageUrls, imagePublicIds, zones, amenities, openingTime, closingTime, is24Hours);
                    }
                }
                @Override
                public void onError(String requestId, ErrorInfo error) {
                    Log.e("Cloudinary", "Ошибка загрузки: " + error.getDescription());
                    Toast.makeText(AddSpaceActivity.this, "Ошибка загрузки фото", Toast.LENGTH_SHORT).show();
                }
                @Override
                public void onReschedule(String requestId, ErrorInfo error) {
                    Log.w("Cloudinary", "Загрузка отложена: " + requestId);
                }
            }).dispatch();
        }
    }

    private void saveDataToFirestore(String name, String address, String description,
                                     List<String> imageUrls, List<String> imagePublicIds,
                                     List<Map<String, Object>> zones, List<String> amenities,
                                     String openingTime, String closingTime, boolean is24Hours) {
        HashMap<String, Object> coworkingData = new HashMap<>();
        coworkingData.put("name", name);
        coworkingData.put("address", address);
        coworkingData.put("description", description);
        coworkingData.put("images", imageUrls);
        coworkingData.put("imagesPublicIds", imagePublicIds);

        // Новые данные:
        coworkingData.put("zones", zones);
        coworkingData.put("amenities", amenities);
        coworkingData.put("openingTime", openingTime);
        coworkingData.put("closingTime", closingTime);
        coworkingData.put("is24Hours", is24Hours);

        // Добавляем идентификатор создателя
        String creatorId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        coworkingData.put("creatorId", creatorId);

        db.collection("coworking_spaces").add(coworkingData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Данные добавлены", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
