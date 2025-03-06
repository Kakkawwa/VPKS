package com.example.diplom;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.diplom.databinding.ItemImageBinding;
import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {
    private List<Uri> imageList;

    public ImageAdapter(List<Uri> imageList) {
        this.imageList = imageList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemImageBinding binding = ItemImageBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Uri imageUri = imageList.get(position);

        Glide.with(holder.imageView.getContext())
                .load(imageUri)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.stat_notify_error)
                .into(holder.imageView);

        // При клике на фотографию открываем FullscreenImageActivity
        holder.imageView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), FullscreenImageActivity.class);
            intent.putExtra("imageUrl", imageUri.toString());
            v.getContext().startActivity(intent);
        });

        // Удаление изображения при нажатии на крестик
        holder.btnRemove.setOnClickListener(v -> {
            imageList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, imageList.size());
        });
    }

    @Override
    public int getItemCount() {
        return imageList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView, btnRemove;

        public ViewHolder(@NonNull ItemImageBinding binding) {
            super(binding.getRoot());
            imageView = binding.imageView;
            btnRemove = binding.btnRemove;
        }
    }
}
