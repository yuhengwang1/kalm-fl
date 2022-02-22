package main.java.edu.stonybrook.cs.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import it.uniroma1.lcl.babelnet.BabelNet;
import it.uniroma1.lcl.babelnet.BabelSynset;
import it.uniroma1.lcl.babelnet.BabelSynsetID;
import it.uniroma1.lcl.babelnet.BabelSynsetIDRelation;
import it.uniroma1.lcl.babelnet.InvalidBabelSynsetIDException;
import it.uniroma1.lcl.babelnet.data.BabelPointer;
import main.java.edu.stonybrook.cs.computation.EdgeNode;
import main.java.edu.stonybrook.cs.correction.SemanticLinkAddition;
import main.java.edu.stonybrook.cs.correction.SemanticLinkOverride;
import main.java.edu.stonybrook.cs.correction.SemanticScoreParameters;

/**
 * A util class offering static methods for score computation. The computation needs to request BabelNet so it uses
 * BabelNetCache to avoid repetitive requests.
 */
public class BabelNetShareResource {
	private static HashMap<String,Double> pairKeyScoreMap = new HashMap<String,Double>(); // (pairKey:index, score)
	private static HashMap<String,String> pairKeyPathMap = new HashMap<String,String>(); // (pairKey:index, path)
	private static HashMap<String, Double> pairKeyGlobalValMap = new HashMap<String, Double>(); // (pairKey, global val)

	public static Double getGlobalVal(String pairKey) { return pairKeyGlobalValMap.getOrDefault(pairKey, 0.0); }

	/**
	 * @param pairKey
	 * @param finalScore the final score computed by every thread
	 * Note that this method is synchronized, which prevent multiple threads access this method as the same time. It updates and saves the final scores.
	 */
	public synchronized static void setGlobalVal(String pairKey, Double finalScore) {
		if (!pairKeyGlobalValMap.containsKey(pairKey)) {
			pairKeyGlobalValMap.put(pairKey, finalScore);
		} else if (finalScore > pairKeyGlobalValMap.get(pairKey)) {  // if the final score is better than before, update
				pairKeyGlobalValMap.put(pairKey, finalScore);
		}
	}
	
	public static void clear() {
		pairKeyScoreMap.clear();
		pairKeyPathMap.clear();
		pairKeyGlobalValMap.clear();
		BabelNetCache.clear();

	}
	
	public static String getPath(String pairKey, int synsetIndex) { return pairKeyPathMap.get(pairKey + ":" + synsetIndex); }
	
	public static Double getScore(String pairKey, int synsetIndex) { return pairKeyScoreMap.get(pairKey + ":" + synsetIndex); }


	/**
	 * @param pairKey
	 * @param synsetIndex
	 * @param val
	 * @param path
	 * This method is synchronized. Methods setting scores are all set as synchronized.
	 */
	public synchronized static void setScore(String pairKey, int synsetIndex, Double val, String path) {
		Double score = -1.0;
		if(pairKeyScoreMap.containsKey(pairKey + ":" + synsetIndex)) {  // if it has score in history, get this score
			score = pairKeyScoreMap.get(pairKey + ":" + synsetIndex);
		}
		if(score < val) {  // if this score is less than the current score, update score and path
			pairKeyScoreMap.put(pairKey + ":" + synsetIndex, val);
			pairKeyPathMap.put(pairKey + ":" + synsetIndex, path);
		}
	}

	/**
	 * @param sid a synset Id
	 * @return the sqrt value of the number of the adjacent synsets of the given Id, which is a property of a synset node.
	 */
	public static Double getSemanticConnectionNum(String sid) {
		Double cachedScore = BabelNetCache.getCachedSemanticConnectionScore(sid); // if the sqrt value has cache, use cache
		
		if(cachedScore != null) {
			return cachedScore;
		}
		
		BabelNet bn = BabelNet.getInstance();
		BabelSynset synset = null;
		try {
			synset = bn.getSynset(new BabelSynsetID(sid)); // get a new instance of the specified synset id
		} catch (IOException | InvalidBabelSynsetIDException e) {
			e.printStackTrace();
		}
		assert synset != null;
		Double score = Math.sqrt(synset.getEdges().size()*1.0); // the score of the synset is related to the number of its adjacent synsets
		BabelNetCache.setCachedSemanticConnectionScore(sid, score); // cache the node score
		return score;
	}

