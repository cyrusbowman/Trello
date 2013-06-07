package edu.purdue.autogenics.trello;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import edu.purdue.autogenics.trello.internet.MembersHandler;
import edu.purdue.autogenics.trello.internet.TrelloMember;

public class MembersList extends Activity implements OnClickListener, OnItemClickListener {

	private ListView membersListView = null;
	private Button buttonNext = null;

	private MembersHandler membersHandler = null;
	private List<TrelloMember> membersList = null;
	MembersArrayAdapter memberAdapter = null;

	private boolean loading = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.loading);

		membersList = new ArrayList<TrelloMember>();
		
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		String orgoId = prefs.getString("organizationId", "null");
		String apiKey = prefs.getString("apiKey", "null");
		String token = prefs.getString("token", "null");
		
		membersHandler = new MembersHandler(this, membersList, orgoId, apiKey, token);
		
		memberAdapter = new MembersArrayAdapter(this, R.layout.member_list_item, membersList, membersHandler);
		loading = true;
		membersHandler.getExistingMembersList();
	}

	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.member_list_next){
			//Go to app list
			Intent go = new Intent(this, AppsList.class);
			startActivity(go);
		}
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		//TODO refresh button
		//getMenuInflater().inflate(R.menu.members_list, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return false;
	}

	public void doneLoadingList() {
		// Done loading organizations update list
		if (loading) {
			loading = false;
			// Remove loading screen
			setContentView(R.layout.member_list_view);
			membersListView = (ListView) findViewById(R.id.member_list_view);
			buttonNext = (Button) findViewById(R.id.member_list_next);
			membersListView.setAdapter(memberAdapter);
			membersListView.setOnItemClickListener(this);

			buttonNext.setOnClickListener(this);
		}
		membersListView.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

		((MembersArrayAdapter) membersListView.getAdapter()).dataChanged();
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View view, int position,
			long id) {
		// Just move on for now
		Log.d("Member List", "Selected HRE");
		TrelloMember item = (TrelloMember) memberAdapter.getItem(position);
		Log.d("Member List", "Selected:" + item.getFullname());
		
		if(item.getId() == null){
			//Add new member
			Intent go = new Intent(this, AddMembersList.class);
			startActivity(go);
		}
	}
}
