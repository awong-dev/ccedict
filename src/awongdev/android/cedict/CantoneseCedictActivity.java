package awongdev.android.cedict;


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
import awongdev.android.cedict.database.DictionaryTaskManager;
import awongdev.android.cedict.database.DictionaryTaskManager.DictionaryTaskManagerInitListener;

public class CantoneseCedictActivity extends Activity implements DictionaryTaskManagerInitListener {
	private static final int DOWNLOADING_DIALOG_ID = 0;
	private static final int INITIALIZING_DIALOG_ID = 1;
	private DictionaryTaskManager dictionaryTaskManager;
	private ProgressDialog downloadingDialog;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Show something.
		setContentView(R.layout.main);
		DictionaryTaskManager.asyncCreate(this, getApplicationContext());
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
			new DownloadDatabaseTask(this, dictionaryTaskManager).execute();
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

	public void onInitialized(DictionaryTaskManager dtm) {
		dictionaryTaskManager = dtm;
		
		// LIST VIEW
		EditText searchBox = (EditText) findViewById(R.id.SearchBox);
		searchBox.addTextChangedListener(
				new SearchBoxHandler(
						getApplicationContext(), 
						(ListView)findViewById(R.id.ResultPanel), 
						dictionaryTaskManager));		
	}

	public void showInitializing() {
		showDialog(INITIALIZING_DIALOG_ID);
	}
}