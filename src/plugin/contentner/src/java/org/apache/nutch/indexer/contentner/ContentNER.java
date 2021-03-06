/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.nutch.indexer.contentner;

import java.lang.invoke.MethodHandles;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.nutch.crawl.CrawlDatum;
import org.apache.nutch.crawl.Inlinks;
import org.apache.nutch.indexer.IndexingException;
import org.apache.nutch.indexer.IndexingFilter;
import org.apache.nutch.indexer.NutchDocument;
import org.apache.nutch.parse.Parse;
import org.apache.tika.parser.ner.corenlp.CoreNLPNERecogniser;
// import org.json.JSONObject;

/**
 * ContentNER gets parsed data from the raw fetched content, applies a Name-Entity Recognizer and
 * adds the results to a {@link NutchDocument} (doc).
 * 
 * The NER recognizer ({@link CoreNLPNERecogniser}) returns Map<String, Set,String>> which
 * is a map containing entries of key-value pairs: key=("PERSON"|"ORGANIZATION",..),
 * value (list of persons|list of organizations,...).The lists of persons,..,organizations 
 * are extracted from the parsed text.
 * 
 * @see IndexingFilter#filter
 * @return doc
 * @param parse
 * @param datum
 * @param inLinks
 */
public class ContentNER implements IndexingFilter {

	private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private static final String CONF_PROPERTY = "contentner.tags";
	// private static String[] urlMetaTags;
	private Configuration conf;

	CoreNLPNERecogniser ner = null;
	
	public NutchDocument filter(NutchDocument doc, Parse parse, Text url, CrawlDatum datum, Inlinks inlinks) {
		
		//parse.getText() returns the main body of unstructured text containing person and entity names
		String content = parse.getText();
		
		//initialize CoreNLPNERecogniser and load dictionaries (FIX: dictionary name is hardcoded)
		if (ner == null) {
			ner = new CoreNLPNERecogniser();
			if (ner == null) {
				LOG.error("CoreNLPNERegcognizer not initialized successfully - terminating program");
				System.exit(-1); //exit if fail
			}
		}

		// call NER recognize() on parse content returning the map of key-value pairs described above.
		Map<String, Set<String>> names = ner.recognise(content);
		
		//loop through NER results, flatten to Map<String>,String> using StringBuilder and add map entries to doc
		Set<Map.Entry<String,Set<String>>> es = names.entrySet();
		
		//iterate through keys
		Iterator<Map.Entry<String,Set<String>>> iterator = es.iterator();
		while (iterator.hasNext()) {
			Map.Entry<String,Set<String>> me = iterator.next();
			String key = me.getKey();
			
			//iterate through values
			Set<String> esv = me.getValue();
			
			//iterate through names, entity set
			Iterator<String> setIterator = esv.iterator();
			String strofnames = new String();
			while (setIterator.hasNext()) {
				String value = setIterator.next();
				
				//build appended string of names, entities,...
				strofnames = new StringBuilder(strofnames).append(value).toString();
				
				//add " : " separator, do not add at end of appended string
				if (setIterator.hasNext()) strofnames = new StringBuilder(strofnames).append(" : ").toString();
			}
			System.out.println("key val: " + key + ": " + strofnames);
			
			// add key, entities to doc
			doc.add(key, strofnames);
		}

		//        JSONObject jNames = new JSONObject(names);
		//        System.out.println(jNames.toString(2));

		return doc;
	}

	/**
	 * getConf - return ContentNER configuration
	 * @return conf - configuration used by ContentNER
	 */
	public Configuration getConf() {
		return conf;
	}

	/**
	 * setConf - set configuration
	 * @param conf - set configuration used by ContentNER
	 */
	public void setConf(Configuration conf) {
		this.conf = conf;
	}
}
