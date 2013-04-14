package awongdev.android.cedict;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import android.app.Activity;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ListView;
import awongdev.android.cedict.R;

public class CantoneseCedictActivity extends Activity {
	private final Pattern ALPHA_NUM = Pattern.compile("^[a-zA-Z0-9 ]+$");
	private SQLiteDatabase database;
	IncrementLookup outstanding_increment = null;

	ListView getResultPanel() { return (ListView) findViewById(R.id.ResultPanel); }
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		DictionaryDatabaseOpenHelper dictionaryOpenHelper = new DictionaryDatabaseOpenHelper("dictionary",
				getApplicationContext());
		database = dictionaryOpenHelper.getWritableDatabase();
		
		setContentView(R.layout.main);
		// //// LIST VIEW
		EditText searchBox = (EditText) findViewById(R.id.SearchBox);
		searchBox.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable s) {
				final String term = s.toString().trim();
				if (outstanding_increment != null) {
					outstanding_increment.cancel(true);
					outstanding_increment = null;
				}
				if (term.length() == 0)
					return;
				
				// Special keyword to go to flashcard mode.
				if (term.equals("hs")) {
					new FlashCardTask(getApplicationContext(), getResultPanel(), database).execute();
					return;
				}
				boolean is_roman = false;
				if (ALPHA_NUM.matcher(term).find()) {
					// We need 2 characters to bother with a romanization lookup.
					if (s.length() < 2)
						return;
					is_roman = true;
				}
				new LookupTask(getApplicationContext(), is_roman, getResultPanel(), database).execute(term);
				if (!is_roman) {
					// Increment lookup frequency.
					outstanding_increment = new IncrementLookup(database);
					Handler handler = new Handler();
					handler.postDelayed(new Runnable() {
						final IncrementLookup existing_task = outstanding_increment;

						public void run() {
							if (!existing_task.isCancelled()) {
								existing_task.execute(term);
							}
						}
					}, 1000);
				}
			}

			public void beforeTextChanged(CharSequence arg0, int arg1,
					int arg2, int arg3) {
			}

			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.Load:
			new Thread(new Runnable() {
				public void run() {
		//			loadDictionaryDir();
					verifyAndLoadDictionaryFile();
				}
			}).start();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	private static String bytesToHex(byte[] bytes) {
	    final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
	    char[] hexChars = new char[bytes.length * 2];
	    int v;
	    for ( int j = 0; j < bytes.length; j++ ) {
	        v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
	
	void verifyAndLoadDictionaryFile() {
		File filename = new File("/sdcard/b9eb6874b3a4fcea767ca2b0a288cef2e612946b.u8.db.gz");
		DigestInputStream instream = null;
		try {
			try {
				final int BUFFER_SIZE = 4096;
				database.beginTransaction();
				
				MessageDigest md = MessageDigest.getInstance("SHA-1");				
				instream = new DigestInputStream(
						new GZIPInputStream(new BufferedInputStream(
								new FileInputStream(filename), BUFFER_SIZE)),
						md);
				
			 
//			    updateDatabaseFromStream(new InputStreamReader(instream));
			 
			    String hash = bytesToHex(md.digest());
				System.err.println("Digest: " + hash);
			    if (hash.equals("the right part of the filename")) {
			    	database.setTransactionSuccessful();
			    } else {
			    	// TODO(awong): Log an error here. Signal to user.
			    }
			 
				
			} catch (NoSuchAlgorithmException nsae) {
				// TODO Auto-generated catch block
				nsae.printStackTrace();
			} finally {
				database.endTransaction();
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
		File data_dir = new File("/sdcard/Download");
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
				overwriteDatabase(dict);
			} else {
				// TODO(awong): List error.
			}
		}
	}
	
	void overwriteDatabase(File dict) {
		BufferedOutputStream myOutput = null;
		GZIPInputStream is = null;
		try {
			try {
				//myOutput = new FileOutputStream("/data/data/awongdev.android.cedict/databases/dictionary");
				File db = getApplicationContext().getDatabasePath("dictionary");
				final int BUFFER_SIZE = 4096;
				myOutput = new BufferedOutputStream(new FileOutputStream(db), BUFFER_SIZE);
				is = new GZIPInputStream(new BufferedInputStream(new FileInputStream(dict), BUFFER_SIZE));
				byte[] buffer = new byte[BUFFER_SIZE];
				int length;
				while ((length = is.read(buffer)) > 0) {
					myOutput.write(buffer, 0, length);
				}
			} finally {
				if (myOutput != null) {
					myOutput.flush();
					myOutput.close();
				}
				if (is != null) is.close();
    		}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	void loadSqlDump(File dict) {
		try {
			BufferedReader reader = openFile(dict);
			DictionaryUpdater updater = new DictionaryUpdater(database);
			updater.replaceWithSqlDump(reader);
		} catch (IOException e) {
			new RuntimeException(e);
		}
	}

	void updateDictionaryFromCcedict(File dict) {
		try {
			BufferedReader reader = openFile(dict);
			DictionaryUpdater updater = new DictionaryUpdater(database);
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
}