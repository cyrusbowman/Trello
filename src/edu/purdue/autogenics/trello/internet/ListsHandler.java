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
import edu.purdue.autogenics.trello.database.CardsTable;
import edu.purdue.autogenics.trello.database.DatabaseHandler;
import edu.purdue.autogenics.trello.database.ListsTable;
import edu.purdue.autogenics.trello.database.NewBoardsTable;
import edu.purdue.autogenics.trello.database.NewCardsTable;
import edu.purdue.autogenics.trello.database.NewListsTable;
import edu.purdue.autogenics.trello.database.WatchCardsTable;
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

public class ListsHandler {
	TrelloService AppContext;
	private String BoardsURI = DatabaseContentProvider.CONTENT_URI + DatabaseContentProvider.BOARDS_PATH;
	private String ListsURI = DatabaseContentProvider.CONTENT_URI + DatabaseContentProvider.LISTS_PATH;
	private String CardsURI = DatabaseContentProvider.CONTENT_URI + DatabaseContentProvider.CARDS_PATH;

	
	private String OrganizationID = null;
	private String TrelloKey = null;
	private String TrelloToken = null;
	
	public ListsHandler(TrelloService applicationContext) {
		if(applicationContext == null){
			Log.d("ListsHandler", "null context");
		} else {
			AppContext = applicationContext;
		}
	}
	
