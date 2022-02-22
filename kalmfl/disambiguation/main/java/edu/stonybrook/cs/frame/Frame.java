package main.java.edu.stonybrook.cs.frame;

import java.util.ArrayList;


/**
 * This class called Frame is used in two ways: 1) instantiate it as template frames; 2) modify the FEs of it so that
 * the frames become candidate parses.
 */
public class Frame {
	private String sentence = null;
	private int sentenceIndex = 0;
	private String luIndex = null;
	private final String frameName;
	private final ArrayList<Role> roles = new ArrayList<Role>();
	private double score = 0.0; // score of the candidate parse, which is the geometric mean of all scores of filled roles
	private String scoreStr = null;
	private double coefficient = 1.0;

	public Frame(String frameName) { this.frameName = frameName; }

	public void setSentenceIndex(int sentenceIndex) { this.sentenceIndex = sentenceIndex; }

	public int getSentenceIndex() { return this.sentenceIndex; }

	public void setSentence(String sentence) { this.sentence = sentence; }

	public void setLUIndex(String index) { this.luIndex = index; }

	public String getSentence() { return this.sentence; }

	public String getFrameName() { return this.frameName; }

	public void addRole(Role role) { roles.add(role); }
	
	public Role getRoleByName(String roleName) {
		Role role = null;
		for (Role value : roles) {
			if (roleName.equals(value.getRoleName())) {
				role = value;
				break;
			}
		}
		return role;
	}
	
	public ArrayList<Role> getAllRoles()
	{
		return roles;
	}

	/**
	 * This method computes the score of the whole candidate parse by geometric mean, and further by its own coefficient.
	 */
	public void computeFrameScore() {
		int len = 0; // the number of filled roles
		score = 1.0;
		for (Role role : roles) {
			if (role.getFiller() != null) {
				len++;
				score = score * role.getBestSynsetScore(); // this score is computed by threads, they are multiplied to get geometric mean
				// ed by threads
			}
		}
//		score = score/len*coefficient;
		score = Math.pow(score, 1.0/len)*coefficient; // geometric mean by a coefficient, if this candidate parse has most filled roles, the coef is 1.0.
		scoreStr = String.valueOf(score);
//		scoreStr = String.format("%.3f", score);
	}
	
	public double getFrameScore()
	{
		return score;
	}
	
	public String getFrameScoreStr() { return scoreStr; }

	/**
	 * @return the number roles if they exist.
	 */
	public int getFilledRolesNum()
	{
		int num = 0;
		for(Role role : roles) {
			if(role.getFiller() != null) {
				num++;
			}
		}
		return num;
	}
	
	public void setCoefficientVal(double val) { coefficient = val; }

	public String getCoefficientStr() { return String.valueOf(coefficient); }
	
	public boolean isZeroScore() { return !(score > 0.0); }

	/**
	 * @return A frame with a calculated score in string.
	 * Invoked by a serialization method to store every calculated candidate parses.
	 */
	public String print() {
		String s = "[" + frameName + "] " + luIndex + " " + score + "\n";
		for(Role role : roles) {
			if (role.getFiller() != null) {
				s += role.print();
			}
		}
		return s;
	}

	/**
	 * @return
	 * Call different (for questions and for narratives) ULR generating methods.
	 */
	public String getULR()
	{	
//		if(!QueryProcessing.isVarWordIndexSetEmpty())
//		{
//			return getULRForQuery();
//		}
//		else
//		{
			return getULRForFacts();
//		}
	}

	/**
	 * @return
	 * Call different ULR generating methods.
	 */
	private String getULRForFacts()
	{
		String fPrefix = "fid_";
		String rPrefix = "rid_";
		String ulr = "";

		ulr += "ulr(" + fPrefix + luIndex + ", " + frameName + ", [";
		for(Role role : roles)
		{
			if(role.getFiller() == null)
			{
				continue;
			}
			ulr += "role(" + rPrefix + role.getFillerIndex() + ", " + role.getRoleName() + ", " + role.getFiller() + ", " + role.getFillerBestSynsetID() + "), ";
		}
		ulr = ulr.substring(0, ulr.length()-2) + "]).";
//		System.out.println(ulr);
		return ulr;
	}

	/**
	 * @return Question (query) ULR in string.
	 * This method generate a ULR that can be further used for display on UI.
	 */
// 	private String getULRForQuery()
// 	{
// 		int count = 2;
// 		String prefix = "X";
// 		String ulr = "";

// 		ulr += "frame(" + prefix + 1 + ", " + frameName + ")";
// 		for(Role fe : roles)
// 		{
// 			if(fe.getFiller() == null)
// 			{
// 				continue;
// 			}
// //			if(!QueryProcessing.isInVarWordIndexSet(fe.getFEValWordIndex()))
// //			{
// //				ulr += ",\n";
// //				ulr += "role(" + prefix + 1 + ", " + fe.getFEName() + ", " + prefix + count + "),\n";
// //				ulr += "entity(" + prefix + count + ", " + fe.getFEValSynsetId() + ")";
// //			}
// //			else
// //			{
// 			ulr += ",\n";
// 			ulr += "role(" + prefix + 1 + ", " + fe.getRoleName() + ", " + prefix + count + "),\n";
// 			ulr += "entity(" + prefix + count + ", " + "S" + count + "),\n";
// 			if(fe.getFiller().equals("what"))
// 			{
// 				ulr += "type(" + "S" + count + ", " + fe.getFENameSynsetId() + ")";
// 			}
// 			else
// 			{
// 				ulr += "type(" + "S" + count + ", " + fe.getFillerBestSynsetID() + ")";
// 			}
// //			}
// //			if(QueryProcessing.isInVarWordIndexSet("q" + fe.getFEValWordIndex()))
// //			{
// //				ulr += ",\n";
// //				ulr += "modifier(" + prefix + count + ", quantity, " + "Z" + count + ")";
// //			}
// 			count++;
// 		}
// 		ulr += ".\n";
// //		System.out.println(ulr);
// 		return ulr;
// 	}

	/**
	 * @return A string representing a frame with its FEs and the total score of the frame (candidate parse).
	 * Note that the frame itself may not be optimal even it is called "Top".
	 */
	public String getTopResult()
	{
		String s = "'" + frameName + "',[";
		boolean flag = false;
		for(Role fe : roles)
		{
			String temp = fe.getTopResult();
			if(temp != null)
			{
				if(flag == false)
				{
					flag = true;
					s += fe.getTopResult();
				}
				else
				{
					s += "," + fe.getTopResult();
				}
			}
		}
		return s + "]," + scoreStr;
	}

	/**
	 * @return A string representing a frame (candidate parse) with its FEs.
	 * Note that the frame itself may not be optimal even it is called "Top".
	 */
	public String getTopResultWithoutScore()
	{
		String s = "'" + frameName + "',[";
		boolean flag = false;
		for(Role fe : roles)
		{
			String temp = fe.getTopResult();
			if(temp != null)
			{
				if(flag == false)
				{
					flag = true;
					s += fe.getTopResult();
				}
				else
				{
					s += "," + fe.getTopResult();
				}
			}
		}
		return s + "],";
	}
}
