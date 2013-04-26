package awongdev.android.cedict.database;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

class DictionaryDatabaseOpenHelper extends SQLiteOpenHelper {
	private static final String LOG_TAG = DictionaryDatabaseOpenHelper.class.getCanonicalName();
	
	// Note, when creating the database to load here, remember to add the android metadata table:
	//  CREATE TABLE "android_metadata" ("locale" TEXT DEFAULT 'en_US');
	//  INSERT INTO "android_metadata" VALUES ('en_US');
	// This only affects COLLATED sql statements which we don't use so en_US is fine.
	
	// Version number history:
	//  1 == no clue
	//  2 == includes annotations database
	//  3 == only the FlattenedEntries table.
    private static final int DATABASE_VERSION = 3;

    DictionaryDatabaseOpenHelper(String name, Context context) {
        super(context, name, null, DATABASE_VERSION);
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
    	Log.e(LOG_TAG, "onCreate");
        db.beginTransaction();
        try {
            DictionaryUpdater updater = new DictionaryUpdater(db);
            updater.createSchema();
	        db.setTransactionSuccessful();
        } catch (RuntimeException e) {
        	Log.e(LOG_TAG, "Failed creating schmea. Continiuing anyways hopig we don't crash.", e);
        } finally {
          db.endTransaction();
        }
    }

	@Override
	public void onUpgrade(SQLiteDatabase db, int from_version, int to_version) {
		if (from_version == 2 && to_version == 3) {
	        db.beginTransaction();
	        try {
	        	db.execSQL("DROP TABLE Annotations;");
		        db.setTransactionSuccessful();
	        } catch (RuntimeException e) {
	        	Log.e(LOG_TAG, "Failed to drop table. Non-critical. Ignoring.", e);
	        } finally {
	          db.endTransaction();
	        }
		}
	}
}