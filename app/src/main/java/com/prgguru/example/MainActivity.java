package com.prgguru.example;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends Activity {
    private DBController controller;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize DBController
        controller = new DBController(this);

        // Retrieve user data from SQLite database
        ArrayList<HashMap<String, String>> userList = controller.getAllUsers();

        // If user data exists, display it in ListView
        if (userList.size() != 0) {
            ListAdapter adapter = new SimpleAdapter(MainActivity.this, userList,
                    R.layout.view_user_entry, new String[]{"userId", "userName"},
                    new int[]{R.id.userId, R.id.userName});
            ListView myList = findViewById(R.id.list);
            myList.setAdapter(adapter);

            // Show synchronization status
            Toast.makeText(getApplicationContext(), controller.getSyncStatus(),
                    Toast.LENGTH_LONG).show();
        }

        // Initialize ProgressDialog
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(false);
    }

    // Method to handle refresh button click
    public void refreshData(View view) {
        syncSQLiteMySQLDB();
    }

    // Method to handle add user button click
    public void addUser(View view) {
        Intent objIntent = new Intent(getApplicationContext(), NewUser.class);
        startActivity(objIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu XML file
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle menu item clicks
        int id = item.getItemId();
        if (id == R.id.refresh) {
            syncSQLiteMySQLDB();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Method to synchronize SQLite data with MySQL database
    public void syncSQLiteMySQLDB() {
        OkHttpClient client = new OkHttpClient();
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        ArrayList<HashMap<String, String>> userList = controller.getAllUsers();

        // Check if there are users to sync
        if (userList.size() != 0) {
            if (controller.dbSyncCount() != 0) {
                // Show ProgressDialog
                progressDialog.setMessage("Syncing SQLite Data with Remote MySQL DB. Please wait...");
                progressDialog.show();

                // Compose JSON data from SQLite database
                String json = controller.composeJSONfromSQLite();

                // Check if JSON data is not null
                if (json != null) {
                    RequestBody body = RequestBody.create(JSON, json.getBytes());

                    // Specify the URL of your PHP script for inserting users
                    Request request = new Request.Builder()
                            .url("http://192.168.113.150/xampp/sqlitemysqlsync/insertuser.php")
                            .post(body)
                            .build();

                    client.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            progressDialog.dismiss();
                            e.printStackTrace();
                            runOnUiThread(() -> {
                                if (e instanceof ConnectException || e instanceof UnknownHostException) {
                                    Toast.makeText(MainActivity.this, "Failed to connect to server. Please check your internet connection.", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(MainActivity.this, "Request failed. Please try again later.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            progressDialog.dismiss();
                            if (!response.isSuccessful()) {
                                throw new IOException("Unexpected code " + response);
                            }
                            String responseData = response.body().string();
                            try {
                                JSONArray arr = new JSONArray(responseData);
                                for (int i = 0; i < arr.length(); i++) {
                                    JSONObject obj = arr.getJSONObject(i);
                                    controller.updateSyncStatus(obj.getString("id"), obj.getString("status"));
                                }
                                runOnUiThread(() -> Toast.makeText(MainActivity.this, "DB Sync completed!", Toast.LENGTH_SHORT).show());
                            } catch (JSONException e) {
                                e.printStackTrace();
                                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Db Sync Succesfully !! Hooyehhhh!!", Toast.LENGTH_SHORT).show());
                            }
                        }
                    });
                } else {
                    progressDialog.dismiss();
                    Toast.makeText(MainActivity.this, "Failed to compose JSON data.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getApplicationContext(), "SQLite and Remote MySQL DBs are in Sync!", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(getApplicationContext(), "No data in SQLite DB. Please add users to perform Sync action.", Toast.LENGTH_LONG).show();
        }
    }

    // Method to synchronize data from web and delete all data from SQLite
    public void SyncDataFromWebBased(View view) {
        // Delete all data from SQLite
        controller.deleteAllData();

        // Call method to fetch data from PHPMyAdmin and insert into SQLite
        fetchFromPHPMyAdminAndInsertToSQLite();
    }

    // Method to fetch data from PHPMyAdmin and insert into SQLite
    private void fetchFromPHPMyAdminAndInsertToSQLite() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("http://192.168.113.150/xampp/sqlitemysqlsync/get_users.php") // Replace with your PHP script to fetch users from PHPMyAdmin
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to fetch data from PHPMyAdmin!", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }
                String responseData = response.body().string();
                try {
                    JSONArray jsonArray = new JSONArray(responseData);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        String userId = jsonObject.getString("userId");
                        String userName = jsonObject.getString("userName");
                        // Insert fetched data into SQLite database
                        controller.insertUser(userId, userName);
                    }
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Data synchronized from PHPMyAdmin to SQLite!", Toast.LENGTH_SHORT).show();
                        // Refresh ListView after syncing
                        refreshListView();
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to parse data from PHPMyAdmin!", Toast.LENGTH_SHORT).show());
                }
            }

        });

    }

    // Method to refresh ListView after syncing
    private void refreshListView() {
        ArrayList<HashMap<String, String>> userList = controller.getAllUsers();
        if (userList.size() != 0) {
            ListAdapter adapter = new SimpleAdapter(MainActivity.this, userList,
                    R.layout.view_user_entry, new String[]{"userId", "userName"},
                    new int[]{R.id.userId, R.id.userName});
            ListView myList = findViewById(R.id.list);
            myList.setAdapter(adapter);

            // Show synchronization status
            Toast.makeText(getApplicationContext(), "Sync Status: " + controller.getSyncStatus(),
                    Toast.LENGTH_LONG).show();
        }
    }

}

