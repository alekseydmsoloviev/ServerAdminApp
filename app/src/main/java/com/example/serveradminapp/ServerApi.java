package com.example.serveradminapp;

import android.util.Base64;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.MediaType;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * Simple API helper for communicating with the admin server.
 */
public class ServerApi {
    private static ServerApi instance;

    private final String baseUrl;
    private final String authHeader;
    private final OkHttpClient client = new OkHttpClient();

    private ServerApi(String baseUrl, String username, String password) {
        String url = baseUrl;
        if (!url.startsWith("http")) {
            url = "http://" + url;
        }
        // When running inside an emulator, localhost refers to the device
        // itself. Replace it with the special 10.0.2.2 host so that the user
        // can simply enter "localhost:port".
        url = url.replace("://localhost", "://10.0.2.2")
                   .replace("://127.0.0.1", "://10.0.2.2");
        this.baseUrl = url;
        String creds = username + ":" + password;
        this.authHeader = "Basic " + Base64.encodeToString(creds.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
    }

    public static void init(String baseUrl, String username, String password) {
        instance = new ServerApi(baseUrl, username, password);
    }

    public static ServerApi get() {
        return instance;
    }

    private Request.Builder baseRequest(String path) {
        return new Request.Builder()
                .url(baseUrl + path)
                .addHeader("Authorization", authHeader);
    }

    public void checkStatus(@NonNull Callback callback) {
        Request request = baseRequest("/admin/api/status").get().build();
        client.newCall(request).enqueue(callback);
    }

    public void listUsers(@NonNull Callback callback) {
        Request request = baseRequest("/admin/api/users").get().build();
        client.newCall(request).enqueue(callback);
    }

    public void createUser(@NonNull RequestBody body, @NonNull Callback callback) {
        Request request = baseRequest("/admin/api/users").post(body).build();
        client.newCall(request).enqueue(callback);
    }

    public void deleteUser(@NonNull String username, @NonNull Callback callback) {
        Request request = baseRequest("/admin/api/users/" + username).delete().build();
        client.newCall(request).enqueue(callback);
    }

    public void listModels(@NonNull Callback callback) {
        Request request = baseRequest("/admin/api/models").get().build();
        client.newCall(request).enqueue(callback);
    }

    public void availableModels(@NonNull Callback callback) {
        Request request = baseRequest("/admin/api/models/available").get().build();
        client.newCall(request).enqueue(callback);
    }

    public void modelVariants(@NonNull String name, @NonNull Callback callback) {
        Request request = baseRequest("/admin/api/models/" + name + "/variants").get().build();
        client.newCall(request).enqueue(callback);
    }

    public void installModel(@NonNull String name, @NonNull Callback callback) {
        Request request = baseRequest("/admin/api/models/" + name + "/install").post(RequestBody.create(new byte[0])).build();
        client.newCall(request).enqueue(callback);
    }

    public void installModel(@NonNull String name, @NonNull String variant, @NonNull Callback callback) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("variant", variant);
        } catch (JSONException ignored) {}
        RequestBody body = RequestBody.create(obj.toString(), MediaType.get("application/json"));
        Request request = baseRequest("/admin/api/models/" + name + "/install").post(body).build();
        client.newCall(request).enqueue(callback);
    }

    public void deleteModel(@NonNull String name, @NonNull Callback callback) {
        Request request = baseRequest("/admin/api/models/" + name).delete().build();
        client.newCall(request).enqueue(callback);
    }

    public void loadConfig(@NonNull Callback callback) {
        Request request = baseRequest("/admin/api/config").get().build();
        client.newCall(request).enqueue(callback);
    }

    public void updateConfig(@NonNull RequestBody body, @NonNull Callback callback) {
        Request request = baseRequest("/admin/api/config").post(body).build();
        client.newCall(request).enqueue(callback);
    }

    public void restartServer(@NonNull Callback callback) {
        Request request = baseRequest("/admin/api/restart").post(RequestBody.create(new byte[0])).build();
        client.newCall(request).enqueue(callback);
    }

    public void fetchLogs(@NonNull Callback callback) {
        Request request = baseRequest("/admin/api/logs").get().build();
        client.newCall(request).enqueue(callback);
    }

    public void fetchUsage(@NonNull Callback callback) {
        Request request = baseRequest("/admin/api/usage").get().build();
        client.newCall(request).enqueue(callback);
    }

    public WebSocket connectMetrics(@NonNull WebSocketListener listener) {
        String url = baseUrl.replaceFirst("^http", "ws") + "/admin/ws";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", authHeader)
                .build();
        return client.newWebSocket(request, listener);
    }

    /** Utility to parse JSON array to String[] */
    public static String[] jsonArrayToStringArray(JSONArray array) throws JSONException {
        String[] result = new String[array.length()];
        for (int i = 0; i < array.length(); i++) {
            result[i] = array.getString(i);
        }
        return result;
    }
}
