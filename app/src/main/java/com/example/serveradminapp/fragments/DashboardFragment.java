package com.example.serveradminapp.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.serveradminapp.GaugeView;
import com.example.serveradminapp.LocaleUtil;
import com.example.serveradminapp.R;
import com.example.serveradminapp.ServerApi;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class DashboardFragment extends Fragment {

    private TextView serverStateText;
    private TextView messages24hText;
    private TextView messagesTotalText;
    private GaugeView cpuGauge;
    private GaugeView memGauge;
    private GaugeView netGauge;
    private GaugeView diskGauge;
    private WebSocket metricsSocket;
    private final Runnable reconnectRunnable = this::connectMetrics;
    private final Handler statusHandler = new Handler(Looper.getMainLooper());
    private final Runnable statusTimeout =
            () -> serverStateText.setText(getString(R.string.status_stop));

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        LocaleUtil.attach(context);
        LocaleUtil.apply(requireActivity());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        serverStateText = view.findViewById(R.id.server_state_text);
        serverStateText.setText(getString(R.string.status_stop));
        messages24hText = view.findViewById(R.id.messages_24h_text);
        messagesTotalText = view.findViewById(R.id.messages_total_text);
        cpuGauge = view.findViewById(R.id.cpu_gauge);
        memGauge = view.findViewById(R.id.mem_gauge);
        netGauge = view.findViewById(R.id.net_gauge);
        diskGauge = view.findViewById(R.id.disk_gauge);
        connectMetrics();
        loadUsage();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUsage();
    }

    @Override
    public void onDestroyView() {
        statusHandler.removeCallbacks(reconnectRunnable);
        if (metricsSocket != null) metricsSocket.cancel();
        super.onDestroyView();
    }

    private final WebSocketListener metricsListener = new WebSocketListener() {
        @Override
        public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
            metricsSocket = webSocket;
            setStatusWork();
        }

        @Override
        public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
            metricsSocket = null;
            setStatusStop();
        }

        @Override
        public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, Response response) {
            metricsSocket = null;
            setStatusStop();
        }

        @Override
        public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
            try {
                JSONObject obj = new JSONObject(text);
                if (obj.has("cpu")) {
                    final int day = obj.optInt("day_total");
                    final int total = obj.optInt("total");
                    final int cpu = (int) Math.round(obj.optDouble("cpu"));
                    final int mem = (int) Math.round(obj.optDouble("memory"));
                    final int net = (int) Math.round(obj.optDouble("network"));
                    final int disk = (int) Math.round(obj.optDouble("disk"));
                    setStatusWork();
                    requireActivity().runOnUiThread(() -> {
                        if (day > 0)
                            messages24hText.setText(getString(R.string.messages_24h_value, day));
                        if (total > 0)
                            messagesTotalText.setText(getString(R.string.messages_total_value, total));
                        cpuGauge.setPercent(cpu);
                        memGauge.setPercent(mem);
                        netGauge.setPercent(net);
                        diskGauge.setPercent(disk);
                    });
                } else if (obj.has("snapshot")) {
                    JSONObject snap = obj.getJSONObject("snapshot");
                    updateUsageFromJson(snap);
                }
            } catch (JSONException ignored) {
            }
        }
    };

    private void setStatusWork() {
        statusHandler.removeCallbacks(statusTimeout);
        statusHandler.removeCallbacks(reconnectRunnable);
        statusHandler.post(() -> serverStateText.setText(getString(R.string.status_work)));
        statusHandler.postDelayed(statusTimeout, 15000);
    }

    private void setStatusStop() {
        statusHandler.removeCallbacks(statusTimeout);
        statusHandler.post(() -> serverStateText.setText(getString(R.string.status_stop)));
        statusHandler.postDelayed(reconnectRunnable, 5000);
    }

    private void connectMetrics() {
        if (metricsSocket != null) {
            metricsSocket.cancel();
            metricsSocket = null;
        }
        metricsSocket = ServerApi.get().connectMetrics(metricsListener);
        if (metricsSocket == null) {
            setStatusStop();
        }
    }

    private void loadUsage() {
        ServerApi.get().fetchUsage(new Callback() {
            @Override
            public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                requireActivity().runOnUiThread(() -> {
                    messages24hText.setText(R.string.messages_24h);
                    messagesTotalText.setText(R.string.messages_total);
                });
            }

            @Override
            public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws IOException {
                if (!response.isSuccessful()) {
                    response.close();
                    return;
                }
                String body = response.body().string();
                response.close();
                try {
                    JSONObject obj = new JSONObject(body);
                    updateUsageFromJson(obj);
                } catch (JSONException ignored) {
                }
            }
        });
    }

    private void updateUsageFromJson(JSONObject obj) throws JSONException {
        int dayCount = obj.optInt("day_total", obj.optInt("day"));
        int totalCount = obj.optInt("total", dayCount);
        final int count24h = dayCount;
        final int countTotal = totalCount;
        requireActivity().runOnUiThread(() -> {
            messages24hText.setText(getString(R.string.messages_24h_value, count24h));
            messagesTotalText.setText(getString(R.string.messages_total_value, countTotal));
        });
    }
}
