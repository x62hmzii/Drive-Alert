package com.example.driverdrowsinessdetectionsystem;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LocationFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Marker currentMarker;
    private Polyline routePolyline;
    private final List<LatLng> routePoints = new ArrayList<>();
    private boolean isTracking = false;
    private boolean isSharingLiveLocation = false;
    private LatLng startPoint;
    private TextView tvTripStatus;
    private TextView tvLocationInfo;
    private FloatingActionButton fabShareLocation;
    private String currentLocationUrl = "";

    // Permission request launchers
    private final ActivityResultLauncher<String> locationPermissionRequest =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    initializeMap();
                } else {
                    Toast.makeText(requireContext(), R.string.location_permission_denied, Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_location, container, false);

        // Initialize views
        tvTripStatus = view.findViewById(R.id.tvTripStatus);
        tvLocationInfo = view.findViewById(R.id.tvLocationInfo);
        FloatingActionButton fabStart = view.findViewById(R.id.fabStartTrip);
        FloatingActionButton fabEnd = view.findViewById(R.id.fabEndTrip);
        fabShareLocation = view.findViewById(R.id.fabShareLocation);

        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Setup FABs
        fabStart.setOnClickListener(v -> startTrip());
        fabEnd.setOnClickListener(v -> endTrip());
        fabShareLocation.setOnClickListener(v -> shareLiveLocation());

        // Initialize map
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        initializeMap();
    }

    private void initializeMap() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        } else {
            requestLocationPermission();
        }
    }

    private void enableMyLocation() {
        try {
            if (mMap != null) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
                mMap.getUiSettings().setZoomControlsEnabled(true);
                mMap.getUiSettings().setCompassEnabled(true);
                getLastLocation();
            }
        } catch (SecurityException e) {
            requestLocationPermission();
        }
    }

    private void startTrip() {
        isTracking = true;
        requireView().findViewById(R.id.fabStartTrip).setVisibility(View.GONE);
        requireView().findViewById(R.id.fabEndTrip).setVisibility(View.VISIBLE);
        fabShareLocation.setVisibility(View.VISIBLE);
        tvTripStatus.setText(R.string.trip_in_progress);
        startLocationUpdates();
    }

    private void endTrip() {
        isTracking = false;
        isSharingLiveLocation = false;
        requireView().findViewById(R.id.fabStartTrip).setVisibility(View.VISIBLE);
        requireView().findViewById(R.id.fabEndTrip).setVisibility(View.GONE);
        fabShareLocation.setVisibility(View.GONE);
        tvTripStatus.setText(String.format(Locale.getDefault(),
                "%s %.2f km",
                getString(R.string.trip_completed),
                calculateDistance()));
        stopLocationUpdates();
    }

    private void shareLiveLocation() {
        if (currentLocationUrl.isEmpty()) {
            Toast.makeText(requireContext(), "Location not available yet", Toast.LENGTH_SHORT).show();
            return;
        }

        isSharingLiveLocation = !isSharingLiveLocation;

        if (isSharingLiveLocation) {
            fabShareLocation.setImageResource(R.drawable.end);

            // Create sharing intent
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "I'm sharing my live location with you: " + currentLocationUrl);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Live Location Sharing");

            // Specifically target WhatsApp if installed
            try {
                shareIntent.setPackage("com.whatsapp");
                startActivity(shareIntent);
            } catch (android.content.ActivityNotFoundException ex) {
                // WhatsApp not installed, fallback to generic chooser
                shareIntent.setPackage(null);
                startActivity(Intent.createChooser(shareIntent, "Share Live Location"));
            }

            Toast.makeText(requireContext(), "Live location sharing started", Toast.LENGTH_SHORT).show();
        } else {
            fabShareLocation.setImageResource(R.drawable.shareloc);
            Toast.makeText(requireContext(), "Live location sharing stopped", Toast.LENGTH_SHORT).show();
        }
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(2000);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) return;
                for (Location location : locationResult.getLocations()) {
                    updateLocation(location);
                }
            }
        };

        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }

    private void updateLocation(@NonNull Location location) {
        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

        // Update the location URL for sharing
        currentLocationUrl = String.format(Locale.getDefault(),
                "https://www.google.com/maps/search/?api=1&query=%.6f,%.6f",
                location.getLatitude(), location.getLongitude());

        if (startPoint == null) {
            startPoint = currentLatLng;
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(startPoint, 15));
        }

        if (currentMarker != null) {
            currentMarker.remove();
        }
        currentMarker = mMap.addMarker(new MarkerOptions()
                .position(currentLatLng)
                .title(getString(R.string.current_location)));

        routePoints.add(currentLatLng);
        updateRoute();

        tvLocationInfo.setText(String.format(Locale.getDefault(),
                "Lat: %.6f, Lng: %.6f | Accuracy: %.1fm",
                location.getLatitude(), location.getLongitude(), location.getAccuracy()));
    }

    private void updateRoute() {
        if (routePolyline != null) {
            routePolyline.remove();
        }

        if (routePoints.size() > 1) {
            PolylineOptions options = new PolylineOptions()
                    .addAll(routePoints)
                    .width(10f)
                    .color(0xFF4285F4);
            routePolyline = mMap.addPolyline(options);
        }
    }

    private float calculateDistance() {
        if (routePoints.size() < 2) return 0;

        float totalDistance = 0;
        for (int i = 1; i < routePoints.size(); i++) {
            float[] results = new float[1];
            Location.distanceBetween(
                    routePoints.get(i-1).latitude, routePoints.get(i-1).longitude,
                    routePoints.get(i).latitude, routePoints.get(i).longitude,
                    results);
            totalDistance += results[0];
        }
        return totalDistance / 1000;
    }

    private void getLastLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(requireActivity(), location -> {
                        if (location != null) {
                            updateLocation(location);
                        }
                    });
        }
    }

    private void stopLocationUpdates() {
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    private void requestLocationPermission() {
        locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
    }
}