	/**
	 * @param parameterKey
	 * @param givenSynsetID a synset Id whose adjacent nodes are to be returned
	 * @return a list of edge nodes related to the given synset
	 * This method finds out all the adjacent edge nodes and add additional edges and return all of them as a list.
	 */
	public static ArrayList<EdgeNode> getEdgeNodes(String parameterKey, String givenSynsetID)
	{
		ArrayList<EdgeNode> cachedEdgeNodeList = BabelNetCache.getCachedEdgeNodes(givenSynsetID);
		if(cachedEdgeNodeList != null) // if the adjacent nodes of this synset are cached,return the cache
		{
			ArrayList<EdgeNode> result = new ArrayList<EdgeNode>();
			for(EdgeNode node : cachedEdgeNodeList)
			{
				assert node.nodeID == -1;
				assert node.parent == -1;
				EdgeNode tmpNode = new EdgeNode(node);
				result.add(tmpNode);
			}
			return result;
		}
		
		BabelNet bn = BabelNet.getInstance();
		HashMap<String,EdgeNode> pointedSynsetIDEdgeNodeMap = new HashMap<String,EdgeNode>(); // record all the outgoing (synsetID, edge node)
		ArrayList<EdgeNode> givenSynsetEdgeNodesList = new ArrayList<EdgeNode>();
			
		BabelSynset by;
		try {
			by = bn.getSynset(new BabelSynsetID(givenSynsetID));
			for(BabelSynsetIDRelation edge : by.getEdges()) { // for every edge of the specified synset
				Double weight = getAdjustedEdgeWeight(parameterKey, edge); // adjusted weight based on edge.getWeight()
				if(weight > 0)
				{
					String pointedSynsetID = edge.getBabelSynsetIDTarget().getID(); // get the pointed synset ID of the edge
					if(SemanticLinkOverride.isSemanticLinkOverridden(givenSynsetID+"-"+getEdgeType(edge.getPointer())+"-"+pointedSynsetID))
					{
						continue; // if this edge type overridden, go to next edge
					}
					
					if(pointedSynsetIDEdgeNodeMap.containsKey(pointedSynsetID)) // if it is a recorded pointed ID
					{
						EdgeNode node = pointedSynsetIDEdgeNodeMap.get(pointedSynsetID); // get this edge node
						if(weight > node.edgeWeight) // update this edge node
						{
							node.edgeWeight = weight;
							node.edgeType = getEdgeType(edge.getPointer());
						}
					}
					else // if it is a new ID never recorded
					{	EdgeNode node = new EdgeNode();
						node.edgeNodeSynsetID = pointedSynsetID;
						node.edgeWeight = weight;
						node.edgeType = getEdgeType(edge.getPointer());
						assert node.nodeID == -1;
						assert node.parent == -1;
						pointedSynsetIDEdgeNodeMap.put(pointedSynsetID, node); // add this edge node to map
					}  
				}    
	        }
			/*
			if(s1.equals("bn:00019336n"))
			{
				EdgeNode node = new EdgeNode();
				node.edgeNodeSynsetID = "bn:00056337n";
				node.edgeWeight = 0.1;
				node.edgeType = "hypernym";
				node.parent = s1;
				nodeMap.put("bn:00056337n", node);
			}
			*/
			if(SemanticLinkAddition.hasAddedSemanticLinks(givenSynsetID)) // if the given synset has additional edges, additionally deal with these edges
			{
				ArrayList<EdgeNode> addedLinkList = SemanticLinkAddition.getAddedSemanticLinks(givenSynsetID);
				for(EdgeNode link : addedLinkList)
				{
					assert link.nodeID == -1;
					assert link.parent == -1;
					if(pointedSynsetIDEdgeNodeMap.containsKey(link.edgeNodeSynsetID))
					{
						EdgeNode node = pointedSynsetIDEdgeNodeMap.get(link.edgeNodeSynsetID);
						if(link.edgeWeight > node.edgeWeight)
						{
							node.edgeWeight = link.edgeWeight;
							node.edgeType = link.edgeType;
						}
					}
					else
					{	
						EdgeNode node = new EdgeNode(link);
						pointedSynsetIDEdgeNodeMap.put(link.edgeNodeSynsetID, node);
					} 
				}
			}
		} catch (IOException | InvalidBabelSynsetIDException e) {
			e.printStackTrace();
		}
		List<String> keyList = new ArrayList<String>(pointedSynsetIDEdgeNodeMap.keySet());
		for(String key : keyList)
		{
			givenSynsetEdgeNodesList.add(pointedSynsetIDEdgeNodeMap.get(key));
		}
		BabelNetCache.setCachedEdgeNodes(givenSynsetID, givenSynsetEdgeNodesList); // add all the newly founded edge nodes to the cache
		ArrayList<EdgeNode> result = new ArrayList<EdgeNode>();
		for(EdgeNode node : givenSynsetEdgeNodesList)
		{
			EdgeNode tmpNode = new EdgeNode(node);
			result.add(tmpNode);
		}
		return result;
	}

