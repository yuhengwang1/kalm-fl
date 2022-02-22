package main.java.edu.stonybrook.cs.correction;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import main.java.edu.stonybrook.cs.computation.EdgeNode;

/**
 * This class is a util class to add some missing edges in BabelNet
 */
public class SemanticLinkAddition {
	private static HashMap<String,ArrayList<EdgeNode>> synsetIdEdgeNodesMap =
			new HashMap<String,ArrayList<EdgeNode>>(); // (synsetId, list of adjacent nodes)
	private static final String input = "../../resources/semantic_score_meta/semantic_link_augmentation.txt";
	
	public static void initialize()
	{
		parse();
	}
	
	public static boolean hasAddedSemanticLinks(String synset)
	{
		return synsetIdEdgeNodesMap.containsKey(synset);
	}
	
	public static ArrayList<EdgeNode> getAddedSemanticLinks(String synset)
	{
		return synsetIdEdgeNodesMap.get(synset);
	}

	/**
	 * @param input a line in resources/semantic_score_meta/semantic_link_augmentation.txt
	 * This method extract the edge, two nodes and the weight
	 */
	private static void parseSemanticLinkAugmentationPredicate(String input)
	{
	     Matcher m = Pattern.compile("^semantic_link_augmentation\\(\\'(.+)\\',\\'(.+)\\',\\'(.+)\\',(.+)\\)\\.$").matcher(input);
	     while(m.find()) {
	    	 EdgeNode node = new EdgeNode();
			 node.edgeNodeSynsetID = m.group(3);
			 node.edgeWeight = Double.parseDouble(m.group(4));
			 node.edgeType = m.group(2);
//			 System.out.println(node.parent + " " + node.edgeType + " " + node.edgeNodeSynsetID + " " + node.edgeWeight);
			 if(synsetIdEdgeNodesMap.containsKey(m.group(1))) // the first synset Id is key
			 {
				 synsetIdEdgeNodesMap.get(m.group(1)).add(node); // value is a list containing all nodes that connect to it
			 }
			 else
			 {
				ArrayList<EdgeNode> list = new ArrayList<EdgeNode>();
				list.add(node);
				synsetIdEdgeNodesMap.put(m.group(1), list);
			 }      
	     }	       	       
	}

	/**
	 * Read from resources/semantic_score_meta/semantic_link_augmentation.txt
	 */
	public static void parse()
	{
		try (BufferedReader br = new BufferedReader(new FileReader(input))) 
		{			
			String sentence;			
			while((sentence = br.readLine())!=null)
			{
				parseSemanticLinkAugmentationPredicate(sentence);
			}		
		}
		catch (IOException x) 
		{
			System.err.println(x);
			x.printStackTrace();
		}
	}
}
