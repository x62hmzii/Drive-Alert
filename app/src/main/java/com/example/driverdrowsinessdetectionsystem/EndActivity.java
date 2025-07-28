package com.example.driverdrowsinessdetectionsystem;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EndActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_end);

        TextView startTextView = findViewById(R.id.startTextView);
        TextView resultTextView = findViewById(R.id.resultTextView);
        TextView sensitivityTextView = findViewById(R.id.sensitivityTextView);
        Button btnRestart = findViewById(R.id.btnRestart);
        Button btnMainMenu = findViewById(R.id.btnMainMenu);

        Intent intent = getIntent();
        String startTime = intent.getStringExtra(Constants.KEY_START_TIME);
        String detectionResult = intent.getStringExtra(Constants.KEY_DETECTION_RESULT);
        String sensitivity = intent.getStringExtra(Constants.KEY_SENSITIVITY);

        // Format and display start time
        if (startTime != null) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
                Date date = inputFormat.parse(startTime);
                startTextView.setText("Start Time: " + displayFormat.format(date));
            } catch (Exception e) {
                startTextView.setText("Start Time: " + startTime);
            }
        } else {
            startTextView.setText("Start Time: Not available");
        }

        // Display detection result
        if (detectionResult != null) {
            resultTextView.setText("Status: " + detectionResult);
            if (detectionResult.equalsIgnoreCase("Drowsy")) {
                resultTextView.setTextColor(Color.RED);
            } else if (detectionResult.equalsIgnoreCase("Sleepy")) {
                resultTextView.setTextColor(Color.YELLOW);
            } else {
                resultTextView.setTextColor(Color.GREEN);
            }
        } else {
            resultTextView.setText("Status: No data");
        }

        // Display sensitivity level
        if (sensitivity != null) {
            String[] sensitivityLevels = {"Very Low", "Low", "Medium", "High", "Very High"};
            try {
                int level = Integer.parseInt(sensitivity);
                if (level >= 0 && level < sensitivityLevels.length) {
                    sensitivityTextView.setText("Sensitivity: " + sensitivityLevels[level]);
                } else {
                    sensitivityTextView.setText("Sensitivity: Custom (" + level + ")");
                }
            } catch (NumberFormatException e) {
                sensitivityTextView.setText("Sensitivity: " + sensitivity);
            }
        } else {
            sensitivityTextView.setText("Sensitivity: Standard");
        }

        // Button handlers
        btnRestart.setOnClickListener(v -> {
            Intent restartIntent = new Intent(EndActivity.this, FaceTrackerActivity.class);
            restartIntent.putExtra(Constants.KEY_START_TIME, startTime);
            startActivity(restartIntent);
            finish();
        });

        btnMainMenu.setOnClickListener(v -> {
            Intent mainIntent = new Intent(EndActivity.this, MainActivity.class);
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(mainIntent);
            finish();
        });
    }
}