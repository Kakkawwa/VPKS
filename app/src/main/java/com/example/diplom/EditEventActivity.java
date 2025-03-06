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

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

public class EditEventActivity extends AppCompatActivity {

    private EditText etEventName, etEventCategory, etEventDate, etEventStartTime, etEventEndTime, etEventDescription;
    private TextView tvCoworkingName;
    private Button btnSaveEvent;
    private FirebaseFirestore db;
    private Calendar calendar;

    // Переменные для передачи данных
    private String eventId, coworkingId, coworkingName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_event);

        etEventName = findViewById(R.id.etEventName);
        etEventCategory = findViewById(R.id.etEventCategory);
        etEventDate = findViewById(R.id.etEventDate);
        etEventStartTime = findViewById(R.id.etEventStartTime);
        etEventEndTime = findViewById(R.id.etEventEndTime);
        etEventDescription = findViewById(R.id.etEventDescription);
        tvCoworkingName = findViewById(R.id.tvCoworkingName);
        btnSaveEvent = findViewById(R.id.btnSaveEvent);

        db = FirebaseFirestore.getInstance();
        calendar = Calendar.getInstance();

        // Получаем данные из Intent
        eventId = getIntent().getStringExtra("eventId");
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(EditEventActivity.this, "Ошибка: идентификатор события не найден", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        String name = getIntent().getStringExtra("name");
        String category = getIntent().getStringExtra("category");
        String date = getIntent().getStringExtra("date");
        String startTime = getIntent().getStringExtra("startTime");
        String endTime = getIntent().getStringExtra("endTime");
        String description = getIntent().getStringExtra("description");
        coworkingId = getIntent().getStringExtra("coworkingId"); // не редактируется
        coworkingName = getIntent().getStringExtra("coworkingName");

        // Заполняем поля
        etEventName.setText(name);
        etEventCategory.setText(category);
        etEventDate.setText(date);
        etEventStartTime.setText(startTime);
        etEventEndTime.setText(endTime);
        etEventDescription.setText(description);
        tvCoworkingName.setText("Коворкинг: " + coworkingName);

        // Обработчик для выбора даты (только сегодня и позже)
        etEventDate.setOnClickListener(v -> {
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            DatePickerDialog datePickerDialog = new DatePickerDialog(EditEventActivity.this,
                    (view, year1, month1, dayOfMonth) -> {
                        calendar.set(year1, month1, dayOfMonth);
                        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
                        etEventDate.setText(sdf.format(calendar.getTime()));
                    }, year, month, day);
            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
            datePickerDialog.show();
        });

        // Обработчик для выбора времени начала
        etEventStartTime.setOnClickListener(v -> {
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);
            TimePickerDialog timePickerDialog = new TimePickerDialog(EditEventActivity.this,
                    (view, hourOfDay, minute1) -> {
                        etEventStartTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute1));
                    }, hour, minute, DateFormat.is24HourFormat(EditEventActivity.this));
            timePickerDialog.show();
        });

        // Обработчик для выбора времени окончания
        etEventEndTime.setOnClickListener(v -> {
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);
            TimePickerDialog timePickerDialog = new TimePickerDialog(EditEventActivity.this,
                    (view, hourOfDay, minute1) -> {
                        etEventEndTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute1));
                    }, hour, minute, DateFormat.is24HourFormat(EditEventActivity.this));
            timePickerDialog.show();
        });

        // Обработка нажатия на кнопку "Сохранить изменения"
        btnSaveEvent.setOnClickListener(v -> {
            String newName = etEventName.getText().toString().trim();
            String newCategory = etEventCategory.getText().toString().trim();
            String newDate = etEventDate.getText().toString().trim();
            String newStartTime = etEventStartTime.getText().toString().trim();
            String newEndTime = etEventEndTime.getText().toString().trim();
            String newDescription = etEventDescription.getText().toString().trim();

            if (newName.isEmpty() || newCategory.isEmpty() || newDate.isEmpty() ||
                    newStartTime.isEmpty() || newEndTime.isEmpty() || newDescription.isEmpty()) {
                Toast.makeText(EditEventActivity.this, "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show();
                return;
            }

            // Проверка, что мероприятие не длится более 5 часов
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            try {
                Date start = sdf.parse(newStartTime);
                Date end = sdf.parse(newEndTime);
                long diffMillis = end.getTime() - start.getTime();
                if (diffMillis < 0) {
                    Toast.makeText(EditEventActivity.this, "Время окончания должно быть позже времени начала", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (diffMillis > 5 * 3600000) {
                    Toast.makeText(EditEventActivity.this, "Мероприятие не может длиться более 5 часов", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (ParseException e) {
                e.printStackTrace();
                Toast.makeText(EditEventActivity.this, "Ошибка обработки времени", Toast.LENGTH_SHORT).show();
                return;
            }

            // Подготовка данных для обновления
            Map<String, Object> updatedData = new HashMap<>();
            updatedData.put("name", newName);
            updatedData.put("category", newCategory);
            updatedData.put("date", newDate);
            updatedData.put("startTime", newStartTime);
            updatedData.put("endTime", newEndTime);
            updatedData.put("description", newDescription);
            // Поля coworkingId и coworkingName остаются неизменными

            // Обновление данных в Firestore
            db.collection("events").document(eventId)
                    .update(updatedData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(EditEventActivity.this, "Мероприятие обновлено", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(EditEventActivity.this, "Ошибка обновления: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }
}
