package com.example.serveradminapp;

import android.os.Bundle;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ImageButton;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;

public class ModelsActivity extends AppCompatActivity {

    private ArrayList<String> modelList = new ArrayList<>();
    private ModelAdapter adapter;
    private ListView listView;
    private Spinner availableSpinner;
    private Spinner variantSpinner;
    private ArrayAdapter<String> availableAdapter;
    private ArrayAdapter<String> variantAdapter;
    private ArrayList<String> availableList = new ArrayList<>();
    private ArrayList<String> variantList = new ArrayList<>();
    private TextView progressText;
    private okhttp3.WebSocket ws;
    private String installingVariant;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ServerApi.restore(this);
        if (ServerApi.get() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_models);
        listView = findViewById(R.id.models_list);
        availableSpinner = findViewById(R.id.available_spinner);
        variantSpinner = findViewById(R.id.variant_spinner);
        Button installButton = findViewById(R.id.install_model_button);
        progressText = findViewById(R.id.progress_text);

        adapter = new ModelAdapter();
        listView.setAdapter(adapter);

        availableAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, availableList);
        availableAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        availableSpinner.setAdapter(availableAdapter);
        availableSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                String name = availableList.get(position);
                loadVariants(name);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        variantAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, variantList);
        variantAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        variantSpinner.setAdapter(variantAdapter);

        loadModels();
        loadAvailableModels();
        connectMetrics();
        installButton.setOnClickListener(v -> installSelected());
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            String model = modelList.get(position);
            confirmDelete(model);
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

    private void loadAvailableModels() {
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
                    availableList.clear();
                    for (int i = 0; i < arr.length(); i++) {
                        availableList.add(arr.getString(i));
                    }
                    runOnUiThread(() -> availableAdapter.notifyDataSetChanged());
                } catch (JSONException ex) {
                    onFailure(call, new IOException(ex));
                }
            }
        });
    }

    private void loadVariants(String name) {
        ServerApi.get().modelVariants(name, new okhttp3.Callback() {
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
                    variantList.clear();
                    for (int i = 0; i < arr.length(); i++) {
                        variantList.add(arr.getString(i));
                    }
                    runOnUiThread(() -> variantAdapter.notifyDataSetChanged());
                } catch (JSONException ex) {
                    onFailure(call, new IOException(ex));
                }
            }
        });
    }

    private void installSelected() {
        if (availableList.isEmpty() || variantList.isEmpty()) return;
        String model = (String) availableSpinner.getSelectedItem();
        String variant = (String) variantSpinner.getSelectedItem();
        String fullVariant = variant.contains(":" ) ? variant : model + ":" + variant;
        installingVariant = fullVariant;
        ServerApi.get().installModelVariant(fullVariant, new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                // Some servers close the connection immediately after accepting
                // the install request which triggers this callback even though
                // the installation proceeds. We ignore this failure to avoid a
                // misleading toast.
            }

            @Override
            public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws IOException {
                response.close();
                // models list will refresh via WebSocket metrics
            }
        });
    }

    private void connectMetrics() {
        ws = ServerApi.get().connectMetrics(new okhttp3.WebSocketListener() {
            @Override
            public void onMessage(@NonNull okhttp3.WebSocket webSocket, @NonNull String text) {
                try {
                    org.json.JSONObject obj = new org.json.JSONObject(text);
                    if ("progress".equals(obj.optString("type"))) {
                        String data = obj.optString("data");
                        String parsed = parseProgressLine(data);
                        if (parsed != null) {
                            if ("success".equals(parsed)) {
                                String name = installingVariant;
                                installingVariant = null;
                                runOnUiThread(() -> {
                                    progressText.setText("");
                                    android.widget.Toast.makeText(ModelsActivity.this,
                                            "модель " + name + " успешно установлена",
                                            android.widget.Toast.LENGTH_SHORT).show();
                                });
                            } else {
                                runOnUiThread(() -> progressText.setText(parsed));
                            }
                        }
                    } else if (obj.has("models")) {
                        org.json.JSONArray arr = obj.getJSONArray("models");
                        modelList.clear();
                        for (int i = 0; i < arr.length(); i++) {
                            modelList.add(arr.getString(i));
                        }
                        runOnUiThread(() -> adapter.notifyDataSetChanged());
                        if (installingVariant != null) {
                            for (int i = 0; i < arr.length(); i++) {
                                if (installingVariant.equals(arr.getString(i))) {
                                    final String name = installingVariant;
                                    installingVariant = null;
                                    runOnUiThread(() -> android.widget.Toast.makeText(
                                            ModelsActivity.this,
                                            "модель " + name + " успешно установлена",
                                            android.widget.Toast.LENGTH_SHORT).show());
                                    break;
                                }
                            }
                        }
                    }
                } catch (org.json.JSONException ignored) {
                }
            }
        });
    }

    private String parseProgressLine(String line) {
        // remove ANSI escape codes and control characters
        line = line.replaceAll("\\u001B\\[[0-9;]*[A-Za-z]", "");
        line = line.replaceAll("[\\r\\n]", " ");
        line = line.replaceAll("[\\x00-\\x1F]", "");
        if (line.contains("success")) {
            return "success";
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                "(\\d+)%.*?([0-9.]+ [kMG]B/s).*?(\\d+s)").matcher(line);
        if (m.find()) {
            return m.group(1) + "% " + m.group(2) + " " + m.group(3);
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        if (ws != null) ws.close(1000, null);
        super.onDestroy();
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

    private class ModelAdapter extends ArrayAdapter<String> {
        ModelAdapter() {
            super(ModelsActivity.this, 0, modelList);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_model, parent, false);
            }
            String name = getItem(position);
            TextView tv = convertView.findViewById(R.id.model_name);
            tv.setText(name);
            ImageButton del = convertView.findViewById(R.id.delete_button);
            del.setOnClickListener(v -> confirmDelete(name));
            return convertView;
        }
    }

    private void confirmDelete(String name) {
        new android.app.AlertDialog.Builder(this)
                .setMessage("Delete " + name + "?")
                .setPositiveButton("Delete", (d, w) -> deleteModel(name))
                .setNegativeButton("Cancel", null)
                .show();
    }
}
