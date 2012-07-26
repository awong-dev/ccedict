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
import java.util.ArrayList;
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
					handler.postDelayed(new Runnable()
					{
						final IncrementLookup existing_task = outstanding_increment; 
					     public void run()
					     {
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
					loadDictionaryDir();
				}
			}).start();
			return true;
		default:
			return super.onOptionsItemSelected(item);
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
				replaceDatabase(dict);
			} else {
				// TODO(awong): List error.
			}
		}
	}
	
	void replaceDatabase(File dict) {
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
		SQLiteDatabase db = (new DictionaryDatabaseOpenHelper("dictionary",
				getApplicationContext())).getWritableDatabase();
		try {
			try { db.execSQL("DROP TABLE FlattenedEntries;"); } catch (SQLException e) {}
			try { db.execSQL("DROP TABLE Entries;"); } catch (SQLException e) {}
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new GZIPInputStream(new BufferedInputStream(new FileInputStream(dict))), "UTF-8"));
			String line = null;
			while ((line = reader.readLine()) != null) {
				db.execSQL(line);
			}
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			db.close();
		}
	}

	static enum EntrySection {
		TRAD, SIMP, CANT, MAND, DEFN
	};

	void loadDictionary(File dict) {
		SQLiteDatabase db = (new DictionaryDatabaseOpenHelper("dictionary",
				getApplicationContext())).getWritableDatabase();
		try {
			db.beginTransaction();
			DictionaryUpdater updater = new DictionaryUpdater(db);
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(dict), "UTF-8"));
			String line = null;
			
			long source_id = updater.sourceIdForName(dict.getName());
			if (source_id == -1) {
				source_id = updater.addSource(dict.getName(), DictionaryUpdater.TrustLevel.FULL_TRUST);
			}

			while ((line = reader.readLine()) != null) {
				StringBuilder buf = new StringBuilder(1024);
				String traditional = null;
				String simplified = null;
				ArrayList<String> jyutping = new ArrayList<String>();
				ArrayList<String> pinyin = new ArrayList<String>();
				ArrayList<ArrayList<String>> definitions = new ArrayList<ArrayList<String>>();

				// Example line: ??ï¿?[kiu4] [qiao2] /surname Qiao/tall/
				EntrySection section = EntrySection.TRAD;

				line_done: for (int i = 0; i < line.length(); i++) {
					char ch = line.charAt(i);
					switch (section) {
					case TRAD:
						if (ch == ' ') {
							traditional = buf.toString();
							buf.setLength(0);
							section = EntrySection.SIMP;
						} else {
							buf.append(ch);
						}
						break;
					case SIMP:
						if (ch == ' ') {
							simplified = buf.toString();
							buf.setLength(0);
							section = EntrySection.CANT;
						} else {
							buf.append(ch);
						}
						break;
					case CANT:
						if (ch == '[') {
							// ignore
						} else if (ch == ']') {
							for (String s : buf.toString().split("\\|")) {
								s = s.trim();
								if (s.length() != 0) {
									jyutping.add(s);
								}
							}
							buf.setLength(0);
							section = EntrySection.MAND;
						} else {
							buf.append(ch);
						}
						break;
					case MAND:
						if (ch == '[') {
							// ignore
						} else if (ch == ']') {
							for (String s : buf.toString().split("\\|")) {
								s = s.trim();
								if (s.trim().length() != 0) {
									pinyin.add(s);
								}
							}
							buf.setLength(0);
							section = EntrySection.DEFN;
						} else {
							buf.append(ch);
						}
						break;
					case DEFN:
						for (String single_definition : line.substring(i).split("/")) {
							single_definition = single_definition.trim();
							if (single_definition.length() == 0) {
								continue;
							}
							ArrayList<String> major_def = new ArrayList<String>();
							major_def.add(single_definition);
							definitions.add(major_def);

						}
						break line_done;
					}
				}
				updater.upsertEntry(source_id, traditional, definitions, pinyin, jyutping);
				updater.upsertEntry(source_id, simplified, definitions, pinyin, jyutping);
			}
			
			db.setTransactionSuccessful();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			db.endTransaction();
			db.close();
		}
	}
}