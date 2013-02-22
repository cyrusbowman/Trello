package com.vip.trello;

import java.util.ArrayList;
import java.util.List;

import com.vip.trello.R;
import com.vip.trello.internet.Organization;
import com.vip.trello.internet.OrganizationsHandler;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;

public class OrganizationsList extends Activity implements OnClickListener, OnItemClickListener {
	
	private String TrelloKey = "b1ae1192adda1b5b61563d30d7ab403b";
	private String TrelloToken = "b1853ab88c26b6c42bcdfe46e218a0441d0ebc390961695de3f93a5053a1ed8b";
	
	
	private ListView orgoList = null;
	private Button test = null;
	
	private OrganizationsHandler organizationHandler = null;
	private List<Organization> organizationList = null;
	
	private boolean loading = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_view);
		
		this.setTitle(getString(R.string.OrganizationsListTitle));
		orgoList = (ListView) findViewById(R.id.list_view);
				
		//Load organizations
		organizationList = new ArrayList<Organization>();
		
		OrganizationArrayAdapter adapter = new OrganizationArrayAdapter(this, R.layout.organization, organizationList);
		orgoList.setAdapter(adapter);
		
		orgoList.setOnItemClickListener(this);
		
		organizationHandler = new OrganizationsHandler(this, organizationList, TrelloKey, TrelloToken);
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
		Intent go = new Intent(this, AppsList.class);
		startActivity(go);
	}
}
