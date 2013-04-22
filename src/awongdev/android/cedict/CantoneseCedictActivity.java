package awongdev.android.cedict;


import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ListView;
import awongdev.android.cedict.R;
import awongdev.android.cedict.database.DictionaryTaskManager;
import awongdev.android.cedict.database.DictionaryTaskManager.DetailedProgress;
import awongdev.android.cedict.database.DictionaryTaskManager.TaskProgressListener;

public class CantoneseCedictActivity extends Activity {
	private DictionaryTaskManager dictionaryTaskManager;
	private Handler handler;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		handler = new Handler();
		// TODO(awong): We should eagerly create the DTM and then have it attach its dictionary later.
		DictionaryTaskManager.asyncCreate(new DTMInitListener(), getApplicationContext(), handler);
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
			dictionaryTaskManager.doDownloadLatestDictionary(new DictionaryDownloadListener());
			return true;
		case R.id.FSLoad:
			//downloadingDialog = ProgressDialog.show(this, "", "Loading...");
			
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
		
	private class DTMInitListener implements TaskProgressListener<String, DictionaryTaskManager> {
		private ProgressDialog slowInitDialog;
		private String lastStatus;
		private boolean finished = false;
		
		public void onBegin(String initialStatus) {
			lastStatus = initialStatus;
			handler.postDelayed(new Runnable() {
				public void run() {
					createSlowInitDialog();
				}
			}, 500);
		}
		
		public void onProgress(String status) {
			lastStatus = status;
			if (slowInitDialog != null) {
				slowInitDialog.setMessage(lastStatus);
			}
			
		}
		
		public void onComplete(DictionaryTaskManager dtm) {
			finished = true;
			if (slowInitDialog != null) {
				slowInitDialog.dismiss();
				slowInitDialog = null;
			}
			dictionaryTaskManager = dtm;
			
			// LIST VIEW
			setContentView(R.layout.main);
			EditText searchBox = (EditText) findViewById(R.id.SearchBox);
			searchBox.addTextChangedListener(
					new SearchBoxHandler(
							getApplicationContext(), 
							(ListView)findViewById(R.id.ResultPanel), 
							dictionaryTaskManager));		
		}
	
		private void createSlowInitDialog() {
			if (!finished) {
				slowInitDialog = ProgressDialog.show(CantoneseCedictActivity.this, "", lastStatus);
			}
		}
	}
	
	private class DictionaryDownloadListener implements TaskProgressListener<DetailedProgress, Void> {
		private ProgressDialog downloadDialog;
		private int currentStyle = -1;

		public void onBegin(DetailedProgress initialProgress) {			
            displayProgress(initialProgress);
		}

		private boolean ensureRightDialog(int style) {
			if (currentStyle == style) {
				return false;
			}
			currentStyle = style;
			if (downloadDialog != null) {
				downloadDialog.dismiss();
			}
			
			downloadDialog = new ProgressDialog(CantoneseCedictActivity.this);
            downloadDialog.setTitle("Update Dictionary");
	        downloadDialog.setProgressStyle(currentStyle);
	        
	        return true;
		}

		public void onProgress(DetailedProgress progress) {
			displayProgress(progress);
		}

		public void onComplete(Void dtm) {
			downloadDialog.dismiss();
			downloadDialog = null;
		}

		private void displayProgress(DetailedProgress currentProgress) {
			boolean needsShow = false;
		    if (currentProgress.progress != -1) {
		    	needsShow = ensureRightDialog(ProgressDialog.STYLE_HORIZONTAL);
		    	downloadDialog.setProgress(currentProgress.progress);
		    	downloadDialog.setMax(currentProgress.max);
		    } else {
		    	needsShow = ensureRightDialog(ProgressDialog.STYLE_SPINNER);
		    }
			downloadDialog.setMessage(currentProgress.status);
			
		    if (needsShow) {
		    	downloadDialog.show();
		    }
		}
	}
}