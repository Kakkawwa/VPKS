package com.example.diplom;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class MyCoworkingsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditableCoworkingCardAdapter editableAdapter;
    private List<CoworkingModel> coworkingList;
    private List<EventModel> eventList;
    private EventAdapter eventAdapter;

    private FirebaseFirestore db;
    private String userId;

    private TextView tabMyCoworkings, tabEvents;
    private Button btnAddAction, btnOrderEquipment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_coworkings);

        recyclerView = findViewById(R.id.recyclerViewMyCoworkings);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        coworkingList = new ArrayList<>();
        editableAdapter = new EditableCoworkingCardAdapter(this, coworkingList);

        eventList = new ArrayList<>();
        eventAdapter = new EventAdapter(this, eventList, true);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        btnAddAction = findViewById(R.id.btnAddAction);
        tabMyCoworkings = findViewById(R.id.tabMyCoworkings);
        tabEvents = findViewById(R.id.tabEvents);

        tabMyCoworkings.setOnClickListener(view -> {
            tabMyCoworkings.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
            tabEvents.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            btnAddAction.setText("Добавить коворкинг");
            btnAddAction.setOnClickListener(v -> startActivity(new Intent(MyCoworkingsActivity.this, AddSpaceActivity.class)));
            recyclerView.setAdapter(editableAdapter);
            loadMyCoworkings();
        });

        tabEvents.setOnClickListener(view -> {
            tabEvents.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
            tabMyCoworkings.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            btnAddAction.setText("Добавить мероприятие");
            btnAddAction.setOnClickListener(v -> startActivity(new Intent(MyCoworkingsActivity.this, AddEventActivity.class)));
            recyclerView.setAdapter(eventAdapter);
            loadEvents();
        });

        tabMyCoworkings.performClick();
    }

    private void loadMyCoworkings() {
        db.collection("coworking_spaces")
                .whereEqualTo("creatorId", userId)
                .orderBy("name", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    coworkingList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        CoworkingModel model = doc.toObject(CoworkingModel.class);
                        if (model != null) {
                            model.setDocumentId(doc.getId());
                            coworkingList.add(model);
                        }
                    }
                    editableAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e("MyCoworkings", "Ошибка загрузки коворкингов: " + e.getMessage());
                    Toast.makeText(MyCoworkingsActivity.this, "Ошибка загрузки коворкингов", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadEvents() {
        db.collection("events")
                .whereEqualTo("creatorId", userId)
                .orderBy("date", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    eventList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        EventModel event = doc.toObject(EventModel.class);
                        if (event != null) {
                            eventList.add(event);
                        }
                    }
                    eventAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e("MyEvents", "Ошибка загрузки мероприятий: " + e.getMessage());
                    Toast.makeText(MyCoworkingsActivity.this, "Ошибка загрузки мероприятий", Toast.LENGTH_SHORT).show();
                });
    }
}
