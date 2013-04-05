package edu.purdue.autogenics.trello.internet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.purdue.autogenics.trello.contentprovider.DatabaseContentProvider;
import edu.purdue.autogenics.trello.database.BoardsTable;
import edu.purdue.autogenics.trello.database.DatabaseHandler;
import edu.purdue.autogenics.trello.database.ListsTable;
import edu.purdue.autogenics.trello.database.NewBoardsTable;
import edu.purdue.autogenics.trello.database.NewListsTable;
import edu.purdue.autogenics.trello.service.TrelloService;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

public class BoardsHandler {
	TrelloService AppContext;
	private String BoardsURI = DatabaseContentProvider.CONTENT_URI + DatabaseContentProvider.BOARDS_PATH;
	private String ListsURI = DatabaseContentProvider.CONTENT_URI + DatabaseContentProvider.LISTS_PATH;
	private String CardsURI = DatabaseContentProvider.CONTENT_URI + DatabaseContentProvider.CARDS_PATH;
	
	private String OrganizationID = null;
	private String TrelloKey = null;
	private String TrelloToken = null;
	//private String OrganizationID = "51147a54abe641a06d005a7c";
	//private String TrelloKey = "b1ae1192adda1b5b61563d30d7ab403b";
	//private String TrelloToken = "943ec9f8bf5f4093635737cb7b39ed74cf5b1f71d28a22805151dfeeb70191ef";
	
	public BoardsHandler(TrelloService applicationContext) {
		if(applicationContext == null){
			Log.d("BoardsHandler", "null context");
		} else {
			AppContext = applicationContext;
		}
	}
	
	private void loadKeys(){
		if(TrelloToken == null){
			Log.d("CardsHandler", "Loading Keys");
			SharedPreferences prefs2 = PreferenceManager.getDefaultSharedPreferences(AppContext);
			String apiKey = prefs2.getString("apiKey", "null");
			String token = prefs2.getString("token", "null");
			String orgoId = prefs2.getString("organizationId", "null");
			OrganizationID = orgoId.trim();
			TrelloKey = apiKey.trim();
			TrelloToken = token.trim(); 
		}
	}
	public void Sync(){
		//Do boards need synced?
		Boolean needSynced = false;
		
		//Check NewBoardsTable
		DatabaseHandler dbHandler = new DatabaseHandler(AppContext);
		SQLiteDatabase database = dbHandler.getWritableDatabase();
		String[] columns = { NewBoardsTable.COL_ID };
				
		Cursor cursor = database.query(NewBoardsTable.TABLE_NAME, columns, null, null,null, null, null);
	    if(cursor.moveToFirst()) {
	    	needSynced = true;
	    }
	    cursor.close();
	    dbHandler.close();

	    //Check BoardsTable
	    if(needSynced == false){
	    	//Check Boards for unsynced
	    	Uri boardsUri = Uri.parse(BoardsURI);
			String[] boardColumns = { BoardsTable.COL_TRELLO_ID, BoardsTable.COL_SYNCED };
			String where = BoardsTable.COL_SYNCED + " = 1";
			Cursor cursor2 = AppContext.getContentResolver().query(boardsUri, boardColumns, where, null, null);
			if(cursor2.moveToFirst()) {
		    	needSynced = true;
		    }
            cursor2.close();
	    }
	    
	    if(needSynced){
	    	//Begin sync of boards
	    	syncBoards();
	    }
	}
	
	
	public void test1(){
		//Simulate first start of rock app
		//Add boards to the newboardstable, then sync boards
		SharedPreferences prefs2 = PreferenceManager.getDefaultSharedPreferences(AppContext);
		String apiKey = prefs2.getString("apiKey", "null");
		String token = prefs2.getString("token", "null");
		String orgoId = prefs2.getString("organizationId", "null");
		
		OrganizationID = orgoId.trim();
		TrelloKey = apiKey.trim();
		TrelloToken = token.trim(); 
		
		DatabaseHandler dbHandler = new DatabaseHandler(AppContext);
		SQLiteDatabase database = dbHandler.getWritableDatabase();
		
		ContentValues values = new ContentValues();
		values.put(NewBoardsTable.COL_ID, "111-111");
		values.put(NewBoardsTable.COL_NAME, "RockAppTest1");
		values.put(NewBoardsTable.COL_DESC, "DescTest1");
		values.put(NewBoardsTable.COL_NAME_KEYWORD, "RockAppTest1");
		values.put(NewBoardsTable.COL_DESC_KEYWORD, "DescTest1");
		values.put(NewBoardsTable.COL_OWNER, "com.vip.test_for_trello");
		
		database.insert(NewBoardsTable.TABLE_NAME, null, values);
		
		database.close();
		dbHandler.close();
		syncBoards();
	}

