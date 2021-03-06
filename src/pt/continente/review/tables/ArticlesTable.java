package pt.continente.review.tables;

import java.util.ArrayList;
import java.util.List;

import pt.continente.review.common.Common;
import pt.continente.review.common.Article;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

public class ArticlesTable {

	// Debugging tag
	private static final String TAG = "CntRev - ArticlesTable";

	/**
	 * Defines the internal exceptions that can be thrown by the class
	 */
	public static final class exceptions {
		public static final String DB_HELPER_ERROR = "Error opening DB helper";
		public static final String WRITABLE_DB_ERROR = "Error capturing a writable DB";
	}

	public static final String TABLE_NAME = "Articles";
	public static final String COLUMN_ARTICLE_ID = "article_id";
	public static final String COLUMN_ARTICLE_NAME = "article_name";
	public static final String COLUMN_ARTICLE_DESCRIPTION = "article_description";
	public static final String COLUMN_ARTICLE_EAN = "article_ean";
	public static final String COLUMN_ARTICLE_PRICE = "article_price";
	public static final String COLUMN_ARTICLE_IMAGE_URL = "article_image_URL";
	public static final String COLUMN_ARTICLE_IMAGE = "article_image";
	public static final String COLUMN_ARTICLE_STRUCTURE_L1 = "article_structure_l1";
	public static final String COLUMN_ARTICLE_STRUCTURE_L2 = "article_structure_l2";
	public static final String COLUMN_ARTICLE_STRUCTURE_L3 = "article_structure_l3";
	public static final String COLUMN_ARTICLE_STRUCTURE_L4 = "article_structure_l4";
	public static final int COLUMN_COUNT = 11;
	
	// Database fields
	private SQLiteDatabase database;
	private boolean usingExternalDB = false;
	private SQLiteHelper dbHelper;
	private String[] allColumns = {
			COLUMN_ARTICLE_ID,
			COLUMN_ARTICLE_NAME,
			COLUMN_ARTICLE_DESCRIPTION,
			COLUMN_ARTICLE_EAN,
			COLUMN_ARTICLE_PRICE,
			COLUMN_ARTICLE_IMAGE_URL,
			COLUMN_ARTICLE_IMAGE,
			COLUMN_ARTICLE_STRUCTURE_L1,
			COLUMN_ARTICLE_STRUCTURE_L2,
			COLUMN_ARTICLE_STRUCTURE_L3,
			COLUMN_ARTICLE_STRUCTURE_L4
			};
	
	
	
	public ArticlesTable(SQLiteHelper helper) throws Exception {
		this(helper, null);
	}
		
	public ArticlesTable(SQLiteHelper helper, SQLiteDatabase originDB) throws Exception {
		try {
			dbHelper = helper;
			database = originDB;
			if(originDB != null)
				usingExternalDB = true;
		} catch (SQLException e) {
			Log.i(TAG, "ArticlesTable: error opening the DB helper - " + e.getMessage());
			throw new Exception(exceptions.DB_HELPER_ERROR);
		}
	}
	
	
	
	public void open() throws Exception {
		if(!usingExternalDB) {
			try {
				database = dbHelper.getWritableDatabase();
			} catch (SQLiteException e) {
				Log.i(TAG, "open: error getting writable database - " + e.getMessage());
				throw new Exception(exceptions.WRITABLE_DB_ERROR);
			}
		}
	}	
	
