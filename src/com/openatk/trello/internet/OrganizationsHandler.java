package com.openatk.trello.internet;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.openatk.trello.OrganizationsList;
import com.openatk.trello.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

public class OrganizationsHandler extends CommonLibrary {
	
	private String TrelloKey = null;
	private String TrelloToken = null;
	private String FindOrganizations = null;
	
	private Bitmap defaultIcon = null;
	private Bitmap addIcon = null;
	
	private List<TrelloOrganization> organizationList = null;
	private OrganizationsList parent;
	
	public OrganizationsHandler(Context parent, List<TrelloOrganization> Organizations, String key, String token) {
		organizationList = Organizations;
		this.parent = (OrganizationsList) parent;
		
		FindOrganizations =  "https://trello.com/1/members/my/organizations?key=" + key + "&token=" + token;
		TrelloKey = key;
		TrelloToken = token;
		
		defaultIcon = BitmapFactory.decodeResource(this.parent.getResources(), R.drawable.organization);
		addIcon = BitmapFactory.decodeResource(this.parent.getResources(), R.drawable.add);
	}
	
	public void getOrganizationsList(){
		//populate list
		organizationList.clear();
		new getOrganizationsFromTrello().execute(FindOrganizations);
	}
	
	private class getOrganizationsFromTrello extends AsyncTask<String, Integer, JSONArray> {
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
				e.printStackTrace();
			}
			return json;
		}
		
		
		
		@Override
		protected void onProgressUpdate(Integer... values) {
			super.onProgressUpdate(values);
			
		}

		protected void onPostExecute(JSONArray organizations) {
			// Add these to list
			for (int i = 0; i < organizations.length(); i++) {
				
				JSONObject orgo = null;
				
				TrelloOrganization newOrg = null;
				
				try {
					orgo = organizations.getJSONObject(i);
					
					newOrg = new TrelloOrganization(); 
					
					newOrg.setId(orgo.getString("id"));
					newOrg.setName(orgo.getString("name"));
					newOrg.setDisplayName(orgo.getString("displayName"));
					newOrg.setDesc(orgo.getString("desc"));	
					newOrg.setIcon(defaultIcon);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				//Add this organization to the list
				if(newOrg != null) organizationList.add(newOrg);
			}			
			
			//Add add new button
			TrelloOrganization newOrg = new TrelloOrganization(); 
			newOrg.setId(null);
			newOrg.setName(null);
			newOrg.setDisplayName(parent.getString(R.string.organizations_list_create));
			newOrg.setDesc(null);
			newOrg.setIcon(addIcon);
			organizationList.add(newOrg);
			
			//Notify list that its done loading
			parent.doneLoadingList();
		}
	}
	
}
