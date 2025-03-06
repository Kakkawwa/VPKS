package com.example.diplom;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.firebase.firestore.*;
import androidx.appcompat.widget.SearchView;
import android.widget.ImageButton; // Импорт для кнопки

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainFragment extends Fragment {

    private RecyclerView recyclerView;
    private CoworkingAdapter adapter;
    private List<CoworkingModel> coworkingList;
    private FirebaseFirestore db;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        coworkingList = new ArrayList<>();
        adapter = new CoworkingAdapter(getContext(), coworkingList);
        recyclerView.setAdapter(adapter);
        db = FirebaseFirestore.getInstance();
        loadCoworkingSpaces();

        // Поиск по названию
        SearchView searchView = view.findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterCoworkingList(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterCoworkingList(newText);
                return false;
            }
        });

        return view;
    }


    private void loadCoworkingSpaces() {
        CollectionReference coworkingRef = db.collection("coworking_spaces");

        coworkingRef.addSnapshotListener((queryDocumentSnapshots, error) -> {
            if (error != null) {
                return;
            }
            if (queryDocumentSnapshots != null) {
                coworkingList.clear();
                for (DocumentSnapshot document : queryDocumentSnapshots) {
                    CoworkingModel coworking = document.toObject(CoworkingModel.class);
                    if (coworking != null) {
                        // Устанавливаем идентификатор документа
                        coworking.setDocumentId(document.getId());
                        coworkingList.add(coworking);
                    }
                }

                // Сортируем список по названию коворкинга
                Collections.sort(coworkingList, Comparator.comparing(CoworkingModel::getName));

                adapter.notifyDataSetChanged();
            }
        });
    }


    private void filterCoworkingList(String query) {
        List<CoworkingModel> filteredList = new ArrayList<>();
        for (CoworkingModel coworking : coworkingList) {
            if (coworking.getName().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(coworking);
            }
        }
        adapter.updateList(filteredList);
    }
}
