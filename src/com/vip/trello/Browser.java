package com.vip.trello;

import com.vip.trello.R;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;


public class Browser extends Activity {

	WebView browser = null;
	private static final String magicString = "25az225MAGICee4587da";
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.browser);
		browser = (WebView) findViewById(R.id.browser);
		browser.getSettings().setJavaScriptEnabled(true);
		
		browser.setWebChromeClient(new PageHandler(this));
		browser.setWebViewClient(new WebViewClient() {
			public void onPageFinished(WebView view, String address){
				view.loadUrl("javascript:console.log('"+magicString+"'+'"+address+" - '+document.getElementsByTagName('html')[0].innerHTML);");
			}
		});		
		
		Intent todo = getIntent();
		if(todo.getBooleanExtra("Setup", false)){
			//Go to trello signup screen
			browser.loadUrl("https://trello.com/signup");
		}
		//browser.loadUrl("https://google.com");
	}
	
	private class APIKeyHandler extends WebChromeClient {
		WebView apiBrowser = null;
		String apiKey = null;
		String token = null;
		String secret = null;
		
		Context appContext = null;
		
		public APIKeyHandler(WebView theBrowser, Context theContext){
			apiBrowser = theBrowser;
			appContext = theContext;
		}
		public boolean onConsoleMessage(ConsoleMessage cmsg){
			if(cmsg.message().startsWith(magicString)){
				
				String categoryMsg = cmsg.message().substring(magicString.length());
				if(categoryMsg.startsWith("KEY")){
					String msg = categoryMsg.substring(3);
					apiKey = msg;
					
					logMessage("The key: " + msg);
				} else if (categoryMsg.startsWith("SECRET")){
					String msg = categoryMsg.substring(6);
					secret = msg;
					
					logMessage("The secret: " + msg);
				} else if (categoryMsg.startsWith("TOKEN")){
					//After we automatically approve
					String msg = categoryMsg.substring(5);
					token = msg;
					logMessage("The token: " + msg);
				}
				if(apiKey!= null && secret != null && token == null){
					//Request the token
					apiBrowser.setWebViewClient(new WebViewClient() {
						public void onPageFinished(WebView view, String address){
							//Setup what to do after we approve
							apiBrowser.setWebViewClient(new WebViewClient() {
								public void onPageFinished(WebView view, String address){
									//Save the token
									view.loadUrl("javascript:console.log('"+magicString+"'+'TOKEN'+document.getElementsByTagName('pre')[0].innerHTML);");
								}
							});
							
							//Auto approve
							view.loadUrl("javascript:document.getElementsByTagName('form')[0].approve.click();");
						}
					});
					//Ask for approve of Application
					apiBrowser.loadUrl("https://trello.com/1/authorize?key="+
							apiKey+"&name=Test+Create+Trello&expiration=never&response_type=token&scope=read,write");
				}
				if(apiKey != null && secret != null && token != null){
					//Have all API keys
					//TODO store in database
					
					//Go to organization list
					Intent go = new Intent(appContext, OrganizationsList.class);
					startActivity(go);
				}
				return true;
			}
			return false;
		}
	}
	
	
	private class PageHandler extends WebChromeClient {
		Context appContext = null;
		public PageHandler(Context theContext){
			appContext = theContext;
		}
		
		public boolean onConsoleMessage(ConsoleMessage cmsg){
			if(cmsg.message().startsWith(magicString)){
				String categoryMsg = cmsg.message().substring(magicString.length());
				if(categoryMsg.contains("https://trello.com/ - ")){
					Integer intStartTitle = categoryMsg.indexOf("<title>", 0);
					Integer intEndTitle = categoryMsg.indexOf("</title>", intStartTitle);
					String title = categoryMsg.substring(intStartTitle + "<title>".length(), intEndTitle);
					logMessage(title);
					//Got name and username, try to find API key's
					//TODO store/look in database
					
					//Done loading get API KEY's if needed
					WebView apiBrowswer = new WebView(getApplicationContext());
					apiBrowswer.getSettings().setJavaScriptEnabled(true);
					apiBrowswer.setWebChromeClient(new APIKeyHandler(apiBrowswer, appContext));
					apiBrowswer.setWebViewClient(new WebViewClient() {
						public void onPageFinished(WebView view, String address){
							view.loadUrl("javascript:console.log('"+magicString+"'+'KEY'+document.getElementById('key').value);" +
									"console.log('"+magicString+"'+'SECRET'+document.getElementById('secret').value);");
						}
					});
					//Load api key page
					apiBrowswer.loadUrl("https://trello.com/1/appKey/generate");
				}
				if(categoryMsg.contains("@cyrusbowman")){
					logMessage("True");
					Integer intStartTitle = categoryMsg.indexOf("@cyrusbowman", 0);
					logMessage(categoryMsg.substring(intStartTitle-100));
				}
				return true;
			}
			return false;
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if(event.getAction() == KeyEvent.ACTION_DOWN){
            switch(keyCode)
            {
            case KeyEvent.KEYCODE_BACK:
                if(browser.canGoBack() == true){
                	browser.goBack();
                }else{
                    finish();
                }
                return true;
            }

        }
		return super.onKeyDown(keyCode, event);
	}

	
	public void logMessage(String msg){
		if (msg.length() > 4000) {
		    Log.v("Trello", "sb.length = " + msg.length());
		    int chunkCount = msg.length() / 4000;     // integer division
		    for (int i = 0; i < chunkCount; i++) {
		        int max = 4000 * (i + 1);
		        if (max >= msg.length()) {
		            Log.v("Trello", "chunk " + (i+1) + " of " + chunkCount + ":" + msg.substring(4000 * i));
		        } else {
		            Log.v("Trello", "chunk " + (i+1) + " of " + chunkCount + ":" + msg.substring(4000 * i, max));
		        }
		    }
		} else {
			Log.v("Trello", msg);
		}
	}
}
