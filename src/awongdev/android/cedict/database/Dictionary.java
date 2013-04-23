package awongdev.android.cedict.database;

import java.io.File;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;
import awongdev.android.cedict.Preconditions;

class Dictionary {
	private static final String LOG_TAG = Dictionary.class.getCanonicalName();
	
	private SQLiteOpenHelper dictionaryOpener;
	private SQLiteOpenHelper annotationsOpener;
	private SQLiteDatabase dictionaryDatabase;
	private SQLiteDatabase annotationsDatabase;
	private final Context context;

	Dictionary(Context context) {
		this.context = context;
	}
	
	public void initializeDatabases() {
		Preconditions.IsNull(dictionaryOpener, "dictionary opener already initialized");
		Preconditions.IsNull(annotationsOpener, "annotations opener already initialized");
		annotationsOpener = 
				new AnnotationsDatabaseOpenHelper("annotations", context);
		dictionaryOpener =
				new DictionaryDatabaseOpenHelper("dictionary", context);
		
		openDictionaryDatabase();
		openAnnotationsDatabase();
	}
	
	public void replaceDictionary(File newDictionaryPath) {
		try {
			dictionaryOpener.close();
			File oldDictionaryPath = getDictionaryPath();
			Log.i(LOG_TAG, "Overwritting: " + oldDictionaryPath + " with " + newDictionaryPath);
			oldDictionaryPath.delete();
			newDictionaryPath.renameTo(oldDictionaryPath);
			/*
			DictionaryLoader loader = new DictionaryLoader();
			loader.overwriteDatabaseFile(oldDictionaryPath, newDictionaryPath);
			*/
		} finally {
			// We must have some sort of database or the app will crash.
			dictionaryDatabase = dictionaryOpener.getReadableDatabase();
		}
	}

	public File getDictionaryPath() {
		return context.getDatabasePath("dictionary");
	}
	
	public Cursor lookupTerm(String term, boolean isRoman) {
		// Lookup Query.
		final String[] COLUMNS = new String[] { "rowid _id", "entry", "simplified",
				"variant", "trust", "cantonese", "pinyin", "definition" };
		final String WHERE_ROMAN =
				" (pinyin >= ?1 AND pinyin < ?2)" +
				" OR (extra_search >= ?1 AND extra_search < ?2)";
		final String WHERE =
				"(entry >= ?1 AND entry < ?2)" +
				" OR (variant >= ?1 AND variant < ?2)";
		
		String nextTerm = incrementTerm(term);
		String[] selectorArgs = { term, nextTerm };
		SQLiteQueryBuilder lookupQuery = new SQLiteQueryBuilder();
		lookupQuery.setTables("FlattenedEntries");
		return lookupQuery.query(dictionaryDatabase, COLUMNS, isRoman ? WHERE_ROMAN : WHERE,
				selectorArgs, null, null, null);
	}
	
	public Cursor lookupStats() {
		SQLiteQueryBuilder lookupQuery = new SQLiteQueryBuilder();
		lookupQuery.setTables("Annotations");
		
		return lookupQuery.query(annotationsDatabase,
				new String[] {"rowid _id", "entry", "num_lookups", "last_lookup"}, "",
				null, null, null, "num_lookups desc, last_lookup desc");
	}

	public void recordTermLookup(String term) {
		try {
			annotationsDatabase.beginTransaction();
			Cursor cursor = annotationsDatabase.query("Annotations",
					new String[] {"rowid, num_lookups"},
					"entry = ?", new String[] {term}, null, null, null);
			cursor.moveToFirst();
			ContentValues values = new ContentValues();
			values.put("last_lookup", System.currentTimeMillis());
	
			if (cursor.getCount() != 0) {
				String row_id = cursor.getString(0);
				long count = cursor.getLong(1);
				values.put("num_lookups", count + 1);
				annotationsDatabase.update("Annotations", values, "rowid = ?",
						new String[] {row_id});
			} else {
				values.put("num_lookups", 1);
				values.put("entry", term);
				annotationsDatabase.insert("Annotations", null, values);
			}
			annotationsDatabase.setTransactionSuccessful();
		} finally {
			annotationsDatabase.endTransaction();
		}
	}

	/// Find the next successor string by incrementing the unicode codepoint for the last character. Won't work
	/// 100% (say, if we're on the last codepoint), but should be good enough for now.  Help with range points.
	private String incrementTerm(String s) {
		int[] codepoints = new int[s.codePointCount(0, s.length())];
		int pos = 0;
		
		final int length = s.length();
		for (int offset = 0; offset < length; ) {
			final int cp = s.codePointAt(offset);
			codepoints[pos++] = cp;
			offset += Character.charCount(cp);
		}
		codepoints[codepoints.length-1]++;
		return new String(codepoints, 0, codepoints.length);
		
	}

	private void openAnnotationsDatabase() {
		annotationsDatabase = annotationsOpener.getWritableDatabase();
	}

	private void openDictionaryDatabase() {
		dictionaryDatabase = dictionaryOpener.getReadableDatabase();
	}
}