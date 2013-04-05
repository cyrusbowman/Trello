package edu.purdue.autogenics.trello.internet;

import java.util.ArrayList;
import java.util.List;


public class TrelloOrganization {
	private String id = null;
	private String name = null;
	private String displayName = null;
	private String desc = null;
	
	private List<TrelloBoard> boards = null;
	
	public TrelloOrganization() {
	
	}
	
	public TrelloOrganization(String ID, String Name, String DisplayName, String Description) {
		if(ID != null) setId(ID);
		if(Name != null) setName(Name);
		if(DisplayName != null) setDisplayName(DisplayName);
		if(Description != null) setDesc(Description);
		boards = new ArrayList<TrelloBoard>();
	}
	
	public void setId(String newId){
		id = newId;
	}
	public void setName(String newName){
		name = newName;
	}
	public void setDisplayName(String newName){
		displayName = newName;
	}
	public void setDesc(String newDesc){
		desc = newDesc;
	}
	
	public void addBoard(TrelloBoard newBoard){
		if(newBoard != null){
			//boards.add(newBoard); TODO
		}
	}
	
	public String getId(){
		return id;
	}
	
	public List<TrelloBoard>getBoards(){
		return boards;
	}
	
	public TrelloBoard getBoard(int Index){
		return boards.get(Index);
	}
	
	public String getDisplayName(){
		if(displayName == null){
			return "No Name";
		} else {
			return displayName;
		}
	}
}
