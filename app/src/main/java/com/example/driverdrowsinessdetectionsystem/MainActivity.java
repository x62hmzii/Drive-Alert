package com.example.driverdrowsinessdetectionsystem;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import com.google.android.material.navigation.NavigationView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends FragmentActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 2;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String PROFILE_IMAGE_NAME = "profile_image.jpg";

    private FrameLayout frame;
    private DrawerLayout drawer;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private ImageView profileImageView;
    private Drawable defaultProfileDrawable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if user is logged in, if not redirect to login
        if (!MyPreferences.isLoggedIn(this)) {
            startActivity(new Intent(this, LoginSucess.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        // Initialize views
        toolbar = findViewById(R.id.toolbar);
        frame = findViewById(R.id.frame);
        drawer = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        // Set up the toolbar
        toolbar.setTitle("Drive Alert");

        // Set up the ActionBarDrawerToggle
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        // Set the NavigationItemSelectedListener
        navigationView.setNavigationItemSelectedListener(this);

        // Initialize profile image view and click listener
        View headerView = navigationView.getHeaderView(0);
        profileImageView = headerView.findViewById(R.id.imageView);
        defaultProfileDrawable = profileImageView.getDrawable();

        // Load saved profile picture if exists
        loadProfilePicture();

        profileImageView.setOnClickListener(v -> showImageSelectionDialog());

        // Check and request permissions
        checkPermissions();

        // Update the drawer header with username and email
        updateDrawerHeader();

        // Load the default fragment (MonitorMenu)
        getSupportFragmentManager().beginTransaction().replace(R.id.frame, new MonitorMenu()).commit();

        // Check if it's the first time and show the HelpActivity
        boolean isFirstTime = MyPreferences.isFirst(MainActivity.this);
        if (isFirstTime) {
            Intent help = new Intent(MainActivity.this, HelpActivity.class);
            startActivity(help);
        }
    }

    private void showImageSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Profile Picture");
        builder.setItems(new CharSequence[]{"Take Photo", "Choose from Gallery", "Remove Photo"},
                (dialog, which) -> {
                    switch (which) {
                        case 0: // Take Photo
                            dispatchTakePictureIntent();
                            break;
                        case 1: // Choose from Gallery
                            openGallery();
                            break;
                        case 2: // Remove Photo
                            resetProfilePicture();
                            break;
                    }
                });
        builder.show();
    }

    private void openGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private void resetProfilePicture() {
        profileImageView.setImageDrawable(defaultProfileDrawable);
        deleteProfileImage();
        Toast.makeText(this, "Profile picture removed", Toast.LENGTH_SHORT).show();
    }

    private void loadProfilePicture() {
        File imageFile = new File(getFilesDir(), PROFILE_IMAGE_NAME);
        if (imageFile.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            profileImageView.setImageBitmap(bitmap);
        }
    }

    private void saveProfileImage(Bitmap bitmap) {
        try {
            FileOutputStream fos = openFileOutput(PROFILE_IMAGE_NAME, MODE_PRIVATE);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteProfileImage() {
        File imageFile = new File(getFilesDir(), PROFILE_IMAGE_NAME);
        if (imageFile.exists()) {
            imageFile.delete();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST && data != null && data.getData() != null) {
                // Handle gallery image selection
                Uri imageUri = data.getData();
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                    profileImageView.setImageBitmap(bitmap);
                    saveProfileImage(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == REQUEST_IMAGE_CAPTURE && data != null) {
                // Handle camera image capture
                Bundle extras = data.getExtras();
                Bitmap imageBitmap = (Bitmap) extras.get("data");
                profileImageView.setImageBitmap(imageBitmap);
                saveProfileImage(imageBitmap);
            }
        }
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.CAMERA
                        },
                        PERMISSION_REQUEST_CODE
                );
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissions granted
            } else {
                Toast.makeText(this, "Permissions are required to change profile picture", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Method to update the drawer header with username and email
    private void updateDrawerHeader() {
        // Retrieve username and email from Intent
        Intent intent = getIntent();
        String username = intent.getStringExtra("USERNAME");
        String email = intent.getStringExtra("EMAIL");

        // Update the Navigation Drawer Header
        View headerView = navigationView.getHeaderView(0);
        TextView textViewName = headerView.findViewById(R.id.textViewName);
        TextView textViewEmail = headerView.findViewById(R.id.textViewEmail);

        textViewName.setText(username);
        textViewEmail.setText(email);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frame, new MonitorMenu())
                    .commit();
        } else if (id == R.id.nav_music_player) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frame, new MusicPlayerFragment())
                    .commit();
        } else if (id == R.id.help_page) {
            Intent intent = new Intent(this, HelpActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_settings) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frame, new SettingsFragment())
                    .commit();
        } else if (id == R.id.nav_location) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frame, new LocationFragment())
                    .commit();
        } else if (id == R.id.nav_emergency) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frame, new EmergencyFragment())
                    .commit();
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}