package com.example.diplom;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.diplom.databinding.ActivityAddSpaceBinding;
import com.example.diplom.databinding.ActivityAddressSelectionBinding;
import com.google.firebase.firestore.FirebaseFirestore;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.IconStyle;
import com.yandex.mapkit.map.InputListener;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.runtime.image.ImageProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AddressSelectionActivity extends AppCompatActivity {
    private ActivityAddressSelectionBinding binding;
    private MapView mapView;
    private PlacemarkMapObject placemark;
    private final String GEOCODER_API_KEY = "ad668e20-08d8-48ac-bfb1-aa6d6662b770";
    private final OkHttpClient client = new OkHttpClient();
    private Handler inactivityHandler = new Handler();
    private Runnable inactivityRunnable = () -> {
        isUserInteracted = false;
    };
    private String lastQuery = "";
    private final Handler handler = new Handler();
    private Runnable inputFinishChecker;
    private boolean isUserInteracted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapKitFactory.initialize(this);
        binding = ActivityAddressSelectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mapView = binding.mapView;

        binding.mapView.getMap().move(
                new CameraPosition(new Point(55.751244, 37.618423), 10.0f, 0.0f, 0.0f) // Москва по умолчанию
        );

        // Загрузка текущего адреса если есть
        String currentAddress = getIntent().getStringExtra("currentAddress");
        if (currentAddress != null) {
            binding.addressEditText.setText(currentAddress);
            getCoordinatesFromAddress(currentAddress);
        }

        binding.mapView.getMap().addInputListener(inputListener);

        binding.addressEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                isUserInteracted = true;
                resetInactivityTimer();
            }
        });

        binding.addressEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isUserInteracted && s.length() > 3 && !s.toString().equals(lastQuery)) {
                    handler.removeCallbacks(inputFinishChecker);
                    inputFinishChecker = () -> {
                        lastQuery = s.toString();
                        getCoordinatesFromAddress(s.toString());
                    };
                    handler.postDelayed(inputFinishChecker, 500);
                    resetInactivityTimer();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        setupMap();
        setupListeners();
    }

    private void setupMap() {
        mapView.getMap().move(new CameraPosition(new Point(55.751244, 37.618423), 10.0f, 0.0f, 0.0f));
        mapView.getMap().addInputListener(new InputListener() {
            @Override
            public void onMapTap(@NonNull com.yandex.mapkit.map.Map map, @NonNull Point point) {
                updatePlacemark(point);
                getAddressFromCoordinates(point);
            }

            @Override public void onMapLongTap(@NonNull com.yandex.mapkit.map.Map map, @NonNull Point point) {}
        });
    }

    private void setupListeners() {
        binding.addressEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 3) getCoordinatesFromAddress(s.toString());
            }
        });

        binding.confirmButton.setOnClickListener(v -> {
            String address = binding.addressEditText.getText().toString().trim();
            if (!address.isEmpty()) {
                Intent result = new Intent();
                result.putExtra("address", address);
                setResult(RESULT_OK, result);
                finish();
            } else {
                Toast.makeText(this, "Введите адрес", Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        isUserInteracted = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        isUserInteracted = false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mapView != null) {
            mapView.onStart();
        } else {
            throw new NullPointerException("MapView is null. Check initialization.");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mapView != null) {
            mapView.onStop();
        }
    }

    private void resetInactivityTimer() {
        inactivityHandler.removeCallbacks(inactivityRunnable);
        inactivityHandler.postDelayed(inactivityRunnable, 120000); // 2 минуты бездействия
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("Permission", "Разрешение на местоположение предоставлено");
            } else {
                boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION);
                if (!showRationale) {
                    Toast.makeText(this, "Вы отключили доступ к местоположению в настройках", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Разрешение на местоположение необходимо для работы карты", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private final InputListener inputListener = new InputListener() {
        @Override
        public void onMapTap(@NonNull com.yandex.mapkit.map.Map map, @NonNull Point point) {
            isUserInteracted = true;
            resetInactivityTimer();
                updatePlacemark(point);
                getAddressFromCoordinates(point);
        }

        @Override
        public void onMapLongTap(@NonNull com.yandex.mapkit.map.Map map, @NonNull Point point) {
        }
    };

    private void updatePlacemark(Point point) {
        if (placemark != null) {
            placemark.setGeometry(point);
        } else {
            placemark = binding.mapView.getMap().getMapObjects().addPlacemark(point);
            placemark.setIcon(ImageProvider.fromResource(this, R.drawable.ic_pin), new IconStyle().setScale(0.05f)); // Уменьшаем иконку
        }
    }

    private void getCoordinatesFromAddress(String address) {
        String url = "https://geocode-maps.yandex.ru/1.x/?format=json&apikey="
                + GEOCODER_API_KEY + "&geocode=" + address.replace(" ", "+");

        client.newCall(new Request.Builder().url(url).build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(AddressSelectionActivity.this, "Ошибка геокодирования", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
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

                            runOnUiThread(() -> {
                                Point newPoint = new Point(lat, lon);
                                updatePlacemark(newPoint);
                                binding.mapView.getMap().move(new CameraPosition(newPoint, 16.0f, 0.0f, 0.0f));
                            });
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void getAddressFromCoordinates(Point point) {
        String url = "https://geocode-maps.yandex.ru/1.x/?format=json&apikey=ad668e20-08d8-48ac-bfb1-aa6d6662b770&geocode="
                + point.getLongitude() + "," + point.getLatitude();

        OkHttpClient client = new OkHttpClient();
        okhttp3.Request request = new okhttp3.Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(AddressSelectionActivity.this, "Ошибка запроса", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull okhttp3.Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseData = Objects.requireNonNull(response.body()).string();
                        JSONObject json = new JSONObject(responseData);
                        JSONArray featureMember = json.getJSONObject("response")
                                .getJSONObject("GeoObjectCollection")
                                .getJSONArray("featureMember");

                        if (featureMember.length() > 0) {
                            String address = featureMember.getJSONObject(0)
                                    .getJSONObject("GeoObject")
                                    .getJSONObject("metaDataProperty")
                                    .getJSONObject("GeocoderMetaData")
                                    .getString("text");

                            runOnUiThread(() -> binding.addressEditText.setText(address));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

}