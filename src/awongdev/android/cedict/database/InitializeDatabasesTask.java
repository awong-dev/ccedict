package awongdev.android.cedict.database;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import awongdev.android.cedict.database.DictionaryTaskManager.TaskProgressListener;

class InitializeDatabasesTask extends AsyncTask<Void, String, Dictionary> {
	private final TaskProgressListener<String, DictionaryTaskManager> listener;
	private final Context context;
	private final Handler handler;
	
	InitializeDatabasesTask(TaskProgressListener<String, DictionaryTaskManager> listener, Context context, Handler handler) {
		this.listener = listener;
		this.context = context;
		this.handler = handler;
	}
	
	@Override
	protected void onPreExecute() {
		listener.onBegin("Loading Databases...");	
	}
	
	@Override
	protected Dictionary doInBackground(Void... params) {
		Dictionary dictionary = new Dictionary(context);
		dictionary.initializeDatabases();
		return dictionary;
	}

	@Override
	protected void onProgressUpdate(String... progress) {
		listener.onProgress(progress[0]);
	}
	
	@Override
	protected void onPostExecute(Dictionary dictionary) {
		listener.onComplete(new DictionaryTaskManager(context, dictionary, handler));
	}
}
