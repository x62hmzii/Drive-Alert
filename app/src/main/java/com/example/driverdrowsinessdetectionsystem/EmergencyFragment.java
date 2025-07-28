package com.example.driverdrowsinessdetectionsystem;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.speech.tts.UtteranceProgressListener;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class EmergencyFragment extends Fragment implements SensorEventListener, TextToSpeech.OnInitListener {

    private static final String TAG = "EmergencyFragment";
    private static final String UTTERANCE_ID = "voice_prompt";
    private static final int PERMISSIONS_REQUEST = 100;
    private static final int SOS_COUNTDOWN = 10000; // 10 seconds

    private static final int MIN_TIME_BETWEEN_SHAKES = 2000; // 2 seconds
    private static final int VOICE_CONFIRMATION_TIMEOUT = 15000; // 15 seconds
    private static final int MAX_VOICE_RETRIES = 2;

    // Sensitivity thresholds
    private float LOW_SENSITIVITY_THRESHOLD = 20f;
    private float MEDIUM_SENSITIVITY_THRESHOLD = 15f;
    private float HIGH_SENSITIVITY_THRESHOLD = 10f;
    private float currentThreshold = HIGH_SENSITIVITY_THRESHOLD;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float[] lastAcceleration = new float[3];
    private float[] currentAcceleration = new float[3];
    private float[] linearAcceleration = new float[3];

    private Button btnSOS, btnLowSensitivity, btnMediumSensitivity, btnHighSensitivity;
    private LottieAnimationView animationView;
    private Vibrator vibrator;
    private ToneGenerator toneGenerator;
    private FusedLocationProviderClient fusedLocationClient;
    private boolean isAccidentDetectionEnabled = true;
    private CountDownTimer sosCountdownTimer;
    private long lastShakeTime = 0;
    private CardView statusCard;
    private TextView tvEmergencyStatus, tvDetectionStatus, tvVoiceStatus;

    private String emergencyNumber = "112";
    private String emergencyMessage = "Emergency! I may have been in an accident! My current location is: ";

    // Voice recognition
    private SpeechRecognizer speechRecognizer;
    private boolean isListeningForVoiceConfirmation = false;
    private TextToSpeech textToSpeech;
    private int voiceRetryCount = 0;
    private Handler voiceConfirmationHandler;

    // Other components
    private Executor executor;
    private boolean isAppInForeground = true;
    private MediaPlayer voiceAssistantPlayer;
    private boolean isTextToSpeechInitialized = false;
    private boolean permissionsGranted = false;

    public EmergencyFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize handlers and executors
        voiceConfirmationHandler = new Handler(Looper.getMainLooper());
        executor = Executors.newSingleThreadExecutor();

        // Check permissions
        permissionsGranted = checkPermissions();
        if (!permissionsGranted) {
            requestPermissions();
        }

        // Initialize components that can be created here
        vibrator = (Vibrator) requireActivity().getSystemService(Context.VIBRATOR_SERVICE);
        toneGenerator = new ToneGenerator(android.media.AudioManager.STREAM_ALARM, 100);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        textToSpeech = new TextToSpeech(getContext(), this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_emergency, container, false);

        // Initialize UI elements
        btnSOS = view.findViewById(R.id.btn_sos);
        btnLowSensitivity = view.findViewById(R.id.btn_low_sensitivity);
        btnMediumSensitivity = view.findViewById(R.id.btn_medium_sensitivity);
        btnHighSensitivity = view.findViewById(R.id.btn_high_sensitivity);
        animationView = view.findViewById(R.id.animation_view);
        statusCard = view.findViewById(R.id.status_card);
        tvEmergencyStatus = view.findViewById(R.id.tv_emergency_status);
        tvDetectionStatus = view.findViewById(R.id.tv_detection_status);
        tvVoiceStatus = view.findViewById(R.id.tv_voice_status);

        // Load settings
        loadEmergencySettings();
        loadSensitivitySettings();

        // Set up buttons
        setupButtons(view);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Lottie animation
        if (animationView != null) {
            try {
                animationView.setAnimation(R.raw.pulse_animation);
                animationView.playAnimation();
            } catch (Exception e) {
                Log.e(TAG, "Error loading animation", e);
                animationView.setVisibility(View.GONE);
            }
        }

        // Initialize sensor manager
        sensorManager = (SensorManager) requireActivity().getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accelerometer == null) {
                showSnackbar("Accelerometer not available");
                isAccidentDetectionEnabled = false;
                tvEmergencyStatus.setText("Accident detection: Disabled");
            }
        } else {
            showSnackbar("Sensor service not available");
            isAccidentDetectionEnabled = false;
            tvEmergencyStatus.setText("Accident detection: Disabled");
        }

        // Initialize voice recognition
        initVoiceRecognition();
    }

    @Override
    public void onResume() {
        super.onResume();
        isAppInForeground = true;

        // Register sensor listener
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }

        // Update status UI
        updateStatusUI();
    }

    @Override
    public void onPause() {
        super.onPause();
        isAppInForeground = false;

        // Unregister sensor listener
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }

        // Stop any ongoing voice recognition
        stopVoiceRecognition();

        // Cancel any countdown timer
        if (sosCountdownTimer != null) {
            sosCountdownTimer.cancel();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Release resources
        if (toneGenerator != null) {
            toneGenerator.release();
        }
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        if (voiceAssistantPlayer != null) {
            voiceAssistantPlayer.release();
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(Locale.getDefault());
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Language not supported");
            } else {
                isTextToSpeechInitialized = true;
            }
        } else {
            Log.e(TAG, "TTS Initialization failed");
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER && isAccidentDetectionEnabled && isAppInForeground) {
            // Apply low-pass filter to isolate gravity
            final float alpha = 0.8f;

            currentAcceleration[0] = alpha * currentAcceleration[0] + (1 - alpha) * event.values[0];
            currentAcceleration[1] = alpha * currentAcceleration[1] + (1 - alpha) * event.values[1];
            currentAcceleration[2] = alpha * currentAcceleration[2] + (1 - alpha) * event.values[2];

            // Remove gravity contribution to get linear acceleration
            linearAcceleration[0] = event.values[0] - currentAcceleration[0];
            linearAcceleration[1] = event.values[1] - currentAcceleration[1];
            linearAcceleration[2] = event.values[2] - currentAcceleration[2];

            // Calculate acceleration magnitude
            double accelerationMagnitude = Math.sqrt(
                    linearAcceleration[0] * linearAcceleration[0] +
                            linearAcceleration[1] * linearAcceleration[1] +
                            linearAcceleration[2] * linearAcceleration[2]);

            // Check for sudden impact (potential accident)
            long currentTime = System.currentTimeMillis();
            if (accelerationMagnitude > currentThreshold) {
                if (currentTime - lastShakeTime > MIN_TIME_BETWEEN_SHAKES) {
                    lastShakeTime = currentTime;
                    Log.d(TAG, "Significant shake detected! Magnitude: " + accelerationMagnitude);
                    handlePotentialAccident();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }

    private void handlePotentialAccident() {
        if (getActivity() == null || !isAdded()) return;

        requireActivity().runOnUiThread(() -> {
            // Cancel any ongoing SOS countdown
            if (sosCountdownTimer != null) {
                sosCountdownTimer.cancel();
            }

            isAccidentDetectionEnabled = false;
            statusCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorWarning));
            tvEmergencyStatus.setText("Possible accident detected!");

            vibratePattern();
            if (toneGenerator != null) {
                toneGenerator.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 1000);
            }

            // Use voice confirmation
            startVoiceRecognition();
        });
    }

    private void setupButtons(View view) {
        // SOS button
        btnSOS.setOnClickListener(v -> activateSOS());
        btnSOS.setOnLongClickListener(v -> {
            showCancelSOSDialog();
            return true;
        });

        // Sensitivity buttons
        btnLowSensitivity.setOnClickListener(v -> setSensitivityLevel(1));
        btnMediumSensitivity.setOnClickListener(v -> setSensitivityLevel(2));
        btnHighSensitivity.setOnClickListener(v -> setSensitivityLevel(3));

        // Settings button
        FloatingActionButton btnSettings = view.findViewById(R.id.btn_emergency_settings);
        btnSettings.setOnClickListener(v -> showSettingsDialog());
    }

    private void activateSOS() {
        if (getActivity() == null || !isAdded()) return;

        try {
            // Set visual feedback
            animationView.setAnimation(R.raw.countdown_animation);
            animationView.playAnimation();
            statusCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorWarning));
            tvEmergencyStatus.setText("Emergency alert in 10 seconds");

            // Audio and haptic feedback
            vibratePattern();
            if (toneGenerator != null) {
                toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 500);
            }
            speak("Emergency alert will be sent in 10 seconds. Shake device to cancel.");

            // Cancel any existing timer
            if (sosCountdownTimer != null) {
                sosCountdownTimer.cancel();
            }

            // Start countdown
            sosCountdownTimer = new CountDownTimer(SOS_COUNTDOWN, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    if (getActivity() == null || !isAdded()) {
                        this.cancel();
                        return;
                    }
                    speak("" + (millisUntilFinished / 1000));
                    vibratePattern();
                }

                @Override
                public void onFinish() {
                    if (getActivity() == null || !isAdded()) return;
                    sendEmergencyAlert();
                }
            }.start();
        } catch (Exception e) {
            Log.e(TAG, "Error activating SOS", e);
            showSnackbar("Error activating emergency system");
        }
    }

    private void showCancelSOSDialog() {
        if (getActivity() == null || !isAdded()) return;

        if (sosCountdownTimer != null) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Cancel Emergency Alert")
                    .setMessage("Are you sure you want to cancel the emergency alert?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        sosCountdownTimer.cancel();
                        resetSOSState();
                        speak("Emergency alert canceled");
                    })
                    .setNegativeButton("No", null)
                    .show();
        }
    }

    private void resetSOSState() {
        if (getActivity() == null || !isAdded()) return;

        statusCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary));
        animationView.setAnimation(R.raw.pulse_animation);
        animationView.playAnimation();
        tvEmergencyStatus.setText("Accident detection: Active");
        isAccidentDetectionEnabled = true;
    }

    private void sendEmergencyAlert() {
        if (getActivity() == null || !isAdded()) return;

        try {
            stopVoiceRecognition();

            // Play emergency sound
            try {
                if (voiceAssistantPlayer != null) {
                    voiceAssistantPlayer.release();
                }
                voiceAssistantPlayer = MediaPlayer.create(requireContext(), R.raw.alert);
                if (voiceAssistantPlayer != null) {
                    voiceAssistantPlayer.setLooping(true);
                    voiceAssistantPlayer.start();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error playing alert sound", e);
            }

            // Show visual alert
            animationView.setAnimation(R.raw.emergency_animation);
            animationView.playAnimation();
            statusCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorEmergency));
            tvEmergencyStatus.setText("Sending emergency alert!");

            speak("Sending emergency alert with your location to authorities. Stay calm, help is on the way.");

            // Get location and send alert
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED && fusedLocationClient != null) {
                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(location -> {
                            if (location != null) {
                                Log.d(TAG, "Location retrieved");
                                sendSMSWithLocation(location);
                                makeEmergencyCall();
                            } else {
                                Log.w(TAG, "Location was null");
                                sendSMSWithoutLocation();
                                makeEmergencyCall();
                            }
                            showSnackbar("Emergency alert sent!");
                            new Handler(Looper.getMainLooper()).postDelayed(this::resetAfterEmergency, 5000);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error getting location", e);
                            sendSMSWithoutLocation();
                            makeEmergencyCall();
                            showSnackbar("Emergency alert sent (no location)!");
                            new Handler(Looper.getMainLooper()).postDelayed(this::resetAfterEmergency, 5000);
                        });
            } else {
                sendSMSWithoutLocation();
                makeEmergencyCall();
                showSnackbar("Emergency alert sent (no location)!");
                new Handler(Looper.getMainLooper()).postDelayed(this::resetAfterEmergency, 5000);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending emergency alert", e);
            showSnackbar("Error sending emergency alert");
        }
    }

    private void resetAfterEmergency() {
        if (getActivity() == null || !isAdded()) return;

        if (voiceAssistantPlayer != null && voiceAssistantPlayer.isPlaying()) {
            voiceAssistantPlayer.pause();
        }
        resetSOSState();
    }

    private void sendSMSWithLocation(Location location) {
        if (getActivity() == null) return;

        String message = emergencyMessage +
                "http://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
        sendSMS(emergencyNumber, message);

        // Also send to emergency contacts if available
        SharedPreferences prefs = requireActivity().getSharedPreferences("EmergencyPrefs", Context.MODE_PRIVATE);
        String contacts = prefs.getString("emergencyContacts", "");
        if (!contacts.isEmpty()) {
            for (String number : contacts.split(",")) {
                sendSMS(number.trim(), message);
            }
        }
    }

    private void sendSMSWithoutLocation() {
        if (getActivity() == null) return;

        sendSMS(emergencyNumber, emergencyMessage + "[Location unavailable]");

        // Also send to emergency contacts if available
        SharedPreferences prefs = requireActivity().getSharedPreferences("EmergencyPrefs", Context.MODE_PRIVATE);
        String contacts = prefs.getString("emergencyContacts", "");
        if (!contacts.isEmpty()) {
            for (String number : contacts.split(",")) {
                sendSMS(number.trim(), emergencyMessage + "[Location unavailable]");
            }
        }
    }

    private void sendSMS(String phoneNumber, String message) {
        if (getActivity() == null) return;

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED) {
            try {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(phoneNumber, null, message, null, null);
                Log.i(TAG, "SMS sent to " + phoneNumber);
            } catch (Exception e) {
                Log.e(TAG, "Failed to send SMS", e);
                showSnackbar("Failed to send SMS");
            }
        } else {
            Log.e(TAG, "No permission to send SMS");
            showSnackbar("No permission to send SMS");
        }
    }

    private void makeEmergencyCall() {
        if (getActivity() == null) return;

        try {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + emergencyNumber));
            if (ActivityCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                startActivity(callIntent);
            } else {
                Log.e(TAG, "No permission to make calls");
                showSnackbar("No permission to make calls");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to make emergency call", e);
            showSnackbar("Failed to make emergency call");
        }
    }

    private void vibratePattern() {
        if (vibrator == null) return;

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(
                        new long[]{0, 200, 200, 200, 200, 200},
                        new int[]{0, 255, 0, 255, 0, 255}, -1));
            } else {
                vibrator.vibrate(500);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error vibrating", e);
        }
    }

    private void initVoiceRecognition() {
        if (getActivity() == null) return;

        if (SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext());
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    if (getActivity() == null || !isAdded()) return;

                    statusCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorListening));
                    tvVoiceStatus.setVisibility(View.VISIBLE);
                    tvVoiceStatus.setText("Listening...");

                    try {
                        animationView.setAnimation(R.raw.voice_listening_animation);
                        animationView.playAnimation();
                    } catch (Exception e) {
                        Log.e(TAG, "Animation error", e);
                    }
                }

                @Override
                public void onBeginningOfSpeech() {}

                @Override
                public void onRmsChanged(float rmsdB) {}

                @Override
                public void onBufferReceived(byte[] buffer) {}

                @Override
                public void onEndOfSpeech() {
                    if (getActivity() == null || !isAdded()) return;

                    try {
                        animationView.setAnimation(R.raw.processing_animation);
                        animationView.playAnimation();
                    } catch (Exception e) {
                        Log.e(TAG, "Animation error", e);
                    }
                }

                @Override
                public void onError(int error) {
                    if (getActivity() == null || !isAdded()) return;
                    handleVoiceRecognitionError(error);
                }

                @Override
                public void onResults(Bundle results) {
                    if (getActivity() == null || !isAdded()) return;

                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        processVoiceCommand(matches.get(0));
                    } else {
                        retryVoiceConfirmation();
                    }
                    resetVoiceRecognitionState();
                }

                @Override
                public void onPartialResults(Bundle partialResults) {}

                @Override
                public void onEvent(int eventType, Bundle params) {}
            });
        } else {
            Log.e(TAG, "Speech recognition not available on this device");
            showSnackbar("Speech recognition not available on this device");
        }
    }

    private void startVoiceRecognition() {
        if (getActivity() == null || !isAdded()) return;

        if (speechRecognizer != null && !isListeningForVoiceConfirmation) {
            try {
                voiceRetryCount = 0;
                isListeningForVoiceConfirmation = true;

                // Start timeout handler
                voiceConfirmationHandler.postDelayed(() -> {
                    if (isListeningForVoiceConfirmation) {
                        Log.w(TAG, "Voice confirmation timeout reached. Proceeding with emergency alert.");
                        stopVoiceRecognition();
                        sendEmergencyAlert();
                    }
                }, VOICE_CONFIRMATION_TIMEOUT);

                // Speak the prompt and listening will start after TTS is done in the speak() method
                Log.d(TAG, "startVoiceRecognition: Speaking confirmation prompt.");
                speak("Possible accident detected! Please say 'Yes' to confirm emergency or 'No' to cancel. You have 15 seconds to respond.");

                // The actual listening is now exclusively started in the onDone() of the speak() method

            } catch (Exception e) {
                Log.e(TAG, "Error starting voice recognition sequence", e);
                retryVoiceConfirmation();
            }
        } else {
            Log.d(TAG, "startVoiceRecognition: SpeechRecognizer is null or already listening.");
        }
    }
    private void startListeningForResponse() {
        if (getActivity() == null || !isAdded() || speechRecognizer == null || !isListeningForVoiceConfirmation) {
            Log.w(TAG, "startListeningForResponse: Conditions not met to start listening.");
            return;
        }

        Log.d(TAG, "startListeningForResponse: Attempting to start speech recognition.");
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say 'Yes' to confirm or 'No' to cancel"); // Optional prompt for the recognizer
            speechRecognizer.startListening(intent);
            Log.d(TAG, "startListeningForResponse: Speech recognition started.");
        } catch (Exception e) {
            Log.e(TAG, "Error starting speech recognition for response", e);
            retryVoiceConfirmation(); // Handle potential errors when starting
        }
    }

    private void processVoiceCommand(String command) {
        if (command == null) {
            retryVoiceConfirmation();
            return;
        }

        String lowerCommand = command.toLowerCase();
        if (lowerCommand.contains("yes") || lowerCommand.contains("send") || lowerCommand.contains("confirm")) {
            sendEmergencyAlert();
        } else if (lowerCommand.contains("no") || lowerCommand.contains("cancel") || lowerCommand.contains("false")) {
            resetAfterFalseAlarm();
        } else {
            retryVoiceConfirmation();
        }
    }

    private void resetAfterFalseAlarm() {
        if (getActivity() == null || !isAdded()) return;

        isAccidentDetectionEnabled = true;
        statusCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary));
        animationView.setAnimation(R.raw.pulse_animation);
        animationView.playAnimation();
        tvEmergencyStatus.setText("Accident detection: Active");
        tvVoiceStatus.setVisibility(View.GONE);

        speak("Emergency canceled. Detection system reactivated.");
        showSnackbar("Emergency canceled. Detection system reactivated.");
    }

    private void handleVoiceRecognitionError(int error) {
        String errorMessage;
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:
                errorMessage = "Audio recording error";
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                errorMessage = "Client side error";
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                errorMessage = "Insufficient permissions";
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                errorMessage = "Network error";
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                errorMessage = "Network timeout";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                errorMessage = "No match found";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                errorMessage = "RecognitionService busy";
                break;
            case SpeechRecognizer.ERROR_SERVER:
                errorMessage = "Server error";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                errorMessage = "No speech input";
                break;
            default:
                errorMessage = "Unknown error";
        }

        Log.e(TAG, "Voice Recognition Error: " + errorMessage);
        retryVoiceConfirmation();
    }

    private void retryVoiceConfirmation() {
        if (getActivity() == null || !isAdded()) return;

        if (voiceRetryCount < MAX_VOICE_RETRIES) {
            voiceRetryCount++;
            speak("I didn't understand. Please say 'Yes' to confirm emergency or 'No' to cancel.");
            new Handler(Looper.getMainLooper()).postDelayed(this::startVoiceRecognition, 2000);
        } else {
            sendEmergencyAlert();
        }
    }

    private void stopVoiceRecognition() {
        if (speechRecognizer != null && isListeningForVoiceConfirmation) {
            try {
                speechRecognizer.stopListening();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping voice recognition", e);
            }
            isListeningForVoiceConfirmation = false;
            voiceConfirmationHandler.removeCallbacksAndMessages(null);
        }
    }

    private void resetVoiceRecognitionState() {
        isListeningForVoiceConfirmation = false;
        voiceConfirmationHandler.removeCallbacksAndMessages(null);
        tvVoiceStatus.setVisibility(View.GONE);
    }

    private void speak(String text) {
        if (textToSpeech != null && isTextToSpeechInitialized) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID); // Use the defined UTTERANCE_ID
            Log.d(TAG, "speak: TTS started with text: " + text + ", utteranceId: " + UTTERANCE_ID);

            textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    // TTS started speaking
                    Log.d(TAG, "TTS onStart: " + utteranceId);
                }

                @Override
                public void onDone(String utteranceId) {
                    // TTS finished speaking, now start listening
                    Log.d(TAG, "TTS onDone: " + utteranceId + ", isListeningForVoiceConfirmation: " + isListeningForVoiceConfirmation);
                    if (utteranceId.equals(UTTERANCE_ID) && isListeningForVoiceConfirmation) {
                        Log.d(TAG, "TTS done, starting voice recognition with a delay.");
                        new Handler(Looper.getMainLooper()).postDelayed(EmergencyFragment.this::startListeningForResponse, 500); // Small delay
                    }
                }

                @Override
                public void onError(String utteranceId) {
                    // Error with TTS
                    Log.e(TAG, "TTS onError: " + utteranceId);
                    // Consider starting listening even on error as a fallback (with a delay)
                    if (isListeningForVoiceConfirmation) {
                        Log.w(TAG, "TTS error, attempting to start voice recognition with a delay as fallback.");
                        new Handler(Looper.getMainLooper()).postDelayed(EmergencyFragment.this::startListeningForResponse, 1000);
                    }
                }
            });
        } else {
            Log.w(TAG, "speak: TextToSpeech not initialized or is null. Cannot speak: " + text);
            // If TTS isn't working, maybe try to start listening immediately? (Consider this carefully)
            if (isListeningForVoiceConfirmation && speechRecognizer != null) {
                Log.w(TAG, "speak: TTS not available, attempting to start listening immediately.");
                new Handler(Looper.getMainLooper()).postDelayed(EmergencyFragment.this::startListeningForResponse, 500);
            }
        }
    }

    private void setSensitivityLevel(int level) {
        switch (level) {
            case 1: // Low
                currentThreshold = LOW_SENSITIVITY_THRESHOLD;
                btnLowSensitivity.setBackgroundResource(R.drawable.btn_sensitivity_bg_selected);
                btnMediumSensitivity.setBackgroundResource(R.drawable.btn_sensitivity_bg);
                btnHighSensitivity.setBackgroundResource(R.drawable.btn_sensitivity_bg);
                speak("Low sensitivity set. System will detect only severe impacts.");
                tvDetectionStatus.setText("Detection Sensitivity: Low");
                break;
            case 2: // Medium
                currentThreshold = MEDIUM_SENSITIVITY_THRESHOLD;
                btnLowSensitivity.setBackgroundResource(R.drawable.btn_sensitivity_bg);
                btnMediumSensitivity.setBackgroundResource(R.drawable.btn_sensitivity_bg_selected);
                btnHighSensitivity.setBackgroundResource(R.drawable.btn_sensitivity_bg);
                speak("Medium sensitivity set. System will detect moderate impacts.");
                tvDetectionStatus.setText("Detection Sensitivity: Medium");
                break;
            case 3: // High
                currentThreshold = HIGH_SENSITIVITY_THRESHOLD;
                btnLowSensitivity.setBackgroundResource(R.drawable.btn_sensitivity_bg);
                btnMediumSensitivity.setBackgroundResource(R.drawable.btn_sensitivity_bg);
                btnHighSensitivity.setBackgroundResource(R.drawable.btn_sensitivity_bg_selected);
                speak("High sensitivity set. System will detect even minor impacts.");
                tvDetectionStatus.setText("Detection Sensitivity: High");
                break;
        }

        saveSensitivitySettings(level);
    }

    private void saveSensitivitySettings(int level) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("EmergencyPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("sensitivityLevel", level);
        editor.apply();
    }

    private void loadSensitivitySettings() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("EmergencyPrefs", Context.MODE_PRIVATE);
        int level = prefs.getInt("sensitivityLevel", 3); // Default to high sensitivity
        setSensitivityLevel(level);
    }

    private void saveEmergencySettings() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("EmergencyPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("emergencyNumber", emergencyNumber);
        editor.putString("emergencyMessage", emergencyMessage);
        editor.apply();
    }

    private void loadEmergencySettings() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("EmergencyPrefs", Context.MODE_PRIVATE);
        emergencyNumber = prefs.getString("emergencyNumber", "112");
        emergencyMessage = prefs.getString("emergencyMessage",
                "Emergency! I may have been in an accident! My current location is: ");
    }

    private void showSettingsDialog() {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Emergency Settings");

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_emergency_settings, null);
        builder.setView(dialogView);

        EditText etEmergencyNumber = dialogView.findViewById(R.id.et_emergency_number);
        EditText etEmergencyMessage = dialogView.findViewById(R.id.et_emergency_message);

        SharedPreferences prefs = getContext().getSharedPreferences("EmergencyPrefs", Context.MODE_PRIVATE);
        etEmergencyNumber.setText(prefs.getString("emergencyNumber", "112"));
        etEmergencyMessage.setText(prefs.getString("emergencyMessage",
                "Emergency! I may have been in an accident! My current location is: "));

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newNumber = etEmergencyNumber.getText().toString().trim();
            String newMessage = etEmergencyMessage.getText().toString().trim();

            if (newNumber.isEmpty()) {
                showSnackbar("Emergency number cannot be empty");
                return;
            }

            emergencyNumber = newNumber;
            emergencyMessage = newMessage;
            saveEmergencySettings();
            showSnackbar("Settings saved");
        });

        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private boolean checkPermissions() {
        List<String> requiredPermissions = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(Manifest.permission.CALL_PHONE);
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(Manifest.permission.SEND_SMS);
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(Manifest.permission.RECORD_AUDIO);
        }
        return requiredPermissions.isEmpty();
    }

    private void requestPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CALL_PHONE);
        }
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.SEND_SMS);
        }
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO);
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(requireActivity(),
                    permissionsToRequest.toArray(new String[0]), PERMISSIONS_REQUEST);
        }
    }

    private void showSnackbar(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
        }
    }

    private void updateStatusUI() {
        if (getActivity() == null || !isAdded()) return;

        tvEmergencyStatus.setText(isAccidentDetectionEnabled ?
                "Accident detection: Active" : "Accident detection: Disabled");

        String sensitivityText = "Detection Sensitivity: ";
        if (currentThreshold == LOW_SENSITIVITY_THRESHOLD) {
            sensitivityText += "Low";
        } else if (currentThreshold == MEDIUM_SENSITIVITY_THRESHOLD) {
            sensitivityText += "Medium";
        } else {
            sensitivityText += "High";
        }
        tvDetectionStatus.setText(sensitivityText);
    }
}