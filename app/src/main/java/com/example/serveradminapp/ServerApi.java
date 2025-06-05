package com.example.serveradminapp;

import android.util.Base64;
import android.content.Context;
import android.content.SharedPreferences;

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
    private static final String PREFS = "server_api";
    private static final String KEY_URL = "url";
    private static final String KEY_USER = "user";
    private static final String KEY_PASS = "pass";

    private final String baseUrl;
    private final String authHeader;
    private final OkHttpClient client = new OkHttpClient();

    private WebSocket metricsSocket;
    private boolean metricsOpen = false;
    private final ForwardingWebSocketListener metricsForwarder;

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
        this.metricsForwarder = new ForwardingWebSocketListener(this);
    }

    public static void init(String baseUrl, String username, String password) {
        instance = new ServerApi(baseUrl, username, password);
    }

    /** Persist credentials so the API can be restored if the process is killed. */
    public static void saveCredentials(Context ctx, String baseUrl, String user, String pass) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_URL, baseUrl)
                .putString(KEY_USER, user)
                .putString(KEY_PASS, pass)
                .apply();
    }

    /** Restore previously saved credentials if available. */
    public static void restore(Context ctx) {
        if (instance != null) return;
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String url = prefs.getString(KEY_URL, null);
        String user = prefs.getString(KEY_USER, null);
        String pass = prefs.getString(KEY_PASS, null);
        if (url != null && user != null && pass != null) {
            instance = new ServerApi(url, user, pass);
        }
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

    /** Fetch detailed info for a single user */
    public void getUser(@NonNull String username, @NonNull Callback callback) {
        Request request = baseRequest("/admin/api/users/" + username).get().build();
        client.newCall(request).enqueue(callback);
    }

    public void createUser(@NonNull RequestBody body, @NonNull Callback callback) {
        Request request = baseRequest("/admin/api/users").post(body).build();
        client.newCall(request).enqueue(callback);
    }

    /** Update an existing user with the given JSON body */
    public void updateUser(@NonNull RequestBody body, @NonNull Callback callback) {
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
        Request request = baseRequest("/admin/api/models/" + name + "/install")
                .post(RequestBody.create(new byte[0]))
                .build();
        client.newCall(request).enqueue(callback);
    }

    /**
     * Install a specific model variant by placing the full variant name in the
     * URL path as required by the server.
     */
    public void installModelVariant(@NonNull String variant, @NonNull Callback callback) {
        // Variant already includes the model name and parameter count like
        // "gemma3:12b". Build the URL manually so that the colon is not
        // percent-encoded which would break the endpoint on the server.
        okhttp3.HttpUrl url = okhttp3.HttpUrl.parse(baseUrl).newBuilder()
                .addPathSegments("admin/api/models")
                .addEncodedPathSegment(variant)
                .addPathSegment("install")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", authHeader)
                .post(RequestBody.create(new byte[0]))
                .build();
        client.newCall(request).enqueue(callback);
    }

    public void deleteModel(@NonNull String name, @NonNull Callback callback) {
        Request request = baseRequest("/admin/api/models/" + name).delete().build();
        client.newCall(request).enqueue(callback);
    }

    /** List all chat sessions */
    public void listSessions(@NonNull Callback callback) {
        Request request = baseRequest("/admin/api/sessions").get().build();
        client.newCall(request).enqueue(callback);
    }

    /** Fetch full message history for a single session */
    public void getSession(@NonNull String id, @NonNull Callback callback) {
        Request request = baseRequest("/admin/api/sessions/" + id).get().build();
        client.newCall(request).enqueue(callback);
    }

    /** Delete a chat session */
    public void deleteSession(@NonNull String id, @NonNull Callback callback) {
        Request request = baseRequest("/admin/api/sessions/" + id).delete().build();
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
        Request request;
        try {
            request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", authHeader)
                    .build();
        } catch (IllegalArgumentException e) {
            return null;
        }
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

    /** Listener that forwards all events to a delegate. */
    private static class ForwardingWebSocketListener extends WebSocketListener {
        private volatile WebSocketListener delegate;
        private final ServerApi api;

        ForwardingWebSocketListener(ServerApi api) {
            this.api = api;
        }

        public void setDelegate(WebSocketListener delegate) {
            this.delegate = delegate;
        }

        @Override
        public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
            api.metricsOpen = true;
            if (delegate != null) delegate.onOpen(webSocket, response);
        }

        @Override
        public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
            api.metricsOpen = false;
            if (delegate != null) delegate.onClosed(webSocket, code, reason);
        }

        @Override
        public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, Response response) {
            api.metricsOpen = false;
            if (delegate != null) delegate.onFailure(webSocket, t, response);
        }

        @Override
        public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
            if (delegate != null) delegate.onMessage(webSocket, text);
        }
    }

    /** Start the persistent metrics WebSocket if not already connected. */
    public synchronized void startMetricsSocket() {
        if (metricsSocket != null) return;
        metricsSocket = connectMetrics(metricsForwarder);
    }

    /** Close the persistent metrics WebSocket. */
    public synchronized void stopMetricsSocket() {
        if (metricsSocket != null) {
            metricsSocket.cancel();
            metricsSocket = null;
            metricsOpen = false;
        }
    }

    /** Set the listener to receive metrics events. */
    public void setMetricsListener(WebSocketListener listener) {
        metricsForwarder.setDelegate(listener);
    }

    public boolean isMetricsOpen() {
        return metricsOpen;
    }
}
