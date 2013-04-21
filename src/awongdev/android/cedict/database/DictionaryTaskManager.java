package awongdev.android.cedict.database;

import java.io.File;

import android.content.Context;
import android.os.Handler;
import android.widget.ListView;

/**
 * This is the public API of the database package designed for use on the UI thread.
 *  
 * DictionaryTaskManager handles creation of all the AsyncTasks associated with
 * accessing the database. The AsyncTasks bridge between the UI thread and the
 * background thread.  All other classes and method are assumed to run in the
 * background only.  This class itself should only ever be used on the UI thread.
 */
public class DictionaryTaskManager {
	private final Context context;
	private final Dictionary dictionary;
	private final Handler handler;
	
	// Outstanding tasks.
	private UpdateStatsTask outstandingStatsUpdate;
	private LookupTask outstandingLookup;
	private LookupStatsTask flashCardTask;
	
	public static interface DictionaryTaskManagerInitListener {
		void onInitialized(DictionaryTaskManager dtm);
	}
	
	public static void asyncCreate(DictionaryTaskManagerInitListener listener, Context context) {
		new InitializeDatabasesTask(listener, context).execute();
	}

	DictionaryTaskManager(Context context, Dictionary dictionary) {
		this.context = context;
		this.dictionary = dictionary;
		this.handler = new Handler();
	}	
	
	public void doLoadNewDictionary(final File newDictionaryPath) {
		new OverwriteDictionaryTask(newDictionaryPath).execute(dictionary);
	}

	public void doLookup(final String term, boolean is_roman, ListView resultPanel) {
		cancelOutstandingSearchTasks();
		
		outstandingLookup = new LookupTask(context, is_roman, resultPanel, dictionary);
		outstandingLookup.execute(term);
		if (!is_roman) {
			doUpdateStats(term);
		}
	}

	public void doStatsLookup(ListView resultPanel) {
		cancelOutstandingSearchTasks();
		
		flashCardTask = new LookupStatsTask(context, resultPanel, dictionary);
		flashCardTask.execute();
	}
	
	// Increment lookup frequency.
	private void doUpdateStats(final String term) {		
		outstandingStatsUpdate = new UpdateStatsTask(dictionary);
		handler.postDelayed(new Runnable() {
			final UpdateStatsTask existing_task = outstandingStatsUpdate;

			public void run() {
				if (!existing_task.isCancelled()) {
					existing_task.execute(term);
				}
			}
		}, 1000);
	}
	
	private void cancelOutstandingSearchTasks() {
		if (outstandingStatsUpdate != null) {
			outstandingStatsUpdate.cancel(true);
			outstandingStatsUpdate = null;
		}
		if (outstandingLookup != null) {
			outstandingLookup.cancel(true);
			outstandingLookup = null;
		}
		if (flashCardTask != null) {
			flashCardTask.cancel(true);
			flashCardTask = null;
		}
	}
}
