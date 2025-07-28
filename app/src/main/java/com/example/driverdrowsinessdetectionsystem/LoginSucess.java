package com.example.driverdrowsinessdetectionsystem;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginSucess extends AppCompatActivity {
    private TextView tvRegister;
    private EditText etLoginGmail, etLoginPassword;
    private Button loginButton;

    private SQLiteDatabase db;
    private SQLiteOpenHelper openHelper;
    private Cursor cursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if user is already logged in
        if (MyPreferences.isLoggedIn(this)) {
            redirectToMainActivity();
            return;
        }

        setContentView(R.layout.activity_login_sucess);

        openHelper = new DatabaseHelper(this);
        db = openHelper.getReadableDatabase();
        tvRegister = findViewById(R.id.tvRegister);
        etLoginGmail = findViewById(R.id.etLogGmail);
        etLoginPassword = findViewById(R.id.etLoginPassword);
        loginButton = findViewById(R.id.btnLogin);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etLoginGmail.getText().toString().trim();
                String password = etLoginPassword.getText().toString().trim();
                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginSucess.this, "Enter your Email and Password to login", Toast.LENGTH_SHORT).show();
                } else {
                    cursor = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_NAME + " WHERE " + DatabaseHelper.COL_3 + "=? AND " + DatabaseHelper.COL_4 + "=?", new String[]{email, password});
                    if (cursor != null) {
                        if (cursor.getCount() > 0) {
                            cursor.moveToFirst();

                            // Fetch data
                            String username = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_2));
                            String userEmail = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_3));

                            // Save login session
                            MyPreferences.setLoggedIn(LoginSucess.this, true, userEmail, username);

                            // Redirect to main activity
                            redirectToMainActivity(username, userEmail);

                            Toast.makeText(getApplicationContext(), "Login success", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getApplicationContext(), "Login error", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        });

        tvRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginSucess.this, RegisterActivity.class));
                finish();
            }
        });
    }

    private void redirectToMainActivity() {
        String username = MyPreferences.getLoggedInName(this);
        String email = MyPreferences.getLoggedInEmail(this);
        redirectToMainActivity(username, email);
    }

    private void redirectToMainActivity(String username, String email) {
        Intent intent = new Intent(LoginSucess.this, MainActivity.class);
        intent.putExtra("USERNAME", username);
        intent.putExtra("EMAIL", email);
        startActivity(intent);
        finish(); // Close login activity
    }
}