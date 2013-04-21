package awongdev.android.cedict.database;

import android.os.AsyncTask;

class UpdateStatsTask extends AsyncTask<String, Void, Void> {
	Dictionary dictionary;
	UpdateStatsTask(Dictionary dictionary) {
		this.dictionary = dictionary;
	}

	@Override
	protected Void doInBackground(String... entry) {
		if (this.isCancelled()) {
			return null;
		}
		dictionary.recordTermLookup(entry[0]);
		return null;
	}

}
