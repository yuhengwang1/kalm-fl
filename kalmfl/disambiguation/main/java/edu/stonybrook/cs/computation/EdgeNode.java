package main.java.edu.stonybrook.cs.computation;

/**
 * This class defines an (edge, node) pair
 */
public class EdgeNode {
	public String edgeNodeSynsetID = null;
	public Double edgeWeight = 0.0;
	public String edgeType = null;
	public Double totalWeightedCount = 0.0; // the numerator of the score computation
	public Double semConnectionNum = 0.0; // the number of adjacent synset nodes
	public int parent = -1; // the nodeId of its parent
	public Double currentScore = 0.0; // the score computed till this node
	public int nodeID = -1;
	
	public EdgeNode(EdgeNode e)
	{
		this.edgeNodeSynsetID = e.edgeNodeSynsetID;
		this.edgeWeight = e.edgeWeight;
		this.edgeType = e.edgeType;
		this.totalWeightedCount = e.totalWeightedCount;
		this.semConnectionNum = e.semConnectionNum;
		this.parent = e.parent;
		this.currentScore = e.currentScore;
	}
	
	public EdgeNode()
	{
		
	}
}
