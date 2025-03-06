package com.example.diplom;

import android.app.Application;
import com.cloudinary.android.MediaManager;
import com.yandex.mapkit.MapKitFactory;

import java.util.HashMap;
import java.util.Map;

public class MyApp extends Application {

    private final String MAPKIT_API_KEY = "577f2403-4cb9-4f4d-b063-799b712a9009";

    public static final Map<String, String> CLOUDINARY_CONFIG = new HashMap<String, String>() {{
        put("cloud_name", "diiyvfgx4");
        put("api_key", "524818991871872");
        put("api_secret", "1QOlgV7r29GfUpdmgoM-fIt99Iw");
    }};

    @Override
    public void onCreate() {
        super.onCreate();
        MediaManager.init(this, CLOUDINARY_CONFIG);

        MapKitFactory.setApiKey(MAPKIT_API_KEY);
        MapKitFactory.initialize(this);

    }
}
