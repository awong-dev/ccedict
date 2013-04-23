package awongdev.android.cedict;

import android.app.Application;
import android.os.Build;

public class CantoneseCedictApplication extends Application {
	public CantoneseCedictApplication() {
		super();
		// Work around pre-Froyo bugs in HTTP connection reuse.
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
			System.setProperty("http.keepAlive", "false");
		}
	}
}
