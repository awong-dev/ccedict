package awongdev.android.cedict;

/**
 * TODO:
 *   - RSA signature
 *   - Ping periodically on updated dictionary.
 *   - DTM does too many bounces for creating loader. People should have
 *     access to DTM directly which means it shouldn't lazy load.
 */

import android.support.v4.app.FragmentActivity;
import android.support.v4.content.Loader;
import android.app.ProgressDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import awongdev.android.cedict.R;
import awongdev.android.cedict.database.DictionaryTaskManager;
import awongdev.android.cedict.database.DictionaryTaskManager.DetailedProgress;
import awongdev.android.cedict.database.DictionaryTaskManager.TaskProgressListener;

public class CantoneseCedictActivity extends FragmentActivity {
	Handler handler;
	private CantoneseCedictApplication applicationContext;
	private DictionaryTaskManager dictionaryTaskManager;
	private DictionaryDownloadListener downloadListener;
	private DTMInitListener dtmInitListener;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		applicationContext = (CantoneseCedictApplication) getApplicationContext();
		downloadListener = new DictionaryDownloadListener();
		dtmInitListener = new DTMInitListener();
		handler = applicationContext.getHandler();
		applicationContext.attachDownloadListener(downloadListener);
		applicationContext.attachDtmListener(dtmInitListener);
	}
	
	@Override
	protected void onDestroy() {
		applicationContext.detachDownloadListener(downloadListener);
		applicationContext.detachDtmListener(dtmInitListener);
		downloadListener = null;
		dtmInitListener = null;
		super.onDestroy();
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
		case R.id.UpdateDictionary:
			applicationContext.updateDictionary();
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	private class DictionaryDownloadListener implements TaskProgressListener<DetailedProgress, Void> {
		private static final int INVALID_STYLE = -1;
		private ProgressDialog downloadDialog;
		private int currentStyle = INVALID_STYLE;

		public void onBegin(DetailedProgress initialProgress) {
			currentStyle = INVALID_STYLE;
	        displayProgress(initialProgress);
		}

		public void onProgress(DetailedProgress progress) {
			displayProgress(progress);
		}

		public void onComplete(Void dtm) {
			if (downloadDialog != null) {
				downloadDialog.dismiss();
				downloadDialog = null;
			}
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
		    downloadDialog.setCancelable(false);
		    
		    return true;
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
			
			// LIST VIEW
			setContentView(R.layout.activity_layout);
			dictionaryTaskManager = dtm;
			
			// Remove ourselves so we can GCed.
			applicationContext.detachDtmListener(this);
			dtmInitListener = null;
		}

		private void createSlowInitDialog() {
			if (!finished) {
				slowInitDialog = ProgressDialog.show(CantoneseCedictActivity.this, "", lastStatus);
			}
		}
	}

	public Loader<Cursor> createLookupStatsLoader() {
		return dictionaryTaskManager.createLookupStatsLoader();
	}

	public Loader<Cursor> createLookupTermLoader(String term, boolean is_roman) {
		return dictionaryTaskManager.createLookupTermLoader(term, is_roman);
	}
}