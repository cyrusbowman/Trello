package com.vip.trello;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.vip.trello.R;
import com.vip.trello.internet.App;
import com.vip.trello.internet.Organization;
import com.vip.trello.internet.OrganizationsHandler;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
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

public class AppsList extends Activity implements OnClickListener, OnItemClickListener {	
	
	private ListView appsListView = null;
	
	private List<App> appsList = null;
	
	private boolean loading = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_view);
		
		this.setTitle(getString(R.string.AppsListTitle));
		appsListView = (ListView) findViewById(R.id.list_view);
				
		//Load organizations
		appsList = new ArrayList<App>();
		
		//Find all supported apps
		Intent sendIntent = new Intent();
		sendIntent.setAction("com.vip.Trello");
		
		PackageManager packageManager = getPackageManager();
		List<ResolveInfo> services = packageManager.queryIntentServices(sendIntent, 0);
		Collections.sort(services, new ResolveInfo.DisplayNameComparator(packageManager));
		
		Log.d("AppsList", "Made it here");
		
		if(services != null){
			 final int count = services.size();
			 Log.d("AppsList", Integer.toString(count));

            for (int i = 0; i < count; i++) {
	          	ResolveInfo info = services.get(i);
	          	
	          	String packageName = info.serviceInfo.applicationInfo.packageName.toString();
	          	CharSequence csDesc = info.serviceInfo.applicationInfo.loadDescription(packageManager);
	          	CharSequence csName = info.serviceInfo.applicationInfo.loadLabel(packageManager);
	          	
	          	String name = (csName == null) ? null : csName.toString();
	          	String desc = (csDesc == null) ? null : csDesc.toString();
	          	Drawable icon = info.serviceInfo.applicationInfo.loadIcon(packageManager);
	          	
	          	Log.d("AppList", "Name:" + name);
	          	App newApp = new App(packageName, name, desc, icon);
	          	appsList.add(newApp);
            }
		}
		AppsArrayAdapter adapter = new AppsArrayAdapter(this, R.layout.app, appsList);
		appsListView.setAdapter(adapter);
		appsListView.setOnItemClickListener(this);
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
		}
		return false;
	}

	public void doneLoadingList(){
		//Done loading apps update list
		if(loading){
			loading = false;
			//Remove loading screen
		}
		((BaseAdapter) appsListView.getAdapter()).notifyDataSetChanged();
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
		
	}
}
