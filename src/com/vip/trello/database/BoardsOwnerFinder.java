package com.vip.trello.database;

import android.database.sqlite.SQLiteDatabase;

public class BoardsOwnerFinder {
	// Table name
	public static final String TABLE_NAME = "boards_ownerfinder";
 
    // Table Columns names
	public static final String COL_ID = "_id";
    public static final String COL_NAME = "name";
    public static final String COL_DESC = "desc";
    public static final String COL_PACKAGE = "package";
    
    private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME 
    		+ " (" 
            + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," 
            + COL_NAME + " VARCHAR(50),"
    		+ COL_DESC + " VARCHAR(150),"
            + COL_PACKAGE + " VARCHAR(200)"
    		+ ")";

    
    // Creating Tables and indexes
    public static void onCreate(SQLiteDatabase db) {    	
        db.execSQL(CREATE_TABLE);
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
