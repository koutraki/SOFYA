package gold;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import com.sun.xml.internal.ws.resources.SoapMessages;

import gold.GetOvelappingRels.Alignment;

public class GetSelfPCAConf {
	public static final String separatorSpace = "\\s+";
	
	public static final HashMap<String, Relation> loadRelationsFromFilesWithFunctionality(String file, boolean skipFirstLine,
			int columnRelation, int columnDirectFunc, int columnInvFunc, int columnTupleNo) throws Exception {
       
		HashMap<String, Relation> relations=new HashMap<String, Relation>();
	
		BufferedReader pairReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
		if (skipFirstLine)
			pairReader.readLine();

		String line = null;
		while ((line = pairReader.readLine()) != null) {
			if (line.isEmpty())
				continue;
			String[] e = line.split(separatorSpace);
			String relation = e[columnRelation];

			double functOfDirect = Double.valueOf(e[columnDirectFunc]);
			double functOfInverse = Double.valueOf(e[columnInvFunc]);
			double tupleNo = Double.valueOf(e[columnTupleNo]);

			Relation r = (functOfDirect >= functOfInverse) ? new Relation(relation, true, functOfDirect, functOfInverse, tupleNo)
					: new Relation(relation, false, functOfInverse, functOfDirect, tupleNo);

			//System.out.println(relation + " " + functOfDirect + " " + functOfInverse);

			relations.put(relation, r);
			
		}
		pairReader.close();
		return relations;
	}
	
	///isql-v 1111 dba dba exec="SPARQL select ?r ?d  (COUNT(*) as ?n)   where {  graph <dbpedia> { { select ?x ?y where { ?y <http://dbpedia.org/ontology/country> ?x.  }  order by ?x ?y     }  {select  ?x ?r ?y ?d where {  values  ?d {  'indirect'  }  ?y <http://dbpedia.org/ontology/isPartOf> ?x. }} }} group by ?r ?d" > out.txt
	  
