package edu.purdue.autogenics.trello.internet;

public class TrelloCard {
	private String id;
	private String name;
	private String desc;
	private String list_id;
	private String date;
	private String owner;
	
	public TrelloCard(){
		
	}
	public TrelloCard(String newId, String newName, String newDesc, String newListId, String newDate, String newOwner){
		setId(newId);
		setName(newName);
		setDesc(newDesc);
		setListId(newListId);
		setDate(newDate);
		setOwner(newOwner);
	}
	
	public void setId(String newId){
		id = newId;
	}
	public void setName(String newName){
		name = newName;
	}
	public void setListId(String newListId){
		list_id = newListId;
	}
	public void setDate(String newDate){
		date = newDate;
	}
	public void setDesc(String newDesc){
		desc = newDesc;
	}
	public void setOwner(String newOwner){
		owner = newOwner;
	}
	
	public String getId(){
		return id;
	}
	public String getName(){
		return name;
	}
	public String getListId(){
		return list_id;
	}
	public String getDate(){
		return date;
	}
	public String getDesc(){
		return desc;
	}
	public String getOwner(){
		return owner;
	}
	
}
