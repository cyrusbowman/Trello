package com.vip.trello;

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
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

public class OrganizationsList extends Activity implements OnClickListener {
	
	private String TrelloKey = "b1ae1192adda1b5b61563d30d7ab403b";
	private String TrelloToken = "9f4879493d59a4f6779a1e024b53abb0b85700c5f69b46bc63f40505ca93c1e2";
	
	
	private ListView orgoList = null;
	
	private OrganizationsHandler organizationHandler = null;
	private List<Organization> organizationList = null;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.organizations_list);
		
		orgoList = (ListView) findViewById(R.id.organizationsList);
		//Load organizations
		organizationHandler = new OrganizationsHandler(organizationList, TrelloKey, TrelloToken);
		organizationHandler.getOrganizationsList();
		
		Log.d("OrganizationList", "GOT HERE");
		
		//ArrayAdapter<Organization> adapter = new ArrayAdapter<Organization>(this,android.R.layout.simple_list_item_1, android.R.id.text1, organizationList);
		//orgoList.setAdapter(adapter);
	}
	
	@Override
	public void onClick(View v) {
		
	}
}
