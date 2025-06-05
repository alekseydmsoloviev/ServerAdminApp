package com.example.serveradminapp;

import android.os.Bundle;
import android.content.Intent;
import android.widget.Button;
import android.widget.EditText;
import android.view.View;
import android.content.Context;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.RequestBody;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleUtil.attach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LocaleUtil.apply(this);
        super.onCreate(savedInstanceState);
        ServerApi.restore(this);
        if (ServerApi.get() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_settings);
        View backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());
        EditText portEdit = findViewById(R.id.port_edit);
        EditText limitEdit = findViewById(R.id.limit_edit);
        android.widget.Spinner langSpinner = findViewById(R.id.lang_spinner);
        langSpinner.setAdapter(android.widget.ArrayAdapter.createFromResource(this,
                R.array.languages, android.R.layout.simple_spinner_item));
        langSpinner.setSelection("ru".equals(getSharedPreferences("app_prefs",
                MODE_PRIVATE).getString("lang", Locale.getDefault().getLanguage())) ? 1 : 0);
        langSpinner.post(() -> langSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            int last = langSpinner.getSelectedItemPosition();

            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position != last) {
                    last = position;
                    String lang = position == 1 ? "ru" : "en";
                    LocaleUtil.setLocale(SettingsActivity.this, lang);
                    recreate();
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        }));
        Button saveButton = findViewById(R.id.save_button);
        Button restartButton = findViewById(R.id.restart_button);

        loadConfig(portEdit, limitEdit);

        saveButton.setOnClickListener(v -> saveConfig(portEdit, limitEdit, langSpinner));
        restartButton.setOnClickListener(v -> restartServer());
    }

    private void loadConfig(EditText portEdit, EditText limitEdit) {
        ServerApi.get().loadConfig(new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                runOnUiThread(() -> android.widget.Toast.makeText(SettingsActivity.this, getString(R.string.failed_load), android.widget.Toast.LENGTH_SHORT).show());
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

    private void saveConfig(EditText portEdit, EditText limitEdit, android.widget.Spinner langSpinner) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("port", Integer.parseInt(portEdit.getText().toString().trim()));
            obj.put("daily_limit", Integer.parseInt(limitEdit.getText().toString().trim()));
        } catch (JSONException e) {
            android.widget.Toast.makeText(this, getString(R.string.invalid_input), android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        RequestBody body = RequestBody.create(obj.toString(), MediaType.get("application/json"));
        String lang = langSpinner.getSelectedItemPosition() == 1 ? "ru" : "en";
        LocaleUtil.setLocale(this, lang);
        ServerApi.get().updateConfig(body, new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                runOnUiThread(() -> android.widget.Toast.makeText(SettingsActivity.this, getString(R.string.failed), android.widget.Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws IOException {
                response.close();
                runOnUiThread(() -> android.widget.Toast.makeText(SettingsActivity.this, response.isSuccessful() ? getString(R.string.saved) : getString(R.string.error), android.widget.Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void restartServer() {
        ServerApi.get().restartServer(new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                runOnUiThread(() -> android.widget.Toast.makeText(SettingsActivity.this, getString(R.string.failed), android.widget.Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws IOException {
                response.close();
                runOnUiThread(() -> android.widget.Toast.makeText(SettingsActivity.this, getString(R.string.restarting), android.widget.Toast.LENGTH_SHORT).show());
            }
        });
    }
}
