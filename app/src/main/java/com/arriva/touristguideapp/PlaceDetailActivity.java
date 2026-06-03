package com.arriva.touristguideapp;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class PlaceDetailActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_detail);

        String name = getIntent().getStringExtra("name");
        int image = getIntent().getIntExtra("image", 0);

        TextView tvName = findViewById(R.id.detailName);
        ImageView ivImage = findViewById(R.id.detailImage);

        if (tvName != null) tvName.setText(name);
        if (ivImage != null && image != 0) ivImage.setImageResource(image);
    }
}
