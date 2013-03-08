package edu.purdue.autogenics.trello.database;

import android.database.sqlite.SQLiteDatabase;

public class NewCardsTable {
	// Table name
	public static final String TABLE_NAME = "new_cards";
 
	 //Table Columns names
    public static final String COL_ID = "_id"; //Not a trello id, just random UDID
    public static final String COL_OWNER = "owner";
    public static final String COL_NAME = "name";
    public static final String COL_DESC = "desc";
    public static final String COL_LIST_ID = "list_id";
    public static final String COL_DATE = "date";
    
    private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME 
    		+ "(" 
            + COL_ID + " VARCHAR(50) PRIMARY KEY," 
            + COL_OWNER + " VARCHAR(200)," //Package name of where the data is stored
            + COL_NAME + " VARCHAR(100),"
            + COL_DESC + " VARCHAR(500),"
            + COL_LIST_ID + " VARCHAR(50),"
            + COL_DATE + " VARCHAR(50)"
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
