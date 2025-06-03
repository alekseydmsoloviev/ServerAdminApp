package com.example.serveradminapp;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;

public class ModelsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_models);
        ListView listView = findViewById(R.id.models_list);
        loadModels(listView);
    }

    private void loadModels(ListView listView) {
        ServerApi.get().listModels(new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                runOnUiThread(() -> android.widget.Toast.makeText(ModelsActivity.this, "Failed to load", android.widget.Toast.LENGTH_SHORT).show());
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
                    JSONArray array = new JSONArray(body);
                    String[] models = ServerApi.jsonArrayToStringArray(array);
                    runOnUiThread(() -> {
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(ModelsActivity.this,
                                android.R.layout.simple_list_item_1, models);
                        listView.setAdapter(adapter);
                    });
                } catch (JSONException ex) {
                    onFailure(call, new IOException(ex));
                }
            }
        });
    }
}
