package com.example.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.MedicareApp;
import com.example.data.model.PatientProfile;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Manages Firebase Phone Authentication session, Firestore user document synchronization,
 * and local SharedPreferences / Room SQLite persistence for dynamic profile display.
 */
public class UserSessionManager {

    private static final String TAG = "UserSessionManager";
    private static final String PREF_NAME = "medicare_user_session";

    public static final String KEY_IS_LOGGED_IN = "is_logged_in";
    public static final String KEY_USER_UID = "user_uid";
    public static final String KEY_USER_NAME = "user_name";
    public static final String KEY_USER_PHONE = "user_phone";
    public static final String KEY_USER_PHOTO = "user_photo";
    public static final String KEY_CREATED_AT = "created_at";

    public interface ProfileCallback {
        void onSuccess(String name, String mobileNumber, boolean isFirstLogin);
        void onError(String errorMessage);
    }

    public interface SaveCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    /**
     * Checks if a valid, live Firebase project configuration is present.
     * Returns false if running without google-services or with dummy/missing keys.
     */
    public static boolean isLiveFirebaseConfigured(Context context) {
        try {
            ensureFirebaseInitialized(context);
            if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
                return false;
            }
            com.google.firebase.FirebaseApp app = com.google.firebase.FirebaseApp.getInstance();
            if (app != null && app.getOptions() != null) {
                String apiKey = app.getOptions().getApiKey();
                if (apiKey != null && !apiKey.trim().isEmpty()) {
                    String lower = apiKey.toLowerCase(java.util.Locale.ROOT);
                    if (!lower.contains("dummy") && !lower.contains("placeholder") && !lower.contains("my_api_key") && apiKey.length() >= 25) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    /**
     * Ensures FirebaseApp is safely initialized to prevent IllegalStateException if resources exist.
     */
    public static void ensureFirebaseInitialized(Context context) {
        try {
            if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
                com.google.firebase.FirebaseApp.initializeApp(context.getApplicationContext());
            }
        } catch (Throwable t) {
            Log.d(TAG, "Firebase default initialization skipped: " + t.getMessage());
        }
    }

    /**
     * Checks if the user is currently authenticated via Firebase or local session.
     */
    public static boolean isLoggedIn(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean localLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false);
        if (localLoggedIn) return true;

        if (isLiveFirebaseConfigured(context)) {
            try {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                return currentUser != null;
            } catch (Throwable t) {
                Log.w(TAG, "isLoggedIn check error: " + t.getMessage());
            }
        }
        return false;
    }

    /**
     * Retrieves the current logged-in user name.
     */
    public static String getUserName(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String name = prefs.getString(KEY_USER_NAME, "");
        if (name != null && !name.trim().isEmpty()) {
            return name.trim();
        }

        if (isLiveFirebaseConfigured(context)) {
            try {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null && user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()) {
                    return user.getDisplayName().trim();
                }
            } catch (Throwable t) {
                Log.w(TAG, "getUserName Firebase error: " + t.getMessage());
            }
        }
        return "Patient";
    }

