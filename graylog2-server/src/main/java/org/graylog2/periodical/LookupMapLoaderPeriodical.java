package org.graylog2.periodical;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.graylog2.database.MongoConnection;
import org.graylog2.filters.LookupFilter;
import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class LookupMapLoaderPeriodical extends Periodical {

	private static final Logger LOG = LoggerFactory.getLogger(LookupMapLoaderPeriodical.class);

	private MongoConnection mongoConnection;
	private static final String LOOKUP = "lookup";

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
		return false;
	}

	@Override
	public boolean masterOnly() {
		return false;
	}

	@Override
	public boolean startOnThisNode() {
		return false;
	}

	@Override
	public boolean isDaemon() {
		return false;
	}

	@Override
	public int getInitialDelaySeconds() {
		// first run is after 5 min of start up
		return 300;
	}

	@Override
	public int getPeriodSeconds() {
		// run every 8 hours
		return 28800;
	}

	@Override
	protected Logger getLogger() {
		return LOG;
	}

	@Override
	public synchronized void doRun() {

		LOG.info("Syncing map with MongoDB...");

		// reload mongo data here; compare with the static lookup data map at the end
		ConcurrentHashMap<Pair<String, String>, Map<String, String>> reloadedMap = new ConcurrentHashMap<Pair<String, String>, Map<String, String>>();

		// retrieve lookup data map from MongoDB
		DBCollection collection = mongoConnection.getDatabase().getCollection(LOOKUP);
		DBCursor cursor = collection.find();
		// loop through each document...
		if (cursor != null) {			
			while (cursor.hasNext()) {							
				DBObject doc = cursor.next();

				// get document as json object
				JsonArray mappings = new JsonArray();
				JsonParser parser = new JsonParser();
				JsonElement je = parser.parse(doc.toString());
				JsonObject json = je.getAsJsonObject();

				// get key and value from document and store them as pair
				String key = json.get("key").getAsString();
				String value = json.get("value").getAsString();
				Pair<String, String> kvPair = new ImmutablePair<String, String>(key, value);

				// get mappings array and get first object in array since it is expected to only have one object
				mappings = json.getAsJsonArray("mappings");
				JsonElement mappingsObject = mappings.get(0);
				JsonObject map = mappingsObject.getAsJsonObject();

				// put the mappings into a data map
				Map<String, String> dm = new HashMap<String, String>();
				for (Map.Entry<String, JsonElement> entry : map.entrySet()) {
					dm.put(entry.getKey(), entry.getValue().getAsString());
				}

				// compare: if static in-memory data map doesn't already have this kvPair, put it
				if (!LookupFilter.dataMap.containsKey(kvPair)) {
					LookupFilter.dataMap.put(kvPair, dm);
				}

				// place kvPair and mapping to reloaded map for further comparison
				reloadedMap.put(kvPair, dm);
			}

			// final compare: remove those that weren't part of the reload
			for (Map.Entry<Pair<String, String>, Map<String, String>> entry : LookupFilter.dataMap.entrySet()) {
				Pair<String, String> kvPair = entry.getKey();
				if (!reloadedMap.containsKey(kvPair)) {
					LookupFilter.dataMap.remove(kvPair);
				}
			}

			LOG.info("Completed reloading of data map.");		
		}
	}

}
