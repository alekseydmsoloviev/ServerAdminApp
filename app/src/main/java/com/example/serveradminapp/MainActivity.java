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

    private TextView uptimeText;
    private TextView messages24hText;
    private TextView messages7dText;
    private View[] weekBars = new View[7];

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
        weekBars[0] = findViewById(R.id.bar1);
        weekBars[1] = findViewById(R.id.bar2);
        weekBars[2] = findViewById(R.id.bar3);
        weekBars[3] = findViewById(R.id.bar4);
        weekBars[4] = findViewById(R.id.bar5);
        weekBars[5] = findViewById(R.id.bar6);
        weekBars[6] = findViewById(R.id.bar7);

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
                runOnUiThread(() -> {
                    messages24hText.setText("Messages last 24h: --");
                    messages7dText.setText("Messages last 7 days: --");
                });
            }

            @Override
            public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws IOException {
                if (!response.isSuccessful()) { response.close(); return; }
                String body = response.body().string();
                response.close();
                try {
                    JSONObject obj = new JSONObject(body);

                    int dayCount = extractSum(obj.opt("day"));
                    int[] weekArray = extractArray(obj.opt("week"));
                    int weekCount = 0;
                    for (int w : weekArray) weekCount += w;

                    if (dayCount == 0) dayCount = extractSum(obj.opt("last24h"));
                    if (weekArray.length == 0) {
                        weekArray = extractArray(obj.opt("last7d"));
                        for (int w : weekArray) weekCount += w;
                    }
                    if (weekCount == 0) weekCount = extractSum(obj); // fallback

                    final int[] weekBarsValues = weekArray;
                    final int count24h = dayCount;
                    final int count7d = weekCount;
                    runOnUiThread(() -> {
                        messages24hText.setText("Messages last 24h: " + count24h);
                        messages7dText.setText("Messages last 7 days: " + count7d);
                        updateWeekChart(weekBarsValues);
                    });
                } catch (JSONException ignored) {}
            }
        });
    }

    private int extractSum(Object obj) throws JSONException {
        if (obj == null) return 0;
        if (obj instanceof Number) return ((Number) obj).intValue();
        int sum = 0;
        if (obj instanceof JSONArray) {
            JSONArray arr = (JSONArray) obj;
            for (int i = 0; i < arr.length(); i++) {
                sum += extractSum(arr.get(i));
            }
        } else if (obj instanceof JSONObject) {
            JSONObject o = (JSONObject) obj;
            java.util.Iterator<String> it = o.keys();
            while (it.hasNext()) {
                sum += extractSum(o.get(it.next()));
            }
        } else if (obj instanceof String) {
            try { sum = Integer.parseInt((String) obj); } catch (NumberFormatException ignored) {}
        }
        return sum;
    }

    private int[] extractArray(Object obj) throws JSONException {
        if (obj instanceof JSONArray) {
            JSONArray arr = (JSONArray) obj;
            int[] result = new int[arr.length()];
            for (int i = 0; i < arr.length(); i++) result[i] = arr.optInt(i);
            return result;
        }
        return new int[0];
    }

    private void updateWeekChart(int[] counts) {
        int max = 0;
        for (int c : counts) if (c > max) max = c;
        for (int i = 0; i < weekBars.length; i++) {
            View bar = weekBars[i];
            int val = i < counts.length ? counts[i] : 0;
            int height = max == 0 ? 0 : (int) (100 * (val / (float) max));
            ViewGroup.LayoutParams lp = bar.getLayoutParams();
            lp.height = (int) (height * bar.getContext().getResources().getDisplayMetrics().density);
            bar.setLayoutParams(lp);
        }
    }
}
