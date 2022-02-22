package main.java.edu.stonybrook.cs.correction;

import main.java.edu.stonybrook.cs.frame.FrameOnt;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is a util class to save bias and penalty in resources/semantic_score_meta/edge_param.txt
 */
public class SemanticScoreParameters {
	private static final String input = "../../resources/semantic_score_meta/edge_param.txt";
	private static HashMap<String, Double> edgeTypeBiasMap = new HashMap<String,Double>();
	private static HashMap<String, Double> edgeTypePenaltyMap = new HashMap<String,Double>();
	
	public static void setFuzzyIsAWeightBias(String frameName, String frameElementName, String edgeType, Double val)
	{
		String[] sidList = FrameOnt.getRoleSynsetIDs(frameName, frameElementName);
		assert sidList != null;
		String ParameterPrefix = Arrays.toString(sidList);
//		System.out.println(ParameterPrefix);
		edgeTypeBiasMap.put(ParameterPrefix + "::" + edgeType, val);
	}
	
	public static void setFuzzyIsAPenaltyVal(String frameName, String frameElementName, String edgeType, Double val)
	{
		String[] sidList = FrameOnt.getRoleSynsetIDs(frameName, frameElementName);
		assert sidList != null;
		String ParameterPrefix = Arrays.toString(sidList);
//		System.out.println(ParameterPrefix);
		edgeTypePenaltyMap.put(ParameterPrefix + "::" + edgeType, val);
	}

	/**
	 * @param weightKey edge type
	 * @return weight bias given edge type
	 */
	public static Double getWeightBias(String weightKey)
	{
		if(edgeTypeBiasMap.containsKey(weightKey))
		{
			return edgeTypeBiasMap.get(weightKey);
		}
		else
		{
			String[] array = weightKey.split("::");
			assert array.length == 2;
			if(edgeTypeBiasMap.containsKey(array[1])) // which is the edge type
			{
				return edgeTypeBiasMap.get(array[1]);
			}
			else 
			{
				return 0.0;
			}
		}
	}

	/**
	 * @param penaltyKey edge type
	 * @return penalty given edge type
	 */
	public static Double getPenaltyVal(String penaltyKey)
	{
		if(edgeTypePenaltyMap.containsKey(penaltyKey))
		{
			return edgeTypePenaltyMap.get(penaltyKey);
		}
		else
		{
			String[] array = penaltyKey.split("::");
			assert array.length == 2;
			if(edgeTypePenaltyMap.containsKey(array[1])) // which is the edge type
			{
				return edgeTypePenaltyMap.get(array[1]);
			}
			else
			{
				return 5.0;
			}
		}
	}

	/**
	 * @param input a line in edge_param.txt
	 * @return if this line is of weight bias, process and save the data to edgeTypeBiasMap
	 */
	private static boolean parseEdgeWeightBiasPredicate(String input)
	{
	     Matcher m = Pattern.compile("^weight_bias\\(\\'(.+)\\',(.+)\\)\\.$").matcher(input);
	     while(m.find()) {	    	 
	    	 edgeTypeBiasMap.put(m.group(1), Double.parseDouble(m.group(2)));
	    	 return true;
	     }
	     return false;
	}

	/**
	 * @param input a line in edge_param.txt
	 * @return if this line is of penalty, process and save the data to edgeTypePenaltyMap
	 */
	private static boolean parseEdgePenaltyPredicate(String input)
	{
	     Matcher m = Pattern.compile("^penalty\\(\\'(.+)\\',(.+)\\)\\.$").matcher(input);
	     while(m.find()) {
	    	 edgeTypePenaltyMap.put(m.group(1), Double.parseDouble(m.group(2)));
	    	 return true;
	     }
	     return false;
	}

	/**
	 * This method read resources/semantic_score_meta/edge_param.txt to get different weight biases and penalties of different edge type
	 */
	public static void initialize()
	{
		try (BufferedReader br = new BufferedReader(new FileReader(input))) 
		{			
			String sentence;			
			while((sentence = br.readLine())!=null)
			{
				if(parseEdgeWeightBiasPredicate(sentence))
				{
					continue;
				}
				else if(parseEdgePenaltyPredicate(sentence))
				{
					continue;
				}
				else
				{
					throw new AssertionError("invalid predicate.");
				}
			}		
		}
		catch (IOException x) 
		{
			System.err.println(x);
			x.printStackTrace();
		}
	}
}
