package main.java.edu.stonybrook.cs.utils;

import main.java.edu.stonybrook.cs.correction.SemanticLinkAddition;
import main.java.edu.stonybrook.cs.correction.SemanticLinkOverride;
import main.java.edu.stonybrook.cs.correction.SemanticScoreParameters;
import main.java.edu.stonybrook.cs.correction.SynsetOverride;
import main.java.edu.stonybrook.cs.frame.Frame;
import main.java.edu.stonybrook.cs.frame.FrameExtractor;
import main.java.edu.stonybrook.cs.frame.FrameOnt;
import main.java.edu.stonybrook.cs.frame.Role;

import java.io.*;
import java.util.*;

public class Disambiguation {
    private static final String errMsgPath = "../../resources/error_msg/message.txt";

    public static String disambiguate(String sourceFile, String targetFile) {
        FrameOnt.parse(); // parse all frame templates
        SemanticLinkOverride.initialize(); // override some relations in BabelNet
        SemanticScoreParameters.initialize(); // structure the (edge type, weight bias) and (edge type, penalty) in edge_param.txt
        SynsetOverride.initialize(); // override and structure the word-synset relations in synset_override.txt
        SemanticLinkAddition.initialize(); // override and structure the synset-edgetype-synset relations

        List<Frame> frameList = FrameExtractor.getFrameExtractionResult(sourceFile);
        HashMap<Integer, String> sentencesByIndex = FrameExtractor.getAllSentences();
        HashMap<Integer, LinkedList<String>> errMsg = getErrorMessage();

        HashMap<Integer, LinkedList<Frame>> frameListByIndex = new HashMap<Integer, LinkedList<Frame>>();

        int maxIndex = 0;
        for (int key : sentencesByIndex.keySet()) {
            if (maxIndex < key) {
                maxIndex = key;
            }
        }
//        int maxIndex = 0;
//        for (Integer sentIndex: errMsg.keySet()) {
//            if (sentIndex > maxIndex) {
//                maxIndex = sentIndex;
//            }
//        }
        for (Frame frame: frameList) {
//            int sentIndex = frame.getSentenceIndex();
//            if (sentIndex > maxIndex) {
//                maxIndex = sentIndex;
//            }
            frameListByIndex.put(frame.getSentenceIndex(), new LinkedList<Frame>());
        }
        for (Frame frame: frameList) {
            int sentIndex = frame.getSentenceIndex();
            LinkedList<Frame> frames = frameListByIndex.get(sentIndex);
            frames.add(frame);
        }


        String data = "[";
        for (int sentenceIndex = 1; sentenceIndex <= maxIndex; sentenceIndex ++) {
            String sentence = sentencesByIndex.get(sentenceIndex);
            LinkedList<Frame> frames = frameListByIndex.get(sentenceIndex);
            if (errMsg.containsKey(sentenceIndex) || frames == null) {
                data += "{\"id\":\"" + String.valueOf(sentenceIndex) + "\",\"sentence\":\"" + sentence + "\",\"error_msg\":[";
                LinkedList<String> templates = errMsg.get(sentenceIndex);
                if (templates != null) {
                    for (String template: templates) {
                        data += "\"" + template + "\",";
                    }
                }
                data += "],\"parses\":null},";
            } else {
                data += "{\"id\":\"" + String.valueOf(sentenceIndex) + "\",\"sentence\":\"" + sentence.replace("\"", "\\\"") + "\",\"error_msg\":null,\"parses\":[";
                for (Frame frame: frames) {
                    data += "{\"frame_name\":\"" + frame.getFrameName() + "\",\"coefficient\":\"" + frame.getCoefficientStr() +
                            "\",\"parse_score\":\"" + frame.getFrameScoreStr() + "\",\"ulr\":\"" + frame.getULR() + "\",\"roles\":[";
                    List<Role> roleList = frame.getAllRoles();
                    for (Role role : roleList) {
                        if (role.getFiller() != null) {
                            data += "{\"role_name\":\"" + role.getRoleName() + "\",\"filler\":\"" + role.getFiller() + "\",\"synsets\":[";
                            int numberOfSynsets =  role.getNumberOfSynsets();
                            for (int synsetIndex = 0; synsetIndex < numberOfSynsets; synsetIndex ++) {
                                String synsetId = role.getFillerSynsetId(synsetIndex);
                                String gloss = role.getSynsetGlossById(synsetId);
                                double score = role.getSynsetScoreById(synsetId);
                                data += "{\"synset\":\"" + synsetId +
                                        "\",\"gloss\":\"" + gloss.substring(0, Math.min(50, gloss.length())).replaceAll("[\"'\\\\]", "") +
                                        "\",\"score\":\"" + String.valueOf(score) + "\"},";
                            }
                            data = data.substring(0, data.length()-1);
                            data += "]},";
                        }
                    }
                    data = data.substring(0, data.length()-1);
                    data += "]},";
                }
                data = data.substring(0, data.length()-1);
                data += "]},";
            }
        }
        data = data.substring(0, data.length()-1);
        data += "]";


        try {
            BufferedWriter out = new BufferedWriter(new FileWriter("resources/results/disambiguated_parses/" + targetFile));
            int prevSentIndex = 0;
            for (Frame frame : frameList) {
                int index = frame.getSentenceIndex();
                while (index != prevSentIndex) {
                    prevSentIndex ++;
                    if (index == prevSentIndex) {
                        out.write("============================" + Integer.toString(index) + "============================\n");
                        out.write(frame.getSentence() + "\n");
                    } else {
                        out.write("============================" + Integer.toString(prevSentIndex) + "============================\n");
                        out.write(sentencesByIndex.get(prevSentIndex) + "\n\n");
                    }
                }
                out.write(frame.print());
                out.write("\n");
            }
            out.close();
        } catch (IOException ignored) {
        }

        return data;
    }

    private static HashMap<Integer, LinkedList<String>> getErrorMessage() {
        HashMap<Integer, LinkedList<String>> errMsg = new HashMap<Integer, LinkedList<String>>();
        try (BufferedReader br = new BufferedReader(new FileReader(errMsgPath))) {
		    String line = null;
		    int sentenceIndex = 0;
		    while ((line = br.readLine()) != null) {  // first you meet frame name, construct a frame; then you meet role fillers, fill the roles
                if (line.contains("==")) {
					sentenceIndex = Integer.parseInt(line.replace("=", ""));
                    LinkedList<String> templates = new LinkedList<String>();
                    errMsg.put(sentenceIndex, templates);
                } else if (line.contains(":required]")) {
                    LinkedList<String> templates = errMsg.get(sentenceIndex);
                    templates.add(line.replace("\n", "").replace("\r", ""));
                }
            }
        } catch (IOException x) {
			System.err.println(x);
			x.printStackTrace();
		}

        return errMsg;
    }

}
