package com.example.serveradminapp;

import android.os.Bundle;

import android.app.AlertDialog;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;

public class ModelsActivity extends AppCompatActivity {

    private ArrayList<String> modelList = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private ListView listView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_models);

        listView = findViewById(R.id.models_list);
        Button installButton = findViewById(R.id.install_model_button);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, modelList);
        listView.setAdapter(adapter);
        loadModels();
        installButton.setOnClickListener(v -> showInstallDialog());
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            String model = modelList.get(position);
            deleteModel(model);
            return true;
        });
    }

    private void loadModels() {
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
                    modelList.clear();
                    for (int i = 0; i < array.length(); i++) {
                        modelList.add(array.getString(i));
                    }
                    runOnUiThread(() -> adapter.notifyDataSetChanged());
                } catch (JSONException ex) {
                    onFailure(call, new IOException(ex));
                }
            }
        });
    }

    private void showInstallDialog() {
        ServerApi.get().availableModels(new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                runOnUiThread(() -> android.widget.Toast.makeText(ModelsActivity.this, "Failed", android.widget.Toast.LENGTH_SHORT).show());
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
                    JSONArray arr = new JSONArray(body);
                    String[] items = ServerApi.jsonArrayToStringArray(arr);
                    runOnUiThread(() -> {
                        AlertDialog.Builder b = new AlertDialog.Builder(ModelsActivity.this);
                        b.setTitle("Install model");
                        b.setItems(items, (d, which) -> installModel(items[which]));
                        b.show();
                    });
                } catch (JSONException ex) {
                    onFailure(call, new IOException(ex));
                }
            }
        });
    }

    private void installModel(String name) {
        ServerApi.get().installModel(name, new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                runOnUiThread(() -> android.widget.Toast.makeText(ModelsActivity.this, "Error", android.widget.Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws IOException {
                response.close();
            }
        });
    }

    private void deleteModel(String name) {
        ServerApi.get().deleteModel(name, new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                runOnUiThread(() -> android.widget.Toast.makeText(ModelsActivity.this, "Error", android.widget.Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws IOException {
                response.close();
                loadModels();
            }
        });

    }
}
