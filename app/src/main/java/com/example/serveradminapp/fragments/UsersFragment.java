package com.example.serveradminapp.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.serveradminapp.LocaleUtil;
import com.example.serveradminapp.R;
import com.example.serveradminapp.ServerApi;
import com.example.serveradminapp.UserDetailActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.MediaType;
import okhttp3.RequestBody;

public class UsersFragment extends Fragment {

    private ListView listView;
    private ArrayAdapter<String> adapter;
    private final ArrayList<String> userList = new ArrayList<>();
    private okhttp3.WebSocket ws;

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
        return inflater.inflate(R.layout.activity_users, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        listView = view.findViewById(R.id.users_list);
        Button addButton = view.findViewById(R.id.add_user_button);
        adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, userList);
        listView.setAdapter(adapter);
        loadUsers();
        connectWs();

        addButton.setOnClickListener(v -> showAddUserDialog());
        listView.setOnItemLongClickListener((parent, itemView, position, id) -> {
            String username = userList.get(position);
            new AlertDialog.Builder(requireContext())
                    .setMessage(getString(R.string.delete_user_q, username))
                    .setPositiveButton(getString(R.string.delete), (d,w)->deleteUser(username))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            return true;
        });
        listView.setOnItemClickListener((p,v,pos,id)->{
            String username = userList.get(pos);
            Intent intent = new Intent(requireContext(), UserDetailActivity.class);
            intent.putExtra("username", username);
            startActivity(intent);
        });
    }

    @Override
    public void onDestroyView() {
        if (ws != null) ws.close(1000, null);
        super.onDestroyView();
    }

    private void loadUsers() {
        ServerApi.get().listUsers(new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() ->
                            android.widget.Toast.makeText(requireContext(), getString(R.string.failed_load_users), android.widget.Toast.LENGTH_SHORT).show());
                }
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
                    userList.clear();
                    for (int i = 0; i < array.length(); i++) {
                        Object item = array.get(i);
                        if (item instanceof JSONObject) {
                            userList.add(((JSONObject) item).optString("username"));
                        } else {
                            userList.add(String.valueOf(item));
                        }
                    }
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> adapter.notifyDataSetChanged());
                    }
                } catch (JSONException ex) {
                    onFailure(call, new IOException(ex));
                }
            }
        });
    }

    private void connectWs() {
        ws = ServerApi.get().connectMetrics(new okhttp3.WebSocketListener() {
            @Override
            public void onMessage(@NonNull okhttp3.WebSocket webSocket, @NonNull String text) {
                try {
                    JSONObject obj = new JSONObject(text);
                    JSONArray users = null;
                    if (obj.has("users")) {
                        users = obj.getJSONArray("users");
                    } else if (obj.has("snapshot")) {
                        users = obj.getJSONObject("snapshot").optJSONArray("users");
                    }
                    if (users != null) {
                        ArrayList<String> list = new ArrayList<>();
                        for (int i = 0; i < users.length(); i++) {
                            JSONObject u = users.getJSONObject(i);
                            list.add(u.optString("username"));
                        }
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                userList.clear();
                                userList.addAll(list);
                                adapter.notifyDataSetChanged();
                            });
                        }
                    }
                } catch (JSONException ignore) {}
            }
        });
    }

    private void showAddUserDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Add User");
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        EditText nameEdit = new EditText(requireContext());
        nameEdit.setHint("Username");
        EditText passEdit = new EditText(requireContext());
        passEdit.setHint("Password");
        passEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        EditText limitEdit = new EditText(requireContext());
        limitEdit.setHint("Daily limit");
        limitEdit.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(nameEdit);
        layout.addView(passEdit);
        layout.addView(limitEdit);
        builder.setView(layout);
        builder.setPositiveButton(getString(R.string.save), (dialog, which) -> {
            JSONObject obj = new JSONObject();
            try {
                obj.put("username", nameEdit.getText().toString().trim());
                obj.put("password", passEdit.getText().toString());
                obj.put("daily_limit", Integer.parseInt(limitEdit.getText().toString().trim()));
            } catch (Exception e) {
                android.widget.Toast.makeText(requireContext(), getString(R.string.invalid_input), android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            RequestBody body = RequestBody.create(obj.toString(), MediaType.get("application/json"));
            ServerApi.get().createUser(body, new okhttp3.Callback() {
                @Override
                public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() ->
                                android.widget.Toast.makeText(requireContext(), getString(R.string.failed), android.widget.Toast.LENGTH_SHORT).show());
                    }
                }

                @Override
                public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws IOException {
                    response.close();
                    loadUsers();
                }
            });
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();
    }

    private void deleteUser(String username) {
        ServerApi.get().deleteUser(username, new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() ->
                            android.widget.Toast.makeText(requireContext(), getString(R.string.failed), android.widget.Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws IOException {
                response.close();
                loadUsers();
            }
        });
    }
}
