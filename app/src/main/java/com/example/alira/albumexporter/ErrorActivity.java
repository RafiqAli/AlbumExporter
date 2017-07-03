package com.example.alira.albumexporter;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

/**
 * Created by alira on 6/20/2017.
 */

public class ErrorActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.error_activity);

        Intent intent = getIntent();
        String error   = intent.getExtras().getString("error");
        TextView errorText = (TextView) findViewById(R.id.error_message);
        errorText.setText(error);
    }

}
