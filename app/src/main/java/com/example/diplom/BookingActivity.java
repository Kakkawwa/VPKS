package com.example.diplom;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
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
    private NumberPicker peoplePicker;
    private EditText commentEditText;
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
        peoplePicker = findViewById(R.id.peoplePicker);
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

        // Настраиваем NumberPicker для количества людей
        peoplePicker.setMinValue(1);
        peoplePicker.setMaxValue(zoneCapacity);

        // Инициализируем календари
        startDateTime = Calendar.getInstance();
        endDateTime = Calendar.getInstance();

        // Устанавливаем слушатели выбора даты/времени
        startDateTimeTextView.setOnClickListener(v -> showDateTimePicker(true));
        endDateTimeTextView.setOnClickListener(v -> showDateTimePicker(false));

        confirmButton.setOnClickListener(v -> saveBookingToFirestore());
    }

    // Метод выбора даты и времени
    private void showDateTimePicker(boolean isStart) {
        Calendar now = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth);
                    // Ограничиваем выбор: нельзя выбрать прошедшие даты и даты позже 6 месяцев
                    Calendar maxDate = Calendar.getInstance();
                    maxDate.add(Calendar.MONTH, 6);
                    if(selected.before(now)) {
                        Toast.makeText(this, "Нельзя выбрать прошедшую дату", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if(selected.after(maxDate)) {
                        Toast.makeText(this, "Нельзя выбрать дату более чем через 6 месяцев", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // После выбора даты – показываем TimePickerDialog
                    TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                            (timeView, hourOfDay, minute) -> {
                                selected.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                selected.set(Calendar.MINUTE, minute);
                                String formatted = DateFormat.format("dd/MM/yyyy HH:mm", selected).toString();
                                if(isStart) {
                                    startDateTime = selected;
                                    startDateTimeTextView.setText(formatted);
                                } else {
                                    endDateTime = selected;
                                    endDateTimeTextView.setText(formatted);
                                }
                            },
                            now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true);
                    timePickerDialog.show();
                },
                now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
        // Ограничиваем даты в DatePickerDialog
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        Calendar sixMonthsLater = Calendar.getInstance();
        sixMonthsLater.add(Calendar.MONTH, 6);
        datePickerDialog.getDatePicker().setMaxDate(sixMonthsLater.getTimeInMillis());
        datePickerDialog.show();
    }

    private void saveBookingToFirestore() {
        int people = peoplePicker.getValue();

        if (people > zoneCapacity) {
            Toast.makeText(this, "Недостаточно мест", Toast.LENGTH_SHORT).show();
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

    private void updateZoneCapacity(int change) {
        // change отрицательное, если бронирование – вычитаем места, и положительное – при отмене бронирования
        FirebaseFirestore.getInstance()
                .collection("coworking_spaces")
                .document(coworkingId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if(documentSnapshot.exists()){
                        // Получаем массив зон
                        List<Map<String, Object>> zones = (List<Map<String, Object>>) documentSnapshot.get("zones");
                        if(zones != null){
                            for (Map<String, Object> zoneMap : zones) {
                                if(zoneMap.get("name").toString().equals(zoneName)){
                                    // Получаем текущее количество мест
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
                            // Обновляем документ коворкинга с новым массивом зон
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
