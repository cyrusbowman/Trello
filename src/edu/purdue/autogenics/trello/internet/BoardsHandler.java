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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

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

import edu.purdue.autogenics.libcommon.trello.Board;
import edu.purdue.autogenics.libcommon.trello.IntentBoard;
import edu.purdue.autogenics.libcommon.trello.TrelloRequest;
import edu.purdue.autogenics.trello.contentprovider.DatabaseContentProvider;
import edu.purdue.autogenics.trello.database.AppsTable;
import edu.purdue.autogenics.trello.database.BoardsTable;
import edu.purdue.autogenics.trello.database.CardsTable;
import edu.purdue.autogenics.trello.database.DatabaseHandler;
import edu.purdue.autogenics.trello.database.NewBoardsTable;
import edu.purdue.autogenics.trello.database.NewCardsTable;
import edu.purdue.autogenics.trello.database.NewListsTable;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

public class BoardsHandler {
	Context AppContext;
	private String BoardsURI = DatabaseContentProvider.CONTENT_URI + DatabaseContentProvider.BOARDS_PATH;
	private String ListsURI = DatabaseContentProvider.CONTENT_URI + DatabaseContentProvider.LISTS_PATH;
	private String CardsURI = DatabaseContentProvider.CONTENT_URI + DatabaseContentProvider.CARDS_PATH;

	private String OtherAppOwner = "edu.purdue.autogenics.rockapp";
	
	private String OrganizationID = "51147a54abe641a06d005a7c";
	private String TrelloKey = "b1ae1192adda1b5b61563d30d7ab403b";
	private String TrelloToken = "943ec9f8bf5f4093635737cb7b39ed74cf5b1f71d28a22805151dfeeb70191ef";

	private String SyncURL = "https://api.trello.com/1/organizations/" + OrganizationID + "/boards?key=" + TrelloKey + "&token=" + TrelloToken + "&actions=createBoard&actions_since=";
	
	public BoardsHandler(Context applicationContext) {
		if(applicationContext == null){
			Log.d("BoardsHandler", "null context");
		} else {
			AppContext = applicationContext;
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
	
	public void checkIfBoardExists(){
		SharedPreferences prefs2 = PreferenceManager.getDefaultSharedPreferences(AppContext);
		String apiKey = prefs2.getString("apiKey", "null");
		String token = prefs2.getString("token", "null");
		String orgoId = prefs2.getString("organizationId", "null");
		
		OrganizationID = orgoId.trim();
		TrelloKey = apiKey.trim();
		TrelloToken = token.trim(); 
		
		List<Board> boardsList = null;
		 
		 Log.d("checkIfBoardExists", "HERE 1");
		 
		 
		 
		 //Get boards from trello since forever
		 boardsList = GetBoards(null);
		 
		 Boolean exists = false;
		 Board trelloBoard = null;
		 //Loop through it check for boardname
		 if(boardsList.isEmpty() == false){
	    	//Match each one on keywords
	    	Iterator<Board> it = boardsList.iterator();
	    	while(it.hasNext() && exists == false){
	    		trelloBoard = it.next();
	    		if(trelloBoard.getName().contentEquals("RockApp")){
	    			exists = true;
	    		}
	    	}
		 }
		 
		 Log.d("checkIfBoardExists", "HERE 2");
		 
		 if(exists == false){
			 Log.d("Board not found", "Adding board and lists");
			 //Add the board
			 Board newBoard = new Board(null, "RockApp", "Board for the rock app", null, null, null, "edu.purdue.autogenics.rockapp");
			 String boardId = AddBoardToTrello(newBoard);
			 //Save the boardId
			 SharedPreferences prefs = PreferenceManager
						.getDefaultSharedPreferences(AppContext);
			 SharedPreferences.Editor editor = prefs.edit();
			 editor.putString("TheBoardId", boardId);
			 
			 
			 //Make lists
			 String list1id = AddListToTrello("Rocks In Field", boardId);
			 String list2id = AddListToTrello("Rocks Picked Up", boardId);
			 
			 editor.putString("List1Id", list1id);
			 editor.putString("List2Id", list2id);
			 
			 editor.commit();
		 } else {
			 Log.d("Board already exists", "Getting id of board and id's of lists");
			 //Save the board id and get List id
			 //Get list trello id from that board id
			 List<TrelloList> trelloLists = null;
			 
			 String list1Id = "";
			 String list2Id = "";
			 
			 trelloLists = GetTrelloLists(null, trelloBoard.getId());
			 Iterator<TrelloList> it = trelloLists.iterator();
			 while(it.hasNext()){
				 TrelloList trelloList = it.next();
				 if(trelloList.getName().contentEquals("Rocks In Field")){
					 Log.d("Found list 1", "Found list 1");
					 list1Id = trelloList.getId();
				 }
				 if(trelloList.getName().contentEquals("Rocks Picked Up")){
					 Log.d("Found list 2", "Found list 2");
					 list2Id = trelloList.getId();
				 }
			 }
			 
			 SharedPreferences prefs = PreferenceManager
						.getDefaultSharedPreferences(AppContext);
			 SharedPreferences.Editor editor = prefs.edit();
			 editor.putString("TheBoardId", trelloBoard.getId());
			 editor.putString("List1Id", list1Id);
			 editor.putString("List2Id", list2Id);
			 editor.commit();
			 
			 //Send back all the cards on from lists and add these rocks to db internal if not already, and send them
			 //Sync it
			 this.syncCards();
		 }
	}
	
	public void pushCard(TrelloCard newCard){
		//Find out if should go in CardsTable or NewCardsTable
		
		DatabaseHandler dbHandler = new DatabaseHandler(AppContext);
		SQLiteDatabase database = dbHandler.getWritableDatabase();
		
  	    
  	    SimpleDateFormat dateFormatGmt = new SimpleDateFormat(
				"yyyy-MM-dd HH:mm:ss.SSS");
		dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));
		String theDate = dateFormatGmt.format(new Date());
		theDate = (theDate.replace(" ", "T") + "Z");
		
		if(newCard.getId().contains("-")){
			//Belongs in NewCardsTable
			//Is it in the table already??
			Log.d("pushCard", "Belongs in NewCardsTable");

			Boolean found = false;
	  		String[] columns2 = { NewCardsTable.COL_ID };
	  		String where = NewCardsTable.COL_ID + " = '" + newCard.getId() + "'";
	  		Cursor cursor2 = database.query(NewCardsTable.TABLE_NAME, columns2, where, null,null, null, null);
	  	    if(cursor2.moveToFirst()) {
	  	    	found = true;
	  	    }
	  	    cursor2.close();
	  	    
	  	    if(found){
	  	    	//Update it
	  	    	Log.d("pushCard", "Found already in NewCardsTable updating");
	  	    	ContentValues values = new ContentValues();
				values.put(NewCardsTable.COL_NAME, newCard.getName());
				values.put(NewCardsTable.COL_DESC, newCard.getDesc());
				values.put(NewCardsTable.COL_LIST_ID, newCard.getListId());
				values.put(NewCardsTable.COL_DATE, theDate);
				values.put(NewCardsTable.COL_OWNER, newCard.getOwner());
				database.update(NewCardsTable.TABLE_NAME, values, where, null);
	  	    } else {
	  	    	//Add it
	  	    	Log.d("pushCard", "Adding to NewCardsTable");
	  	    	ContentValues values = new ContentValues();
				values.put(NewCardsTable.COL_ID, newCard.getId());
				values.put(NewCardsTable.COL_NAME, newCard.getName());
				values.put(NewCardsTable.COL_DESC, newCard.getDesc());
				values.put(NewCardsTable.COL_LIST_ID, newCard.getListId());
				values.put(NewCardsTable.COL_DATE, theDate);
				values.put(NewCardsTable.COL_OWNER, newCard.getOwner());
	  	    	database.insert(NewCardsTable.TABLE_NAME, null, values);
	  	    }
		} else {
			//Belongs in CardsTable, update it
			Log.d("pushCard", "Updating in CardsTable");
			Uri cardURI = Uri.parse(CardsURI + "/" + newCard.getId());
			ContentValues values = new ContentValues();
			values.put(CardsTable.COL_SYNCED, 0);
			values.put(CardsTable.COL_DATE, theDate);
			values.put(CardsTable.COL_LIST_ID, newCard.getListId());
			values.put(CardsTable.COL_OWNER, newCard.getOwner());
			AppContext.getContentResolver().update(cardURI, values, null, null);
		}
		dbHandler.close();
	}
	
