package com.routefinder.dan.routefinder;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private Button btnLoad, btnNew;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTitle("Route Mapper");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        btnLoad = (Button) findViewById(R.id.btnLoad);
        btnNew = (Button) findViewById(R.id.btnNew);

        btnLoad.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                intent.putExtra("filePath", getApplicationContext().getFilesDir() + "/test.obj");
                startActivity(intent);
                finish();
            }
        });

        btnNew.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, MapsActivity.class));
                finish();
            }
        });
    }
}
