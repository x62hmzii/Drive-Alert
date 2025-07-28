package com.example.driverdrowsinessdetectionsystem;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

public class SettingsFragment extends Fragment {

    private TextView textViewName, textViewEmail;
    private EditText editEmail, editTextNewPassword;
    private Button btnChangePassword, btnLogout;
    private DatabaseHelper databaseHelper;
    private String currentEmail;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Initialize views
        textViewName = view.findViewById(R.id.textViewName);
        textViewEmail = view.findViewById(R.id.textViewEmail);
        editEmail = view.findViewById(R.id.editEmail);
        editTextNewPassword = view.findViewById(R.id.editTextNewPassword);
        btnChangePassword = view.findViewById(R.id.btnChangePassword);
        btnLogout = view.findViewById(R.id.btnlogout);

        // Initialize database helper
        databaseHelper = new DatabaseHelper(getActivity());

        // Get current user data from MainActivity
        if (getActivity() != null && getActivity().getIntent() != null) {
            String username = getActivity().getIntent().getStringExtra("USERNAME");
            currentEmail = getActivity().getIntent().getStringExtra("EMAIL");

            textViewName.setText(username);
            textViewEmail.setText(currentEmail);
            editEmail.setText(currentEmail);
        }

        // Set up change password button
        btnChangePassword.setOnClickListener(v -> changePassword());

        // Set up logout button
        btnLogout.setOnClickListener(v -> logout());

        return view;
    }

    private void changePassword() {
        String email = editEmail.getText().toString().trim();
        String newPassword = editTextNewPassword.getText().toString().trim();

        if (email.isEmpty() || newPassword.isEmpty()) {
            Toast.makeText(getActivity(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!email.equals(currentEmail)) {
            Toast.makeText(getActivity(), "You can only change password for your own account", Toast.LENGTH_SHORT).show();
            return;
        }
        // Add password strength validation
        if (newPassword.length() < 6) {
            Toast.makeText(getActivity(), "Password should be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }


        boolean isUpdated = databaseHelper.updatePassword(email, newPassword);
        if (isUpdated) {
            Toast.makeText(getActivity(), "Password updated successfully", Toast.LENGTH_SHORT).show();
            editTextNewPassword.setText("");
        } else {
            Toast.makeText(getActivity(), "Password update failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void logout() {
        // Clear the session
        MyPreferences.clearSession(getActivity());
        // For now, just navigate to LoginSucess activity
        Intent intent = new Intent(getActivity(), LoginSucess.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }
}