	public void pushBoard(TrelloBoard newBoard){
		//Find out if should go in BoardsTable or NewBoardsTable
		
		DatabaseHandler dbHandler = new DatabaseHandler(AppContext);
		SQLiteDatabase database = dbHandler.getWritableDatabase();
		
		if(newBoard.getId().contains("-")){
			//Belongs in NewBoardsTable
			//Is it in the table already??
			Log.d("pushBoard", "Belongs in NewBoardsTable");

			Boolean found = false;
	  		String[] columns2 = { NewBoardsTable.COL_ID };
	  		String where = NewBoardsTable.COL_ID + " = '" + newBoard.getId() + "'";
	  		Cursor cursor2 = database.query(NewBoardsTable.TABLE_NAME, columns2, where, null,null, null, null);
	  	    if(cursor2.moveToFirst()) {
	  	    	found = true;
	  	    }
	  	    cursor2.close();
	  	    
	  	    if(found){
	  	    	//Update it
	  	    	Log.d("pushBoard", "Found already in NewBoardsTable updating");
	  	    	ContentValues values = new ContentValues();
				values.put(NewBoardsTable.COL_NAME, newBoard.getName());
				values.put(NewBoardsTable.COL_DESC, newBoard.getDesc());
				values.put(NewBoardsTable.COL_NAME_KEYWORD, newBoard.getNameKeyword());
				values.put(NewBoardsTable.COL_DESC_KEYWORD, newBoard.getDescKeyword());
				values.put(NewBoardsTable.COL_OWNER, newBoard.getOwner());
				database.update(NewBoardsTable.TABLE_NAME, values, where, null);
	  	    } else {
	  	    	//Add it
	  	    	Log.d("pushBoard", "Adding to NewBoardsTable");
	  	    	ContentValues values = new ContentValues();
				values.put(NewBoardsTable.COL_ID, newBoard.getId());
				values.put(NewBoardsTable.COL_NAME, newBoard.getName());
				values.put(NewBoardsTable.COL_DESC, newBoard.getDesc());
				values.put(NewBoardsTable.COL_NAME_KEYWORD, newBoard.getNameKeyword());
				values.put(NewBoardsTable.COL_DESC_KEYWORD, newBoard.getDescKeyword());
				values.put(NewBoardsTable.COL_OWNER, newBoard.getOwner());
	  	    	database.insert(NewBoardsTable.TABLE_NAME, null, values);
	  	    }
		} else {
			//Belongs in BoardsTable, update it
			Log.d("pushBoard", "Updating in NewBoardsTable");
			Uri boardURI = Uri.parse(BoardsURI + "/" + newBoard.getId());
			ContentValues values = new ContentValues();
			values.put(BoardsTable.COL_SYNCED, 0);
			values.put(BoardsTable.COL_NAME, newBoard.getName());
			values.put(BoardsTable.COL_DESC, newBoard.getDesc());
			values.put(BoardsTable.COL_OWNER, newBoard.getOwner());
			AppContext.getContentResolver().update(boardURI, values, null, null);
		}
		dbHandler.close();
	}
	
