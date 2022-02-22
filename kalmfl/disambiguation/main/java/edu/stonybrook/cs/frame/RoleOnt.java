package main.java.edu.stonybrook.cs.frame;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class deals with the property (role) part in the serialized template frames to save them in variables.
 */
public class RoleOnt {
	private String roleName = null;
	private String[] roleNameSynsetIDs = null;
	private String[] roleNameDataTypes = null;
	private String[] prohibitedEdgeList = null;

	/**
	 * @param roleName retrieve the given role name
	 * @param roleSynsets a string containing 3 fields. BabelNet ID(s), data type
	 *                                  constraint(s) and prohibited edge list (?)
	 * 									['bn:00017803n'],['Currency']
	 * This method breaks down the 3 fields and stores them into the PropertyPredicate object.
	 */
	RoleOnt(String roleName, String roleSynsets) {
		this.roleName = roleName;
		Matcher m = Pattern.compile("\\[(.*?)\\]").matcher(roleSynsets);
		int counter = 0;
	    while(m.find()) {
	    	counter++;
	    	if(counter == 1) {
	    		setSynsetIDs(m.group(1));
	    	} else if (counter == 2) {
	    		setDataTypeConstraints(m.group(1));
	    	} else if(counter == 3) {
	    		setProhibitedEdgeList(m.group(1));
	    	}
	    }
	}
	
	public String getRoleName() { return roleName; }

	/**
	 * @param input a string representing synset list
	 *              'bn:00046516n','bn:00059480n'
	 * This method saves the synsets of a role into a variable
	 */
	private void setSynsetIDs(String input) { roleNameSynsetIDs = getElementFromList(input); }

	/**
	 * @param input a string representing the synset list
	 *              'Integer'
	 * This method saves the data types of a role into a variable
	 */
	private void setDataTypeConstraints(String input) { roleNameDataTypes = getElementFromList(input); }

	/**
	 * @param input a string representing the data type list
	 *              'hyponym' (means one can only fill it out with its hyoinym?)
	 * This method saves the prohibit edges of a role into a variable
	 */
	private void setProhibitedEdgeList(String input)
	{
		prohibitedEdgeList = getElementFromList(input);
	}

	/**
	 * @param input A string split by ","
	 *              'bn:00046516n','bn:00059480n' or 'Integer' or 'hyponym'
	 * @return A list of String
	 * This is a tool method to split multiple synset IDs or data type constraints...
	 */
	private String[] getElementFromList(String input) {
		String[] tmpList = null;
		if(input.length() == 0)
		{
			return tmpList;
		}
		tmpList = input.split(",");
		for(int i = 0; i < tmpList.length; i++)
		{
			assert(tmpList[i].charAt(0) == '\'');
			assert(tmpList[i].charAt(tmpList[i].length()-1) == '\'');
			tmpList[i] = tmpList[i].substring(1, tmpList[i].length() - 1);
		}
		return tmpList;
	}
		
	public String[] getSynsetIDs() { return roleNameSynsetIDs; }
	
	public String[] getDataTypeConstraints()
	{
		return roleNameDataTypes;
	}
	
	public String[] getProhibitedEdgeList()
	{
		return prohibitedEdgeList;
	}
}
