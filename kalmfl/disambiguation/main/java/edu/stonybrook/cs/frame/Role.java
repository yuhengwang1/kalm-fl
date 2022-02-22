package main.java.edu.stonybrook.cs.frame;

import java.util.*;

import it.uniroma1.lcl.babelnet.BabelSynset;
import main.java.edu.stonybrook.cs.correction.SynsetOverride;
import main.java.edu.stonybrook.cs.utils.BabelNetConnector;
import main.java.edu.stonybrook.cs.utils.BabelNetShareResource;
import main.java.edu.stonybrook.cs.computation.RoleToFillerThread;
import main.java.edu.stonybrook.cs.computation.FillerToRoleThread;

/**
 * This class represents a (role, role-filler) relation. This relation is able to create computation threads to get the
 * score of each (role synset, role-filler synset) pair and give a sorted list of role-filler synsets by score.
 */
public class Role {
	private final String roleName; // role name
	private final String[] roleNameSynsetIDs; // the synsets of the role
	private String roleNameSynsetGloss; // the main gloss of the role
	private String filler; // role-filler
	private String fillerPOS; // the part-of-speech of the role-filler
	private String fillerIndex;
	private String fillerClass;
	private String fillerQuant; // the quantity of the role-filler
	private String fillerBestSynsetID; // the specified role-filler synsetID
	private String fillerBestSynsetGloss; // the specified role-filler score
	private Double fillerBestSynsetScore; // the specified role-filler score
	private String pairKey; // key of (role, filler), "role-filler:{synsets of the role}", this is needed because we want to know if the threads brought by this pair is added
	private List<String> fillerSynsetIDs; // synsets of the role-filler, it is a sorted list by score
	private List<String> fillerSynsetGlosses; // glosses of the role-filler, it is a sorted list score
	private List<ScoreTriplet> fillerScoreTriplets; // (synsetID, gloss, score)
	private List<ScoreTriplet> exactMatchFillerScoreTriplets;
	private String roleDataType; // data type of the role
	private Hashtable<String,String> synsetIdGlossMap = new Hashtable<String,String>(); // table storing (role-filler synsetID, gloss)
	private Hashtable<String,Double> synsetIdScoreMap = new Hashtable<String,Double>(); // table storing (role-filler synsetID, score)
	private Hashtable<String,String> synsetIdPathMap = new Hashtable<String,String>(); // table storing (role-filler synsetID, path in string)
	private HashSet<String> roleNameSynsetIDSet = new HashSet<String>(); // set storing (role name synsetIDs)
	private ArrayList<String> prohibitedEdgeList; // the role cannot be reached by the edges of such type
	private boolean hasExactMatch;
	private final double defaulScore = 1.2;
	private final HashSet<String> personPronouns = new HashSet<String>(){{
		add("i");
		add("you");
		add("he");
		add("she");
		add("we");
	}};
	private final HashSet<String> entityPronouns = new HashSet<String>(){{
		add("they");
		add("it");
		add("this");
		add("that");
		add("these");
		add("those");
	}};

	public static class ScoreTriplet {
		public String fillerSynsetID = null;
		public String fillerGloss = null;
		public double score = 0.0;
	}

	/**
	 * @param roleName
	 * @param roleNameSynsetIDs
	 * (role, role-filler) relation constructing called by LoadFrame by FrameExtractor.
	 */
	public Role(String roleName, String[] roleNameSynsetIDs) // role name and the synsets of the role name
	{
		this.roleName = roleName;
		this.roleNameSynsetIDs = roleNameSynsetIDs;
		this.filler = null;
		this.fillerPOS = null;
		this.fillerIndex = null;
		this.fillerClass = null;
		this.fillerQuant = null;
		this.fillerBestSynsetID = null;
		this.fillerBestSynsetGloss = null;
		this.fillerBestSynsetScore = 0.0;
		this.fillerSynsetIDs = new ArrayList<String>();
		this.fillerSynsetGlosses = new ArrayList<String>();
		if (!roleName.equals("Time")) {
			BabelSynset synset = BabelNetConnector.getSynsetById(roleNameSynsetIDs[0]); // get the first synset of the role
			this.roleNameSynsetGloss = BabelNetConnector.getMainGloss(synset); // get the gloss of the first synset
		}
		this.roleDataType = null;
		this.pairKey = null;
		this.fillerScoreTriplets = new ArrayList<ScoreTriplet>();
		this.exactMatchFillerScoreTriplets = new ArrayList<ScoreTriplet>();
		this.prohibitedEdgeList = null;
		this.hasExactMatch = false;

		Collections.addAll(roleNameSynsetIDSet, roleNameSynsetIDs);
	}

