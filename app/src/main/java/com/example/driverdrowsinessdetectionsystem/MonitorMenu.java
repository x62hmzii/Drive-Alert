package com.example.driverdrowsinessdetectionsystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MonitorMenu extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_monitor_menu, container, false);
        Button startDetectionButton = view.findViewById(R.id.btnStartDetection);

        startDetectionButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), FaceTrackerActivity.class);
            String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            intent.putExtra(Constants.KEY_START_TIME, currentTime);
            startActivity(intent);
        });

        return view;
    }
}