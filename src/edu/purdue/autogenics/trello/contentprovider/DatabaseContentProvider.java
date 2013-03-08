package edu.purdue.autogenics.trello.contentprovider;

import java.util.Arrays;
import java.util.HashSet;

import edu.purdue.autogenics.trello.database.BoardsOwnerFinder;
import edu.purdue.autogenics.trello.database.BoardsTable;
import edu.purdue.autogenics.trello.database.CardsTable;
import edu.purdue.autogenics.trello.database.DatabaseHandler;
import edu.purdue.autogenics.trello.database.ListenersTable;
import edu.purdue.autogenics.trello.database.ListsTable;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

public class DatabaseContentProvider extends ContentProvider  {

	private DatabaseHandler database;
	
	//Used for the UriMatcher
	private static final int URIMATCH_BOARDS = 10;
	private static final int URIMATCH_BOARD_ID = 20;
	
	private static final int URIMATCH_LISTS = 30;
	private static final int URIMATCH_LIST_ID = 40;
	
	private static final int URIMATCH_CARDS = 50;
	private static final int URIMATCH_CARD_ID = 60;	
	
	private static final int URIMATCH_LISTENERS = 70;
	private static final int URIMATCH_LISTENER_ID = 80;	 //Trello_ID
	
	private static final int URIMATCH_BOARDS_OWNERFINDER = 90;
	private static final int URIMATCH_BOARDS_OWNERFINDER_ID = 100;
	
	
	private static final String AUTHORITY = "edu.purdue.autogenics.trello.contentprovider";
	
	//Predefined tables
	public static final String BOARDS_PATH = "boards";
	public static final String LISTS_PATH = "lists";
	public static final String CARDS_PATH = "cards";
	public static final String LISTENERS_PATH = "listeners";
	public static final String BOARDS_OWNERFINDER_PATH = "boards_ownerfinder";
	
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/");

	public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/boards";
	public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/board";

	
	//Add URI's of other tables somehow
	private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	static {
		sURIMatcher.addURI(AUTHORITY, BOARDS_PATH, URIMATCH_BOARDS);
		sURIMatcher.addURI(AUTHORITY, BOARDS_PATH + "/*", URIMATCH_BOARD_ID);
		
		sURIMatcher.addURI(AUTHORITY, LISTS_PATH, URIMATCH_LISTS);
		sURIMatcher.addURI(AUTHORITY, LISTS_PATH + "/*", URIMATCH_LIST_ID);
		
		sURIMatcher.addURI(AUTHORITY, CARDS_PATH, URIMATCH_CARDS);
		sURIMatcher.addURI(AUTHORITY, CARDS_PATH + "/*", URIMATCH_CARD_ID);
		
		sURIMatcher.addURI(AUTHORITY, LISTENERS_PATH, URIMATCH_CARDS);
		sURIMatcher.addURI(AUTHORITY, LISTENERS_PATH + "/*", URIMATCH_CARD_ID);
		
		sURIMatcher.addURI(AUTHORITY, BOARDS_OWNERFINDER_PATH, URIMATCH_BOARDS_OWNERFINDER);
		sURIMatcher.addURI(AUTHORITY, BOARDS_OWNERFINDER_PATH + "/#", URIMATCH_BOARDS_OWNERFINDER_ID);
	}
	
	@Override
	public boolean onCreate() {
		database = new DatabaseHandler(getContext());
		return false;
	}
		
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		// Using SQLiteQueryBuilder instead of query() method
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		
		// Check if the caller has requested a column which does not exists
		//checkColumns(projection);  //Loop through??
				
