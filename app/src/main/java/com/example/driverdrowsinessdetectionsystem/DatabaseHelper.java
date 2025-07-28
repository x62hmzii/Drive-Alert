package com.example.driverdrowsinessdetectionsystem;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    // Database Name and Version
    private static final String DATABASE_NAME = "UserDatabase.db";
    private static final int DATABASE_VERSION = 2; // Incremented version for schema changes

    // User Table Constants
    public static final String TABLE_NAME = "user_table";
    public static final String COL_1 = "ID"; // Auto-incrementing primary key
    public static final String COL_2 = "NAME";
    public static final String COL_3 = "EMAIL";
    public static final String COL_4 = "PASSWORD";
    public static final String COL_5 = "OTHER_COLUMN";

    // Music Player Tables (New in version 2)
    public static final String TABLE_MOODS = "moods";
    public static final String COL_MOOD_ID = "mood_id";
    public static final String COL_MOOD_NAME = "mood_name";

    public static final String TABLE_SONGS = "songs";
    public static final String COL_SONG_ID = "song_id";
    public static final String COL_SONG_NAME = "song_name";
    public static final String COL_SONG_PATH = "song_path";
    public static final String COL_SONG_TYPE = "song_type"; // "local" or "online"
    public static final String COL_SONG_URL = "song_url"; // for online songs
    public static final String COL_MOOD_FK = "mood_id"; // Foreign key to moods table

    // emergency DatabaseHelper class
    public static final String TABLE_EMERGENCY_CONTACTS = "emergency_contacts";
    public static final String COL_CONTACT_ID = "contact_id";
    public static final String COL_CONTACT_NAME = "contact_name";
    public static final String COL_CONTACT_NUMBER = "contact_number";
    public static final String COL_IS_PRIMARY = "is_primary";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create user table (original functionality)
        String createUserTable = "CREATE TABLE " + TABLE_NAME + " (" +
                COL_1 + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_2 + " TEXT, " +
                COL_3 + " TEXT, " +
                COL_4 + " TEXT, " +
                COL_5 + " TEXT)";
        db.execSQL(createUserTable);

        // Create moods table (new for music player)
        String createMoodsTable = "CREATE TABLE " + TABLE_MOODS + " (" +
                COL_MOOD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_MOOD_NAME + " TEXT UNIQUE)";
        db.execSQL(createMoodsTable);

        // Create songs table (new for music player)
        String createSongsTable = "CREATE TABLE " + TABLE_SONGS + " (" +
                COL_SONG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_SONG_NAME + " TEXT, " +
                COL_SONG_PATH + " TEXT, " + // Local file path or online query
                COL_SONG_TYPE + " TEXT, " +  // "local" or "online"
                COL_SONG_URL + " TEXT, " +  // URL for online songs
                COL_MOOD_FK + " INTEGER, " + // Foreign key to moods table
                "FOREIGN KEY(" + COL_MOOD_FK + ") REFERENCES " +
                TABLE_MOODS + "(" + COL_MOOD_ID + "))";
        db.execSQL(createSongsTable);

        // Insert default moods (Happy, Sad, Romantic, Energetic)
        insertDefaultMoods(db);
        //emergency
        String createEmergencyContactsTable = "CREATE TABLE " + TABLE_EMERGENCY_CONTACTS + " (" +
                COL_CONTACT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_CONTACT_NAME + " TEXT, " +
                COL_CONTACT_NUMBER + " TEXT, " +
                COL_IS_PRIMARY + " INTEGER DEFAULT 0)";
        db.execSQL(createEmergencyContactsTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Handle database upgrades while preserving user data
        if (oldVersion < 2) {
            // Create new tables for music player
            String createMoodsTable = "CREATE TABLE " + TABLE_MOODS + " (" +
                    COL_MOOD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_MOOD_NAME + " TEXT UNIQUE)";
            db.execSQL(createMoodsTable);

            String createSongsTable = "CREATE TABLE " + TABLE_SONGS + " (" +
                    COL_SONG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_SONG_NAME + " TEXT, " +
                    COL_SONG_PATH + " TEXT, " +
                    COL_SONG_TYPE + " TEXT, " +
                    COL_SONG_URL + " TEXT, " +
                    COL_MOOD_FK + " INTEGER, " +
                    "FOREIGN KEY(" + COL_MOOD_FK + ") REFERENCES " +
                    TABLE_MOODS + "(" + COL_MOOD_ID + "))";
            db.execSQL(createSongsTable);

            insertDefaultMoods(db);
        }if (oldVersion < 3) {  // Increment your version to 3
            String createEmergencyContactsTable = "CREATE TABLE " + TABLE_EMERGENCY_CONTACTS + " (" +
                    COL_CONTACT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_CONTACT_NAME + " TEXT, " +
                    COL_CONTACT_NUMBER + " TEXT, " +
                    COL_IS_PRIMARY + " INTEGER DEFAULT 0)";
            db.execSQL(createEmergencyContactsTable);
        }
    }

    /**
     * Inserts default moods into the database
     * @param db The SQLiteDatabase instance
     */
    private void insertDefaultMoods(SQLiteDatabase db) {
        String[] defaultMoods = {"Happy", "Sad", "Romantic", "Energetic"};
        for (String mood : defaultMoods) {
            ContentValues values = new ContentValues();
            values.put(COL_MOOD_NAME, mood);
            db.insert(TABLE_MOODS, null, values);
        }
    }

    // Original user management methods
    public boolean insertData(String name, String email, String password, String other) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_2, name);
        contentValues.put(COL_3, email);
        contentValues.put(COL_4, password);
        contentValues.put(COL_5, other);

        long result = db.insert(TABLE_NAME, null, contentValues);
        return result != -1;
    }

    public Cursor getUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE " +
                COL_3 + "=? AND " + COL_4 + "=?", new String[]{email, password});
    }

    // New methods for music player functionality

    /**
     * Adds a song to the specified mood category
     * @param moodName The name of the mood category
     * @param songName The name of the song
     * @param path The file path or online query
     * @param type "local" or "online"
     * @param url The URL for online songs (optional for local songs)
     * @return row ID of the newly inserted song, or -1 if error
     */
    public long addSongToMood(String moodName, String songName, String path,
                              String type, String url) {
        SQLiteDatabase db = this.getWritableDatabase();

        // First get the mood ID
        Cursor cursor = db.query(TABLE_MOODS,
                new String[]{COL_MOOD_ID},
                COL_MOOD_NAME + "=?",
                new String[]{moodName},
                null, null, null);

        if (cursor.moveToFirst()) {
            long moodId = cursor.getLong(0);
            cursor.close();

            ContentValues values = new ContentValues();
            values.put(COL_SONG_NAME, songName);
            values.put(COL_SONG_PATH, path);
            values.put(COL_SONG_TYPE, type);
            values.put(COL_SONG_URL, url);
            values.put(COL_MOOD_FK, moodId);

            return db.insert(TABLE_SONGS, null, values);
        }
        cursor.close();
        return -1;
    }

    /**
     * Gets all songs for a specific mood
     * @param moodName The name of the mood category
     * @return Cursor containing all songs for the mood
     */
    public Cursor getSongsByMood(String moodName) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT s.* FROM " + TABLE_SONGS + " s " +
                        "JOIN " + TABLE_MOODS + " m ON s." + COL_MOOD_FK + "=m." + COL_MOOD_ID + " " +
                        "WHERE m." + COL_MOOD_NAME + "=?",
                new String[]{moodName});
    }

    /**
     * Deletes a song from the database
     * @param songId The ID of the song to delete
     * @return true if deletion was successful, false otherwise
     */
    public boolean deleteSong(long songId) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_SONGS, COL_SONG_ID + "=?",
                new String[]{String.valueOf(songId)}) > 0;
    }

    /**
     * Gets all available mood categories
     * @return Cursor containing all moods
     */
    public Cursor getAllMoods() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_MOODS,
                new String[]{COL_MOOD_ID, COL_MOOD_NAME},
                null, null, null, null, null);
    }

    /**
     * Gets song details by song ID
     * @param songId The ID of the song
     * @return Cursor containing the song details
     */
    public Cursor getSongById(long songId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_SONGS,
                new String[]{COL_SONG_ID, COL_SONG_NAME, COL_SONG_PATH,
                        COL_SONG_TYPE, COL_SONG_URL, COL_MOOD_FK},
                COL_SONG_ID + "=?",
                new String[]{String.valueOf(songId)},
                null, null, null);
    }
    //here i changed
    public boolean updatePassword(String email, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_4, newPassword);

        int result = db.update(TABLE_NAME, contentValues, COL_3 + " = ?", new String[]{email});
        return result > 0;
    }
    // emergency new methods to DatabaseHelper
    public long addEmergencyContact(String name, String number, boolean isPrimary) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_CONTACT_NAME, name);
        values.put(COL_CONTACT_NUMBER, number);
        values.put(COL_IS_PRIMARY, isPrimary ? 1 : 0);

        return db.insert(TABLE_EMERGENCY_CONTACTS, null, values);
    }

    public Cursor getAllEmergencyContacts() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_EMERGENCY_CONTACTS,
                new String[]{COL_CONTACT_ID, COL_CONTACT_NAME, COL_CONTACT_NUMBER, COL_IS_PRIMARY},
                null, null, null, null, null);
    }

    public Cursor getPrimaryEmergencyContact() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_EMERGENCY_CONTACTS,
                new String[]{COL_CONTACT_ID, COL_CONTACT_NAME, COL_CONTACT_NUMBER, COL_IS_PRIMARY},
                COL_IS_PRIMARY + "=1", null, null, null, null);
    }

    public boolean updateEmergencyContact(long id, String name, String number, boolean isPrimary) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_CONTACT_NAME, name);
        values.put(COL_CONTACT_NUMBER, number);
        values.put(COL_IS_PRIMARY, isPrimary ? 1 : 0);

        return db.update(TABLE_EMERGENCY_CONTACTS, values, COL_CONTACT_ID + "=?",
                new String[]{String.valueOf(id)}) > 0;
    }

    public boolean deleteEmergencyContact(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_EMERGENCY_CONTACTS, COL_CONTACT_ID + "=?",
                new String[]{String.valueOf(id)}) > 0;
    }
}