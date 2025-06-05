package com.example.serveradminapp.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.serveradminapp.LocaleUtil;
import com.example.serveradminapp.R;
import com.example.serveradminapp.ServerApi;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;

public class ModelsFragment extends Fragment {

    private final ArrayList<String> modelList = new ArrayList<>();
    private ModelAdapter adapter;
    private ListView listView;
    private Spinner availableSpinner;
    private Spinner variantSpinner;
    private ArrayAdapter<String> availableAdapter;
    private ArrayAdapter<String> variantAdapter;
    private final ArrayList<String> availableList = new ArrayList<>();
    private final ArrayList<String> variantList = new ArrayList<>();
    private TextView progressText;
    private okhttp3.WebSocket ws;
    private String installingVariant;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        LocaleUtil.attach(context);
        LocaleUtil.apply(requireActivity());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_models, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        listView = view.findViewById(R.id.models_list);
        availableSpinner = view.findViewById(R.id.available_spinner);
        variantSpinner = view.findViewById(R.id.variant_spinner);
        Button installButton = view.findViewById(R.id.install_model_button);
        progressText = view.findViewById(R.id.progress_text);

        adapter = new ModelAdapter();
        listView.setAdapter(adapter);

        availableAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, availableList);
        availableAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        availableSpinner.setAdapter(availableAdapter);
        availableSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View v, int position, long id) {
                String name = availableList.get(position);
                loadVariants(name);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        variantAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, variantList);
        variantAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        variantSpinner.setAdapter(variantAdapter);

        loadModels();
        loadAvailableModels();
        connectMetrics();
        installButton.setOnClickListener(v -> installSelected());
        listView.setOnItemLongClickListener((parent, itemView, position, id) -> {
            String model = modelList.get(position);
            confirmDelete(model);
            return true;
        });
    }

    @Override
    public void onDestroyView() {
        if (ws != null) ws.close(1000, null);
        super.onDestroyView();
    }

    private void loadModels() {
        ServerApi.get().listModels(new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                requireActivity().runOnUiThread(() ->
                        android.widget.Toast.makeText(requireContext(), getString(R.string.failed_load), android.widget.Toast.LENGTH_SHORT).show());
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
                    requireActivity().runOnUiThread(() -> adapter.notifyDataSetChanged());
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
                requireActivity().runOnUiThread(() ->
                        android.widget.Toast.makeText(requireContext(), getString(R.string.failed), android.widget.Toast.LENGTH_SHORT).show());
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
                    requireActivity().runOnUiThread(() -> availableAdapter.notifyDataSetChanged());
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
                requireActivity().runOnUiThread(() ->
                        android.widget.Toast.makeText(requireContext(), getString(R.string.failed), android.widget.Toast.LENGTH_SHORT).show());
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
                    requireActivity().runOnUiThread(() -> variantAdapter.notifyDataSetChanged());
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
                // ignore; installation may still proceed
            }

            @Override
            public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws IOException {
                response.close();
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
                                requireActivity().runOnUiThread(() -> {
                                    progressText.setText("");
                                    android.widget.Toast.makeText(requireContext(),
                                            "модель " + name + " успешно установлена",
                                            android.widget.Toast.LENGTH_SHORT).show();
                                });
                            } else {
                                requireActivity().runOnUiThread(() -> progressText.setText(parsed));
                            }
                        }
                    } else if (obj.has("models")) {
                        org.json.JSONArray arr = obj.getJSONArray("models");
                        modelList.clear();
                        for (int i = 0; i < arr.length(); i++) {
                            modelList.add(arr.getString(i));
                        }
                        requireActivity().runOnUiThread(() -> adapter.notifyDataSetChanged());
                        if (installingVariant != null) {
                            for (int i = 0; i < arr.length(); i++) {
                                if (installingVariant.equals(arr.getString(i))) {
                                    final String name = installingVariant;
                                    installingVariant = null;
                                    requireActivity().runOnUiThread(() -> android.widget.Toast.makeText(
                                            requireContext(),
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

    private class ModelAdapter extends ArrayAdapter<String> {
        ModelAdapter() {
            super(requireContext(), 0, modelList);
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
        new AlertDialog.Builder(requireContext())
                .setMessage(getString(R.string.delete_q, name))
                .setPositiveButton(getString(R.string.delete), (d, w) -> deleteModel(name))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void deleteModel(String name) {
        ServerApi.get().deleteModel(name, new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                requireActivity().runOnUiThread(() ->
                        android.widget.Toast.makeText(requireContext(), getString(R.string.error), android.widget.Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws IOException {
                response.close();
                loadModels();
            }
        });
    }
}
