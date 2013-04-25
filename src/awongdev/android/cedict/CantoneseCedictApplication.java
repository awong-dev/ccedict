package awongdev.android.cedict;

import java.io.File;
import java.util.HashSet;

import android.annotation.TargetApi;
import android.app.Application;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import awongdev.android.cedict.database.DictionaryTaskManager;
import awongdev.android.cedict.database.DictionaryTaskManager.DetailedProgress;
import awongdev.android.cedict.database.DictionaryTaskManager.TaskProgressListener;

public class CantoneseCedictApplication extends Application {
	private File databaseDir;
	private Handler handler;
	private TaskProgressDispatcher<String, DictionaryTaskManager> dtmInitDispatcher;
	private TaskProgressDispatcher<DetailedProgress, Void> downloadDispatcher;

	@Override
	public void onCreate() {
		super.onCreate();
		// Work around pre-Froyo bugs in HTTP connection reuse.
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
			System.setProperty("http.keepAlive", "false");
		}
		databaseDir = ensureDatabaseDir();
		handler = new Handler();
		dtmInitDispatcher = new TaskProgressDispatcher<String, DictionaryTaskManager>();
		downloadDispatcher = new TaskProgressDispatcher<DetailedProgress, Void>();
		DictionaryTaskManager.asyncCreate(dtmInitDispatcher, this, handler);
	}

	@Override
	public File getDatabasePath(String name) {
		return new File(databaseDir, name);
	}

	@Override
	public SQLiteDatabase openOrCreateDatabase(String name, int mode, CursorFactory factory) {
		return openOrCreateDatabase(name, mode, factory, null);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public SQLiteDatabase openOrCreateDatabase(String name, int mode,
			CursorFactory factory, DatabaseErrorHandler errorHandler) {
		File path = getDatabasePath(name);
		Log.v("Database", path.getAbsolutePath());
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
		    return SQLiteDatabase.openDatabase(path.getPath(), factory,
		    		SQLiteDatabase.CREATE_IF_NECESSARY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
		} else {
		    return SQLiteDatabase.openDatabase(path.getPath(), factory,
		    		SQLiteDatabase.CREATE_IF_NECESSARY | SQLiteDatabase.NO_LOCALIZED_COLLATORS,
		    		errorHandler);
		}
	}
	
	public File getDatabaseDir() {
		return databaseDir;
	}

	public void attachDtmListener(TaskProgressListener<String, DictionaryTaskManager> listener) {
		dtmInitDispatcher.attach(listener);
	}
	
	public void detachDtmListener(TaskProgressListener<String, DictionaryTaskManager> listener) {
		dtmInitDispatcher.detach(listener);
	}
	
	public void attachDownloadListener(TaskProgressListener<DetailedProgress, Void> listener) {
		downloadDispatcher.attach(listener);
	}
	
	public void detachDownloadListener(TaskProgressListener<DetailedProgress, Void> listener) {
		downloadDispatcher.detach(listener);
	}
	
	public Handler getHandler() {
		return handler;
	}
	
	public void updateDictionary() {
		dtmInitDispatcher.savedResult.doDownloadLatestDictionary(downloadDispatcher);
	}

	@TargetApi(Build.VERSION_CODES.FROYO)
	private File ensureDatabaseDir() {
		if (!shouldUseExternalStorage()) {
			File path = super.getDatabasePath("dictionary");
			return path.getParentFile();
		}
		
		String storagePath;
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
			storagePath = Environment.getExternalStorageDirectory().getAbsolutePath()
					+ "/Android/data/" + getPackageName();
		} else {
			storagePath = getExternalFilesDir(null).getAbsolutePath();
		}
		File newDatabaseDir = new File(storagePath + "/databases/");
		newDatabaseDir.mkdirs();
		return newDatabaseDir;
	}
	
	private boolean shouldUseExternalStorage() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
		    // We can read and write the media
			return true;
		} else {
			return false;
		}
	}

	private static class TaskProgressDispatcher<Progress, Result> implements TaskProgressListener<Progress, Result> {
		private enum Status {
			NEW(0),
			BEGAN(1),
			PROGRESSING(2),
			COMPLETE(3);
			
			private final int value;
			Status(int value) {
				this.value = value;
			}
		}
		
		private HashSet<TaskProgressListener<Progress, Result>> listeners = 
				new HashSet<TaskProgressListener<Progress, Result>>();
		private Progress savedInitialStatus;
		private Progress savedProgress;
		private Result savedResult;
		private Status status = Status.NEW;

		private void attach(TaskProgressListener<Progress, Result> listener) {
			listeners.add(listener);
			if (status.value >= Status.BEGAN.value) {
				listener.onBegin(savedInitialStatus);
			}
			if (status.value >= Status.PROGRESSING.value) {
				listener.onProgress(savedProgress);
			}
			if (status.value >= Status.COMPLETE.value) {
				listener.onComplete(savedResult);
			}
		}
		
		private void detach(TaskProgressListener<Progress, Result> listener) {
			listeners.remove(listener);
		}
		
		public void onBegin(Progress initialStatus) {
			savedInitialStatus = initialStatus;
			status = Status.BEGAN;
			for (TaskProgressListener<Progress, Result> listener : listeners) {
				listener.onBegin(initialStatus);
			}
		}
	
		public void onProgress(Progress progress) {
			savedProgress = progress;
			status = Status.PROGRESSING;
			for (TaskProgressListener<Progress, Result> listener : listeners) {
				listener.onProgress(progress);
			}
		}
	
		public void onComplete(Result result) {
			savedResult = result;
			status = Status.COMPLETE;
			for (TaskProgressListener<Progress, Result> listener : listeners) {
				listener.onComplete(result);
			}
		}
	}
}