	/*************************************************************************************************************/
	/**  SPEED-UP PROCESSING BY GETTING RESULT FOR OVERLAP**/
	/**************************************************************************************************************/
	public static final void getHashMapWithOverlap(String rootDir, String tmpDir, KB kb) throws Exception{
	
		String fileWithAlignments=rootDir+kb.name+"/"+kb.name+"_"+kb.name+"_align.txt";
		String fileWithRelations = rootDir+kb.name+"/"+ kb.name + "_functionality_ee.txt";
		
		HashMap<String, Relation> relations=loadRelationsFromFilesWithFunctionality( fileWithRelations, true, 0, 4, 5, 3);
		
		HashMap<Relation, HashMap<Relation, PartialAlignment>>   partialResults=read_results_overlap(fileWithAlignments, relations, kb);
		
		BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(fileWithAlignments), "UTF-8"));
	
		String header="source target sharedXY originalXY pcaDenRelDirectCheckCounterpartForObject ";
		System.out.println(header);
		
	
		ArrayList<Relation> sortedRelations=new ArrayList<Relation>();
		sortedRelations.addAll(relations.values());
		Collections.sort(sortedRelations, new Relation.RelationCompBasedOnTupleNo());
		
		String fileWithPairs=tmpDir+"self_pairs.txt";
		for (Relation rS : sortedRelations) {
			
			if(! partialResults.containsKey(rS)) continue;
			HashMap<Relation, PartialAlignment> relationsAtOther=partialResults.get(rS);
			if(relationsAtOther==null || relationsAtOther.keySet().isEmpty()) continue;
			 
			System.out.println("I process relation "+rS);
		}
		writer.close();
	}
	  
	
	public static final HashMap<Relation, HashMap<Relation, PartialAlignment>>  read_results_overlap(String fileWithResults, HashMap<String,Relation> relations,  KB kb) throws Exception{	
		HashMap<Relation, HashMap<Relation, PartialAlignment>>  partialResults= new HashMap<Relation, HashMap<Relation, PartialAlignment>> ();
		
		BufferedReader pairReader = new BufferedReader(new InputStreamReader(new FileInputStream(fileWithResults), "UTF8"));
		String line = null;
		while ((line = pairReader.readLine()) != null) {
			line.trim();
			if (line.isEmpty()) continue;
			
			String[] e = line.split(separatorSpace);
			
			String rel1=e[0].trim();
			rel1=(rel1.endsWith("-"))?rel1.substring(0, rel1.length()-1): rel1;
			
			String rel2=e[1].trim();
			rel2=(rel2.endsWith("-"))?rel2.substring(0, rel2.length()-1): rel2;
			
			Relation r1=relations.get(rel1);
			if(r1==null){
				System.out.println(" Relation "+r1+" est inconnue ");
				System.exit(0);
			}
			
			Relation r2=relations.get(rel2);
			if(r2==null){
				System.out.println(" Relation "+r2+" est inconnue ");
				System.exit(0);
			}
			
			int shared = Integer.valueOf(e[2]);
			
			addToPartialResults(shared, r1, r2, partialResults);
			addToPartialResults(shared, r1, r2, partialResults);
			
		}
		
		pairReader.close();
		return partialResults;
	}

	public static final void addToPartialResults(int shared, Relation r1, Relation r2, HashMap<Relation, HashMap<Relation, PartialAlignment>> map){
		HashMap<Relation, PartialAlignment> alignmentsForR1=map.get(r1);
		if(alignmentsForR1==null){
			alignmentsForR1=new HashMap<Relation, PartialAlignment>();
			map.put(r1, alignmentsForR1);
		}
		
		PartialAlignment a=alignmentsForR1.get(r2);
		if(a==null){
			 a=new PartialAlignment(r1, r2, shared, (int)r1.tupleNo);
			 alignmentsForR1.put(r2, a);
		}
	
	}
	
	
	public static final class PartialAlignment {

		public static final DecimalFormat df = new DecimalFormat("0.00");
		
		public final Relation rS;
		public final Relation rT;
		
		
		public final int sharedXY;
		public final int originalSamples;
		public int pcaDenominator;
		
		public double pca;
		public final double cwa;

		
		public PartialAlignment(Relation rS, Relation rT, int sharedXY, int originalSamples){
			this.sharedXY=sharedXY;
			this.originalSamples=originalSamples;
			
			this.rS=rS;
			this.rT=rT;
			
			this.cwa=((double)sharedXY)/originalSamples;
			
		}
		
		@Override
		public final String toString(){
			return  rS + " \t " + rT;
		}
		
		public  String toStringAll(){
					return  rS + "   " + rT+ "  "+ sharedXY+ "  "+ originalSamples+ "  "+ pcaDenominator+ "  "+ df.format(pca)+ "  " +df.format(cwa);
		}
		
		public final String toStringWithShared(){
			return  rS + "  " + rT+ "  "+ sharedXY;

	  }
		
		@Override
		public boolean equals(Object o){
			if(! (o instanceof PartialAlignment)) return false;
			PartialAlignment other=(PartialAlignment) o;
			if(other.rS.equals(rS) && other.rT.equals(rT)) return true;
			return false;
		}
		
		@Override
		public int hashCode(){
			return (rS+" "+rT).hashCode();
		}
		
		}
	
	
	
 public static void main(String[] args) throws Exception {	
		
		String dir ="/Users/adi/Dropbox/DBP/feb-sofya/"; //"feb-sofya/";  "/home/mary/Dropbox/feb-sofya/"; "/home/mary/Dropbox/feb-sofya/"; 
		String tmpDir ="/Users/adi/Dropbox/DBP/"; //"tmpDir/"; //   "/home/mary/Dropbox/"; //"/Users/adi/Dropbox/DBP/"; 

		KB yago = new KB("yago", "http://s6.adam.uvsq.fr:8892/sparql", "http://yago-knowledge.org");
		KB dbpedia = new KB("dbpedia", "http://s6.adam.uvsq.fr:8892/sparql", "http://dbpedia.org");
		KB freebase = new KB("freebase", "http://s6.adam.uvsq.fr:8892/sparql", "http://rdf.freebase.com");
		
		int bulkSize=2000;
		try{
				getHashMapWithOverlap(dir, tmpDir, dbpedia);
		}  catch(Exception e ){
			  System.err.println(e.getMessage());
			  e.printStackTrace();
		}
		
		
		//nohup java -cp "./lib/*:sofya_gold_fbTodbpedia.jar" gold.GetOvelappingRels http://rdf.freebase.com/ns/location.location.people_born_here- &

	
	}
	
}
