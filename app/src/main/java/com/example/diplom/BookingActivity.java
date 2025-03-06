package com.example.diplom;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BookingActivity extends AppCompatActivity {

    private TextView startDateTimeTextView, endDateTimeTextView, selectedZoneTextView;
    private EditText peopleEditText, commentEditText;
    private Button confirmButton;

    private Calendar startDateTime, endDateTime;
    private String coworkingId, coworkingName, zoneName, userId;
    private int zoneCapacity;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        startDateTimeTextView = findViewById(R.id.startDateTimeTextView);
        endDateTimeTextView = findViewById(R.id.endDateTimeTextView);
        selectedZoneTextView = findViewById(R.id.selectedZoneTextView);
        peopleEditText = findViewById(R.id.peopleEditText);
        commentEditText = findViewById(R.id.commentEditText);
        confirmButton = findViewById(R.id.confirmButton);

        db = FirebaseFirestore.getInstance();

        // Получаем данные из Intent
        coworkingId = getIntent().getStringExtra("coworkingId");
        coworkingName = getIntent().getStringExtra("coworkingName");
        zoneName = getIntent().getStringExtra("zoneName");
        try {
            zoneCapacity = Integer.parseInt(getIntent().getStringExtra("zoneCapacity"));
        } catch (Exception e) {
            zoneCapacity = 20;
        }

        if(coworkingId == null || coworkingId.equals("0")){
            Toast.makeText(this, "Ошибка: неверный идентификатор коворкинга", Toast.LENGTH_SHORT).show();
            finish();
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if(currentUser != null){
            userId = currentUser.getUid();
        } else {
            Toast.makeText(this, "Ошибка: пользователь не авторизован", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Отобразим выбранную зону и её вместимость
        selectedZoneTextView.setText("Выбрана зона: " + zoneName + " (" + zoneCapacity + " мест)");

        // Инициализируем календари
        startDateTime = Calendar.getInstance();
        endDateTime = Calendar.getInstance();

        // При нажатии на startDateTimeTextView выбираем дату/время начала бронирования
        startDateTimeTextView.setOnClickListener(v -> showDateTimePicker(true));

        // При выборе даты/времени окончания бронирования, устанавливаем минимальное значение равное startDateTime
        endDateTimeTextView.setOnClickListener(v -> showDateTimePicker(false));

        confirmButton.setOnClickListener(v -> saveBookingToFirestore());
    }

    private void showDateTimePicker(boolean isStart) {
        Calendar now = Calendar.getInstance();
        if (!isStart && startDateTime != null) {
            now = (Calendar) startDateTime.clone();
        }
        Calendar finalNow = now;
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    // Объявляем переменную как final, чтобы её можно было использовать во вложенной лямбде
                    final Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth);

                    final Calendar minDate = Calendar.getInstance();
                    final Calendar maxDate = Calendar.getInstance();
                    maxDate.add(Calendar.MONTH, 6);
                    // Для окончания бронирования минимальная дата – выбранная дата начала
                    if (!isStart && startDateTime != null) {
                        minDate.setTimeInMillis(startDateTime.getTimeInMillis());
                    }
                    if (selected.before(minDate)) {
                        Toast.makeText(this, "Нельзя выбрать прошедшую дату или дату раньше начала бронирования", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (selected.after(maxDate)) {
                        Toast.makeText(this, "Нельзя выбрать дату более чем через 6 месяцев", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Показываем TimePickerDialog после выбора даты
                    TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                            (timeView, hourOfDay, minute) -> {
                                selected.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                selected.set(Calendar.MINUTE, minute);
                                String formatted = DateFormat.format("dd MMM yyyy HH:mm", selected).toString();
                                if (isStart) {
                                    startDateTime = selected;
                                    startDateTimeTextView.setText(formatted);
                                    // Отмечаем, что по умолчанию конец бронирования совпадает с началом
                                    endDateTimeTextView.setText(formatted);
                                    endDateTime = (Calendar) startDateTime.clone();
                                } else {
                                    if (selected.before(startDateTime)) {
                                        Toast.makeText(this, "Дата и время окончания не может быть раньше начала", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    endDateTime = selected;
                                    endDateTimeTextView.setText(formatted);
                                }
                            },
                            finalNow.get(Calendar.HOUR_OF_DAY), finalNow.get(Calendar.MINUTE), true);
                    timePickerDialog.show();
                },
                now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));

        // Устанавливаем минимальную дату для DatePickerDialog
        if (isStart) {
            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        } else if (startDateTime != null) {
            datePickerDialog.getDatePicker().setMinDate(startDateTime.getTimeInMillis());
        }
        Calendar sixMonthsLater = Calendar.getInstance();
        sixMonthsLater.add(Calendar.MONTH, 6);
        datePickerDialog.getDatePicker().setMaxDate(sixMonthsLater.getTimeInMillis());
        datePickerDialog.show();
    }


    private void saveBookingToFirestore() {
        // Получаем количество человек из поля
        String peopleStr = peopleEditText.getText().toString().trim();
        if(peopleStr.isEmpty()){
            Toast.makeText(this, "Введите количество человек", Toast.LENGTH_SHORT).show();
            return;
        }
        int people;
        try {
            people = Integer.parseInt(peopleStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Неверное число", Toast.LENGTH_SHORT).show();
            return;
        }

        if (people > zoneCapacity) {
            Toast.makeText(this, "Недостаточно мест в зоне", Toast.LENGTH_SHORT).show();
            return;
        }

        if(zoneCapacity <= 0){
            Toast.makeText(this, "Нет свободных мест в зоне", Toast.LENGTH_SHORT).show();
            return;
        }

        if(startDateTimeTextView.getText().toString().equals("Выберите дату и время") ||
                endDateTimeTextView.getText().toString().equals("Выберите дату и время")) {
            Toast.makeText(this, "Выберите дату и время начала и окончания бронирования", Toast.LENGTH_SHORT).show();
            return;
        }
        if(!endDateTime.after(startDateTime)) {
            Toast.makeText(this, "Дата окончания должна быть позже даты начала", Toast.LENGTH_SHORT).show();
            return;
        }
        // Проверяем, что бронирование минимум на 1 час
        long diff = endDateTime.getTimeInMillis() - startDateTime.getTimeInMillis();
        if(diff < 60 * 60 * 1000) { // 1 час = 3600000 миллисекунд
            Toast.makeText(this, "Бронирование должно быть минимум на 1 час", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> booking = new HashMap<>();
        booking.put("coworkingId", coworkingId);
        booking.put("coworkingName", coworkingName);
        booking.put("zoneName", zoneName);
        booking.put("userId", userId);
        booking.put("startDateTime", DateFormat.format("dd MMM yyyy HH:mm", startDateTime).toString());
        booking.put("endDateTime", DateFormat.format("dd MMM yyyy HH:mm", endDateTime).toString());
        booking.put("people", people);
        booking.put("comment", commentEditText.getText().toString());

        db.collection("Bookings").add(booking).addOnSuccessListener(documentReference -> {
            updateZoneCapacity(-people); // Уменьшаем количество мест
            Toast.makeText(this, "Бронирование успешно!", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Ошибка бронирования", Toast.LENGTH_SHORT).show()
        );
    }


    // Метод обновления количества мест (как в предыдущем примере, обновляем массив зон в документе коворкинга)
    private void updateZoneCapacity(int change) {
        FirebaseFirestore.getInstance()
                .collection("coworking_spaces")
                .document(coworkingId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if(documentSnapshot.exists()){
                        List<Map<String, Object>> zones = (List<Map<String, Object>>) documentSnapshot.get("zones");
                        if(zones != null){
                            for (Map<String, Object> zoneMap : zones) {
                                if(zoneMap.get("name").toString().equals(zoneName)){
                                    int currentCapacity = 0;
                                    try {
                                        currentCapacity = Integer.parseInt(zoneMap.get("places").toString());
                                    } catch (NumberFormatException ex) {
                                        currentCapacity = 0;
                                    }
                                    int newCapacity = currentCapacity + change;
                                    if(newCapacity < 0) newCapacity = 0;
                                    zoneMap.put("places", newCapacity);
                                }
                            }
                            FirebaseFirestore.getInstance()
                                    .collection("coworking_spaces")
                                    .document(coworkingId)
                                    .update("zones", zones)
                                    .addOnSuccessListener(aVoid -> {
                                        // Обновляем локальное значение zoneCapacity
                                        for (Map<String, Object> zoneMap : zones) {
                                            if(zoneMap.get("name").toString().equals(zoneName)){
                                                try {
                                                    zoneCapacity = Integer.parseInt(zoneMap.get("places").toString());
                                                } catch(Exception e) {
                                                    zoneCapacity = 0;
                                                }
                                                break;
                                            }
                                        }
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(BookingActivity.this, "Ошибка обновления мест: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                    );
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(BookingActivity.this, "Ошибка получения данных: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}
