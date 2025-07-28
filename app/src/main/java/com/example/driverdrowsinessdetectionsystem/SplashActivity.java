package com.example.driverdrowsinessdetectionsystem;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Delay for 3 seconds before starting the next activity (LoginActivity)
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashActivity.this, LoginSucess.class);
                startActivity(intent);
                finish(); // Close the SplashActivity so it can't be returned to
            }
        }, 3000); // 3000 milliseconds = 3 seconds
    }
}
