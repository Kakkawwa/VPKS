package com.example.diplom;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class EditableImageAdapter extends RecyclerView.Adapter<EditableImageAdapter.ViewHolder> {
    public interface OnDeleteListener {
        void onDelete(int position);
    }

    private final Context context;
    private List<String> imageUrls;
    private List<String> imagePublicIds;
    private final List<Uri> newUris;
    private final OnDeleteListener deleteListener;

    public EditableImageAdapter(Context context,
                                List<String> imageUrls,
                                List<String> imagePublicIds,
                                List<Uri> newUris,
                                OnDeleteListener deleteListener) {
        this.context = context;
        this.imageUrls = imageUrls;
        this.imagePublicIds = imagePublicIds;
        this.newUris = newUris;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_image_edit, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position < imageUrls.size()) {
            // Существующее изображение из Firestore
            String url = imageUrls.get(position);
            Glide.with(context)
                    .load(url)
                    .into(holder.imageView);

            // При клике переходим в FullscreenImageActivity
            holder.imageView.setOnClickListener(v -> {
                Intent intent = new Intent(context, FullscreenImageActivity.class);
                intent.putExtra("imageUrl", url);
                context.startActivity(intent);
            });

            // Используем getBindingAdapterPosition() для получения актуального индекса
            holder.btnRemove.setOnClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    deleteListener.onDelete(pos);
                }
            });
        } else {
            // Новое изображение (еще не загружено)
            int newPos = position - imageUrls.size();
            Uri uri = newUris.get(newPos);
            Glide.with(context)
                    .load(uri)
                    .into(holder.imageView);

            holder.imageView.setOnClickListener(v -> {
                Intent intent = new Intent(context, FullscreenImageActivity.class);
                intent.putExtra("imageUrl", uri.toString());
                context.startActivity(intent);
            });

            holder.btnRemove.setOnClickListener(v -> {
                newUris.remove(newPos);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, getItemCount());
            });
        }
    }

    @Override
    public int getItemCount() {
        return imageUrls.size() + newUris.size();
    }

    public void updateData(List<String> urls, List<String> publicIds) {
        this.imageUrls = urls;
        this.imagePublicIds = publicIds;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageView btnRemove;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }
    }
}
