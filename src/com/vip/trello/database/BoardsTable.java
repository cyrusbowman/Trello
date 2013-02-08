package com.vip.trello.database;

import android.database.sqlite.SQLiteDatabase;

public class BoardsTable {
	// Contacts table name
	public static final String TABLE_NAME = "boards";
 
    // Contacts Table Columns names
    public static final String COL_TRELLO_ID = "_id"; //How to tell if new board or not
    public static final String COL_DATE = "date";
    public static final String COL_SYNCED = "synced";
    public static final String COL_OWNER = "owner";
    public static final String COL_NAME_KEYWORD = "name_keyword";
    public static final String COL_DESC_KEYWORD = "desc_keyword";
    
    private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME 
    		+ "(" 
            + COL_TRELLO_ID + " VARCHAR(50) PRIMARY KEY," 
            + COL_OWNER + " VARCHAR(200)," //Package name of where the data is stored
            + COL_NAME_KEYWORD + " VARCHAR(50),"
            + COL_DESC_KEYWORD + " VARCHAR(50),"
    		+ COL_DATE + " VARCHAR(50),"
            + COL_SYNCED + " INTEGER"
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
