package main.java.edu.stonybrook.cs.computation;

import main.java.edu.stonybrook.cs.utils.BabelNetShareResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.PriorityQueue;

/**
 * This class a part of Bidirectional Breadth Search (from role to role-filler) to calculate the score.
 */
public class RoleToFillerThread extends Thread{
	private String source = null;
	private String[] destinations = null;
	private String pairKey = null;
	private String parameterKey = null;
	private HashMap<String, Integer> destIndexMap = new HashMap<String, Integer>(); // (synset of the role-filler, index)
	HashMap<String, Double> synsetIdScoreMap = new HashMap<String, Double>(); // (synset Id, score)
	HashMap<Integer, EdgeNode> synsetIdEdgeNodeMap = new HashMap<Integer, EdgeNode>(); // (synset Id, edge-node)
	PriorityQueue<EdgeNode> queue = 
			new PriorityQueue<EdgeNode>(100, (a,b) -> (int)Math.ceil(b.currentScore - a.currentScore)); // (capacity, comparator)
	private double cutOffVal = 0.005;
	private int depthLimit = 7;
	private ArrayList<String> prohibitedEdgeList = null;
	private int id = 0;

	/**
	 * @param parameterKey "{synsets of the role}"
	 * @param pairKey "{role-filler} : {synsets of the role}"
	 * @param roleNameSID a single synset of the role
	 * @param fillerSIDs synsets of the role-filler
	 * @param prohibitedEdgeList prohibited edge list of the role
	 */
	public RoleToFillerThread(String parameterKey, String pairKey, String roleNameSID, String[] fillerSIDs, ArrayList<String> prohibitedEdgeList) {
		this.parameterKey = parameterKey;
		this.source = roleNameSID;
		this.destinations = fillerSIDs;
		this.pairKey = pairKey;
		int destIndex = 0;
		for(String sid : this.destinations) {
			destIndexMap.put(sid, destIndex); // set a map to store (synset of the role-filler, index)
			destIndex++;
		}
		this.prohibitedEdgeList = prohibitedEdgeList;
	}

	/**
	 * This method perform breadth first search and compute the score.
	 */
	@Override
	public void run() {
		EdgeNode node = new EdgeNode();
		node.edgeNodeSynsetID = source; // the source is a synset of the role, expand the search from the source
		node.nodeID = id;
		id++;
		synsetIdEdgeNodeMap.put(node.nodeID, node); // this map actually saves the pairs to be added to the queue
		queue.add(node);
		
		while (!queue.isEmpty()) {
			EdgeNode currentFEValEdgeNode = queue.poll(); // get the first element in the queue and dequeue it
			double globalVal = BabelNetShareResource.getGlobalVal(pairKey);
			
			if(currentFEValEdgeNode.currentScore < globalVal && currentFEValEdgeNode.currentScore > 0.00000001 &&
					BabelNetShareResource.getPathLen(synsetIdEdgeNodeMap, currentFEValEdgeNode) > 2) {
				continue;
			}
			
			ArrayList<EdgeNode> adjacentEdgeNodeList = BabelNetShareResource.getEdgeNodes(parameterKey, currentFEValEdgeNode.edgeNodeSynsetID); // get all the edge nodes of the given synset of the role-filler
			for (EdgeNode targetNode : adjacentEdgeNodeList) { // for every adjacent node
				assert targetNode.edgeNodeSynsetID != null;
				assert targetNode.edgeWeight > 0.0;
				assert targetNode.edgeType != null;
				assert targetNode.parent == -1;
				assert targetNode.nodeID == -1;
				
				targetNode.parent = currentFEValEdgeNode.nodeID; // the father of the adjacent node is the given node
				targetNode.nodeID = id;
				id++;
				
				globalVal = BabelNetShareResource.getGlobalVal(pairKey);
				if (targetNode.edgeNodeSynsetID.equals(source) || targetNode.edgeNodeSynsetID.equals("bn:02248101n") ||
						isProhibitedEdge(targetNode.edgeType)) {
					continue; // if source and the target one is the same synset or the target one has a strange meaning or the edge is prohibited, check next adjacent node
				} 
				else if(destIndexMap.containsKey(targetNode.edgeNodeSynsetID)) {  // the adjacent node is one of the synsets of the role-filler
					targetNode.semConnectionNum = // Sem means semantic
							BabelNetShareResource.getSemanticConnectionNum(targetNode.edgeNodeSynsetID); // the sqrt of the number of the adjacent synsets
					targetNode.totalWeightedCount = targetNode.semConnectionNum
							* targetNode.edgeWeight + synsetIdEdgeNodeMap.get(currentFEValEdgeNode.nodeID).totalWeightedCount; // the score of the target node is the score of the current node + edge sqrt number * edge weight
					ArrayList<String> path = BabelNetShareResource.getPath(synsetIdEdgeNodeMap, targetNode); // get a list of synset Id strings for path
					targetNode.currentScore = BabelNetShareResource.computeSemanticScore(parameterKey, targetNode.totalWeightedCount, path); // compute the final score
					BabelNetShareResource.setGlobalVal(pairKey, targetNode.currentScore); // update the final score and save
					String tmpAncestorPath = BabelNetShareResource.printPath(synsetIdEdgeNodeMap, targetNode); // print the path
					if(targetNode.currentScore > cutOffVal) {
						BabelNetShareResource.setScore(pairKey, destIndexMap.get(targetNode.edgeNodeSynsetID), targetNode.currentScore, tmpAncestorPath);
					}
				} else {  // if the final destination is still not found
					targetNode.semConnectionNum =
							BabelNetShareResource.getSemanticConnectionNum(targetNode.edgeNodeSynsetID); // the same as the above block
					targetNode.totalWeightedCount = targetNode.semConnectionNum
							* targetNode.edgeWeight + synsetIdEdgeNodeMap.get(currentFEValEdgeNode.nodeID).totalWeightedCount;
					ArrayList<String> path = BabelNetShareResource.getPath(synsetIdEdgeNodeMap, targetNode); // the same as the above block
					targetNode.currentScore = BabelNetShareResource.computeSemanticScore(parameterKey, targetNode.totalWeightedCount, path); // compute the score up till now
					if ((targetNode.currentScore >= globalVal && targetNode.currentScore > cutOffVal &&
							path.size() < depthLimit) || (BabelNetShareResource.getPathLen(synsetIdEdgeNodeMap, targetNode) <= 2)) {
						if(synsetIdScoreMap.containsKey(targetNode.edgeNodeSynsetID)) {
							Double tempScore = synsetIdScoreMap.get(targetNode.edgeNodeSynsetID);
							if(tempScore < targetNode.currentScore) {  // if the stored one is less than the newly updated one
								queue.add(targetNode); // add to the queue
								synsetIdScoreMap.put(targetNode.edgeNodeSynsetID, targetNode.currentScore);
								synsetIdEdgeNodeMap.put(targetNode.nodeID, targetNode);
							}
						} else {// directly add to the queue
							queue.add(targetNode);
							synsetIdScoreMap.put(targetNode.edgeNodeSynsetID, targetNode.currentScore);
							synsetIdEdgeNodeMap.put(targetNode.nodeID, targetNode);
						}
					}
				}
			}
		}
	}

	/**
	 * @param edge in string
	 * @return if the edge is prohibited (hypernym...) then true
	 */
	private boolean isProhibitedEdge(String edge) {
		if(prohibitedEdgeList == null) {
			return false;
		}
		for(String tmp : prohibitedEdgeList) {
			if(edge.equals(tmp)) {
				return true;
			}
		}
		return false;
	}
}
