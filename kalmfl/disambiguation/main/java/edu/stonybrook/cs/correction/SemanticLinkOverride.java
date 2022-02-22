package main.java.edu.stonybrook.cs.correction;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This method overrides the some edge types presented in resources/semantic_score_meta/semantic_link_override.txt
 */
public class SemanticLinkOverride {
	private static Set<String> overriddenEdgeTypeSet = new HashSet<String>();
	private static final String input = "../../resources/semantic_score_meta/semantic_link_override.txt";
	
	public static void initialize()
	{
		parse();
	}

	/**
	 * @param link a relation string
	 * @return check if the edge type relation is overridden
	 */
	public static boolean isSemanticLinkOverridden(String link)
	{
		if(overriddenEdgeTypeSet.contains(link))
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	/**
	 * @param input a lien of semantic_link_override.txt
	 * This method read every line of semantic_link_override.txt and add the relation to a set
	 */
	private static void parseSemanticLinkOverridePredicate(String input)
	{
	     Matcher m = Pattern.compile("^semantic_link_override\\(\\'(.+)\\',\\'(.+)\\',\\'(.+)\\'\\)\\.$").matcher(input);
	     while(m.find()) {	    	 
	    	 overriddenEdgeTypeSet.add(m.group(1) + "-" + m.group(2) + "-" + m.group(3));
	     }	       	       
	}

	/**
	 * This method reads resources/semantic_score_meta/semantic_link_override.txt
	 */
	public static void parse()
	{
		try (BufferedReader br = new BufferedReader(new FileReader(input))) 
		{			
			String sentence;			
			while((sentence = br.readLine())!=null)
			{
				parseSemanticLinkOverridePredicate(sentence);
			}		
		}
		catch (IOException x) 
		{
			System.err.println(x);
			x.printStackTrace();
		}
	}
}
