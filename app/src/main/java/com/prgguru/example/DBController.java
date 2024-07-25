package com.prgguru.example;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DBController extends SQLiteOpenHelper {

    private Context mContext;

    private static final String DATABASE_NAME = "androidsqlite.db";
    private static final int DATABASE_VERSION = 1;

    public DBController(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        String query = "CREATE TABLE users (userId INTEGER PRIMARY KEY, userName TEXT, updateStatus TEXT)";
        database.execSQL(query);
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        String query = "DROP TABLE IF EXISTS users";
        database.execSQL(query);
        onCreate(database);
    }

    public void InsertingUser(HashMap<String, String> userData) {
        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("userName", userData.get("userName"));
        values.put("updateStatus", "no");
        long newRowId = database.insert("users", null, values);
        database.close();

        // Check if insertion was successful and update status
        if (newRowId != -1) {
            // Data inserted successfully, update status to indicate it needs synchronization
            String userId = String.valueOf(newRowId);
            updateSyncStatus(userId, "no");
        }
    }

    public void insertUser(String userId, String userName) {
        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("userId", userId);
        values.put("userName", userName);
        values.put("updateStatus", "no");
        long newRowId = database.insert("users", null, values);
        database.close();

        // Check if insertion was successful and update status
        if (newRowId != -1) {
            // Data inserted successfully, update status to indicate it needs synchronization
            updateSyncStatus(String.valueOf(newRowId), "no");
        }
    }


    public ArrayList<HashMap<String, String>> getAllUsers() {
        ArrayList<HashMap<String, String>> userList = new ArrayList<>();
        String selectQuery = "SELECT * FROM users";
        SQLiteDatabase database = this.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                HashMap<String, String> user = new HashMap<>();
                user.put("userId", cursor.getString(0));
                user.put("userName", cursor.getString(1));
                userList.add(user);
            } while (cursor.moveToNext());
        }
        cursor.close();
        database.close();
        return userList;
    }

    public String composeJSONfromSQLite() {
        ArrayList<HashMap<String, String>> userList = new ArrayList<>();
        String selectQuery = "SELECT * FROM users WHERE updateStatus = 'no'";
        SQLiteDatabase database = this.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                HashMap<String, String> user = new HashMap<>();
                user.put("userId", cursor.getString(0));
                user.put("userName", cursor.getString(1));
                userList.add(user);
            } while (cursor.moveToNext());
        }
        cursor.close();
        database.close();
        Gson gson = new GsonBuilder().create();
        return gson.toJson(userList);
    }

    public void syncDataWithServer() {
        String url = "http://192.168.113.150/xampp/SQLiteMySQLSync/database_sync.php";
        String dataToSync = composeJSONfromSQLite();

        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), dataToSync);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    Log.d("Sync Response", responseData);
                } else {
                    Log.e("Sync Error", "Unsuccessful response: " + response.message());
                }
            }
        });
    }

    public boolean isConnectedToInternet() {
        ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    public String getSyncStatus() {
        int syncCount = dbSyncCount();
        if (syncCount == 0) {
            return "SQLite and Remote MySQL DBs are in Sync!";
        } else {
            return "DB Sync needed";
        }
    }

    public int dbSyncCount() {
        int count = 0;
        String selectQuery = "SELECT * FROM users WHERE updateStatus = 'no'";
        SQLiteDatabase database = this.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);
        count = cursor.getCount();
        cursor.close();
        database.close();
        return count;
    }

    public void updateSyncStatus(String id, String status) {
        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("updateStatus", status);
        database.update("users", values, "userId = ?", new String[]{id});
        database.close();
    }

    public void deleteAllData() {
        SQLiteDatabase database = this.getWritableDatabase();
        database.delete("users", null, null);
        database.close();
    }
}
