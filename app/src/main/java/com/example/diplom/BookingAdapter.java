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
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.ViewHolder> {
    private Context context;
    private List<BookingModel> bookingList;

    public BookingAdapter(Context context, List<BookingModel> bookingList) {
        this.context = context;
        this.bookingList = bookingList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_booking, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BookingModel booking = bookingList.get(position);
        holder.tvCoworkingName.setText(booking.getCoworkingName());
        holder.tvDate.setText("Дата: " + booking.getDate());
        holder.tvHours.setText("Часы: " + booking.getHours());
        holder.tvPeople.setText("Количество людей: " + booking.getPeople());
        holder.tvComment.setText("Комментарий: " + booking.getComment());

        // Обработчик для кнопки опций
        holder.optionsButton.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(context, holder.optionsButton);
            popup.getMenuInflater().inflate(R.menu.booking_item_menu, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.menu_edit) {
                    // Переход в активность редактирования бронирования
                    Intent intent = new Intent(context, EditBookingActivity.class);
                    intent.putExtra("bookingId", booking.getBookingId());
                    // Можно передать и другие данные бронирования, если необходимо
                    context.startActivity(intent);
                    return true;
                } else if (id == R.id.menu_delete) {
                    // Удаление бронирования из Firestore
                    FirebaseFirestore.getInstance()
                            .collection("Bookings")
                            .document(booking.getBookingId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                bookingList.remove(position);
                                notifyItemRemoved(position);
                                notifyItemRangeChanged(position, bookingList.size());
                                Toast.makeText(context, "Бронирование удалено", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(context, "Ошибка удаления: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                    return true;
                }
                return false;
            });
            popup.show();
        });
    }

    @Override
    public int getItemCount() {
        return bookingList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCoworkingName, tvDate, tvHours, tvPeople, tvComment;
        ImageButton optionsButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCoworkingName = itemView.findViewById(R.id.tvCoworkingName);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvHours = itemView.findViewById(R.id.tvHours);
            tvPeople = itemView.findViewById(R.id.tvPeople);
            tvComment = itemView.findViewById(R.id.tvComment);
            optionsButton = itemView.findViewById(R.id.optionsButton);
        }
    }
}
