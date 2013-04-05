package edu.purdue.autogenics.trello.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Semaphore;

import edu.purdue.autogenics.libcommon.trello.IntentBoard;
import edu.purdue.autogenics.libcommon.trello.IntentList;
import edu.purdue.autogenics.libcommon.trello.TrelloRequest;
import edu.purdue.autogenics.trello.internet.BoardsHandler;
import edu.purdue.autogenics.trello.internet.CardsHandler;
import edu.purdue.autogenics.trello.internet.ListsHandler;
import edu.purdue.autogenics.trello.internet.TrelloBoard;
import edu.purdue.autogenics.trello.internet.TrelloCard;
import edu.purdue.autogenics.trello.internet.TrelloList;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

public class TrelloService extends Service {
	static final int registerClient = 9999;
	static final int unregisterClient= 9998;
	static final int shutdownService = 9994;
		
	private Boolean serviceIsRunning = false; // Is service started already
	private serviceHandler myServiceHandler = new serviceHandler(this);
	final Messenger mMessenger = new Messenger(myServiceHandler);
	
	private BoardsHandler boardHandler = new BoardsHandler(this);
	private ListsHandler listHander = new ListsHandler(this);
	private CardsHandler cardHandler = new CardsHandler(this);
	
	protected List<Bundle> intentData = new  ArrayList<Bundle>();
	private Boolean looping = false;
	
	private List<String> waitingList = new  ArrayList<String>();
	