	public void deleteBoard(TrelloBoard theBoard){
		//Find out if should go in BoardsTable or NewBoardsTable
		
		DatabaseHandler dbHandler = new DatabaseHandler(AppContext);
		SQLiteDatabase database = dbHandler.getWritableDatabase();
		
		if(theBoard.getId().contains("-")){
			//Belongs in NewBoardsTable
			//Is it in the table already??
			Log.d("deleteBoard", "Deleting from NewBoardsTable");
			
			//Delete from NewBoardsTable
			String whereClause = NewBoardsTable.COL_ID + " = '" + theBoard.getId() + "'";
			database.delete(NewBoardsTable.TABLE_NAME, whereClause, null);
		} else {
			//Belongs in BoardsTable, delete it, remove from trello
			Log.d("deleteBoard", "Deleting from BoardsTable");
			Uri boardURI = Uri.parse(BoardsURI + "/" + theBoard.getId());
			AppContext.getContentResolver().delete(boardURI, null, null);
			
			DeleteBoardOnTrello(theBoard);
		}
		database.close();
		dbHandler.close();
	}
	public boolean needsSync(){
		//Check if any boards added to NewBoardsTable
		Boolean anyAdded = false;
		
		DatabaseHandler dbHandler = new DatabaseHandler(AppContext);
		SQLiteDatabase database = dbHandler.getWritableDatabase();
  		String[] columns = { NewBoardsTable.COL_ID };
  		Cursor cursor = database.query(NewBoardsTable.TABLE_NAME, columns, null, null,null, null, null);
  		if(cursor.moveToFirst()){
  			anyAdded = true;
  		}
  	    cursor.close();
  	    database.close();
  	    dbHandler.close();
  	    
		return anyAdded;
	}
	public void syncBoards(){
		//Any in NewBoardsTable with keywords??
		
		List<TrelloBoard> dbBoardsWkeywords = new  ArrayList<TrelloBoard>();
		
		DatabaseHandler dbHandler = new DatabaseHandler(AppContext);
		SQLiteDatabase database = dbHandler.getWritableDatabase();
		String[] columns = { NewBoardsTable.COL_ID, NewBoardsTable.COL_NAME, NewBoardsTable.COL_DESC, NewBoardsTable.COL_NAME_KEYWORD, NewBoardsTable.COL_DESC_KEYWORD, NewBoardsTable.COL_OWNER };
		String where = "(" + NewBoardsTable.COL_NAME_KEYWORD + " != '' AND " + NewBoardsTable.COL_NAME_KEYWORD + " IS NOT NULL) ";
		where = where + " OR (" + NewBoardsTable.COL_DESC_KEYWORD + " != '' AND " + NewBoardsTable.COL_DESC_KEYWORD + " IS NOT NULL)";
		
		//Get boards from NewBoardsTable that have keywords
		Cursor cursor = database.query(NewBoardsTable.TABLE_NAME, columns, where, null,null, null, null);
	    while(cursor.moveToNext()) {
	    	TrelloBoard newBoard = new TrelloBoard(cursor.getString(cursor.getColumnIndex(NewBoardsTable.COL_ID)), cursor.getString(cursor.getColumnIndex(NewBoardsTable.COL_NAME)), cursor.getString(cursor.getColumnIndex(NewBoardsTable.COL_DESC)), cursor.getString(cursor.getColumnIndex(NewBoardsTable.COL_NAME_KEYWORD)), cursor.getString(cursor.getColumnIndex(NewBoardsTable.COL_DESC_KEYWORD)), cursor.getString(cursor.getColumnIndex(NewBoardsTable.COL_OWNER)), 0);
	    	dbBoardsWkeywords.add(newBoard);
	    }
	    cursor.close();
	    
	    List<TrelloBoard> boardsList = null;
	    //Get boards from trello
	    boardsList = GetBoards();
	    
	    if(dbBoardsWkeywords.isEmpty() == false){
	    	//Match each trello board to NewBoardsTable with keywords
	    	Iterator<TrelloBoard> it = boardsList.iterator();
	    	while(it.hasNext())
	    	{
	    		TrelloBoard trelloBoard = it.next();
	    	    Iterator<TrelloBoard> it2 = dbBoardsWkeywords.iterator();
	    		Boolean match = false;
	    		TrelloBoard dbBoard = null;
		    	while(it2.hasNext() && match == false)
		    	{
		    		dbBoard = it2.next();
		    		if(dbBoard.getNameKeyword() != null && dbBoard.getNameKeyword().contentEquals(trelloBoard.getName())){
		    			//Match on name
				    	Log.d("syncBoards", "Found Match on name:" + trelloBoard.getName());
		    			match = true;
		    		}
		    		if(dbBoard.getDescKeyword() != null && dbBoard.getDescKeyword().contentEquals(trelloBoard.getDesc())){
		    			//Match on keyword
				    	Log.d("syncBoards", "Found Match on desc:" + trelloBoard.getDesc());
		    			match = true;
		    		}
		    	}
		    	if(match){
		    		//Remove from NewBoardsTable
			    	Log.d("syncBoards", "Removing from NewBoardsTable");

		    		String whereClause = NewBoardsTable.COL_ID + " = '" + dbBoard.getId() + "'";
					database.delete(NewBoardsTable.TABLE_NAME, whereClause, null);
		    		
		    		//If not already there, Add Matching Trello Board to BoardsTable in db with trello id
					Boolean exists = false;
					String[] columns2 = { BoardsTable.COL_TRELLO_ID };
					whereClause = BoardsTable.COL_TRELLO_ID + " = '" + trelloBoard.getId() + "'";
			  		Cursor cursor2 = database.query(BoardsTable.TABLE_NAME, columns2, whereClause, null,null, null, null);
			  	    if(cursor2.moveToFirst()) {
			  	    	exists = true;
			  	    }
			  	    if(exists == false){
				    	Log.d("syncBoards", "Doesn't exist in db, adding:" + trelloBoard.getName());

			  	    	//Add matching trello board to boardstable with trello id
			    		ContentValues values = new ContentValues();
			    		values.put(BoardsTable.COL_SYNCED, 1);
						values.put(BoardsTable.COL_TRELLO_ID, trelloBoard.getId());
						values.put(BoardsTable.COL_NAME, trelloBoard.getName());
						values.put(BoardsTable.COL_DESC, trelloBoard.getDesc());
						values.put(BoardsTable.COL_LISTS_LAST_SYNC, "");
						values.put(BoardsTable.COL_OWNER, dbBoard.getOwner());
						AppContext.getContentResolver().insert(Uri.parse(BoardsURI), values);
						
						
				    	Log.d("syncBoards", "Changing Id's on lists" + trelloBoard.getName());
						//Change any id's in WatchListsTable, NewListsTable, ListTable should be fine
						//NewListsTable
				  		String where2 = NewListsTable.COL_BOARD_ID + " = '" + dbBoard.getId() + "'";
			  	    	ContentValues values2 = new ContentValues();
						values2.put(NewListsTable.COL_BOARD_ID, trelloBoard.getId());
						database.update(NewListsTable.TABLE_NAME, values2, where2, null);		
						//ListsTable
						String where3 = ListsTable.COL_BOARD_ID + " = '" + dbBoard.getId() + "'";
			  	    	ContentValues values3 = new ContentValues();
						values3.put(ListsTable.COL_BOARD_ID, trelloBoard.getId());
						database.update(ListsTable.TABLE_NAME, values3, where3, null);	
						//TODO add Change any id's  WatchListsTable *********
						
						
						//Send back data with intent to owner
						Intent sendIntent = new Intent();
						Bundle extras = new Bundle();
						extras.putString("request", "updateData");
						extras.putString("type", "board");
						extras.putString("id", dbBoard.getId());
						extras.putString("newId", trelloBoard.getId());
						extras.putString("name", trelloBoard.getName());
						extras.putString("desc", trelloBoard.getDesc());
						sendIntent.setAction(Intent.ACTION_SEND);
						sendIntent.setPackage(dbBoard.getOwner());
						sendIntent.putExtras(extras);
						AppContext.startService(sendIntent);
			  	    } else {
				    	Log.d("syncBoards", "This board already exists in our database, no need to add");
			  	    }
					
					//Remove from both arrays
					it2.remove(); //dbBoardsWkeywords.remove(dbBoard);
		    		it.remove(); //boardsList.remove(trelloBoard);
		    	}
	    	}
	    }
	    
	    //Check if anything left in NewBoardsTable, add them to trello
	    List<TrelloBoard> dbNewBoards = new  ArrayList<TrelloBoard>();
  		String[] columns2 = { NewBoardsTable.COL_ID,  NewBoardsTable.COL_NAME,  NewBoardsTable.COL_DESC,  NewBoardsTable.COL_NAME_KEYWORD,  NewBoardsTable.COL_DESC_KEYWORD, NewBoardsTable.COL_OWNER };
  		Cursor cursor2 = database.query(NewBoardsTable.TABLE_NAME, columns2, null, null,null, null, null);
  	    while(cursor2.moveToNext()) {
  	    	//Set as not synced
  	    	TrelloBoard newBoard = new TrelloBoard(cursor2.getString(cursor2.getColumnIndex(NewBoardsTable.COL_ID)), cursor2.getString(cursor2.getColumnIndex(NewBoardsTable.COL_NAME)), cursor2.getString(cursor2.getColumnIndex(NewBoardsTable.COL_DESC)), cursor2.getString(cursor2.getColumnIndex(NewBoardsTable.COL_NAME_KEYWORD)), cursor2.getString(cursor2.getColumnIndex(NewBoardsTable.COL_DESC_KEYWORD)), cursor2.getString(cursor2.getColumnIndex(NewBoardsTable.COL_OWNER)), 0);
	    	dbNewBoards.add(newBoard);
  	    }
  	    cursor2.close();
  	    
  	    //Add all boards left in NewBoardsTable to Trello (Didn't match keywords so need to add them, or just don't have keywords)
  	    if(dbNewBoards.isEmpty() == false){
  	    	Iterator<TrelloBoard> it = dbNewBoards.iterator();
	    	while(it.hasNext())
	    	{
	    		TrelloBoard dbBoard = it.next();
	    	    //Add board to trello and get Trello Id
	    	    Log.d("BoardsHandler", "New board sent to trello, name:" + dbBoard.getName());
	    	    String newId = AddBoardToTrello(dbBoard);
	    	    
				if (newId != null) {
					//Insert with new id in BoardsTable
					
					//Add to BoardsTable in db with trello id
		    		ContentValues values = new ContentValues();
		    		values.put(BoardsTable.COL_SYNCED, 1); //Now synced
					values.put(BoardsTable.COL_TRELLO_ID, newId);
					values.put(BoardsTable.COL_NAME, dbBoard.getName());
					values.put(BoardsTable.COL_DESC, dbBoard.getDesc());
					values.put(BoardsTable.COL_LISTS_LAST_SYNC, "");
					values.put(BoardsTable.COL_OWNER, dbBoard.getOwner());
					AppContext.getContentResolver().insert(Uri.parse(BoardsURI), values);

					//Remove from NewBoardsTable
		    		String whereClause = NewBoardsTable.COL_ID + " = '" + dbBoard.getId() + "'";
					database.delete(NewBoardsTable.TABLE_NAME, whereClause, null);
					
					//Update all lists that have old id
					//NewListsTable
			  		String where2 = NewListsTable.COL_BOARD_ID + " = '" + dbBoard.getId() + "'";
		  	    	ContentValues values2 = new ContentValues();
					values2.put(NewListsTable.COL_BOARD_ID, newId);
					database.update(NewListsTable.TABLE_NAME, values2, where2, null);		
					//ListsTable
					String where3 = ListsTable.COL_BOARD_ID + " = '" + dbBoard.getId() + "'";
		  	    	ContentValues values3 = new ContentValues();
					values3.put(ListsTable.COL_BOARD_ID, newId);
					database.update(ListsTable.TABLE_NAME, values3, where3, null);	
					//TODO add Change any id's  WatchListsTable *********
					
					//Add to waiting list, wait for owner to update
					AppContext.addWaiting(dbBoard.getId());
					
					//Send back new id to owner
					Intent sendIntent = new Intent();
					Bundle extras = new Bundle();
					extras.putString("request", "updateId");
					extras.putString("type", "board");
					extras.putString("id", dbBoard.getId());
					extras.putString("newId", newId);
					sendIntent.setAction(Intent.ACTION_SEND);
					sendIntent.setPackage(dbBoard.getOwner());
					sendIntent.putExtras(extras);
					AppContext.startService(sendIntent);
				}
	    	}
  	    }
  	    
  	    //Sync new trello boards into this db
  	    //Loop through boards in BoardsTable and compare name/desc and synced with Trello boards in boardsList
  	    
  	    //Get BoardsTable boards
  	    List<TrelloBoard> dbBoards = new  ArrayList<TrelloBoard>();
  	    Uri boardsUri = Uri.parse(BoardsURI);
		String[] boardColumns = { BoardsTable.COL_TRELLO_ID, BoardsTable.COL_SYNCED, BoardsTable.COL_OWNER, BoardsTable.COL_NAME, BoardsTable.COL_DESC };
		Cursor cursor3 = AppContext.getContentResolver().query(boardsUri, boardColumns, null, null, null);
		while(cursor3.moveToNext()) {
			TrelloBoard newBoard = new TrelloBoard(cursor3.getString(cursor3.getColumnIndex(BoardsTable.COL_TRELLO_ID)), cursor3.getString(cursor3.getColumnIndex(BoardsTable.COL_NAME)), cursor3.getString(cursor3.getColumnIndex(BoardsTable.COL_DESC)), null, null, cursor3.getString(cursor3.getColumnIndex(BoardsTable.COL_OWNER)), cursor3.getInt(cursor3.getColumnIndex(BoardsTable.COL_SYNCED)));
			dbBoards.add(newBoard);
	    }
		cursor3.close();
		if(dbBoards.isEmpty() == false){
			Iterator<TrelloBoard> it = dbBoards.iterator();
	    	while(it.hasNext())
	    	{
	    		TrelloBoard dbBoard = it.next();
	    		Iterator<TrelloBoard> it2 = boardsList.iterator();
	    		Boolean found = false;
		    	while(it2.hasNext() && found == false)
		    	{
		    		TrelloBoard trelloBoard = it2.next();
		    		
		    		//If same board
			    	if(trelloBoard.getId().contentEquals(dbBoard.getId())){
			    		//Match on names
				    	Boolean matchName = false;
				    	if(dbBoard.getName() == null) dbBoard.setName("");
				    	if(trelloBoard.getName() == null) trelloBoard.setName("");
				    	if(dbBoard.getName().contentEquals(trelloBoard.getName())) matchName = true;
				    	Log.d("Matching names", "dbName:" + dbBoard.getName() + " trelloName:" + trelloBoard.getName());
				    	Log.d("Match?", Boolean.toString(matchName));
				    	
				    	//Match on desc
				    	Boolean matchDesc = false;
				    	if(dbBoard.getDesc() == null) dbBoard.setDesc("");
				    	if(trelloBoard.getDesc() == null) trelloBoard.setDesc("");
				    	if(dbBoard.getDesc().contentEquals(trelloBoard.getDesc())) matchDesc = true;
				    	Log.d("Matching desc", "dbDesc:" + dbBoard.getDesc() + " trelloDesc:" + trelloBoard.getDesc());
				    	Log.d("Match?", Boolean.toString(matchDesc));
				    	
				    	if(matchName && matchDesc && dbBoard.getSynced() == 0){
				    		//Set database to synced, no visible changes on trello
				    		Uri boardURI = Uri.parse(BoardsURI + "/" + trelloBoard.getId());
							ContentValues values = new ContentValues();
							values.put(BoardsTable.COL_SYNCED, 1);
							AppContext.getContentResolver().update(boardURI, values, null, null);
				    	} else if(matchName == false || matchDesc == false){
				    		if(dbBoard.getSynced() == 0){
				    			//Local change hasn't been synced to trello, update trello to this
				    			Uri boardURI = Uri.parse(BoardsURI + "/" + trelloBoard.getId());
								ContentValues values = new ContentValues();
								values.put(BoardsTable.COL_SYNCED, 1);
								AppContext.getContentResolver().update(boardURI, values, null, null);
								
								//Update trello to this
								UpdateBoardOnTrello(trelloBoard); //TODO isnt this suppose to be dbBoard??
				    		} else {
				    			//Update local to the change that exists on trello
				    			Uri boardURI = Uri.parse(BoardsURI + "/" + trelloBoard.getId());
								ContentValues values = new ContentValues();
								values.put(BoardsTable.COL_NAME, trelloBoard.getName());
								values.put(BoardsTable.COL_DESC, trelloBoard.getDesc());
								AppContext.getContentResolver().update(boardURI, values, null, null);
				    			
								//Update data on owner
								Intent sendIntent = new Intent();
								Bundle extras = new Bundle();
								extras.putString("request", "updateData");
								extras.putString("type", "board");
								extras.putString("id", dbBoard.getId());
								extras.putString("name", trelloBoard.getName());
								extras.putString("desc", trelloBoard.getDesc());
								sendIntent.setAction(Intent.ACTION_SEND);
								sendIntent.setPackage(dbBoard.getOwner());
								sendIntent.putExtras(extras);
								AppContext.startService(sendIntent);
				    		}
				    	}
				    	
				    	//Remove this board from both
				    	it.remove(); //dbBoards.remove(dbBoard);
				    	it2.remove(); //boardsList.remove(trelloBoard);
				    	found = true;
			    	}
		    	}
	    	}
		}
		//TODO Boards left in dbBoards are the ones that have been deleted
		//Maybe want archived board too, not just open in requst to trello? to avoid no response problems??
		//Notify owner and delete them, their lists, and cards
		if(dbBoards.isEmpty() == false){
			Iterator<TrelloBoard> it = dbBoards.iterator();
	    	while(it.hasNext())
	    	{
	    		//Delete local to match trello, and notify owner of removal
	    		TrelloBoard dbBoard = it.next();
    			//Uri boardURI = Uri.parse(BoardsURI + "/" + dbBoard.getId());
				//AppContext.getContentResolver().delete(boardURI, null, null);
				
				//TODO delete all children (lists/cards) of this boardId
	
				//TODO THIS ISN'T REALLY WHAT WE WANT?, WILL ALWAYS HAVE OTHER BOARDS
				//Notify owner of deletion
				/*Intent sendIntent = new Intent();
				Bundle extras = new Bundle();
				extras.putString("request", "deleteData");
				extras.putString("type", "board");
				extras.putString("id", dbBoard.getId());
				sendIntent.setAction(Intent.ACTION_SEND);
				sendIntent.setPackage(dbBoard.getOwner());
				sendIntent.putExtras(extras);
				AppContext.startService(sendIntent);*/
	    	}
		}
  	    dbHandler.close();
  	    
  	    //Check watchlists??
	}
	
