package com.jamesma.purdue.maps.database;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import android.app.SearchManager;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.provider.BaseColumns;

/**
 * This class helps open, create, and upgrade the database file.
 * We use our own SQLite database in our application, instead of creating one from scratch.
 * 
 * @see http://www.reigndesign.com/blog/using-your-own-sqlite-database-in-android-applications/
 * @author James Ma (http://jamesma.info)
 * 
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    // Android default system path of our application database
    private static final String DB_PATH = "/data/data/com.jamesma.purdue.maps/databases/";
    private static final String DB_NAME = "purdue_locations.db";
    private static final String TABLE_NAME = "purdue_campus_locations";
    
    private static final String[] COLUMN_NAMES = {
        BaseColumns._ID,
        SearchManager.SUGGEST_COLUMN_TEXT_1,
        SearchManager.SUGGEST_COLUMN_TEXT_2,
        SearchManager.SUGGEST_COLUMN_INTENT_DATA,
        SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA
    };

    public static final String LOC_NAME = "name";
    public static final String ABBR = "abbr";
    public static final String COORDS = "coords";
    public static final String ADDR = "addr";
    public static final String DATA_SEPARATOR = "%";
    
    private SQLiteDatabase sqliteDB;

    private final Context mContext;

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, 1);
        this.mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    @Override
    public synchronized void close() {
        if (sqliteDB != null) {
            sqliteDB.close();
        }
        super.close();
    }

    /**
     * Creates an empty database on the system and rewrites it with your own
     * database.
     * 
     * @throws IOException
     */
    public void createDatabase() throws IOException {
        boolean dbExists = checkDatabase();

        if (dbExists) {
            // Do nothing - database already exists
        } else {
            // By calling this method an empty database will be created into the
            // default system path of our application so we are going to be able
            // to overwrite the database with our database.
            this.getReadableDatabase();

            try {
                copyDatabase();
            } catch (IOException e) {
                throw new Error("Error copying database");
            }
        }
    }

    /**
     * Check if the database already exists to avoid re-copying the file each
     * time the user opens the application.
     * 
     * @return true if it exists, false if not
     */
    private boolean checkDatabase() {
        SQLiteDatabase checkDB = null;

        try {
            String dbPath = DB_PATH + DB_NAME;
            checkDB = SQLiteDatabase.openDatabase(
                    dbPath, 
                    null,
                    SQLiteDatabase.NO_LOCALIZED_COLLATORS   // Open without support for localized collators. setLocate() will do nothing.
                    );
        } catch (SQLiteException e) {
            // Do nothing - database doesn't exist yet
        }

        if (checkDB != null) {
            checkDB.close();
        }

        return checkDB != null ? true : false;
    }

    /**
     * Copies our database from our local assets/ folder to the newly created
     * empty database in the system/ folder, from where it can be accessed and
     * handled.
     * 
     * @throws IOException
     */
    private void copyDatabase() throws IOException {
        // Open your local database as the input stream
        InputStream inputStream = mContext.getAssets().open(DB_NAME);
        try {
            // Path to the newly created empty database
            String outFileName = DB_PATH + DB_NAME;
            OutputStream outputStream = new FileOutputStream(outFileName);
            try {
                // Transfer bytes from input file to output file
                byte[] buffer = new byte[1024];
                int len;
                while ((len = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, len);
                }
                outputStream.flush();
            } finally {
                outputStream.close();
            }
        } finally {
            inputStream.close();
        }
    }

    /**
     * Opens the database.
     * 
     * @throws SQLiteException
     */
    public void openDatabase() throws SQLiteException {
        String dbPath = DB_PATH + DB_NAME;
        sqliteDB = SQLiteDatabase.openDatabase(
                dbPath, 
                null,
                SQLiteDatabase.NO_LOCALIZED_COLLATORS   // Open without support for localized collators. setLocate() will do nothing.
                );
    }

    /**
     * Returns a Cursor over all words that match the given query
     * 
     * @param query
     *            The string to search for
     * @param columns
     *            The columns to include, if null then all are included
     * @return Cursor over all words that match, or null if none found.
     */
    public Cursor getWordMatches(String query, String[] columns) {
        // Search over location names and abbreviations
        String selection = 
                LOC_NAME + " LIKE '%" + query + "%'" + " OR " + 
                ABBR     + " LIKE '%" + query + "%'";

        return query(selection, columns);
    }

    /**
     * Performs a database query.
     * 
     * @param selection
     *            The selection clause
     * @param columns
     *            The columns to return
     * @return A Cursor over all rows matching the query
     */
    private Cursor query(String selection, String[] columns) {
        
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(TABLE_NAME);

        Cursor cursor = builder.query(
                sqliteDB,       // SQLiteDatabase
                columns,        // columns
                selection,      // selection
                null,           // selectionArgs
                null,           // groupBy
                null,           // having
                null            // sortOrder
                );
        
        if (cursor == null) {
            return null;
        } else if (!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }

        return convertCursorToMatrixCursor(cursor);
    }
    
    /**
     * Construct a Cursor conforming to Android custom suggestion standards.
     * 
     * @see http://developer.android.com/guide/topics/search/adding-custom-suggestions.html#SuggestionTable
     * @param cursor
     * @return The constructed Cursor
     */
    private Cursor convertCursorToMatrixCursor(Cursor cursor) {
        int id_index        = cursor.getColumnIndex(BaseColumns._ID);
        int loc_name_index  = cursor.getColumnIndex(LOC_NAME);
        int abbr_index      = cursor.getColumnIndex(ABBR);
        int coords_index    = cursor.getColumnIndex(COORDS);
        int addr_index      = cursor.getColumnIndex(ADDR);
        
        MatrixCursor mCursor = new MatrixCursor(COLUMN_NAMES, cursor.getCount());
        
        while (!cursor.isAfterLast()) {
            int id          = cursor.getInt(id_index);
            String loc_name = cursor.getString(loc_name_index);
            String abbr     = cursor.getString(abbr_index);
            String coords   = cursor.getString(coords_index);
            String addr     = cursor.getString(addr_index);
            
            List<Object> columnValues = new ArrayList<Object>();
            columnValues.add(id);
            columnValues.add(loc_name);
            columnValues.add(abbr);
            columnValues.add(loc_name + DATA_SEPARATOR + abbr + DATA_SEPARATOR + coords);
            columnValues.add(addr);
            
            mCursor.addRow(columnValues);
            
            cursor.moveToNext();
        }
        
        // As we are not returning this Cursor, release the Cursor and its resources 
        cursor.close();
        
        return mCursor;
    }
}