	private void loadKeys(){
		if(TrelloToken == null){
			Log.d("ListsHandler", "Loading Keys");
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
	    	syncLists();
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
		syncLists();
	}

	public void pushList(TrelloList newList){
		//Find out if should go in BoardsTable or NewBoardsTable
		
		DatabaseHandler dbHandler = new DatabaseHandler(AppContext);
		SQLiteDatabase database = dbHandler.getWritableDatabase();
		
		if(newList.getId().contains("-")){
			//Belongs in NewBoardsTable
			//Is it in the table already??
			Log.d("pushList", "Belongs in NewListsTable");

			Boolean found = false;
	  		String[] columns2 = { NewBoardsTable.COL_ID };
	  		String where = NewBoardsTable.COL_ID + " = '" + newList.getId() + "'";
	  		Cursor cursor2 = database.query(NewBoardsTable.TABLE_NAME, columns2, where, null,null, null, null);
	  	    if(cursor2.moveToFirst()) {
	  	    	found = true;
	  	    }
	  	    cursor2.close();
	  	    
	  	    if(found){
	  	    	//Update it
	  	    	Log.d("pushList", "Found already in NewListsTable updating");
	  	    	ContentValues values = new ContentValues();
				values.put(NewListsTable.COL_NAME, newList.getName());
				values.put(NewListsTable.COL_BOARD_ID, newList.getBoardId());
				values.put(NewListsTable.COL_NAME_KEYWORD, newList.getNameKeyword());
				values.put(NewListsTable.COL_OWNER, newList.getOwner());
				database.update(NewListsTable.TABLE_NAME, values, where, null);
	  	    } else {
	  	    	//Add it
	  	    	Log.d("pushList", "Adding to NewListsTable");
	  	    	ContentValues values = new ContentValues();
				values.put(NewListsTable.COL_ID, newList.getId());
				values.put(NewListsTable.COL_NAME, newList.getName());
				values.put(NewListsTable.COL_BOARD_ID, newList.getBoardId());
				values.put(NewListsTable.COL_NAME_KEYWORD, newList.getNameKeyword());
				values.put(NewListsTable.COL_OWNER, newList.getOwner());
	  	    	database.insert(NewListsTable.TABLE_NAME, null, values);
	  	    }
		} else {
			//Belongs in BoardsTable, update it
			Log.d("pushList", "Updating in ListsTable");
			Uri listURI = Uri.parse(ListsURI + "/" + newList.getId());
			ContentValues values = new ContentValues();
			values.put(ListsTable.COL_SYNCED, 0);
			values.put(ListsTable.COL_NAME, newList.getName());
			values.put(ListsTable.COL_BOARD_ID, newList.getBoardId());
			values.put(ListsTable.COL_OWNER, newList.getOwner());
			AppContext.getContentResolver().update(listURI, values, null, null);
		}
		dbHandler.close();
	}
	
	public void deleteList(TrelloList theList){
		//Find out if should go in NewListsTable or ListsTable
		
		DatabaseHandler dbHandler = new DatabaseHandler(AppContext);
		SQLiteDatabase database = dbHandler.getWritableDatabase();
		
		if(theList.getId().contains("-")){
			//Belongs in NewListsTable
			//Is it in the table already??
			Log.d("deleteBoard", "Deleting from NewListsTable");
			
			//Delete from NewListsTable
			String whereClause = NewListsTable.COL_ID + " = '" + theList.getId() + "'";
			database.delete(NewListsTable.TABLE_NAME, whereClause, null);
		} else {
			//Belongs in ListsTable, delete it, remove from trello
			Log.d("deleteBoard", "Deleting from ListsTable");
			Uri listURI = Uri.parse(ListsURI + "/" + theList.getId());
			AppContext.getContentResolver().delete(listURI, null, null);
			
			DeleteListOnTrello(theList);
		}
		database.close();
		dbHandler.close();
	}
	public boolean needsSync(){
		//Check if any boards added to NewBoardsTable
		Boolean anyAdded = false;
		
		DatabaseHandler dbHandler = new DatabaseHandler(AppContext);
		SQLiteDatabase database = dbHandler.getWritableDatabase();
  		String[] columns = { NewListsTable.COL_ID };
  		Cursor cursor = database.query(NewListsTable.TABLE_NAME, columns, null, null,null, null, null);
  		if(cursor.moveToFirst()){
  			anyAdded = true;
  		}
  	    cursor.close();
  	    database.close();
  	    dbHandler.close();
  	    
		return anyAdded;
	}
	public void syncLists(){
		
		DatabaseHandler dbHandler = new DatabaseHandler(AppContext);
		SQLiteDatabase database = dbHandler.getWritableDatabase();
		

		//Get all BoardIds from BoardsTable
		List<String> boardIds = new ArrayList<String>();
		
		String[] columns = { BoardsTable.COL_TRELLO_ID, BoardsTable.COL_NAME };
  	    //Cursor cursor = database.query(BoardsTable.TABLE_NAME, columns, null, null,null, null, null);
	    Cursor cursor = AppContext.getContentResolver().query(Uri.parse(BoardsURI), columns, null, null, null);
	    while(cursor.moveToNext()) {
	    	String id = cursor.getString(cursor.getColumnIndex(BoardsTable.COL_TRELLO_ID));
	    	Log.d("syncLists", "Found board:" + id);
	    	if(boardIds.contains(id) == false){
		    	boardIds.add(id);
	    	}
	    }
	    cursor.close();
	    
	    //Get all trello lists that reside inside our boards
	    List<TrelloList> trelloLists = new  ArrayList<TrelloList>();
	    
	    Iterator<String> idIter = boardIds.iterator();
    	while(idIter.hasNext())
    	{
    		String trelloBoardId = idIter.next();
    		Log.d("syncLists", "Getting lists of board:" + trelloBoardId);
    	    List<TrelloList> joinTrelloLists = GetTrelloLists(trelloBoardId);
    	    trelloLists.addAll(joinTrelloLists);
    	}
    	
    	//Now have all trello lists, compare these with ListsTable and sync
	    
    	//Get ListsTable lists 
  	    List<TrelloList> dbLists = new  ArrayList<TrelloList>();
  	    Uri listsURI = Uri.parse(ListsURI);
		String[] listColumns = { ListsTable.COL_TRELLO_ID, ListsTable.COL_SYNCED, ListsTable.COL_OWNER, ListsTable.COL_NAME, ListsTable.COL_BOARD_ID };
		Cursor cursor3 = AppContext.getContentResolver().query(listsURI, listColumns, null, null, null);
		while(cursor3.moveToNext()) {
	    	TrelloList newList = new TrelloList(cursor3.getString(cursor3.getColumnIndex(ListsTable.COL_TRELLO_ID)), cursor3.getString(cursor3.getColumnIndex(ListsTable.COL_NAME)), cursor3.getString(cursor3.getColumnIndex(ListsTable.COL_BOARD_ID)), null, cursor3.getString(cursor3.getColumnIndex(ListsTable.COL_OWNER)), cursor3.getInt(cursor3.getColumnIndex(ListsTable.COL_SYNCED)));
			dbLists.add(newList);
	    }
		cursor3.close();
		
		
		//Compare lists that have matching trello ids, compare name and synced status and sync if needed
		Iterator<TrelloList> it3 = dbLists.iterator();
    	while(it3.hasNext())
    	{
    		TrelloList dbList = it3.next(); //List from db with currentBoardId as boardId
    		
    		//Loop through trelloLists with this id
    		Iterator<TrelloList> it4 = trelloLists.iterator();
    		Boolean found = false;
	    	while(it4.hasNext() && found == false)
	    	{
	    		TrelloList trelloList = it4.next();

	    		//If same board
		    	if(trelloList.getId().contentEquals(dbList.getId())){
		    		//Match on names
			    	Boolean matchName = false;
			    	if(dbList.getName() == null) dbList.setName("");
			    	if(trelloList.getName() == null) trelloList.setName("");
			    	if(dbList.getName().contentEquals(trelloList.getName())) matchName = true;
			    	Log.d("Matching names", "dbName:" + dbList.getName() + " trelloName:" + trelloList.getName());
			    	Log.d("Match?", Boolean.toString(matchName));
			    	
			    	if(matchName && dbList.getSynced() == 0){
			    		//Set database to synced, no visible changes on trello
			    		Uri listURI = Uri.parse(ListsURI + "/" + trelloList.getId());
						ContentValues values = new ContentValues();
						values.put(ListsTable.COL_SYNCED, 1);
						AppContext.getContentResolver().update(listURI, values, null, null);
			    	} else if(matchName == false){
			    		if(dbList.getSynced() == 0){
			    			//Local change hasn't been synced to trello, update trello to this
			    			Uri listURI = Uri.parse(ListsURI + "/" + trelloList.getId());
							ContentValues values = new ContentValues();
							values.put(ListsTable.COL_SYNCED, 1);
							AppContext.getContentResolver().update(listURI, values, null, null);
							
							//Update trello to this
							UpdateListOnTrello(dbList);
			    		} else {
			    			//Update local to the change that exists on trello
			    			Uri listURI = Uri.parse(ListsURI + "/" + trelloList.getId());
							ContentValues values = new ContentValues();
							values.put(ListsTable.COL_NAME, trelloList.getName());
							//TODO add BoardId if want ability to move lists between boards
							AppContext.getContentResolver().update(listURI, values, null, null);
			    			
							//Update data on owner
							Intent sendIntent = new Intent();
							Bundle extras = new Bundle();
							extras.putString("request", "updateData");
							extras.putString("type", "list");
							extras.putString("id", dbList.getId()); //Should be same as trelloList.getId()
							extras.putString("boardId", trelloList.getBoardId());
							extras.putString("name", trelloList.getName());
							//TODO add BoardId if want ability to move lists between boards
							sendIntent.setAction(Intent.ACTION_SEND);
							sendIntent.setPackage(dbList.getOwner());
							sendIntent.putExtras(extras);
							AppContext.startService(sendIntent);
			    		}
			    	}
			    	
			    	//Remove this board from both
			    	it3.remove(); //dbLists.remove(dbList);
			    	it4.remove(); //trelloLists.remove(trelloList);
			    	found = true;
		    	}
	    	}
    	}
    	
    	//TODO Lists left in dbLists have trello ids that don't match any list on Trello, these were deleted
  		//Maybe want archived board too, not just open in requst to trello? to avoid no response problems??
    	
	    if(dbLists.isEmpty() == false){
	    	//These were deleted, delete them and there cards from db and notify owner
	    	Iterator<TrelloList> it = dbLists.iterator();
  	    	while(it.hasNext())
  	    	{
  	    		//Delete local to match trello, and notify owner of removal
  	    		TrelloList dbList = it.next();
  	    		
      			Uri listURI = Uri.parse(ListsURI + "/" + dbList.getId());
  				AppContext.getContentResolver().delete(listURI, null, null);
  				
  				//TODO delete all children (cards) of this listId
  	
  				//Notify owner of deletion
  				Intent sendIntent = new Intent();
  				Bundle extras = new Bundle();
  				extras.putString("request", "deleteData");
  				extras.putString("type", "list");
  				extras.putString("id", dbList.getId());
  				sendIntent.setAction(Intent.ACTION_SEND);
  				sendIntent.setPackage(dbList.getOwner());
  				sendIntent.putExtras(extras);
  				AppContext.startService(sendIntent);
  	    	}
	    }
	    
	    
	    
		
		//Any in NewListsTable with keywords??
		
		List<TrelloList> dbListsWkeywords = new  ArrayList<TrelloList>();
		String[] NewListsTableColumns = { NewListsTable.COL_ID, NewListsTable.COL_NAME, NewListsTable.COL_BOARD_ID, NewListsTable.COL_NAME_KEYWORD, NewListsTable.COL_OWNER };
		String where = "(" + NewListsTable.COL_NAME_KEYWORD + " != '' AND " + NewBoardsTable.COL_NAME_KEYWORD + " IS NOT NULL)";
		
		//Get boards from NewBoardsTable that have keywords
		Cursor cursor2 = database.query(NewListsTable.TABLE_NAME, NewListsTableColumns, where, null,null, null, null);
	    while(cursor2.moveToNext()) {
	    	TrelloList newList = new TrelloList(cursor2.getString(cursor2.getColumnIndex(NewListsTable.COL_ID)), cursor2.getString(cursor2.getColumnIndex(NewListsTable.COL_NAME)), cursor2.getString(cursor2.getColumnIndex(NewListsTable.COL_BOARD_ID)), cursor2.getString(cursor2.getColumnIndex(NewListsTable.COL_NAME_KEYWORD)), cursor2.getString(cursor2.getColumnIndex(NewListsTable.COL_OWNER)), 1);
	    	dbListsWkeywords.add(newList);
	    }
	    cursor2.close();

	    //Match each lists with keyword with Lists from trello (trelloLists)
	    if(dbListsWkeywords.isEmpty()) Log.d("SyncLists", "None with Keywords");
	    	
	    if(dbListsWkeywords.isEmpty() == false){
	    	Iterator<TrelloList> it4 = trelloLists.iterator();
	    	while(it4.hasNext())
	    	{
	    		TrelloList trelloList = it4.next();
	    		
	    		Iterator<TrelloList> it5 = dbListsWkeywords.iterator();
	    		Boolean match = false;
	    		TrelloList dbList = null;
		    	while(it5.hasNext() && match == false)
		    	{
		    		dbList = it5.next();
		    		if(dbList.getNameKeyword() == null) Log.d("SyncLists", "dbList.getNameKeyword() null");
		    		if(dbList.getNameKeyword() != null && dbList.getNameKeyword().contentEquals(trelloList.getName())){
		    			//Match on name
		    			match = true;
		    		} else {
			    		Log.d("SyncLists", "dbList.getNameKeyword() :" + dbList.getNameKeyword());
			    		Log.d("SyncLists", "trelloList.getName() :" + trelloList.getName());
		    		}
		    	}
		    	if(match){
		    		//Remove from NewListsTable
		    		String whereClause = NewListsTable.COL_ID + " = '" + dbList.getId() + "'";
					database.delete(NewListsTable.TABLE_NAME, whereClause, null);
		    		
		    		//If not already there, Add Matching Trello List to ListsTable in db with trello id
					Boolean exists = false;
					String[] columns3 = { ListsTable.COL_TRELLO_ID };
					whereClause = ListsTable.COL_TRELLO_ID + " = '" + trelloList.getId() + "'";
			  		Cursor cursor4 = database.query(ListsTable.TABLE_NAME, columns3, whereClause, null,null, null, null);
			  	    while(cursor4.moveToFirst()) {
			  	    	exists = true;
			  	    }
			  	    cursor4.close();
			  	    
			  	    if(exists == false){
			  	    	//Add matching trello list to ListsTable with trello id
			    		ContentValues values = new ContentValues();
			    		values.put(ListsTable.COL_SYNCED, 1);
						values.put(ListsTable.COL_TRELLO_ID, trelloList.getId());
						values.put(ListsTable.COL_NAME, trelloList.getName());
						values.put(ListsTable.COL_BOARD_ID, trelloList.getBoardId());
						values.put(ListsTable.COL_CARDS_LAST_SYNC, "");
						values.put(ListsTable.COL_OWNER, dbList.getOwner());
						AppContext.getContentResolver().insert(Uri.parse(ListsURI), values);
						
						//Change any id's in WatchCardsTable, NewCardsTable, CardsTable should be fine
						//NewCardsTable
						ContentValues values2 = new ContentValues();
						values2.put(NewCardsTable.COL_LIST_ID, trelloList.getId());
						String whereClause2 = NewCardsTable.COL_LIST_ID + " = '" + dbList.getId() + "'";
						database.update(NewCardsTable.TABLE_NAME, values2, whereClause2, null);	
						//CardsTable
						String where3 = CardsTable.COL_LIST_ID + " = '" + dbList.getId() + "'";
			  	    	ContentValues values3 = new ContentValues();
						values3.put(CardsTable.COL_LIST_ID, trelloList.getId());
						database.update(CardsTable.TABLE_NAME, values3, where3, null);	
						//WatchCardsTable
						String where4 = WatchCardsTable.COL_LIST_ID + " = '" + dbList.getId() + "'";
			  	    	ContentValues values4 = new ContentValues();
						values4.put(WatchCardsTable.COL_LIST_ID, trelloList.getId());
						database.update(WatchCardsTable.TABLE_NAME, values4, where4, null);	
						
						//TODO add send back data with intent to OWNER *******
						Intent sendIntent = new Intent();
						Bundle extras = new Bundle();
						extras.putString("request", "updateData");
						extras.putString("type", "list");
						extras.putString("id", dbList.getId());
						extras.putString("newId", trelloList.getId()); //TODO add this field in updateData on RockApp
						extras.putString("name", trelloList.getName());
						extras.putString("boardId", trelloList.getBoardId());
						sendIntent.setAction(Intent.ACTION_SEND);
						sendIntent.setPackage(dbList.getOwner());
						sendIntent.putExtras(extras);
						AppContext.startService(sendIntent);
			  	    }
					//Remove from both arrays
			  	    it5.remove(); //dbListsWkeywords.remove(dbList);
			  	    it4.remove(); //trelloLists.remove(trelloList);
		    	}
	    	}
	    }
	    
	    
	    
	    
	    //Check if anything left in NewListsTable, add them to trello
	    List<TrelloList> dbNewLists = new  ArrayList<TrelloList>();
  		Cursor cursor4 = database.query(NewListsTable.TABLE_NAME, NewListsTableColumns, null, null,null, null, null);
  	    while(cursor4.moveToNext()) {
  	    	//Set as not synced
  	    	TrelloList newList = new TrelloList(cursor4.getString(cursor4.getColumnIndex(NewListsTable.COL_ID)), cursor4.getString(cursor4.getColumnIndex(NewListsTable.COL_NAME)), cursor4.getString(cursor4.getColumnIndex(NewListsTable.COL_BOARD_ID)), cursor4.getString(cursor4.getColumnIndex(NewListsTable.COL_NAME_KEYWORD)), cursor4.getString(cursor4.getColumnIndex(NewListsTable.COL_OWNER)), 0);
  	    	dbNewLists.add(newList);
  	    }
  	    cursor4.close();
  	    
  	    //Add all lists left in NewListsTable to Trello (Didn't match keyword so need to add them, or just don't have keyword)
  	    if(dbNewLists.isEmpty() == false){
  	    	Iterator<TrelloList> it = dbNewLists.iterator();
	    	while(it.hasNext())
	    	{
	    		TrelloList dbList = it.next();
	    	    //Add board to trello and get Trello Id
	    	    Log.d("ListsHandler", "New list sent to trello, name:" + dbList.getName());
	    	    String newId = AddListToTrello(dbList);
	    	    
				if (newId != null) {
					//Insert with new id in BoardsTable
					
					//Add to ListsTable in db with trello id
		    		ContentValues values = new ContentValues();
		    		values.put(ListsTable.COL_SYNCED, 1); //Now synced
					values.put(ListsTable.COL_TRELLO_ID, newId);
					values.put(ListsTable.COL_NAME, dbList.getName());
					values.put(ListsTable.COL_BOARD_ID, dbList.getBoardId());
					values.put(ListsTable.COL_CARDS_LAST_SYNC, "");
					values.put(ListsTable.COL_OWNER, dbList.getOwner());
					AppContext.getContentResolver().insert(Uri.parse(ListsURI), values);

					//Remove from NewListsTable
		    		String whereClause = NewListsTable.COL_ID + " = '" + dbList.getId() + "'";
					database.delete(NewListsTable.TABLE_NAME, whereClause, null);
					
					//Change any id's in WatchCardsTable, NewCardsTable, CardsTable should be fine
					//NewCardsTable
					ContentValues values2 = new ContentValues();
					values2.put(NewCardsTable.COL_LIST_ID, newId);
					String whereClause2 = NewCardsTable.COL_LIST_ID + " = '" + dbList.getId() + "'";
					database.update(NewCardsTable.TABLE_NAME, values2, whereClause2, null);	
					//CardsTable
					String where3 = CardsTable.COL_LIST_ID + " = '" + dbList.getId() + "'";
		  	    	ContentValues values3 = new ContentValues();
					values3.put(CardsTable.COL_LIST_ID, newId);
					database.update(CardsTable.TABLE_NAME, values3, where3, null);	
					//WatchCardsTable
					String where4 = WatchCardsTable.COL_LIST_ID + " = '" + dbList.getId() + "'";
		  	    	ContentValues values4 = new ContentValues();
					values4.put(WatchCardsTable.COL_LIST_ID, newId);
					database.update(WatchCardsTable.TABLE_NAME, values4, where4, null);	
					
					//Add to waiting list, wait for owner to update
					AppContext.addWaiting(dbList.getId());
					
					Log.d("ListsHandler", "Sending new listId back to owner, new listId:" + newId);
					//Send back new id to owner
					Intent sendIntent = new Intent();
					Bundle extras = new Bundle();
					extras.putString("request", "updateId");
					extras.putString("type", "list");
					extras.putString("id", dbList.getId());
					extras.putString("newId", newId);
					sendIntent.setAction(Intent.ACTION_SEND);
					sendIntent.setPackage(dbList.getOwner());
					sendIntent.putExtras(extras);
					AppContext.startService(sendIntent);
				}
					    	    
	    	}
  	    }
  	    dbHandler.close();
  	    
  	    //Check watchlists??
	}
	
	private String AddListToTrello(TrelloList theList){
		loadKeys();
		
		HttpClient client = new DefaultHttpClient();
		HttpPost post = new HttpPost(
				"https://api.trello.com/1/lists");
		List<BasicNameValuePair> results = new ArrayList<BasicNameValuePair>();
		
		results.add(new BasicNameValuePair("key",TrelloKey));
		results.add(new BasicNameValuePair("token",TrelloToken));
		
		results.add(new BasicNameValuePair("idBoard", theList.getBoardId()));
		if(theList.getName() != null) results.add(new BasicNameValuePair("name", theList.getName()));
		
		Log.d("ListsHandler", "idBoard:" + theList.getBoardId());
		
		String newId = null;
		try {
			post.setEntity(new UrlEncodedFormEntity(
					results));
		} catch (UnsupportedEncodingException e) {
			// Auto-generated catch block
			Log.e("ListsHandler","An error has occurred", e);
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
			
			Log.d("ListsHandler", "Add Response:" + result);
			JSONObject json;
			try {
				json = new JSONObject(result);
				newId = json.getString("id");
			} catch (JSONException e) {
				e.printStackTrace();
			}
		} catch (ClientProtocolException e) {
			Log.e("ListsHandler","client protocol exception", e);
		} catch (IOException e) {
			Log.e("ListsHandler", "io exception", e);
		}
		
		return newId; //Also need to return date
	}
	
	private String UpdateListOnTrello(TrelloList theList){
		loadKeys();
		
		HttpClient client = new DefaultHttpClient();

		HttpPut put = new HttpPut("https://api.trello.com/1/lists/" + theList.getId());

		List<BasicNameValuePair> results = new ArrayList<BasicNameValuePair>();
		results.add(new BasicNameValuePair("key",TrelloKey));
		results.add(new BasicNameValuePair("token",TrelloToken));
		
		//results.add(new BasicNameValuePair("idOrganization",OrganizationID));
		results.add(new BasicNameValuePair("name", theList.getName()));
		results.add(new BasicNameValuePair("idBoard", theList.getBoardId()));

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
			Log.d("ListsHandler", "Update Response:" + result);
		} catch (Exception e) {
			// Auto-generated catch block
			Log.e("Log Thread",
					"client protocol exception", e);
		}
		return null; //TODO return date?
	}
	private String DeleteListOnTrello(TrelloList theList){
		loadKeys();
		
		HttpClient client = new DefaultHttpClient();

		HttpPut put = new HttpPut("https://api.trello.com/1/lists/" + theList.getId());

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
			Log.d("ListsHandler", "Update Response:" + result);
		} catch (Exception e) {
			// Auto-generated catch block
			Log.e("Log Thread",
					"client protocol exception", e);
		}
		return null; //TODO return date?
	}
	
	private List<TrelloList> GetTrelloLists(String boardId){
		loadKeys();
		
		List<TrelloList> trelloListsList = new  ArrayList<TrelloList>();
		String url = "https://api.trello.com/1/boards/" + boardId + "/lists?key=" + TrelloKey + "&token=" + TrelloToken + "&fields=name";

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
			
			JSONObject jsonList = null;
			
			String trello_id = "";
			String name = "";
			
			try {
				
				jsonList = json.getJSONObject(i);
				trello_id = jsonList.getString("id");
				name = jsonList.getString("name");
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			TrelloList newList = new TrelloList(trello_id, name, boardId, null, null, 1);
			trelloListsList.add(newList);
		}
		return trelloListsList;
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
