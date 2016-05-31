package org.graylog2.filters;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.filters.MessageFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class LookupFilter implements MessageFilter {

	private static final Logger LOG = LoggerFactory.getLogger(LookupFilter.class);

	private static final String LOOKUP = "lookup";
	public static ConcurrentHashMap<Pair<String, String>, Map<String, String>> dataMap = new ConcurrentHashMap<Pair<String, String>, Map<String, String>>();

	@Inject
	public LookupFilter(MongoConnection mongoConnection) {

		LOG.info("Starting lookup filter...");

		/*
		 * Expected MongoDB lookup document format (one document per mapping):
		 * 
		   {
			 	"_id" : ObjectId("some_mongo_id"),
			    "key" : "xxxx",
			    "value" : "yyyy,zzzz",
			    "mappings" : [
			    				{ 
			    					xxxx_value1:yyyy_and_zzzz_value1,
			    				 	xxxx_value2:yyyy_and_zzzz_value2 
			    				 }
			    			]
		    }

		 */

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
				LOG.info("Adding < " + key + " > : < " + value + " > pair mappings...");
				Map<String, String> dm = new HashMap<String, String>();
				for (Map.Entry<String, JsonElement> entry : map.entrySet()) {
					dm.put(entry.getKey(), entry.getValue().getAsString());
				}

				// put kvPair and corresponding mappings into the data map stored in memory
				dataMap.put(kvPair, dm);
			}

			LOG.info("Completed lookup filter injection.");		
		}

	}

	@Override
	public boolean filter(Message msg) {

		// for each message, check if any of the kv mappings apply
		for (Map.Entry<Pair<String, String>, Map<String, String>> entry : dataMap.entrySet()) {
			Pair<String, String> kvPair = entry.getKey();
			String existingField = kvPair.getLeft(); // the field to look for in the message
			if (msg.hasField(existingField)) {
				String valueOfExistingField = (String) msg.getField(existingField);	// the value of the field found; this is the 'key' in the mappings
				String newField = kvPair.getRight();	// the names of the new field(s) we want to add
				Map<String, String> mappings = entry.getValue();	// the mappings for this field
				if (!mappings.isEmpty()) {
					String valueForNewField = mappings.get(valueOfExistingField);	// the value for the field(s) we want to add
					String[] newFieldsArray = newField.split(",");	// multiple new fields are expected to be separated by a comma
					// add the new fields; values for type fields should not have special chars
					for (String nf : newFieldsArray) {
						if (nf.equals("doctype_")) {
							String parsedValue = valueForNewField.replaceAll("[^a-zA-Z0-9]+", "");
							msg.addField(nf, parsedValue);
						} else {
							msg.addField(nf, valueForNewField);
						}
					}
				}
			}
		}

		// continue on with other filters
		return false;
	}

	@Override
	public String getName() {
		return "LookupFilter";
	}

	@Override
	public int getPriority() {
		// run this filter after extractors
		return 15;
	}
	
}
