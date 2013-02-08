package com.vip.trello.service;

import java.util.ArrayList;

import com.vip.trello.internet.BoardsHandler;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class TrelloService extends Service {
	static final int registerClient = 9999;
	static final int unregisterClient= 9998;
	static final int shutdownService = 9994;
		
	private Boolean serviceIsRunning = false; // Is service started already
	private serviceHandler myServiceHandler = new serviceHandler(this);
	final Messenger mMessenger = new Messenger(myServiceHandler);
	
	private BoardsHandler boardHandler = new BoardsHandler(this);
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
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		Log.d("TrelloService", "RECEIVED INTENT");
		
		Bundle data = intent.getExtras();		
		if(data.containsKey("type")){
			if(data.getString("type").contentEquals("card")){
				
			} else if(data.getString("type").contentEquals("list")){
				
			} else if(data.getString("type").contentEquals("board")){
				Log.d("TrelloService", "Passing to BoardHandler");
				boardHandler.handle(data);
			}
		}
		
		if(data.containsKey("listener") && data.getString("listener").contentEquals("true")){
			//Add id to listener table
			
		}
		return START_STICKY;
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
