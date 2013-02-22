package com.vip.trello;

import java.util.List;

import com.vip.trello.internet.App;
import com.vip.trello.internet.Organization;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AppsArrayAdapter extends ArrayAdapter<App> {
	private final Context context;
	private List<App> apps = null;
	private int resId;

	public AppsArrayAdapter(Context context, int layoutResourceId, List<App> data) {
		super(context, layoutResourceId, data);
		this.resId = layoutResourceId;
		this.context = context;
		this.apps = data;
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
    }
}
