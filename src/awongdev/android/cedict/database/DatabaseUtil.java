package awongdev.android.cedict.database;

import java.io.File;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Environment;

public class DatabaseUtil {
	private static File databaseDir;
	
	@TargetApi(Build.VERSION_CODES.FROYO)
	public static File getDatabaseDir(Context context) {
		if (databaseDir == null) {
			String storagePath;
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
				storagePath = Environment.getExternalStorageDirectory().getAbsolutePath()
						+ "/Android/data/" + context.getPackageName();
			} else {
				storagePath = context.getExternalFilesDir(null).getAbsolutePath();
			}
			databaseDir = new File(storagePath + "/databases/");
			databaseDir.mkdirs();
		}
		return databaseDir;
	}
}