	public void syncCards(){
		//Sync cards in db
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(AppContext);
		String since = prefs.getString("LastSync", null);
		String apiKey = prefs.getString("apiKey", "null");
		String token = prefs.getString("token", "null");
		String orgoId = prefs.getString("organizationId", "null");
		
		OrganizationID = orgoId.trim();
		TrelloKey = apiKey.trim();
		TrelloToken = token.trim(); 
		
		if(since != null){
			Log.d("Request since:", since);
		}
		
		SimpleDateFormat dateFormatGmt = new SimpleDateFormat(
				"yyyy-MM-dd HH:mm:ss.SSS");
		dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));
		String theDate = dateFormatGmt.format(new Date());
		theDate = (theDate.replace(" ", "T") + "Z"); //New sync date
		
		
		String list1Id = prefs.getString("List1Id", null);
		String list2Id = prefs.getString("List2Id", null);
		
		if(list1Id != null && list2Id != null){
			Log.d("SyncCards","List1 Id:" + list1Id);
			Log.d("SyncCards","List2 Id:" + list2Id);
		} else {
			Log.d("SyncCards", "List id's null");
		}
		
		//Get cards since last sync from 2 lists
		List<TrelloCard> trelloCards1 = null;
		trelloCards1 = GetTrelloCards(since, list1Id);
		
		List<TrelloCard> trelloCards2 = null;
		trelloCards2 = GetTrelloCards(since, list2Id);
		
		//Get cards in internal db CardsTable
		List<TrelloCard> dbCards = new  ArrayList<TrelloCard>();
  	    Uri cardsURI = Uri.parse(CardsURI);
		String[] boardColumns = { CardsTable.COL_TRELLO_ID, CardsTable.COL_OWNER, CardsTable.COL_DATE, CardsTable.COL_LIST_ID };
		Cursor cursor3 = AppContext.getContentResolver().query(cardsURI, boardColumns, null, null, null);
		while(cursor3.moveToNext()) {
			TrelloCard newCard = new TrelloCard(cursor3.getString(cursor3.getColumnIndex(CardsTable.COL_TRELLO_ID)),null,null,cursor3.getString(cursor3.getColumnIndex(CardsTable.COL_LIST_ID)),cursor3.getString(cursor3.getColumnIndex(CardsTable.COL_DATE)),cursor3.getString(cursor3.getColumnIndex(CardsTable.COL_OWNER)));
			dbCards.add(newCard);
	    }
		cursor3.close();
		
		//Compare internal db to trello
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		Iterator<TrelloCard> it = dbCards.iterator();
		while(it.hasNext()){
			TrelloCard dbCard = it.next();
			Boolean found1 = false;
			Boolean found2 = false;
			TrelloCard trelloCard = null;
			
			Iterator<TrelloCard> it1 = trelloCards1.iterator();
			while(it1.hasNext() && found1 == false){
				trelloCard = it1.next();
				if(trelloCard.getId().contentEquals(dbCard.getId())){
					found1 = true;
					trelloCards1.remove(trelloCard);
				}
			}
			Iterator<TrelloCard> it2 = trelloCards2.iterator();
			while(it2.hasNext() && found2 == false){
				trelloCard = it2.next();
				if(trelloCard.getId().contentEquals(dbCard.getId())){
					found2 = true;
					trelloCards2.remove(trelloCard);
				}
			}
			
			if(found1 || found2){
				Log.d("syncCards","Found card in CardsTable matchig trello");
				Log.d("syncCards","trelloDate:" + trelloCard.getDate());
				Log.d("syncCards","dbDate:" + dbCard.getDate());
				
				Date syncDate = null;
		    	Date inDBDate = null;
		    	try {
					syncDate = dateFormat.parse(trelloCard.getDate().replace("T", " ").replace("Z", ""));
					inDBDate = dateFormat.parse(dbCard.getDate().replace("T", " ").replace("Z", ""));
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Log.d("Failed to parse","Could not parse date");
				}
				if(inDBDate.before(syncDate)){
		    		//Trello date is newer for this card replace internal db with its data
					Log.d("syncCards","Trello date is newer");
					Uri cardURI = Uri.parse(CardsURI + "/" + trelloCard.getId());
					ContentValues values = new ContentValues();
					values.put(CardsTable.COL_SYNCED, 1);
					values.put(CardsTable.COL_DATE, trelloCard.getDate());
					values.put(CardsTable.COL_LIST_ID, trelloCard.getListId());
					AppContext.getContentResolver().update(cardURI, values, null, null);
					
					Log.d("syncCards","Cards data is different send data update intent to owner");
					Intent sendIntent = new Intent();
					Bundle extras = new Bundle();
					extras.putString("request", "updateData");
					extras.putString("type", "card");
					extras.putString("id", dbCard.getId());
					extras.putString("name", trelloCard.getName());
					extras.putString("desc", trelloCard.getDesc());
					String listId = trelloCard.getListId();
					String newListId = dbCard.getListId();
					if(listId != null && list1Id != null && list2Id != null){
						Log.d("TrelloService", "Translating List Id's");
						if(listId.contentEquals(list1Id)){
							newListId = "1";
						} else if(listId.contentEquals(list2Id)) {
							newListId = "2";
						}
					}
					extras.putString("list_id", newListId);
					sendIntent.setAction(Intent.ACTION_SEND);
					sendIntent.setPackage(dbCard.getOwner());
					sendIntent.putExtras(extras);
					AppContext.startService(sendIntent);
					
		    	} else if(inDBDate.after(syncDate)) {
		    		//Internal database date is newer then trello, send update trello data
		    		//Send intent to owner to push this card update to trello
		    		Log.d("syncCards","CardsTable date is newer update trello");
		    		Log.d("syncCards","Sending data request intent to owner");
		    		
		    		Intent sendIntent = new Intent();
					Bundle extras = new Bundle();
					extras.putString("request", "dataRequest");
					extras.putString("type", "card");
					extras.putString("id", dbCard.getId());
					String listId = trelloCard.getListId();
					String newListId = dbCard.getListId();
					if(listId != null && list1Id != null && list2Id != null){
						Log.d("TrelloService", "Translating List Id's");
						if(listId.contentEquals(list1Id)){
							newListId = "1";
						} else if(listId.contentEquals(list2Id)) {
							newListId = "2";
						}
					}
					extras.putString("list_id", newListId);
					sendIntent.setAction(Intent.ACTION_SEND);
					sendIntent.setPackage(dbCard.getOwner());
					sendIntent.putExtras(extras);
					AppContext.startService(sendIntent);
		    	}
			}
		}
		
		//Add all trello card to internal db and send them to app
		Iterator<TrelloCard> it1 = trelloCards1.iterator();
		while(it1.hasNext()){
			TrelloCard trelloCard = it1.next();
			Log.d("SyncCards", "Has card left1:" + trelloCard.getName());
			ContentValues values = new ContentValues();
			values.put(CardsTable.COL_TRELLO_ID, trelloCard.getId());
			values.put(CardsTable.COL_OWNER, OtherAppOwner); //TODO change
			values.put(CardsTable.COL_SYNCED, 1);
			values.put(CardsTable.COL_DATE, trelloCard.getDate());
			values.put(CardsTable.COL_LIST_ID, trelloCard.getListId());
			AppContext.getContentResolver().insert(Uri.parse(CardsURI), values);
			
			//Send back new card to owner
			Intent sendIntent = new Intent();
			Bundle extras = new Bundle();
			extras.putString("request", "addCard");
			extras.putString("type", "card");
			extras.putString("id", trelloCard.getId());
			extras.putString("name", trelloCard.getName());
			extras.putString("desc", trelloCard.getDesc());
			String listId = trelloCard.getListId();
			String newListId = trelloCard.getListId();
			if(listId != null && list1Id != null && list2Id != null){
				Log.d("TrelloService", "Translating List Id's");
				if(listId.contentEquals(list1Id)){
					newListId = "1";
				} else if(listId.contentEquals(list2Id)) {
					newListId = "2";
				}
			}
			extras.putString("list_id", newListId);
			sendIntent.setAction(Intent.ACTION_SEND);
			sendIntent.setPackage(OtherAppOwner);
			sendIntent.putExtras(extras);
			AppContext.startService(sendIntent);
		}
		Iterator<TrelloCard> it2 = trelloCards2.iterator();
		while(it2.hasNext()){
			TrelloCard trelloCard = it2.next();
			Log.d("SyncCards", "Has card left2:" + trelloCard.getName());
			ContentValues values = new ContentValues();
			values.put(CardsTable.COL_TRELLO_ID, trelloCard.getId());
			values.put(CardsTable.COL_OWNER, OtherAppOwner); //TODO change
			values.put(CardsTable.COL_SYNCED, 1);
			values.put(CardsTable.COL_DATE, trelloCard.getDate());
			values.put(CardsTable.COL_LIST_ID, trelloCard.getListId());
			AppContext.getContentResolver().insert(Uri.parse(CardsURI), values);
			
			//Send back new card to owner
			Intent sendIntent = new Intent();
			Bundle extras = new Bundle();
			extras.putString("request", "addCard");
			extras.putString("type", "card");
			extras.putString("id", trelloCard.getId());
			extras.putString("name", trelloCard.getName());
			extras.putString("desc", trelloCard.getDesc());
			String listId = trelloCard.getListId();
			String newListId = trelloCard.getListId();
			if(listId != null && list1Id != null && list2Id != null){
				Log.d("TrelloService", "Translating List Id's");
				if(listId.contentEquals(list1Id)){
					newListId = "1";
				} else if(listId.contentEquals(list2Id)) {
					newListId = "2";
				}
			}
			extras.putString("list_id", newListId);
			sendIntent.setAction(Intent.ACTION_SEND);
			sendIntent.setPackage(OtherAppOwner);
			sendIntent.putExtras(extras);
			AppContext.startService(sendIntent);
		}
		
		//Get cards from NewCardsTable add them to trello and send back trello id's
		//Check if anything left in NewBoardsTable, add them to trello
		DatabaseHandler dbHandler = new DatabaseHandler(AppContext);
		SQLiteDatabase database = dbHandler.getWritableDatabase();
		
	    List<TrelloCard> dbNewCards = new  ArrayList<TrelloCard>();
  		String[] columns2 = { NewCardsTable.COL_ID,  NewCardsTable.COL_NAME,  NewCardsTable.COL_DESC,  NewCardsTable.COL_LIST_ID, NewCardsTable.COL_DATE, NewCardsTable.COL_OWNER };
  		Cursor cursor2 = database.query(NewCardsTable.TABLE_NAME, columns2, null, null,null, null, null);
  	    while(cursor2.moveToNext()) {
			TrelloCard newCard = new TrelloCard(cursor2.getString(cursor2.getColumnIndex(NewCardsTable.COL_ID)),cursor2.getString(cursor2.getColumnIndex(NewCardsTable.COL_NAME)),cursor2.getString(cursor2.getColumnIndex(NewCardsTable.COL_DESC)),cursor2.getString(cursor2.getColumnIndex(CardsTable.COL_LIST_ID)),cursor2.getString(cursor2.getColumnIndex(CardsTable.COL_DATE)),cursor2.getString(cursor2.getColumnIndex(CardsTable.COL_OWNER)));
	    	if(newCard.getListId() == null){
	    		Log.d("Card ListId:", "is null");
	    	} else {
	    		Log.d("Card ListId:", newCard.getListId());
	    	}
			dbNewCards.add(newCard);
  	    }
  	    cursor2.close();
  	    
  	    //Add all cards left in NewCardsTable to Trello
  	    if(dbNewCards.isEmpty() == false){
  	    	Log.d("BoardsHandler", "Adding new cards to trello from NewCardsTable");
  	    	Iterator<TrelloCard> it3 = dbNewCards.iterator();
	    	while(it3.hasNext())
	    	{
	    	    TrelloCard dbCard = it3.next();
	    	    //Add board to trello and get Trello Id
	    	    Log.d("BoardsHandler", "New card sent to trello, name:" + dbCard.getName());
	    	    String newId = AddCardToTrello(dbCard);
	    	    
				if (newId != null) {
					//Insert with new id in BoardsTable
					
					//Add to BoardsTable in db with trello id
		    		ContentValues values = new ContentValues();
		    		values.put(CardsTable.COL_SYNCED, 1);
					values.put(CardsTable.COL_TRELLO_ID, newId);
					values.put(CardsTable.COL_DATE, dbCard.getDate()); //New date from trello //TODO ***************
					values.put(CardsTable.COL_LIST_ID, dbCard.getListId());
					values.put(CardsTable.COL_OWNER, dbCard.getOwner());
					AppContext.getContentResolver().insert(Uri.parse(CardsURI), values);

					//Delete from NewCardsTable
					String whereClause = NewCardsTable.COL_ID + " = '" + dbCard.getId() + "'";
					database.delete(NewCardsTable.TABLE_NAME, whereClause, null);
					
					//Send back new id to owner
					Intent sendIntent = new Intent();
					Bundle extras = new Bundle();
					extras.putString("request", "updateId");
					extras.putString("type", "card");
					extras.putString("id", dbCard.getId());
					extras.putString("new_id", newId);
					String listId = dbCard.getListId();
					String newListId = dbCard.getListId();
					if(listId != null && list1Id != null && list2Id != null){
						Log.d("TrelloService", "Translating List Id's");
						if(listId.contentEquals(list1Id)){
							newListId = "1";
						} else if(listId.contentEquals(list2Id)) {
							newListId = "2";
						}
					}
					extras.putString("list_id", newListId);
					sendIntent.setAction(Intent.ACTION_SEND);
					sendIntent.setPackage(dbCard.getOwner());
					sendIntent.putExtras(extras);
					AppContext.startService(sendIntent);
				}    	    
	    	}
  	    }
  	    dbHandler.close();
  	    
  	    
  	    //May want to get date at beginning TODO *************
		SharedPreferences.Editor editor = prefs.edit();
		Log.d("LastSync:", theDate);
		editor.putString("LastSync", theDate);
		editor.commit();
	}
	
	private void syncBoards(){
		//Any in NewBoardsTable with keywords??
		
		Boolean NewBoardsWithKeywords = false;
		
		DatabaseHandler dbHandler = new DatabaseHandler(AppContext);
		SQLiteDatabase database = dbHandler.getWritableDatabase();
		String[] columns = { NewBoardsTable.COL_ID, NewBoardsTable.COL_NAME_KEYWORD, NewBoardsTable.COL_DESC_KEYWORD };
		String where = "(" + NewBoardsTable.COL_NAME_KEYWORD + " != '' AND " + NewBoardsTable.COL_NAME_KEYWORD + " IS NOT NULL) ";
		where = where + " OR (" + NewBoardsTable.COL_DESC_KEYWORD + " != '' AND " + NewBoardsTable.COL_DESC_KEYWORD + " IS NOT NULL)";
		
		Cursor cursor = database.query(NewBoardsTable.TABLE_NAME, columns, where, null,null, null, null);
	    if(cursor.moveToFirst()) {
	    	NewBoardsWithKeywords = true;
	    }
	    cursor.close();
	    
	    
	    List<Board> boardsList = null;
	    if(NewBoardsWithKeywords){
	    	//Get boards from trello since forever
	    	boardsList = GetBoards(null);
	    } else {
	    	
	    }
	    
	    if(NewBoardsWithKeywords){
	    	//Match each one w/keywords
	    	String[] columns2 = { NewBoardsTable.COL_ID };
			String where2 = "(" + NewBoardsTable.COL_NAME_KEYWORD + " != '' AND " + NewBoardsTable.COL_NAME_KEYWORD + " IS NOT NULL) ";
			where2 = where2 + " OR (" + NewBoardsTable.COL_DESC_KEYWORD + " != '' AND " + NewBoardsTable.COL_DESC_KEYWORD + " IS NOT NULL)";
			
			List<Board> dbBoardsWkeywords = new  ArrayList<Board>();
			Cursor cursor2 = database.query(NewBoardsTable.TABLE_NAME, columns2, where2, null,null, null, null);
		    while(cursor2.moveToNext()) {
		    	//Date field on Board Class is filled with COL_OWNER data from NewBoardsTable
		    	Board newBoard = new Board(cursor2.getString(cursor2.getColumnIndex(NewBoardsTable.COL_ID)), cursor2.getString(cursor2.getColumnIndex(NewBoardsTable.COL_NAME)), cursor2.getString(cursor2.getColumnIndex(NewBoardsTable.COL_DESC)), null, cursor2.getString(cursor2.getColumnIndex(NewBoardsTable.COL_NAME_KEYWORD)), cursor2.getString(cursor2.getColumnIndex(NewBoardsTable.COL_DESC_KEYWORD)), cursor2.getString(cursor2.getColumnIndex(NewBoardsTable.COL_OWNER)));
		    	dbBoardsWkeywords.add(newBoard);
		    }
		    cursor2.close();
		    
		    if(dbBoardsWkeywords.isEmpty() == false){
		    	//Match each one on keywords
		    	Iterator<Board> it = boardsList.iterator();
		    	while(it.hasNext())
		    	{
		    	    Board trelloBoard = it.next();
		    	    Iterator<Board> it2 = dbBoardsWkeywords.iterator();
		    		Boolean match = false;
		    		Board dbBoard = null;
			    	while(it2.hasNext() && match == false)
			    	{
			    		dbBoard = it2.next();
			    		if(dbBoard.getNameKeyword() != null && dbBoard.getNameKeyword().contentEquals(trelloBoard.getName())){
			    			//Match on name
			    			match = true;
			    		}
			    		if(dbBoard.getDescKeyword() != null && dbBoard.getDescKeyword().contentEquals(trelloBoard.getName())){
			    			//Match on keyword
			    			match = true;
			    		}
			    	}
			    	if(match){
			    		//Remove from NewBoardsTable
			    		String whereClause = NewBoardsTable.COL_ID + " = " + dbBoard.getId();
						database.delete(NewListsTable.TABLE_NAME, whereClause, null);
			    		
			    		//Add to BoardsTable in db with trello id
			    		ContentValues values = new ContentValues();
			    		values.put(BoardsTable.COL_SYNCED, 1);
						values.put(BoardsTable.COL_TRELLO_ID, trelloBoard.getId());
						values.put(BoardsTable.COL_DATE, trelloBoard.getDate());
						values.put(BoardsTable.COL_LISTS_LAST_SYNC, "");
						values.put(BoardsTable.COL_OWNER, dbBoard.getDate()); //Date is owner on internal db dbBoard
						AppContext.getContentResolver().insert(Uri.parse(BoardsURI), values);
						
						//Change any id's in WatchListsTable, NewListsTable, ListTable should be fine
						//NewListsTable
						ContentValues values2 = new ContentValues();
						values2.put(NewListsTable.COL_BOARD_ID, trelloBoard.getId());
						String whereClause2 = NewListsTable.COL_BOARD_ID + " = " + dbBoard.getId();
						database.update(NewListsTable.TABLE_NAME, values2, whereClause2, null);
						
						//TODO add Change any id's  WatchListsTable *********
						
						//TODO add send back data with intent to OWNER *******
						
						
						//Remove from both arrays
						dbBoardsWkeywords.remove(dbBoard);
			    		boardsList.remove(trelloBoard);
			    	}
		    	}
		    }
	    }
	    
	    //Check if anything left in NewBoardsTable, add them to trello
	    List<Board> dbNewBoards = new  ArrayList<Board>();
  		String[] columns2 = { NewBoardsTable.COL_ID,  NewBoardsTable.COL_NAME,  NewBoardsTable.COL_DESC,  NewBoardsTable.COL_NAME_KEYWORD,  NewBoardsTable.COL_DESC_KEYWORD,  NewBoardsTable.COL_OWNER };
  		Cursor cursor2 = database.query(NewBoardsTable.TABLE_NAME, columns2, null, null,null, null, null);
  	    while(cursor2.moveToNext()) {
  	    	//Date field on Board Class is filled with COL_OWNER data from NewBoardsTable
	    	Board newBoard = new Board(cursor2.getString(cursor2.getColumnIndex(NewBoardsTable.COL_ID)), cursor2.getString(cursor2.getColumnIndex(NewBoardsTable.COL_NAME)), cursor2.getString(cursor2.getColumnIndex(NewBoardsTable.COL_DESC)), null, cursor2.getString(cursor2.getColumnIndex(NewBoardsTable.COL_NAME_KEYWORD)), cursor2.getString(cursor2.getColumnIndex(NewBoardsTable.COL_DESC_KEYWORD)), cursor2.getString(cursor2.getColumnIndex(NewBoardsTable.COL_OWNER)));
	    	dbNewBoards.add(newBoard);
  	    }
  	    cursor2.close();
  	    
  	    //Add all boards left in NewBoardsTable to Trello (Didn't match keywords so need to add them, or just don't have keywords)
  	    if(dbNewBoards.isEmpty() == false){
  	    	Iterator<Board> it = dbNewBoards.iterator();
	    	while(it.hasNext())
	    	{
	    	    Board dbBoard = it.next();
	    	    //Add board to trello and get Trello Id
	    	    Log.d("BoardsHandler", "New board sent to trello, name:" + dbBoard.getName());
	    	    String newId = AddBoardToTrello(dbBoard);
	    	    
				if (newId != null) {
					//Insert with new id in BoardsTable
					
					//Add to BoardsTable in db with trello id
		    		ContentValues values = new ContentValues();
		    		values.put(BoardsTable.COL_SYNCED, 1);
					values.put(BoardsTable.COL_TRELLO_ID, newId);
					values.put(BoardsTable.COL_DATE, ""); //New date from trello //TODO ***************
					values.put(BoardsTable.COL_LISTS_LAST_SYNC, "");
					values.put(BoardsTable.COL_OWNER, dbBoard.getOwner());
					AppContext.getContentResolver().insert(Uri.parse(BoardsURI), values);

					//Delete from NewBoardsTable
					String whereClause = NewBoardsTable.COL_ID + " = " + dbBoard.getId();
					database.delete(NewListsTable.TABLE_NAME, whereClause, null);
					
					//TODO Send back new id to owner *************** 
					
					/*Intent sendIntent = new Intent();
					Bundle extras = new Bundle();
					extras.putString("request", "updateData");
					extras.putString("type", "board");
					extras.putString("id", dbBoard.getId());
					extras.putString("new_id", newId);
					
					
					TrelloRequest updateId = new TrelloRequest();
					updateId.setRequest(TrelloRequest.REQUEST_UPDATE_ID);
					updateId.setType(TrelloRequest.TYPE_BOARD);
					updateId.setId(dbBoard.getId());
					updateId.setNewId(newId);
					
					
					sendIntent.setAction(Intent.ACTION_SEND);
					sendIntent.setPackage(owner);
					sendIntent.putExtras(extras);
					AppContext.startService(sendIntent);*/
				}
					    	    
	    	}
  	    }
  	    
  	    //Loop through boards in BoardsTable and compare dates with Trello boards in boardsList
  	    List<Board> dbBoards = new  ArrayList<Board>();
  	    Uri boardsUri = Uri.parse(BoardsURI);
		String[] boardColumns = { BoardsTable.COL_TRELLO_ID, BoardsTable.COL_SYNCED, BoardsTable.COL_OWNER, BoardsTable.COL_DATE };
		Cursor cursor3 = AppContext.getContentResolver().query(boardsUri, boardColumns, null, null, null);
		while(cursor3.moveToNext()) {
			Board newBoard = new Board(cursor3.getString(cursor3.getColumnIndex(BoardsTable.COL_TRELLO_ID)), null, null, cursor3.getString(cursor3.getColumnIndex(BoardsTable.COL_DATE)), null, null, cursor3.getString(cursor2.getColumnIndex(NewBoardsTable.COL_OWNER)));
			dbBoards.add(newBoard);
	    }
		cursor3.close();
		if(dbBoards.isEmpty() == false){
			Iterator<Board> it = dbBoards.iterator();
	    	while(it.hasNext())
	    	{
	    		Board dbBoard = it.next();
	    		Iterator<Board> it2 = boardsList.iterator();
		    	while(it2.hasNext())
		    	{
		    		Board trelloBoard = it2.next();
		    	    //Compare dates
		    		
		    		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
			    	
			    	Date syncDate = null;
			    	Date inDBDate = null;
					try {
						syncDate = dateFormat.parse(trelloBoard.getDate().replace("T", " ").replace("Z", ""));
						inDBDate = dateFormat.parse(dbBoard.getDate().replace("T", " ").replace("Z", ""));
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						Log.d("Failed to parse","Could not parse date");
					}
					if(inDBDate.before(syncDate)){
			    		//online date is newer replace internal with it
						
			    	} else {
			    		//sql date is newer update trello
			    		
			    	}
		    	}
	    	}
		}
  	    dbHandler.close();
	}
	
	private String AddBoardToTrello(Board theBoard){
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
	
	private String AddCardToTrello(TrelloCard theCard){
		HttpClient client = new DefaultHttpClient();
		HttpPost post = new HttpPost(
				"https://api.trello.com/1/cards");
		List<BasicNameValuePair> results = new ArrayList<BasicNameValuePair>();
		
		results.add(new BasicNameValuePair("key",TrelloKey));
		results.add(new BasicNameValuePair("token",TrelloToken));
		results.add(new BasicNameValuePair("idOrganization",OrganizationID));
		
		if(theCard.getName() != null) results.add(new BasicNameValuePair("name", theCard.getName()));
		if(theCard.getDesc() != null) results.add(new BasicNameValuePair("desc", theCard.getDesc()));
		
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(AppContext);
		String List1Id = prefs.getString("List1Id", null);
		String List2Id = prefs.getString("List2Id", null);
		String newListId = theCard.getListId();
		
		if(newListId != null && List1Id != null && List2Id != null){
			Log.d("TrelloService", "Translating List Id's");
			if(newListId.contentEquals("1")){
				newListId = List1Id;
			} else if(newListId.contentEquals("2")) {
				newListId = List2Id;
			} else {
				newListId = theCard.getListId();
			}
		}
		
		if(newListId != null) results.add(new BasicNameValuePair("idList", newListId));

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
			Log.d("BoardsHandler", "CARD *********** TODO LOOK FOR DATE" + result);
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
	public String UpdateCardOnTrello(TrelloCard theCard){
		HttpClient client = new DefaultHttpClient();

		HttpPut put = new HttpPut("https://api.trello.com/1/cards/" + theCard.getId());

		List<BasicNameValuePair> results = new ArrayList<BasicNameValuePair>();
		results.add(new BasicNameValuePair("key",TrelloKey));
		results.add(new BasicNameValuePair("token",TrelloToken));
		results.add(new BasicNameValuePair("idOrganization",OrganizationID));
				
		results.add(new BasicNameValuePair("name", theCard.getName()));
		results.add(new BasicNameValuePair("desc", theCard.getDesc()));
		if(theCard.getListId() != null) results.add(new BasicNameValuePair("idList", theCard.getListId()));
		
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
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
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
	
	private String UpdateBoardOnTrello(Board theBoard){
		HttpClient client = new DefaultHttpClient();

		HttpPut put = new HttpPut("https://api.trello.com/1/boards/" + theBoard.getId());

		List<BasicNameValuePair> results = new ArrayList<BasicNameValuePair>();
		results.add(new BasicNameValuePair("key",TrelloKey));
		results.add(new BasicNameValuePair("token",TrelloToken));
		results.add(new BasicNameValuePair("idOrganization",OrganizationID));
		
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
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
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
	
	private String AddListToTrello(String name, String boardId){
		HttpClient client = new DefaultHttpClient();
		HttpPost post = new HttpPost(
				"https://api.trello.com/1/lists");
		List<BasicNameValuePair> results = new ArrayList<BasicNameValuePair>();
		
		results.add(new BasicNameValuePair("key",TrelloKey));
		results.add(new BasicNameValuePair("token",TrelloToken));
		results.add(new BasicNameValuePair("idOrganization",OrganizationID));
		
		if(boardId != null) results.add(new BasicNameValuePair("idBoard", boardId));
		if(name != null) results.add(new BasicNameValuePair("name", name));
		
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
			
			Log.d("BoardsHandler", "Add Response List:" + result);
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
	
	private List<TrelloList> GetTrelloLists(String sinceDate, String boardId){
		List<TrelloList> trelloListsList = new  ArrayList<TrelloList>();
		
		String url = "https://api.trello.com/1/boards/" + boardId + "/lists?key=" + TrelloKey + "&token=" + TrelloToken + "&actions=createList";
		if(sinceDate == null){
			//Get boards since forever
			
		} else {
			url = url + "&actions_since="; //SOME DATE
		}
		
		
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
			String board_id = "";
			JSONArray actions;
			String date = ""; //From most recent action
			
			try {
				
				jsonList = json.getJSONObject(i);
				trello_id = jsonList.getString("id");
				name = jsonList.getString("name");
				board_id = jsonList.getString("idBoard");
				//actions = jsonList.getJSONArray("actions");
				//date = actions.getJSONObject(0).getString("date"); //Get last date, Not here, no value Alternative??
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			TrelloList newList = new TrelloList(trello_id, name, board_id, date, null, null, null);
			trelloListsList.add(newList);
		}
		return trelloListsList;
	}
	
	private List<TrelloCard> GetTrelloCards(String sinceDate, String listId){
		List<TrelloCard> cardList = new  ArrayList<TrelloCard>();
		
		String url = "https://api.trello.com/1/lists/" + listId + "/cards?key=" + TrelloKey + "&token=" + TrelloToken + "&actions=createCard,updateCard";
		if(sinceDate == null){
			//Get cards since forever
		} else {
			//url = url + "&since=" + sinceDate; //SOME DATE
		}

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
			String desc = "";
			String list_id = "";
			JSONArray actions;
			String date = ""; //From most recent action
			
			try {
				
				jsonList = json.getJSONObject(i);
				trello_id = jsonList.getString("id");
				name = jsonList.getString("name");
				list_id = jsonList.getString("idList");
				desc = jsonList.getString("desc");
				actions = jsonList.getJSONArray("actions");
				date = actions.getJSONObject(0).getString("date"); //Get last date
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			TrelloCard newCard = new TrelloCard(trello_id, name, desc, list_id, date, null);
			cardList.add(newCard);
		}
		return cardList;
	}
	
	private List<Board> GetBoards(String sinceDate){
		List<Board> boardsList = new  ArrayList<Board>();
		
		String url = "https://api.trello.com/1/organizations/" + OrganizationID + "/boards?key=" + TrelloKey + "&token=" + TrelloToken + "&actions=createBoard&filter=open";
		if(sinceDate == null){
			//Get boards since forever
			
		} else {
			url = url + "&actions_since="; //SOME DATE
		}
		
		
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
			JSONArray actions;
			String date = ""; //From most recent action
			
			try {
				
				jsonBoard = json.getJSONObject(i);
				trello_id = jsonBoard.getString("id");
				name = jsonBoard.getString("name");
				desc = jsonBoard.getString("desc");
				actions = jsonBoard.getJSONArray("actions");
				date = actions.getJSONObject(0).getString("date"); //Get last date
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			Board newBoard = new Board(trello_id, name, desc, date, null, null, null);
			boardsList.add(newBoard);
		}
		return boardsList;
	}
	
	public void handle(Bundle data){
		
		Log.d("BoardsHandler", "handing data");
		if(data == null){
			Log.d("BoardsHandler", "null data");
		}
		if(data.containsKey("request")){
			if(data.getString("request").contentEquals("push")){
				//Add to internal database then request data from app
				PushRequest(data);
			} else if(data.getString("request").contentEquals("pushData")){
				//Push data to trello
				//Push(data);
			}
		}		
	}
		
	
	
	private void Push(IntentBoard data){
		//Send data to trello
		final String org_trello_id = data.getId();
		
		final String name = data.getName();
		final String desc = null;
		final String owner = data.getOwner();
		
		//TODO add check if name/desc both null then do nothing
		if(org_trello_id.contains("-")){
			//New, add to trello, update org id to new trello id
			new Thread(new Runnable() {
				public void run() {
					Log.d("BoardsHandler", "New board sent to trello, name:" + name);
					HttpClient client = new DefaultHttpClient();
					HttpPost post = new HttpPost(
							"https://api.trello.com/1/boards");
					List<BasicNameValuePair> results = new ArrayList<BasicNameValuePair>();
					
					results.add(new BasicNameValuePair("key",TrelloKey));
					results.add(new BasicNameValuePair("token",TrelloToken));
					results.add(new BasicNameValuePair("idOrganization",OrganizationID));
					
					if(name != null) results.add(new BasicNameValuePair("name", name));
					if(desc != null) results.add(new BasicNameValuePair("desc", desc));

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
						JSONObject json;
						String newId = null;
						try {
							json = new JSONObject(result);
							newId = json.getString("id");
						} catch (JSONException e) {
							e.printStackTrace();
						}
						if (newId != null) {							
							//Update new id and synced in internal db
							Uri boardUri = Uri.parse(BoardsURI + "/" + org_trello_id);
							ContentValues values = new ContentValues();
							values.put(BoardsTable.COL_SYNCED, 1);
							values.put(BoardsTable.COL_TRELLO_ID, newId);
							AppContext.getContentResolver().update(boardUri, values, null, null);
	
							//Send back new id to owner
							Intent sendIntent = new Intent();
							Bundle extras = new Bundle();
							extras.putString("request", "updateData");
							extras.putString("type", "board");
							extras.putString("id", org_trello_id);
							extras.putString("new_id", newId);
							
							
							TrelloRequest updateId = new TrelloRequest();
							updateId.setRequest(TrelloRequest.REQUEST_UPDATE_ID);
							updateId.setType(TrelloRequest.TYPE_BOARD);
							updateId.setId(org_trello_id);
							updateId.setNewId(newId);
							
							
							sendIntent.setAction(Intent.ACTION_SEND);
							sendIntent.setPackage(owner);
							sendIntent.putExtras(extras);
							AppContext.startService(sendIntent);
						}
						
					} catch (ClientProtocolException e) {
						Log.e("BoardsHandler","client protocol exception", e);
					} catch (IOException e) {
						Log.e("Log Thread", "io exception", e);
					}
				}
			}).start();
		} else {
			//Update, changed name/desc
			new Thread(new Runnable() {
				public void run() {
					Log.d("Updating card", "Card updated to trello");
					HttpClient client = new DefaultHttpClient();

					HttpPut put = new HttpPut("https://api.trello.com/1/boards/" + org_trello_id);

					List<BasicNameValuePair> results = new ArrayList<BasicNameValuePair>();
					results.add(new BasicNameValuePair("key",TrelloKey));
					results.add(new BasicNameValuePair("token",TrelloToken));
					results.add(new BasicNameValuePair("idOrganization",OrganizationID));
					
					results.add(new BasicNameValuePair("name", name));
					results.add(new BasicNameValuePair("desc", desc));

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
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						Log.d("BoardsHandler", "Update Response:" + result);
					} catch (Exception e) {
						// Auto-generated catch block
						Log.e("Log Thread",
								"client protocol exception", e);
					}
					
					//Update synced in internal db
					Uri boardUri = Uri.parse(BoardsURI + "/" + org_trello_id);
					ContentValues values = new ContentValues();
					values.put(BoardsTable.COL_SYNCED, 1);
					AppContext.getContentResolver().update(boardUri, values, null, null);
				}
			}).start();
		}
	}
	
	private void OldSync(){
		//Download all changes since a internal sync date
		//Compare with matching trello_ids 'synced=0' and pick newest
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(AppContext);
		String since = prefs.getString("LastSync", "null");
		Log.d("Request since:", since);
		new SyncBoards().execute(SyncURL + since);
		
	}
	
	private void PushRequest(Bundle data){
		Log.d("BoardsHandler", "Push Request");
		
		String trello_id = data.getString("id");
		String ownerPackage = data.getString("owner"); //Package name of where the data is stored
		String nameKeyword = data.getString("name_keyword");
		String descKeyword = data.getString("desc_keyword");
		String new_trello_id = null;
		
		if(nameKeyword == null) nameKeyword = "";
		if(descKeyword == null) descKeyword = "";
		
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
		
		
		if(update == false && (nameKeyword.length() > 0 || descKeyword.length() > 0)){
			//New board doesn't exist yet
			//Sync with trello here first, might be in db now
			Sync();
		}
		
		if(update){
			Log.d("BoardsHandler", "Updating in db");
			//Update in database, needs to sync again
			values.put(BoardsTable.COL_SYNCED, 0);
			//NEED DATE
			AppContext.getContentResolver().update(boardUri, values, null, null);
		} else {
			//Add to database
			
			//TODO need to check if keyword is in db now
			
			
			Log.d("BoardsHandler", "Adding to db");
			
			values.put(BoardsTable.COL_SYNCED, 0);
			values.put(BoardsTable.COL_TRELLO_ID, trello_id);
			values.put(BoardsTable.COL_OWNER, ownerPackage);
			//values.put(BoardsTable.COL_NAME_KEYWORD, nameKeyword);
			//values.put(BoardsTable.COL_DESC_KEYWORD, descKeyword);
			//NEED DATE
			AppContext.getContentResolver().insert(Uri.parse(BoardsURI), values);
		}
		
		
		//Sync updates and adds
		Sync();
	}
	
	private class SyncBoards extends AsyncTask<String, Integer, JSONArray> {
		
		void syncBoard(String trello_id, String name, String desc, String date){
			//Compare this "From Trello" board to the ones in internal database
			int update = 0;
	    	int add = 0;
	    	boolean inDb = false;
	    	
	    	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	    	
	    	//Check if exists in db
	    	String inDBDateString = "";
	    	String inDBOwner = "";
	    	
			Uri boardUri = Uri.parse(BoardsURI + "/" + trello_id);
			String[] projectionExists = { BoardsTable.COL_TRELLO_ID, BoardsTable.COL_DATE };
			Cursor cursor = AppContext.getContentResolver().query(boardUri, projectionExists, null, null, null);
			if (cursor.moveToFirst()) {
				//Exists
				inDb = true;
				inDBDateString = cursor.getString(cursor.getColumnIndex(BoardsTable.COL_DATE));
				inDBOwner = cursor.getString(cursor.getColumnIndex(BoardsTable.COL_OWNER));
				cursor.close();
	        }
	    	
	    	if(inDb){
		    	Date syncDate = null;
		    	Date inDBDate = null;
				try {
					syncDate = dateFormat.parse(date.replace("T", " ").replace("Z", ""));
					inDBDate = dateFormat.parse(inDBDateString.replace("T", " ").replace("Z", ""));
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Log.d("Failed to parse","Could not parse date");
				}
				if(inDBDate.before(syncDate)){
		    		//online date is newer replace internal with it
					update = 1;
		    	}
	    	} else {
	    		add = 1;
	    	}
	    	
	    	if(update == 1){
	    		//Update internal db with this date and send intent to update apps internal db
	    		ContentValues values = new ContentValues();
	    		values.put(BoardsTable.COL_SYNCED, 1);
	    		values.put(BoardsTable.COL_DATE, inDBDateString);
				AppContext.getContentResolver().update(boardUri, values, null, null);
				
				//Send intent to owner to update data
				Intent sendIntent = new Intent();
				Bundle extras = new Bundle();
				extras.putString("request", "updateData");
				extras.putString("type", "board");
				extras.putString("id", trello_id);
				extras.putString("name", name);
				extras.putString("desc", desc);
				
				sendIntent.setAction(Intent.ACTION_SEND);
				sendIntent.setPackage(inDBOwner);
				sendIntent.putExtras(extras);
				AppContext.startService(sendIntent);
	    	}
	    	if(add == 1){
	    		//Not in our database, try to match on keyword, skip if none found
	    		//Check if any other boards with these keywords
	    		
    			Uri boardsUri = Uri.parse(BoardsURI);
    			Boolean keyword = false;
    			String old_trello_id = null;
    			//Comment cause errors
    			/*String[] nameKeywordExists = { BoardsTable.COL_TRELLO_ID, BoardsTable.COL_NAME_KEYWORD, BoardsTable.COL_DESC_KEYWORD };
    			Cursor cursor2 = AppContext.getContentResolver().query(boardsUri, nameKeywordExists, null, null, null);
    			if (cursor2.moveToFirst()) {
    	            do {
	    				if(cursor2.getString(cursor2.getColumnIndex(BoardsTable.COL_NAME_KEYWORD)).contentEquals(name)){
	    					//Name keyword matches
	    					keyword = true;
	    					old_trello_id = cursor2.getString(cursor2.getColumnIndex(BoardsTable.COL_TRELLO_ID));
	    				}
	    				if(cursor2.getString(cursor2.getColumnIndex(BoardsTable.COL_DESC_KEYWORD)).contentEquals(desc)){
	    					//Desc Keyword matches
	    					keyword = true;
	    					old_trello_id = cursor2.getString(cursor2.getColumnIndex(BoardsTable.COL_TRELLO_ID));
	    				}
    	            } while (cursor2.moveToNext() && keyword == false);
    	            cursor2.close();
    	        }*/
    				    		
	    		//Add in internal database and send to intent to owner	  
    			if(keyword){
    				Log.d("BoardsHandler", "Matched keyword, sending intent to owner");
    				
    				//Send intent to owner to update data
    				Intent sendIntent = new Intent();
    				Bundle extras = new Bundle();
    				extras.putString("request", "updateData");
    				extras.putString("type", "board");
    				extras.putString("id", old_trello_id);
    				extras.putString("new_id", trello_id);
    				extras.putString("name", name);
    				extras.putString("desc", desc);
    				
    				sendIntent.setAction(Intent.ACTION_SEND);
    				sendIntent.setPackage(inDBOwner);
    				sendIntent.putExtras(extras);
    				AppContext.startService(sendIntent);
    			} else {
    				Log.d("BoardsHandler", "No owner found for board, skipping.");
    			}
	    		
	    	}  
		}
		
		void requestDataUnsynced(){
			Uri boardsUri = Uri.parse(BoardsURI);
			
			String[] projection = { BoardsTable.COL_TRELLO_ID, BoardsTable.COL_SYNCED, BoardsTable.COL_OWNER };
			Cursor cursor2 = AppContext.getContentResolver().query(boardsUri, projection, null, null, null);
			if (cursor2.moveToFirst()) {
	            do {
    				if(cursor2.getInt(cursor2.getColumnIndex(BoardsTable.COL_SYNCED)) == 0){
    					String trello_id =  cursor2.getString(cursor2.getColumnIndex(BoardsTable.COL_TRELLO_ID));
    					Log.d("BoardsHandler","Asking for data of: " + trello_id);
    					Intent sendIntent = new Intent();
    					Bundle extras = new Bundle();
    					extras.putString("request", "data");
    					extras.putString("type", "board");
    					extras.putString("id", trello_id);
    					
    					sendIntent.setAction(Intent.ACTION_SEND);
    					sendIntent.setPackage(cursor2.getString(cursor2.getColumnIndex(BoardsTable.COL_OWNER)));
    					sendIntent.putExtras(extras);
    					AppContext.startService(sendIntent);
    				}
	            } while (cursor2.moveToNext());
	            cursor2.close();
	        }
		}
		
		protected JSONArray doInBackground(String... urls) {
			HttpResponse response = getData(urls[0]);

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
			return json;
		}

		protected void onPostExecute(JSONArray boards) {
			// Sync cards from web
			for (int i = 0; i < boards.length(); i++) {
				
				JSONObject board = null;
				
				String trello_id = "";
				String name = "";
				String desc = "";
				JSONArray actions;
				String date = ""; //From most recent action
				
				try {
					
					board = boards.getJSONObject(i);
					trello_id = board.getString("id");
					name = board.getString("name");
					desc = board.getString("desc");
					actions = board.getJSONArray("actions");
					date = actions.getJSONObject(0).getString("date");
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				//Compare this to ones in db and replace if needed
				syncBoard(trello_id, name, desc, date);
			}
			
			requestDataUnsynced();

			SimpleDateFormat dateFormatGmt = new SimpleDateFormat(
					"yyyy-MM-dd HH:mm:ss.SSS");
			dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));
			String theDate = dateFormatGmt.format(new Date());
			theDate = (theDate.replace(" ", "T") + "Z");
			Log.d("LastSync:", theDate);

			SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(AppContext);
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString("LastSyncBoards", theDate);
			editor.commit();
		}
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
