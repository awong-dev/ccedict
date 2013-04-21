package awongdev.android.cedict;


import java.io.File;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ListView;
import awongdev.android.cedict.R;
import awongdev.android.cedict.database.Dictionary;
import awongdev.android.cedict.database.DownloadDatabaseTask;
import awongdev.android.cedict.database.InitializeDatabasesTask;
import awongdev.android.cedict.database.OverwriteDictionaryTask;

public class CantoneseCedictActivity extends Activity {
	private static final int DOWNLOADING_DIALOG_ID = 0;
	private static final int INITIALIZING_DIALOG_ID = 1;
	Dictionary dictionary;
	private ProgressDialog downloadingDialog;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Show something.
		setContentView(R.layout.main);
		
		new InitializeDatabasesTask(this).execute(getApplicationContext());
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
		case R.id.Download:
			new DownloadDatabaseTask(this).execute();
			return true;
		case R.id.FSLoad:
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public Dialog onCreateDialog(int id) {
		switch(id) {
		case DOWNLOADING_DIALOG_ID:
		  downloadingDialog = ProgressDialog.show(this, "", "Loading...");
		  return downloadingDialog;
		default:
			return null;
		}
	}
	
	public void showDownloadingDialog() {
		if (downloadingDialog != null) {
			downloadingDialog.dismiss();
			downloadingDialog = null;
		}
	}

	public void assignDictionary(Dictionary d) {
		dictionary = d;
		
		// LIST VIEW
		EditText searchBox = (EditText) findViewById(R.id.SearchBox);
		searchBox.addTextChangedListener(
				new SearchBoxHandler(
						getApplicationContext(), 
						(ListView)findViewById(R.id.ResultPanel), 
						dictionary));		
	}

	public void showInitializing() {
		showDialog(INITIALIZING_DIALOG_ID);
	}

	public void loadNewDictionary(final File newDictionaryPath) {
		new OverwriteDictionaryTask(newDictionaryPath).execute(dictionary);
	}
}