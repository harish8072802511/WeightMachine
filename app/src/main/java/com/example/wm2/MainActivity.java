package com.example.wm2;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private TextView weightTextView;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final String TAG = "MainActivity";

    private static final String URL = "http://192.168.1.115:8000/getweight";
    private static final long FETCH_INTERVAL_MS = 250; // Interval to fetch data ( seconds)
    private OkHttpClient client;

    private final Runnable fetchWeightTask = new Runnable() {
        @Override
        public void run() {
            fetchWeight();
            // Schedule the next fetch after the specified interval
            mainHandler.postDelayed(this, FETCH_INTERVAL_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Set the content view to the activity_main layout

        weightTextView = findViewById(R.id.weightTextView);
        client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        // Start the periodic weight fetching
        mainHandler.post(fetchWeightTask);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove any pending posts of fetchWeightTask when the activity is destroyed
        mainHandler.removeCallbacks(fetchWeightTask);
    }

    private void fetchWeight() {
        Request request = new Request.Builder()
                .url(URL)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Network Request Failed: " + e.getMessage(), e);
                mainHandler.post(() -> weightTextView.setText("Failed to fetch data"));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Response Body: " + responseBody);
                    try {
                        JSONObject jsonObject = new JSONObject(responseBody);
                        final String message = jsonObject.getString("message");
                        final double weight = jsonObject.getDouble("weight");
                        final String status = jsonObject.getString("Status");

                        mainHandler.post(() -> weightTextView.setText("Message: " + message + "\nWeight: " + weight + " kg\nStatus: " + status));
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON Parsing Error: " + e.getMessage(), e);
                        mainHandler.post(() -> weightTextView.setText("Failed to parse data"));
                    }
                } else {
                    Log.e(TAG, "Server Response Failed: " + response.message());
                    mainHandler.post(() -> weightTextView.setText("Failed to fetch data"));
                }
            }
        });
    }
}
