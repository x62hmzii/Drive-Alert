package com.example.driverdrowsinessdetectionsystem;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class HelpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        // Show a toast message (optional)
        Toast.makeText(this, "Welcome to Help Section", Toast.LENGTH_SHORT).show();
    }
}