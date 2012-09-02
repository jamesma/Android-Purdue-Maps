package com.jamesma.purdue.maps.database;

import java.io.IOException;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * A content provider for custom suggestions in a search dialog.
 * 
 * @author James Ma (http://jamesma.info)
 * 
 */
public class LocationsProvider extends ContentProvider {
    public static final String AUTHORITY = "com.jamesma.purdue.maps.database.LocationsProvider";

    // UriMatcher stuff
    private static final int SEARCH_SUGGEST = 1;
    private static final UriMatcher sURIMatcher = buildUriMatcher();

    private DatabaseHelper dbHelper;

    /**
     * Builds up a UriMatcher for search suggestion.
     */
    private static UriMatcher buildUriMatcher() {
        UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_SUGGEST);
        matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_SUGGEST);
        return matcher;
    }

    /**
     * Initializes the provider by creating a new DatabaseHelper. onCreate() is
     * called automatically when Android creates the provider in response to a
     * resolver request from a client.
     */
    @Override
    public boolean onCreate() {
        // Create a database helper object to manage our SQLite database in assets/
        dbHelper = new DatabaseHelper(getContext());
        
        try {
            dbHelper.createDatabase();
        } catch (IOException e) {
            throw new Error("Unable to create database");
        }

        try {
            dbHelper.openDatabase();
        } catch (SQLiteException e) {
            throw new Error("Unable to open database");
        }
        
        // Assumes that any failures will be reported by a thrown exception.
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        // Use the UriMatcher to see what kind of query we have and format the database query accordingly
        switch (sURIMatcher.match(uri)) {
            case SEARCH_SUGGEST:
                if (selectionArgs == null) {
                    throw new IllegalArgumentException("selectionArgs must be provided for the Uri: " + uri);
                }
                return getSuggestions(selectionArgs[0]);
            default:
                throw new IllegalArgumentException("Unknown Uri: " + uri);
        }
    }

    /**
     * Helper method to begin search process in DatabaseHelper.
     * 
     * @param query
     * @return
     */
    private Cursor getSuggestions(String query) {
        query = query.toLowerCase();
        String[] columns = new String[] { 
                BaseColumns._ID,
                DatabaseHelper.LOC_NAME, 
                DatabaseHelper.ABBR,
                DatabaseHelper.COORDS,
                DatabaseHelper.ADDR
                };

        return dbHelper.getWordMatches(query, columns);
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        return 0;
    }

}
