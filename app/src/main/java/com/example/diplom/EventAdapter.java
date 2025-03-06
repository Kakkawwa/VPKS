package com.example.diplom;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {
    private Context context;
    private List<EventModel> eventList;
    // Новое поле для контроля отображения кнопки опций
    private boolean showOptions;

    public EventAdapter(Context context, List<EventModel> eventList, boolean showOptions) {
        this.context = context;
        this.eventList = eventList;
        this.showOptions = showOptions;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.event_card, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        EventModel event = eventList.get(position);
        holder.tvEventName.setText(event.getName());
        holder.tvEventCategory.setText(event.getCategory());
        holder.tvEventDate.setText(event.getDate());
        holder.tvEventTime.setText(event.getStartTime() + " - " + event.getEndTime());
        holder.tvCoworkingName.setText(event.getCoworkingName());
        holder.tvEventDescription.setText(event.getDescription());

        // Если нужно показывать опции, устанавливаем обработчик, иначе скрываем кнопку
        if (showOptions) {
            holder.optionsButton.setVisibility(View.VISIBLE);
            holder.optionsButton.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(context, holder.optionsButton);
                popup.getMenuInflater().inflate(R.menu.editable_event_item_menu, popup.getMenu());
                popup.setOnMenuItemClickListener(item -> {
                    int id = item.getItemId();
                    if (id == R.id.menu_edit_event) {
                        Intent intent = new Intent(context, EditEventActivity.class);
                        intent.putExtra("eventId", event.getId());
                        intent.putExtra("name", event.getName());
                        intent.putExtra("category", event.getCategory());
                        intent.putExtra("date", event.getDate());
                        intent.putExtra("startTime", event.getStartTime());
                        intent.putExtra("endTime", event.getEndTime());
                        intent.putExtra("description", event.getDescription());
                        intent.putExtra("coworkingId", event.getCoworkingId());
                        intent.putExtra("coworkingName", event.getCoworkingName());
                        context.startActivity(intent);
                        return true;
                    } else if (id == R.id.menu_delete_event) {
                        new AlertDialog.Builder(context)
                                .setTitle("Удаление мероприятия")
                                .setMessage("Вы уверены, что хотите удалить это мероприятие?")
                                .setPositiveButton("Удалить", (dialog, which) -> {
                                    FirebaseFirestore.getInstance()
                                            .collection("events")
                                            .document(event.getId())
                                            .delete()
                                            .addOnSuccessListener(aVoid -> {
                                                eventList.remove(position);
                                                notifyItemRemoved(position);
                                                notifyItemRangeChanged(position, eventList.size());
                                                Toast.makeText(context, "Мероприятие удалено", Toast.LENGTH_SHORT).show();
                                            })
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(context, "Ошибка удаления: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                            );
                                })
                                .setNegativeButton("Отмена", null)
                                .show();
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        } else {
            holder.optionsButton.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView tvEventName, tvEventCategory, tvEventDate, tvEventTime, tvCoworkingName, tvEventDescription;
        ImageButton optionsButton;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEventName = itemView.findViewById(R.id.tvEventName);
            tvEventCategory = itemView.findViewById(R.id.tvEventCategory);
            tvEventDate = itemView.findViewById(R.id.tvEventDate);
            tvEventTime = itemView.findViewById(R.id.tvEventTime);
            tvCoworkingName = itemView.findViewById(R.id.tvCoworkingName);
            tvEventDescription = itemView.findViewById(R.id.tvEventDescription);
            optionsButton = itemView.findViewById(R.id.optionsButton);
        }
    }
}
