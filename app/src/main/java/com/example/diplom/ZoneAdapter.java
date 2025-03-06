package com.example.diplom;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ZoneAdapter extends RecyclerView.Adapter<ZoneAdapter.ViewHolder> {
    private List<Map<String, Object>> zones;
    private String coworkingId;
    private String coworkingName;

    public ZoneAdapter(
            List<Map<String, Object>> zones,
            String coworkingId,
            String coworkingName
    ) {
        this.zones = zones;
        this.coworkingId = coworkingId;
        this.coworkingName = coworkingName;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_zone_detail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> zone = zones.get(position);
        holder.zoneName.setText(zone.get("name").toString());
        holder.zonePlaces.setText("Мест: " + zone.get("places").toString());

        String price = zone.get("price").toString();
        String priceType = zone.get("priceType").toString();
        holder.zonePrice.setText(String.format("%s ₽/%s", price, priceType));

        // Проверяем, сколько доступных мест в зоне
        int available = 0;
        try {
            available = Integer.parseInt(zone.get("places").toString());
        } catch (NumberFormatException e) {
            available = 0;
        }

        if (available == 0) {
            holder.btnBook.setText("Нет доступных мест");
            holder.btnBook.setEnabled(false);
        } else {
            holder.btnBook.setText("Забронировать");
            holder.btnBook.setEnabled(true);
            holder.btnBook.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), BookingActivity.class);
                intent.putExtra("coworkingId", coworkingId);
                intent.putExtra("coworkingName", coworkingName);
                intent.putExtra("zoneName", zone.get("name").toString());
                intent.putExtra("price", zone.get("price").toString());
                intent.putExtra("priceType", zone.get("priceType").toString());
                // Передаем вместимость зоны (количество мест)
                intent.putExtra("zoneCapacity", zone.get("places").toString());
                v.getContext().startActivity(intent);
            });
        }
    }

    @Override
    public int getItemCount() {
        return zones.size();
    }

    // Метод для обновления списка
    public void updateList(List<Map<String, Object>> newList) {
        zones = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView zoneName, zonePlaces, zonePrice;
        Button btnBook;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            zoneName = itemView.findViewById(R.id.zoneName);
            zonePlaces = itemView.findViewById(R.id.zonePlaces);
            zonePrice = itemView.findViewById(R.id.zonePrice);
            btnBook = itemView.findViewById(R.id.btnBookZone);
        }
    }
}
