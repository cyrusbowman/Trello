package com.vip.trello.database;

import android.database.sqlite.SQLiteDatabase;

public class ListenersTable {
	// Table name
	public static final String TABLE_NAME = "listeners";
 
    // Table Columns names
	private static final String COL_ID = "_id";
    public static final String COL_TRELLO_ID = "trello_id";
    public static final String COL_TABLE = "tablename";
    public static final String COL_PACKAGE = "package";
    
    private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME 
    		+ " (" 
            + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," 
            + COL_TRELLO_ID + " VARCHAR(50),"
    		+ COL_TABLE + " VARCHAR(10)," //ie. boards, lists, cards
            + COL_PACKAGE + " VARCHAR(200)"
    		+ ")";
    private static final String CREATE_INDEX = "CREATE INDEX listeners_trello_id ON " + TABLE_NAME
    		+ " (" + COL_TRELLO_ID + ")";
    
    // Creating Tables and indexes
    public static void onCreate(SQLiteDatabase db) {    	
        db.execSQL(CREATE_TABLE);
        db.execSQL(CREATE_INDEX);
        //TODO may want to create index for trello_id/uri used for deleting listener
        //Also need to add that lookup for delete in DatabaseContentProvider
    }
 
    // Upgrading database
    public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        // Create tables again
        onCreate(db);
    }
}