	private String AddBoardToTrello(TrelloBoard theBoard){
		loadKeys();
		
		HttpClient client = new DefaultHttpClient();
		HttpPost post = new HttpPost(
				"https://api.trello.com/1/boards");
		List<BasicNameValuePair> results = new ArrayList<BasicNameValuePair>();
		
		results.add(new BasicNameValuePair("key",TrelloKey));
		results.add(new BasicNameValuePair("token",TrelloToken));
		results.add(new BasicNameValuePair("idOrganization",OrganizationID));
		
		if(theBoard.getName() != null) results.add(new BasicNameValuePair("name", theBoard.getName()));
		if(theBoard.getDesc() != null) results.add(new BasicNameValuePair("desc", theBoard.getDesc()));
		
		String newId = null;
		try {
			post.setEntity(new UrlEncodedFormEntity(
					results));
		} catch (UnsupportedEncodingException e) {
			// Auto-generated catch block
			Log.e("BoardsHandler","An error has occurred", e);
		}
		try {
			HttpResponse response = client
					.execute(post);
			String result = "";
			try {
				// Error here if no internet
				InputStream is = response.getEntity()
						.getContent(); 
				result = convertStreamToString(is);
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			Log.d("BoardsHandler", "Add Response:" + result);
			Log.d("BoardsHandler", "*********** TODO LOOK FOR DATE" + result);
			JSONObject json;
			
			try {
				json = new JSONObject(result);
				newId = json.getString("id");
			} catch (JSONException e) {
				e.printStackTrace();
			}
		} catch (ClientProtocolException e) {
			Log.e("BoardsHandler","client protocol exception", e);
		} catch (IOException e) {
			Log.e("BoardsHandler", "io exception", e);
		}
		
		return newId; //Also need to return date
	}
	
	private String UpdateBoardOnTrello(TrelloBoard theBoard){
		loadKeys();
		
		HttpClient client = new DefaultHttpClient();

		HttpPut put = new HttpPut("https://api.trello.com/1/boards/" + theBoard.getId());

		List<BasicNameValuePair> results = new ArrayList<BasicNameValuePair>();
		results.add(new BasicNameValuePair("key",TrelloKey));
		results.add(new BasicNameValuePair("token",TrelloToken));
		
		//results.add(new BasicNameValuePair("idOrganization",OrganizationID));
		results.add(new BasicNameValuePair("name", theBoard.getName()));
		results.add(new BasicNameValuePair("desc", theBoard.getDesc()));

		try {
			String result = "";
			try {
				put.setEntity(new UrlEncodedFormEntity(
						results));
				HttpResponse response = client
						.execute(put);
				InputStream is = response.getEntity()
						.getContent(); // Error here if
										// no internet
				result = convertStreamToString(is);
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			Log.d("BoardsHandler", "Update Response:" + result);
		} catch (Exception e) {
			// Auto-generated catch block
			Log.e("Log Thread",
					"client protocol exception", e);
		}
		return null; //TODO return date?
	}
	
	private String DeleteBoardOnTrello(TrelloBoard theBoard){
		loadKeys();
		
		HttpClient client = new DefaultHttpClient();

		HttpPut put = new HttpPut("https://api.trello.com/1/boards/" + theBoard.getId());

		List<BasicNameValuePair> results = new ArrayList<BasicNameValuePair>();
		results.add(new BasicNameValuePair("key",TrelloKey));
		results.add(new BasicNameValuePair("token",TrelloToken));
		
		results.add(new BasicNameValuePair("closed", "true"));

		try {
			String result = "";
			try {
				put.setEntity(new UrlEncodedFormEntity(
						results));
				HttpResponse response = client
						.execute(put);
				InputStream is = response.getEntity()
						.getContent(); // Error here if
										// no internet
				result = convertStreamToString(is);
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			Log.d("BoardsHandler", "Update Response:" + result);
		} catch (Exception e) {
			// Auto-generated catch block
			Log.e("Log Thread",
					"client protocol exception", e);
		}
		return null; //TODO return date?
	}
	
	private List<TrelloBoard> GetBoards(){
		loadKeys();
		
		//No date field on boards, update based on name/desc change
		List<TrelloBoard> boardsList = new  ArrayList<TrelloBoard>();
		String url = "https://api.trello.com/1/organizations/" + OrganizationID + "/boards?key=" + TrelloKey + "&token=" + TrelloToken + "&filter=open&fields=name,desc";
		
		HttpResponse response = getData(url);
		String result = "";
		try {
			//Error here if no Internet
			InputStream is = response.getEntity().getContent(); 
			result = convertStreamToString(is);
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Log.d("Result:", result);
		JSONArray json = null;
		try {
			json = new JSONArray(result);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		// Loop through boards from trello
		for (int i = 0; i < json.length(); i++) {
			
			JSONObject jsonBoard = null;
			
			String trello_id = "";
			String name = "";
			String desc = "";
			
			try {
				
				jsonBoard = json.getJSONObject(i);
				trello_id = jsonBoard.getString("id");
				name = jsonBoard.getString("name");
				desc = jsonBoard.getString("desc");
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			TrelloBoard newBoard = new TrelloBoard(trello_id, name, desc, null, null, null, null);
			boardsList.add(newBoard);
		}
		return boardsList;
	}

	public static String convertStreamToString(InputStream inputStream)
			throws IOException {
		if (inputStream != null) {
			Writer writer = new StringWriter();

			char[] buffer = new char[1024];
			try {
				Reader reader = new BufferedReader(new InputStreamReader(
						inputStream, "UTF-8"), 1024);
				int n;
				while ((n = reader.read(buffer)) != -1) {
					writer.write(buffer, 0, n);
				}
			} finally {
				inputStream.close();
			}
			return writer.toString();
		} else {
			return "";
		}
	}
	public HttpResponse getData(String url) {
		HttpResponse response = null;
		try {
			HttpClient client = new DefaultHttpClient();
			HttpGet request = new HttpGet();
			request.setURI(new URI(url));
			response = client.execute(request);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return response;
	}
}
