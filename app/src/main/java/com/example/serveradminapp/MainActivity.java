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

        View usersButton = findViewById(R.id.users_button);
        View modelsButton = findViewById(R.id.models_button);
        View chatsButton = findViewById(R.id.chats_button);
        View settingsButton = findViewById(R.id.settings_button);

        usersButton.setOnClickListener(v -> switchFragment(new UsersFragment()));
        modelsButton.setOnClickListener(v -> switchFragment(new ModelsFragment()));
        chatsButton.setOnClickListener(v -> switchFragment(new ChatsFragment()));
        settingsButton.setOnClickListener(v -> switchFragment(new SettingsFragment()));

        if (savedInstanceState == null) {
            switchFragment(new DashboardFragment());
        }
    }

    private void switchFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}
