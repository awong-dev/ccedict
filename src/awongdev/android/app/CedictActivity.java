package awongdev.android.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.TreeMap;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TextView;

public class CedictActivity extends Activity {
    private DictionaryOpenHelper dictionaryOpenHelper;

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		dictionaryOpenHelper = new DictionaryOpenHelper("main", getApplicationContext());
        setContentView(R.layout.main);
        
        ////// LIST VIEW
        ListView resultPanel = (ListView)findViewById(R.id.ResultPanel);
        ArrayList<String> myArrayList = new ArrayList<String>();
        myArrayList.add(new String(Environment.getExternalStorageDirectory().toString()));
        ArrayAdapter<String> adapter = new ArrayAdapter<String> (
        		getApplicationContext(),
        		R.layout.entry_layout,
        		myArrayList);
        resultPanel.setAdapter(adapter);
        resultPanel.invalidate();
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
            loadDictionaryDir();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    void loadDictionaryDir() {
    	String state = Environment.getExternalStorageState();

    	if (Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
    	    // We can only read the media
    	} else {
    	    // Something else is wrong. It may be one of many other states, but all we need
    	    //  to know is we can neither read nor write
    	    // TODO(awong): Show error.
    	    return;
    	}
    	
    	File data_dir = Environment.getExternalStorageDirectory();
    	File[] files = data_dir.listFiles(new FilenameFilter(){
    		public boolean accept(File dir, String s) {
    			if (s.endsWith(".u8")) {
    				return true;
    			} 
    			
    			return false;
    		}
    		});
    	for (File dict : files) {
    		if (dict.canRead()) {
    			loadDictionary(dict);
    		} else {
    			// TODO(awong): List error.
    		}
    	}
    }
	static enum EntrySection {
		 TRAD,
		 SIMP,
		 CANT,
		 MAND,
		 DEFN
		};
		
    void loadDictionary(File dict) {
    	try {
			SQLiteDatabase db = dictionaryOpenHelper.getWritableDatabase();
			db.beginTransaction();
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(new FileInputStream(dict), "UTF-8"));
			String line = null;
			while ((line = reader.readLine()) != null) {
				StringBuilder buf = new StringBuilder(1024);
				// Example line: 喬 乔 [kiu4] [qiao2] /surname Qiao/tall/
				EntrySection section = EntrySection.TRAD;
				ContentValues entry = new ContentValues();
				String[] defn = null;
				for (int i = 0; i < line.length(); i++) {
					char ch = line.charAt(i);
					switch(section) {
					case TRAD:
						if (ch == ' ') {
							entry.put(section.toString(), buf.toString());
							buf.setLength(0);
							section = EntrySection.SIMP;
						} else {
							buf.append(ch);
						}
						break;
					case SIMP:
						if (ch == ' ') {
							entry.put(section.toString(), buf.toString());
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
							entry.put(section.toString(), buf.toString());
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
							entry.put(section.toString(), buf.toString());
							buf.setLength(0);
							section = EntrySection.DEFN;
						} else {
							buf.append(ch);
						}
						break;
					case DEFN:
						defn = line.substring(i).split("/");
						break;
					}
				}

				long row_id = db.insert("Entries", null, entry);
				for (String d: defn) {
					ContentValues definition = new ContentValues();
					definition.put("entry_id", row_id);
					definition.put(EntrySection.DEFN.toString(), d);
					db.insert("Definitions", null, definition);
				}
			}
			
			db.endTransaction();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    public static class DictionaryOpenHelper extends SQLiteOpenHelper {
        private static final int DATABASE_VERSION = 1;

        DictionaryOpenHelper(String name, Context context) {
            super(context, name, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
        	// Entries table has all the basic words + phonetics.
            db.execSQL("CREATE TABLE Entries (entry_id INTEGER PRIMARY KEY, "
        	   + EntrySection.TRAD + " TEXT, " 
        	   + EntrySection.SIMP + " TEXT, "
        	   + EntrySection.CANT + " TEXT, " 
        	   + EntrySection.MAND + " TEXT);");
        	// Definitions table has one definition per row. Within each entry_id, all the def_ids should have a unique
            // sort_order.  Sort ascending.
            db.execSQL("CREATE TABLE Definitions (defn_id INTEGER PRIMARY KEY, entry_id INTEGER NOT NULL, sort_order INTEGER, "
            		+ EntrySection.DEFN + " TEXT, FOREIGN KEY(entry_id) REFERENCES Entries(entry_id));");
		}

		@Override
		public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
			// TODO Auto-generated method stub
			
		}
    }
}