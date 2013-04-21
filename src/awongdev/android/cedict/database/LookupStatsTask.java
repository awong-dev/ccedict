package awongdev.android.cedict.database;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import awongdev.android.cedict.R;

public class LookupStatsTask extends AsyncTask<Void, Void, Cursor> {
	private final ListView resultPanel;
	private final Context applicationContext;
	private final Dictionary dictionary;

	public LookupStatsTask(Context context, ListView resultPanel, Dictionary dictionary) {
		this.applicationContext = context;
		this.resultPanel = resultPanel;
		this.dictionary = dictionary;
	}
	static final int[] TO_FIELDS = new int[] { R.id.entry, R.id.variant, R.id.cantonese};
	static final String[] VIEW_COLUMNS = new String[] { "entry", "last_lookup", "num_lookups"};
	
	@Override
	protected Cursor doInBackground(Void... arg0) {
		return dictionary.lookupStats();
	}
	
	protected void onPostExecute(Cursor cursor) {
		resultPanel.setAdapter(new SimpleCursorAdapter(
				applicationContext, R.layout.entry_layout, cursor,
				VIEW_COLUMNS, TO_FIELDS));
		resultPanel.invalidate();
	 }

}
