package awongdev.android.cedict.database;

import android.content.Context;
import android.os.AsyncTask;
import awongdev.android.cedict.database.DictionaryTaskManager.DictionaryTaskManagerInitListener;

class InitializeDatabasesTask extends AsyncTask<Void, Integer, Dictionary> {
	private final DictionaryTaskManagerInitListener listener;
	private final Context context;
	
	InitializeDatabasesTask(DictionaryTaskManagerInitListener listener, Context context) {
		this.listener = listener;
		this.context = context;
	}
	
	@Override
	protected Dictionary doInBackground(Void... params) {
		Dictionary dictionary = new Dictionary(context);
		// TODO(awong): Set 100ms timer to show splash screen of initialize doesn't complete.
		dictionary.initializeDatabases();
		return dictionary;
	}

	@Override
	protected void onProgressUpdate(Integer... progress) {
		// listener.showInitializing();
	}
	
	@Override
	protected void onPostExecute(Dictionary d) {
		listener.onInitialized(new DictionaryTaskManager(context, d));
	}
}
