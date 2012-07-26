package awongdev.android.cedict;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.AsyncTask;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import awongdev.android.cedict.R;

public class FlashCardTask extends AsyncTask<Void, Void, Cursor> {
	private final ListView resultPanel;
	Context applicationContext;
	private SQLiteDatabase database;

	FlashCardTask(Context context, ListView resultPanel, SQLiteDatabase database) {
		this.applicationContext = context;
		this.resultPanel = resultPanel;
		this.database = database;
	}
	static final int[] TO_FIELDS = new int[] { R.id.entry, R.id.variant, R.id.cantonese};
	static final String[] VIEW_COLUMNS = new String[] { "entry", "last_lookup", "num_lookups"};
	
	@Override
	protected Cursor doInBackground(Void... arg0) {
		SQLiteQueryBuilder lookupQuery = new SQLiteQueryBuilder();
		lookupQuery.setTables("Annotations");
		
		return lookupQuery.query(database,
				new String[] {"rowid _id", "entry", "num_lookups", "last_lookup"}, "",
				null, null, null, "num_lookups desc, last_lookup desc");
	}
	
	protected void onPostExecute(Cursor cursor) {
		resultPanel.setAdapter(new SimpleCursorAdapter(
				applicationContext, R.layout.entry_layout, cursor,
				VIEW_COLUMNS, TO_FIELDS));
		resultPanel.invalidate();
	 }

}
