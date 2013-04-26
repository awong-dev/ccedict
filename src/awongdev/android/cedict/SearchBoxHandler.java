package awongdev.android.cedict;

import java.util.regex.Pattern;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ListView;

final class SearchBoxHandler implements TextWatcher {
	private static final Pattern ALPHA_NUM = Pattern.compile("^[a-zA-Z0-9 ]+$");
	private final ListView resultPanel;
	private final CantoneseCedictActivity activity;
	
	public SearchBoxHandler(ListView resultPanel, CantoneseCedictActivity activity) {
		this.resultPanel = resultPanel;
		this.activity = activity;
	}
	
	public void afterTextChanged(Editable s) {
		final String term = s.toString().trim();
		if (term.length() == 0)
			return;
		
		// Special keyword to go to stats mode.
		if (term.equals("hs")) {
			activity.doStatsLookup(resultPanel);
			return;
		}
		boolean is_roman = false;
		if (ALPHA_NUM.matcher(term).find()) {
			// We need 2 characters to bother with a romanization lookup.
			if (s.length() < 2)
				return;
			is_roman = true;
		}
		activity.doLookup(term, is_roman, resultPanel);
	}

	public void beforeTextChanged(CharSequence arg0, int arg1,
			int arg2, int arg3) {
	}

	public void onTextChanged(CharSequence s, int start, int before,
			int count) {
	}
}