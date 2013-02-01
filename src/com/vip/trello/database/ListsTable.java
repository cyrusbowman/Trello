package com.vip.trello.database;

import android.database.sqlite.SQLiteDatabase;

public class ListsTable {
	// Table name
	public static final String TABLE_NAME = "lists";
 
    // Table Columns names
    public static final String TRELLO_ID = "_id"; //How to tell if new board or not
    public static final String DATE = "date";
    public static final String SYNCED = "synced";
    
    private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME 
    		+ " (" 
            + TRELLO_ID + " VARCHAR(50) PRIMARY KEY," 
    		+ DATE + " VARCHAR(50),"
            + SYNCED + " INTEGER,"
    		+ ")";
    
    // Creating Tables
    public static void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE); 
    }
 
    // Upgrading database
    public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        // Create tables again
        onCreate(db);
    }
}
