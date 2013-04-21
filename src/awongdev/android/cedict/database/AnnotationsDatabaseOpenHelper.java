package awongdev.android.cedict.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class AnnotationsDatabaseOpenHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;

    AnnotationsDatabaseOpenHelper(String name, Context context) {
        super(context, name, null, DATABASE_VERSION);
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.beginTransaction();
        try {
    		db.execSQL("CREATE TABLE Annotations (entry TEXT NOT NULL"
    				+ ", num_lookups INTEGER NOT NULL"
    				+ ", last_lookup INTEGER NOT NULL" + ", star INTEGER"
    				+ ", UNIQUE (entry)" + ")");
	        db.setTransactionSuccessful();
        } finally {
          db.endTransaction();
        }
    }

	@Override
	public void onConfigure(SQLiteDatabase db) {
		db.execSQL("PRAGMA read_uncommitted = true;");
		db.execSQL("PRAGMA synchronous = OFF;");
		db.execSQL("PRAGMA count_changes = OFF;");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}
}