		int uriType = sURIMatcher.match(uri);
		switch (uriType) {
			case URIMATCH_BOARDS:
				queryBuilder.setTables(BoardsTable.TABLE_NAME);
				break;
			case URIMATCH_BOARD_ID:
				queryBuilder.setTables(BoardsTable.TABLE_NAME);
				// Adding the ID to the original query
				queryBuilder.appendWhere(BoardsTable.COL_TRELLO_ID + "= '" + uri.getLastPathSegment() + "'");
				break;
			case URIMATCH_LISTS:
				queryBuilder.setTables(ListsTable.TABLE_NAME);
				break;
			case URIMATCH_LIST_ID:
				queryBuilder.setTables(ListsTable.TABLE_NAME);
				// Adding the ID to the original query
				queryBuilder.appendWhere(ListsTable.COL_TRELLO_ID + "= '" + uri.getLastPathSegment() + "'");
				break;
			case URIMATCH_CARDS:
				queryBuilder.setTables(CardsTable.TABLE_NAME);
				break;
			case URIMATCH_CARD_ID:
				queryBuilder.setTables(CardsTable.TABLE_NAME);
				// Adding the ID to the original query
				queryBuilder.appendWhere(CardsTable.COL_TRELLO_ID + "= '" + uri.getLastPathSegment() + "'");
				break;
			case URIMATCH_LISTENERS:
				queryBuilder.setTables(ListenersTable.TABLE_NAME);
				break;
			case URIMATCH_LISTENER_ID:
				queryBuilder.setTables(ListenersTable.TABLE_NAME);
				// Adding the ID to the original query, should select multiple, Trello_ID not unique on listeners
				queryBuilder.appendWhere(ListenersTable.COL_TRELLO_ID + "= '" + uri.getLastPathSegment() + "'");
				break;
			case URIMATCH_BOARDS_OWNERFINDER:
				queryBuilder.setTables(BoardsOwnerFinder.TABLE_NAME);
				break;
			case URIMATCH_BOARDS_OWNERFINDER_ID:
				queryBuilder.setTables(BoardsOwnerFinder.TABLE_NAME);
				// Adding the ID to the original query
				queryBuilder.appendWhere(BoardsOwnerFinder.COL_ID + "= '" + uri.getLastPathSegment() + "'");
				break;
			default:
			  throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		
		SQLiteDatabase db = database.getWritableDatabase();
		Cursor cursor = queryBuilder.query(db, projection, selection,
		    selectionArgs, null, null, sortOrder);
		
		
		// Make sure that potential listeners are getting notified
		cursor.setNotificationUri(getContext().getContentResolver(), uri);
		
		return cursor;
	}
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int uriType = sURIMatcher.match(uri);
	    SQLiteDatabase sqlDB = database.getWritableDatabase();
	    int rowsDeleted = 0;
	    String id;
	    switch (uriType) {
		    case URIMATCH_BOARDS:
		    	rowsDeleted = sqlDB.delete(BoardsTable.TABLE_NAME, selection,
		    			selectionArgs);
		    	break;
		    case URIMATCH_BOARD_ID:
			    id = uri.getLastPathSegment();
			    if (TextUtils.isEmpty(selection)) {
			    	rowsDeleted = sqlDB.delete(BoardsTable.TABLE_NAME,
			    			BoardsTable.COL_TRELLO_ID + "=" + id, null);
			    } else {
			        rowsDeleted = sqlDB.delete(BoardsTable.TABLE_NAME,
			        		BoardsTable.COL_TRELLO_ID + "=" + id + " and " + selection, selectionArgs);
			      }
			      break;
		    case URIMATCH_LISTS:
			    rowsDeleted = sqlDB.delete(ListsTable.TABLE_NAME, selection,selectionArgs);
			    break;
		    case URIMATCH_LIST_ID:
			    id = uri.getLastPathSegment();
			    if (TextUtils.isEmpty(selection)) {
			    	rowsDeleted = sqlDB.delete(ListsTable.TABLE_NAME,
			    			ListsTable.COL_TRELLO_ID + "=" + id, null);
			    } else {
			    	rowsDeleted = sqlDB.delete(ListsTable.TABLE_NAME,
			    			ListsTable.COL_TRELLO_ID + "=" + id + " and " + selection, selectionArgs);
			    }
		    break;
		    case URIMATCH_CARDS:
			    rowsDeleted = sqlDB.delete(CardsTable.TABLE_NAME, selection,selectionArgs);
			    break;
		    case URIMATCH_CARD_ID:
			    id = uri.getLastPathSegment();
			    if (TextUtils.isEmpty(selection)) {
			    	rowsDeleted = sqlDB.delete(CardsTable.TABLE_NAME,
			    			CardsTable.COL_TRELLO_ID + "=" + id, null);
			    } else {
			    	rowsDeleted = sqlDB.delete(CardsTable.TABLE_NAME,
			    			CardsTable.COL_TRELLO_ID + "=" + id + " and " + selection, selectionArgs);
			    }
		    case URIMATCH_LISTENERS:
			    rowsDeleted = sqlDB.delete(CardsTable.TABLE_NAME, selection,selectionArgs);
			    break;
		    case URIMATCH_LISTENER_ID:
			    id = uri.getLastPathSegment();
			    if (TextUtils.isEmpty(selection)) {
			    	rowsDeleted = sqlDB.delete(ListenersTable.TABLE_NAME,
			    			ListenersTable.COL_TRELLO_ID + "=" + id, null);
			    } else {
			    	rowsDeleted = sqlDB.delete(ListenersTable.TABLE_NAME,
			    			ListenersTable.COL_TRELLO_ID + "=" + id + " and " + selection, selectionArgs);
			    }
			    break;
		    case URIMATCH_BOARDS_OWNERFINDER:
		    	rowsDeleted = sqlDB.delete(BoardsOwnerFinder.TABLE_NAME, selection,
		    			selectionArgs);
		    	break;
		    case URIMATCH_BOARDS_OWNERFINDER_ID:
			    id = uri.getLastPathSegment();
			    if (TextUtils.isEmpty(selection)) {
			    	rowsDeleted = sqlDB.delete(BoardsOwnerFinder.TABLE_NAME,
			    			BoardsOwnerFinder.COL_ID + "=" + id, null);
			    } else {
			        rowsDeleted = sqlDB.delete(BoardsOwnerFinder.TABLE_NAME,
			        		BoardsOwnerFinder.COL_ID + "=" + id + " and " + selection, selectionArgs);
			      }
			      break;
		    default:
		      throw new IllegalArgumentException("Unknown URI: " + uri);
	    }
	    getContext().getContentResolver().notifyChange(uri, null);
	    return rowsDeleted;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		int uriType = sURIMatcher.match(uri);
	    SQLiteDatabase sqlDB = database.getWritableDatabase();
	    long id = 0;
	    String tableName = "";
	    switch (uriType) {
		    case URIMATCH_BOARDS:
		    	id = sqlDB.insert(BoardsTable.TABLE_NAME, null, values);
		    	tableName = BOARDS_PATH;
		    	break;
		    case URIMATCH_LISTS:
		    	id = sqlDB.insert(ListsTable.TABLE_NAME, null, values);
		    	tableName = LISTS_PATH;
		    	break;
		    case URIMATCH_CARDS:
		    	id = sqlDB.insert(CardsTable.TABLE_NAME, null, values);
		    	tableName = CARDS_PATH;
		    	break;
		    case URIMATCH_LISTENERS:
		    	id = sqlDB.insert(ListenersTable.TABLE_NAME, null, values);
		    	tableName = LISTENERS_PATH;
		    	break;
		    case URIMATCH_BOARDS_OWNERFINDER:
		    	id = sqlDB.insert(BoardsOwnerFinder.TABLE_NAME, null, values);
		    	tableName = BOARDS_OWNERFINDER_PATH;
		    	break;
		    default:
		      throw new IllegalArgumentException("Unknown URI: " + uri);
	    }
	    getContext().getContentResolver().notifyChange(uri, null);
	    return Uri.parse(tableName + "/" + id);
	}
	

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		int uriType = sURIMatcher.match(uri);
	    SQLiteDatabase sqlDB = database.getWritableDatabase();
	    int rowsUpdated = 0;
	    String id;
	    
	    switch (uriType) {
	    	case URIMATCH_BOARDS:
	    		rowsUpdated = sqlDB.update(BoardsTable.TABLE_NAME, values, selection, selectionArgs);
	    		break;
		    case URIMATCH_BOARD_ID:
		    	id = uri.getLastPathSegment();
		    	if (TextUtils.isEmpty(selection)) {
		    		rowsUpdated = sqlDB.update(BoardsTable.TABLE_NAME, values, BoardsTable.COL_TRELLO_ID + "=" + id, null);
		    	} else {
		    		rowsUpdated = sqlDB.update(BoardsTable.TABLE_NAME, values, BoardsTable.COL_TRELLO_ID + "=" + id 
		    				+ " and " + selection, selectionArgs);
		    	}
		    	break;
		    case URIMATCH_LISTS:
	    		rowsUpdated = sqlDB.update(ListsTable.TABLE_NAME, values, selection, selectionArgs);
	    		break;
		    case URIMATCH_LIST_ID:
		    	id = uri.getLastPathSegment();
		    	if (TextUtils.isEmpty(selection)) {
		    		rowsUpdated = sqlDB.update(ListsTable.TABLE_NAME, values, ListsTable.COL_TRELLO_ID + "=" + id, null);
		    	} else {
		    		rowsUpdated = sqlDB.update(ListsTable.TABLE_NAME, values, ListsTable.COL_TRELLO_ID + "=" + id 
		    				+ " and " + selection, selectionArgs);
		    	}
		    	break;
		    case URIMATCH_CARDS:
	    		rowsUpdated = sqlDB.update(CardsTable.TABLE_NAME, values, selection, selectionArgs);
	    		break;
		    case URIMATCH_CARD_ID:
		    	id = uri.getLastPathSegment();
		    	if (TextUtils.isEmpty(selection)) {
		    		rowsUpdated = sqlDB.update(CardsTable.TABLE_NAME, values, CardsTable.COL_TRELLO_ID + "= '" + id + "'", null);
		    	} else {
		    		rowsUpdated = sqlDB.update(CardsTable.TABLE_NAME, values, CardsTable.COL_TRELLO_ID + "= '" + id + "'" 
		    				+ " and " + selection, selectionArgs);
		    	}
		    	break;
		    case URIMATCH_LISTENERS:
	    		rowsUpdated = sqlDB.update(ListenersTable.TABLE_NAME, values, selection, selectionArgs);
	    		break;
		    case URIMATCH_LISTENER_ID:
		    	id = uri.getLastPathSegment();
		    	if (TextUtils.isEmpty(selection)) {
		    		rowsUpdated = sqlDB.update(ListenersTable.TABLE_NAME, values, ListenersTable.COL_TRELLO_ID + "=" + id, null);
		    	} else {
		    		rowsUpdated = sqlDB.update(ListenersTable.TABLE_NAME, values, ListenersTable.COL_TRELLO_ID + "=" + id 
		    				+ " and " + selection, selectionArgs);
		    	}
		    	break;
		    case URIMATCH_BOARDS_OWNERFINDER:
	    		rowsUpdated = sqlDB.update(BoardsOwnerFinder.TABLE_NAME, values, selection, selectionArgs);
	    		break;
		    case URIMATCH_BOARDS_OWNERFINDER_ID:
		    	id = uri.getLastPathSegment();
		    	if (TextUtils.isEmpty(selection)) {
		    		rowsUpdated = sqlDB.update(BoardsOwnerFinder.TABLE_NAME, values, BoardsOwnerFinder.COL_ID + "=" + id, null);
		    	} else {
		    		rowsUpdated = sqlDB.update(BoardsOwnerFinder.TABLE_NAME, values, BoardsOwnerFinder.COL_ID + "=" + id 
		    				+ " and " + selection, selectionArgs);
		    	}
		    	break;
		    default:
		    	throw new IllegalArgumentException("Unknown URI: " + uri);
	    }
	    getContext().getContentResolver().notifyChange(uri, null);
	    return rowsUpdated;
	}
	
	private void checkColumns(String[] projection) {
		String[] available = { BoardsTable.COL_TRELLO_ID, BoardsTable.COL_DATE, BoardsTable.COL_SYNCED,
				ListsTable.COL_TRELLO_ID, ListsTable.COL_DATE, ListsTable.COL_SYNCED,
				CardsTable.COL_TRELLO_ID, CardsTable.COL_DATE, CardsTable.COL_SYNCED};
		if (projection != null) {
			HashSet<String> requestedColumns = new HashSet<String>(Arrays.asList(projection));
			HashSet<String> availableColumns = new HashSet<String>(Arrays.asList(available));
			// Check if all columns which are requested are available
			if (!availableColumns.containsAll(requestedColumns)) {
				throw new IllegalArgumentException("Unknown columns in projection");
			}
		}
	}

}
