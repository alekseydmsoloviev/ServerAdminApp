package com.example.serveradminapp;

import android.os.Bundle;
import android.content.Intent;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class ChatDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ServerApi.restore(this);
        if (ServerApi.get() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_chat_detail);

        String sessionId = getIntent().getStringExtra("session_id");
        if (sessionId == null) {
            finish();
            return;
        }

        TextView messagesView = findViewById(R.id.messages_text);
        loadMessages(sessionId, messagesView);
    }

    private void loadMessages(String id, TextView view) {
        ServerApi.get().getSession(id, new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                runOnUiThread(() -> view.setText("Failed"));
            }

            @Override
            public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws IOException {
                if (!response.isSuccessful()) { response.close(); return; }
                String body = response.body().string();
                response.close();
                try {
                    JSONObject obj = new JSONObject(body);
                    JSONArray arr = obj.optJSONArray("messages");
                    StringBuilder sb = new StringBuilder();
                    if (arr != null) {
                        for (int i=0;i<arr.length();i++) {
                            JSONObject m = arr.getJSONObject(i);
                            sb.append(m.optString("username"))
                              .append(" ("+m.optString("role")+"):")
                              .append('\n')
                              .append(m.optString("content"))
                              .append("\n\n");
                        }
                    }
                    final String text = sb.toString();
                    runOnUiThread(() -> view.setText(text));
                } catch (JSONException ex) {
                    runOnUiThread(() -> view.setText("Error"));
                }
            }
        });
    }
}
