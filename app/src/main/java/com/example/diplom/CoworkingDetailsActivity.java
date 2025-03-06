package com.example.diplom;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.IconStyle;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.runtime.image.ImageProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CoworkingDetailsActivity extends AppCompatActivity {

    // Основные элементы интерфейса
    private RecyclerView recyclerView; // Для изображений (карусель)
    private ImageSliderAdapter imageSliderAdapter;
    private TextView nameTextView, addressTextView, descriptionTextView, addedByTextView;
    // Поле для отображения адреса уже имеется, теперь добавим карту
    private MapView mapView;
    private CircleImageView creatorAvatarImageView;
    private CustomScrollView scrollView;
    private PlacemarkMapObject placemark;
    private OkHttpClient client = new OkHttpClient();
    private final String GEOCODER_API_KEY = "ad668e20-08d8-48ac-bfb1-aa6d6662b770";

    // Элементы для зон
    private RecyclerView rvZones;
    private TextView tvZonesTitle;
    // Элементы для удобств
    private TextView tvAmenitiesTitle, tvAmenities;
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
        // Инициализируем MapKit перед setContentView
        MapKitFactory.initialize(this);
        setContentView(R.layout.activity_coworking_details);

        // Инициализация элементов интерфейса
        initViews();
        scrollView.addInterceptableView(mapView);
        scrollView.addInterceptableView(rvZones);
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
        scrollView = findViewById(R.id.scrollView);
        mapView = findViewById(R.id.mapView);
        rvZones = findViewById(R.id.rvZones);

        // Отключаем вложенную прокрутку
        rvZones.setNestedScrollingEnabled(false);
        recyclerView = findViewById(R.id.recyclerView);
        nameTextView = findViewById(R.id.nameTextView);
        addressTextView = findViewById(R.id.addressTextView);
        descriptionTextView = findViewById(R.id.descriptionTextView);
        addedByTextView = findViewById(R.id.addedByTextView);
        creatorAvatarImageView = findViewById(R.id.creatorAvatarImageView);

        // Инициализация MapView (карты)
        mapView = findViewById(R.id.mapView);

        rvZones = findViewById(R.id.rvZones);
        rvZones.setLayoutManager(new LinearLayoutManager(this));

        tvAmenitiesTitle = findViewById(R.id.tvAmenitiesTitle);
        tvAmenities = findViewById(R.id.tvAmenities);

        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        tvZonesTitle = findViewById(R.id.tvZonesTitle);
        rvZones.setLayoutManager(new LinearLayoutManager(this));
        rvZones.setAdapter(zoneAdapter);

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
                        String name = documentSnapshot.getString("name");
                        String address = documentSnapshot.getString("address");
                        nameTextView.setText(name);
                        addressTextView.setText(address);
                        descriptionTextView.setText(documentSnapshot.getString("description"));

                        // Инициализируем адаптер для зон
                        zoneAdapter = new ZoneAdapter(zones, coworkingId, name);
                        rvZones.setAdapter(zoneAdapter);

                        // Загружаем зоны
                        List<Map<String, Object>> zonesList = (List<Map<String, Object>>) documentSnapshot.get("zones");
                        if (zonesList != null && !zonesList.isEmpty()) {
                            zones.clear();
                            zones.addAll(zonesList);
                            zoneAdapter.notifyDataSetChanged();
                            tvZonesTitle.setVisibility(View.VISIBLE);
                        } else {
                            tvZonesTitle.setVisibility(View.GONE);
                        }

                        // Загружаем изображения
                        List<String> images = (List<String>) documentSnapshot.get("images");
                        if (images != null && !images.isEmpty()) {
                            imageSliderAdapter = new ImageSliderAdapter(this, images);
                            recyclerView.setAdapter(imageSliderAdapter);
                        } else {
                            recyclerView.setVisibility(View.GONE);
                        }

                        // Загружаем информацию о создателе
                        String creatorId = documentSnapshot.getString("creatorId");
                        if (creatorId != null) {
                            loadCreatorInfo(creatorId);
                        }

                        loadEventsForCoworking(coworkingId);
                        List<String> amenities = (List<String>) documentSnapshot.get("amenities");
                        if (amenities != null && !amenities.isEmpty()) {
                            displayAmenities(amenities);
                        }

                        // Отобразим карту с меткой под адресом
                        if (address != null && !address.isEmpty()) {
                            getCoordinatesFromAddress(address);
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

    private void displayAmenities(List<String> amenities) {
        StringBuilder amenitiesString = new StringBuilder();
        for (String amenity : amenities) {
            amenitiesString.append("• ").append(amenity).append("\n");
        }
        tvAmenities.setText(amenitiesString.toString());
        tvAmenitiesTitle.setVisibility(View.VISIBLE);
        tvAmenities.setVisibility(View.VISIBLE);
    }

    // Геокодирование адреса для получения координат
    private void getCoordinatesFromAddress(String address) {
        String url = "https://geocode-maps.yandex.ru/1.x/?format=json&apikey="
                + GEOCODER_API_KEY + "&geocode=" + address.replace(" ", "+");
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(CoworkingDetailsActivity.this, "Ошибка геокодирования", Toast.LENGTH_SHORT).show());
            }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.isSuccessful()){
                    try {
                        String responseData = Objects.requireNonNull(response.body()).string();
                        JSONObject json = new JSONObject(responseData);
                        JSONArray featureMember = json.getJSONObject("response")
                                .getJSONObject("GeoObjectCollection")
                                .getJSONArray("featureMember");
                        if (featureMember.length() > 0) {
                            JSONObject pointJson = featureMember.getJSONObject(0)
                                    .getJSONObject("GeoObject")
                                    .getJSONObject("Point");
                            String[] coords = pointJson.getString("pos").split(" ");
                            double lon = Double.parseDouble(coords[0]);
                            double lat = Double.parseDouble(coords[1]);
                            Point point = new Point(lat, lon);
                            runOnUiThread(() -> {
                                // Перемещаем камеру, но не обновляем метку, если она уже создана
                                mapView.getMap().move(new CameraPosition(point, 16.0f, 0.0f, 0.0f));
                                if (placemark == null) {
                                    placemark = mapView.getMap().getMapObjects().addPlacemark(point);
                                    placemark.setIcon(ImageProvider.fromResource(CoworkingDetailsActivity.this, R.drawable.ic_pin),
                                            new IconStyle().setScale(0.05f));
                                }
                            });
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mapView != null) {
            mapView.onStart();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mapView != null) {
            mapView.onStop();
        }
    }
}
