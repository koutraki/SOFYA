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
	public static final void selfAlign(String dir, String tmpDir, KB kb, String fromRelation, int bulkSize) throws Exception{
		String fileWithRelations = dir + kb.name + "/" + kb.name + "_functionality_ee.txt";
		ArrayList<Relation> relations= new ArrayList<Relation>();
		loadRelationsFromFilesWithFunctionality(relations, fileWithRelations, true, 0, 4, 5, 3);

		String fileWithAlignements =  tmpDir +kb.name+"_"+kb.name+"_align.txt";
				
		BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(fileWithAlignements), "UTF-8"));
	
		String header="source target sharedXY originalXY pcaDenRelDirectCheck ";
		System.out.println(header);
		
		boolean start=(fromRelation==null)?true:false;
		for (Relation r : relations) {
			if( ! start ){
				if(r.toString().equals(fromRelation)) start=true;
				continue;
			}
			
			HashMap<Relation,  Integer> overlap=null;
			if(r.tupleNo<5000)  overlap=getSharedForRelation(kb, r);
			else overlap=getSharedForRelationByIteration(kb, r, bulkSize);
			
			for(Entry<Relation,Integer> rT:overlap.entrySet()){
				if(rT.getKey().equals(r)) continue;
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
		    querystr+=(rS.isDirect)?" ?x <"+rS.uri+"> ?y. ":" ?y <"+rS.uri+"> ?x. \n";
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
	
	public static final HashMap<Relation,  Integer> getSharedForRelationByIteration(KB kb, Relation rS, int bulkSize ) throws Exception{
		 HashMap<Relation,  Integer> overlapp=new HashMap<Relation,  Integer> ();
		 
		 int offset=0;
		
		 
		// http://dbpedia.org/ontology/careerStation
			 
			 
		 while(offset<rS.tupleNo){
			 
			 String querystr="select ?r ?d  (COUNT(*) as ?n)   where { ";
			 querystr+= " graph <" + kb.name + "> ";
			 querystr+="	 {\n";
			 querystr+=" { select ?x ?y where { ";
			 querystr+="\t "+((rS.isDirect)?" ?x <"+rS.uri+"> ?y. ":" ?y <"+rS.uri+"> ?x. ");
			 querystr+=" }  order by ?x ?y  ";
			 querystr+=(offset==0)?" ":" offset "+offset+" ";
		
			 querystr+=" limit "+((int)((offset+bulkSize<rS.tupleNo)?bulkSize:rS.tupleNo-offset))+"  ";
			 offset+=bulkSize;
			 querystr+=" } \n";
			 
			 querystr+= getSubQueryForDirection(true);
			 querystr+=" UNION \n";
			 querystr+= getSubQueryForDirection(false);
			 querystr+="}} group by ?r ?d  ";
			
			System.out.println("Query for detecting overlapping relations: \n "+querystr);
			
			QueryEngineHTTP query = new QueryEngineHTTP(kb.endpoint, querystr);
			query.setTimeout(-100);
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
					
					int existent=overlapp.containsKey(rel)?overlapp.get(rel):0;
					overlapp.put(rel, new Integer(sharedXY+existent));
				}
			}else{
				
			}	
			
			
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
		
		String dir ="feb-sofya/"; // "/Users/adi/Dropbox/DBP/feb-sofya/";  "/home/mary/Dropbox/feb-sofya/"; "/home/mary/Dropbox/feb-sofya/"; 
		String tmpDir ="tmpDir/"; // "/Users/adi/Dropbox/DBP/";  "/home/mary/Dropbox/"; //"/Users/adi/Dropbox/DBP/"; 

		KB yago = new KB("yago", "http://s6.adam.uvsq.fr:8892/sparql", "http://yago-knowledge.org");
		KB dbpedia = new KB("dbpedia", "http://s6.adam.uvsq.fr:8892/sparql", "http://dbpedia.org");
		KB freebase = new KB("freebase", "http://s6.adam.uvsq.fr:8892/sparql", "http://rdf.freebase.com");
		
		int bulkSize=2000;
		try{
					selfAlign(dir, tmpDir, dbpedia, (args.length==0)?null:args[0], bulkSize);
		}  catch(Exception e ){
			  System.err.println(e.getMessage());
			  e.printStackTrace();
		}
		
		
		//nohup java -cp "./lib/*:sofya_gold_fbTodbpedia.jar" gold.GetOvelappingRels http://rdf.freebase.com/ns/location.location.people_born_here- &

	
	}
	
	

}
