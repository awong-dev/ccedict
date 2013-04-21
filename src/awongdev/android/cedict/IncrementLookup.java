package awongdev.android.cedict;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import awongdev.android.cedict.database.Dictionary;

public class IncrementLookup extends AsyncTask<String, Void, Void> {
	Dictionary dictionary;
	IncrementLookup(Dictionary dictionary) {
		this.dictionary = dictionary;
	}

	@Override
	protected Void doInBackground(String... entry) {
		SQLiteDatabase database = dictionary.getAnnotationsDatabase();
		if (this.isCancelled()) {
			return null;
		}
		try {
			database.beginTransaction();
			Cursor cursor = database.query("Annotations",
					new String[] {"rowid, num_lookups"},
					"entry = ?", new String[] {entry[0]}, null, null, null);
			cursor.moveToFirst();
			ContentValues values = new ContentValues();
			values.put("last_lookup", System.currentTimeMillis());
	
			if (cursor.getCount() != 0) {
				String row_id = cursor.getString(0);
				long count = cursor.getLong(1);
				values.put("num_lookups", count + 1);
				database.update("Annotations", values, "rowid = ?",
						new String[] {row_id});
			} else {
				values.put("num_lookups", 1);
				values.put("entry", entry[0]);
				database.insert("Annotations", null, values);
			}
			database.setTransactionSuccessful();
		} finally {
			database.endTransaction();
		}
		return null;
	}

}
