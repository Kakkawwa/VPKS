package com.example.diplom;

import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;

public class FullscreenImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen_image);

        ImageView fullImageView = findViewById(R.id.fullImageView);

        // Получаем URL изображения из Intent
        String imageUrl = getIntent().getStringExtra("imageUrl");
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this).load(imageUrl).into(fullImageView);
        }

        // Можно установить обработчик для закрытия Activity при нажатии
        fullImageView.setOnClickListener(v -> finish());
    }
}
