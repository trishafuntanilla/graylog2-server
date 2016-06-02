package org.graylog2.filters;

import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.graylog2.lookup.LookupDataMap;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.filters.MessageFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author tfuntani
 *
 */
public class LookupFilter implements MessageFilter {
	
	private static final Logger LOG = LoggerFactory.getLogger(LookupFilter.class);

	private static final String NAME = "Lookup Filter";
	private static final String FIELD_DOCTYPE_ = "doctype_";
	private static final String FIELD_INVALID = "parsing_failed";	// add parsing_failed field if msg failed extractor for whatever reason
	private static final String FIELD_INVALID_VALUE = "fail";
	private static final String ALPHANUM_REGEX = "[^a-zA-Z0-9]+";	// for removing special chars for doctype_ values
	
	@Override
	public boolean filter(Message msg) {
		
		if (msg.getSourceInputId() == null) {      	
    		msg.addField(FIELD_INVALID, FIELD_INVALID_VALUE);
            return false;
        }
		
		try {
			lookup(msg);
		} catch(Exception e) {
			LOG.error("Could not apply lookup for " + msg.getId());		
			msg.addField(FIELD_INVALID, FIELD_INVALID_VALUE);
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
	
	private void lookup(Message msg) {
		
		for (Map.Entry<Pair<String, String>, Map<String, String>> entry : LookupDataMap.dataMap.entrySet()) {
			
			Pair<String, String> kvPair = entry.getKey();
			String existingField = kvPair.getLeft();
			String newField = kvPair.getRight();
			
			if (msg.hasField(existingField)) {				
				
				String existingFieldValue = (String) msg.getField(existingField);			
				Map<String, String> mappings = entry.getValue();
				
				if (!mappings.isEmpty()) {
					
					String newFieldValue = mappings.get(existingFieldValue);
									
					if (newFieldValue != null) {
						
						String[] newFieldsArray = newField.split(",");
						for (String nf : newFieldsArray) {
							if (nf.equals(FIELD_DOCTYPE_)) {
								String alphanum_newFieldValue = newFieldValue.replaceAll(ALPHANUM_REGEX, "");
								msg.addField(nf, alphanum_newFieldValue);
							} else {
								msg.addField(nf, newFieldValue);
							}
						}
						
					} else { // lookup failed; there is no mapping
						// check if doctype_ is supposed to be set based on the mapping for the value of this existing field
						if (newField.contains(FIELD_DOCTYPE_)) {
							// if so, make doctype_ = value of existing field (remove all special chars)
							String alphanum_existingFieldValue = existingFieldValue.replaceAll(ALPHANUM_REGEX, "");							
							msg.addField(FIELD_DOCTYPE_, alphanum_existingFieldValue);
						}
					}
				}
			} else {
				// if doctype_ is supposed to be set based on the mapping for the value of this existing field, 
				// if the existing field itself is missing from the msg, the msg is automatically invalid
				if (newField.contains(FIELD_DOCTYPE_)) {
					msg.addField(FIELD_INVALID, FIELD_INVALID_VALUE);	// add parsing_failed field
				}						
			}
		}
		
	}
	
}
