package com.vip.trello.database;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHandler extends SQLiteOpenHelper {
	 
    // All Static variables
    // Database Version
    private static final int DATABASE_VERSION = 1;
 
    // Database Name
    private static final String DATABASE_NAME = "trello.db";
 
    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }    
    
    // Creating tables
    @Override
    public void onCreate(SQLiteDatabase db) {    	
        BoardsTable.onCreate(db);
        ListsTable.onCreate(db);
        CardsTable.onCreate(db);
        ListenersTable.onCreate(db);
        
        BoardsOwnerFinder.onCreate(db);
    }
 
    // Upgrading tables
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    	BoardsTable.onUpgrade(db, oldVersion, newVersion);
    	ListsTable.onUpgrade(db, oldVersion, newVersion);
    	CardsTable.onUpgrade(db, oldVersion, newVersion);
    	ListenersTable.onUpgrade(db, oldVersion, newVersion);
    }
}
