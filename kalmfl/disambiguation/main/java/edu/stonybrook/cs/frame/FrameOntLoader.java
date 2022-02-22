package main.java.edu.stonybrook.cs.frame;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * This is a loader to generate structured template frames.
 */
public class FrameOntLoader {
	public static Frame getFrameOnt(String frameName) { return createFrame(frameName); }

	/**
	 * @param f1
	 * @param f2
	 * Compare two frames to see if they have identical frame and property names and meaning.
	 */
	private static void compareFrame(Frame f1, Frame f2)
	{
		assert f1.getFrameName().equals(f2.getFrameName());
		assert f1.getAllRoles().size() == f2.getAllRoles().size();
		ArrayList<Role> l1 = f1.getAllRoles();
		ArrayList<Role> l2 = f2.getAllRoles();
		for(int i = 0; i < l1.size(); i++) {
			assert l1.get(i).getRoleName().equals(l2.get(i).getRoleName());
			assert l1.get(i).getFENameSynsetId().equals(l2.get(i).getFENameSynsetId());
		}	
	}

	/**
	 * @param frameName
	 * @return a created template frame by name.
	 * This method creates and return a new Frame with a list of FrameElement in it from FrameDescriptionPredicate
	 * and PropertyPredicate.
	 */
	public static Frame createFrame(String frameName) {
		ArrayList<RoleOnt> roleOnts = FrameOnt.getRoleOnts(frameName); // get frame template
		Frame frame = new Frame(frameName);
		for (RoleOnt roleOnt : roleOnts) {
			Role role = new Role(roleOnt.getRoleName(), roleOnt.getSynsetIDs());
			String[] dataTypes = roleOnt.getDataTypeConstraints();
			if (dataTypes != null) {
				for (String dataType : dataTypes) {
					if (dataType.equals("Integer")) {
						role.setRoleDataType(dataType);
					} else if (dataType.equals("Currency")) {
						role.setRoleDataType(dataType);
					}
				}
			}
			String[] prohibitedEdgeList = roleOnt.getProhibitedEdgeList();
			if (prohibitedEdgeList != null) {
				ArrayList<String> list = new ArrayList<String>(Arrays.asList(prohibitedEdgeList));
				role.setProhibitedEdgeList(list);
			}
			frame.addRole(role);
		}
		return frame;
	}

	/**
	 * @param frameName
	 * Test if created template frame is identical with directly obtained frame.
	 */
	public static void test(String frameName)
	{
		Frame f1 = createFrame(frameName);
		Frame f2 = getFrameOnt(frameName);
		compareFrame(f1,f2);
	}

}
