package edu.purdue.autogenics.trello;

import java.util.List;

import edu.purdue.autogenics.trello.R;

import edu.purdue.autogenics.trello.database.AppsTable;
import edu.purdue.autogenics.trello.database.DatabaseHandler;
import edu.purdue.autogenics.trello.internet.App;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;

public class AppsArrayAdapter extends ArrayAdapter<App> {
	private final Context context;
	private List<App> apps = null;
	private int resId;
	
	private SQLiteDatabase database;
	private DatabaseHandler dbHandler;
	
	
	private OnCheckedChangeListener chkListener = new OnCheckedChangeListener(){
		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			final App parentApp = (App) buttonView.getTag();
			
			//Save this selection in database
			database = dbHandler.getWritableDatabase();
			
			String[] columns = { AppsTable.COL_ID, AppsTable.PACKAGE_NAME, AppsTable.COL_ALLOW_SYNCING };
			
			Cursor cursor = database.query(AppsTable.TABLE_NAME,
					columns, AppsTable.PACKAGE_NAME + " = '" + parentApp.getPackageName() + "'", null,
		        null, null, null);
			
			
	    	final Integer newValue = isChecked ? 1 : 0;
	    	ContentValues values = new ContentValues();
	    	values.put(AppsTable.COL_ALLOW_SYNCING, newValue);
	    	
			Long id = null;
			Integer selected = null;
		    if(cursor.moveToFirst()){
		    	id = cursor.getLong(cursor.getColumnIndex(AppsTable.COL_ID));
			    selected = cursor.getInt(cursor.getColumnIndex(AppsTable.COL_ALLOW_SYNCING));
			    if((selected == 0 && isChecked == true) || (selected == 1 && isChecked == false)){
			    	//update
			    	database.update(AppsTable.TABLE_NAME, values,(AppsTable.COL_ID + "=" + id.toString()),null);
			    }
		    } else {
            	ContentValues newValues = new ContentValues();
            	newValues.put(AppsTable.COL_ALLOW_SYNCING, newValue);
            	newValues.put(AppsTable.COL_NAME, parentApp.getName());
            	newValues.put(AppsTable.PACKAGE_NAME, parentApp.getPackageName());
 		    	database.insert(AppsTable.TABLE_NAME, null, newValues);
		    }
		    parentApp.setSyncApp(isChecked);
			Log.d("Selected", parentApp.getName());
			cursor.close();
			dbHandler.close();
			
			if(isChecked){
				//Send intent to package to enable trello syncing

				Log.d("Syncing enabled on:", parentApp.getPackageName());
				Intent sendIntent = new Intent();
				Bundle extras = new Bundle();
				extras.putString("sync", "true");
				sendIntent.setAction(Intent.ACTION_SEND);
				sendIntent.setPackage(parentApp.getPackageName());
				sendIntent.putExtras(extras);
				context.startService(sendIntent);
			} else {
				//Send intent to package to disable trello syncing

				Log.d("Syncing disabled on:", parentApp.getPackageName());
				Intent sendIntent = new Intent();
				Bundle extras = new Bundle();
				extras.putString("sync", "false");
				sendIntent.setAction(Intent.ACTION_SEND);
				sendIntent.setPackage(parentApp.getPackageName());
				sendIntent.putExtras(extras);
				context.startService(sendIntent);
			}
			
		}
	};

	public AppsArrayAdapter(Context context, int layoutResourceId, List<App> data) {
		super(context, layoutResourceId, data);
		this.resId = layoutResourceId;
		this.context = context;
		this.apps = data;
		dbHandler = new DatabaseHandler(this.context);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		View row = convertView;
		AppHolder holder = null;
		
		if(row == null){
			LayoutInflater inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			row = inflater.inflate(resId, parent, false);

			holder = new AppHolder();
			holder.txtTitle = (TextView) row.findViewById(R.id.appName);
			holder.imgIcon = (ImageView) row.findViewById(R.id.appIcon);
			holder.imgIconSizer = (ImageView) row.findViewById(R.id.appIconSizer);
			holder.chkSyncing = (CheckBox) row.findViewById(R.id.appChkSyncThisApp);
			holder.chkSyncing.setChecked(apps.get(position).getSyncApp());
			
			//TODO different color for uninstalled
			if(apps.get(position).getInstalled() == false){
				holder.chkSyncing.setEnabled(false);
			}
			
			holder.chkSyncing.setTag(apps.get(position));
			holder.chkSyncing.setOnCheckedChangeListener(chkListener);
			row.setTag(holder);
		} else {
			holder = (AppHolder) row.getTag();
		}
		
		if(apps == null){
			Log.d("AppsArrayAdapter", "apps null");
		} else {
			Log.d("AppsArrayAdapter", "Length:" + Integer.toString(apps.size()));
			Log.d("AppsArrayAdapter", "Pos:" + Integer.toString(position));
			Log.d("AppsArrayAdapter", "PackName:" + apps.get(position).getPackageName());
			Log.d("AppsArrayAdapter", "Name:" + apps.get(position).getName());
		}
		
		if(holder == null){
			Log.d("AppsArrayAdapter", "holder null");
		} else {
			if(holder.txtTitle == null){
				Log.d("AppsArrayAdapter", "txtTitle null");
			} else {
				holder.txtTitle.setText("test");
			}
		}
		
		holder.txtTitle.setText(apps.get(position).getName());
		holder.imgIcon.setImageDrawable(apps.get(position).getIcon());
		holder.imgIconSizer.setImageDrawable(apps.get(position).getIcon());
		
				//ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
		// Change the icon for Windows and iPhone
//		String s = values[position];
//		if (s.startsWith("iPhone")) {
//			imageView.setImageResource(R.drawable.no);
//		} else {
//			imageView.setImageResource(R.drawable.ok);
//		}
		return row;
	}
	
	static class AppHolder
    {
        ImageView imgIcon;
        ImageView imgIconSizer; //To size right linear layout height value
        TextView txtTitle;
        CheckBox chkSyncing;
    }
}
