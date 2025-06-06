package com.example.serveradminapp;

import android.os.Bundle;
import android.content.Intent;
import android.widget.TextView;
import android.view.View;
import android.content.Context;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;

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
        TextView titleView = findViewById(R.id.chat_title);
        TextView modelView = findViewById(R.id.chat_model);
        messagesView.setLineSpacing(0f, 1.4f);
        messagesView.setMovementMethod(LinkMovementMethod.getInstance());
        messagesView.setText(R.string.loading);

        View refreshButton = findViewById(R.id.refresh_button);
        View backButton = findViewById(R.id.back_button);

        refreshButton.setOnClickListener(v -> loadMessages(sessionId, messagesView, titleView, modelView));
        backButton.setOnClickListener(v -> finish());

        loadMessages(sessionId, messagesView, titleView, modelView);
    }

    private void loadMessages(String id, TextView view, TextView titleView, TextView modelView) {
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
                    String sessionModel = "";
                    StringBuilder sb = new StringBuilder();
                    if (arr != null && arr.length() > 0) {
                        JSONObject first = arr.getJSONObject(0);
                        sessionModel = first.optString("model");
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
                    final String model = sessionModel;
                    runOnUiThread(() -> {
                        setTitle(title);
                        titleView.setText(title);
                        modelView.setText(getString(R.string.model_label, model));
                        view.setText(markdownToSpanned(text));
                    });
                } catch (JSONException ex) {
                    runOnUiThread(() -> view.setText(getString(R.string.error)));
                }
            }
        });
    }

    private Spanned markdownToSpanned(String md) {
        String html = Html.escapeHtml(md);

        html = html.replaceAll("\\*\\*(.+?)\\*\\*", "<h3>$1</h3>");
        html = html.replaceAll("__(.+?)__", "<h3>$1</h3>");

        html = html.replaceAll("~~(.+?)~~", "<strike>$1</strike>");

        html = html.replaceAll("(?<!\\*)\\*(.+?)\\*(?!\\*)", "<i>$1</i>");
        html = html.replaceAll("(?<!_)_(.+?)_(?!_)", "<i>$1</i>");

        html = html.replaceAll("`(.+?)`", "<tt>$1</tt>");

        html = html.replaceAll("\\[(.+?)\\]\\((.+?)\\)", "<a href=\"$2\">$1</a>");
        html = html.replaceAll("\\[(https?://[^\\]]+)\\]", "<a href=\"$1\">$1</a>");

        html = html.replaceAll("(?m)^\\s*[\\*-]\\s+(.+)$", "&#8226; $1");

        html = html.replace("\n", "<br>");

        return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
    }

}
