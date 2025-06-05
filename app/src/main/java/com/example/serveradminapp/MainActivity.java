package com.example.serveradminapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.serveradminapp.fragments.ChatsFragment;
import com.example.serveradminapp.fragments.DashboardFragment;
import com.example.serveradminapp.fragments.ModelsFragment;
import com.example.serveradminapp.fragments.SettingsFragment;
import com.example.serveradminapp.fragments.UsersFragment;

public class MainActivity extends AppCompatActivity {

    private View usersIndicator;
    private View modelsIndicator;
    private View chatsIndicator;
    private View settingsIndicator;
    private View homeIndicator;

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
        setContentView(R.layout.activity_main);
        ServerApi.get().startMetricsSocket();

        View usersButton = findViewById(R.id.users_button);
        View modelsButton = findViewById(R.id.models_button);
        View chatsButton = findViewById(R.id.chats_button);
        View settingsButton = findViewById(R.id.settings_button);
        View homeButton = findViewById(R.id.home_button);
        usersIndicator = findViewById(R.id.users_indicator);
        modelsIndicator = findViewById(R.id.models_indicator);
        chatsIndicator = findViewById(R.id.chats_indicator);
        settingsIndicator = findViewById(R.id.settings_indicator);
        homeIndicator = findViewById(R.id.home_indicator);

        usersButton.setOnClickListener(v -> switchFragment(new UsersFragment(), usersIndicator));
        modelsButton.setOnClickListener(v -> switchFragment(new ModelsFragment(), modelsIndicator));
        chatsButton.setOnClickListener(v -> switchFragment(new ChatsFragment(), chatsIndicator));
        settingsButton.setOnClickListener(v -> switchFragment(new SettingsFragment(), settingsIndicator));
        homeButton.setOnClickListener(v -> switchFragment(new DashboardFragment(), homeIndicator));

        if (savedInstanceState == null) {
            switchFragment(new DashboardFragment(), homeIndicator);
        }
    }

    private void switchFragment(Fragment fragment, View indicator) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
        updateIndicators(indicator);
    }

    private void updateIndicators(View active) {
        usersIndicator.setVisibility(View.GONE);
        modelsIndicator.setVisibility(View.GONE);
        chatsIndicator.setVisibility(View.GONE);
        settingsIndicator.setVisibility(View.GONE);
        homeIndicator.setVisibility(View.GONE);
        if (active != null) {
            active.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        if (isFinishing() && !isChangingConfigurations()) {
            ServerApi.get().stopMetricsSocket();
        }
        super.onDestroy();
    }
}
