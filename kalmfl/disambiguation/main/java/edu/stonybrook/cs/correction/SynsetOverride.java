package main.java.edu.stonybrook.cs.correction;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SynsetOverride {
	private static Set<String> synsetOverrideSet = new HashSet<String>();
	private static final String input = "../../resources/semantic_score_meta/synset_override.txt";
	
	public static void initialize()
	{
		parse();
	}

	/**
	 * @param synset
	 * @return a boolean value to tell if a word-synset relation is overridden
	 */
	public static boolean isSynsetOverridden(String synset)
	{
		if(synsetOverrideSet.contains(synset))
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	/**
	 * @param input
	 * This method receives a line in synset_override.txt and stores the word-synset relation in a set
	 */
	private static void parseSynsetOverridePredicate(String input)
	{
	     Matcher m = Pattern.compile("^synset_override\\(\\'(.+)\\',\\'(.+)\\'\\)\\.$").matcher(input);
	     while(m.find()) {
//	    	 System.out.println(m.group(1) + "-" + m.group(2));
	    	 synsetOverrideSet.add(m.group(1) + "-" + m.group(2));      
	     }	       	       
	}
	/**
	 * This method reads resources/semantic_score_meta/synset_override.txt and parse it.
	 */
	public static void parse()
	{
		try (BufferedReader br = new BufferedReader(new FileReader(input))) 
		{			
			String sentence;			
			while((sentence = br.readLine())!=null)
			{
				parseSynsetOverridePredicate(sentence);
			}		
		}
		catch (IOException x) 
		{
			System.err.println(x);
			x.printStackTrace();
		}
	}
}
