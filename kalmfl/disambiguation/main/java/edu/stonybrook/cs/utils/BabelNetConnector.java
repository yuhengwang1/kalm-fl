package main.java.edu.stonybrook.cs.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import it.uniroma1.lcl.babelnet.BabelNet;
import it.uniroma1.lcl.babelnet.BabelSynset;
import it.uniroma1.lcl.babelnet.BabelSynsetID;
import it.uniroma1.lcl.babelnet.BabelSynsetIDRelation;
import it.uniroma1.lcl.babelnet.InvalidBabelSynsetIDException;
import it.uniroma1.lcl.babelnet.data.BabelGloss;
import it.uniroma1.lcl.babelnet.data.BabelPOS;
import it.uniroma1.lcl.babelnet.data.BabelPointer;
import it.uniroma1.lcl.jlt.util.Language;

/**
 * This is a util class to connect BabelNet and request data.
 */
public class BabelNetConnector {
	private static String rootSID = "bn:00031027n"; // entity: That which is perceived or known or inferred to have its own distinct existence (living or nonliving)
	private static final int depthLimit = 13; // depth
	private static BabelNet bn = BabelNet.getInstance();

	/**
	 * @param word word of who or where.
	 * @return a list of synsets of who or where.
	 */
	public static List<BabelSynset> getBabelNetSynsetsForQuery(String word) {
		List<BabelSynset> list = new ArrayList<BabelSynset>();
		try {
			if(word.equals("who")) // add two meanings of who in the list
			{
				BabelSynset by1 = bn.getSynset(new BabelSynsetID("bn:00046516n")); // individual: human being
				BabelSynset by2 = bn.getSynset(new BabelSynsetID("bn:00059480n")); // organization: a group of people who work together
				list.add(by1);
				list.add(by2);
			}
			else if(word.equals("where")) // add two meanings of where in the list
			{
				BabelSynset by1 = bn.getSynset(new BabelSynsetID("bn:00066884n")); // region: A large indefinite location on the surface of the Earth
				BabelSynset by2 = bn.getSynset(new BabelSynsetID("bn:15769800n")); // indoor: Space inside
				list.add(by1);
				list.add(by2);
			}
		} catch (IOException | InvalidBabelSynsetIDException e) {
			e.printStackTrace();
		}
		return list;
	}