	/**
	 * Default display of the dropdown menu, the first synset of the role-filler with the highest score since
	 * the list is sorted.
	 */
	public void setBestResult() {
		if(fillerSynsetIDs.size() > 0) {
			this.fillerBestSynsetID = fillerSynsetIDs.get(0);
			this.fillerBestSynsetGloss = fillerSynsetGlosses.get(0);
			this.fillerBestSynsetScore = synsetIdScoreMap.get(fillerBestSynsetID);
		}
	}

	/**
	 * @param FEValSynsetId the selected synset Id in the dropdown menu
	 * If the synset Id is changed, the relevant data to be showcased changes accordingly.
	 */
	public void changeFEValSynsetId(String FEValSynsetId)
	{
		this.fillerBestSynsetID = FEValSynsetId;
		this.fillerBestSynsetGloss = synsetIdGlossMap.get(FEValSynsetId);
		this.fillerBestSynsetScore = synsetIdScoreMap.get(FEValSynsetId);
	}

	/**
	 * @return a list of threads
	 * This method creates computation threads of the bidirectional search. Multiple roles vs multiple role-fillers,
	 * each (role, role-filler) and (role-filler, role) is a thread.
	 */
	public List<Thread> createSematicScoreComputationThreads() {
		if (roleName.equals("Time")) {
			fillerSynsetIDs.add("time");
			fillerSynsetGlosses.add("a time value");
			synsetIdScoreMap.put("time", 1.0);
			synsetIdGlossMap.put("time", "a time value");
			synsetIdPathMap.put("time", "");
			return null;
		}

		if (roleDataType != null && roleDataType.equals("Integer")) {
			fillerSynsetIDs.add("integer");
			fillerSynsetGlosses.add("an integer");
			synsetIdScoreMap.put("integer", defaulScore);
			synsetIdGlossMap.put("integer", "an integer");
			synsetIdPathMap.put("integer", "");
			return null;
		}

		if (roleDataType != null && roleDataType.equals("Currency")) {
			fillerSynsetIDs.add("currency");
			fillerSynsetGlosses.add("some currency");
			synsetIdScoreMap.put("currency", defaulScore);
			synsetIdGlossMap.put("currency", "some currency");
			synsetIdPathMap.put("currency", "");
			return null;
		}

		List<BabelSynset> fillerSynsets = BabelNetConnector.getBabelNetSynsetsByWord(filler, fillerPOS); // get all the synsets of a role-filler

		int synsetSize = 0;
		for (BabelSynset synset : fillerSynsets) {
			String synsetId = synset.getId().getID();
			if (SynsetOverride.isSynsetOverridden(filler + "-" + synsetId)) continue;
			synsetSize++;
		}

		String changedFiller = filler;
		String[] splitFiller = filler.split(" ");
		if (synsetSize == 0) {
			if (splitFiller[0].equals("a") || splitFiller[0].equals("the")) {
				changedFiller = String.join(" ", Arrays.copyOfRange(splitFiller, 1, splitFiller.length));
				fillerSynsets = BabelNetConnector.getBabelNetSynsetsByWord(changedFiller, fillerPOS);
				for (BabelSynset synset : fillerSynsets) {
					String synsetId = synset.getId().getID();
					if (SynsetOverride.isSynsetOverridden(changedFiller + "-" + synsetId)) continue;
					synsetSize++;
				}
			}
			if (synsetSize == 0) {
				changedFiller = splitFiller[splitFiller.length-1];
				fillerSynsets = BabelNetConnector.getBabelNetSynsetsByWord(changedFiller, fillerPOS);
			}
		}

		for (BabelSynset synset : fillerSynsets) {  // iterate every synset of the role-filler, rewrite glosses
			String synsetId = synset.getId().getID();
			String mainGloss = BabelNetConnector.getMainGloss(synset); // get the main gloss of a synset, only one

			if (SynsetOverride.isSynsetOverridden(changedFiller + "-" + synsetId)) continue;  // if word-synset relation is overridden, check next synset


			if (mainGloss != null && mainGloss.length() != 0) {  // if this synset has a main gloss
				if (synset.isKeyConcept()) {  // if a synset is a key concept (something related to FrameNet according to BabelNet.org)
					if (fillerPOS.equals("propn")) continue;// and POS is a name, check next synset
				} else if (synset.getSynsetType().name().equals("NAMED_ENTITY")) {  // the synset means a name
					if (!fillerPOS.equals("propn") && !filler.equals("cooking school" ) && !filler.equals("us intelligence") && !filler.equals("us history") && !filler.equals("us election") && !filler.equals("kyoto protocol")) continue; // but POS is not a name (stanza conflicts with babelnet), check next synset
				}
				//  else {
				// 	if (fillerPOS.equals("propn")) continue;// if it is a name
				// }
			}

			synsetIdGlossMap.put(synsetId, mainGloss); // generate synset-gloss table
			ScoreTriplet fillerScoreTriplet = new ScoreTriplet(); // which is an inner static class
			fillerScoreTriplet.fillerSynsetID = synsetId;
			fillerScoreTriplet.fillerGloss = mainGloss;
			fillerScoreTriplets.add(fillerScoreTriplet); // Tuple(synsetID, gloss, score)
		}

		if (entityPronouns.contains(filler)) {
			ScoreTriplet fillerScoreTriplet = new ScoreTriplet();
			fillerScoreTriplet.fillerSynsetID = "bn:00031027n";
			fillerScoreTriplet.fillerGloss = "That which is perceived or known or inferred to have its own distinct existence (living or nonliving)";
			synsetIdGlossMap.put(fillerScoreTriplet.fillerSynsetID, fillerScoreTriplet.fillerGloss);
			fillerScoreTriplets.add(fillerScoreTriplet);
		}

		if (fillerClass.equals("person") || personPronouns.contains(filler)) {
			ScoreTriplet fillerScoreTriplet = new ScoreTriplet();
			fillerScoreTriplet.fillerSynsetID = "bn:00046516n";
			fillerScoreTriplet.fillerGloss = "A human being";
			synsetIdGlossMap.put(fillerScoreTriplet.fillerSynsetID, fillerScoreTriplet.fillerGloss);
			fillerScoreTriplets.add(fillerScoreTriplet);
//			fillerScoreTriplet = new ScoreTriplet();
//			fillerScoreTriplet.fillerSynsetID = "bn:00044576n";
//			fillerScoreTriplet.fillerGloss = "Any living or extinct member of the family Hominidae characterized by superior intelligence, articulate speech, and erect carriage";
//			synsetIdGlossMap.put(fillerScoreTriplet.fillerSynsetID, fillerScoreTriplet.fillerGloss);
//			fillerScoreTriplets.add(fillerScoreTriplet);
		}

		if (fillerClass.equals("gpe")) {
			ScoreTriplet fillerScoreTriplet = new ScoreTriplet();
			fillerScoreTriplet.fillerSynsetID = "bn:00062699n";
			fillerScoreTriplet.fillerGloss = "A point located with respect to surface features of some region";
			synsetIdGlossMap.put(fillerScoreTriplet.fillerSynsetID, fillerScoreTriplet.fillerGloss);
			fillerScoreTriplets.add(fillerScoreTriplet);
			fillerScoreTriplet = new ScoreTriplet();
			fillerScoreTriplet.fillerSynsetID = "bn:00066884n";
			fillerScoreTriplet.fillerGloss = "A large indefinite location on the surface of the Earth";
			synsetIdGlossMap.put(fillerScoreTriplet.fillerSynsetID, fillerScoreTriplet.fillerGloss);
			fillerScoreTriplets.add(fillerScoreTriplet);
			fillerScoreTriplet = new ScoreTriplet();
			fillerScoreTriplet.fillerSynsetID = "bn:00051760n";
			fillerScoreTriplet.fillerGloss = "A point or extent in space";
			synsetIdGlossMap.put(fillerScoreTriplet.fillerSynsetID, fillerScoreTriplet.fillerGloss);
			fillerScoreTriplets.add(fillerScoreTriplet);
		}

		if (fillerClass.equals("work_of_art")) {
			ScoreTriplet fillerScoreTriplet = new ScoreTriplet();
			fillerScoreTriplet.fillerSynsetID = "bn:00077409n";
			fillerScoreTriplet.fillerGloss = "The name of a work of art or literary composition etc.";
			synsetIdGlossMap.put(fillerScoreTriplet.fillerSynsetID, fillerScoreTriplet.fillerGloss);
			fillerScoreTriplets.add(fillerScoreTriplet);
		}

		if (fillerClass.equals("org")) {
			ScoreTriplet fillerScoreTriplet = new ScoreTriplet();
			fillerScoreTriplet.fillerSynsetID = "bn:00059480n";
			fillerScoreTriplet.fillerGloss = "A group of people who work together";
			synsetIdGlossMap.put(fillerScoreTriplet.fillerSynsetID, fillerScoreTriplet.fillerGloss);
			fillerScoreTriplets.add(fillerScoreTriplet);
		}

		if (fillerClass.equals("norp")) {
			ScoreTriplet fillerScoreTriplet = new ScoreTriplet();
			fillerScoreTriplet.fillerSynsetID = "bn:00056964n";
			fillerScoreTriplet.fillerGloss = "The status of belonging to a particular nation by birth or naturalization";
			synsetIdGlossMap.put(fillerScoreTriplet.fillerSynsetID, fillerScoreTriplet.fillerGloss);
			fillerScoreTriplets.add(fillerScoreTriplet);
			// fillerScoreTriplet = new ScoreTriplet();
			// fillerScoreTriplet.fillerSynsetID = "bn:00032768n";
			// fillerScoreTriplet.fillerGloss = "A strong belief in a supernatural power or powers that control human destiny";
			// synsetIdGlossMap.put(fillerScoreTriplet.fillerSynsetID, fillerScoreTriplet.fillerGloss);
			// fillerScoreTriplets.add(fillerScoreTriplet);
		}

		removeExactMatch();  // does not add threads for exact matches since we manually assign them scores

		if (fillerScoreTriplets.size() == 0) {
			if (!hasExactMatch) {
				fillerSynsetIDs.add(filler);
				fillerSynsetGlosses.add("not found");
				synsetIdScoreMap.put(filler, 0.01);
				synsetIdGlossMap.put(filler, "not found");
				synsetIdPathMap.put(filler, "");
			}
			return null;
		}

		BabelNetShareResource.clear();
		List<Thread> threadPool = new ArrayList<Thread>(); // list for threads
		String[] fillerSIDs = new String[fillerScoreTriplets.size()];
		for (int i = 0; i < fillerSIDs.length; i++) {
			fillerSIDs[i] = fillerScoreTriplets.get(i).fillerSynsetID;
		}
		for (String roleNameSID : roleNameSynsetIDs) {  // iterate every synset of the role, new inverse Bidirectional Search threads, from role to role-filler
			Thread roleToFillerThread =  new RoleToFillerThread(Arrays.toString(roleNameSynsetIDs), pairKey, roleNameSID, fillerSIDs, prohibitedEdgeList);
			threadPool.add(roleToFillerThread); // inverse ones are added to threadPool
		}
		for (int i = 0; i < fillerScoreTriplets.size(); i++) {  // iterate every synset of the role-filler, new forward Bidirectional Search threads, from role-filler to role
			String fillerSynsetID = fillerScoreTriplets.get(i).fillerSynsetID;
			Thread fillerToRoleThread = new FillerToRoleThread(pairKey, i, fillerSynsetID, roleNameSynsetIDs, prohibitedEdgeList);
			threadPool.add(fillerToRoleThread); // forward ones are added to threadPool as well
		}
		return threadPool; // every pair of (role synset, filler synset) relation is added to the pool
	}


