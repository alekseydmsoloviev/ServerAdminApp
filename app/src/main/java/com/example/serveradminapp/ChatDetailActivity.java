package com.example.serveradminapp;

import android.os.Bundle;
import android.content.Intent;
import android.widget.TextView;
import android.view.View;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class ChatDetailActivity extends AppCompatActivity {
    
    private String sessionId;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleUtil.attach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LocaleUtil.apply(this);
        super.onCreate(savedInstanceState);
        ServerApi.restore(this);
        if (ServerApi.get() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_chat_detail);

        sessionId = getIntent().getStringExtra("session_id");
        if (sessionId == null) {
            sessionId = getIntent().getStringExtra("id");
        }
        if (sessionId == null) {
            finish();
            return;
        }

        TextView messagesView = findViewById(R.id.messages_text);
        messagesView.setText(R.string.loading);

        View refreshButton = findViewById(R.id.refresh_button);
        refreshButton.setOnClickListener(v -> loadMessages(sessionId, messagesView));

        loadMessages(sessionId, messagesView);
    }

    private void loadMessages(String id, TextView view) {
        ServerApi.get().getSession(id, new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                runOnUiThread(() -> view.setText(getString(R.string.failed)));
            }

            @Override
            public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws IOException {

                if (!response.isSuccessful()) {
                    response.close();
                    runOnUiThread(() -> view.setText(getString(R.string.error) + " " + response.code()));
                    return;
                }

                String body = response.body().string();
                response.close();
                try {
                    JSONObject obj = new JSONObject(body);
                    String pageTitle = obj.optString("title", obj.optString("session_id"));
                    JSONArray arr = obj.optJSONArray("messages");
                    StringBuilder sb = new StringBuilder();
                    if (arr != null && arr.length() > 0) {
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject m = arr.getJSONObject(i);
                            sb.append(m.optString("username"))
                              .append(" (")
                              .append(m.optString("role"))
                              .append("):\n")
                              .append(m.optString("content"))
                              .append("\n\n");
                        }
                    } else {
                        sb.append(getString(R.string.no_messages));
                    }
                    final String text = sb.toString();
                    final String title = pageTitle;
                    runOnUiThread(() -> {
                        setTitle(title);
                        view.setText(text);
                    });
                } catch (JSONException ex) {
                    runOnUiThread(() -> view.setText(getString(R.string.error)));
                }
            }
        });
    }

}
