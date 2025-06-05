package com.example.serveradminapp.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.serveradminapp.LocaleUtil;
import com.example.serveradminapp.R;
import com.example.serveradminapp.ServerApi;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.RequestBody;

public class SettingsFragment extends Fragment {

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
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        EditText portEdit = view.findViewById(R.id.port_edit);
        EditText limitEdit = view.findViewById(R.id.limit_edit);
        Spinner langSpinner = view.findViewById(R.id.lang_spinner);

        ArrayAdapter<CharSequence> langAdapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.languages, R.layout.spinner_item_big);
        langAdapter.setDropDownViewResource(R.layout.spinner_dropdown_big);
        langSpinner.setAdapter(langAdapter);
        langSpinner.setSelection("ru".equals(requireActivity().getSharedPreferences("app_prefs",
                Context.MODE_PRIVATE).getString("lang", Locale.getDefault().getLanguage())) ? 1 : 0);
        langSpinner.post(() -> langSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            int last = langSpinner.getSelectedItemPosition();

            @Override
            public void onItemSelected(AdapterView<?> parent, View view1, int position, long id) {
                if (position != last) {
                    last = position;
                    String lang = position == 1 ? "ru" : "en";
                    LocaleUtil.setLocale(requireContext(), lang);
                    requireActivity().recreate();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        }));
        Button saveButton = view.findViewById(R.id.save_button);
        Button restartButton = view.findViewById(R.id.restart_button);

        loadConfig(portEdit, limitEdit);

        saveButton.setOnClickListener(v -> saveConfig(portEdit, limitEdit));
        restartButton.setOnClickListener(v -> restartServer());
    }

    private void loadConfig(EditText portEdit, EditText limitEdit) {
        ServerApi.get().loadConfig(new okhttp3.Callback() {
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
                    JSONObject obj = new JSONObject(body);
                    requireActivity().runOnUiThread(() -> {
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
            android.widget.Toast.makeText(requireContext(), getString(R.string.invalid_input), android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        RequestBody body = RequestBody.create(obj.toString(), MediaType.get("application/json"));
        ServerApi.get().updateConfig(body, new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                requireActivity().runOnUiThread(() ->
                        android.widget.Toast.makeText(requireContext(), getString(R.string.failed), android.widget.Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws IOException {
                response.close();
                requireActivity().runOnUiThread(() ->
                        android.widget.Toast.makeText(requireContext(), response.isSuccessful() ? getString(R.string.saved) : getString(R.string.error), android.widget.Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void restartServer() {
        ServerApi.get().restartServer(new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                requireActivity().runOnUiThread(() ->
                        android.widget.Toast.makeText(requireContext(), getString(R.string.failed), android.widget.Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws IOException {
                response.close();
                requireActivity().runOnUiThread(() ->
                        android.widget.Toast.makeText(requireContext(), getString(R.string.restarting), android.widget.Toast.LENGTH_SHORT).show());
            }
        });
    }

}
