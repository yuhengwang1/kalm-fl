package main.java.edu.stonybrook.cs.utils;

import main.java.edu.stonybrook.cs.computation.EdgeNode;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * This method caches some accessed data for future use without requesting BabelNet again.
 */
public class BabelNetCache {
	private static HashMap<String, Double> SynsetIdSemanticConnectionScoreMap = new HashMap<String, Double>(); // (synset Id, sqrt score of the synset)
	private static HashMap<String, ArrayList<EdgeNode>> synsetIdAdjacentEdgeNodesMap = new HashMap<String, ArrayList<EdgeNode>>(); // (synset Id, adjacent edge nodes)
	
	public static void clear()
	{
		SynsetIdSemanticConnectionScoreMap.clear();
		synsetIdAdjacentEdgeNodesMap.clear();
	}
	
	public synchronized static void recordBabelNetCall(String bncall)
	{
		try (BufferedWriter bw = new BufferedWriter(new FileWriter("resources/logs/log.txt", true))) 
		{
			bw.write(bncall + "\n");
		}
		catch (IOException x) 
		{
			System.err.println(x);
			x.printStackTrace();
		}
	}

	/**
	 * @param sid synset Id
	 * @return sqrt of number of adjacent synsets
	 * A sqrt score getter.
	 */
	public static Double getCachedSemanticConnectionScore(String sid) {
		if(SynsetIdSemanticConnectionScoreMap.containsKey(sid)) {
			return SynsetIdSemanticConnectionScoreMap.get(sid);
		} else {
			return null;
		}
	}

	/**
	 * @param sid synset Id
	 * @param score the connection score (sqrt of the number of the adjacent synsets)
	 * A sqrt score setter.
	 */
	public synchronized static void setCachedSemanticConnectionScore(String sid, Double score)
	{
		if(!SynsetIdSemanticConnectionScoreMap.containsKey(sid))
		{
			SynsetIdSemanticConnectionScoreMap.put(sid, score);
			return;
		}
	}

	/**
	 * @param sid
	 * @return the edge node list
	 * Edge node list getter.
	 */
	public static ArrayList<EdgeNode> getCachedEdgeNodes(String sid)
	{
		if(synsetIdAdjacentEdgeNodesMap.containsKey(sid))
		{
			return synsetIdAdjacentEdgeNodesMap.get(sid);
		}
		else
		{
			return null;
		}
	}

	/**
	 * @param sid synset Id
	 * @param nodeList a list containing all the adjacent edge nodes of this synset
	 * Edge node list setter.
	 */
	public synchronized static void setCachedEdgeNodes(String sid, ArrayList<EdgeNode> nodeList)
	{
		if(!synsetIdAdjacentEdgeNodesMap.containsKey(sid))
		{
			synsetIdAdjacentEdgeNodesMap.put(sid, nodeList);
			return;
		}
	}
}
