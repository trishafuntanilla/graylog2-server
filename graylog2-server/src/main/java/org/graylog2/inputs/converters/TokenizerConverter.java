/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.inputs.converters;

import com.google.common.collect.ImmutableMap;
import org.graylog2.plugin.inputs.Converter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Strings.isNullOrEmpty;

public class TokenizerConverter extends Converter {
    // ┻━┻ ︵ ¯\(ツ)/¯ ︵ ┻━┻
    private static final Pattern PATTERN = Pattern.compile("(?:^|\\s)(?:([\\w-]+)\\s?=\\s?((?:\"[^\"]+\")|(?:'[^']+')|(?:[\\S]+)))");
    
    // ┻━┻ ︵ ¯\(ツ)/¯ ︵ ┻━┻
    // for when key=value pairs are separated by commas or pipes
    // should still work if they're just separated by white spaces (default)
    private static final Pattern PATTERN_2 = Pattern.compile("(?:^|\\s|,|)(?:([\\w-]+),?=,?((?:\"[^\"]+\")|(?:'[^']+')|(?:[^,\\s|]+)))");

    // pattern for skipping those with consecutive =
    Pattern SKIP_PATTERN = Pattern.compile("(\\=)\\1+");
    
    public TokenizerConverter(Map<String, Object> config) {
        super(Type.TOKENIZER, config);
    }

    @Override
    public Object convert(String value) {
        if (isNullOrEmpty(value)) {
            return value;
        }
        
        // remove quotes from keys because the converters won't work with them
        String str = value;
		
		// replace all ',' or '|' with space, as fields are getting merged if separator is one of these and value is empty
		str = str.replaceAll("([\\,]|[\\|])", " ");
		
		// replace multiple consecutive '=' with space, k=v pairs extraction failing if message contains consecutive '=' chars 
    	str = str.replaceAll("(\\=)\\1+", " ");
		
		//remove space in front and after '=' in key-value pair, as those fields are not getting extracted
		str = str.replaceAll("((\\s+)=(\\s+))", "=");
		
        if (str.indexOf("\"=\"") > -1) {
        	
        	// remove quotes on first key
			str = str.substring(1);
			int index = str.indexOf("\"=\"");
			str = str.substring(0, index) + str.substring(index+1);

			// remove quotes on rest of keys
			// ┻━┻ ︵ ¯\(ツ)/¯ ︵ ┻━┻
			while (str.indexOf("\"=\"") > -1) {

				if (str.indexOf("\" \"") > -1) {
					index = str.indexOf("\" \"");
				} else if (str.indexOf("\",\"") > -1) {
					index = str.indexOf("\",\"");
				} else if (str.indexOf("\"|\"") > -1) {
					index = str.indexOf("\"|\"");
				}

				str = str.substring(0, index+1) + " " + str.substring(index+3);

				index = str.indexOf("\"=\"");
				str = str.substring(0, index) + str.substring(index+1);
				
			}
			
        }
		
		value = str;


        if (value.contains("=")) {
        	
        	/*
			// if there are multiple consecutive '=' in the message, it will mess up the key=value extractions so skip it...
        	Matcher s = SKIP_PATTERN.matcher(value);
        	if (s.find()) {
        		return Collections.emptyMap();
        	}*/
        	
            final ImmutableMap.Builder<String, String> fields = ImmutableMap.builder();

            Matcher m = PATTERN_2.matcher(value);
            Map<String, String> pairs = new HashMap<String, String>();
            while (m.find()) {
                if (m.groupCount() != 2) {
                    continue;
                }
                
                // avoid duplicates by putting it on a map first, replacing previous key value with new one
                pairs.put(removeQuotes(m.group(1)), removeQuotes(m.group(2)));
                
                // fields.put(removeQuotes(m.group(1)), removeQuotes(m.group(2)));
            }
            
            // after collecting all pairs, put them to fields
            for (Map.Entry<String, String> entry : pairs.entrySet()) {
            	fields.put(entry.getKey(), entry.getValue());
          	}

            return fields.build();
        } else {
            return Collections.emptyMap();
        }
    }

    private String removeQuotes(String s) {
        if ((s.startsWith("\"") && s.endsWith("\"")) ||
                (s.startsWith("'") && s.endsWith("'")) ) {
            return s.substring(1, s.length() - 1);
        } else {
            return s;
        }
    }

    @Override
    public boolean buildsMultipleFields() {
        return true;
    }
}
