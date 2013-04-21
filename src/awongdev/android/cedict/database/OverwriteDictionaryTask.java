package awongdev.android.cedict.database;

import java.io.File;

import android.os.AsyncTask;

public class OverwriteDictionaryTask extends AsyncTask<Dictionary, Void, Void> {
	private final File newDictionaryPath;

	public OverwriteDictionaryTask(File newDictionaryPath) {
		this.newDictionaryPath = newDictionaryPath;
	}

	@Override
	protected Void doInBackground(Dictionary... params) {
		params[0].overwriteDatabaseFile(newDictionaryPath);
		return null;
	}
}