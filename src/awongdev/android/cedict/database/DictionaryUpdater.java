package awongdev.android.cedict.database;

import android.database.sqlite.SQLiteDatabase;

class DictionaryUpdater{
	DictionaryUpdater(SQLiteDatabase db) {
	}
	
	static void createSchema(SQLiteDatabase database) {
		database.execSQL("CREATE TABLE FlattenedEntries (entry TEXT NOT NULL"
				+ ", simplified TEXT"
				+ ", variant TEXT"
				+ ", trust INTEGER NOT NULL"
				+ ", cantonese TEXT"
				+ ", pinyin TEXT"
				+ ", definition TEXT"
				+ ", extra_search TEXT"
				+ ", UNIQUE (entry)"
				+ ");");
		database.execSQL("CREATE INDEX FlattenedEntries_extra_search on FlattenedEntries (extra_search);");
		database.execSQL("CREATE INDEX FlattenedEntries_pinyin on FlattenedEntries (pinyin);");
		database.execSQL("CREATE INDEX FlattenedEntries_simplified on FlattenedEntries (simplified);");
		database.execSQL("CREATE INDEX FlattenedEntries_variant on FlattenedEntries (variant);");
	}

	/*
	void updateFromCcedict(String sourceId, BufferedReader reader) throws IOException {
		database.beginTransaction();
		try {
			updateFromReaderInternal(sourceId, reader);
			database.setTransactionSuccessful();
		} finally {
			database.endTransaction();
		}
	}
	
	void replaceWithSqlDump(BufferedReader reader) throws IOException {
		try {
			database.beginTransaction();
			try { database.execSQL("DROP TABLE FlattenedEntries;"); } catch (SQLException e) {}
			try { database.execSQL("DROP TABLE Entries;"); } catch (SQLException e) {}
			String line = null;
			while ((line = reader.readLine()) != null) {
				database.execSQL(line);
			}
			database.setTransactionSuccessful();
		} finally {
			database.endTransaction();
		}
	}
	
	private void upsertDefinitions(long source_id, long entry_id,
			ArrayList<ArrayList<String>> definitions) {
		database.delete("Definitions", "entry_id = ? and source_id = ?",
				new String[] {Long.toString(entry_id), Long.toString(source_id)});
		for (int major = 0; major < definitions.size(); ++major) {
			ArrayList<String> minor_definitions = definitions.get(major);
			for (int minor = 0; minor < minor_definitions.size(); ++minor) {
				ContentValues definition_entry = new ContentValues();
				definition_entry.put("entry_id", entry_id);
				definition_entry.put("source_id", source_id);
				definition_entry.put("major_id", major);
				definition_entry.put("minor_id", minor);
				definition_entry.put("definition", minor_definitions.get(minor));
				database.insertOrThrow("Definitions", null, definition_entry);
			}
		}
	}

	private void upsertPinyin(long source_id, long entry_id,
			ArrayList<String> pinyin) {
		database.delete("Pinyin", "entry_id = ? and source_id = ?",
				new String[] {Long.toString(entry_id), Long.toString(source_id)});
		for (int sort_order = 0; sort_order < pinyin.size(); ++sort_order) {
			ContentValues definition_entry = new ContentValues();
			definition_entry.put("entry_id", entry_id);
			definition_entry.put("source_id", source_id);
			definition_entry.put("sort_order", sort_order);
			definition_entry.put("pinyin", pinyin.get(sort_order));
			database.insertOrThrow("Pinyin", null, definition_entry);
		}
	}

	private void upsertJyutping(long source_id, long entry_id,
			ArrayList<String> jyutping) {
		database.delete("Jyutping", "entry_id = ? and source_id = ?",
				new String[] {Long.toString(entry_id), Long.toString(source_id)});
		for (int sort_order = 0; sort_order < jyutping.size(); ++sort_order) {
			ContentValues definition_entry = new ContentValues();
			definition_entry.put("entry_id", entry_id);
			definition_entry.put("source_id", source_id);
			definition_entry.put("sort_order", sort_order);
			definition_entry.put("jyutping", jyutping.get(sort_order));
			database.insertOrThrow("Jyutping", null, definition_entry);
		}
	}

	private void upsertEntry(long source_id, String entry,
			ArrayList<ArrayList<String>> definitions, ArrayList<String> pinyin, ArrayList<String> jyutping) {
		assert(database.inTransaction());
		Cursor cursor = database.query("Entries", new String[] {"entry_id"}, "entry = ?", 
				new String[] { entry }, null, null, null);
		cursor.moveToFirst();
		if (cursor.getCount() == 0) {
			cursor.close();
			// Create it, and then find it again.
			ContentValues new_entry = new ContentValues();
			new_entry.put("entry", entry);
			database.insertOrThrow("Entries", null, new_entry);
			cursor = database.query("Entries", new String[] {"entry_id"}, "entry = ?", 
					new String[] { entry }, null, null, null);
			cursor.moveToFirst();
		}
		
		long entry_id = cursor.getLong(0);
		cursor.close();

		upsertDefinitions(source_id, entry_id, definitions);
		upsertPinyin(source_id, entry_id, pinyin);
		upsertJyutping(source_id, entry_id, jyutping);
	}
	
	// If the name is unknown, returns -1.
	private long sourceIdForName(String name) {
		Cursor cursor = database.query("Sources", new String[] {"source_id"}, "source_name = ?",
				new String[] { name }, null, null, null);
		cursor.moveToFirst();
		// source_id is NOT NULL PK.  No need to check column count.
		long source_id = -1;
		if (cursor.getCount() != 0) {
			source_id = cursor.getLong(0);
		}
		cursor.close();
		
		return source_id;
	}
	
	private static enum TrustLevel {
		FULL_TRUST(1),
		PARTIAL_TRUST(2),
		DISTRUST(3),
		UNKNOWN_TRUST(4);
		
		private final int db_value;
		public int db_value() { return db_value; }
		
		TrustLevel(int db_value) {
			this.db_value = db_value;
		}
	}
	
	private long addSource(String name, TrustLevel trust) {
		Cursor cursor = database.query("Sources", new String[] {"max(priority)"}, "priority not in (1999999, 999999)",
				null, null, null, null);
		cursor.moveToFirst();
		long max_priority = 0;
		if (cursor.getCount() != 0 && cursor.getColumnCount() != 0) {
			max_priority = cursor.getLong(0) + 1;
		}
		cursor.close();
		return addSource(name, max_priority, trust);
	}

	private long addSource(String name, long priority, TrustLevel trust) {
		ContentValues source_entry = new ContentValues();
		source_entry.put("source_name", name);
		source_entry.put("priority", priority);
		source_entry.put("trust", trust.db_value());
		database.insertOrThrow("Sources", null, source_entry);

		Cursor cursor = database.query("Sources", new String[] {"source_id"}, "source_name = ?",
				new String[] { name }, null, null, null);
		cursor.moveToFirst();
		long source_id = cursor.getLong(0); 
		cursor.close();
		return source_id;
	}
	
	private void addSource(String name, long priority, TrustLevel trust, int source_id) {
		ContentValues source_entry = new ContentValues();
		source_entry.put("source_id", source_id);
		source_entry.put("source_name", name);
		source_entry.put("priority", priority);
		source_entry.put("trust", trust.db_value());
		database.insertOrThrow("Sources", null, source_entry);
	}
	
	enum EntrySection {
		TRAD, SIMP, CANT, MAND, DEFN
	}
	private void updateFromReaderInternal(String sourceId, BufferedReader reader) throws IOException {
		String line = null;
		
		long source_id = sourceIdForName(sourceId);
		if (source_id == -1) {
			source_id = addSource(sourceId, DictionaryUpdater.TrustLevel.FULL_TRUST);
		}
	
		while ((line = reader.readLine()) != null) {
			StringBuilder buf = new StringBuilder(1024);
			String traditional = null;
			String simplified = null;
			ArrayList<String> jyutping = new ArrayList<String>();
			ArrayList<String> pinyin = new ArrayList<String>();
			ArrayList<ArrayList<String>> definitions = new ArrayList<ArrayList<String>>();
	
			// Example line: ??�?[kiu4] [qiao2] /surname Qiao/tall/
			EntrySection section = EntrySection.TRAD;
	
			line_done: for (int i = 0; i < line.length(); i++) {
				char ch = line.charAt(i);
				switch (section) {
				case TRAD:
					if (ch == ' ') {
						traditional = buf.toString();
						buf.setLength(0);
						section = EntrySection.SIMP;
					} else {
						buf.append(ch);
					}
					break;
				case SIMP:
					if (ch == ' ') {
						simplified = buf.toString();
						buf.setLength(0);
						section = EntrySection.CANT;
					} else {
						buf.append(ch);
					}
					break;
				case CANT:
					if (ch == '[') {
						// ignore
					} else if (ch == ']') {
						for (String s : buf.toString().split("\\|")) {
							s = s.trim();
							if (s.length() != 0) {
								jyutping.add(s);
							}
						}
						buf.setLength(0);
						section = EntrySection.MAND;
					} else {
						buf.append(ch);
					}
					break;
				case MAND:
					if (ch == '[') {
						// ignore
					} else if (ch == ']') {
						for (String s : buf.toString().split("\\|")) {
							s = s.trim();
							if (s.trim().length() != 0) {
								pinyin.add(s);
							}
						}
						buf.setLength(0);
						section = EntrySection.DEFN;
					} else {
						buf.append(ch);
					}
					break;
				case DEFN:
					for (String single_definition : line.substring(i).split("/")) {
						single_definition = single_definition.trim();
						if (single_definition.length() == 0) {
							continue;
						}
						ArrayList<String> major_def = new ArrayList<String>();
						major_def.add(single_definition);
						definitions.add(major_def);
	
					}
					break line_done;
				}
			}
			upsertEntry(source_id, traditional, definitions, pinyin, jyutping);
			upsertEntry(source_id, simplified, definitions, pinyin, jyutping);
		}
	}*/

}