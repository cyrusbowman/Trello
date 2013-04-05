package edu.purdue.autogenics.trello.database;

import android.database.sqlite.SQLiteDatabase;

public class WatchCardsTable {
	// Table name
	public static final String TABLE_NAME = "watch_cards";
 
    // Table Columns names
    public static final String COL_ID = "_id"; //How to tell if new board or not
    public static final String COL_KEYWORD = "keyword";
    public static final String COL_LIST_ID = "list_id";
    public static final String COL_OWNER = "owner";
    
    private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME 
    		+ " (" 
            + COL_ID + " VARCHAR(50) PRIMARY KEY," 
    		+ COL_KEYWORD + " VARCHAR(200),"
            + COL_LIST_ID + " VARCHAR(50),"
            + COL_OWNER + " VARCHAR(200)"
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
