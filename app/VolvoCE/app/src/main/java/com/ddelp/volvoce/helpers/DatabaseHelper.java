package com.ddelp.volvoce.helpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.ddelp.volvoce.objects.Worker;
import com.ddelp.volvoce.objects.Worksite;

import java.util.ArrayList;

/**
 * Helper to store and retrieve worksite and worker information in a
 * local SQLite database. Uses singleton instance to prevent memory leaks.
 *
 * @author  Denny Delp
 * @version 1.0
 * @since   2016-05-5
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    // Make database instance a singleton instance to prevent memory leaks
    private static DatabaseHelper sInstance;

    // Tag for logging
    private static final String TAG = "DatabaseHelper";

    // Database Info
    private static final String DATABASE_NAME = "VolvoCEDatabase";
    private static final int DATABASE_VERSION = 1;

    // Table Names
    private static final String TABLE_WORKSITE_LIST = "worksiteList";
    private static final String TABLE_WORKSITES = "worksites";
    private static final String TABLE_WORKERS = "workers";

    // WorksiteList Table Columns
    private static final String KEY_WORKSITE_LIST_ID = "id";
    private static final String KEY_WORKSITE_LIST_NAME = "worksiteName";

    // Worksite Table Columns
    private static final String KEY_WORKSITE_ID = "id";
    private static final String KEY_WORKSITE_NAME = "worksiteName";
    private static final String KEY_WORKSITE_TOP_LEFT_GPS = "worksiteTopLeftGPS";
    private static final String KEY_WORKSITE_BOTTOM_RIGHT_GPS = "worksiteBottomRightGPS";

    // Worker Table Columns
    private static final String KEY_WORKER_ID = "id";
    private static final String KEY_WORKER_NAME = "workerId";
    private static final String KEY_WORKER_WORKSITE_NAME = "workerWorksiteName";
    private static final String KEY_WORKER_GPS = "workerGPS";

    // Call getInstance rather than regular constructor.. prevents memory leaks
    public static synchronized DatabaseHelper getInstance(Context context) {
        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (sInstance == null) {
            sInstance = new DatabaseHelper(context.getApplicationContext());
        }
        return sInstance;
    }

    // Constructor should be private to prevent direct instantiation.
    // Make a call to the static method "getInstance()" instead.
    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Called when the database connection is being configured.
    // Configure database settings for things like foreign key support, write-ahead logging, etc.
    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    // Called when the database is created for the FIRST time.
    // If a database already exists on disk with the same DATABASE_NAME, this method will NOT be called.
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_WORKSITE_LIST_TABLE = "CREATE TABLE " + TABLE_WORKSITE_LIST +
                "(" +
                KEY_WORKSITE_LIST_ID + " INTEGER PRIMARY KEY," +
                KEY_WORKSITE_LIST_NAME + " TEXT" +
                ")";

        String CREATE_WORKSITE_TABLE = "CREATE TABLE " + TABLE_WORKSITES +
                "(" +
                KEY_WORKSITE_ID + " INTEGER PRIMARY KEY," +
                KEY_WORKSITE_NAME + " TEXT," +
                KEY_WORKSITE_TOP_LEFT_GPS + " TEXT," +
                KEY_WORKSITE_BOTTOM_RIGHT_GPS + " TEXT" +
                ")";

        String CREATE_WORKERS_TABLE = "CREATE TABLE " + TABLE_WORKERS +
                "(" +
                KEY_WORKER_ID + " INTEGER PRIMARY KEY," +
                KEY_WORKER_NAME + " TEXT," +
                KEY_WORKER_WORKSITE_NAME + " TEXT," +
                KEY_WORKER_GPS + " TEXT" +
                ")";

        db.execSQL(CREATE_WORKSITE_LIST_TABLE);
        db.execSQL(CREATE_WORKSITE_TABLE);
        db.execSQL(CREATE_WORKERS_TABLE);
    }

    // Called when the database needs to be upgraded.
    // This method will only be called if a database already exists on disk with the same DATABASE_NAME,
    // but the DATABASE_VERSION is different than the version of the database that exists on disk.
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion != newVersion) {
            Log.w("DBAdapter", "Upgrading from version " + oldVersion + " to "
                    + newVersion + ", destroy old data and recreate.");
            // Simplest implementation is to drop all old table and recreate
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_WORKSITE_LIST);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_WORKSITES);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_WORKERS);
            onCreate(db);
        }
    }

    //---------------------------- GETTERS/SETTERS FOR ALL TABLES -----------------------------//

    /**
     * Add a worksite to the worksite list
     * @param worksiteName Worksite name
     */
    public void addWorksiteToList(String worksiteName) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(KEY_WORKSITE_LIST_NAME, worksiteName);
            int rows = db.update(TABLE_WORKSITE_LIST, values, KEY_WORKSITE_LIST_NAME + "= ?",
                    new String[]{worksiteName});
            if (rows == 1) {
                //Log.d(TAG, "Worksite successfully updated in list: " + worksiteName);
            } else {
                // Worksite with this name did not already exist, so insert new worksite
                //Log.d(TAG, "Creating new worksite in list: " + worksiteName);
                db.insertOrThrow(TABLE_WORKSITE_LIST, null, values);
                db.setTransactionSuccessful();
            }
        } catch (Exception e) {
            Log.d(TAG, "addWorksiteToList Error: " + worksiteName);
        } finally {
            db.endTransaction();
        }
    }

    /**
     * getWorksiteList
     * @return ArrayList of worksite names
     */
    public ArrayList<String> getWorksiteList() {
        ArrayList<String> worksiteList = new ArrayList<>();
        String WORKSITES_SELECT_QUERY = "SELECT * FROM " + TABLE_WORKSITE_LIST;
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(WORKSITES_SELECT_QUERY, null);
        try {
            if (cursor.moveToFirst()) {
                do {
                    String worksiteName = cursor.getString(cursor.getColumnIndex(KEY_WORKSITE_LIST_NAME));
                    worksiteList.add(worksiteName);
                } while(cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.d(TAG, "Error while trying to get worksites from database");
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        return worksiteList;
    }

    /**
     * Insert or update a worker in the database
     * Since SQLite doesn't support "upsert" we need to fall back on an attempt to UPDATE (in case the
     * worker already exists) optionally followed by an INSERT (in case the user does not already exist).
     * Unfortunately, there is a bug with the insertOnConflict method
     * (https://code.google.com/p/android/issues/detail?id=13045) so we need to fall back to the more
     * verbose option of querying for the user's primary key if we did an update.
     *
     * @param worksite Worksite to add or update
     */
    public void addOrUpdateWorksite(Worksite worksite) {
        // The database connection is cached so it's not expensive to call getWriteableDatabase()
        // multiple times.
        SQLiteDatabase db = getWritableDatabase();
        // It's a good idea to wrap our insert in a transaction. This helps with performance and ensures
        // consistency of the database.
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(KEY_WORKSITE_NAME, worksite.name);
            values.put(KEY_WORKSITE_TOP_LEFT_GPS, worksite.getTopLeft());
            values.put(KEY_WORKSITE_BOTTOM_RIGHT_GPS, worksite.getBottomRight());
            // First try to update the worksite in case the it already exists in the database
            // This assumes worksite names are unique
            int rows = db.update(TABLE_WORKSITES, values, KEY_WORKER_NAME + "= ?", new String[]{worksite.name});

            // Check if update succeeded
            if (rows == 1) {
                //Log.d(TAG, "Worksite successfully updated: " + worksite.name);
            } else {
                // Worksite with this name did not already exist, so insert new worksite
                //Log.d(TAG, "Creating new worksite: " + worksite.name);
                db.insertOrThrow(TABLE_WORKSITES, null, values);
                db.setTransactionSuccessful();
            }

        } catch (Exception e) {
            Log.d(TAG, "addOrUpdateWorksite Error: " + worksite.getName());
        } finally {
            db.endTransaction();
        }
    }

    /**
     * containsWorksite
     * @param worksite Worksite object to check
     */
    public boolean containsWorksite(Worksite worksite) {
        return containsWorksite(worksite.getName());
    }

    /**
     * containsWorksite
     * @param worksiteName Name of Worksite to check
     */
    public boolean containsWorksite(String worksiteName) {
        String WORKSITE_SELECT_QUERY = "SELECT * FROM " + TABLE_WORKSITES +
                " WHERE " + KEY_WORKSITE_NAME + " =?";
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(WORKSITE_SELECT_QUERY, new String[] {worksiteName});
        try {
            return cursor.getCount() >= 1;
        } catch (Exception e) {
            Log.d(TAG, "containsWorksite Error: " + worksiteName);
            return false;
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }

    /**
     * getWorksite
     * @return Worksite
     */
    public Worksite getWorksite(String worksiteName) {
        String WORKSITE_SELECT_QUERY = KEY_WORKSITE_NAME + "=?";
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_WORKSITES, null, WORKSITE_SELECT_QUERY,
                new String[] { worksiteName }, null, null, null);
        try {
            if (cursor.getCount() < 1) {
                cursor.close();
                return null;
            }
            String topLeft = cursor.getString(cursor.getColumnIndex(KEY_WORKSITE_TOP_LEFT_GPS));
            String bottomRight = cursor.getString(cursor.getColumnIndex(KEY_WORKSITE_BOTTOM_RIGHT_GPS));
            Worksite worksite = new Worksite(worksiteName, topLeft, bottomRight);
            ArrayList<Worker> workers = getWorkers(worksiteName);
            worksite.addWorkers(workers);
            return worksite;
        } catch (Exception e) {
            Log.d(TAG, "getWorksite Error: " + worksiteName);
            return null;
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }

    /**
     * getWorksites
     * @return ArrayList of worksites
     */
    public ArrayList<Worksite> getWorksites() {
        ArrayList<Worksite> worksites = new ArrayList<>();
        String WORKSITES_SELECT_QUERY = "SELECT * FROM " + TABLE_WORKSITES;
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(WORKSITES_SELECT_QUERY, null);
        try {
            if (cursor.moveToFirst()) {
                do {
                    String worksiteName = cursor.getString(cursor.getColumnIndex(KEY_WORKSITE_NAME));
                    String topLeft = cursor.getString(cursor.getColumnIndex(KEY_WORKSITE_TOP_LEFT_GPS));
                    String bottomRight = cursor.getString(cursor.getColumnIndex(KEY_WORKSITE_BOTTOM_RIGHT_GPS));
                    Worksite worksite = new Worksite(worksiteName, topLeft, bottomRight);
                    ArrayList<Worker> workers = getWorkers(worksiteName);
                    worksite.addWorkers(workers);
                    worksites.add(worksite);
                } while(cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.d(TAG, "Error while trying to get worksites from database");
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        return worksites;
    }

    /**
     *
     * @param worker Worker to add or update
     */
    public void addOrUpdateWorker(Worker worker) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(KEY_WORKER_NAME, worker.getID());
            values.put(KEY_WORKER_WORKSITE_NAME, worker.getWorksite());
            values.put(KEY_WORKER_GPS, worker.getGPS());
            int rows = db.update(TABLE_WORKERS, values, KEY_WORKER_NAME + "= ?", new String[]{worker.getID()});
            if (rows == 1) {
                Log.d(TAG, "Worker successfully updated: " + worker.getID());
            } else {
                Log.d(TAG, "Creating new worker: " + worker.getID());
                db.insertOrThrow(TABLE_WORKERS, null, values);
                db.setTransactionSuccessful();
            }
        } catch (Exception e) {
            Log.d(TAG, "addOrUpdateWorker Error: " + worker.getID());
        } finally {
            db.endTransaction();
        }
    }

    /**
     * getWorker
     * @param id The workers id
     * @return the Worker
     */
    public Worker getWorker(String id) {
        String WORKER_SELECT_QUERY = KEY_WORKER_NAME + "=?";
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_WORKERS, null, WORKER_SELECT_QUERY,
                new String[] { id }, null, null, null);
        try {
            if (cursor.getCount() < 1) {
                cursor.close();
                return null;
            }
            String gps = cursor.getString(cursor.getColumnIndex(KEY_WORKER_GPS));
            return new Worker(id, gps);
        } catch (Exception e) {
            Log.d(TAG, "Error trying to get worker from database: " + id);
            return null;
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }

    /**
     * getWorkers
     * @return ArrayList of Workers in database
     */
    public ArrayList<Worker> getWorkers() {
        String SELECT_QUERY = "SELECT * FROM " + TABLE_WORKERS;
        return getWorkerQuery(SELECT_QUERY);
    }

    /**
     * getWorkers at a specified worksite
     * @param worksiteName Name of worksite
     * @return ArrayList of Workers in database at specified worksite
     */
    public ArrayList<Worker> getWorkers(String worksiteName) {
        String SELECT_QUERY = "SELECT * FROM " + TABLE_WORKERS +
                " WHERE " + KEY_WORKSITE_NAME + "=" + worksiteName;
        return getWorkerQuery(SELECT_QUERY);
    }

    /**
     * getWorkerQuery
     * @param query Structured database query
     * @return ArrayList of Workers
     */
    private ArrayList<Worker> getWorkerQuery (String query) {
        ArrayList<Worker> workers = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        try {
            if (cursor.moveToFirst()) {
                do {
                    String id = cursor.getString(cursor.getColumnIndex(KEY_WORKER_NAME));
                    String gps = cursor.getString(cursor.getColumnIndex(KEY_WORKER_GPS));
                    Worker worker = new Worker(id, gps);
                    workers.add(worker);
                } while(cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.d(TAG, "getWorkerQuery Error" + query);
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        return workers;
    }

    //--------------------------- ABSTRACTED HELPERS FOR ALL TABLES ---------------------------//
    //                               delete and isEmpty methods

    /**
     * deleteWorksite
     * @param worksite Worksite to delete
     * @return Number of table entries deleted
     */
    public int deleteWorksite(Worksite worksite) {
        return deleteWorksite(worksite.getName());
    }

    /**
     * deleteWorksite (removes worksite from both list and info)
     * @param name Name of Worksite to delete
     * @return Number of table entries deleted
     */
    public int deleteWorksite(String name) {
        Log.d(TAG, "deleteWorksiteFromList: " + name);
        String LIST_SELECT_QUERY = KEY_WORKSITE_LIST_NAME + "=?";
        String WORKSITE_SELECT_QUERY = KEY_WORKSITE_LIST_NAME + "=?";
        deleteTableEntry(TABLE_WORKSITE_LIST, LIST_SELECT_QUERY, name);
        return deleteTableEntry(TABLE_WORKERS, WORKSITE_SELECT_QUERY, name);
    }

    /**
     * deleteWorker
     * @param id ID of Worker to delete
     * @return Number of table entries deleted
     */
    public int deleteWorker(String id) {
        String WORKER_SELECT_QUERY = KEY_WORKER_NAME + "=?";
        return deleteTableEntry(TABLE_WORKERS, WORKER_SELECT_QUERY, id);
    }

    /**
     *
     * @param table The table to delete entry from
     * @param query The SQL query (Key + "=?")
     * @param id The id of the entry to delete
     * @return Number of table entries deleted
     */
    private int deleteTableEntry(String table ,String query, String id) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            return db.delete(table, query, new String[] { id });
        } catch (Exception e) {
            Log.d(TAG, "deleteTableEntry Error: " + table + ", " + id);
            return 0;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * deleteAllTables
     */
    public void deleteAllTables() {
        Log.d(TAG, "Delete all tables in database");
        deleteTable(TABLE_WORKSITE_LIST);
        deleteTable(TABLE_WORKSITES);
        deleteTable(TABLE_WORKERS);
    }

    /**
     * deleteWorksiteList
     */
    public void deleteWorksiteList() {
        Log.d(TAG, "Delete all worksites");
        deleteTable(TABLE_WORKSITE_LIST);
    }

    /**
     * deleteAllWorksites
     */
    public void deleteAllWorksites() {
        Log.d(TAG, "Delete all worksites");
        deleteTable(TABLE_WORKSITES);
    }

    /**
     * deleteAllWorkers
     */
    public void deleteAllWorkers() {
        Log.d(TAG, "Delete all workers");
        deleteTable(TABLE_WORKERS);
    }

    /**
     * Delete a table from the database
     * @param table table to delete
     */
    private void deleteTable(String table) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            // Order of deletions is important when foreign key relationships exist. (none for now)
            db.delete(table, null, null);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.d(TAG, "deleteTable Error:" + table);
        } finally {
            db.endTransaction();
        }
    }

    /**
     * @return flag if worksite list table is empty
     */
    public boolean isWorksiteListTableEmpty() {
        return isTableEmpty(TABLE_WORKSITE_LIST);
    }

    /**
     * @return flag if worksite table is empty
     */
    public boolean isWorksiteTableEmpty() {
        return isTableEmpty(TABLE_WORKSITES);
    }

    /**
     * @return flag if worker table is empty
     */
    public boolean isWorkerTableEmpty() {
        return isTableEmpty(TABLE_WORKERS);
    }

    /**
     * isTableEmpty
     * @param table name of table to check
     * @return if table is empty
     */
    private boolean isTableEmpty(String table) {
        String SELECT_QUERY = "SELECT * FROM " + table;
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(SELECT_QUERY, null);
        try {
            if (cursor.moveToFirst()) {
                Log.i(TAG, "Table not empty: " + table);
                return false;
            } else {
                Log.i(TAG, "Table empty: " + table);
                return true;
            }
        } catch (Exception e) {
            Log.d(TAG, "isTableEmpty Error: " + table);
            return true; // Something went wrong, indicate db is empty
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }
}