package awongdev.android.cedict.database;

import android.content.Context;
import android.os.AsyncTask;
import awongdev.android.cedict.CantoneseCedictActivity;

public class InitializeDatabasesTask extends AsyncTask<Context, Integer, Dictionary> {
	private final CantoneseCedictActivity activity;
	
	public InitializeDatabasesTask (CantoneseCedictActivity activity) {
		this.activity = activity;
	}
	
	@Override
	protected Dictionary doInBackground(Context... params) {
		Context context = params[0];
		Dictionary dictionary = new Dictionary(context);
		// TODO(awong): Set 100ms timer to show splash screen of initialize doesn't complete.
		dictionary.initializeDatabases();
		return dictionary;
	}

	@Override
	protected void onProgressUpdate(Integer... progress) {
		activity.showInitializing();
	}
	
	@Override
	protected void onPostExecute(Dictionary d) {
		activity.assignDictionary(d);
	}
}
