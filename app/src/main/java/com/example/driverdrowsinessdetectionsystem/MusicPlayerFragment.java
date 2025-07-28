package com.example.driverdrowsinessdetectionsystem;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.airbnb.lottie.LottieAnimationView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;

public class MusicPlayerFragment extends Fragment implements TextToSpeech.OnInitListener {

    // UI Components
    private ImageButton btnAssistant;
    private LottieAnimationView assistantAnimation;
    private TextView tvAssistantStatus;
    private ListView lvPlaylist;
    private Button btnAddSong;
    private AlertDialog currentAddSongDialog;
    private EditText currentEtSongPath;
    private EditText currentEtSongName;

    // Music Player Components
    private MediaPlayer mediaPlayer;
    private ArrayAdapter<String> playlistAdapter;
    private final ArrayList<String> moodsList = new ArrayList<>();
    private String currentMood = "";
    private boolean isOnlineMode = false;
    private Button btnStop;
    private Button btnPlayPause;
    private TextView tvNowPlaying;
    private String currentPlayingSongName = "";

    // AI Assistant Components
    private TextToSpeech textToSpeech;
    private boolean isAssistantSpeaking = false;
    private boolean isListening = false;
    private static final String TTS_UTTERANCE_ID = "AI_ASSISTANT_UTTERANCE";

    // Database Helper
    private DatabaseHelper databaseHelper;

    // Request Codes
    private static final int VOICE_RECOGNITION_REQUEST_CODE = 1001;
    private static final String TAG = "MusicPlayerFragment";

    // Mood mapping with possible user expressions
    private final HashMap<String, String[]> moodExpressions = new HashMap<String, String[]>() {{
        put("Happy", new String[]{"happy", "joyful", "excited", "good", "great", "cool"});
        put("Sad", new String[]{"sad", "depressed", "down", "unhappy", "low", "unloved"});
        put("Romantic", new String[]{"romantic", "love", "loving", "affectionate", "passionate"});
        put("Energetic", new String[]{"energetic", "boostup", "active", "workout", "exercise", "running"});
    }};

