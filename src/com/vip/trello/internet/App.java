package com.vip.trello.internet;

import android.graphics.drawable.Drawable;

public class App {
	private String packageName = null;
	private String name = null;
	private String desc = null;
	private Drawable icon = null;
	
	public App() {
	
	}
	
	public App(String PackageName, String Name, String Description, Drawable Icon) {
		if(PackageName != null) setPackage(packageName);
		if(Name != null) setName(Name);
		if(Description != null) setDesc(Description);
		if(Icon != null) setIcon(Icon);
	}
	
	public void setPackage(String newPackage){
		packageName = newPackage;
	}
	public void setName(String newName){
		name = newName;
	}
	public void setDesc(String newDesc){
		desc = newDesc;
	}
	public void setIcon(Drawable newIcon){
		icon = newIcon;
	}
	
	public String getName(){
		if(name != null) return name;
		return "No Name";
	}
	public String getDesc(){
		if(desc != null) return desc;
		return "";
	}
	public String getPackageName(){
		return packageName;
	} 
	public Drawable getIcon(){
		return icon;
	} 
}
