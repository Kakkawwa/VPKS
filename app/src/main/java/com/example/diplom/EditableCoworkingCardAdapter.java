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
import androidx.viewpager2.widget.ViewPager2;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class EditableCoworkingCardAdapter extends RecyclerView.Adapter<EditableCoworkingCardAdapter.ViewHolder> {

    private List<CoworkingModel> coworkingList;
    private Context context;

    public EditableCoworkingCardAdapter(Context context, List<CoworkingModel> coworkingList) {
        this.context = context;
        this.coworkingList = coworkingList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_editable_coworking_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CoworkingModel model = coworkingList.get(position);
        holder.tvName.setText(model.getName());
        holder.tvAddress.setText("Адрес: " + model.getAddress());
        holder.tvPlaces.setText("Мест: " + model.getPlaces());
        holder.tvPrice.setText("Цена: " + model.getPrice() + "₽");

        // Настраиваем карусель изображений, если они есть
        if (model.getImages() != null && !model.getImages().isEmpty()) {
            ImageSliderAdapter sliderAdapter = new ImageSliderAdapter(context, model.getImages());
            holder.imageViewPager.setAdapter(sliderAdapter);
            holder.imageViewPager.setVisibility(View.VISIBLE);
        } else {
            holder.imageViewPager.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, CoworkingDetailsActivity.class);
            intent.putExtra("coworkingId", model.getDocumentId()); // Исправлено здесь!
            intent.putExtra("name", model.getName());
            intent.putExtra("address", model.getAddress());
            intent.putExtra("places", model.getPlaces());
            intent.putExtra("description", model.getDescription());
            intent.putExtra("price", model.getPrice());
            intent.putStringArrayListExtra("images", new ArrayList<>(model.getImages()));
            intent.putExtra("creatorId", model.getCreatorId());
            intent.putExtra("hideBooking", true);
            context.startActivity(intent);
        });


        // Обработчик для показа PopupMenu при нажатии на кнопку с тремя точками
        holder.optionsButton.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(context, holder.optionsButton);
            popup.getMenuInflater().inflate(R.menu.editable_coworking_item_menu, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.menu_edit) {
                    // Переход к редактированию коворкинга
                    Intent intent = new Intent(context, EditCoworkingActivity.class);
                    intent.putExtra("documentId", model.getDocumentId());
                    context.startActivity(intent);
                    return true;
                } else if (id == R.id.menu_delete) {
                    new AlertDialog.Builder(context)
                            .setTitle("Удаление коворкинга")
                            .setMessage("Вы уверены, что хотите удалить этот коворкинг?")
                            .setPositiveButton("Удалить", (dialog, which) -> {
                                deleteCoworkingWithImages(model, position);
                            })
                            .setNegativeButton("Отмена", null)
                            .show();
                    return true;
                }
                return false;
            });
            popup.show();
        });
    }

    private void deleteCoworkingWithImages(CoworkingModel model, int position) {
        List<String> publicIds = model.getImagesPublicIds(); // список public_id фотографий
        if (publicIds != null && !publicIds.isEmpty()) {
            Cloudinary cloudinary = new Cloudinary(MyApp.CLOUDINARY_CONFIG); // убедитесь, что MyApp.CLOUDINARY_CONFIG настроен
            final int total = publicIds.size();
            final AtomicInteger completed = new AtomicInteger(0);
            for (String publicId : publicIds) {
                new Thread(() -> {
                    try {
                        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                        // Логирование успешного удаления
                        // Log.d("Cloudinary", "Deleted image: " + publicId);
                    } catch (Exception e) {
                        // Логирование ошибки удаления
                        // Log.e("Cloudinary", "Error deleting image: " + publicId, e);
                    } finally {
                        if (completed.incrementAndGet() == total) {
                            // После удаления всех изображений – удаляем документ Firestore
                            deleteFirestoreDocument(model, position);
                        }
                    }
                }).start();
            }
        } else {
            // Если фотографий для удаления нет, сразу удаляем документ
            deleteFirestoreDocument(model, position);
        }
    }

    private void deleteFirestoreDocument(CoworkingModel model, int position) {
        FirebaseFirestore.getInstance()
                .collection("coworking_spaces")
                .document(model.getDocumentId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Удаляем связанные с коворкингом мероприятия
                    FirebaseFirestore.getInstance()
                            .collection("events")
                            .whereEqualTo("coworkingId", model.getDocumentId())
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                    doc.getReference().delete();
                                }
                                // Удаляем коворкинг из списка адаптера и обновляем UI
                                coworkingList.remove(position);
                                notifyItemRemoved(position);
                                notifyItemRangeChanged(position, coworkingList.size());
                                Toast.makeText(context, "Коворкинг и связанные мероприятия удалены", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(context, "Ошибка удаления связанных мероприятий: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(context, "Ошибка удаления коворкинга: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }


    @Override
    public int getItemCount() {
        return coworkingList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvAddress, tvPlaces, tvPrice;
        ImageButton optionsButton;
        ViewPager2 imageViewPager;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            tvPlaces = itemView.findViewById(R.id.tvPlaces);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            optionsButton = itemView.findViewById(R.id.optionsButton);
            imageViewPager = itemView.findViewById(R.id.imageViewPager);
        }
    }
}
