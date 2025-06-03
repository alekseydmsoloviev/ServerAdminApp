package com.example.serveradminapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView uptimeText;
    private TextView messages24hText;
    private TextView messages7dText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
}
