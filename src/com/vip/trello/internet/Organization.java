package com.vip.trello.internet;

import java.util.ArrayList;
import java.util.List;



public class Organization {
	private String id = null;
	private String name = null;
	private String displayName = null;
	private String desc = null;
	
	private List<Board> boards = null;
	
	public Organization() {
	
	}
	
	public Organization(String ID, String Name, String DisplayName, String Description) {
		if(ID != null) setId(ID);
		if(Name != null) setName(Name);
		if(DisplayName != null) setDisplayName(DisplayName);
		if(Description != null) setDesc(Description);
		boards = new ArrayList<Board>();
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
	
	public void addBoard(Board newBoard){
		if(newBoard != null){
			boards.add(newBoard);
		}
	}
	
	public List<Board>getBoards(){
		return boards;
	}
	
	public Board getBoard(int Index){
		return boards.get(Index);
	}
	
	public String toString(){
		return displayName;
	}
}
