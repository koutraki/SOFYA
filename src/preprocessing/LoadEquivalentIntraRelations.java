package preprocessing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import gold.KB;
import gold.Relation;
import preprocessing.ComputingIntraCWA.AlignmentCWA;

public class LoadEquivalentIntraRelations {
	public static final String separatorSpace = "\\s+";
	
	/******************************************************************************/
	/*** Load Pairs ***/
	/*****************************************************************************/
     public static final  ArrayList<EquivPair>  loadEquivalentRelations(String file, boolean skipFirstLine) throws Exception {
    	    ArrayList<EquivPair> pairs=new ArrayList<EquivPair> ();
		BufferedReader pairReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
		if (skipFirstLine) pairReader.readLine();

		String line = null;
		while ((line = pairReader.readLine()) != null) {
			line=line.trim();
			if (line.isEmpty())  continue;
			String[] e = line.split(separatorSpace);
			
			Relation rLeft=getRelationFromStringDescr(e[0].trim());
			rLeft.tupleNo=Integer.valueOf(e[3]);
			
			Relation rRight=getRelationFromStringDescr(e[1].trim());
			rRight.tupleNo=Integer.valueOf(e[4]);
			
			if(rLeft.uri.equals(rRight.uri)) {
				System.err.println("Symetric relations :   "+line);
				continue;
			}
		
			EquivPair pair=new EquivPair(rLeft, rRight);
			
		     pairs.add(pair);
				
		}
		pairReader.close();
		return pairs;
	}

     
     
     public static final void getLeftRightSet(HashSet<String> left, HashSet<String> right, ArrayList<EquivPair> pairs){
    	     for(EquivPair p: pairs){
    	    	 	 left.add(p.leftHand.uri);
    	    	 	 right.add(p.rightHand.uri);
    	     }
     }
     
     public static final Relation getRelationFromStringDescr(String s)throws Exception{
 		
 		String uri=(s.endsWith("-"))?s.substring(0,s.length()-1): s;
 		Relation r=(s.endsWith("-"))?new Relation(uri, false):new Relation(uri, true);
 		return r;	
 	}
 	
 	
 	public static final boolean doesFileExist(String pathToFile){
 		File f=new File(pathToFile);
 		if(!f.exists()) {
 			System.err.println("  File does not exit "+pathToFile);
 			return false;
 		}else {
 			return true;
 		}
 	}
	
	

	/******************************************************************************/
 	/*** CLASS EQUIV PAIR  ***/
 	/*****************************************************************************/
	public static final class EquivPair{
		public final Relation leftHand;
		public final Relation rightHand;
		
		public EquivPair(Relation leftHand, Relation rightHand){
			this.leftHand=leftHand;
			this.rightHand=rightHand;
		}
	}
	
	public static final HashSet<String>  getSkipRelations(KB kb, String rootDir) throws Exception{
		String fileWithQuivalences=rootDir+kb.name+"/"+kb.name+"_"+kb.name+"_equi_ee.txt";
		boolean exist=doesFileExist(fileWithQuivalences);
		if(!exist) return new HashSet<String>  ();
		
			
		ArrayList<EquivPair> pairs=loadEquivalentRelations(fileWithQuivalences, true);
		
		HashSet<String> left=new HashSet<String>();
		HashSet<String> right=new HashSet<String>();
		getLeftRightSet(left, right, pairs);
		
		
		Iterator<String> it=right.iterator();
		while(it.hasNext()){
			if(left.contains(it.next())) it.remove();
		}

		return right;
	}
	
	
	public static void main(String[] args) throws Exception {	
		
		String dir ="/Users/adi/Dropbox/DBP/feb-sofya/"; //"feb-sofya/";  "feb-sofya/";
		
		KB yago = new KB("yago", "http://s6.adam.uvsq.fr:8892/sparql", "http://yago-knowledge.org");
		KB dbpedia = new KB("dbpedia", "http://s6.adam.uvsq.fr:8892/sparql", "http://dbpedia.org");
		KB freebase = new KB("freebase", "http://s6.adam.uvsq.fr:8892/sparql", "http://rdf.freebase.com");
		
		KB kb=freebase;
		HashSet<String>   skipRel=getSkipRelations(kb, dir);
		System.out.println("I can eliminate "+skipRel.size()+"   relations/cases ");
		
	}

	
	
}
