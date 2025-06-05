package com.example.serveradminapp;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;

import java.util.ArrayList;
import java.util.List;

public class AccountDbHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "accounts.db";
    private static final int DB_VERSION = 1;

    public AccountDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE accounts (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "server TEXT NOT NULL," +
                "username TEXT NOT NULL," +
                "password TEXT NOT NULL," +
                "last_used INTEGER NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS accounts");
        onCreate(db);
    }

    public void saveAccount(String server, String username, String password) {
        SQLiteDatabase db = getWritableDatabase();
        long now = System.currentTimeMillis();
        Cursor c = db.query("accounts", new String[]{"id"}, "server=? AND username=?", new String[]{server, username}, null, null, null);
        if (c.moveToFirst()) {
            long id = c.getLong(0);
            ContentValues values = new ContentValues();
            values.put("password", password);
            values.put("last_used", now);
            db.update("accounts", values, "id=?", new String[]{String.valueOf(id)});
        } else {
            ContentValues values = new ContentValues();
            values.put("server", server);
            values.put("username", username);
            values.put("password", password);
            values.put("last_used", now);
            db.insert("accounts", null, values);
        }
        c.close();
    }

    public List<Account> loadAccounts() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query("accounts", null, null, null, null, null, "last_used DESC");
        List<Account> list = new ArrayList<>();
        while (c.moveToNext()) {
            Account a = new Account();
            a.id = c.getLong(c.getColumnIndexOrThrow("id"));
            a.server = c.getString(c.getColumnIndexOrThrow("server"));
            a.username = c.getString(c.getColumnIndexOrThrow("username"));
            a.password = c.getString(c.getColumnIndexOrThrow("password"));
            a.lastUsed = c.getLong(c.getColumnIndexOrThrow("last_used"));
            list.add(a);
        }
        c.close();
        return list;
    }
}