	/** 
	* A constructor is required, and must call the super IntentService(String)
	* constructor with a name for the worker thread.
	*/
	public TrelloService() {
		super();
	}

	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		//android.os.Debug.waitForDebugger();
		Log.d("TrelloService","Binding");
		if(serviceIsRunning == false){
			//Do initial startups, most done in serviceHandler constructor
			serviceIsRunning = true;
		}
		return mMessenger.getBinder();
	}
	
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	class TestTask extends AsyncTask<String, Void, String> {
	     protected String doInBackground(String... urls) {
	    	 try {
	 			for(int i=0; i < 10; i++){
	 				Thread.sleep(1000);
	 				String sleeping = "Sleeping" + Integer.toString(i + 1);
	 				Log.d("TrelloService", sleeping);
	 			}
	 		} catch (InterruptedException e) {
	 			// TODO Auto-generated catch block
	 			e.printStackTrace();
	 		}
	        return urls[0];
	     }

	     protected void onPostExecute(String result) {
	    	 Log.d("TrelloService", "DONE! :" + result);
	     }
	}
	
	public void addWaiting(String Id){
		Log.d("TrelloService - Worker Thread", "Adding id to waiting list:" + Id);
		synchronized(waitingList){
			waitingList.add(Id);
		}
	}
	
	private void processJobs(){
		//Looper thread, loops through intents in intentData starting them until none left, one at a time
		
		//Get an intent to process
		Boolean jobsLeft = true;
		
		while(jobsLeft){
			Log.d("TrelloService - Intent Looper", "Looping");
			Bundle currentJob = null;
			synchronized(intentData){
				if(intentData.isEmpty()){
					jobsLeft = false;
				} else {
					currentJob = intentData.remove(0);
				}
			}
			Boolean isSyncing = false;
			if(currentJob != null){
				if(currentJob.containsKey("sync")){
					Log.d("TrelloService - Intent Looper", "Starting sync job");
					isSyncing = true;
				}
				
				//Do job
				handleRequest(currentJob);
				
				//Job is done
				//If was syncing job wait until no more "-"'s inside the intent que that were there before the sync
				//Weights - Also have to account for what type of sync it is, because cards syncs cards, lists, and boards. Etc.
				while(isSyncing){
					Boolean stillWaiting = false;
					Log.d("TrelloService - Intent Looper", "Done syncing, checking if id's still in waiting list" );

					synchronized(waitingList){
						if(waitingList.isEmpty() == false) stillWaiting = true;
					}
					
					//Check if any id's 
					if(stillWaiting){
						Log.d("TrelloService - Intent Looper", "Waiting on id replacements after sync");
						//Check if any data requests, we want to process these
						Boolean found = true;
						while(found) {
							found = false;
							Bundle dataJob = null;
							String waitingId = null;
							synchronized(intentData) {
								Iterator<Bundle> it = intentData.iterator();
						    	while(it.hasNext() && found == false)
						    	{
						    		Bundle data = it.next();
						    		if(data.containsKey("data")){
						    			dataJob = data;
						    			it.remove();
						    			found = true;
						    		} else if(data.containsKey("updateIdComplete")){
										Log.d("TrelloService - HANDLER", "updateIdComplete Intent Recieved");
						    			//Update any id's in the intent que with this new id, also remove from wait list
						    			if(data.containsKey("id") && data.containsKey("newId")){
						    				String intentId = data.getString("id");
						    				String intentNewId = data.getString("newId");
							    			it.remove();
							    			
							    			//Loop again changing any others
							    			Iterator<Bundle> it2 = intentData.iterator();
									    	while(it2.hasNext())
									    	{
									    		Bundle intentData = it2.next();
									    		if(intentData.containsKey("id")){
									    			if(intentData.getString("id").contentEquals(intentId)){
														Log.d("TrelloService - HANDLER", "Updated id of an intent in the que");
									    				intentData.putString("id", intentNewId);
									    			}
									    		}
									    	}
									    	//Remove from waiting list
									    	waitingId = intentId;
						    			}
					    				found = true;
						    		} else if(data.containsKey("deleteComplete")){
										Log.d("TrelloService - HANDLER", "deleteComplete Intent Recieved");
						    			//Update any id's in the intent que with this new id, also remove from wait list
					    				String intentId = data.getString("deleteComplete");
						    			it.remove(); //Remove intent
						    			
						    			//Loop again deleting any others
						    			Iterator<Bundle> it2 = intentData.iterator();
								    	while(it2.hasNext())
								    	{
								    		Bundle intentData = it2.next();
								    		if(intentData.containsKey("id")){
								    			if(intentData.getString("id").contentEquals(intentId)){
													Log.d("TrelloService - HANDLER", "Removed intent in the que");
								    				it2.remove(); //Remove any other intents for this id
								    			}
								    		}
								    	}
								    	//Remove from waiting list
								    	waitingId = intentId;
					    				found = true;
						    		}
						    	}
							}
							if(waitingId != null){
								synchronized(waitingList) {
									if(waitingList.contains(waitingId)) waitingList.remove(waitingId);
								}
							}
							//If have a data job process it
							if(dataJob != null){
								handleRequest(dataJob);
							}
						} 
						//Wait a little bit
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						//TODO eventual timeout?? Then remove intents with "-"'s?
					} else {
						isSyncing = false;
					}
				}
			}
		}
		synchronized(looping){
			looping = false;
		}
		Log.d("TrelloService - Intent Looper", "Finished processing jobs");
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		//Store intents in an array, poll for progress, start one at a time
		Log.d("TrelloService", "RECEIVED INTENT");
  	    
		
		
		Bundle data = intent.getExtras();
		Boolean addedJob = false;
		if(data != null){
			//Check to see if its a change intent id
			if(data.containsKey("push")){
				Log.d("TrelloService", data.getString("push") + " Push request for:" + data.getString("id") + " added to que");
			}else if(data.containsKey("data")){
				Log.d("TrelloService", data.getString("data") + " Data request for:" + data.getString("id") + " added to que");
			} else if(data.containsKey("data")){
				Log.d("TrelloService", "updateIdComplete intent");
			} else if(data.containsKey("delete")){
				Log.d("TrelloService", data.getString("delete") + " Delete request for:" + data.getString("id") + " added to que");
			}
			
			addedJob = true;
			synchronized(intentData){
		  	    intentData.add(intent.getExtras());
			}
		}
		if(addedJob){
			synchronized(looping){
				if(looping == false){
					//Start looping jobs
					looping = true;
					Log.d("TrelloService", "Starting Intent Looper");
					new Thread(new Runnable() {
					    public void run() {
					    	processJobs();
					    }
					  }).start();
				}
			}
		}
		return START_STICKY;
	}
	
	private void handleRequest(Bundle data){
		if(data != null){
			Thread activeThread = null;
			if(data.containsKey("push")){
				Log.d("TrelloService - HANDLER", "Push Intent Recieved");
				if(data.getString("push").contentEquals("board")){
					String newId = data.getString("id");
					String newName = data.getString("name");
					String newDesc = data.getString("desc");
					String newNameKeyword = data.getString("nameKeyword");
					String newDescKeyword = data.getString("descKeyword");
					String newOwner = data.getString("owner");
					Log.d("TrelloService - HANDLER", "Pushing Board:" + newName);
					final TrelloBoard newBoard = new TrelloBoard(newId, newName, newDesc, newNameKeyword, newDescKeyword, newOwner, 0);
					activeThread = new Thread(new Runnable() {
					    public void run() {
					    	boardHandler.pushBoard(newBoard);
					    }
					  });
				} else if(data.getString("push").contentEquals("list")){
					String newId = data.getString("id");
					String newName = data.getString("name");
					String newBoardId = data.getString("boardId");
					String newNameKeyword = data.getString("nameKeyword");
					String newOwner = data.getString("owner");
					Log.d("TrelloService - HANDLER", "Pushing List:" + newName);
					final TrelloList newList = new TrelloList(newId, newName, newBoardId, newNameKeyword, newOwner, 0);
					activeThread = new Thread(new Runnable() {
					    public void run() {
					    	listHander.pushList(newList);
					    }
					  });
				} else if(data.getString("push").contentEquals("card")){
					String newId = data.getString("id");
					String newName = data.getString("name");
					String newDesc = data.getString("desc");
					String newListId = data.getString("listId");
					String newOwner = data.getString("owner");
					Log.d("TrelloService - HANDLER", "Pushing Card:" + newName);
					final TrelloCard newCard = new TrelloCard(newId, newName, newDesc, newListId, null, newOwner, 0);
					activeThread = new Thread(new Runnable() {
					    public void run() {
					    	cardHandler.pushCard(newCard);
					    }
					  });
				} else if(data.getString("push").contentEquals("watchcard")){
					String newId = data.getString("id");
					String newKeyword = data.getString("nameKeyword");
					String newListId = data.getString("listId");
					String newOwner = data.getString("owner");
					Log.d("TrelloService - HANDLER", "Pushing WatchCard:" + newKeyword);
					final TrelloCard newCard = new TrelloCard(newId, newKeyword, null, newListId, null, newOwner, 0);
					activeThread = new Thread(new Runnable() {
					    public void run() {
					    	cardHandler.pushWatchCard(newCard);
					    }
					  });
				}
			} else if(data.containsKey("data")){
				Log.d("TrelloService - HANDLER", "Data Intent Recieved");
				if(data.getString("data").contentEquals("board")){
					//Shouldn't happen now, name/desc stored in BoardsTable
				} else if(data.getString("data").contentEquals("list")){
					//Shouldn't happen now, name stored in ListsTable
				} else if(data.getString("data").contentEquals("card")){
					String theId = data.getString("id");
					String theName = data.getString("name");
					String theDesc = data.getString("desc");
					String theListId = data.getString("listId");
					String theOwner = data.getString("owner");
					Log.d("TrelloService - HANDLER", "Card data:" + theName);
					final TrelloCard theCard = new TrelloCard(theId, theName, theDesc, theListId, null, theOwner, 0);
					activeThread = new Thread(new Runnable() {
					    public void run() {
							cardHandler.processDataResponse(theCard);
					    }
					  });
				}
			} else if(data.containsKey("delete")){
				Log.d("TrelloService - HANDLER", "Delete Intent Recieved");
				if(data.getString("delete").contentEquals("board")){
					//TODO
				} else if(data.getString("delete").contentEquals("list")){
					//TODO
				} else if(data.getString("delete").contentEquals("card")){
					String theId = data.getString("id");
					final TrelloCard theCard = new TrelloCard(theId, null, null, null, null, null, 0);
					Log.d("TrelloService - HANDLER", "Removing Card:" + theId);
					activeThread = new Thread(new Runnable() {
					    public void run() {
					    	cardHandler.deleteCard(theCard);
					    }
					  });
				}
			} else if(data.containsKey("sync")){
				//TODO overall sync function?? Checks if needs syncing (Timelimit??)
				Log.d("TrelloService - HANDLER", "Sync Intent Recieved");
				if(data.getString("sync").contentEquals("boards")){
					Log.d("TrelloService - HANDLER", "Syncing boards");
					activeThread = new Thread(new Runnable() {
					    public void run() {
					    	boardHandler.syncBoards();
					    }
					  });
				} else if(data.getString("sync").contentEquals("lists")){
					Log.d("TrelloService - HANDLER", "Syncing lists");
					activeThread = new Thread(new Runnable() {
					    public void run() {
					    	if(boardHandler.needsSync()){
					    		Log.d("TrelloService", "Boards need synced first");
					    		boardHandler.syncBoards();
					    	}
					    	listHander.syncLists();
					    }
					  });
				} else if(data.getString("sync").contentEquals("cards")){
					Log.d("TrelloService - HANDLER", "Syncing cards");
					activeThread = new Thread(new Runnable() {
					    public void run() {
					    	if(listHander.needsSync()){
					    		Log.d("TrelloService", "Lists need synced first");
					    		listHander.syncLists();
					    	}
					    	cardHandler.syncCards();
					    }
					  });
					
				}
			}
			if(activeThread != null)
			{
				activeThread.start();
				try {
					activeThread.join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					Log.d("TrelloService - HANDLER", "Error joining thread");
					e.printStackTrace();
				}
				Log.d("TrelloService - HANDLER", "Completed Request");
			}
			if(data.containsKey("Test1")){
				Log.d("TrelloService - HANDLER", "Doing work Test1");
				new Thread(new Runnable() {
				    public void run() {
				    	//boardHandler.test1();
				    }
				  }).start();
			}
			

		}
	}
	
	/*
	Class that handles connection between Activities and the service
	*/
	private static class serviceHandler extends Handler {
		//Handlers all messages to activities
		ArrayList<Messenger> mClients = new ArrayList<Messenger>();
		TrelloService parentService = null;
		
		
		public  serviceHandler(TrelloService trelloService){
			Log.d("TrelloService","Created service handler");
			parentService = trelloService;
		}
		public void handleMessage(Message msg){
			//arg1 is the destination
			if(msg.what == registerClient){
				Log.d("TrelloService","Registered");
				//Link a UI handler
				mClients.add(msg.replyTo);
			} else if(msg.what == unregisterClient){
				mClients.remove(msg.replyTo);
			} else if(msg.what == shutdownService){
				//Check if should stop service
				shutdownService();
			} else {
				//Send message to all clients
				//super.handleMessage(msg);
				
				for (int i = mClients.size() - 1; i >= 0; i--) {
					try {
						// Send message to registered activities
						Message sendMsg = Message.obtain();
						sendMsg.copyFrom(msg);
						mClients.get(i).send(sendMsg);
					} catch (RemoteException e) {
						//The client is dead, remove
						mClients.remove(i);
					}
				}
			}
		}
		private void shutdownService(){
			Log.d("TrelloService","Shutting down");
			parentService.stopSelf();
		}
	}
}
