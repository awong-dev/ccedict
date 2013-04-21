package awongdev.android.cedict.database;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import awongdev.android.cedict.R;

class LookupTask extends AsyncTask<String, Void, Cursor> {
	private static final int[] TO_FIELDS = new int[] { R.id.entry, R.id.variant, R.id.cantonese,
	R.id.pinyin, R.id.definition };
	private static final String[] VIEW_COLUMNS = new String[] { "entry", "variant", "cantonese",
	"pinyin", "definition" };
	
	private final boolean isRoman;
	private final ListView resultPanel;
	private final Context applicationContext;
	private final Dictionary dictionary;
	
	LookupTask(Context context, boolean isRoman, ListView resultPanel, Dictionary dictionary) {
		this.applicationContext = context;
		this.isRoman = isRoman;
		this.resultPanel = resultPanel;
		this.dictionary = dictionary;
	}

	@Override
	protected Cursor doInBackground(String... term) {
		return dictionary.lookupTerm(term[0], isRoman);
	 }
	
	@Override
	protected void onPostExecute(Cursor cursor) {
		resultPanel.setAdapter(new SimpleCursorAdapter(
				applicationContext, R.layout.entry_layout, cursor,
				VIEW_COLUMNS, TO_FIELDS));
		resultPanel.invalidate();
	 }
}