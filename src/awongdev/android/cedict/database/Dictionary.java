package awongdev.android.cedict.database;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPInputStream;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;
import awongdev.android.cedict.HexUtil;
import awongdev.android.cedict.Preconditions;

public class Dictionary {
	private SQLiteOpenHelper dictionaryOpener;
	private SQLiteOpenHelper annotationsOpener;
	private SQLiteDatabase dictionaryDatabase;
	private SQLiteDatabase annotationsDatabase;
	private final Context applicationContext;
	private static final int BUFFER_SIZE = 4096;
	private static final String LOG_TAG = Dictionary.class.getCanonicalName();

	public Dictionary(Context applicationContext) {
		this.applicationContext = applicationContext;
	}

	public SQLiteDatabase getDictionaryDatabase() {
		synchronized(dictionaryOpener) {
			return dictionaryDatabase;
		}
	}

	public SQLiteDatabase getAnnotationsDatabase() {
		return annotationsDatabase;
	}

	public void initializeDatabases() {
		Preconditions.IsNull(dictionaryOpener, "dictionary opener already initialized");
		Preconditions.IsNull(annotationsOpener, "annotations opener already initialized");
		annotationsOpener = 
				new AnnotationsDatabaseOpenHelper("annotations", applicationContext);
		dictionaryOpener =
				new DictionaryDatabaseOpenHelper("dictionary", applicationContext);
		
		openDictionaryDatabase();
		openAnnotationsDatabase();
	}
	
	void overwriteDatabaseGzipFile(File dict) {
		GZIPInputStream is = null;
		try {
			is = new GZIPInputStream(new BufferedInputStream(new FileInputStream(dict), BUFFER_SIZE));
			overwriteDatabaseStream(is);
		} catch (IOException e) {
			Log.w(LOG_TAG, "Error opening database from gzip file: " + dict, e);
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				Log.w(LOG_TAG, "Unable to close input gzip file: " + dict, e);	
			}
		}
	}

	void overwriteDatabaseFile(File dict) {
		InputStream is = null;
		try {
			is = new BufferedInputStream(new FileInputStream(dict), BUFFER_SIZE);
			overwriteDatabaseStream(is);
		} catch (IOException e) {
			Log.w(LOG_TAG, "Error opening database from gzip file: " + dict, e);
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				Log.w(LOG_TAG, "Unable to close input gzip file: " + dict, e);	
			}
		}
	}
	
	void overwriteDatabaseStream(InputStream is) {
		BufferedOutputStream myOutput = null;
		File databasePath = applicationContext.getDatabasePath("dictionary");
		try {
			dictionaryOpener.close();
			//myOutput = new FileOutputStream("/data/data/awongdev.android.cedict/databases/dictionary");
			myOutput = new BufferedOutputStream(new FileOutputStream(databasePath), BUFFER_SIZE);
			byte[] buffer = new byte[BUFFER_SIZE];
			int length;
			while ((length = is.read(buffer)) > 0) {
				myOutput.write(buffer, 0, length);
			}
		} catch (IOException e) {
			Log.w(LOG_TAG, "Error overwriting database: " + databasePath + " from stream" , e);
		} finally {
			if (myOutput != null) {
				try {
					myOutput.close();
				} catch (IOException e) {
					Log.w(LOG_TAG, "Error closing database: " + databasePath, e);
				}
			}
			
			// We must have some sort of database or the app will crash.
			dictionaryDatabase = dictionaryOpener.getReadableDatabase();
		}

	}

	void verifyAndLoadDictionaryFile(File filename) {
		DigestInputStream instream = null;
		try {
			try {
				final int BUFFER_SIZE = 4096;
				getDictionaryDatabase().beginTransaction();
				
				MessageDigest md = MessageDigest.getInstance("SHA-1");				
				instream = new DigestInputStream(
						new GZIPInputStream(new BufferedInputStream(
								new FileInputStream(filename), BUFFER_SIZE)),
						md);
				
			 
	//			    updateDatabaseFromStream(new InputStreamReader(instream));
			 
			    String hash = HexUtil.bytesToHex(md.digest());
				System.err.println("Digest: " + hash);
			    if (hash.equals("the right part of the filename")) {
			    	getDictionaryDatabase().setTransactionSuccessful();
			    } else {
			    	// TODO(awong): Log an error here. Signal to user.
			    }
			 
				
			} catch (NoSuchAlgorithmException nsae) {
				// TODO Auto-generated catch block
				nsae.printStackTrace();
			} finally {
				getDictionaryDatabase().endTransaction();
				if (instream != null) {
					instream.close();
				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	void loadDictionaryDir() {
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
				overwriteDatabaseFile(dict);
			} else {
				// TODO(awong): List error.
			}
		}
	}

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

	BufferedReader openFile(File file) {
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

	private void openAnnotationsDatabase() {
		annotationsDatabase = annotationsOpener.getWritableDatabase();
	}

	private void openDictionaryDatabase() {
		dictionaryDatabase = dictionaryOpener.getReadableDatabase();
	}
}