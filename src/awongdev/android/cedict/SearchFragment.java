package awongdev.android.cedict;

import java.util.regex.Pattern;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;

public class SearchFragment extends Fragment implements LoaderCallbacks<Cursor> {
	private CantoneseCedictActivity activity;
	private SimpleCursorAdapter currentAdapter;
	private SimpleCursorAdapter statsAdapter;
	private SimpleCursorAdapter termAdapter;
	
	private static final int[] STATS_TO_FIELDS = new int[] {
		R.id.entry, R.id.variant, R.id.cantonese
	};
	private static final String[] STATS_VIEW_COLUMNS = 
			new String[] {
		"entry", "last_lookup", "num_lookups"
	};
		
	
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (CantoneseCedictActivity)activity;
    }
    
    @Override
	public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	statsAdapter = new SimpleCursorAdapter(
    			getActivity(),
    			R.layout.entry_layout,
    			null,
    			new String[] { "entry", "last_lookup", "num_lookups" },
    			new int[] { R.id.entry, R.id.variant, R.id.cantonese },
                0);
    	termAdapter = new SimpleCursorAdapter(
    			getActivity(),
    			R.layout.entry_layout,
    			null,
    			new String[] { "entry", "variant", "cantonese",
    				"pinyin", "definition" },
    			new int[] { R.id.entry, R.id.variant, R.id.cantonese,
    				R.id.pinyin, R.id.definition },
                0);
    	currentAdapter = termAdapter;
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	View searchView = inflater.inflate(R.layout.search_fragment, container, false);
		EditText searchBox = (EditText) searchView.findViewById(R.id.SearchBox);
		searchBox.addTextChangedListener(
				new SearchBoxHandler());
		return searchView;
    }
	
	@Override
	public void onDetach() {
		activity = null;
		super.onDetach();
	}
	
	private static final Pattern ALPHA_NUM = Pattern.compile("^[a-zA-Z0-9 ]+$");
	public static final int STATS = 0;
	public static final int TERM = 1;
	final class SearchBoxHandler implements TextWatcher {
		private final Bundle bundle;
		
		public SearchBoxHandler() {
			this.bundle = new Bundle();
		}
		
		public void afterTextChanged(Editable s) {
			final String term = s.toString().trim();
			if (term.length() == 0)
				return;
			
			// Special keyword to go to stats mode.
			if (term.equals("hs")) {
				getLoaderManager().restartLoader(STATS, bundle, SearchFragment.this);
				return;
			}
			boolean is_roman = false;
			if (ALPHA_NUM.matcher(term).find()) {
				// We need 2 characters to bother with a romanization lookup.
				if (s.length() < 2)
					return;
				is_roman = true;
			}
			bundle.putBoolean("isRoman", is_roman);
			bundle.putString("term", term);
			getLoaderManager().restartLoader(TERM, bundle, SearchFragment.this);
		}

		public void beforeTextChanged(CharSequence arg0, int arg1,
				int arg2, int arg3) {
		}

		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
		}
	}

	public Loader<Cursor> onCreateLoader(int lookupType, Bundle bundle) {
		try {
			switch (lookupType) {
				case STATS:
					currentAdapter = statsAdapter;
					return activity.createLookupStatsLoader();
					
				case TERM:
					currentAdapter = termAdapter;
					return activity.createLookupTermLoader(
							bundle.getString("term"),
							bundle.getBoolean("isRoman"));
			}
		} finally {
			ListFragment results = (ListFragment)getFragmentManager().findFragmentById(R.id.ResultPanel);
			results.setListAdapter(currentAdapter);
		}
		return null;
	}

	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
	    currentAdapter.swapCursor(data);
	}

	public void onLoaderReset(Loader<Cursor> loader) {
		currentAdapter.swapCursor(null);
	}
}
