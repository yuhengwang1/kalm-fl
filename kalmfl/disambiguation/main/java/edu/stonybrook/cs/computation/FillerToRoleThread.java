package main.java.edu.stonybrook.cs.computation;

import main.java.edu.stonybrook.cs.utils.BabelNetShareResource;

import java.util.*;

/**
 * This class a part of Bidirectional Breadth Search (from role-filler to role) to calculate the score.
 */
public class FillerToRoleThread extends Thread
{
	private String source = null;
	private String[] destinations = null;
	private HashSet<String> destinationSet = new HashSet<String>();
	HashMap<String, Double> nodeScoreMap = new HashMap<String, Double>();
	HashMap<Integer, EdgeNode> nodeMap = new HashMap<Integer, EdgeNode>();
	PriorityQueue<EdgeNode> queue = 
			new PriorityQueue<EdgeNode>(100, (a,b) -> (int)Math.ceil(b.currentScore - a.currentScore));
	private String pairKey = null;
	private String ancestorPath = "";
	private double val = 0.0;
	private int sourceIndex = -1;
	private double cutOffVal = 0.005;
	private int depthLimit = 7;
	private ArrayList<String> prohibitedEdgeList = null;
	private String parameterKey = null;
	private int id = 0;

	/**
	 * @param pairKey "{role-filler} : {synsets of the role}"
	 * @param sourceIndex i-th synset of the role-filler
	 * @param fillerSID a single synset of the role-filler
	 * @param roleNameSIDs synsets of the role
	 * @param prohibitedEdgeList prohibited edge list of the role
	 */
	public FillerToRoleThread(String pairKey, int sourceIndex, String fillerSID, String[] roleNameSIDs, ArrayList<String> prohibitedEdgeList)
	{
		source = fillerSID;
		destinations = roleNameSIDs;
		this.sourceIndex = sourceIndex;
		this.pairKey = pairKey;
		this.prohibitedEdgeList = prohibitedEdgeList;
		Collections.addAll(destinationSet, this.destinations);
		parameterKey = Arrays.toString(destinations);
	}
	
	@Override
	public void run() {
		EdgeNode node = new EdgeNode();
		node.edgeNodeSynsetID = source;  // a filler synset if
		node.semConnectionNum = BabelNetShareResource.getSemanticConnectionNum(source);
		node.nodeID = id;
		id++;
		nodeMap.put(node.nodeID, node);
		queue.add(node);  // add the filler synset to the queue, as the root
		
		while (!queue.isEmpty()) {
			EdgeNode curNode = queue.poll();
			double globalVal = BabelNetShareResource.getGlobalVal(pairKey);

			//  if the score up till curNode has a score and this score is less than global best
			if(curNode.currentScore < globalVal && curNode.currentScore > 0.00000001 && BabelNetShareResource.getPathLen(nodeMap, curNode) > 2) {
				continue;  // discard this node
			}
			
			ArrayList<EdgeNode> adjacentNodes = BabelNetShareResource.getEdgeNodes(parameterKey, curNode.edgeNodeSynsetID);  // if no cached, new EdgeNode by querying BabelNet
			for (EdgeNode nextNode : adjacentNodes) {
				assert nextNode.edgeNodeSynsetID != null;
				assert nextNode.edgeWeight > 0.0;
				assert nextNode.edgeType != null;
				assert nextNode.parent == -1;
				assert nextNode.nodeID == -1;
				
				nextNode.parent = curNode.nodeID;
				nextNode.nodeID = id;
				id++;  // id is the pointer of the queue
				
				globalVal = BabelNetShareResource.getGlobalVal(pairKey);
				if (nextNode.edgeNodeSynsetID.equals(source) || nextNode.edgeNodeSynsetID.equals("bn:02248101n") || isProhibitedEdge(nextNode.edgeType)) {
					continue;  // if the next node is the source or is a strange synset or the edge between them is prohibited
				} else if (destinationSet.contains(nextNode.edgeNodeSynsetID)) {  // if the next node is one of the target, update all the results if better
					nextNode.totalWeightedCount = nodeMap.get(curNode.nodeID).semConnectionNum  // connection score: sqrt #connections
							* nextNode.edgeWeight + nodeMap.get(curNode.nodeID).totalWeightedCount;  // edgeWeight stored in EdgeNode
					ArrayList<String> path = BabelNetShareResource.getPath(nodeMap, nextNode);
					nextNode.currentScore = BabelNetShareResource.computeSemanticScore(parameterKey, nextNode.totalWeightedCount, path);
					String tmpAncestorPath = BabelNetShareResource.printPath(nodeMap, nextNode);  // prepare the path to be prited
					if (nextNode.currentScore > val) {  // if the score reaches high
						val = nextNode.currentScore;  // update the highest score
						BabelNetShareResource.setGlobalVal(pairKey, val);
						ancestorPath = tmpAncestorPath;
					}
				} else {  // other cases that are on track to achieving the goal
					nextNode.semConnectionNum =
							BabelNetShareResource.getSemanticConnectionNum(nextNode.edgeNodeSynsetID);  // get connection score
					nextNode.totalWeightedCount = nodeMap.get(curNode.nodeID).semConnectionNum
							* nextNode.edgeWeight + nodeMap.get(curNode.nodeID).totalWeightedCount;  // add up new edge weight to the cumulative score
					ArrayList<String> path = BabelNetShareResource.getPath(nodeMap, nextNode);  // get the path to next node from source
					nextNode.currentScore = BabelNetShareResource.computeSemanticScore(parameterKey, nextNode.totalWeightedCount, path);  // get the current score
					if ((nextNode.currentScore >= globalVal && nextNode.currentScore > cutOffVal &&
							path.size() < depthLimit) || (BabelNetShareResource.getPathLen(nodeMap, nextNode) <= 2)) {
						if(nodeScoreMap.containsKey(nextNode.edgeNodeSynsetID)) { // if this next node is visited
							Double tempScore = nodeScoreMap.get(nextNode.edgeNodeSynsetID);  // get the score of last visit
							if(tempScore < nextNode.currentScore) {  // if current score is higher
								queue.add(nextNode);  // still add this visited node to the queue
								nodeScoreMap.put(nextNode.edgeNodeSynsetID, nextNode.currentScore);  // update score
								nodeMap.put(nextNode.nodeID, nextNode);
							}
						} else {
							queue.add(nextNode);
							nodeScoreMap.put(nextNode.edgeNodeSynsetID, nextNode.currentScore);
							nodeMap.put(nextNode.nodeID, nextNode);
						}
					}
				}
			}
		}			
//		System.out.println("max: " + val);
		if(val > cutOffVal) {
			BabelNetShareResource.setScore(pairKey, sourceIndex, val, ancestorPath);
		} else {
			BabelNetShareResource.setScore(pairKey, sourceIndex, 0.0, "");
		}
	}
	
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
