package awongdev.android.cedict;

import java.io.File;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.os.Build;
import android.util.Log;
import awongdev.android.cedict.database.DatabaseUtil;

public class CantoneseCedictApplication extends Application {
	public CantoneseCedictApplication() {
		super();
		// Work around pre-Froyo bugs in HTTP connection reuse.
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
			System.setProperty("http.keepAlive", "false");
		}
	}
	
	@Override
	public synchronized File getDatabasePath(String name) {
		
		return new File(DatabaseUtil.getDatabaseDir(this), name);
	}
	
	@Override
	public synchronized SQLiteDatabase openOrCreateDatabase(String name, int mode, CursorFactory factory) {
		File path = getDatabasePath(name);
		Log.v("Database", path.getAbsolutePath());
	    return SQLiteDatabase.openOrCreateDatabase(path, factory);
	}
}
