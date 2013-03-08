package edu.purdue.autogenics.trello.database;

import android.database.sqlite.SQLiteDatabase;

public class NewBoardsTable {
	// Contacts table name
	public static final String TABLE_NAME = "new_boards";
 
    // Contacts Table Columns names
    public static final String COL_ID = "_id"; //Not a trello id, just random UDID
    public static final String COL_OWNER = "owner";
    public static final String COL_NAME_KEYWORD = "name_keyword";
    public static final String COL_DESC_KEYWORD = "desc_keyword";
    public static final String COL_NAME= "name";
    public static final String COL_DESC = "desc";
    
    private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME 
    		+ "(" 
            + COL_ID + " VARCHAR(50) PRIMARY KEY," 
            + COL_OWNER + " VARCHAR(200)," //Package name of where the data is stored
            + COL_NAME_KEYWORD + " VARCHAR(50),"
            + COL_DESC_KEYWORD + " VARCHAR(50),"
            + COL_NAME+ " VARCHAR(50),"
            + COL_DESC + " VARCHAR(50)"
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
