package edu.purdue.autogenics.trello.internet;

public class TrelloList {
	private String id;
	private String name;
	private String board_id;
	private String date;
	
	private String name_keyword;
	private String desc_keyword;
	private String owner;
	
	public TrelloList(){
		
	}
	public TrelloList(String newId, String newName, String newBoardId, String newDate, String newNameKeyword, String newDescKeyword, String newOwner){
		setId(newId);
		setName(newName);
		setBoardId(newBoardId);
		setDate(newDate);
		setNameKeyword(newNameKeyword);
		setDescKeyword(newDescKeyword);
		setOwner(newOwner);
	}
	
	public void setId(String newId){
		id = newId;
	}
	public void setName(String newName){
		name = newName;
	}
	public void setBoardId(String newBoardId){
		board_id = newBoardId;
	}
	public void setDate(String newDate){
		date = newDate;
	}
	public void setNameKeyword(String newNameKeyword){
		name_keyword = newNameKeyword;
	}
	public void setDescKeyword(String newDescKeyword){
		desc_keyword = newDescKeyword;
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
	public String getBoardId(){
		return board_id;
	}
	public String getDate(){
		return date;
	}
	public String getNameKeyword(){
		return name_keyword;
	}
	public String getDescKeyword(){
		return desc_keyword;
	}
	public String getOwner(){
		return owner;
	}
	
}
