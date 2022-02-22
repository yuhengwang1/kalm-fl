package main.java.edu.stonybrook.cs.frame;

import java.io.*;
import java.util.*;


public class FrameExtractor {
	private static final String candidateParseDir = "../../resources/results/candidate_parses/";
	private static HashMap<Integer, String> sentencesByIndex = new HashMap<Integer, String>();

	public static int getMaxSentIdx() {
		int maxSentIdx = 0;
		for (int key : sentencesByIndex.keySet()) {
			if (maxSentIdx < key) {
				maxSentIdx = key;
			}
		}
		return maxSentIdx;
	}

	/**
	 * @return A list of frames (candidate parses), sorted by scores.
	 * This method takes input in ape/tmp/query_output.txt containing candidate parses in string and converts them
	 * to the instances of Frame.
	 * Meanwhile, calculates the score of every candidate parse and sorts them by scores.
	 */
	public static ArrayList<Frame> getFrameExtractionResult(String sourceFile) {
		ArrayList<Frame> frameList = new ArrayList<Frame>();

		try (BufferedReader br = new BufferedReader(new FileReader(candidateParseDir + sourceFile))) {
			HashSet<String> addedPairKeys = new HashSet<String>();
			List<Thread> allThreads = new ArrayList<Thread>();
		    String line, sentence = null;
		    int sentenceIndex = 0;
		    while ((line = br.readLine()) != null) {  // first you meet frame name, construct a frame; then you meet role fillers, fill the roles
		    	if (line.contains("==")) {
					sentenceIndex = Integer.parseInt(line.replace("=", ""));
				} else if (line.contains(" = index(")) {  // if a frame name, create the template frame
					String[] val = line.split(" = ");
		    		Frame frame = FrameOntLoader.getFrameOnt(val[0]);
		    		frame.setSentence(sentence);
		    		frame.setSentenceIndex(sentenceIndex);
					String luIndex = val[1].split(",")[1];
					frame.setLUIndex(luIndex.substring(0, luIndex.length()-1));
					frameList.add(frame); // this is a template, including all the roles and their synset IDs
		    	} else if (line.contains(") - ")) { // if a role and its filler, instantiate the roles and get candidate parses
		    		String[] val = line.split(" - ");
					Frame frame = frameList.get(frameList.size() - 1); // get the newly added template
					Role role = frame.getRoleByName(val[0]);  // get the roleont
					assert role != null;
					String fillerIndex = val[2].split(",")[1];
					role.setRoleFiller(val[1].replace("_", " "), fillerIndex.substring(0, fillerIndex.length()-1), val[3], val[4], val[5]);  // fill the role
					List<Thread> threads = role.createSematicScoreComputationThreads(); // every role and filler has a thread list
					if (!addedPairKeys.contains(role.getPairKey()) && threads != null) {  // remove duplicates because like Person-Mary pair may be added in other parses
						addedPairKeys.add(role.getPairKey());
						allThreads.addAll(threads);
					}
				} else if (!line.equals("\n") && !line.equals("")) {
					sentence = line;
					sentencesByIndex.put(sentenceIndex, sentence);
				}
		    }
		    for (Thread t : allThreads) {
		    	t.start();
		    }
		    for(Thread t : allThreads) {
		    	t.join(); // block father thread until it is finished
		    }


		    for(Frame frame : frameList) {  // for every candidate parse
		    	for(Role role : frame.getAllRoles()) {  // for all roles
		    		role.sortSynsetsByScore(); // set and sort the sysnet Ids OF THE ROLE list by score
		    		role.setBestResult(); // set up the display info for the synset with highest score
		    		double score = role.getBestSynsetScore();  // get the highest score
					if (role.existExactMatch()) {
						if (role.getNumberOfSynsets() > 0) {
							role.setExactMatchResult(score + 0.5);  // update best result
						} else {
							role.setExactMatchResult(2.0);
						}
						role.setBestResult();  // exact match is set as best result, update the best results
					}
		    	}
		    }

		} catch (IOException x) {
			System.err.println(x);
			x.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		// Remove if this change affects the accuracy
		computeCoefficientVal(frameList); // set up coefficient of every candidate parse
		
		for(Frame frame : frameList) {
			frame.computeFrameScore(); // compute score of every candidate parse
		}

		frameList.sort(new Comparator<Frame>() { // sort the candidate parses by their scores
			public int compare(Frame f1, Frame f2) {
				if (f1.getSentenceIndex() > f2.getSentenceIndex()) {
					return 1;
				} else if (f1.getSentenceIndex() < f2.getSentenceIndex()) {
					return -1;
				} else {
					return Double.compare(f2.getFrameScore(), f1.getFrameScore());
				}
			}
		});
		return frameList;
	}

	public static HashMap<Integer, String> getAllSentences() {
		return sentencesByIndex;
	}

	/**
	 * @param frameList candidate parses
	 * This method computes coefficients of every candidate parse to get their respective scores.
	 * The candidate parse with more filled roles has higher coefficient. 1.0 is the max value.
	 */
	private static void computeCoefficientVal(ArrayList<Frame> frameList)
	{
		for (Frame frame : frameList) {// for every candidate parse
			int max = frame.getFilledRolesNum(); // the max number of filled roles of candidate parses with the same name
			for (Frame tmpFrame : frameList) {
				if (frame.getSentenceIndex() == tmpFrame.getSentenceIndex() &&
						tmpFrame.getFrameName().equals(frame.getFrameName()) && tmpFrame.getFilledRolesNum() > max) {
					max = tmpFrame.getFilledRolesNum(); // max is the largest filled role number of a frame under the same frame name
				}
			}
			frame.setCoefficientVal(1.0*frame.getFilledRolesNum()/max); // the candidate parse with more filled roles has higher coefficient
		}
	}
}