	/**
	 * This method deal with all the calculated info in FEValTupleList and sort the elements by score then save them
	 * to FEValSynsetIdList and FEValSynsetGlossList for final use.
	 */
	public void sortSynsetsByScore()
	{
		if(fillerScoreTriplets.size() == 0) { //  if there's no synset id
			return;
		}

		for(int i = 0; i < fillerScoreTriplets.size(); i++) {  // for every synset of the role-filler
			String fillerSynsetID = fillerScoreTriplets.get(i).fillerSynsetID;
			double score = BabelNetShareResource.getScore(pairKey, i); // get the best score of the i-th synset of the role-filler
			String path = BabelNetShareResource.getPath(pairKey, i); // get the best path of the i-th synset of the role-filler to the role
			synsetIdScoreMap.put(fillerSynsetID, score);  // add the result of the i-th synset to the map
			synsetIdPathMap.put(fillerSynsetID, path);
			fillerScoreTriplets.get(i).score = score;
		}

		fillerScoreTriplets.sort(new Comparator<ScoreTriplet>() { // sort the role-filler synset tuple list by score
			public int compare(ScoreTriplet triplet1, ScoreTriplet triplet2) {
				return Double.compare(triplet2.score, triplet1.score);
			}
		});

		for(ScoreTriplet fillerScoreTriplet : fillerScoreTriplets) {
			fillerSynsetIDs.add(fillerScoreTriplet.fillerSynsetID); // update role-filler synset ID list after sorting
			fillerSynsetGlosses.add(fillerScoreTriplet.fillerGloss); // update role-filler gloss list after sorting
//			System.out.println(fillerScoreTriplet.fillerSynsetID + "-" + roleNameSynsetIDs[0]+ " " + fillerScoreTriplet.score);
		}
	}

