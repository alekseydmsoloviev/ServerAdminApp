package com.example.serveradminapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.IOException;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.Callback;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView metricsText;
    private TextView resourceText;
    private TextView messages24hText;
    private TextView messagesTotalText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ServerApi.restore(this);
        if (ServerApi.get() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_main);

        metricsText = findViewById(R.id.metrics_text);
        resourceText = findViewById(R.id.resource_text);
        messages24hText = findViewById(R.id.messages_24h_text);
        messagesTotalText = findViewById(R.id.messages_total_text);

        Button usersButton = findViewById(R.id.users_button);
        Button modelsButton = findViewById(R.id.models_button);
        Button chatsButton = findViewById(R.id.chats_button);
        Button settingsButton = findViewById(R.id.settings_button);

        usersButton.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, UsersActivity.class)));
        modelsButton.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ModelsActivity.class)));
        chatsButton.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ChatsActivity.class)));
        settingsButton.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, SettingsActivity.class)));

        connectMetrics();
        loadStatus();
        loadUsage();
    }

    private void connectMetrics() {
        ServerApi.get().connectMetrics(new WebSocketListener() {
            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                try {
                    JSONObject obj = new JSONObject(text);
                    if (obj.has("cpu")) {

                        final String metrics = "CPU: " + obj.optString("cpu") + "%  MEM: " + obj.optString("memory") + "%";
                        final String res = "NET: " + obj.optString("network") + "%  DISK: " + obj.optString("disk") + "%";
                        final int day = obj.optInt("day_total");
                        final int total = obj.optInt("total");
                        runOnUiThread(() -> {
                            metricsText.setText(metrics);
                            resourceText.setText(res);
                            if (day > 0) messages24hText.setText("Messages last 24h: " + day);
                            if (total > 0) messagesTotalText.setText("Messages total: " + total);
                        });
                    } else if (obj.has("snapshot")) {
                        // overview snapshot may include usage data

                        JSONObject snap = obj.getJSONObject("snapshot");
                        updateUsageFromJson(snap);
                    } else if ("progress".equals(obj.optString("type"))) {
                        // ignore progress for now
                    }
                } catch (JSONException ignored) {
                }
            }
        });
    }

    private void loadStatus() {
        ServerApi.get().checkStatus(new Callback() {
            @Override
            public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
            }

            @Override
            public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws IOException {
                if (!response.isSuccessful()) { response.close(); return; }
                String body = response.body().string();
                response.close();
                try {
                    JSONObject obj = new JSONObject(body);
                    final String status = "Port " + obj.optString("port") + ", sessions: " + obj.optString("sessions");
                    runOnUiThread(() -> metricsText.setText(status));

                } catch (JSONException ignored) {}
            }
        });
    }

    private void loadUsage() {
        ServerApi.get().fetchUsage(new Callback() {
            @Override
            public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    messages24hText.setText("Messages last 24h: --");
                    messagesTotalText.setText("Messages total: --");

                });
            }

            @Override
            public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws IOException {
                if (!response.isSuccessful()) { response.close(); return; }
                String body = response.body().string();
                response.close();
                try {
                    JSONObject obj = new JSONObject(body);
                    updateUsageFromJson(obj);
                } catch (JSONException ignored) {}
            }
        });
    }

    /** Parse usage information from a JSON object and update the UI. */
    private void updateUsageFromJson(JSONObject obj) throws JSONException {
        int dayCount = obj.optInt("day_total", obj.optInt("day"));
        int totalCount = obj.optInt("total", dayCount);

        final int count24h = dayCount;
        final int countTotal = totalCount;
        runOnUiThread(() -> {
            messages24hText.setText("Messages last 24h: " + count24h);
            messagesTotalText.setText("Messages total: " + countTotal);
        });
    }
}
