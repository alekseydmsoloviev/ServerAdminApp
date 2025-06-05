package com.example.serveradminapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.content.SharedPreferences;
import android.content.Context;
import com.example.serveradminapp.databinding.ActivityLoginBinding;

import androidx.annotation.NonNull;
import java.io.IOException;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private EditText usernameEdit;
    private EditText passwordEdit;
    private EditText serverEdit;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleUtil.attach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LocaleUtil.apply(this);
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        usernameEdit = binding.usernameEdit;
        passwordEdit = binding.passwordEdit;
        serverEdit = binding.serverEdit;
        ServerApi.restore(this);
        SharedPreferences prefs = getSharedPreferences("server_api", MODE_PRIVATE);
        serverEdit.setText(prefs.getString("url", ""));
        usernameEdit.setText(prefs.getString("user", ""));
        passwordEdit.setText(prefs.getString("pass", ""));
        Button loginButton = binding.loginButton;

        loginButton.setOnClickListener(v -> {
            String server = serverEdit.getText().toString().trim();
            String username = usernameEdit.getText().toString().trim();
            String password = passwordEdit.getText().toString().trim();

            ServerApi.init(server, username, password);
            ServerApi.get().checkStatus(new okhttp3.Callback() {
                @Override
                public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                    runOnUiThread(() ->
                            android.widget.Toast.makeText(LoginActivity.this, getString(R.string.connection_failed), android.widget.Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws IOException {
                    response.close();
                    if (response.isSuccessful()) {
                        ServerApi.saveCredentials(LoginActivity.this, server, username, password);
                        runOnUiThread(() -> {
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        });
                    } else {
                        runOnUiThread(() ->
                                android.widget.Toast.makeText(LoginActivity.this, getString(R.string.invalid_credentials), android.widget.Toast.LENGTH_SHORT).show());
                    }
                }
            });
        });
    }
}
