package edu.purdue.autogenics.trello;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import edu.purdue.autogenics.trello.database.DatabaseHandler;
import edu.purdue.autogenics.trello.database.LoginsTable;
import edu.purdue.autogenics.trello.internet.CommonLibrary;
import edu.purdue.autogenics.trello.internet.TrelloOrganization;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class AddOrganization extends Activity implements OnClickListener {
	
	private SQLiteDatabase database;
	private DatabaseHandler dbHandler;

	private Button cancel = null;
	private Button add = null;
	private EditText name = null;
	private Boolean adding = false;
	String todo = null;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_organization);
		
		Bundle extras = getIntent().getExtras();
		if(extras != null){
			todo = extras.getString("todo");
		}
		dbHandler = new DatabaseHandler(this);

		cancel = (Button) findViewById(R.id.butAddOrgoCancel);
		cancel.setOnClickListener(this);

		add = (Button) findViewById(R.id.butAddOrgoAdd);
		add.setOnClickListener(this);

		name = (EditText) findViewById(R.id.etAddOrgoName);
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.butAddOrgoAdd) {
			// Add button pressed
			// Add an organization and go back to select screen
			String strName = name.getText().toString();			
			
			if(strName == null || strName.length() == 0){
				Toast.makeText(getApplicationContext(), "Please enter an organization name", Toast.LENGTH_LONG).show();
			} else {
				SharedPreferences prefs = PreferenceManager
						.getDefaultSharedPreferences(getApplicationContext());
				String apiKey = prefs.getString("apiKey", "null");
				String token = prefs.getString("token", "null");
				if(adding == false){
					new AddOrganizationToTrello(this).execute(apiKey, token, strName);
					adding = true;
					setContentView(R.layout.loading);
				}
			}
			
		} else if (v.getId() == R.id.butAddOrgoCancel) {
			// Cancel button pressed
			// Go back to select orgo screen
			if(todo != null && todo.contentEquals("mustAdd")){
				Intent go = new Intent(this, MainActivity.class);
				startActivity(go);
			} else {
				Intent go = new Intent(this, OrganizationsList.class);
				startActivity(go);
			}
		}
	}
	
	private class AddOrganizationToTrello extends AsyncTask<String, Integer, TrelloOrganization> {
		
		AddOrganization parent;
		
		public AddOrganizationToTrello(AddOrganization parent){
			this.parent = parent;
		}
		
		protected TrelloOrganization doInBackground(String... query) {
			HttpClient client = new DefaultHttpClient();
			HttpPost post = new HttpPost("https://api.trello.com/1/organizations");
			List<BasicNameValuePair> results = new ArrayList<BasicNameValuePair>();
			
			results.add(new BasicNameValuePair("key",query[0]));
			results.add(new BasicNameValuePair("token",query[1]));
			
			TrelloOrganization newOrgo = null;
			if(query[2] != null) results.add(new BasicNameValuePair("displayName", query[2]));
			
			try {
				post.setEntity(new UrlEncodedFormEntity(results));
			} catch (UnsupportedEncodingException e) {
				Log.e("AddCardToTrello","An error has occurred", e);
			}
			try {
				HttpResponse response = client.execute(post);
				String result = "";
				try {
					// Error here if no Internet TODO
					InputStream is = response.getEntity().getContent(); 
					result = CommonLibrary.convertStreamToString(is);
				} catch (IllegalStateException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				JSONObject json;
				try {
					json = new JSONObject(result);
					String newId = json.getString("id");
					newOrgo = new TrelloOrganization();
					newOrgo.setId(newId.trim());
					newOrgo.setDisplayName(query[2]);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			} catch (ClientProtocolException e) {
				Log.e("AddOrganizationToTrello","client protocol exception", e);
			} catch (IOException e) {
				Log.e("AddOrganizationToTrello", "io exception", e);
			}
			return newOrgo;
		}
		
		@Override
		protected void onProgressUpdate(Integer... values) {
			super.onProgressUpdate(values);

		}

		protected void onPostExecute(TrelloOrganization newOrgo) {
			if(newOrgo == null) {
				//failed to add
				Log.d("AddOrganizationToTrello", "Failed to add organization");
				this.parent.adding = false;
				this.parent.setContentView(R.layout.add_organization);
			} else {
				//Save id in prefs
				database = dbHandler.getWritableDatabase();
				ContentValues updateValues = new ContentValues();
				updateValues.put(LoginsTable.COL_ORGO_ID, newOrgo.getId());
				if(newOrgo.getDisplayName() != null){
					updateValues.put(LoginsTable.COL_ORGO_NAME, newOrgo.getDisplayName());
				}
				String where = LoginsTable.COL_ACTIVE + " = 1";
				database.update(LoginsTable.TABLE_NAME, updateValues, where, null);
				dbHandler.close();

				SharedPreferences prefs = PreferenceManager
						.getDefaultSharedPreferences(getApplicationContext());
				SharedPreferences.Editor editor = prefs.edit();
				editor.putString("organizationId", newOrgo.getId());
				if(newOrgo.getDisplayName() != null){
					editor.putString("organizationName", newOrgo.getDisplayName());
				}
				editor.putBoolean("FirstSetup", true);
				editor.commit();
				
				Intent go = new Intent(this.parent, MembersList.class);
				this.parent.startActivity(go);
			}
		}
	}
}
