package awongdev.android.cedict;

import java.io.File;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.os.Environment;
import android.util.Log;

public class CantoneseCedictApplication extends Application {
	final File databaseDir = new File(Environment.getExternalStorageDirectory().getPath() +
			"/Android/data/awongdev.android.ccedict/databases/");
	@Override
	public File getDatabasePath(String name) {
		databaseDir.mkdirs();
		return new File(databaseDir, name);
	}
	
	@Override
	public SQLiteDatabase openOrCreateDatabase(String name, int mode, CursorFactory factory) {
		File path = getDatabasePath(name);
		Log.e("Database", path.getAbsolutePath());
	    return SQLiteDatabase.openOrCreateDatabase(path, factory);
	}
}
