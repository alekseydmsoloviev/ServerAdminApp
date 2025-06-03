package com.example.serveradminapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

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

    private TextView uptimeText;
    private TextView messages24hText;
    private TextView messages7dText;

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

        uptimeText = findViewById(R.id.uptime_text);
        messages24hText = findViewById(R.id.messages_24h_text);
        messages7dText = findViewById(R.id.messages_7d_text);

        Button usersButton = findViewById(R.id.users_button);
        Button modelsButton = findViewById(R.id.models_button);
        Button settingsButton = findViewById(R.id.settings_button);

        usersButton.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, UsersActivity.class)));
        modelsButton.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ModelsActivity.class)));
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
                        final String uptime = obj.optString("cpu") + "% CPU, " + obj.optString("memory") + "% MEM";
                        runOnUiThread(() -> uptimeText.setText(uptime));
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
                    final String uptime = obj.optString("port") + ", sessions: " + obj.optString("sessions");
                    runOnUiThread(() -> uptimeText.setText(uptime));
                } catch (JSONException ignored) {}
            }
        });
    }

    private void loadUsage() {
        ServerApi.get().fetchUsage(new Callback() {
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
                    int dayCount = 0;
                    int weekCount = 0;
                    if (obj.has("day")) {
                        Object day = obj.get("day");
                        if (day instanceof Number) {
                            dayCount = ((Number) day).intValue();
                        } else if (day instanceof JSONObject) {
                            java.util.Iterator<String> dKeys = ((JSONObject) day).keys();
                            while (dKeys.hasNext()) {
                                Object val = ((JSONObject) day).get(dKeys.next());
                                if (val instanceof Number) dayCount += ((Number) val).intValue();
                            }
                        }
                    }
                    if (obj.has("week")) {
                        Object week = obj.get("week");
                        if (week instanceof JSONArray) {
                            JSONArray arr = (JSONArray) week;
                            for (int i = 0; i < arr.length(); i++) weekCount += arr.optInt(i);
                        } else if (week instanceof JSONObject) {
                            java.util.Iterator<String> wKeys = ((JSONObject) week).keys();
                            while (wKeys.hasNext()) {
                                Object val = ((JSONObject) week).get(wKeys.next());
                                if (val instanceof Number) weekCount += ((Number) val).intValue();
                            }
                        } else if (week instanceof Number) {
                            weekCount = ((Number) week).intValue();
                        }
                    }
                    if (weekCount == 0) {
                        // Fallback: sum all numeric values
                        java.util.Iterator<String> keys = obj.keys();
                        while (keys.hasNext()) {
                            Object val = obj.get(keys.next());
                            if (val instanceof Number) weekCount += ((Number) val).intValue();
                        }
                    }
                    if (dayCount == 0) dayCount = weekCount; // at least show something
                    final int count24h = dayCount;
                    final int count7d = weekCount;
                    runOnUiThread(() -> {
                        messages24hText.setText("Messages last 24h: " + count24h);
                        messages7dText.setText("Messages last 7 days: " + count7d);
                    });
                } catch (JSONException ignored) {}
            }
        });
    }
}
