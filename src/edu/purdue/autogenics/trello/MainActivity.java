package edu.purdue.autogenics.trello;

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
import android.widget.Button;

public class MainActivity extends Activity implements OnClickListener {

	WebView browser = null;
	private Button setupAccount = null;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		
		setupAccount = (Button) findViewById(R.id.setupAccount);
		setupAccount.setOnClickListener(this);
		
		
		
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		Boolean setup = prefs.getBoolean("FirstSetup", false);
		if(setup){
			Intent go = new Intent(this, AppsList.class);
			startActivity(go);
		} else {
			//Make setupAccount button visible
			setupAccount.setVisibility(View.VISIBLE);
		}
	}
	
	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.setupAccount){
			//Go to browser
			Intent go = new Intent(this, Browser.class);
			go.putExtra("Setup", true);
			startActivity(go);
		}
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		if(item.getItemId() == R.id.menu_settings){
			//Show new organization dialog
			Log.d("Main", "Settings");
			
			//TODO Goes to google logout at the moment for testing 
			Intent go = new Intent(this, Browser.class);
			startActivity(go);
		}
		return false;
	}


	
	
}
