package com.example.serveradminapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.RequestBody;
import okhttp3.MediaType;

public class UserDetailActivity extends AppCompatActivity {
    private String username;

    private TextView usernameView;
    private TextView adminView;
    private TextView limitView;
    private TextView chatsView;
    private TextView dayView;
    private TextView totalView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ServerApi.restore(this);
        if (ServerApi.get() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_user_detail);

        username = getIntent().getStringExtra("username");
        if (username == null) {
            finish();
            return;
        }

        usernameView = findViewById(R.id.detail_username);
        adminView = findViewById(R.id.detail_admin);
        limitView = findViewById(R.id.detail_limit);
        chatsView = findViewById(R.id.detail_chats);
        dayView = findViewById(R.id.detail_day);
        totalView = findViewById(R.id.detail_total);
        Button editButton = findViewById(R.id.detail_edit);
        Button deleteButton = findViewById(R.id.detail_delete);

        editButton.setOnClickListener(v -> showEditDialog());
        deleteButton.setOnClickListener(v -> confirmDelete());

        loadUser();
    }

    private void loadUser() {
        ServerApi.get().getUser(username, new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(UserDetailActivity.this, "Failed", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws IOException {
                if (!response.isSuccessful()) { response.close(); onFailure(call,new IOException("HTTP"+response.code())); return; }
                String body = response.body().string();
                response.close();
                try {
                    JSONObject obj = new JSONObject(body);
                    updateUi(obj);
                } catch (JSONException ex) {
                    onFailure(call,new IOException(ex));
                }
            }
        });
    }

    private void updateUi(JSONObject obj) {
        runOnUiThread(() -> {
            usernameView.setText(obj.optString("username", username));
            adminView.setText("Admin: " + obj.optBoolean("is_admin"));
            limitView.setText("Limit: " + obj.optInt("daily_limit"));
            dayView.setText("Last 24h: " + obj.optInt("day"));
            totalView.setText("Total: " + obj.optInt("total"));
            JSONArray chats = obj.optJSONArray("chats");
            if (chats != null) {
                StringBuilder sb = new StringBuilder();
                for (int i=0;i<chats.length();i++) {
                    JSONObject c = chats.optJSONObject(i);
                    if (c != null) sb.append(c.optString("session_id")).append("\n");
                }
                chatsView.setText(sb.toString());
            } else {
                chatsView.setText("");
            }
        });
    }

    private void showEditDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit User");
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        EditText nameEdit = new EditText(this);
        nameEdit.setHint("Username");
        nameEdit.setText(username);
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
        builder.setPositiveButton("Save", (d, w) -> {
            JSONObject obj = new JSONObject();
            try {
                obj.put("username", nameEdit.getText().toString().trim());
                if (passEdit.getText().length() > 0) {
                    obj.put("password", passEdit.getText().toString());
                }
                if (limitEdit.getText().length() > 0) {
                    obj.put("daily_limit", Integer.parseInt(limitEdit.getText().toString().trim()));
                }
            } catch (Exception e) {
                Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show();
                return;
            }
            RequestBody body = RequestBody.create(obj.toString(), MediaType.get("application/json"));
            ServerApi.get().updateUser(body, new okhttp3.Callback() {
                @Override
                public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                    runOnUiThread(() -> Toast.makeText(UserDetailActivity.this, "Failed", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws IOException {
                    response.close();
                    if (response.isSuccessful()) {
                        username = nameEdit.getText().toString().trim();
                        loadUser();
                    }
                }
            });
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setMessage("Delete user " + username + "?")
                .setPositiveButton("Delete", (d, w) -> deleteUser())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteUser() {
        ServerApi.get().deleteUser(username, new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(UserDetailActivity.this, "Failed", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws IOException {
                response.close();
                runOnUiThread(() -> {
                    Toast.makeText(UserDetailActivity.this, "Deleted", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }
}
