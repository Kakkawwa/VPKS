package com.example.diplom;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CoworkingAdapter extends RecyclerView.Adapter<CoworkingAdapter.ViewHolder> {
    private List<CoworkingModel> coworkingList;
    private Context context;

    public CoworkingAdapter(Context context, List<CoworkingModel> coworkingList) {
        this.context = context;
        this.coworkingList = coworkingList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_coworking, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CoworkingModel coworking = coworkingList.get(position);
        holder.tvName.setText(coworking.getName());
        holder.tvAddress.setText("Адрес: " + coworking.getAddress());

        // Отображаем зоны: выводим первые две зоны, каждая на новой строке;
        // если зон больше двух, добавляем строку "Все тарифы"
        List<Map<String, Object>> zones = coworking.getZones();
        if (zones != null && !zones.isEmpty()) {
            StringBuilder zonesText = new StringBuilder();
            int count = Math.min(zones.size(), 2);
            for (int i = 0; i < count; i++) {
                Map<String, Object> zone = zones.get(i);
                zonesText.append(zone.get("name").toString())
                        .append(" (")
                        .append(zone.get("places").toString())
                        .append(" мест)");
                if (i < count - 1) {
                    zonesText.append("\n"); // переход на новую строку
                }
            }
            if (zones.size() > 2) {
                zonesText.append("\nВсе тарифы");
            }
            holder.tvZones.setText(zonesText.toString());
            holder.tvZones.setVisibility(View.VISIBLE);
        } else {
            holder.tvZones.setVisibility(View.GONE);
        }

        // Обработка изображений (если есть)
        if (coworking.getImages() != null && !coworking.getImages().isEmpty()) {
            ImageSliderAdapter imageSliderAdapter = new ImageSliderAdapter(holder.viewPager.getContext(), coworking.getImages());
            holder.viewPager.setAdapter(imageSliderAdapter);
        } else {
            holder.viewPager.setAdapter(null);
        }

        // Обработка клика по элементу
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, CoworkingDetailsActivity.class);
            intent.putExtra("coworkingId", coworking.getDocumentId());
            intent.putExtra("name", coworking.getName());
            intent.putExtra("address", coworking.getAddress());
            intent.putExtra("description", coworking.getDescription());
            intent.putStringArrayListExtra("images", new ArrayList<>(coworking.getImages()));
            intent.putExtra("creatorId", coworking.getCreatorId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return coworkingList.size();
    }

    // Метод для обновления списка
    public void updateList(List<CoworkingModel> newList) {
        coworkingList = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvAddress, tvZones;
        ViewPager2 viewPager;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            viewPager = itemView.findViewById(R.id.imageViewPager);
            tvName = itemView.findViewById(R.id.tvName);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            tvZones = itemView.findViewById(R.id.tvZones); // Убедитесь, что этот элемент присутствует в item_coworking.xml
        }
    }
}
