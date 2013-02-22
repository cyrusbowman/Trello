package com.vip.trello;

import java.util.List;

import com.vip.trello.internet.Organization;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class OrganizationArrayAdapter extends ArrayAdapter<Organization> {
	private final Context context;
	private List<Organization> organizations = null;
	private int resId;

	public OrganizationArrayAdapter(Context context, int layoutResourceId, List<Organization> data) {
		super(context, layoutResourceId, data);
		this.resId = layoutResourceId;
		this.context = context;
		this.organizations = data;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		View row = convertView;
		OrganizationHolder holder = null;
		
		
		if(row == null){
			LayoutInflater inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			row = inflater.inflate(resId, parent, false);
			holder = new OrganizationHolder();
			holder.txtTitle = (TextView) row.findViewById(R.id.organizationTitle);
			row.setTag(holder);
		} else {
			holder = (OrganizationHolder) row.getTag();
		}
		
		holder.txtTitle.setText(organizations.get(position).getDisplayName());
		
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
	
	static class OrganizationHolder
    {
        ImageView imgIcon;
        TextView txtTitle;
    }
}