	public void close() {
		if(!usingExternalDB) {
			database.close();
		}
	}
	
	
	
	
	public Article getItem(long itemId) {

		Article newItem = null;
		
	    Cursor cursor = database.query(TABLE_NAME, allColumns, COLUMN_ARTICLE_ID + "=" + itemId, null, null, null, null);
	    
	    int cursorRows = cursor.getCount();
	    if (cursorRows <= 0) {
	    	Common.log(1, TAG, "getItem: no line found with Id " + itemId);
	    	return null;
	    } else if (cursorRows > 1) {
	    	Common.log(1, TAG, "getItem: Id " + itemId + " returned " + cursorRows + " rows");
	    	return null;
	    } else {
	    	cursor.moveToNext();
	    	newItem = cursorToObject(cursor);
	    	if (newItem == null) {
	    		Log.i(TAG, "getItem: couldn't create Object");
	    		return null;
	    	}
	    }
	    cursor.close();
	    return newItem;
	}

	
	public List<Article> getAllItems() {

		List<Article> item = new ArrayList<Article>();
	
	    Cursor cursor = database.query(TABLE_NAME, allColumns, null, null, null, null, null);
	    
    	cursor.moveToNext();
    	while (!cursor.isAfterLast()) {
    		Article newItem = cursorToObject(cursor);
	    	if (newItem == null) {
	    		Common.log(1, TAG, "getAllItems: couldn't create Object for itemId " + cursor.getLong(cursor.getColumnIndex(COLUMN_ARTICLE_ID)));
	    	} else {
	    		item.add(newItem);
	    	}
	    	cursor.moveToNext();
    	}

	    cursor.close();
	    
	    return item;
	}

	
	/**
	 * @param device the KNXDevice to be added to the table
	 * @return
	 * the <b>device id</b> generated by the table (should be added to the supplied Object)<br>
	 * <b>-1</b> if the supplied object does not contain proper data<br>
	 * <b>-2</b> if the supplied object already exists<br>
	 * <b>-3</b> if there was a general error adding to the table
	 */
	public long addItem(Article item) {
		Common.log(5, TAG, "addItem: entrou");

		if (!item.isFullyDefined()) {
			Common.log(1, TAG, "addItem: the supplied item is not fully defined");
			return -1;
		}
		
		Common.log(5, TAG, "addItem: vai verificar se o item j� existe na tabela");
		if (findItem(item.getEAN()) != -1) {
			Common.log(1, TAG, "addItem: an item for same content already exists");
			return -2;
		}
		
	    ContentValues values = new ContentValues();
	    
	    values.put(COLUMN_ARTICLE_ID, item.getId());
	    values.put(COLUMN_ARTICLE_NAME, item.getName());
	    values.put(COLUMN_ARTICLE_DESCRIPTION, item.getDescription());
	    values.put(COLUMN_ARTICLE_EAN, item.getEAN());
	    values.put(COLUMN_ARTICLE_PRICE, item.getPrice());
	    values.put(COLUMN_ARTICLE_IMAGE_URL, item.getImageURL());
	    values.put(COLUMN_ARTICLE_IMAGE, Common.imageToBlob(item.getImage()));
	    values.put(COLUMN_ARTICLE_STRUCTURE_L1, item.getStructureL1());
	    values.put(COLUMN_ARTICLE_STRUCTURE_L2, item.getStructureL2());
	    values.put(COLUMN_ARTICLE_STRUCTURE_L3, item.getStructureL3());
	    values.put(COLUMN_ARTICLE_STRUCTURE_L4, item.getStructureL4());
		
		Common.log(5, TAG, "addItem: vai tentar carregar registo na db");
	    long deviceId = database.insert(TABLE_NAME, null, values);
	    if(deviceId == -1) {
	    	Log.i(TAG, "addItem: couldn't insert new Device into table");
			return -3;
	    }
	    
		Common.log(5, TAG, "addItem: vai sair");
	    return deviceId;
	}
	

	public boolean updateItem(Article updatedItem) {
		
		if (!updatedItem.isFullyDefined()) {
			Log.i(TAG, "updateAction: the supplied Object is not defined as expected");
			return false;
		}
		
		long itemIdToUpdate = updatedItem.getId();
		
		if (findItem(updatedItem.getEAN()) != itemIdToUpdate) {
			Log.i(TAG, "updateDevice: the update would generate duplicate devices");
			return false;
		}
		
	    ContentValues values = new ContentValues();
	    
	    values.put(COLUMN_ARTICLE_NAME, updatedItem.getName());
	    values.put(COLUMN_ARTICLE_DESCRIPTION, updatedItem.getDescription());
	    values.put(COLUMN_ARTICLE_EAN, updatedItem.getEAN());
	    values.put(COLUMN_ARTICLE_PRICE, updatedItem.getPrice());
	    values.put(COLUMN_ARTICLE_IMAGE_URL, updatedItem.getImageURL());
	    values.put(COLUMN_ARTICLE_IMAGE, Common.imageToBlob(updatedItem.getImage()));
	    values.put(COLUMN_ARTICLE_STRUCTURE_L1, updatedItem.getStructureL1());
	    values.put(COLUMN_ARTICLE_STRUCTURE_L2, updatedItem.getStructureL2());
	    values.put(COLUMN_ARTICLE_STRUCTURE_L3, updatedItem.getStructureL3());
	    values.put(COLUMN_ARTICLE_STRUCTURE_L4, updatedItem.getStructureL4());
	    
	    int recordsAffected = database.update(TABLE_NAME, values, COLUMN_ARTICLE_ID + "=" + itemIdToUpdate, null);
	    if(recordsAffected <= 0) {
	    	Common.log(1, TAG, "updateDevice: couldn't update table for Object with Id " + itemIdToUpdate);
			return false;
	    } else if(recordsAffected > 1) {
	    	Common.log(3, TAG, "updateDevice: more than one line have been changed by Object with Id " + itemIdToUpdate);
	    }
	    
	    return true;
	}
	
	
	public int deleteItem (Article item) {
		long itemIdToDelete = item.getId();
		int rowsAffected = database.delete(TABLE_NAME, COLUMN_ARTICLE_ID + "=" + itemIdToDelete, null);
		Common.log(5, TAG, "deleteDevice: deleted " + rowsAffected + " rows with deviceId " + itemIdToDelete);
		return rowsAffected;
	}

	
	/**
	 * @return
	 * <i><b>int</b></i> with number of rows affected
	 * <b>-1</b> if failed to read number of rows prior to deleting
	 * <b>-2</b> if no rows where deleted
	 * <b>-3</b> if not all rows where deleted
	 */
	public int deleteAllItems () {
		int rowsAvailable = getNumberOfRows();
		Common.log(5, TAG, "deleteAllItems: rows available = " + rowsAvailable);
		
		if (rowsAvailable < 0) {
			Log.i(TAG, "deleteAllItems: failed to read number of rows");
			return -1;
		}
		int rowsAffected = database.delete(TABLE_NAME, "1", null);

		Common.log(5, TAG, "deleteAllItems: apagou " + rowsAffected + " linhas");
		if (rowsAffected == rowsAvailable) {
			Common.log(5, TAG, "deleteAllItems: deleted " + rowsAffected + " rows (all)");
			return rowsAffected;
		} else if (rowsAffected == 0) {
			Log.i(TAG, "deleteAllItems: could not delete any rows");
			return -2;
		} else {
			Log.i(TAG, "deleteAllItems: not all rows where deleted (only " + rowsAffected + " out of " + rowsAvailable);
			return -3;
		}
	}

