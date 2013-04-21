package awongdev.android.cedict.database;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import android.os.Environment;
import android.util.Log;

class DictionaryLoader {
	private static final String LOG_TAG = DictionaryLoader.class.getCanonicalName();
	private static final int BUFFER_SIZE = 4096;
	
	void overwriteDatabaseGzipFile(File oldDictionaryPath, File newDictionaryPath) {
		GZIPInputStream is = null;
		try {
			is = new GZIPInputStream(new BufferedInputStream(new FileInputStream(newDictionaryPath), BUFFER_SIZE));
			overwriteDatabaseStream(oldDictionaryPath, is);
		} catch (IOException e) {
			Log.w(LOG_TAG, "Error opening database from gzip file: " + newDictionaryPath, e);
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				Log.w(LOG_TAG, "Unable to close input gzip file: " + newDictionaryPath, e);	
			}
		}
	}

	void overwriteDatabaseFile(File oldDictionaryPath, File newDictionaryPath) {
		InputStream is = null;
		try {
			is = new BufferedInputStream(new FileInputStream(newDictionaryPath), BUFFER_SIZE);
			overwriteDatabaseStream(oldDictionaryPath, is);
		} catch (IOException e) {
			Log.w(LOG_TAG, "Error opening database from gzip file: " + newDictionaryPath, e);
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				Log.w(LOG_TAG, "Unable to close input gzip file: " + newDictionaryPath, e);	
			}
		}
	}

	void overwriteDatabaseStream(File oldDictionaryPath, InputStream is) {
		BufferedOutputStream myOutput = null;
		try {
			myOutput = new BufferedOutputStream(new FileOutputStream(oldDictionaryPath), BUFFER_SIZE);
			byte[] buffer = new byte[BUFFER_SIZE];
			int length;
			while ((length = is.read(buffer)) > 0) {
				myOutput.write(buffer, 0, length);
			}
		} catch (IOException e) {
			Log.w(LOG_TAG, "Error overwriting database: " + oldDictionaryPath + " from stream" , e);
		} finally {
			if (myOutput != null) {
				try {
					myOutput.close();
				} catch (IOException e) {
					Log.w(LOG_TAG, "Error closing database: " + oldDictionaryPath, e);
				}
			}
		}
	
	}

	void loadDictionaryDir(File oldDictionaryPath) {
		String state = Environment.getExternalStorageState();
	
		if (Environment.MEDIA_MOUNTED.equals(state)
				|| Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			// We can only read the media
		} else {
			// Something else is wrong. It may be one of many other states, but
			// all we need
			// to know is we can neither read nor write
			// TODO(awong): Show error.
			return;
		}
	
		// String path = Environment.getExternalStorageDirectory().getPath() +
		// "/Android/data/awongdev.android.app/cc_cedicts";
		File data_dir = new File(""); // /sdcard/Download");
		if (!data_dir.exists()) {
			return;
		}
		File[] files = data_dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String s) {
				if (s.endsWith(".u8.db.gz")) {
					return true;
				}
	
				return false;
			}
		});
		for (File dict : files) {
			if (dict.canRead()) {
				//loadDictionary(dict);
				//loadSqlDump(dict);
				overwriteDatabaseFile(oldDictionaryPath, dict);
			} else {
				// TODO(awong): List error.
			}
		}
	}
	/*
	void loadSqlDump(File dict) {
		try {
			BufferedReader reader = openFile(dict);
			DictionaryUpdater updater = new DictionaryUpdater(getDictionaryDatabase());
			updater.replaceWithSqlDump(reader);
		} catch (IOException e) {
			new RuntimeException(e);
		}
	}

	void updateDictionaryFromCcedict(File dict) {
		try {
			BufferedReader reader = openFile(dict);
			DictionaryUpdater updater = new DictionaryUpdater(getDictionaryDatabase());
			updater.updateFromCcedict(dict.getName(), reader);
		} catch (IOException e) {
			new RuntimeException(e);
		}
	}

	private BufferedReader openFile(File file) {
		try {
			return new BufferedReader(new InputStreamReader(
					new FileInputStream(file), "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			new RuntimeException(e);
		} catch (FileNotFoundException e) {
			new RuntimeException(e);
		}
		return null;
	}
	*/

}