    // Activity result launchers
    private final ActivityResultLauncher<Intent> voiceRecognitionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    ArrayList<String> results = result.getData().getStringArrayListExtra(
                            RecognizerIntent.EXTRA_RESULTS);
                    if (results != null && !results.isEmpty()) {
                        String command = results.get(0);
                        processVoiceCommand(command);
                    }
                } else {
                    // User canceled or error occurred
                    isListening = false;
                    tvAssistantStatus.setText(getString(R.string.tap_to_try_again));
                    assistantAnimation.setAnimation(R.raw.ai_assistant_wave);
                }
            });
    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null && currentEtSongPath != null) {
                        currentEtSongPath.setText(uri.toString());

                        // Auto-fill song name if empty
                        if (currentEtSongName != null && currentEtSongName.getText().toString().isEmpty()) {
                            String fileName = getFileName(uri);
                            if (fileName != null) {
                                // Remove file extension
                                int dotIndex = fileName.lastIndexOf('.');
                                if (dotIndex > 0) {
                                    fileName = fileName.substring(0, dotIndex);
                                }
                                currentEtSongName.setText(fileName);
                            }
                        }
                    }
                }
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        databaseHelper = new DatabaseHelper(requireContext());
        initializeTextToSpeech();
    }

    private void initializeTextToSpeech() {
        textToSpeech = new TextToSpeech(requireContext(), this);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(Locale.getDefault());
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Language not supported");
            } else {
                setupTtsListener();
            }
        } else {
            Log.e(TAG, "Initialization failed");
        }
    }

    private void setupTtsListener() {
        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                safeRunOnUiThread(() -> {
                    isAssistantSpeaking = true;
                    assistantAnimation.setAnimation(R.raw.ai_assistant_speak);
                });
            }

            @Override
            public void onDone(String utteranceId) {
                safeRunOnUiThread(() -> {
                    isAssistantSpeaking = false;
                    assistantAnimation.setAnimation(R.raw.ai_assistant_wave);
                    if (isListening) {
                        startVoiceRecognition();
                    }
                });
            }

            @Override
            public void onError(String utteranceId) {
                safeRunOnUiThread(() -> {
                    isAssistantSpeaking = false;
                    assistantAnimation.setAnimation(R.raw.ai_assistant_wave);
                });
            }
        });
    }
    private void safeRunOnUiThread(Runnable action) {
        if (getView() != null && isAdded()) {
            requireActivity().runOnUiThread(action);
        }
    }



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_music_player, container, false);

        btnAssistant = view.findViewById(R.id.btnAssistant);
        tvAssistantStatus = view.findViewById(R.id.tvAssistantStatus);
        assistantAnimation = view.findViewById(R.id.assistantAnimation);
        lvPlaylist = view.findViewById(R.id.lvPlaylist);
        btnAddSong = view.findViewById(R.id.btnAddSong);
        btnStop = view.findViewById(R.id.btnStop);
        btnPlayPause = view.findViewById(R.id.btnPlayPause);
        tvNowPlaying = view.findViewById(R.id.tvNowPlaying);

        initializeMoodsList();
        setupListeners();

        return view;
    }

    private void initializeMoodsList() {
        moodsList.clear();
        try (Cursor cursor = databaseHelper.getAllMoods()) {
            if (cursor != null) {
                int moodNameIndex = cursor.getColumnIndex(DatabaseHelper.COL_MOOD_NAME);
                if (moodNameIndex >= 0) {
                    while (cursor.moveToNext()) {
                        String moodName = cursor.getString(moodNameIndex);
                        if (moodName != null) {
                            moodsList.add(moodName);
                        }
                    }
                } else {
                    Log.e(TAG, "COL_MOOD_NAME column not found in cursor");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing moods list", e);
        }

        playlistAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, moodsList);
        lvPlaylist.setAdapter(playlistAdapter);
    }

    private void setupListeners() {
        btnAssistant.setOnClickListener(v -> activateAssistant());

        lvPlaylist.setOnItemClickListener((parent, view, position, id) -> {
            currentMood = moodsList.get(position);
            showSongsForMood(currentMood);
        });

        btnAddSong.setOnClickListener(v -> {
            if (!currentMood.isEmpty()) {
                showAddSongDialog();
            } else {
                showToast(getString(R.string.select_mood_first));
            }
        });

        btnStop.setOnClickListener(v -> togglePlayback());
    }

    private void activateAssistant() {
        if (isAssistantSpeaking) return;

        // Reset states
        isListening = true;
        isAssistantSpeaking = false;

        assistantAnimation.setAnimation(R.raw.ai_assistant_listen);

        String[] greetings = getResources().getStringArray(R.array.assistant_greetings);
        String greeting = greetings[new Random().nextInt(greetings.length)];

        speak(greeting + " " + getString(R.string.online_offline_prompt), () -> {
            if (isListening) {
                startVoiceRecognition();
            }
        });
    }

    private void speak(String text, Runnable onComplete) {
        if (textToSpeech != null) {
            Bundle params = new Bundle();
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, TTS_UTTERANCE_ID);

            // Set up utterance listener
            textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    getActivity().runOnUiThread(() -> {
                        isAssistantSpeaking = true;
                        assistantAnimation.setAnimation(R.raw.ai_assistant_speak);
                        tvAssistantStatus.setText(text);
                    });
                }

                @Override
                public void onDone(String utteranceId) {
                    getActivity().runOnUiThread(() -> {
                        isAssistantSpeaking = false;
                        assistantAnimation.setAnimation(R.raw.ai_assistant_listen);
                        if (onComplete != null) {
                            onComplete.run();
                        }
                    });
                }

                @Override
                public void onError(String utteranceId) {
                    getActivity().runOnUiThread(() -> {
                        isAssistantSpeaking = false;
                        assistantAnimation.setAnimation(R.raw.ai_assistant_wave);
                    });
                }
            });

            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params, TTS_UTTERANCE_ID);
        }
    }

    private void togglePlayback() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                btnStop.setText(getString(R.string.resume));
                btnPlayPause.setText("▶");
                showToast(getString(R.string.playback_paused));
            } else {
                mediaPlayer.start();
                btnStop.setText(getString(R.string.stop));
                btnPlayPause.setText("⏸");
                showToast(getString(R.string.playback_resumed));
            }
        }
    }

    private void updatePlaybackUI(boolean isPlaying) {
        safeRunOnUiThread(() -> {
            if (isPlaying) {
                btnStop.setVisibility(View.VISIBLE);
                btnPlayPause.setVisibility(View.VISIBLE);
                tvNowPlaying.setVisibility(View.VISIBLE);
                btnStop.setText(getString(R.string.stop));
                btnPlayPause.setText("⏸");
                tvNowPlaying.setText(getString(R.string.now_playing, currentPlayingSongName));
            } else {
                btnStop.setVisibility(View.GONE);
                btnPlayPause.setVisibility(View.GONE);
                tvNowPlaying.setVisibility(View.GONE);
            }
        });
    }

    private void startVoiceRecognition() {
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                    getString(R.string.voice_prompt));
            voiceRecognitionLauncher.launch(intent);
        } catch (Exception e) {
            showToast(getString(R.string.voice_not_supported));
            Log.e(TAG, "Voice recognition error", e);
            isListening = false;
        }
    }

    private void processVoiceCommand(String command) {
        command = command.toLowerCase();

        if (isAssistantSpeaking) return;

        if (command.contains("online") || command.contains("stream") || command.contains("youtube")) {
            isOnlineMode = true;
            speak(getString(R.string.youtube_search_prompt), () -> {
                isListening = true;
                startVoiceRecognition(); // Listen for mood
            });
        }
        else if (command.contains("offline") || command.contains("local") || command.contains("device")) {
            isOnlineMode = false;
            speak(getString(R.string.local_playback_prompt), () -> {
                isListening = true;
                startVoiceRecognition(); // Listen for mood
            });
        }
        else {
            detectMoodFromCommand(command);
        }
    }

    private void detectMoodFromCommand(String command) {
        for (String mood : moodExpressions.keySet()) {
            String[] expressions = moodExpressions.get(mood);
            if (expressions != null) {
                for (String expression : expressions) {
                    if (command.contains(expression)) {
                        currentMood = mood;
                        handleMoodDetectionSuccess(mood);
                        return;
                    }
                }
            }
        }

        // If no mood detected
        speak(getString(R.string.mood_not_understood));
    }

    private void handleMoodDetectionSuccess(String mood) {
        String[] responses = getResources().getStringArray(R.array.mood_responses);
        String response = String.format(responses[new Random().nextInt(responses.length)], mood);

        speak(response, () -> {
            if (isOnlineMode) {
                playYouTubeMoodPlaylist(mood);
            } else {
                showSongsForMood(mood);
            }
            // Reset listening state for next interaction
            isListening = false;
        });
    }

    private void speak(String text) {
        speak(text, null); // Calls the main speak method with null callback
    }

    private void playYouTubeMoodPlaylist(String mood) {
        String query = mood + " " + getString(R.string.music_playlist);
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(query)));
            startActivity(intent);
            speak(String.format(getString(R.string.opening_youtube), mood));
        } catch (Exception e) {
            Log.e(TAG, "Error opening YouTube", e);
            speak(getString(R.string.youtube_open_error));
        }
    }

    private void showSongsForMood(String mood) {
        try (Cursor cursor = databaseHelper.getSongsByMood(mood)) {
            if (cursor != null) {
                ArrayList<String> songNames = new ArrayList<>();
                final ArrayList<Long> songIds = new ArrayList<>();

                int songNameIndex = cursor.getColumnIndex(DatabaseHelper.COL_SONG_NAME);
                int songIdIndex = cursor.getColumnIndex(DatabaseHelper.COL_SONG_ID);

                if (songNameIndex >= 0 && songIdIndex >= 0) {
                    while (cursor.moveToNext()) {
                        String songName = cursor.getString(songNameIndex);
                        long songId = cursor.getLong(songIdIndex);
                        if (songName != null) {
                            songNames.add(songName);
                            songIds.add(songId);
                        }
                    }
                }

                if (songNames.isEmpty()) {
                    showToast(String.format(getString(R.string.no_songs_found), mood));
                    return;
                }

                new AlertDialog.Builder(requireContext())
                        .setTitle(mood + " " + getString(R.string.songs))
                        .setAdapter(
                                new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, songNames),
                                (dialog, which) -> showSongOptions(songIds.get(which))
                        )
                        .setNegativeButton(getString(R.string.back), null)
                        .show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing songs for mood", e);
        }
    }


    private void showSongOptions(final long songId) {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.song_options))
                .setItems(new String[]{
                        getString(R.string.play),
                        getString(R.string.delete)
                }, (dialog, which) -> {
                    if (which == 0) {
                        playSong(songId);
                    } else {
                        confirmDeleteSong(songId);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void playSong(long songId) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        try (Cursor cursor = databaseHelper.getSongById(songId)) {
            if (cursor != null && cursor.moveToFirst()) {
                int typeIndex = cursor.getColumnIndex(DatabaseHelper.COL_SONG_TYPE);
                int pathIndex = cursor.getColumnIndex(DatabaseHelper.COL_SONG_PATH);
                int nameIndex = cursor.getColumnIndex(DatabaseHelper.COL_SONG_NAME);

                if (typeIndex >= 0 && pathIndex >= 0 && nameIndex >= 0) {
                    String type = cursor.getString(typeIndex);
                    String path = cursor.getString(pathIndex);
                    currentPlayingSongName = cursor.getString(nameIndex);

                    if ("local".equals(type)) {
                        playLocalSong(path);
                    } else {
                        playYouTubeSong(path); // path contains URL in this case
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing song", e);
            showToast(getString(R.string.playback_error));
        }
    }

    private void playLocalSong(String filePath) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }

            mediaPlayer = new MediaPlayer();

            if (filePath.startsWith("content://")) {
                mediaPlayer.setDataSource(requireContext(), Uri.parse(filePath));
            } else {
                mediaPlayer.setDataSource(filePath);
            }

            mediaPlayer.prepare();
            mediaPlayer.start();

            // Get the song name for display
            currentPlayingSongName = getFileName(Uri.parse(filePath));
            if (currentPlayingSongName != null && currentPlayingSongName.lastIndexOf('.') > 0) {
                currentPlayingSongName = currentPlayingSongName.substring(0, currentPlayingSongName.lastIndexOf('.'));
            }

            mediaPlayer.setOnCompletionListener(mp -> {
                updatePlaybackUI(false);
                showToast(getString(R.string.playback_completed));
            });

            updatePlaybackUI(true);
            showToast(getString(R.string.now_playing, currentPlayingSongName));
        } catch (IOException e) {
            Log.e(TAG, "Error playing local song", e);
            showToast(getString(R.string.playback_error) + ": " + e.getMessage());
        }
    }

    private void playYouTubeSong(String query) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(query)));
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening YouTube", e);
            showToast(getString(R.string.youtube_open_error));
        }
    }

    private void confirmDeleteSong(final long songId) {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.confirm_delete))
                .setMessage(getString(R.string.delete_song_confirmation))
                .setPositiveButton(getString(R.string.delete), (dialog, which) -> {
                    if (databaseHelper.deleteSong(songId)) {
                        showToast(getString(R.string.song_deleted));
                    } else {
                        showToast(getString(R.string.delete_failed));
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void showAddSongDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_song, null);

        currentEtSongName = dialogView.findViewById(R.id.etSongName);  // Store reference
        RadioGroup rgSongType = dialogView.findViewById(R.id.rgSongType);
        currentEtSongPath = dialogView.findViewById(R.id.etSongPath);  // Store reference
        Button btnBrowse = dialogView.findViewById(R.id.btnBrowse);

        btnBrowse.setOnClickListener(v -> {
            if (checkStoragePermission()) {
                openFilePicker();
            }
        });

        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.add_new_song))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.add), (dialog, which) -> {
                    String name = currentEtSongName.getText().toString();
                    String path = currentEtSongPath.getText().toString();
                    int selectedId = rgSongType.getCheckedRadioButtonId();
                    String type = selectedId == R.id.rbLocal ? "local" : "online";

                    if (!name.isEmpty() && !path.isEmpty()) {
                        long result = databaseHelper.addSongToMood(currentMood, name, path, type,
                                type.equals("online") ? path : null);

                        if (result != -1) {
                            showToast(getString(R.string.song_added));
                        } else {
                            showToast(getString(R.string.add_failed));
                        }
                    } else {
                        showToast(getString(R.string.fill_all_fields));
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    android.Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{android.Manifest.permission.READ_MEDIA_AUDIO},
                        VOICE_RECOGNITION_REQUEST_CODE);
                return false;
            }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                        VOICE_RECOGNITION_REQUEST_CODE);
                return false;
            }
        }
        return true;
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        filePickerLauncher.launch(intent);
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (displayNameIndex != -1) {
                        result = cursor.getString(displayNameIndex);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting file name", e);
            }
        }
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
        }
        return result;
    }

    private void showToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void stopPlayback() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
            updatePlaybackUI(false);
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        stopPlayback();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        isListening = false; // Reset listening state
    }
}