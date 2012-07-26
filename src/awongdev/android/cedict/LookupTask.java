package awongdev.android.cedict;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.AsyncTask;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import awongdev.android.cedict.R;

final class LookupTask extends
		AsyncTask<String, Void, Cursor> {
	/**
	 * 
	 */
	private final String where;
	private final ListView resultPanel;
	static final int[] TO_FIELDS = new int[] { R.id.entry, R.id.variant, R.id.cantonese,
	R.id.pinyin, R.id.definition };
	static final String[] VIEW_COLUMNS = new String[] { "entry", "variant", "cantonese",
	"pinyin", "definition" };
	static final String[] COLUMNS = new String[] { "rowid _id", "entry", "simplified",
	"variant", "trust", "cantonese", "pinyin", "definition" };
	static final String WHERE_ROMAN =
	" (pinyin >= ? AND pinyin < ?)" +
	" OR (extra_search >= ? AND extra_search < ?)";
	Context applicationContext;
	private SQLiteDatabase database;
	
	static final String WHERE =
			"(entry >= ? AND entry < ?)" +
			" OR (variant >= ? AND variant < ?)";

	LookupTask(Context context, boolean is_roman, ListView resultPanel, SQLiteDatabase database) {
		this.applicationContext = context;
		this.where = is_roman ? WHERE_ROMAN : WHERE;
		this.resultPanel = resultPanel;
		this.database = database;
	}
	
	/// Find the next successor string by incrementing the unicode codepoint for the last character. Won't work
	/// 100% (say, if we're on the last codepoint), but should be good enough for now.  Help with range points.
	private String increment(String s) {
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

	protected Cursor doInBackground(String... term) {
		String term_inc = increment(term[0]);
		String[] selectorArgs = {
				term[0], term_inc,
				term[0], term_inc
				};
		SQLiteQueryBuilder lookupQuery = new SQLiteQueryBuilder();
		lookupQuery.setTables("FlattenedEntries");
		
		return lookupQuery.query(database, COLUMNS, where, selectorArgs, null, null, null);
	 }

	protected void onPostExecute(Cursor cursor) {
		resultPanel.setAdapter(new SimpleCursorAdapter(
				applicationContext, R.layout.entry_layout, cursor,
				VIEW_COLUMNS, TO_FIELDS));
		resultPanel.invalidate();
	 }
}