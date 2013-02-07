package com.vip.trello.internet;

import com.vip.trello.contentprovider.DatabaseContentProvider;
import com.vip.trello.database.BoardsTable;
import com.vip.trello.database.ListenersTable;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

public class BoardsHandler {
	Context AppContext;
	private String BoardsURI = DatabaseContentProvider.CONTENT_URI + DatabaseContentProvider.BOARDS_PATH;
		
	public BoardsHandler(Context applicationContext) {
		if(applicationContext == null){
			Log.d("BoardsHandler", "null context");
		} else {
			AppContext = applicationContext;
		}
	}
	
	public int TrelloReady(){
		//Returns weather Trello account is setup and ready to use
		return 0;
	}
	
	public void handle(Bundle data){
		//String name = data.getString("name");
		//String desc = data.getString("desc");
		
		Log.d("BoardsHandler", "handing data");
		if(data == null){
			Log.d("BoardsHandler", "null data");
		}
		if(data.containsKey("request")){
			if(data.getString("request").contentEquals("push")){
				PushRequest(data);
			}
		}		
	}
	
	private void PushRequest(Bundle data){
		Log.d("BoardsHandler", "Push Request");
		
		String trello_id = data.getString("id");
		String package_name = data.getString("listener");
		
		ContentValues values = new ContentValues();
		Boolean update = false;
		
		//Check if exists in db
		Uri boardUri = Uri.parse(BoardsURI + "/" + trello_id);
		String[] projectionExists = { BoardsTable.COL_TRELLO_ID, BoardsTable.COL_SYNCED };
		Cursor cursor = AppContext.getContentResolver().query(boardUri, projectionExists, null, null, null);
		if (cursor.moveToFirst()) {
			//Exists
			update = true;
			cursor.close();
        }
		
		if(update){
			Log.d("BoardsHandler", "Updating in db");
			//Update in database
			values.put(BoardsTable.COL_SYNCED, 0);
			AppContext.getContentResolver().update(boardUri, values, null, null);
		} else {
			Log.d("BoardsHandler", "Adding to db");
			//Add to database
			values.put(BoardsTable.COL_SYNCED, 0);
			values.put(BoardsTable.COL_TRELLO_ID, trello_id);
			AppContext.getContentResolver().insert(Uri.parse(BoardsURI), values);
			
			//Add listener
			 values = new ContentValues();
			 values.put(ListenersTable.COL_PACKAGE, package_name);
		}
		
		
		//Try read
		String[] projection = { BoardsTable.COL_TRELLO_ID, BoardsTable.COL_SYNCED, BoardsTable.COL_DATE };
		cursor = AppContext.getContentResolver().query(boardUri, projection, null, null, null);
		if (cursor.moveToFirst()) {
            do {
            	String tId;
            	String date;
            	Integer synced;
            	
            	tId = cursor.getString(0);
            	date = cursor.getString(1);
            	synced = cursor.getInt(2);
            	
            	Log.d("BoardsHandler","tID:" + tId);
            	Log.d("BoardsHandler","date:" + date);
            	Log.d("BoardsHandler","synced:" + Integer.toString(synced));
            	
            } while (cursor.moveToNext());
            cursor.close();
        }
		
		//Send back intent for data once online online
		
		
	}

	public String sendBoard(String dataURI){
		//Adds or updates board on Trello
		
		
		
		
		return "NOTHING";
	}
	
}