	/**
	 * @return
	 * <i><b>int</b></i> with number of rows in table
	 * <b>-1</b> if failed to count rows
	 */
	private int getNumberOfRows() {
		Cursor cursor;
		int numLines;
		
		Common.log(5, TAG, "getNumberOfRows: entrou");
		
		try {
			cursor = database.query(TABLE_NAME, new String[] { COLUMN_ARTICLE_ID }, null, null, null, null, null);
		} catch (Exception e) {
			Log.i(TAG, "getNumberOfRows: error counting rows (1) - " + e.getMessage());
			return -1;
		}
		
		try {
	    	numLines = 0;
			cursor.moveToNext();
	    	while (!cursor.isAfterLast()) {
	    		numLines++;
	    		cursor.moveToNext();
	    	}
		} catch (Exception e) {
			Log.i(TAG, "getNumberOfRows: error counting rows (2) - " + e.getMessage());
			return -1;
		}

		try {
		    cursor.close();
		} catch (Exception e) {
			Log.i(TAG, "getNumberOfRows: error counting rows (3) - " + e.getMessage());
			return -1;
		}
		
		return numLines;
	}
	
	private Article cursorToObject(Cursor cursor) {
		
	    Article newItem = null;
	    
	    if (cursor.getColumnCount() != COLUMN_COUNT) {
		    Common.log(1, TAG, "cursorToObject: column count different from expected: " + cursor.getColumnCount() + " vs. " + COLUMN_COUNT);
	    	return null;
	    }
	    
	    try {
	    	newItem = new Article(
	    			cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ARTICLE_ID)),
	    			cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ARTICLE_NAME)),
	    			cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ARTICLE_DESCRIPTION)),
	    			cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ARTICLE_EAN)),
	    			cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_ARTICLE_PRICE)),
	    			cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ARTICLE_IMAGE_URL)),
	    			Common.blobToImage(cursor.getBlob(cursor.getColumnIndexOrThrow(COLUMN_ARTICLE_IMAGE))),
	    			cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ARTICLE_STRUCTURE_L1)),
	    			cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ARTICLE_STRUCTURE_L2)),
	    			cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ARTICLE_STRUCTURE_L3)),
	    			cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ARTICLE_STRUCTURE_L4))
	    			);
	    } catch (Exception e) {
		    Common.log(1, TAG, "cursorToObject: could not create new Object");
	    	return null;
	    }
		
	    return newItem;
	}

	
	
	
	/**
	 * @return
	 * <b>1</b> if one line matches the input<br>
	 * <b>-1</b> if no lines match the input<br>
	 * <b>-2</b> if more than one line match the input<br>
	 * <b>-3</b> if only one device was found but couldn't create the object in order to find the ID
	 */
	public long findItem(String itemEAN) {
		
		Common.log(5, TAG, "findItem: started");

		Article newItem = null;
	    Cursor cursor = database.query(TABLE_NAME, allColumns, COLUMN_ARTICLE_EAN + "='" + itemEAN + "'", null, null, null, null);
	    
		int cursorRows = cursor.getCount();
		
		Common.log(5, TAG, "findItem: found " + cursorRows + " lines");
	    if (cursorRows <= 0) {
	    	Common.log(5, TAG, "findItem: Item with ArticleEAN '" + itemEAN + "' does not exist");
	    	return -1;
	    } else if (cursorRows > 1) {
	    	Log.i(TAG, "findItem: more than one Item with name " + itemEAN + " exists");
	    	return -2;
	    } else {
	    	cursor.moveToNext();
	    	newItem = cursorToObject(cursor);
	    	if (newItem == null) {
	    		Log.i(TAG, "findItem: couldn't create Object");
	    		return -3;
	    	}
	    }

	    cursor.close();
		return newItem.getId();
	}

}
