package awongdev.android.cedict.database;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class CountingInputStream extends FilterInputStream {
	private long bytesRead = 0; 

	public CountingInputStream(InputStream in) {
		super(in);
	}
	
	@Override
	public int read() throws IOException {
		bytesRead++;
		return super.read();
	}

	@Override
	public int read(byte[] buffer, int offset, int count) throws IOException {
		int amtRead = super.read(buffer, offset, count); 
		bytesRead += amtRead;
		return amtRead;
	}

	@Override
	public long skip(long byteCount) throws IOException {
		long amtRead = super.skip(byteCount); 
		bytesRead += amtRead;
		return amtRead;
	}

	public long getBytesRead() {
		return bytesRead;
	}
}
