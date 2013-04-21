package awongdev.android.cedict;

import java.util.regex.Pattern;

import android.content.Context;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ListView;
import awongdev.android.cedict.database.Dictionary;

final class SearchBoxHandler implements TextWatcher {
	private static final Pattern ALPHA_NUM = Pattern.compile("^[a-zA-Z0-9 ]+$");
	private final Context applicationContext;
	private final ListView resultPanel;
	private final Dictionary dictionary;
	private IncrementLookup outstandingIncrement = null;
	
	public SearchBoxHandler(Context applicationContext, ListView resultPanel, Dictionary dictionary) {
		this.applicationContext = applicationContext;
		this.resultPanel = resultPanel;
		this.dictionary = dictionary;
	}
	
	public void afterTextChanged(Editable s) {
		final String term = s.toString().trim();
		if (outstandingIncrement != null) {
			outstandingIncrement.cancel(true);
			outstandingIncrement = null;
		}
		if (term.length() == 0)
			return;
		
		// Special keyword to go to flashcard mode.
		if (term.equals("hs")) {
			new FlashCardTask(applicationContext, resultPanel, dictionary).execute();
			return;
		}
		boolean is_roman = false;
		if (ALPHA_NUM.matcher(term).find()) {
			// We need 2 characters to bother with a romanization lookup.
			if (s.length() < 2)
				return;
			is_roman = true;
		}
		new LookupTask(applicationContext, is_roman, resultPanel, dictionary).execute(term);
		if (!is_roman) {
			// Increment lookup frequency.
			outstandingIncrement = new IncrementLookup(dictionary);
			Handler handler = new Handler();
			handler.postDelayed(new Runnable() {
				final IncrementLookup existing_task = outstandingIncrement;

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
}