    /**
     * Retrieves the current logged-in mobile number.
     */
    public static String getMobileNumber(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String phone = prefs.getString(KEY_USER_PHONE, "");
        if (phone != null && !phone.trim().isEmpty()) {
            return phone.trim();
        }

        if (isLiveFirebaseConfigured(context)) {
            try {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null && user.getPhoneNumber() != null) {
                    return user.getPhoneNumber();
                }
            } catch (Throwable t) {
                Log.w(TAG, "getMobileNumber Firebase error: " + t.getMessage());
            }
        }
        return "";
    }

    /**
     * Retrieves the current user's UID.
     */
    public static String getUserId(Context context) {
        if (isLiveFirebaseConfigured(context)) {
            try {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) return user.getUid();
            } catch (Throwable t) {
                Log.w(TAG, "getUserId Firebase error: " + t.getMessage());
            }
        }
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_USER_UID, "user_local_uid");
    }

    /**
     * Saves user data in Firebase Firestore collection "users" under doc(uid).
     * Required fields: name, mobile number, uid, profile photo, createdAt.
     */
    public static void saveUserDataToFirestore(
            Context context,
            String uid,
            String name,
            String mobileNumber,
            String profilePhoto,
            SaveCallback callback
    ) {
        // Save locally first for instant UI responsiveness
        saveLocalSession(context, uid, name, mobileNumber, profilePhoto);
        syncToRoomDatabase(context, name, mobileNumber);

        if (!isLiveFirebaseConfigured(context)) {
            Log.d(TAG, "Local profile persisted successfully (no live Firebase)");
            if (callback != null) callback.onSuccess();
            return;
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("mobileNumber", mobileNumber);
        userData.put("uid", uid);
        userData.put("profilePhoto", profilePhoto != null ? profilePhoto : "default_avatar");
        userData.put("createdAt", FieldValue.serverTimestamp());

        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(uid)
                    .set(userData, SetOptions.merge())
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "User data successfully saved in Firestore for UID: " + uid);
                            } else {
                                Log.w(TAG, "Firestore write warning: " + task.getException());
                            }
                            if (callback != null) callback.onSuccess();
                        }
                    });
        } catch (Throwable e) {
            Log.e(TAG, "Error initiating Firestore write", e);
            if (callback != null) callback.onSuccess();
        }
    }

    /**
     * Fetches user data from Firebase Firestore after successful phone verification.
     * Checks if user already exists or if it's their first login.
     */
    public static void fetchUserDataFromFirestore(
            Context context,
            String uid,
            String mobileNumber,
            ProfileCallback callback
    ) {
        if (!isLiveFirebaseConfigured(context)) {
            // Check local preference fallback
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String cachedName = prefs.getString(KEY_USER_NAME, "");
            boolean isFirst = (cachedName == null || cachedName.trim().isEmpty());
            if (callback != null) callback.onSuccess(cachedName, mobileNumber, isFirst);
            return;
        }

        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(uid).get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                                DocumentSnapshot doc = task.getResult();
                                String name = doc.getString("name");
                                String phone = doc.getString("mobileNumber");
                                String photo = doc.getString("profilePhoto");

                                if (phone == null || phone.isEmpty()) phone = mobileNumber;

                                if (name != null && !name.trim().isEmpty()) {
                                    // Returning user with existing name
                                    saveLocalSession(context, uid, name, phone, photo);
                                    syncToRoomDatabase(context, name, phone);
                                    if (callback != null) callback.onSuccess(name, phone, false);
                                    return;
                                }
                            }
                            // Document does not exist or name is empty -> First-time login
                            if (callback != null) callback.onSuccess("", mobileNumber, true);
                        }
                    });
        } catch (Throwable e) {
            Log.e(TAG, "Error fetching from Firestore", e);
            // Check local preference fallback
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String cachedName = prefs.getString(KEY_USER_NAME, "");
            boolean isFirst = (cachedName == null || cachedName.trim().isEmpty());
            if (callback != null) callback.onSuccess(cachedName, mobileNumber, isFirst);
        }
    }

    /**
     * Stores session data in SharedPreferences.
     */
    public static void saveLocalSession(
            Context context,
            String uid,
            String name,
            String mobileNumber,
            String profilePhoto
    ) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putString(KEY_USER_UID, uid)
                .putString(KEY_USER_NAME, name)
                .putString(KEY_USER_PHONE, mobileNumber)
                .putString(KEY_USER_PHOTO, profilePhoto != null ? profilePhoto : "default_avatar")
                .putLong(KEY_CREATED_AT, System.currentTimeMillis())
                .apply();
    }

    /**
     * Synchronizes the user's dynamic name and phone into the local SQLite Room database
     * so that all Jetpack Compose screens, Drawer, TopBar, and PDF summaries display it instantly.
     */
    public static void syncToRoomDatabase(Context context, String name, String phone) {
        if (name == null || name.trim().isEmpty()) return;

        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // Update patient_profile SQLite table directly via SQLite OpenHelper
                    com.example.data.local.MedicareDatabase db = MedicareApp.Companion.getInstance().getDatabase();
                    db.getOpenHelper().getWritableDatabase().execSQL(
                            "UPDATE patient_profile SET fullName = ?, emergencyPhone = ? WHERE id = 1",
                            new Object[]{name, phone}
                    );
                    Log.d(TAG, "Successfully synced dynamic patient name to Room SQLite: " + name);
                } catch (Exception e) {
                    Log.w(TAG, "Note: Patient profile table will update upon initial seed", e);
                }
            }
        });
    }

    /**
     * Clears Firebase auth state and local session preferences.
     */
    public static void logout(Context context) {
        if (isLiveFirebaseConfigured(context)) {
            try {
                FirebaseAuth.getInstance().signOut();
            } catch (Exception ignored) {}
        }

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();

        // Reset name in Room SQLite to default "Patient"
        syncToRoomDatabase(context, "Patient", "+91 98765 43210");
        Log.d(TAG, "User logged out successfully");
    }
}