	/**
	 * This method is to deal with the situation that the role and the role-filler share an identical
	 * synset. If the role-filler is who or where and has a match, not consider this role-filler any more. If not who or where
	 * and has a match, delete the matched synset in the role-filler.
	 */
	private void removeExactMatch() {
		Iterator<ScoreTriplet> tupleListIterator = fillerScoreTriplets.iterator();
		while (tupleListIterator.hasNext()) { // iterate every synset of the role-filler
			ScoreTriplet element = tupleListIterator.next();
			if(roleNameSynsetIDSet.contains(element.fillerSynsetID)) {
				hasExactMatch = true; // mark as has exact match
				tupleListIterator.remove(); // delete this synset in the role-filler
				exactMatchFillerScoreTriplets.add(element); // add this match
			}
		}
	}

	/**
	 * @return a boolean value to show if there's an exact match between the role and the role-filler.
	 */
	public boolean existExactMatch() { return hasExactMatch; }

	/**
	 * @param score a given score for an exact match
	 * This method gives an exact match a highest rank and assign a score to it without computing.
	 */
	public void setExactMatchResult(double score) {
		for(ScoreTriplet fillerScoreTriplet : exactMatchFillerScoreTriplets) {  // for every matched synset
			fillerSynsetIDs.add(0, fillerScoreTriplet.fillerSynsetID); // set this synset to the very beginning of the synset list of the role-filler
			fillerSynsetGlosses.add(0, fillerScoreTriplet.fillerGloss);
			synsetIdScoreMap.put(fillerScoreTriplet.fillerSynsetID, score); // set this a predefined score
			synsetIdPathMap.put(fillerScoreTriplet.fillerSynsetID, "");
		}
	}

