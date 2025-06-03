package com.example.serveradminapp;

import android.os.Bundle;

import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.RequestBody;


public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EditText portEdit = findViewById(R.id.port_edit);
        EditText limitEdit = findViewById(R.id.limit_edit);
        Button saveButton = findViewById(R.id.save_button);
        Button restartButton = findViewById(R.id.restart_button);

        loadConfig(portEdit, limitEdit);

        saveButton.setOnClickListener(v -> saveConfig(portEdit, limitEdit));
        restartButton.setOnClickListener(v -> restartServer());
    }

    private void loadConfig(EditText portEdit, EditText limitEdit) {
        ServerApi.get().loadConfig(new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                runOnUiThread(() -> android.widget.Toast.makeText(SettingsActivity.this, "Failed to load config", android.widget.Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws IOException {
                if (!response.isSuccessful()) {
                    response.close();
                    onFailure(call, new IOException("HTTP " + response.code()));
                    return;
                }
                String body = response.body().string();
                response.close();
                try {
                    JSONObject obj = new JSONObject(body);
                    runOnUiThread(() -> {
                        portEdit.setText(obj.optString("port"));
                        limitEdit.setText(obj.optString("daily_limit"));
                    });
                } catch (JSONException ex) {
                    onFailure(call, new IOException(ex));
                }
            }
        });
    }

    private void saveConfig(EditText portEdit, EditText limitEdit) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("port", Integer.parseInt(portEdit.getText().toString().trim()));
            obj.put("daily_limit", Integer.parseInt(limitEdit.getText().toString().trim()));
        } catch (JSONException e) {
            android.widget.Toast.makeText(this, "Invalid input", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        RequestBody body = RequestBody.create(obj.toString(), MediaType.get("application/json"));
        ServerApi.get().updateConfig(body, new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                runOnUiThread(() -> android.widget.Toast.makeText(SettingsActivity.this, "Failed to save", android.widget.Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws IOException {
                response.close();
                runOnUiThread(() -> android.widget.Toast.makeText(SettingsActivity.this, response.isSuccessful() ? "Saved" : "Error", android.widget.Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void restartServer() {
        ServerApi.get().restartServer(new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                runOnUiThread(() -> android.widget.Toast.makeText(SettingsActivity.this, "Failed", android.widget.Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws IOException {
                response.close();
                runOnUiThread(() -> android.widget.Toast.makeText(SettingsActivity.this, "Restarting", android.widget.Toast.LENGTH_SHORT).show());
            }
        });

    }
}
