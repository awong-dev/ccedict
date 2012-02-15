package awongdev.android.ccedict;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

public class IncrementLookup extends AsyncTask<String, Void, Void> {
	SQLiteDatabase database;
	IncrementLookup(SQLiteDatabase database) {
		this.database = database;
	}

	@Override
	protected Void doInBackground(String... entry) {
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