	/**
	 * @param word the word
	 * @param pos part-of-speech
	 * @return a list of synsets
	 */
	public static List<BabelSynset> getBabelNetSynsetsByWord(String word, String pos) {
		List<BabelSynset> list = null;
		try {
			list = bn.getSynsets( word, Language.EN, getPOSTag(pos));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return list;
	}

	public static HashMap<String, String> getSynsetIdsAndGlossesByWord(String word, String pos) {
		List<BabelSynset> list;
		HashMap<String, String> idGlossMap = new HashMap<>();
		try {
			list = bn.getSynsets(word, Language.EN, getPOSTag(pos));
			for (BabelSynset synset : list) {
				idGlossMap.put(synset.getId().getID(), getMainGloss(synset));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return idGlossMap;
	}

	/**
	 * @param bs synset
	 * @return its main gloss
	 */
	public static String getMainGloss(BabelSynset bs) {
		String mainGloss = null;
		try {
			List<BabelGloss> glosses = bs.getGlosses(Language.EN);  // get all glosses of this synset
			if (glosses.size() == 0) {  // if got nothing, return null
				return "";
			} else {
				mainGloss = bs.getMainGloss(Language.EN).getGloss();  // else return the main gloss
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return mainGloss;
	}

	/**
	 * @param babelSynsetID an Id in string
	 * @return the synset with that Id
	 */
	public static BabelSynset getSynsetById(String babelSynsetID) {
		BabelSynset synset = null;
		try {
			synset = bn.getSynset(new BabelSynsetID(babelSynsetID));
		} catch (IOException | InvalidBabelSynsetIDException e) {
			e.printStackTrace();
		}
		return synset;
	}

	/**
	 * @param pos the POS read from candidate parse txt
	 * @return the POS object
	 */
	private static BabelPOS getPOSTag(String pos) {
		if (pos.equals("noun") || pos.equals("propn")) {
			return BabelPOS.NOUN;
		} else if (pos.equals("adj")) {
			return BabelPOS.ADJECTIVE;
		} else if (pos.equals("verb")) {
			return BabelPOS.VERB;
		} else {
			return BabelPOS.ADVERB;
		}
	}

	/**
	 * @param synset
	 * @return a boolean value to tell if the hypernym of a synset represents one's name (the synset is a name)
	 */
	public static boolean isGivenName(BabelSynset synset) {
		for(BabelSynsetIDRelation edge : synset.getEdges(BabelPointer.ANY_HYPERNYM)) {
			String hypernym = edge.getBabelSynsetIDTarget().getID();  // get the synset id of its hypernym
			if(hypernym.equals("bn:15930029n") || hypernym.equals("bn:15930045n")) // synsets of feminine name and boy's name accordingly
				return true;
        }
		return false;
	}

	/**
	 * @param synsetId in string
	 * @return a list of string representing Ids of the hyper synset of the given synset
	 */
	public static ArrayList<String> getHyperSynsets(String synsetId)
	{
		Set<String> hyperSynsetSet = new HashSet<String>();
		ArrayList<String> hyperList = new ArrayList<String>();
		ArrayList<String> keyHyperList = new ArrayList<String>();
		
		if(synsetId.equals(rootSID))
		{
			return hyperList; // an empty list
		}
		
		BabelSynset by;
		try {
			by = bn.getSynset(new BabelSynsetID(synsetId));
			for(BabelSynsetIDRelation edge : by.getEdges(BabelPointer.ANY_HYPERNYM)) {
				String targetSID = edge.getBabelSynsetIDTarget().getID();
				if(hyperSynsetSet.contains(targetSID))
				{
					continue;
				}
				else
				{		
					hyperList.add(targetSID);						
					hyperSynsetSet.add(targetSID);
					if(isKeyConcept(targetSID)) // if this synset is a key concept
					{
						keyHyperList.add(targetSID);
					}
				}           
	        }
		} catch (IOException | InvalidBabelSynsetIDException e) {
			e.printStackTrace();
		}
		if(keyHyperList.size() > 0)
		{
			return keyHyperList;
		}
		else
		{
			return hyperList;
		}
	}
	
	public static ArrayList<String> getHypoSynsets(String s1)
	{
		Set<String> set = new HashSet<String>();
		ArrayList<String> hypoList = new ArrayList<String>();
		ArrayList<String> keyHypoList = new ArrayList<String>();
		
		BabelSynset by;
		try {
			by = bn.getSynset(new BabelSynsetID(s1));
			for(BabelSynsetIDRelation edge : by.getEdges(BabelPointer.ANY_HYPONYM)) {
				String targetSID = edge.getBabelSynsetIDTarget().getID();
				if(set.contains(targetSID))
				{
					continue;
				}
				else
				{		
					hypoList.add(targetSID);
					set.add(targetSID);
					//if(IsKeyConcept(targetSID))
					{
						keyHypoList.add(targetSID);
					}
				}           
	        }
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidBabelSynsetIDException e) {
			e.printStackTrace();
		}
		if(keyHypoList.size() > 0)
		{
			return keyHypoList;
		}
		else
		{
			return hypoList;
		}
	}
	
	private static boolean isKeyConcept(String s1)
	{
		BabelSynset by = null;
		try {
			by = bn.getSynset(new BabelSynsetID(s1));
		} catch (IOException | InvalidBabelSynsetIDException e) {
			e.printStackTrace();
		}
		return by.isKeyConcept();
	}
	
	public static ArrayList<ArrayList<String>> getPathToRoot(String s1)
	{
		ArrayList<String> ancestorList = new ArrayList<String>();
		Set<String> ancestorSet = new HashSet<String>();
		ArrayList<ArrayList<String>> result = new ArrayList<ArrayList<String>>();
		ancestorList.add(s1);
		ancestorSet.add(s1);
		getPathToTargetHelper(s1, rootSID, ancestorList, ancestorSet, result);
		return result;
	}
	
	public static ArrayList<ArrayList<String>> getPathToTarget(String from, String to)
	{
		ArrayList<String> ancestorList = new ArrayList<String>();
		Set<String> ancestorSet = new HashSet<String>();
		ArrayList<ArrayList<String>> result = new ArrayList<ArrayList<String>>();
		ancestorList.add(from);
		ancestorSet.add(from);
		getPathToTargetHelper(from, to, ancestorList, ancestorSet, result);
		return result;
	}

	/**
	 * @param from
	 * @param to
	 * @param ancestorList
	 * @param ancestorSet
	 * @param result
	 * No method invokes it.
	 */
	public static void getPathToTargetHelper(String from, String to, ArrayList<String> ancestorList,
											 Set<String> ancestorSet, ArrayList<ArrayList<String>> result)
	{
		if(from.equals(to))
		{
			ArrayList<String> newAncestorList = new ArrayList<String>();
			for(String sid : ancestorList)
			{
				newAncestorList.add(sid);
			}
			result.add(newAncestorList);
			Set<String> tempSet = new HashSet<String>();
			for(String sid : ancestorList)
			{
				assert tempSet.contains(sid) == false;
				tempSet.add(sid);
				System.out.print(sid + " ");
			}
			System.out.print("\n");
			return;
		}
			
		if(result.size() > 0 && ancestorList.size() >= depthLimit)
		{
			return;
		}	
		
		ArrayList<String> hyperList = BabelNetConnector.getHyperSynsets(from);
		for(String sid : hyperList)
		{
			if(ancestorSet.contains(sid))
			{
				continue;
			}
			else
			{
				ancestorList.add(sid);
				ancestorSet.add(sid);
				getPathToTargetHelper(sid, to, ancestorList, ancestorSet, result);
				ancestorList.remove(ancestorList.size()-1);
				ancestorSet.remove(sid);
			}
		}	
	}
	
	public static int getAllPaths(String from, String to)
	{		
		int count = 0;
		Set<String> set = new HashSet<String>();
		Queue<String> queue = new LinkedList<String>();
		queue.add(from);
		int depth = 0, curLevel = 1, nextLevel = 0;
		while (!queue.isEmpty() && depth < depthLimit) {
			String temp = queue.poll();
			BabelSynset by = null;
			try {
				by = bn.getSynset(new BabelSynsetID(temp));
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InvalidBabelSynsetIDException e) {
				e.printStackTrace();
			}
			assert by != null;
			for(BabelSynsetIDRelation edge : by.getEdges()) {
				String targetSID = edge.getBabelSynsetIDTarget().getID();
				if(set.contains(targetSID))
				{
					continue;
				}
				else if (targetSID.equals(rootSID))
				{
					continue;
				}
				else if(targetSID.equals(to))
				{
//					System.out.println(edge.getPointer());
					count++;
				}
				else
				{								
					set.add(targetSID);
					queue.add(targetSID);
					nextLevel++;
				}           
	        }
			curLevel--;
			if (curLevel == 0)
			{
				curLevel = nextLevel;
				nextLevel = 0;
				depth++;
			}
		}			
		return count;
	}
	
	public static ArrayList<String> getShortestPath(String from, String to)
	{	
		class Node {
			String parentVal;
		}
		
		ArrayList<String> path = new ArrayList<String>();
		Set<String> set = new HashSet<String>();
		HashMap<String, Node> nodeMap = new HashMap<String, Node>();
		Queue<String> queue = new LinkedList<String>();
		Node node = new Node();
		node.parentVal = null;
		queue.add(from);
		nodeMap.put(from, node);
		set.add(from);
		int depth = 0, curLevel = 1, nextLevel = 0;
		while (!queue.isEmpty() && depth < depthLimit) {
			String temp = queue.poll();
			BabelSynset by = null;
			try {
				by = bn.getSynset(new BabelSynsetID(temp));
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InvalidBabelSynsetIDException e) {
				e.printStackTrace();
			}
			assert by != null;
			for(BabelSynsetIDRelation edge : by.getEdges()) {
				String targetSID = edge.getBabelSynsetIDTarget().getID();
				if(set.contains(targetSID))
				{
					continue;
				}
				else if (targetSID.equals(rootSID))
				{
					continue;
				}
				else if(targetSID.equals(to))
				{
					path.add(targetSID);
					path.add(temp);
					String parent = temp;
					while(true)
					{
						Node tempNode = nodeMap.get(parent);
						if(tempNode.parentVal == null)
						{
							break;
						}
						else
						{
							parent = tempNode.parentVal;
							path.add(parent);
						}
					}
					return path;
				}
				else
				{								
					set.add(targetSID);
					Node t2 = new Node();
					t2.parentVal = temp;
					queue.add(targetSID);
					nodeMap.put(targetSID, t2);
					nextLevel++;
				}           
	        }
			curLevel--;
			if (curLevel == 0)
			{
				curLevel = nextLevel;
				nextLevel = 0;
				depth++;
			}
		}			
		return path;
	}
	
	public static ArrayList<String> getShortestHyperPath(String from, String to)
	{	
		class Node {
			String parentVal;
		}
		
		ArrayList<String> path = new ArrayList<String>();
		Set<String> set = new HashSet<String>();
		HashMap<String, Node> nodeMap = new HashMap<String, Node>();
		Queue<String> queue = new LinkedList<String>();
		Node node = new Node();
		node.parentVal = null;
		queue.add(from);
		nodeMap.put(from, node);
		set.add(from);
		int depth = 0, curLevel = 1, nextLevel = 0;
		while (!queue.isEmpty() && depth < depthLimit) {
			String temp = queue.poll();
			ArrayList<String> parents = BabelNetConnector.getHyperSynsets(temp);
			for(String targetSID : parents) {
				if(set.contains(targetSID))
				{
					continue;
				}
				else if (targetSID.equals(rootSID))
				{
					continue;
				}
				else if(targetSID.equals(to))
				{
					path.add(targetSID);
					path.add(temp);
					String parent = temp;
					while(true)
					{
						Node tempNode = nodeMap.get(parent);
						if(tempNode.parentVal == null)
						{
							break;
						}
						else
						{
							parent = tempNode.parentVal;
							path.add(parent);
						}
					}
					return path;
				}
				else
				{								
					set.add(targetSID);
					Node t2 = new Node();
					t2.parentVal = temp;
					queue.add(targetSID);
					nodeMap.put(targetSID, t2);
					nextLevel++;
				}           
	        }
			curLevel--;
			if (curLevel == 0)
			{
				curLevel = nextLevel;
				nextLevel = 0;
				depth++;
			}
		}			
		return path;
	}

	/**
	 * @param sid synset Id in string
	 * @return the number of the adjacent synsets of the given one.
	 */
	public static int getSemanticConnectionNum(String sid)
	{
		BabelSynset by = null;
		try {
			by = bn.getSynset(new BabelSynsetID(sid));
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidBabelSynsetIDException e) {
			e.printStackTrace();
		}
		return by.getEdges().size();
	}

	/**
	 * @param from
	 * @param to
	 * @return
	 * No method invokes it.
	 */
	public static int getDirectedTriangleNum(String from, String to)
	{
		int count = 1;
		BabelSynset toSynset = null;
		try {
			toSynset = bn.getSynset(new BabelSynsetID(to));
			for(BabelSynsetIDRelation edge : toSynset.getEdges())
			{
				String tempSID = edge.getBabelSynsetIDTarget().getID();
				BabelSynset tempSynset = bn.getSynset(new BabelSynsetID(tempSID));
				for(BabelSynsetIDRelation newEdge : tempSynset.getEdges())
				{
					if(newEdge.getBabelSynsetIDTarget().getID().equals(from))
					{
						count++;
					}
				}
			}
		} catch (IOException e) {			
			e.printStackTrace();
		} catch (InvalidBabelSynsetIDException e) {		
			e.printStackTrace();
		}
		return count;
	}
	
	public static double getEdgeWeight(String from, String to)
	{
		BabelSynset fromSynset = null;
		Set<String> set = new HashSet<String>();
		int numerator = 0;
		int denominator = 0;
		try {
			fromSynset = bn.getSynset(new BabelSynsetID(from));
			for(BabelSynsetIDRelation edge : fromSynset.getEdges())
			{
				String temp = edge.getBabelSynsetIDTarget().getID();
				if(set.contains(temp))
				{
					continue;
				}
				else
				{
					set.add(temp);
					int num = getSemanticConnectionNum(temp);
					numerator += num;
					if(temp.equals(to))
					{
						denominator = num;
					}
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidBabelSynsetIDException e) {
			e.printStackTrace();
		}
		
		if(denominator == 0)
		{
			try {
				BabelSynset toSynset = bn.getSynset(new BabelSynsetID(to));
				denominator = toSynset.getEdges().size();
				numerator += denominator;
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InvalidBabelSynsetIDException e) {
				e.printStackTrace();
			}
		}
		
		double score = 1.0 * denominator/numerator;
		return Math.log10(score);
	}
	
	public static double getEdgeWeight2(String from, String to)
	{
		BabelSynset fromSynset = null;
		Set<String> set = new HashSet<String>();
		int numerator = 0;
		int denominator = 0;
		try {
			fromSynset = bn.getSynset(new BabelSynsetID(from));
			for(BabelSynsetIDRelation edge : fromSynset.getEdges())
			{
				String temp = edge.getBabelSynsetIDTarget().getID();
				if(set.contains(temp))
				{
					continue;
				}
				else
				{
					set.add(temp);
					int num = getDirectedTriangleNum(from, temp);
					numerator += num;
					if(temp.equals(to))
					{
						denominator = num;
					}
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidBabelSynsetIDException e) {
			e.printStackTrace();
		}
		
		if(denominator == 0)
		{							
			denominator = getDirectedTriangleNum(from, to);
			numerator += denominator;			
		}
//		System.out.println(denominator + " " + numerator);
		double score = 1.0 * denominator/numerator;
		return Math.log10(score);
	}
	
	public static double getWeight3(String from, String to)
	{	
		class Node {
			String parentVal;
			int total;
		}
		
		boolean flag = false;
		double val = 0;
		Set<String> set = new HashSet<String>();
		HashMap<String, Node> nodeMap = new HashMap<String, Node>();
		Queue<String> queue = new LinkedList<String>();
		Node node = new Node();
		node.parentVal = null;
		node.total = getSemanticConnectionNum(from);
		queue.add(from);
		nodeMap.put(from, node);
		set.add(from);
		int depth = 0, curLevel = 1, nextLevel = 0;
		while (!queue.isEmpty() && depth < depthLimit) {
			String temp = queue.poll();
			ArrayList<String> parents = BabelNetConnector.getHyperSynsets(temp);
			//ArrayList<String> hypoList = BabelNetConnector.GetHypoSynsets(temp);
			//parents.addAll(hypoList);
			for(String targetSID : parents) {
				if(set.contains(targetSID))
				{
					continue;
				}
				else if (targetSID.equals(rootSID))
				{
					continue;
				}
				else if(targetSID.equals(to))
				{
					int tempTotal = nodeMap.get(temp).total + getSemanticConnectionNum(targetSID);
					System.out.print(targetSID + ":" + tempTotal + " ");
					double tempVal = computeSemanticScore(tempTotal, depth + 1);//1.0 * tempVal/ Math.pow(1.0*(depth + 1),2);
					flag = true;
					if(tempVal > val)
					{
						val = tempVal;
					}
					String parent = temp;
					while(true)
					{
						Node tempNode = nodeMap.get(parent);
						System.out.print(parent + ":" + tempNode.total + " ");
						if(tempNode.parentVal == null)
						{
							break;
						}
						else
						{
							parent = tempNode.parentVal;
						}
					}
					System.out.print(tempVal + "\n");
				}
				else
				{								
					Node t2 = new Node();
					t2.parentVal = temp;
					t2.total = nodeMap.get(temp).total + getSemanticConnectionNum(targetSID);
					if(flag == false)
					{
						set.add(targetSID);
						queue.add(targetSID);
						nodeMap.put(targetSID, t2);
						nextLevel++;
					}
					else
					{
						double tempVal = computeSemanticScore(t2.total, depth + 1);//t2.total*1.0 / Math.pow(1.0*(depth + 1), 2);
						if(tempVal >= val)
						{
							set.add(targetSID);
							queue.add(targetSID);
							nodeMap.put(targetSID, t2);
							nextLevel++;
						}
					}
				}           
	        }
			curLevel--;
			if (curLevel == 0)
			{
				curLevel = nextLevel;
				nextLevel = 0;
				depth++;
			}
		}
//		System.out.println("max: " + val);
		return val;
	}

	/**
	 * @param semanticConnectionNum
	 * @param depth
	 * @return
	 * No method invokes it.
	 */
	private static double computeSemanticScore(int semanticConnectionNum, int depth)
	{
		return semanticConnectionNum*1.0/Math.pow(1.0*depth, 2);
	}
}
