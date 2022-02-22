package main.java.edu.stonybrook.cs.frame;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This method deals with the serialized template frames in string to save them in variables.
 */
public class FrameOnt {
	private static final String input = "../../resources/frameont/frame_ont.txt";
	private static HashMap<String, ArrayList<RoleOnt>> frameNameRoleOntPairs = new HashMap<String, ArrayList<RoleOnt>>();
	
	FrameOnt() {
		
	}

	/**
	 * This method parses every frame in frame_property.txt, converting strings to a
	 * (frame name, property predicates) map, stored as a static variable in FrameOnt.
	 */
	public static void parse() {
		try (BufferedReader br = new BufferedReader(new FileReader(input))) {
			String sentence;			
			while((sentence = br.readLine())!=null) {
//				System.out.println(sentence);
				parseFrameOnt(sentence);
			}
		} catch (IOException x) {
			System.err.println(x);
			x.printStackTrace();
		}
	}

	/**
	 * @param input a line in frame_property.txt, representing a frame.
	 * fp('Commerce_buy',[property('Buyer',['bn:00014332n']),
	 *                    property('Seller',['bn:00053479n']),
	 *                    property('Goods',['bn:00006126n','bn:00021045n','bn:00014157n','bn:00046038n','bn:00022071n']),
	 *                    property('Money',['bn:00017803n'],['Currency']),
	 *                    property('Place',['bn:00066884n']),
	 *                    property('Recipient',['bn:00066495n'])]).
	 * This method converts a frame in string to a (name, property list) and adds it to a map storing all frames.
	 */
	private static void parseFrameOnt(String input)
	{
		 ArrayList<RoleOnt> roleOnts = new ArrayList<RoleOnt>();
		 String frameName = null;
	     Matcher m = Pattern.compile("^fp\\(\\'([a-zA-Z_]+)\\',\\[(.*)\\]\\).$").matcher(input);
	     while(m.find()) {
	       frameName = m.group(1);
	       String propertyList = m.group(2);
	       Matcher m2 = Pattern.compile("property\\(\\'(.*?)\\',(.*?)\\)").matcher(propertyList);
	       while(m2.find()) {
	    	   RoleOnt roleOnt = new RoleOnt(m2.group(1), m2.group(2));
	    	   roleOnts.add(roleOnt);
	       }	       	       
	     }
	     frameNameRoleOntPairs.put(frameName, roleOnts);
	}

	/**
	 * @param frameName a template frame name
	 * @return A list of properties (roles), containing name and synset IDs.
	 */
	public static ArrayList<RoleOnt> getRoleOnts(String frameName) { return frameNameRoleOntPairs.get(frameName); }

	/**
	 * @param frameName a template frame name
	 * @param roleName a given element name of the frame.
	 * @return a list of synset IDs because the property (role) may have multiple IDs.
	 */
	public static String[] getRoleSynsetIDs(String frameName, String roleName) {
		ArrayList<RoleOnt> roleOnts = frameNameRoleOntPairs.get(frameName);
		for (RoleOnt roleOnt : roleOnts) {
			if(roleOnt.getRoleName().equals(roleName)) {
				return roleOnt.getSynsetIDs();
			}
		}
		return null;
	}
}
