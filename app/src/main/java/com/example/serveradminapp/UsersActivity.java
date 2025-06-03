package com.example.serveradminapp;

import android.os.Bundle;
import android.app.AlertDialog;
import android.content.Intent;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.LinearLayout;
import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.RequestBody;
import okhttp3.MediaType;


public class UsersActivity extends AppCompatActivity {

    private ListView listView;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> userList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (ServerApi.get() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_users);

        listView = findViewById(R.id.users_list);
        Button addButton = findViewById(R.id.add_user_button);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, userList);
        listView.setAdapter(adapter);
        loadUsers();

        addButton.setOnClickListener(v -> showAddUserDialog());
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            String username = userList.get(position);
            deleteUser(username);
            return true;
        });
    }

    private void loadUsers() {
        ServerApi.get().listUsers(new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                runOnUiThread(() -> android.widget.Toast.makeText(UsersActivity.this, "Failed to load users", android.widget.Toast.LENGTH_SHORT).show());
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
                        userList.add(array.getString(i));
                    }
                    runOnUiThread(() -> adapter.notifyDataSetChanged());
                } catch (JSONException ex) {
                    onFailure(call, new IOException(ex));
                }
            }
        });
    }

    private void showAddUserDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add User");
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        EditText nameEdit = new EditText(this);
        nameEdit.setHint("Username");
        EditText passEdit = new EditText(this);
        passEdit.setHint("Password");
        passEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        EditText limitEdit = new EditText(this);
        limitEdit.setHint("Daily limit");
        limitEdit.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(nameEdit);
        layout.addView(passEdit);
        layout.addView(limitEdit);
        builder.setView(layout);
        builder.setPositiveButton("Save", (dialog, which) -> {
            JSONObject obj = new JSONObject();
            try {
                obj.put("username", nameEdit.getText().toString().trim());
                obj.put("password", passEdit.getText().toString());
                obj.put("daily_limit", Integer.parseInt(limitEdit.getText().toString().trim()));
            } catch (Exception e) {
                android.widget.Toast.makeText(this, "Invalid input", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            RequestBody body = RequestBody.create(obj.toString(), okhttp3.MediaType.get("application/json"));
            ServerApi.get().createUser(body, new okhttp3.Callback() {
                @Override
                public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                    runOnUiThread(() -> android.widget.Toast.makeText(UsersActivity.this, "Failed", android.widget.Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws IOException {
                    response.close();
                    loadUsers();
                }
            });
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void deleteUser(String username) {
        ServerApi.get().deleteUser(username, new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                runOnUiThread(() -> android.widget.Toast.makeText(UsersActivity.this, "Failed", android.widget.Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws IOException {
                response.close();
                loadUsers();
            }
        });
    }
}
