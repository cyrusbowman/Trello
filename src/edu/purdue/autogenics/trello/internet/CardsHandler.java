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

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

public class CardsHandler {
	Context AppContext;
	private String BoardsURI = DatabaseContentProvider.CONTENT_URI + DatabaseContentProvider.BOARDS_PATH;
		
	
	private String ListID = "51275ecdf4e4e76920003867";
	private String BoardID = "51147c16b98fc4036d00b0a1";
	private String OrganizationID = "51147a54abe641a06d005a7c";
	private String TrelloKey = "b1ae1192adda1b5b61563d30d7ab403b";
	private String TrelloToken = "b1853ab88c26b6c42bcdfe46e218a0441d0ebc390961695de3f93a5053a1ed8b";

	private String SyncURL = "https://api.trello.com/1/board/" + BoardID + "/actions?key=" + TrelloKey + "&token=" + TrelloToken + "&filter=createCard,updateCard&since=";

	public CardsHandler(Context applicationContext) {
		if(applicationContext == null){
			Log.d("CardsHandler", "null context");
		} else {
			AppContext = applicationContext;
		}
	}
	
	public void handle(Bundle data){
		
		Log.d("CardsHandler", "handing data");
		if(data == null){
			Log.d("CardsHandler", "null data");
		}
		if(data.containsKey("request")){
			if(data.getString("request").contentEquals("push")){
				//Add to internal database then request data from app
				PushRequest(data);
			} else if(data.getString("request").contentEquals("pushData")){
				//Push data to trello
				Push(data);
			}
		}		
	}
		
	private void Push(Bundle data){
		//Send data to trello
		final String org_trello_id = data.getString("id");
		
		final String name = data.getString("name");
		final String desc = data.getString("desc");
		final String owner = data.getString("owner");
		
		
		//Added this for now TODO REMOVE *******
		boolean skip = false;
		if(name.contentEquals("Test Rock Board")){
			SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(AppContext);
			skip = prefs.getBoolean("shouldskip", false);			
		}
		if(skip == false){
			//Skip next time
			
		}
		
		
		//TODO add check if name/desc both null then do nothing
		if(org_trello_id.contains("-") && skip == false){
			//New, add to trello, update org id to new trello id
			new Thread(new Runnable() {
				public void run() {
					Log.d("CardsHandler", "New board sent to trello, name:" + name);
					HttpClient client = new DefaultHttpClient();
					HttpPost post = new HttpPost(
							"https://api.trello.com/1/cards");
					List<BasicNameValuePair> results = new ArrayList<BasicNameValuePair>();
					
					results.add(new BasicNameValuePair("key",TrelloKey));
					results.add(new BasicNameValuePair("token",TrelloToken));
					results.add(new BasicNameValuePair("idOrganization",OrganizationID));
					results.add(new BasicNameValuePair("idList",ListID));					
					
					if(name != null) results.add(new BasicNameValuePair("name", name));
					if(desc != null) results.add(new BasicNameValuePair("desc", desc));

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
						
						Log.d("CardsHandler", "Add Response:" + result);
						JSONObject json;
						String newId = null;
						try {
							json = new JSONObject(result);
							newId = json.getString("id");
						} catch (JSONException e) {
							e.printStackTrace();
						}
						//Dont do this yet
						if (newId != null && false) {							
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
							
							sendIntent.setAction(Intent.ACTION_SEND);
							sendIntent.setPackage(owner);
							sendIntent.putExtras(extras);
							AppContext.startService(sendIntent);
						}
						
					} catch (ClientProtocolException e) {
						Log.e("CardsHandler","client protocol exception", e);
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
						Log.d("CardsHandler", "Update Response:" + result);
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
	
	private void Sync(){
		//Download all changes since a internal sync date
		//Compare with matching trello_ids 'synced=0' and pick newest
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(AppContext);
		String since = prefs.getString("LastSync", "null");
		Log.d("Request since:", since);
		new SyncBoards().execute(SyncURL + since);
		
	}
	
	private void PushRequest(Bundle data){
		Log.d("CardsHandler", "Push Request");
		
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
			//Sync with trello here first
			Sync();
		}
		
		if(update){
			Log.d("CardsHandler", "Updating in db");
			//Update in database
			values.put(BoardsTable.COL_SYNCED, 0);
			//NEED DATE
			AppContext.getContentResolver().update(boardUri, values, null, null);
		} else {
			Log.d("CardsHandler", "Adding to db");
			//Add to database
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
    				Log.d("CardsHandler", "Matched keyword, sending intent to owner");
    				
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
    				Log.d("CardsHandler", "No owner found for board, skipping.");
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
    					Log.d("CardsHandler","Asking for data of: " + trello_id);
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
