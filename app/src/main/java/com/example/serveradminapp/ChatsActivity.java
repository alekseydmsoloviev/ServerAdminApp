package com.example.serveradminapp;

import android.os.Bundle;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

public class ChatsActivity extends AppCompatActivity {

    private final ArrayList<JSONObject> chatList = new ArrayList<>();
    private ChatAdapter adapter;
    private String sortField = "created_at";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ServerApi.restore(this);
        if (ServerApi.get() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_chats);

        ListView listView = findViewById(R.id.chats_list);
        Spinner sortSpinner = findViewById(R.id.sort_spinner);
        View refreshButton = findViewById(R.id.refresh_button);

        adapter = new ChatAdapter();
        listView.setAdapter(adapter);

        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"User", "Created", "Last msg", "Messages"});
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(sortAdapter);
        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: sortField = "username"; break;
                    case 1: sortField = "created_at"; break;
                    case 2: sortField = "last_message"; break;
                    case 3: sortField = "message_count"; break;
                }
                sortAndUpdate();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        refreshButton.setOnClickListener(v -> loadChats());

        listView.setOnItemClickListener((p,v,pos,id)->{
            JSONObject obj = chatList.get(pos);
            Intent intent = new Intent(ChatsActivity.this, ChatDetailActivity.class);
            intent.putExtra("session_id", obj.optString("session_id"));
            startActivity(intent);
        });

        loadChats();
    }

    private void loadChats() {
        ServerApi.get().listSessions(new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                runOnUiThread(() -> chatList.clear());
            }

            @Override
            public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws IOException {
                if (!response.isSuccessful()) { response.close(); return; }
                String body = response.body().string();
                response.close();
                try {
                    JSONArray arr = new JSONArray(body);
                    chatList.clear();
                    for (int i=0;i<arr.length();i++) {
                        chatList.add(arr.getJSONObject(i));
                    }
                    runOnUiThread(() -> sortAndUpdate());
                } catch (JSONException ex) {
                    // ignore
                }
            }
        });
    }

    private void sortAndUpdate() {
        Collections.sort(chatList, new Comparator<JSONObject>() {
            final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            private long parseDate(String s) {
                try { Date d = fmt.parse(s); return d==null?0:d.getTime(); } catch (ParseException e) { return 0; }
            }
            @Override
            public int compare(JSONObject o1, JSONObject o2) {
                if ("message_count".equals(sortField)) {
                    return Integer.compare(o2.optInt(sortField), o1.optInt(sortField));
                }
                if ("username".equals(sortField)) {
                    return o1.optString("username").compareToIgnoreCase(o2.optString("username"));
                }
                long t1 = parseDate(o1.optString(sortField));
                long t2 = parseDate(o2.optString(sortField));
                return Long.compare(t2, t1);
            }
        });
        adapter.notifyDataSetChanged();
    }

    private class ChatAdapter extends ArrayAdapter<JSONObject> {
        ChatAdapter() { super(ChatsActivity.this, 0, chatList); }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);
            }
            JSONObject obj = getItem(position);
            TextView sid = convertView.findViewById(R.id.chat_title);
            TextView user = convertView.findViewById(R.id.chat_username);
            TextView created = convertView.findViewById(R.id.chat_created);
            View deleteBtn = convertView.findViewById(R.id.delete_button);


            String title = obj.optString("title");
            if (title == null || title.isEmpty() || "null".equals(title)) {
                title = obj.optString("session_id");
            }
            sid.setText(title);
            user.setText(obj.optString("username"));
            created.setText(obj.optString("created_at"));

            deleteBtn.setOnClickListener(v -> {
                String id = obj.optString("session_id");
                ServerApi.get().deleteSession(id, new okhttp3.Callback() {
                    @Override public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {}
                    @Override public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws IOException {
                        response.close();
                        if (response.isSuccessful()) {
                            runOnUiThread(() -> {
                                chatList.remove(obj);
                                notifyDataSetChanged();
                            });
                        }
                    }
                });
            });

            return convertView;
        }
    }
}
