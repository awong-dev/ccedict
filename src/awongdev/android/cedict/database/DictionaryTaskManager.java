package awongdev.android.cedict.database;

import java.io.File;

import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.support.v4.content.Loader;
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
	
	public static class DetailedProgress {
		public DetailedProgress(String status) {
			this.status = status;
			this.progress = -1;
			this.max = -1;
		}
		
		public DetailedProgress(String status, int progress, int max) {
			this.status = status;
			this.progress = progress;
			this.max = max;
		}
		public final String status;
		public final int progress;
		public final int max;
	}
	
	public static interface TaskProgressListener<Progress, Result> {
		void onBegin(Progress initialStatus);
		void onProgress(Progress progress);
		void onComplete(Result dtm);
	}	
	
	public static void asyncCreate(TaskProgressListener<String, DictionaryTaskManager> listener, Context context, Handler handler) {
		new InitializeDatabasesTask(listener, context, handler).execute();
	}

	DictionaryTaskManager(Context context, Dictionary dictionary, Handler handler) {
		this.context = context;
		this.dictionary = dictionary;
		this.handler = handler;
	}	
	
	public void doDownloadLatestDictionary(TaskProgressListener<DetailedProgress, Void> listener) {
		new DownloadDatabaseTask(context, dictionary, listener).execute();
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

	public Loader<Cursor> createLookupStatsLoader() {
		return new SqliteCursorLoader(context) {
			@Override
			protected Cursor getCursor() {
				return dictionary.lookupStats();
			}
		};
	}

	public Loader<Cursor> createLookupTermLoader(final String term, final boolean is_roman) {
		return new SqliteCursorLoader(context) {
			@Override
			protected Cursor getCursor() {
				return dictionary.lookupTerm(term, is_roman);
			}
		};
	}
}
