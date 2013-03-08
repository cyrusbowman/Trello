package edu.purdue.autogenics.trello.database;

import android.database.sqlite.SQLiteDatabase;

public class LoginsTable {
	// Table name
	public static final String TABLE_NAME = "logins";
 
    // Table Columns names
    public static final String COL_ID = "_id";
    public static final String COL_NAME = "name";
    public static final String COL_USERNAME = "username";
    public static final String COL_SECRET = "secret";
    public static final String COL_TOKEN = "token";
    public static final String COL_APIKEY = "apikey";
    
    private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME 
    		+ " (" 
            + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," 
    		+ COL_NAME + " VARCHAR(50),"
    		+ COL_USERNAME + " VARCHAR(50),"
    		+ COL_SECRET + " VARCHAR(50),"
    		+ COL_TOKEN + " VARCHAR(50),"
    		+ COL_APIKEY + " VARCHAR(50)"
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
