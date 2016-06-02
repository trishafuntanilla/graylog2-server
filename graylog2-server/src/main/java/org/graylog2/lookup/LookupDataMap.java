package org.graylog2.lookup;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.tuple.Pair;

/**
 * 
 * @author tfuntani
 *
 */
public class LookupDataMap {
	
	public static ConcurrentHashMap<Pair<String, String>, Map<String, String>> dataMap = new ConcurrentHashMap<Pair<String, String>, Map<String, String>>();
	
}
