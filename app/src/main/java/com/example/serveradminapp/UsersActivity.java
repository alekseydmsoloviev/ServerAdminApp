package com.example.serveradminapp;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import org.json.JSONArray;
import org.json.JSONException;
import java.io.IOException;


public class UsersActivity extends AppCompatActivity {

    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users);

        listView = findViewById(R.id.users_list);
        loadUsers();
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
                    String[] users = ServerApi.jsonArrayToStringArray(array);
                    runOnUiThread(() -> {
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(UsersActivity.this,
                                android.R.layout.simple_list_item_1, users);
                        listView.setAdapter(adapter);
                    });
                } catch (JSONException ex) {
                    onFailure(call, new IOException(ex));
                }
            }
        });
    }
}
