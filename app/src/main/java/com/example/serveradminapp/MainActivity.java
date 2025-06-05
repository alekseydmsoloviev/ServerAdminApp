package com.example.serveradminapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.view.View;
import com.example.serveradminapp.GaugeView;

import androidx.annotation.NonNull;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.Callback;
import okhttp3.Response;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView serverStateText;
    private TextView messages24hText;
    private TextView messagesTotalText;
    private GaugeView cpuGauge;
    private GaugeView memGauge;
    private GaugeView netGauge;
    private GaugeView diskGauge;
    private WebSocket metricsSocket;
    private final Runnable reconnectRunnable = this::connectMetrics;
    private final WebSocketListener metricsListener = new WebSocketListener() {
        @Override
        public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
            metricsSocket = webSocket;
            setStatusWork();
        }

        @Override
        public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
            metricsSocket = null;
            setStatusStop();
        }

        @Override
        public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, Response response) {
            metricsSocket = null;
            setStatusStop();
        }

        @Override
        public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
            try {
                JSONObject obj = new JSONObject(text);
                if (obj.has("cpu")) {
                    final int day = obj.optInt("day_total");
                    final int total = obj.optInt("total");
                    final int cpu = (int) Math.round(obj.optDouble("cpu"));
                    final int mem = (int) Math.round(obj.optDouble("memory"));
                    final int net = (int) Math.round(obj.optDouble("network"));
                    final int disk = (int) Math.round(obj.optDouble("disk"));
                    setStatusWork();
                    runOnUiThread(() -> {
                        if (day > 0) messages24hText.setText("Messages last 24h: " + day);
                        if (total > 0) messagesTotalText.setText("Messages total: " + total);
                        cpuGauge.setPercent(cpu);
                        memGauge.setPercent(mem);
                        netGauge.setPercent(net);
                        diskGauge.setPercent(disk);
                    });
                } else if (obj.has("snapshot")) {
                    JSONObject snap = obj.getJSONObject("snapshot");
                    updateUsageFromJson(snap);
                }
            } catch (JSONException ignored) {
            }
        }
    };
    private final Handler statusHandler = new Handler(Looper.getMainLooper());
    private final Runnable statusTimeout = () -> serverStateText.setText("Status: Stop");

    private void setStatusWork() {
        statusHandler.removeCallbacks(statusTimeout);
        statusHandler.removeCallbacks(reconnectRunnable);
        statusHandler.post(() -> serverStateText.setText("Status: Work"));
        statusHandler.postDelayed(statusTimeout, 15000);
    }

    private void setStatusStop() {
        statusHandler.removeCallbacks(statusTimeout);
        statusHandler.post(() -> serverStateText.setText("Status: Stop"));
        statusHandler.postDelayed(reconnectRunnable, 5000);
    }

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

        serverStateText = findViewById(R.id.server_state_text);
        serverStateText.setText("Status: Stop");
        messages24hText = findViewById(R.id.messages_24h_text);
        messagesTotalText = findViewById(R.id.messages_total_text);
        cpuGauge = findViewById(R.id.cpu_gauge);
        memGauge = findViewById(R.id.mem_gauge);
        netGauge = findViewById(R.id.net_gauge);
        diskGauge = findViewById(R.id.disk_gauge);

        View usersButton = findViewById(R.id.users_button);
        View modelsButton = findViewById(R.id.models_button);
        View chatsButton = findViewById(R.id.chats_button);
        View settingsButton = findViewById(R.id.settings_button);

        usersButton.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, UsersActivity.class)));
        modelsButton.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ModelsActivity.class)));
        chatsButton.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ChatsActivity.class)));
        settingsButton.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, SettingsActivity.class)));

        connectMetrics();
        loadUsage();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUsage();
    }

    private void connectMetrics() {
        if (metricsSocket != null) {
            metricsSocket.cancel();
            metricsSocket = null;
        }
        metricsSocket = ServerApi.get().connectMetrics(metricsListener);
        if (metricsSocket == null) {
            setStatusStop();
        }
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

    @Override
    protected void onDestroy() {
        statusHandler.removeCallbacks(reconnectRunnable);
        if (metricsSocket != null) metricsSocket.cancel();
        super.onDestroy();
    }
}
