package edu.purdue.autogenics.trello;

import java.util.ArrayList;
import java.util.List;

import edu.purdue.autogenics.libcommon.trello.Organization;
import edu.purdue.autogenics.trello.internet.OrganizationsHandler;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;

public class OrganizationsList extends Activity implements OnClickListener, OnItemClickListener {
	
	private String TrelloKey = "b1ae1192adda1b5b61563d30d7ab403b";
	private String TrelloToken = "943ec9f8bf5f4093635737cb7b39ed74cf5b1f71d28a22805151dfeeb70191ef";
	
	
	private ListView orgoList = null;
	private Button test = null;
	
	private OrganizationsHandler organizationHandler = null;
	private List<Organization> organizationList = null;
	OrganizationArrayAdapter orgoAdapter = null;
	
	private boolean loading = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_view);
		
		this.setTitle(getString(R.string.OrganizationsListTitle));
		orgoList = (ListView) findViewById(R.id.list_view);
		
		//Load organizations
		organizationList = new ArrayList<Organization>();
		
		orgoAdapter = new OrganizationArrayAdapter(this, R.layout.organization, organizationList);
		orgoList.setAdapter(orgoAdapter);
		
		orgoList.setOnItemClickListener(this);
		
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		String apiKey = prefs.getString("apiKey", "null");
		String token = prefs.getString("token", "null");
		
		organizationHandler = new OrganizationsHandler(this, organizationList, apiKey, token);
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
		if(item.getItemId() == R.id.menu_addOrganization){
			//Show new organization dialog
			Log.d("Orgo List", "Add organization");
			
			//Just move on for now
			Intent go = new Intent(this, AppsList.class);
			startActivity(go);
		}
		return false;
	}

	public void doneLoadingList(){
		//Done loading organizations update list
		if(loading){
			loading = false;
			//Remove loading screen
		}
		((BaseAdapter) orgoList.getAdapter()).notifyDataSetChanged();
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
		//Just move on for now
		Organization item = (Organization) orgoAdapter.getItem(position);
		Log.d("Organization List", "Selected:" + item.getId());
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString("organizationId", item.getId().trim());
		editor.putBoolean("FirstSetup", true);
		editor.commit();

		Intent go = new Intent(this, AppsList.class);
		startActivity(go);
	}
}
