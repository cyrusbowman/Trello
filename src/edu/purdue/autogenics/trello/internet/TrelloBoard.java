package edu.purdue.autogenics.trello.internet;

public class TrelloBoard {
	private String id;
	private String name;
	private String desc;
	
	private String name_keyword;
	private String desc_keyword;
	private String owner;
	private Integer synced;
	
	public TrelloBoard(){
		
	}
	public TrelloBoard(String newId, String newName, String newDesc, String newNameKeyword, String newDescKeyword, String newOwner, Integer newSynced){
		setId(newId);
		setName(newName);
		setDesc(newDesc);
		setNameKeyword(newNameKeyword);
		setDescKeyword(newDescKeyword);
		setOwner(newOwner);
		setSynced(newSynced);
	}
	
	public void setId(String newId){
		id = newId;
	}
	public void setName(String newName){
		name = newName;
	}
	public void setDesc(String newDesc){
		desc = newDesc;
	}
	public void setSynced(Integer newSynced){
		synced = newSynced;
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
	public String getDesc(){
		return desc;
	}
	public Integer getSynced(){
		return synced;
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