	/**
	 * @param bp the pointer of an edge
	 * @return the type of the edge
	 */
	private static String getEdgeType(BabelPointer bp)
	{
		if(bp.isHypernym())
		{
			return "hypernym"; // higher level
		}
		else if(bp.isHyponym())
		{
			return "hyponym"; // lower level
		}
		else if(bp.isHolonym())
		{
			return "holonym"; // whole
		}
		else if(bp.isMeronym())
		{
			return "meronym"; // part
		}
		else if(bp.getName().equals("Derivationally related form"))
		{
			return "Derivationally related form";
		}
		else if(bp.getName().equals("Gloss related form (disambiguated)"))
		{
			return "Gloss related form (disambiguated)";
		}
		else if(bp.getName().equals("Gloss related form (monosemous)"))
		{
			return "Gloss related form (monosemous)";
		}
		else
		{
			return "other";
		}
	}

	/**
	 * @param parameterKey
	 * @param edge
	 * @return The adjusted weight of an edge
	 * This method check if the edge has overridden weight. If so, adjust it and return.
	 */
	private static Double getAdjustedEdgeWeight(String parameterKey, BabelSynsetIDRelation edge)
	{
		Double weight = edge.getWeight();
		String edgeType = getEdgeType(edge.getPointer());
		return weight + SemanticScoreParameters.getWeightBias(parameterKey + "::" + edgeType); // weight itself + preset bias
	}

	/**
	 * @param parameterKey
	 * @param weightedCountTotal this is a sum of scores, until now, until the currently explored node
	 * @param path this is a path until now
	 * @return
	 */
	public static double computeSemanticScore(String parameterKey, Double weightedCountTotal, ArrayList<String> path)
	{
		double numerator = weightedCountTotal * 1.0;
		double denominator = 0.0;
		
		for(String edge : path)
		{
			denominator += SemanticScoreParameters.getPenaltyVal(parameterKey + "::" + edge); // different types of edges have different penalties
		}
		return numerator/Math.pow(5.0, denominator);
	}

	/**
	 * @param synsetIdEdgeNodeMap a map that saves all the (synsetId, edge node)s that are traversed
	 * @param targetEdgeNode the target synset
	 * @return a path list containing synset Ids
	 * This method uses the structure of EdgeNode (with a father field) to recursively find fathers till the source.
	 */
	public static ArrayList<String> getPath(HashMap<Integer, EdgeNode> synsetIdEdgeNodeMap, EdgeNode targetEdgeNode)
	{
		ArrayList<String> path = new ArrayList<String>();
		path.add(targetEdgeNode.edgeType);
		int parent = targetEdgeNode.parent;
		
		while(true)
		{
			EdgeNode tempNode = synsetIdEdgeNodeMap.get(parent);
			if(tempNode.parent == -1)
			{
				break;
			}
			else
			{
				path.add(tempNode.edgeType);
				parent = tempNode.parent;
			}
		}
		return path;
	}

	/**
	 * @param nodeMap (index, edge node) representing a searching path
	 * @param node
	 * @return
	 */
	public static int getPathLen(HashMap<Integer, EdgeNode> nodeMap, EdgeNode node)
	{
		if(node.parent == -1)
		{
			return 0;
		}
		
		int count = 0;
		int parent = node.parent;	
		while(true)  // keep tracking the father nodes until the path length is counted
		{
			EdgeNode tempNode = nodeMap.get(parent);
			count++;
			if(tempNode.parent == -1)
			{
				break;
			}
			else
			{
				parent = tempNode.parent;
			}
		}
		return count;
	}

	/**
	 * @param synsetIdEdgeNodeMap
	 * @param targetNode
	 * @return a formatted string
	 * This method print the path after completing the path and score.
	 */
	public static String printPath(HashMap<Integer, EdgeNode> synsetIdEdgeNodeMap, EdgeNode targetNode)
	{
		String pathResult = "";
		ArrayList<String> path = new ArrayList<String>();
		path.add(targetNode.edgeType);
		int parent = targetNode.parent;
//		System.out.print(targetNode.edgeNodeSynsetID + ":" + targetNode.currentScore + " ");
//		pathResult += targetNode.edgeNodeSynsetID + ":" + targetNode.currentScore;
		pathResult += targetNode.edgeNodeSynsetID;
		pathResult += " - " + targetNode.edgeType + " - ";
		while(true)
		{
			EdgeNode tempNode = synsetIdEdgeNodeMap.get(parent);
//			System.out.print(tempNode.edgeNodeSynsetID + ":" + tempNode.currentScore + " ");
//			pathResult += tempNode.edgeNodeSynsetID + ":" + tempNode.currentScore;
			pathResult += tempNode.edgeNodeSynsetID;
			if(tempNode.parent == -1)
			{
				break;
			}
			else
			{
				path.add(tempNode.edgeType);
				parent = tempNode.parent;
				pathResult += " - " + tempNode.edgeType + " - ";
			}
		}
		
//		System.out.print(targetNode.currentScore + "\n");
//		for(String edge : path)
//		{
//			System.out.print(edge + " ");
//		}
//		System.out.print("\n");
		return pathResult;
	}
}
