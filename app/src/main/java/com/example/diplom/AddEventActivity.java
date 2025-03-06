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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddEventActivity extends AppCompatActivity {

    private EditText etEventName, etEventCategory, etEventDate, etEventStartTime, etEventEndTime, etEventDescription;
    private Button btnAddEvent, btnSelectCoworking;
    private TextView tvSelectedCoworking;
    private FirebaseFirestore db;
    private Calendar calendar;

    // Для хранения выбранного коворкинга
    private String selectedCoworkingId = null;
    private String selectedCoworkingName = null;
    // Идентификатор текущего пользователя
    private String userId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_event);

        etEventName = findViewById(R.id.etEventName);
        etEventCategory = findViewById(R.id.etEventCategory);
        etEventDate = findViewById(R.id.etEventDate);
        etEventStartTime = findViewById(R.id.etEventStartTime);
        etEventEndTime = findViewById(R.id.etEventEndTime);
        etEventDescription = findViewById(R.id.etEventDescription);
        btnAddEvent = findViewById(R.id.btnAddEvent);
        btnSelectCoworking = findViewById(R.id.btnSelectCoworking);
        tvSelectedCoworking = findViewById(R.id.tvSelectedCoworking);

        db = FirebaseFirestore.getInstance();
        calendar = Calendar.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Обработчик для выбора даты с ограничением (только сегодня и позже)
        etEventDate.setOnClickListener(v -> {
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            DatePickerDialog datePickerDialog = new DatePickerDialog(AddEventActivity.this,
                    (view, year1, month1, dayOfMonth) -> {
                        calendar.set(year1, month1, dayOfMonth);
                        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
                        etEventDate.setText(sdf.format(calendar.getTime()));
                    }, year, month, day);
            // Устанавливаем минимальную дату на сегодняшний день
            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
            datePickerDialog.show();
        });


        // Обработчик для выбора времени начала
        etEventStartTime.setOnClickListener(v -> {
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);
            TimePickerDialog timePickerDialog = new TimePickerDialog(AddEventActivity.this,
                    (view, hourOfDay, minute1) -> {
                        etEventStartTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute1));
                    }, hour, minute, DateFormat.is24HourFormat(AddEventActivity.this));
            timePickerDialog.show();
        });

        // Обработчик для выбора времени окончания
        etEventEndTime.setOnClickListener(v -> {
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);
            TimePickerDialog timePickerDialog = new TimePickerDialog(AddEventActivity.this,
                    (view, hourOfDay, minute1) -> {
                        etEventEndTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute1));
                    }, hour, minute, DateFormat.is24HourFormat(AddEventActivity.this));
            timePickerDialog.show();
        });

        // Обработчик для выбора коворкинга (вызываем диалог)
        btnSelectCoworking.setOnClickListener(v -> showCoworkingSelectionDialog());

        // Обработка нажатия кнопки для добавления мероприятия
        btnAddEvent.setOnClickListener(v -> {
            String name = etEventName.getText().toString().trim();
            String category = etEventCategory.getText().toString().trim();
            String date = etEventDate.getText().toString().trim();
            String startTime = etEventStartTime.getText().toString().trim();
            String endTime = etEventEndTime.getText().toString().trim();
            String description = etEventDescription.getText().toString().trim();

            if(name.isEmpty() || category.isEmpty() || date.isEmpty() || startTime.isEmpty() ||
                    endTime.isEmpty() || description.isEmpty() || selectedCoworkingId == null){
                Toast.makeText(AddEventActivity.this, "Пожалуйста, заполните все поля и выберите коворкинг", Toast.LENGTH_SHORT).show();
                return;
            }

            // Проверка, что мероприятие не длится более 5 часов
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            try {
                Date startDate = sdf.parse(startTime);
                Date endDate = sdf.parse(endTime);
                long diffMillis = endDate.getTime() - startDate.getTime();
                // Если разница отрицательная (например, событие переходит через полночь) – можно добавить дополнительную обработку
                if(diffMillis < 0) {
                    Toast.makeText(AddEventActivity.this, "Время окончания должно быть позже времени начала", Toast.LENGTH_SHORT).show();
                    return;
                }
                // 5 часов = 5 * 3600000 = 18 000 000 миллисекунд
                if(diffMillis > 5 * 3600000) {
                    Toast.makeText(AddEventActivity.this, "Мероприятие не может длиться более 5 часов", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (ParseException e) {
                e.printStackTrace();
                Toast.makeText(AddEventActivity.this, "Ошибка обработки времени", Toast.LENGTH_SHORT).show();
                return;
            }

            // Подготовка данных для сохранения в Firestore
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("name", name);
            eventData.put("category", category);
            eventData.put("date", date);
            eventData.put("startTime", startTime);
            eventData.put("endTime", endTime);
            eventData.put("description", description);
            eventData.put("coworkingId", selectedCoworkingId);
            eventData.put("coworkingName", selectedCoworkingName);
            eventData.put("creatorId", userId);

            db.collection("events")
                    .add(eventData)
                    .addOnSuccessListener(documentReference -> {
                        String id = documentReference.getId();
                        // Обновляем документ, добавляя в него поле "id"
                        documentReference.update("id", id)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(AddEventActivity.this, "Мероприятие добавлено", Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(AddEventActivity.this, "Ошибка обновления id: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(AddEventActivity.this, "Ошибка добавления мероприятия: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        });
    }

    // Метод для отображения диалога выбора коворкинга
    private void showCoworkingSelectionDialog() {
        // Получаем список коворкингов, добавленных текущим пользователем
        db.collection("coworking_spaces")
                .whereEqualTo("creatorId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if(queryDocumentSnapshots.isEmpty()){
                        Toast.makeText(AddEventActivity.this, "У вас пока нет добавленных коворкингов", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    ArrayList<String> coworkingNames = new ArrayList<>();
                    ArrayList<String> coworkingIds = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        String name = doc.getString("name");
                        if(name != null){
                            coworkingNames.add(name);
                            coworkingIds.add(doc.getId());
                        }
                    }
                    // Преобразуем список в массив
                    String[] coworkingArray = coworkingNames.toArray(new String[0]);
                    new AlertDialog.Builder(AddEventActivity.this)
                            .setTitle("Выберите коворкинг")
                            .setItems(coworkingArray, (dialog, which) -> {
                                selectedCoworkingName = coworkingArray[which];
                                selectedCoworkingId = coworkingIds.get(which);
                                // Обновляем отдельное поле с названием выбранного коворкинга
                                tvSelectedCoworking.setText(selectedCoworkingName);
                            })
                            .setNegativeButton("Отмена", null)
                            .show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(AddEventActivity.this, "Ошибка загрузки коворкингов: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}
