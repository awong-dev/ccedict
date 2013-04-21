package awongdev.android.cedict.database;

import java.io.File;

import android.os.AsyncTask;

class OverwriteDictionaryTask extends AsyncTask<Dictionary, Void, Void> {
	private final File newDictionaryPath;

	public OverwriteDictionaryTask(File newDictionaryPath) {
		this.newDictionaryPath = newDictionaryPath;
	}

	@Override
	protected Void doInBackground(Dictionary... params) {
		params[0].replaceDictionary(newDictionaryPath);
		return null;
	}
}