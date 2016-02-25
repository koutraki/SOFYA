package gold;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;




public class GetSelfOvelappingRels {

	/****************************************************************************/
	/** LOAD RELATIONS WITH FUNCT **/
	/****************************************************************************/
	public static final String separatorSpace = "\\s+";

	public static final void loadRelationsFromFilesWithFunctionality(Collection<Relation> list, String file, boolean skipFirstLine,
			int columnRelation, int columnDirectFunc, int columnInvFunc, int columnTupleNo) throws Exception {

		File f = new File(file);
		if (!f.exists()) {
			System.err.println("  File does not exit " + file);
			return;
		} else {
			/** System.out.println(" Load : "+file); **/
		}

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
			if (!list.contains(relation)) {
				list.add(r);
			}
		}
		pairReader.close();
		return;
	}

	/*************************************************************************************************************/
	/**  SIMPLE ALIGN **/
	/**************************************************************************************************************/
	public static final void selfAlign(String dir, String tmpDir, KB kb) throws Exception{
		String fileWithRelations = dir + kb.name + "/" + kb.name + "_functionality_ee.txt";
		ArrayList<Relation> relations= new ArrayList<Relation>();
		loadRelationsFromFilesWithFunctionality(relations, fileWithRelations, true, 0, 4, 5, 3);

		String fileWithAlignements =  tmpDir +kb.name+"_"+kb.name+"_align.txt";
				
		BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(fileWithAlignements), "UTF-8"));
	
		String header="source target sharedXY originalXY pcaDenRelDirectCheck ";
		System.out.println(header);
		for (Relation r : relations) {
			
			
			HashMap<Relation,  Integer> overlap=getSharedForRelation(kb, r);
			
			for(Entry<Relation,Integer> rT:overlap.entrySet()){
				String line=r+" \t  "+rT.getKey()+"  \t  "+rT.getValue();
				System.out.println(line);
				writer.write(line+"\n");
				writer.flush();
			}
			
			writer.write("\n");
			System.out.println();
			
		}
		writer.close();
	}
	
	/**************************************************************************************************************/
	/** GIVEN A RELATION, FIND THE OVERLAPPING RELATIONS **/
	/**************************************************************************************************************/
	public static final HashMap<Relation,  Integer> getSharedForRelation(KB kb, Relation rS ) throws Exception{
		 HashMap<Relation,  Integer> overlapp=new HashMap<Relation,  Integer> ();
		 
		 String querystr="select ?r ?d  (COUNT(*) as ?n)   where {graph <" + kb.name + "> {\n";
		    querystr+=(rS.isDirect)?" ?x <"+rS.uri+"> ?y. ":" ?y <"+rS.uri+"> ?x. ";
			querystr+= getSubQueryForDirection(true);
			querystr+=" UNION \n";
			querystr+= getSubQueryForDirection(false);
			querystr+="}} group by ?r ?d  ";
			
			//System.out.println("Query for detecting overlapping relations: "+querystr);
			
			QueryEngineHTTP query = new QueryEngineHTTP(kb.endpoint, querystr);
			ResultSet rst = query.execSelect();
			if (rst != null) {
				while (rst.hasNext()) {
					QuerySolution qs = rst.next();
					
					String n= qs.get("?n").asLiteral().getString().trim();
					int sharedXY=Integer.valueOf(n);
					if(sharedXY==0) continue;
					
					RDFNode r = (RDFNode) qs.get("?r");
					String d = qs.get("?d").asLiteral().getString().trim();
					boolean dir=(d.equals("direct"))?true:false;
					Relation rel=new Relation(r.toString(), dir);
					overlapp.put(rel, new Integer(sharedXY));
				}
			}else{
				
			}	
		 return overlapp;
	}
	


	public static final String getSubQueryForDirection(boolean direction){
		String query=" ";
		query+=" {select  ?x ?r ?y ?d where { ";
				query+=" values  ?d { ";
				query+=(direction)?" \"direct\" ":" \"indirect\" ";
				query+= " } ";
				query+= (direction)?" ?x ?r ?y. ":" ?y ?r ?x. ";
				
				query+="}}   \n";
		return query;		
	}
	

	
	
	/*************************************************************************************************************/
	/**  SPEED-UP PROCESSING BY GETTING PARTIAL RESULTS FOR S TO T FROM T TO S **/
	/**************************************************************************************************************/
	public static final void alignByCompletingPartialResults(String dir, String tmpDir, KB S, KB T, String lastProcessedOK) throws Exception{
		String fileWithPairs = tmpDir + "pairs.txt";
		String fileWithAlignements =  tmpDir +S.name+"_"+T.name+"_align_2.txt";
		int tuplesPerQuery = 200; // changed!!!
		
		
		/** read the relations for the target, in the correct direction according to the functionality **/
		String fileWithRelations = dir + S.name + "/" + S.name + "_functionality_ee.txt";
		ArrayList<Relation> sortedRelations= new ArrayList<Relation>();
		loadRelationsFromFilesWithFunctionality(sortedRelations, fileWithRelations, true, 0, 4, 5, 3);
		//Collections.sort(sortedRelations, new Relation.RelationCompBasedOnTupleNo());
		
		
		/** get the target to source results from the file **/
		HashSet<Relation> relSetAtS=new HashSet<Relation>();
		relSetAtS.addAll(sortedRelations);
		HashMap<Relation, HashMap<Relation, SelfAlignment>>   StoT_partialResults= read_TtoS_results(dir, S, relSetAtS, T);
		
			
		BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(fileWithAlignements), "UTF-8"));
	
		String header="source target sharedXY originalXY pcaDenRelDirectCheckCounterpartForObject ";
		System.out.println(header);
		
		boolean startProcessing=(lastProcessedOK==null)?true:false;
		for (Relation rS : sortedRelations) {
			if(! startProcessing){
				if(rS.toString().equals(lastProcessedOK)) startProcessing=true;
				continue;
			}
			
			if(! StoT_partialResults.containsKey(rS)) continue;
			HashMap<Relation, SelfAlignment> relationsAtOther=StoT_partialResults.get(rS);
			if(relationsAtOther.keySet().isEmpty()) continue;
			 
			System.out.println(rS);
			
		}
		writer.close();
	}
	
	

	
	public static final HashMap<Relation, HashMap<Relation, SelfAlignment>>  read_TtoS_results(String dir, KB S, HashSet<Relation> relSetAtS,  KB T) throws Exception{
		//read the relations for the target, in the correct direction according to the functionality
	//	String fileWithResults = dir +"_gold/" +T.name+ "->" + S.name +"/" + T.name+"_"+S.name + "_align.txt";
		String fileWithResults = dir +"_gold/" + T.name+"_"+S.name + "_align_2.txt";
		
		File f = new File(fileWithResults);
		if (!f.exists()) {
			System.err.println("  File does not exit " + fileWithResults);
			System.exit(0);
		} 

		HashMap<Relation, HashMap<Relation, SelfAlignment>>  StoT_partialResults= new HashMap<Relation, HashMap<Relation, SelfAlignment>> ();
		
		BufferedReader pairReader = new BufferedReader(new InputStreamReader(new FileInputStream(fileWithResults), "UTF8"));
		String line = null;
		while ((line = pairReader.readLine()) != null) {
			if (line.isEmpty()) continue;
		
			//System.out.println(line);
			
			String[] e = line.split(separatorSpace);
			Relation rT=Relation.getRelationFromStringDesc(e[0]);
			Relation rS=Relation.getRelationFromStringDesc(e[1]);
			
			int shared = Integer.valueOf(e[2]);
			
			/** inverse the rS (and rT) **/
			if(! relSetAtS.contains(rS)){
				   rS=new Relation(rS.uri, !rS.isDirect);
				   rT=new Relation(rT.uri, !rT.isDirect);
				   if(!relSetAtS.contains(rS)) {
					   System.err.println("Relation removed from functionality file: "+rS);
					   continue;
					//   System.err.println("Big problem: I couldn't find rel "+rS);
					//   System.exit(0);
				   }
			}
		 
			SelfAlignment a=new SelfAlignment(0);  //the number of original samples is unknown 
			a.sharedXY=shared;
			
			/** insert the alignment in the partial results**/
			HashMap<Relation, SelfAlignment> alignmentsFor_rS=StoT_partialResults.get(rS);
			if(alignmentsFor_rS==null) {
				alignmentsFor_rS= new HashMap<Relation, SelfAlignment> ();
				StoT_partialResults.put(rS, alignmentsFor_rS);
			}
			
			if(alignmentsFor_rS.containsKey(rT)){
				   System.err.println("There are two lines that align "+rS+" and "+rT);
				   System.exit(0);
			}
			
			alignmentsFor_rS.put(rT, a);
		}
		
		pairReader.close();
		return StoT_partialResults;
	}

	
	
	/*****************************************************************/
	/**  Alignment Class**/
	/*****************************************************************/
	public static class  SelfAlignment{
		public int sharedXY=0;
		public int originalSamples=0;
		public int pcaDenominatorSourceToTarget=0;
		
		
		public SelfAlignment(int originalSamples){
			this.originalSamples=originalSamples;
		}
	}
	
	public static void main(String[] args) throws Exception {	
		
		String dir ="/home/mary/Dropbox/feb-sofya/"; //"/home/mary/Dropbox/feb-sofya/"; //"feb-sofya/";
		String tmpDir = "/home/mary/Dropbox/"; //"/Users/adi/Dropbox/DBP/"; // "tmpDir/";  

		KB yago = new KB("yago", "http://s6.adam.uvsq.fr:8892/sparql", "http://yago-knowledge.org");
		KB dbpedia = new KB("dbpedia", "http://s6.adam.uvsq.fr:8892/sparql", "http://dbpedia.org");
		KB freebase = new KB("freebase", "http://s6.adam.uvsq.fr:8892/sparql", "http://rdf.freebase.com");
		
		selfAlign(dir, tmpDir, freebase);
		System.exit(0);
		
		
		
		String lastProcessedOK=(args.length==0)?null:args[0];
		System.out.println("lastProcessedOK: "+lastProcessedOK);
		
		alignByCompletingPartialResults(dir, tmpDir, freebase, freebase,  lastProcessedOK);
		
		//test(target);
		//System.exit(0);
	}
	
	

}
