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

	public static final void loadEquivalentRelations(HashMap<String, ArrayList<ComputingIntraCWA.AlignmentCWA>> mapOnFirstRel, HashMap<String, ArrayList<ComputingIntraCWA.AlignmentCWA>> mapOnSecondRelation, String file, boolean skipFirstLine) throws Exception {
		BufferedReader pairReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
		if (skipFirstLine)
			pairReader.readLine();

		String line = null;
		while ((line = pairReader.readLine()) != null) {
			if (line.isEmpty())
				continue;
			String[] e = line.split(separatorSpace);
			
			Relation rS=getRelationFromStringDescr(e[0].trim());
			rS.tupleNo=Integer.valueOf(e[3]);
			
			Relation rT=getRelationFromStringDescr(e[1].trim());
			rT.tupleNo=Integer.valueOf(e[4]);
			
			if(rT.uri.equals(rS.uri)) {
				System.out.println("Symetric relations :   "+line);
			}
		
			
			AlignmentCWA align=new AlignmentCWA(rS, rT, Integer.valueOf(e[2]));

			addToList(mapOnFirstRel, rS.uri, align);
			addToList(mapOnSecondRelation,rT.uri, align);
				
		}
		pairReader.close();
		return;
	}

	public static void addToList(HashMap<String, ArrayList<ComputingIntraCWA.AlignmentCWA>> map, String key, AlignmentCWA a){
		ArrayList<ComputingIntraCWA.AlignmentCWA> list=map.get(key);
		if (list==null) {
			list= new ArrayList<ComputingIntraCWA.AlignmentCWA>();
			map.put(key, list);
		}
		list.add(a);
	}
	
	
	/** this function returns the names of the target relations that do not appeat as source relations **/
	public static final void getRelationNamesThatCanBeSkipedSinceEquivalent(HashMap<String, ArrayList<ComputingIntraCWA.AlignmentCWA>> mapOnFirstRel, HashMap<String, ArrayList<ComputingIntraCWA.AlignmentCWA>> mapOnSecondRelation) throws Exception {
	    HashSet<String> relationsCanCanBeSkipped=new HashSet<String>();
		
		Iterator<String> targetRelations=mapOnSecondRelation.keySet().iterator();
		
		
		while(targetRelations.hasNext()){
			String target=targetRelations.next();
			
			if(mapOnFirstRel.containsKey(target)){
				System.out.println("Target which appears as source: "+target+"     tupleNo="+((int)mapOnFirstRel.get(target).get(0).rS.tupleNo));
			}
			
		}
		
		
	}
	
	public static final Relation getRelationFromStringDescr(String s)throws Exception{
		
		String uri=(s.endsWith("-"))?s.substring(0,s.length()-1): s;
		Relation r=(s.endsWith("-"))?new Relation(uri, false):new Relation(uri, true);
		
		return r;
		
	}
	
	
	public static final void abortIfFileDoesNotExist(String pathToFile){
		File f=new File(pathToFile);
		if(!f.exists()) {
			System.err.println("  File does not exit "+pathToFile);
			System.exit(0);
		}else {
			/** System.out.println(" Load : "+file);**/
		}
	}

	
	public static void main(String[] args) throws Exception {	
		
		String dir ="/Users/adi/Dropbox/DBP/feb-sofya/"; //"feb-sofya/";  "feb-sofya/";
		
		KB yago = new KB("yago", "http://s6.adam.uvsq.fr:8892/sparql", "http://yago-knowledge.org");
		KB dbpedia = new KB("dbpedia", "http://s6.adam.uvsq.fr:8892/sparql", "http://dbpedia.org");
		KB freebase = new KB("freebase", "http://s6.adam.uvsq.fr:8892/sparql", "http://rdf.freebase.com");
		
		KB kb=freebase;
		String fileWithQuivalences=dir+kb.name+"/"+kb.name+"_"+kb.name+"_equi_ee.txt";
		abortIfFileDoesNotExist(fileWithQuivalences);
		
		HashMap<String, ArrayList<ComputingIntraCWA.AlignmentCWA>> mapOnFirstRel= new HashMap<String, ArrayList<ComputingIntraCWA.AlignmentCWA>> (); 
		HashMap<String, ArrayList<ComputingIntraCWA.AlignmentCWA>> mapOnSecondRelation = new HashMap<String, ArrayList<ComputingIntraCWA.AlignmentCWA>> ();
		loadEquivalentRelations(mapOnFirstRel, mapOnSecondRelation, fileWithQuivalences, true);
		getRelationNamesThatCanBeSkipedSinceEquivalent(mapOnFirstRel, mapOnSecondRelation);
	}

	
	
}