	// private boolean isFEValMatchFEName(List<ScoreTriplet> fillerScoreTripletList)
	// {
	// 	boolean isMatched = false;
	// 	for(ScoreTriplet fillerScoreTriplet : fillerScoreTripletList)
	// 	{
	// 		if(roleNameSynsetIDSet.contains(fillerScoreTriplet.fillerSynsetID))
	// 		{
	// 			isMatched = true;
	// 			fillerSynsetIDs.add(fillerScoreTriplet.fillerSynsetID);
	// 			fillerSynsetGlosses.add(fillerScoreTriplet.fillerGloss);
	// 			synsetIdScoreMap.put(fillerScoreTriplet.fillerSynsetID, defaulScore);
	// 			synsetIdPathMap.put(fillerScoreTriplet.fillerSynsetID, "");
	// 			break;
	// 		}
	// 	}		
	// 	if(isMatched)
	// 	{
	// 		for(ScoreTriplet fillerScoreTriplet : fillerScoreTripletList)
	// 		{
	// 			if(!roleNameSynsetIDSet.contains(fillerScoreTriplet.fillerSynsetID))
	// 			{
	// 				fillerSynsetIDs.add(fillerScoreTriplet.fillerSynsetID);
	// 				fillerSynsetGlosses.add(fillerScoreTriplet.fillerGloss);
	// 				synsetIdScoreMap.put(fillerScoreTriplet.fillerSynsetID, 0.0);
	// 				synsetIdPathMap.put(fillerScoreTriplet.fillerSynsetID, "");
	// 			}
	// 		}
	// 		return true;
	// 	}
	// 	else
	// 	{
	// 		return false;
	// 	}
	// }

