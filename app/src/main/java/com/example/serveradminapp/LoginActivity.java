package com.example.serveradminapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;


import androidx.annotation.NonNull;
import java.io.IOException;


import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {
    private EditText usernameEdit;
    private EditText passwordEdit;
    private EditText serverEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        usernameEdit = findViewById(R.id.username_edit);
        passwordEdit = findViewById(R.id.password_edit);
        serverEdit = findViewById(R.id.server_edit);
        Button loginButton = findViewById(R.id.login_button);

        loginButton.setOnClickListener(v -> {

            String server = serverEdit.getText().toString().trim();
            String username = usernameEdit.getText().toString().trim();
            String password = passwordEdit.getText().toString().trim();

            ServerApi.init(server, username, password);
            ServerApi.get().checkStatus(new okhttp3.Callback() {
                @Override
                public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                    runOnUiThread(() ->
                            android.widget.Toast.makeText(LoginActivity.this, "Connection failed", android.widget.Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws IOException {
                    response.close();
                    if (response.isSuccessful()) {
                        runOnUiThread(() -> {
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        });
                    } else {
                        runOnUiThread(() ->
                                android.widget.Toast.makeText(LoginActivity.this, "Invalid credentials", android.widget.Toast.LENGTH_SHORT).show());
                    }
                }
            });

        });
    }
}
