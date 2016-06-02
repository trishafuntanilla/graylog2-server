package org.graylog2.filters;

import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.graylog2.lookup.LookupDataMap;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.filters.MessageFilter;

public class LookupFilter implements MessageFilter {

	private static final String NAME = "Lookup Filter";
	private static final String DOCTYPE_ = "doctype_";
	private static final String ALPHANUMERIC_REGEX = "[^a-zA-Z0-9]+";	// for removing special chars for doctype_ values
	
	@Override
	public boolean filter(Message msg) {

		for (Map.Entry<Pair<String, String>, Map<String, String>> entry : LookupDataMap.dataMap.entrySet()) {
			Pair<String, String> kvPair = entry.getKey();
			String existingField = kvPair.getLeft();
			String newField = kvPair.getRight();
			if (msg.hasField(existingField)) {				
				String valueOfExistingField = (String) msg.getField(existingField);			
				Map<String, String> mappings = entry.getValue();
				if (!mappings.isEmpty()) {
					String valueForNewField = mappings.get(valueOfExistingField);
					String[] newFieldsArray = newField.split(",");
					if (valueForNewField != null) {	
						for (String nf : newFieldsArray) {
							if (nf.equals(DOCTYPE_)) {
								String alphanum_valueForNewField = valueForNewField.replaceAll(ALPHANUMERIC_REGEX, "");
								msg.addField(nf, alphanum_valueForNewField);
							} else {
								msg.addField(nf, valueForNewField);
							}
						}
					} else {
						// if lookup fails because there's no mapping,
						// check if doctype_ is set based on the mapping for the value of this existing field
						if (newField.contains(DOCTYPE_)) {
							String alphanum_valueOfExistingField = valueOfExistingField.replaceAll(ALPHANUMERIC_REGEX, "");							
							// if so, make doctype_ = value of existing field without all the special chars
							msg.addField(DOCTYPE_, alphanum_valueOfExistingField);
						}
					}
				}
			} else {
				// if doctype_ is set based on the mapping for the value of this existing field, 
				// msg without this existing field is automatically invalid
				if (newField.contains(DOCTYPE_)) {
					msg.addField(DOCTYPE_, "notfound");
				}						
			}
		}

		return false;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public int getPriority() {
		// run this filter after extractors
		return 15;
	}
	
}
