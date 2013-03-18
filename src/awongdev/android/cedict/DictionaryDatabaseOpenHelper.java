package awongdev.android.cedict;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DictionaryDatabaseOpenHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 2;

    DictionaryDatabaseOpenHelper(String name, Context context) {
        super(context, name, null, DATABASE_VERSION);
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            DictionaryUpdater updater = new DictionaryUpdater(db);
            updater.createSchema();
	        db.setTransactionSuccessful();
        } finally {
          db.endTransaction();
        }
    }

	@Override
	public void onUpgrade(SQLiteDatabase db, int from_version, int to_version) {
		if (from_version <= 1 && to_version > 1) {
	        db.beginTransaction();
	        try {
	            DictionaryUpdater updater = new DictionaryUpdater(db);
	            updater.createSchema();
		        db.setTransactionSuccessful();
	        } finally {
	          db.endTransaction();
	        }
		}
	}
}