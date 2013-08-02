package com.openatk.trello;

import java.util.ArrayList;
import java.util.List;

import com.openatk.trello.database.DatabaseHandler;
import com.openatk.trello.database.LoginsTable;
import com.openatk.trello.internet.OrganizationsHandler;
import com.openatk.trello.internet.TrelloOrganization;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;

public class OrganizationsList extends Activity implements OnClickListener,
		OnItemClickListener {

	private SQLiteDatabase database;
	private DatabaseHandler dbHandler;

	private ListView orgoList = null;

	private OrganizationsHandler organizationHandler = null;
	private List<TrelloOrganization> organizationList = null;
	OrganizationArrayAdapter orgoAdapter = null;

	private boolean loading = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.loading);

		dbHandler = new DatabaseHandler(this);

		// Load organizations
		organizationList = new ArrayList<TrelloOrganization>();

		orgoAdapter = new OrganizationArrayAdapter(this, R.layout.organization, organizationList);

		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		String apiKey = prefs.getString("apiKey", "null");
		String token = prefs.getString("token", "null");

		organizationHandler = new OrganizationsHandler(this, organizationList,
				apiKey, token);

		loading = true;
		organizationHandler.getOrganizationsList();
	}

	@Override
	public void onClick(View v) {

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		getMenuInflater().inflate(R.menu.organizations_list, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		if (item.getItemId() == R.id.menu_addOrganization) {
			// Show new organization dialog
			Log.d("Orgo List", "Add Organization");
			Intent go = new Intent(this, AddOrganization.class);
			startActivity(go);
		}
		return false;
	}

	public void doneLoadingList() {
		// Done loading organizations update list
		if(loading) {
			if(organizationList.size() < 2) {
				//No organizations exist go straight to add
				Intent go = new Intent(this, AddOrganization.class);
				go.putExtra("todo", "mustAdd");
				startActivity(go);
				finish();
			} else {
				loading = false;
				// Remove loading screen
				setContentView(R.layout.organizations_list);
				orgoList = (ListView) findViewById(R.id.list_view);
				orgoList.setAdapter(orgoAdapter);
				orgoList.setOnItemClickListener(this);
				((BaseAdapter) orgoList.getAdapter()).notifyDataSetChanged();
			}
		} else {
			((BaseAdapter) orgoList.getAdapter()).notifyDataSetChanged();
		}
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View view, int position,
			long id) {
		TrelloOrganization item = (TrelloOrganization) orgoAdapter
				.getItem(position);

		if(item.getId() == null){
			//Add new organization
			Intent go = new Intent(this, AddOrganization.class);
			startActivity(go);
		} else {
			database = dbHandler.getWritableDatabase();
			ContentValues updateValues = new ContentValues();
			updateValues.put(LoginsTable.COL_ORGO_ID, item.getId().trim());
			updateValues.put(LoginsTable.COL_ORGO_NAME, item.getName().trim());
			String where = LoginsTable.COL_ACTIVE + " = 1";
			database.update(LoginsTable.TABLE_NAME, updateValues, where, null);
			dbHandler.close();
	
			SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(getApplicationContext());
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString("organizationId", item.getId().trim());
			editor.putString("organizationName", item.getDisplayName().trim());
			editor.putBoolean("FirstSetup", true);
			editor.commit();
			
			Intent go = new Intent(this, MembersList.class);
			startActivity(go);
		}		
	}
}
