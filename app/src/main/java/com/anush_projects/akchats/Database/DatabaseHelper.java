package com.anush_projects.akchats.Database;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.anush_projects.akchats.Models.Message;
import com.anush_projects.akchats.utils.FirebaseUtils;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "AKChats.db";
    private static final int DATABASE_VERSION = 2;

    private static final String TABLE_MESSAGES = "messages";

    private static final String COLUMN_ID = "id";
    private static final String COLUMN_MESSAGE_ID = "message_id";
    private static final String COLUMN_CHAT_ID = "chat_id";
    private static final String COLUMN_SENDER_ID = "sender_id";
    private static final String COLUMN_RECEIVER_ID = "receiver_id";
    private static final String COLUMN_TEXT = "text";
    private static final String COLUMN_IMAGE = "image";
    private static final String COLUMN_TYPE = "type";
    private static final String COLUMN_TIMESTAMP = "timestamp";

    private static final String CREATE_TABLE_MESSAGES =
            "CREATE TABLE " + TABLE_MESSAGES + "(" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_MESSAGE_ID + " TEXT UNIQUE, " +
                    COLUMN_CHAT_ID + " TEXT, " +
                    COLUMN_SENDER_ID + " TEXT, " +
                    COLUMN_RECEIVER_ID + " TEXT, " +
                    COLUMN_TEXT + " TEXT, " +
                    COLUMN_IMAGE + " BLOB, " +
                    COLUMN_TYPE + " TEXT, " +
                    COLUMN_TIMESTAMP + " INTEGER, " +
                    "status TEXT DEFAULT 'sent'" +
                    ");";


    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_MESSAGES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_MESSAGES + " ADD COLUMN status TEXT DEFAULT 'sent';");
        }
    }

    public long insertMessage(Message message, Context context) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_MESSAGE_ID, message.getMessageId());
        values.put(COLUMN_CHAT_ID, message.getChatId());
        values.put(COLUMN_SENDER_ID, message.getSenderId());
        values.put(COLUMN_RECEIVER_ID, message.getReceiverId());
        values.put(COLUMN_TEXT, message.getText());
        values.put(COLUMN_TYPE, message.getMessageType());
        values.put(COLUMN_TIMESTAMP, message.getTimestamp());

        if (message.getImageBytes() != null) {
            values.put(COLUMN_IMAGE, message.getImageBytes());
        }

        long result = -1;
        try {
            result = db.insertWithOnConflict(TABLE_MESSAGES, null, values, SQLiteDatabase.CONFLICT_IGNORE);
            if (result != -1) {
                Intent broadcastIntent = new Intent("com.anush_projects.NEW_MESSAGE");
                broadcastIntent.putExtra("chatId", message.getChatId());
                context.sendBroadcast(broadcastIntent);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            db.close();
        }
        return result;
    }


    // Get Messages by Chat ID
    public List<Message> getMessagesByChatId(String chatId) {
        List<Message> messages = new ArrayList<>();

        // Null check
        if (chatId == null || chatId.isEmpty()) {
            return messages;
        }

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_MESSAGES,
                null,
                COLUMN_CHAT_ID + " = ?",
                new String[]{chatId},
                null,
                null,
                COLUMN_TIMESTAMP + " ASC"
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                Message message = new Message();
                message.setMessageId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE_ID)));
                message.setChatId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CHAT_ID)));
                message.setSenderId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SENDER_ID)));
                message.setReceiverId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RECEIVER_ID)));
                message.setText(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TEXT)));
                message.setMessageType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE)));
                message.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)));

                byte[] imageBytes = cursor.getBlob(cursor.getColumnIndexOrThrow(COLUMN_IMAGE));
                if (imageBytes != null) {
                    message.setImageBytes(imageBytes);
                }

                messages.add(message);
            }
            cursor.close();
        }

        db.close();
        return messages;
    }

    public boolean isMessageExists(String messageId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_MESSAGES,
                new String[]{COLUMN_MESSAGE_ID},
                COLUMN_MESSAGE_ID + " = ?",
                new String[]{messageId},
                null,
                null,
                null
        );
        boolean exists = (cursor != null && cursor.getCount() > 0);
        if (cursor != null) {
            cursor.close();
        }
        db.close();
        return exists;
    }

    public void updateMessageStatus(String messageId, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("status", status);
        db.update(TABLE_MESSAGES, values, COLUMN_MESSAGE_ID + "=?", new String[]{messageId});
        db.close();
    }

    public List<String> getChattedUserIds() {
        List<String> userIds = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT DISTINCT sender_id FROM messages WHERE sender_id != ? UNION SELECT DISTINCT receiver_id FROM messages WHERE receiver_id != ?",
                new String[]{FirebaseUtils.getCurrentUserId(), FirebaseUtils.getCurrentUserId()});

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String userId = cursor.getString(0);
                userIds.add(userId);
            }
            cursor.close();
        }
        return userIds;
    }


    public void deleteMessagesByChatId(String chatId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_MESSAGES, COLUMN_CHAT_ID + " = ?", new String[]{chatId});
        db.close();
    }

    public void clearAllChats() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("messages", null, null); // Clear all messages
        db.close();
    }
}
