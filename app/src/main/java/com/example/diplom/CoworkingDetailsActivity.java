package com.example.diplom;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class CoworkingDetailsActivity extends AppCompatActivity {

    // Основные элементы интерфейса
    private RecyclerView recyclerView; // Для изображений (карусель)
    private ImageSliderAdapter imageSliderAdapter;
    private TextView nameTextView, addressTextView, descriptionTextView, addedByTextView;
    private CircleImageView creatorAvatarImageView;

    // Элементы для зон
    private RecyclerView rvZones;
    private TextView tvZonesTitle;
    // Элементы для удобств
    private TextView tvAmenitiesTitle, tvAmenities; // Добавьте эти переменные
    private ZoneAdapter zoneAdapter;
    private List<Map<String, Object>> zones = new ArrayList<>();

    // Элементы для мероприятий
    private RecyclerView rvEvents;
    private TextView tvEventsTitle;
    private EventAdapter eventAdapter;
    private List<EventModel> eventList;

    // Переменная для хранения идентификатора коворкинга
    private String coworkingId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coworking_details);

        // Инициализация элементов интерфейса
        initViews();

        // Получаем ID коворкинга из Intent
        coworkingId = getIntent().getStringExtra("coworkingId");
        if (coworkingId == null || coworkingId.isEmpty()) {
            Toast.makeText(this, "Ошибка: неверный идентификатор коворкинга", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Загружаем данные о коворкинге
        loadCoworkingDetails();

    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        nameTextView = findViewById(R.id.nameTextView);
        addressTextView = findViewById(R.id.addressTextView);
        descriptionTextView = findViewById(R.id.descriptionTextView);
        addedByTextView = findViewById(R.id.addedByTextView);
        creatorAvatarImageView = findViewById(R.id.creatorAvatarImageView);

        rvZones = findViewById(R.id.rvZones);
        rvZones.setLayoutManager(new LinearLayoutManager(this));

        // Инициализация элементов для удобств
        tvAmenitiesTitle = findViewById(R.id.tvAmenitiesTitle); // Добавьте эту строку
        tvAmenities = findViewById(R.id.tvAmenities); // Добавьте эту строку

        // Инициализация RecyclerView для изображений
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Инициализация RecyclerView для зон
        tvZonesTitle = findViewById(R.id.tvZonesTitle);
        rvZones = findViewById(R.id.rvZones);
        rvZones.setLayoutManager(new LinearLayoutManager(this));
        rvZones.setAdapter(zoneAdapter);

        // Инициализация RecyclerView для мероприятий
        tvEventsTitle = findViewById(R.id.tvEventsTitle);
        rvEvents = findViewById(R.id.rvEvents);
        rvEvents.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        eventList = new ArrayList<>();
        eventAdapter = new EventAdapter(this, eventList, false);
        rvEvents.setAdapter(eventAdapter);
    }

    private void loadCoworkingDetails() {
        FirebaseFirestore.getInstance()
                .collection("coworking_spaces")
                .document(coworkingId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Основные данные
                        nameTextView.setText(documentSnapshot.getString("name"));
                        addressTextView.setText(documentSnapshot.getString("address"));
                        descriptionTextView.setText(documentSnapshot.getString("description"));
                        String coworkingName = documentSnapshot.getString("name");

                        // Инициализируйте адаптер здесь
                        zoneAdapter = new ZoneAdapter(
                                zones,
                                coworkingId,
                                coworkingName
                        );
                        rvZones.setAdapter(zoneAdapter);

                        // Загрузка зон
                        List<Map<String, Object>> zones = (List<Map<String, Object>>) documentSnapshot.get("zones");
                        if (zones != null && !zones.isEmpty()) {
                            this.zones.clear();
                            this.zones.addAll(zones);
                            zoneAdapter.notifyDataSetChanged();
                            tvZonesTitle.setVisibility(View.VISIBLE);
                        } else {
                            tvZonesTitle.setVisibility(View.GONE);
                        }

                        // Загрузка изображений
                        List<String> images = (List<String>) documentSnapshot.get("images");
                        if (images != null && !images.isEmpty()) {
                            imageSliderAdapter = new ImageSliderAdapter(this, images);
                            recyclerView.setAdapter(imageSliderAdapter);
                        } else {
                            recyclerView.setVisibility(View.GONE);
                        }

                        // Загрузка информации о создателе
                        String creatorId = documentSnapshot.getString("creatorId");
                        if (creatorId != null) {
                            loadCreatorInfo(creatorId);
                        }

                        // Загрузка мероприятий
                        loadEventsForCoworking(coworkingId);

                        // Загрузка удобств
                        List<String> amenities = (List<String>) documentSnapshot.get("amenities");
                        if (amenities != null && !amenities.isEmpty()) {
                            displayAmenities(amenities);
                        }

                    } else {
                        Toast.makeText(this, "Коворкинг не найден", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Ошибка загрузки данных: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void displayAmenities(List<String> amenities) {
        // Формируем строку с удобствами
        StringBuilder amenitiesString = new StringBuilder();
        for (String amenity : amenities) {
            amenitiesString.append("• ").append(amenity).append("\n");
        }

        // Отображаем удобства
        tvAmenities.setText(amenitiesString.toString());
        tvAmenitiesTitle.setVisibility(View.VISIBLE);
        tvAmenities.setVisibility(View.VISIBLE);
    }

    private void loadCreatorInfo(String creatorId) {
        FirebaseFirestore.getInstance()
                .collection("Users")
                .document(creatorId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String creatorName = documentSnapshot.getString("name");
                        String creatorSurname = documentSnapshot.getString("surname");
                        String creatorAvatarUrl = documentSnapshot.getString("avatarUrl");

                        addedByTextView.setText("Добавлено пользователем: " + creatorName + " " + creatorSurname);
                        if (creatorAvatarUrl != null && !creatorAvatarUrl.isEmpty()) {
                            Glide.with(CoworkingDetailsActivity.this)
                                    .load(creatorAvatarUrl)
                                    .into(creatorAvatarImageView);
                        } else {
                            creatorAvatarImageView.setImageResource(R.drawable.img);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    addedByTextView.setText("Ошибка загрузки информации о пользователе");
                    creatorAvatarImageView.setImageResource(R.drawable.img);
                });
    }

    private void loadEventsForCoworking(String coworkingId) {
        FirebaseFirestore.getInstance()
                .collection("events")
                .whereEqualTo("coworkingId", coworkingId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    eventList.clear();
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            EventModel event = doc.toObject(EventModel.class);
                            if (event != null) {
                                eventList.add(event);
                            }
                        }
                        tvEventsTitle.setVisibility(View.VISIBLE);
                        rvEvents.setVisibility(View.VISIBLE);
                        eventAdapter.notifyDataSetChanged();
                    } else {
                        tvEventsTitle.setVisibility(View.GONE);
                        rvEvents.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Ошибка загрузки мероприятий", Toast.LENGTH_SHORT).show();
                    tvEventsTitle.setVisibility(View.GONE);
                    rvEvents.setVisibility(View.GONE);
                });
    }
}