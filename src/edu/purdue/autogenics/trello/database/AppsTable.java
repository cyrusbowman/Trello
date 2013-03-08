package edu.purdue.autogenics.trello.database;

import android.database.sqlite.SQLiteDatabase;

public class AppsTable {
	// Contacts table name
	public static final String TABLE_NAME = "apps";
 
    // Contacts Table Columns names
    public static final String COL_ID = "_id";
    public static final String COL_NAME = "name";
    public static final String PACKAGE_NAME = "package_name";
    public static final String COL_ALLOW_SYNCING = "allow_syncing";
    public static final String COL_LAST_SYNC = "lastsync";
    
    private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME 
    		+ "(" 
            + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," 
            + COL_NAME + " VARCHAR(200),"
            + PACKAGE_NAME + " VARCHAR(200),"
            + COL_ALLOW_SYNCING + " INTEGER,"
            + COL_LAST_SYNC + " VARCHAR(50)"
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
