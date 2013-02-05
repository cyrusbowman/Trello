package com.vip.trello.database;

import android.database.sqlite.SQLiteDatabase;

public class ListenersTable {
	// Table name
	public static final String TABLE_NAME = "listeners";
 
    // Table Columns names
	public static final String ID = "_id";
    public static final String TRELLO_ID = "trello_id";
    public static final String TABLE = "tablename";
    public static final String URI = "uri";
    
    private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME 
    		+ " (" 
            + ID + " INTEGER PRIMARY KEY AUTOINCREMENT," 
            + TRELLO_ID + " VARCHAR(50),"
    		+ TABLE + " VARCHAR(10)," //ie. boards, lists, cards
            + URI + " VARCHAR(150)"
    		+ ")";
    private static final String CREATE_INDEX = "CREATE INDEX listeners_trello_id ON " + TABLE_NAME
    		+ " (" + TRELLO_ID + ")";
    
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