	public void setRoleFiller(String filler, String fillerIndex, String fillerPOS, String fillerClass, String fillerQuant) {
		this.filler = filler;
		this.fillerIndex = fillerIndex;
		this.fillerPOS = fillerPOS;
		this.fillerClass = fillerClass;
		this.fillerQuant = fillerQuant;
		String roleNameSIDs = "";
		for(String roleNameSynsetID : roleNameSynsetIDs) {
			roleNameSIDs += roleNameSynsetID;
		}
		this.pairKey = filler + ":" + fillerPOS + ":" + roleNameSIDs;
	}

	/**
	 * @return the score of the synset that is currently being shown.
	 */
	public double getBestSynsetScore() { return this.fillerBestSynsetScore; }

	public String getRoleName()
	{
		return roleName;
	}

	public String getFiller() { return filler; }

	public String getFillerIndex()
	{
		return fillerIndex;
	}

	public String getFillerQuant()
	{
		return fillerQuant;
	}

	public String getFENameSynsetId()
	{
		return roleNameSynsetIDs[0];
	}

	public String getFillerBestSynsetID()
	{
		return fillerBestSynsetID;
	}

	public String getFillerBestSynsetGloss() { return fillerBestSynsetGloss; }

	public String getFillerSynsetId(int index)
	{
		return fillerSynsetIDs.get(index);
	}

	public String getSynsetGlossById(String synsetId)
	{
		return synsetIdGlossMap.get(synsetId);
	}

	public double getSynsetScoreById(String synsetId)
	{
		return synsetIdScoreMap.get(synsetId);
	}

	public String getRoleNameSynsetGloss()
	{
		return roleNameSynsetGloss;
	}

	public int getNumberOfSynsets() { return fillerSynsetIDs.size(); }

	public String getPairKey() { return this.pairKey; }

	public void setRoleDataType(String roleDataType)
	{
		this.roleDataType = roleDataType;
	}

	public boolean checkInteger(String val)
	{
		try {
			Integer.parseInt(val);
		} catch(NumberFormatException | NullPointerException e) {
			return false;
		}
		return true;
	}

	public void setProhibitedEdgeList(ArrayList<String> prohibitedEdgeList) { this.prohibitedEdgeList = prohibitedEdgeList;	}

	public String print() {
		String s = roleName + " = " + filler + " = " + fillerIndex + " = " + fillerBestSynsetScore + " = " + fillerBestSynsetGloss + "\n";
		if (fillerSynsetIDs.size() > 0) {
			String synset = fillerSynsetIDs.get(0);
			if (synsetIdScoreMap.get(synset) > 0.0) {
				if (!synsetIdPathMap.get(synset).equals("")) {
					s += "\t" + synsetIdPathMap.get(synset) + "\n";
				}
			}
		}
		return s;
	}

	/**
	 * @return A tuple in string contains role, role-filler and the score of best synset(s) of the role-filler.
	 * So-called top result is most suitable synset given the role-filler, which itself (the role-filler) may be wrong.
	 */
	public String getTopResult()
	{
		if(fillerSynsetIDs.size() == 0)
		{
			return null;
		}

		String synsetSet = "'" + fillerSynsetIDs.get(0) + "'";
		Double score = synsetIdScoreMap.get(fillerSynsetIDs.get(0));
		for(int i = 1; i < fillerSynsetIDs.size(); i++)
		{
			String synset = fillerSynsetIDs.get(i);
			if(Double.toString(synsetIdScoreMap.get(synset)).equals(Double.toString(score)))
			{
				synsetSet += "/'" + synset + "'";
			}
		}

		return "triple('" + roleName + "','" + filler + "'," + synsetSet + ")";
	}
}
