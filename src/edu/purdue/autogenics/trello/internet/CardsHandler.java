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

public class CardsHandler {
	TrelloService AppContext;
	private String BoardsURI = DatabaseContentProvider.CONTENT_URI + DatabaseContentProvider.BOARDS_PATH;
	private String ListsURI = DatabaseContentProvider.CONTENT_URI + DatabaseContentProvider.LISTS_PATH;
	private String CardsURI = DatabaseContentProvider.CONTENT_URI + DatabaseContentProvider.CARDS_PATH;

	
	private String OrganizationID = null;
	private String TrelloKey = null;
	private String TrelloToken = null;
	
	public CardsHandler(TrelloService applicationContext) {
		if(applicationContext == null){
			Log.d("CardsHandler", "null context");
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
		//Do cards need synced?
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
	    	//syncLists();
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
		//syncLists();
	}
	public void pushWatchCard(TrelloCard newCard){
		DatabaseHandler dbHandler = new DatabaseHandler(AppContext);
		SQLiteDatabase database = dbHandler.getWritableDatabase();
		
		ContentValues values = new ContentValues();
	    values.put(WatchCardsTable.COL_ID, newCard.getId());
		values.put(WatchCardsTable.COL_LIST_ID, newCard.getListId());
		values.put(WatchCardsTable.COL_KEYWORD, newCard.getName());
		values.put(WatchCardsTable.COL_OWNER, newCard.getOwner());
	    database.insert(WatchCardsTable.TABLE_NAME, null, values);
		
	    database.close();
	    dbHandler.close();
	}
	public void pushCard(TrelloCard newCard){
		//Find out if should go in BoardsTable or NewBoardsTable
		
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
				values.put(NewCardsTable.COL_LIST_ID, newCard.getListId());
				values.put(NewCardsTable.COL_NAME, newCard.getName()); //Used to avoid duplicates from different devices
				values.put(NewCardsTable.COL_DESC, newCard.getDesc()); //Used to avoid duplicates from different devices
				values.put(NewListsTable.COL_OWNER, newCard.getOwner());
				values.put(NewCardsTable.COL_DATE, theDate);
				database.update(NewCardsTable.TABLE_NAME, values, where, null);
	  	    } else {
	  	    	//Add it
	  	    	Log.d("pushCard", "Adding to NewCardsTable");
	  	    	ContentValues values = new ContentValues();
	  	    	values.put(NewCardsTable.COL_ID, newCard.getId());
				values.put(NewCardsTable.COL_LIST_ID, newCard.getListId());
				values.put(NewCardsTable.COL_NAME, newCard.getName()); //Used to avoid duplicates from different devices
				values.put(NewCardsTable.COL_DESC, newCard.getDesc()); //Used to avoid duplicates from different devices
				values.put(NewListsTable.COL_OWNER, newCard.getOwner());
				values.put(NewCardsTable.COL_DATE, theDate);
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
		database.close();
		dbHandler.close();
	}
	
	public void deleteCard(TrelloCard theCard){
		//Find out if should go in NewCardsTable or CardsTable
		
		DatabaseHandler dbHandler = new DatabaseHandler(AppContext);
		SQLiteDatabase database = dbHandler.getWritableDatabase();
		
		if(theCard.getId().contains("-")){
			//Belongs in NewCardsTable
			//Is it in the table already??
			Log.d("deleteCard", "Deleting from NewCardsTable");
			
			//Delete from NewCardsTable
			String whereClause = NewCardsTable.COL_ID + " = '" + theCard.getId() + "'";
			database.delete(NewCardsTable.TABLE_NAME, whereClause, null);
		} else {
			//Belongs in CardsTable, delete it, remove from trello
			Log.d("deleteCard", "Deleting from CardsTable");
			Uri cardURI = Uri.parse(CardsURI + "/" + theCard.getId());
			AppContext.getContentResolver().delete(cardURI, null, null);
			
			DeleteCardOnTrello(theCard);
		}
		database.close();
		dbHandler.close();
	}
	
	public void syncCards(){
		
		DatabaseHandler dbHandler = new DatabaseHandler(AppContext);
		SQLiteDatabase database = dbHandler.getWritableDatabase();
		

		//Get all ListIds from ListsTable
		List<String> listIds = new  ArrayList<String>();
		
		String[] columns = { ListsTable.COL_TRELLO_ID };
  	    Cursor cursor = database.query(ListsTable.TABLE_NAME, columns, null, null,null, null, null);
	    while(cursor.moveToNext()) {
	    	String id = cursor.getString(cursor.getColumnIndex(ListsTable.COL_TRELLO_ID));
	    	if(listIds.contains(id) == false){
	    		listIds.add(id);
	    	}
	    }
	    cursor.close();
	    
	    //Get all trello lists that reside inside our boards
	    List<TrelloCard> trelloCards = new  ArrayList<TrelloCard>();
	    
	    Iterator<String> idIter = listIds.iterator();
    	while(idIter.hasNext())
    	{
    		String trelloListId = idIter.next();
    	    List<TrelloCard> joinTrelloCards = GetTrelloCards(trelloListId);
    	    trelloCards.addAll(joinTrelloCards);
    	}
    	
    	//Now have all trello cards, compare these with CardsTable and sync
	    
    	//Get CardsTable cards 
  	    List<TrelloCard> dbCards = new  ArrayList<TrelloCard>();
  	    Uri cardsURI = Uri.parse(CardsURI);
		String[] cardColumns = { CardsTable.COL_TRELLO_ID, CardsTable.COL_SYNCED, CardsTable.COL_OWNER, CardsTable.COL_DATE, CardsTable.COL_LIST_ID };
		Cursor cursor3 = AppContext.getContentResolver().query(cardsURI, cardColumns, null, null, null);
		while(cursor3.moveToNext()) {
	    	TrelloCard newCard = new TrelloCard(cursor3.getString(cursor3.getColumnIndex(CardsTable.COL_TRELLO_ID)), null, null, cursor3.getString(cursor3.getColumnIndex(CardsTable.COL_LIST_ID)), cursor3.getString(cursor3.getColumnIndex(CardsTable.COL_DATE)), cursor3.getString(cursor3.getColumnIndex(CardsTable.COL_OWNER)), cursor3.getInt(cursor3.getColumnIndex(CardsTable.COL_SYNCED)));
	    	dbCards.add(newCard);
	    }
		cursor3.close();
		
		
		//Compare lists that have matching trello ids, compare date
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

		Iterator<TrelloCard> it3 = dbCards.iterator();
    	while(it3.hasNext())
    	{
    		TrelloCard dbCard = it3.next(); //List from db with currentBoardId as boardId
    		
    		//Loop through trelloLists with this id
    		Iterator<TrelloCard> it4 = trelloCards.iterator();
    		Boolean found = false;
	    	while(found == false && it4.hasNext())
	    	{
	    		TrelloCard trelloCard = it4.next();
	    		//If same card
		    	if(trelloCard.getId().contentEquals(dbCard.getId())){
		    		//Compare dates
		    		Log.d("syncCards", "Comparing dates");
		    		Log.d("syncCards", "        Trello:" + trelloCard.getDate().replace("T", " ").replace("Z", ""));
		    		Log.d("syncCards", "Local Database:" + dbCard.getDate().replace("T", " ").replace("Z", ""));
		    		
		    		Date syncDate = null;
			    	Date inDBDate = null;
			    	try {
						syncDate = dateFormat.parse(trelloCard.getDate().replace("T", " ").replace("Z", ""));
						inDBDate = dateFormat.parse(dbCard.getDate().replace("T", " ").replace("Z", ""));
					} catch (ParseException e) {
						e.printStackTrace();
						Log.d("Failed to parse","Could not parse date");
					}
			    	
			    	if(trelloCard.getClosed() == true){
			    		//Card was deleted by another device
			    		//Delete local to match trello, and notify owner of removal
		  	    		
		      			Uri cardURI = Uri.parse(CardsURI + "/" + dbCard.getId());
		  				AppContext.getContentResolver().delete(cardURI, null, null);
		  						  	
		  				//Add to waiting list, wait for owner to delete card
						AppContext.addWaiting(dbCard.getId());
		  				
		  				//Notify owner of deletion
		  				Intent sendIntent = new Intent();
		  				Bundle extras = new Bundle();
		  				extras.putString("request", "deleteData");
		  				extras.putString("type", "card");
		  				extras.putString("id", dbCard.getId());
		  				extras.putString("listId", dbCard.getListId());
		  				sendIntent.setAction(Intent.ACTION_SEND);
		  				sendIntent.setPackage(dbCard.getOwner());
		  				sendIntent.putExtras(extras);
		  				AppContext.startService(sendIntent);
		  				
		  				
			    	} else if(inDBDate.before(syncDate)){
						//Trello data is newer, update local to match Trello (send request to owner with new data)
			    		Uri cardURI = Uri.parse(CardsURI + "/" + trelloCard.getId());
						ContentValues values = new ContentValues();
						values.put(CardsTable.COL_SYNCED, 1);
						values.put(CardsTable.COL_DATE, trelloCard.getDate());
						values.put(CardsTable.COL_LIST_ID, trelloCard.getListId());
						AppContext.getContentResolver().update(cardURI, values, null, null);
						
						Log.d("syncCards","Trello date is newer local database");
						Log.d("syncCards","Sending data update intent to owner" + " new:" + trelloCard.getDate());
						Intent sendIntent = new Intent();
						Bundle extras = new Bundle();
						extras.putString("request", "updateData");
						extras.putString("type", "card");
						extras.putString("id", dbCard.getId());
						extras.putString("name", trelloCard.getName());
						extras.putString("desc", trelloCard.getDesc());
						extras.putString("listId", trelloCard.getListId());
						sendIntent.setAction(Intent.ACTION_SEND);
						sendIntent.setPackage(dbCard.getOwner());
						sendIntent.putExtras(extras);
						AppContext.startService(sendIntent);
					} else if(inDBDate.after(syncDate)){
						//Local change hasn't been synced to trello, send update request to owner, it will reply with data to update trello
						Log.d("syncCards","CardsTable date is newer update trello");
			    		Log.d("syncCards","Sending data request intent to owner");
			    		
			    		//No need to add to waiting list here, not a new card so trello id won't change
			    		
						Intent sendIntent = new Intent();
						Bundle extras = new Bundle();
						extras.putString("request", "dataRequest");
						extras.putString("type", "card");
						extras.putString("id", dbCard.getId());
						extras.putString("listId", dbCard.getListId());
						sendIntent.setAction(Intent.ACTION_SEND);
						sendIntent.setPackage(dbCard.getOwner());
						sendIntent.putExtras(extras);
						AppContext.startService(sendIntent);
						//ISSUE***, after this, it will do the .before code will execute on next sync because trello date will be a little newer
					}
			    	//Remove this board from both
			    	it3.remove(); //dbCards.remove(dbCard);
			    	it4.remove(); //trelloCards.remove(trelloCard);
			    	found = true;
		    	}
	    	}
    	}
	    
	    //Check if anything in NewCardsTable, add them to trello
	    List<TrelloCard> dbNewCards = new  ArrayList<TrelloCard>();
		String[] NewCardColumns = {NewCardsTable.COL_ID, NewCardsTable.COL_LIST_ID, NewCardsTable.COL_NAME, NewCardsTable.COL_DESC, NewCardsTable.COL_DATE, NewCardsTable.COL_OWNER};

  		Cursor cursor4 = database.query(NewCardsTable.TABLE_NAME, NewCardColumns, null, null,null, null, null);
  	    while(cursor4.moveToNext()) {
  	    	//Set as not synced
  	    	TrelloCard newCard = new TrelloCard(cursor4.getString(cursor4.getColumnIndex(NewCardsTable.COL_ID)), cursor4.getString(cursor4.getColumnIndex(NewCardsTable.COL_NAME)), cursor4.getString(cursor4.getColumnIndex(NewCardsTable.COL_DESC)), cursor4.getString(cursor4.getColumnIndex(NewCardsTable.COL_LIST_ID)), cursor4.getString(cursor4.getColumnIndex(NewCardsTable.COL_DATE)), cursor4.getString(cursor4.getColumnIndex(NewCardsTable.COL_OWNER)), 0);
  	    	dbNewCards.add(newCard);
  	    }
  	    cursor4.close();
  	    
  	    //Add all cards in NewCardsTable to Trello, send request to owner
  	    if(dbNewCards.isEmpty() == false){
  	    	Iterator<TrelloCard> it = dbNewCards.iterator();
	    	while(it.hasNext())
	    	{
	    		TrelloCard dbCard = it.next();
	    	    //Send request to owner to get data to Send to trello
	    		Log.d("syncCards","New card found in NewCardsTable");
	    		Log.d("syncCards","Sending data request intent to owner");
	    		
	    		//Add to waiting list, wait for owner to update
				AppContext.addWaiting(dbCard.getId());
	    		
				Intent sendIntent = new Intent();
				Bundle extras = new Bundle();
				extras.putString("request", "dataRequest");
				extras.putString("type", "card");
				extras.putString("id", dbCard.getId());
				extras.putString("listId", dbCard.getListId());
				sendIntent.setAction(Intent.ACTION_SEND);
				sendIntent.setPackage(dbCard.getOwner());
				sendIntent.putExtras(extras);
				AppContext.startService(sendIntent);
	    	}
  	    }
  	    
  	    //Check watchcards list
  	    List<TrelloCard> dbWatchCards = new  ArrayList<TrelloCard>();
		String[] WatchCardColumns = {WatchCardsTable.COL_ID, WatchCardsTable.COL_LIST_ID, WatchCardsTable.COL_KEYWORD, WatchCardsTable.COL_OWNER};

		Cursor cursor5 = database.query(WatchCardsTable.TABLE_NAME, WatchCardColumns, null, null,null, null, null);
	    while(cursor5.moveToNext()) {
	    	//Set as not synced
	    	TrelloCard newCard = new TrelloCard(cursor5.getString(cursor5.getColumnIndex(WatchCardsTable.COL_ID)), cursor5.getString(cursor5.getColumnIndex(WatchCardsTable.COL_KEYWORD)), null, cursor5.getString(cursor5.getColumnIndex(WatchCardsTable.COL_LIST_ID)), null, cursor5.getString(cursor5.getColumnIndex(WatchCardsTable.COL_OWNER)), 0);
	    	dbWatchCards.add(newCard);
	    }
	    cursor5.close();
  	    
	    Iterator<TrelloCard> it4 = trelloCards.iterator();
    	while(it4.hasNext())
    	{
    		TrelloCard trelloCard = it4.next(); 
    		if(trelloCard.getClosed() == false){ //If not card that was archived
	    		//Loop through watchCards with this id
	    		Iterator<TrelloCard> it5 = dbWatchCards.iterator();
		    	while(it5.hasNext())
		    	{
		    		TrelloCard watchCard = it5.next();
		    		if(trelloCard.getListId().contentEquals(watchCard.getListId())){
		    			if(trelloCard.getName().matches(watchCard.getName())){
			    			//TrelloCard name matches keyword
		    				//Add this card to local database
		    				Log.d("syncCards","Trello card matches pattern in watchlist");
		    				ContentValues values = new ContentValues();
		    	    		values.put(CardsTable.COL_SYNCED, 1); //Now synced
		    				values.put(CardsTable.COL_TRELLO_ID, trelloCard.getId());
		    				values.put(CardsTable.COL_LIST_ID, trelloCard.getListId());
		    				values.put(CardsTable.COL_DATE, trelloCard.getDate()); //Date of now
		    				values.put(ListsTable.COL_OWNER, watchCard.getOwner());
		    				AppContext.getContentResolver().insert(Uri.parse(CardsURI), values);
		    				
			    			//Send this card to owner to add
		    				Intent sendIntent = new Intent();
		    				Bundle extras = new Bundle();
		    				extras.putString("request", "addData");
		    				extras.putString("type", "card");
		    				extras.putString("id", trelloCard.getId());
		    				extras.putString("name", trelloCard.getName());
		    				extras.putString("desc", trelloCard.getDesc());
		    				extras.putString("listId", trelloCard.getListId());
		    				sendIntent.setAction(Intent.ACTION_SEND);
		    				sendIntent.setPackage(watchCard.getOwner());
		    				sendIntent.putExtras(extras);
		    				AppContext.startService(sendIntent);
			    		}
		    		}
		    	}
    		}
    	}
  	    dbHandler.close();
	}
	
	public void processDataResponse(TrelloCard theCard){
		
		if(theCard.getId().contains("-")){
			//Add to Trello
			//Data received from owner, by the - in trello the id we know we need to add this one to trello
			Log.d("CardsHandler", "New card sent to trello, name:" + theCard.getName());
			
			SimpleDateFormat dateFormatGmt = new SimpleDateFormat(
					"yyyy-MM-dd HH:mm:ss.SSS");
			dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));
			String theDate = dateFormatGmt.format(new Date());
			theDate = (theDate.replace(" ", "T") + "Z");
			
		    String newId = AddCardToTrello(theCard);
		    
			if (newId != null) {
				DatabaseHandler dbHandler = new DatabaseHandler(AppContext);
				SQLiteDatabase database = dbHandler.getWritableDatabase();
				
				//Insert with new id in CardsTable
				//Remove from NewCardsTable
	    		String whereClause = NewCardsTable.COL_ID + " = '" + theCard.getId() + "'";
				database.delete(NewCardsTable.TABLE_NAME, whereClause, null);
				database.close();
		  	    dbHandler.close();
		  	    
		  	    
				//Add to CardsTable in db with trello id
	    		ContentValues values = new ContentValues();
	    		values.put(CardsTable.COL_SYNCED, 1); //Now synced
				values.put(CardsTable.COL_TRELLO_ID, newId);
				values.put(CardsTable.COL_LIST_ID, theCard.getListId());
				values.put(CardsTable.COL_DATE, theDate); //Date of now
				values.put(ListsTable.COL_OWNER, theCard.getOwner());
				AppContext.getContentResolver().insert(Uri.parse(CardsURI), values);
				
				
				//No need to add to waiting list, already added when send data request
				
				//Send back new id to owner
				Intent sendIntent = new Intent();
				Bundle extras = new Bundle();
				extras.putString("request", "updateId");
				extras.putString("type", "card");
				extras.putString("id", theCard.getId());
				extras.putString("newId", newId);
				sendIntent.setAction(Intent.ACTION_SEND);
				sendIntent.setPackage(theCard.getOwner());
				sendIntent.putExtras(extras);
				AppContext.startService(sendIntent);
			}
		} else {
			//Update Trello
			UpdateCardOnTrello(theCard);
		}
	}
	
	private String AddCardToTrello(TrelloCard theCard){
		loadKeys();
		
		HttpClient client = new DefaultHttpClient();
		HttpPost post = new HttpPost(
				"https://api.trello.com/1/cards");
		List<BasicNameValuePair> results = new ArrayList<BasicNameValuePair>();
		
		results.add(new BasicNameValuePair("key",TrelloKey));
		results.add(new BasicNameValuePair("token",TrelloToken));
		results.add(new BasicNameValuePair("idList", theCard.getListId()));
		
		if(theCard.getName() != null) results.add(new BasicNameValuePair("name", theCard.getName()));
		if(theCard.getDesc() != null) results.add(new BasicNameValuePair("desc", theCard.getDesc()));

		String newId = null;
		try {
			post.setEntity(new UrlEncodedFormEntity(
					results));
		} catch (UnsupportedEncodingException e) {
			// Auto-generated catch block
			Log.e("CardsHandler","An error has occurred", e);
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
			
			Log.d("CardsHandler", "Add Response:" + result + " LOOK FOR DATE, please!!");
			JSONObject json;
			try {
				json = new JSONObject(result);
				newId = json.getString("id");
			} catch (JSONException e) {
				e.printStackTrace();
			}
		} catch (ClientProtocolException e) {
			Log.e("CardsHandler","client protocol exception", e);
		} catch (IOException e) {
			Log.e("CardsHandler", "io exception", e);
		}
		
		return newId; //Also need to return date
	}
	
	private String UpdateCardOnTrello(TrelloCard theCard){
		loadKeys();
		
		HttpClient client = new DefaultHttpClient();

		HttpPut put = new HttpPut("https://api.trello.com/1/cards/" + theCard.getId());

		List<BasicNameValuePair> results = new ArrayList<BasicNameValuePair>();
		results.add(new BasicNameValuePair("key",TrelloKey));
		results.add(new BasicNameValuePair("token",TrelloToken));
		
		results.add(new BasicNameValuePair("name", theCard.getName()));
		results.add(new BasicNameValuePair("desc", theCard.getDesc()));
		results.add(new BasicNameValuePair("idList", theCard.getListId()));

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
			Log.d("CardsHandler", "Update Response:" + result + " Look for date please ****");
		} catch (Exception e) {
			// Auto-generated catch block
			Log.e("Log Thread",
					"client protocol exception", e);
		}
		return null; //TODO return date?
	}
	private String DeleteCardOnTrello(TrelloCard theCard){
		loadKeys();
		
		HttpClient client = new DefaultHttpClient();

		HttpPut put = new HttpPut("https://api.trello.com/1/cards/" + theCard.getId());

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
			Log.d("CardsHandler", "Delete Response:" + result);
		} catch (Exception e) {
			// Auto-generated catch block
			Log.e("Log Thread",
					"client protocol exception", e);
		}
		return null; //TODO return date?
	}
	
	private List<TrelloCard> GetTrelloCards(String listId){
		loadKeys();
		
		List<TrelloCard> trelloCardsList = new  ArrayList<TrelloCard>();
		//Old no actions
		//String url = "https://api.trello.com/1/lists/" + listId + "/cards?key=" + TrelloKey + "&token=" + TrelloToken + "&fields=dateLastActivity,name,desc,closed";

		String url = "https://api.trello.com/1/lists/" + listId + "/cards?key=" + TrelloKey + "&token=" + TrelloToken + "&fields=name,desc,closed&actions=updateCard,createCard";
		
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
			String date = "";
			Boolean closed = false;
			JSONArray actions = null;
			
			try {
				
				jsonList = json.getJSONObject(i);
				trello_id = jsonList.getString("id");
				name = jsonList.getString("name");
				desc = jsonList.getString("desc");
				//date = jsonList.getString("dateLastActivity");
				actions = jsonList.getJSONArray("actions");
				date = actions.getJSONObject(0).getString("date");
				closed = jsonList.getBoolean("closed");
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			TrelloCard newCard = new TrelloCard(trello_id, name, desc, listId, date, null, 0);
			newCard.setClosed(closed);
			trelloCardsList.add(newCard);
		}
		return trelloCardsList;
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
