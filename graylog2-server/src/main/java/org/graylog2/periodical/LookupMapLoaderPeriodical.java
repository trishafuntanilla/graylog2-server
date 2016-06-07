package org.graylog2.periodical;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.graylog2.database.MongoConnection;
import org.graylog2.lookup.LookupDataMap;
import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

/**
 * Reloads the lookup data map from MongoDB periodically.
 * 
 * @author tfuntani
 *
 */
public class LookupMapLoaderPeriodical extends Periodical {

	private static final Logger LOG = LoggerFactory.getLogger(LookupMapLoaderPeriodical.class);

	private static final String LOOKUP = "lookup";
	private static final String KEY = "key";
	private static final String VALUE = "value";
	private static final String MAPPINGS = "mappings";

	private final MongoConnection mongoConnection;

	@Inject
	public LookupMapLoaderPeriodical(MongoConnection mongoConnection) {
		this.mongoConnection = mongoConnection;
	}

	@Override
	public boolean runsForever() {
		return false;
	}

	@Override
	public boolean stopOnGracefulShutdown() {
		return true;
	}

	@Override
	public boolean masterOnly() {
		return false;
	}

	@Override
	public boolean startOnThisNode() {
		return true;
	}

	@Override
	public boolean isDaemon() {
		return false;
	}

	@Override
	public int getInitialDelaySeconds() {
		return 0;
	}

	@Override
	public int getPeriodSeconds() {
		// run every 4 hours
		return 14400;
	}

	@Override
	protected Logger getLogger() {
		return LOG;
	}

	@Override
	public synchronized void doRun() {

		ConcurrentHashMap<Pair<String, String>, Map<String, String>> reloadMap = new ConcurrentHashMap<Pair<String, String>, Map<String, String>>();

		if (!mongoConnection.getDatabase().collectionExists(LOOKUP)) {

			LOG.info("Lookup collection does not exist. Creating lookup collection...");
			DBObject options = BasicDBObjectBuilder.start().add("capped", false).get();
			mongoConnection.getDatabase().createCollection(LOOKUP, options);

		} else {

			try {
				DBCollection collection = mongoConnection.getDatabase().getCollection(LOOKUP);
				DBCursor cursor = collection.find();
				while (cursor.hasNext()) {							
					DBObject doc = cursor.next();

					JsonParser parser = new JsonParser();
					JsonElement je = parser.parse(doc.toString());
					JsonObject json = je.getAsJsonObject();

					String key = json.get(KEY).getAsString();
					String value = json.get(VALUE).getAsString();
					Pair<String, String> kvPair = new ImmutablePair<String, String>(key, value);

					JsonArray mappings = json.getAsJsonArray(MAPPINGS);
					JsonElement mappingsObject = mappings.get(0);	
					JsonObject map = mappingsObject.getAsJsonObject();

					LOG.info("Adding <" + key + "> : <" + value + "> pair mappings...");

					Map<String, String> dm = new HashMap<String, String>();
					for (Map.Entry<String, JsonElement> entry : map.entrySet()) {
						dm.put(entry.getKey(), entry.getValue().getAsString());
					}

					LOG.info("Data map for <" + key + "> : <" + value + "> pair has " + dm.size() + " mappings.");

					// compare: if lookup data map doesn't already have this key-value pair, put it
					if (!LookupDataMap.dataMap.containsKey(kvPair)) {
						LookupDataMap.dataMap.put(kvPair, dm);
					}

					// place key-value pair and mapping to reload map for further comparison
					reloadMap.put(kvPair, dm);

				}

				// final compare: remove those that weren't part of the reload
				for (Map.Entry<Pair<String, String>, Map<String, String>> entry : LookupDataMap.dataMap.entrySet()) {
					Pair<String, String> kvPair = entry.getKey();
					if (!reloadMap.containsKey(kvPair)) {
						LookupDataMap.dataMap.remove(kvPair);
					}
				}

				LOG.info("Completed lookup data map refresh.");

			} catch(Exception e) {
				LOG.error("Exception while loading lookup data map.", e);
			}

		}

	}

}
