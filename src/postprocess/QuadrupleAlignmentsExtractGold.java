package postprocess;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;

import org.apache.jena.atlas.lib.NumberUtils;

import com.hp.hpl.jena.reasoner.rulesys.builtins.StrConcat;

import compare.CompareWithParis.AlignmentWithComments;
import config.Alignment;
import gold.GetSelfPCAConf.CompleteAlignment;
import gold.KB;
import gold.Relation;

public class QuadrupleAlignmentsExtractGold {
	public static final int NONE=-1;
	public static final int CORRECT=0;
	public static final int ERROR=1;
	public static final int OVERLAP=2;
	
	public static void loadGoldPairs(String pathToFile, boolean skipFirstLine) throws Exception{
		BufferedReader alignReader = new BufferedReader(new InputStreamReader(new FileInputStream(pathToFile),"UTF8"));
	    if(skipFirstLine) alignReader.readLine();
		
		String line=null;
		
		ArrayList<CompleteAlignment> alignmentSet=new ArrayList<CompleteAlignment>();
		StringBuffer setAlign=new StringBuffer();
		int status=NONE;
		while ((line = alignReader.readLine()) != null) {
			    line=line.trim();
			 
				if(line.isEmpty()){ 
					continue;	
				}
				
				if(line.startsWith("**")) {
					/** extract correct alignment from previous **/
					treatQuadruples(alignmentSet, setAlign, status);
					
					alignmentSet=new ArrayList<CompleteAlignment>();
					setAlign=new StringBuffer();
					status=NONE;
					continue;
				}
				
				setAlign.append(line+"\n");
			
	
				String[] e = line.split("\\s+");
				
				/** if it does not seem to be a valid alignment **/
				if( !(e[0].matches("[0-9]+") &&  e[3].matches("[0-9]+")  && e[4].matches("[0-9]+")  && e[5].matches("[0-9]+") && e[6].matches("[0-9]+") ) ){	
					if(line.toLowerCase().startsWith("cor")) {
							status=(status==NONE)?CORRECT:NONE;
					}
					if(line.toLowerCase().startsWith("error")) {
						status=(status==NONE)?ERROR:NONE;
					}
					if(line.toLowerCase().startsWith("overlap")) {
						status=(status==NONE)?OVERLAP:NONE;
					}
					continue;
				}
				
				Relation rS=Relation.getRelationFromStringDesc(e[1].trim());
				Relation rT=Relation.getRelationFromStringDesc(e[2].trim());			
						
				Integer sharedXY=Integer.valueOf(e[3].trim());
				Integer originalSamples=Integer.valueOf(e[4].trim());
				Integer PCA_X=Integer.valueOf(e[5].trim());
				Integer PCA_Y=Integer.valueOf(e[6].trim());
						
				CompleteAlignment align=new CompleteAlignment(rS, rT, sharedXY, originalSamples, PCA_X, PCA_Y);
				
				alignmentSet.add(align);
				
		}
		alignReader.close();
		
	}
	
	public static final void treatQuadruples(	ArrayList<CompleteAlignment> alignmentSet, StringBuffer setAlign, int status){
		if(status==NONE) {
			System.out.println("*******************");
			System.out.println(setAlign);
		}
	}
	
	
	public static void main(String[] args) throws Exception {	
		String dir = "/Users/adi/Dropbox/DBP/feb-sofya/";  //"feb-sofya/"; //  "/home/mary/Dropbox/feb-sofya/"; "/home/mary/Dropbox/feb-sofya/"; 
		
		KB yago = new KB("yago", "http://s6.adam.uvsq.fr:8892/sparql", "http://yago-knowledge.org");
		KB dbpedia = new KB("dbpedia", "http://s6.adam.uvsq.fr:8892/sparql", "http://dbpedia.org");
		KB freebase = new KB("freebase", "http://s6.adam.uvsq.fr:8892/sparql", "http://rdf.freebase.com");
		
		
		KB kb=freebase;
		
		try{
					loadGoldPairs(dir+"_crawd/_"+kb.name+"_observations_PCAs.txt", false);
		}  catch(Exception e ){
			  System.err.println(e.getMessage());
			  e.printStackTrace();
		}
		//nohup java -cp "./lib/*:sofya_gold_fbTodbpedia.jar" gold.GetOvelappingRels http://rdf.freebase.com/ns/location.location.people_born_here- &
	}

}
