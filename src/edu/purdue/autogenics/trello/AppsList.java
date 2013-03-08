package edu.purdue.autogenics.trello;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import edu.purdue.autogenics.trello.R;

import edu.purdue.autogenics.trello.database.AppsTable;
import edu.purdue.autogenics.trello.database.DatabaseHandler;
import edu.purdue.autogenics.trello.internet.App;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
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
		sendIntent.setAction("edu.purdue.autogenics.trello");
		
		PackageManager packageManager = getPackageManager();
		List<ResolveInfo> services = packageManager.queryIntentServices(sendIntent, 0);
		Collections.sort(services, new ResolveInfo.DisplayNameComparator(packageManager));
		
		DatabaseHandler dbHandler = new DatabaseHandler(this);
		SQLiteDatabase database = dbHandler.getWritableDatabase();
		String[] columns = { AppsTable.COL_ID, AppsTable.COL_NAME, AppsTable.PACKAGE_NAME, AppsTable.COL_ALLOW_SYNCING };
		
	    List<App> appsInDb = new ArrayList<App>();
		
		Cursor cursor = database.query(AppsTable.TABLE_NAME, columns, null, null,null, null, null);
	    while (cursor.moveToNext()) {
	    	Log.d("In Db", "it:" + cursor.getString(cursor.getColumnIndex(AppsTable.COL_NAME)));
	    	Log.d("In Db", "it:" + cursor.getString(cursor.getColumnIndex(AppsTable.PACKAGE_NAME)));
	    	
	    	Boolean isSynced = (cursor.getInt(cursor.getColumnIndex(AppsTable.COL_ALLOW_SYNCING)) == 1) ? true : false;
	    	//Assume uninstalled
	    	App newApp = new App(cursor.getLong(cursor.getColumnIndex(AppsTable.COL_ID)), isSynced, false, cursor.getString(cursor.getColumnIndex(AppsTable.PACKAGE_NAME)), cursor.getString(cursor.getColumnIndex(AppsTable.COL_NAME)), null, null);
	    	appsInDb.add(newApp);
	    }
	    cursor.close();
	    dbHandler.close();
		
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
	          	Log.d("AppList", "Package:" + packageName);
	          	
	          	Long theId = null;
	          	Boolean isSynced = null;
	          	Iterator<App> iterator = appsInDb.iterator();
	        	while (iterator.hasNext()) {
	        		App compare = iterator.next();
	        		String comparePackageName = compare.getPackageName();
	        		if(comparePackageName != null && packageName.contentEquals(comparePackageName)){
	        			theId = compare.getId();
	        			isSynced = compare.getSyncApp();
	        			iterator.remove(); //Remove this found already
	        		}
	        	}
	          	App newApp = new App(theId, isSynced, true, packageName, name, desc, icon);
	          	appsList.add(newApp);
            }
            //Add ones that arn't installed anymore (only one's left after removing)
            Iterator<App> iterator = appsInDb.iterator();
        	while (iterator.hasNext()) {
        		App compare = iterator.next();
        		compare.setSyncApp(false); //Can't sync anymore
        		appsList.add(compare);
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
		Log.d("List item:", "test");
	}
}
