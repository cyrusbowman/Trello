package com.vip.trello.internet;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.os.AsyncTask;
import android.util.Log;
import com.vip.trello.internet.Organization;
import com.vip.trello.internet.Board;

public class OrganizationsHandler extends CommonLibrary {
	
	private String TrelloKey = null;
	private String TrelloToken = null;
	private String FindOrganizations = null;
	
	private List<Organization> organizationList = null;
	
	
	public OrganizationsHandler(List<Organization> Organizations, String key, String token) {
		organizationList = Organizations;
		FindOrganizations =  "https://trello.com/1/members/my/organizations?key=" + key + "&token=" + token;
		TrelloKey = key;
		TrelloToken = token;
	}
	
	public void getOrganizationsList(){
		organizationList = new ArrayList<Organization>();
		//populate list
		new getOrganizationsFromTrello().execute(FindOrganizations);
	}
	
	private class getOrganizationsFromTrello extends AsyncTask<String, Void, JSONArray> {
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
		
		protected void onPostExecute(JSONArray organizations) {
			
			
			// Add these to list
			for (int i = 0; i < organizations.length(); i++) {
				
				JSONObject orgo = null;
				
				Organization newOrg = null;
				
				try {
					orgo = organizations.getJSONObject(i);
					
					newOrg = new Organization(); 
					
					newOrg.setId(orgo.getString("id"));
					newOrg.setName(orgo.getString("name"));
					newOrg.setName(orgo.getString("displayName"));
					newOrg.setDesc(orgo.getString("desc"));
					
					JSONArray boards = orgo.getJSONArray("idBoards");
					for(int x=0; x < boards.length(); x++){
						String curBoardId = boards.getString(x);
						
						Board newBoard = new Board();
						newBoard.setId(curBoardId);
						
						newOrg.addBoard(newBoard);
					}
					
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				//Add this organization to the list
				if(newOrg != null) organizationList.add(newOrg);
			}
		}
	}
	
}