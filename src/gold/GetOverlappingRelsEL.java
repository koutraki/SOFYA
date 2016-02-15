package gold;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;

import gold.GetOvelappingRels.Alignment;



public class GetOverlappingRelsEL {

	
	/****************************************************************************/
	/** LOAD RELATIONS WITH FUNCT **/
	/****************************************************************************/
	public static final String separatorSpace = "\\s+";

	public static final ArrayList<Relation> loadELRelationsFromFilesWithFunctionality(String file, boolean skipFirstLine,
			int columnRelation, int columnDirectFunc, int columnInvFunc) throws Exception {

		File f = new File(file);
		if (!f.exists()) {
			System.err.println("  File does not exit " + file);
			return null;
		} else {
			/** System.out.println(" Load : "+file); **/
		}

		BufferedReader pairReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
		if (skipFirstLine)
			pairReader.readLine();

		ArrayList<Relation> list = new ArrayList<Relation>();
		String line = null;
		while ((line = pairReader.readLine()) != null) {
			if (line.isEmpty())
				continue;
			String[] e = line.split(separatorSpace);
			String relation = e[columnRelation];

			double functOfDirect = Double.valueOf(e[columnDirectFunc]);
			double functOfInverse = Double.valueOf(e[columnInvFunc]);

			Relation r =  new Relation(relation, true, functOfDirect, functOfInverse);
					

			System.out.println(relation + " " + functOfDirect + " " + functOfInverse);

			if (!list.contains(relation)) {
				list.add(r);
			}
		}
		pairReader.close();
		return list;
	}
	
	
	
	
	/****************************************************************************/
	/** EXTRACT SUBJECTS FOR EL RELATIONS **/
	/****************************************************************************/
	public static final String separatorForELPairs=" %%% ";
	
	public static final void extractSubjects(Relation r, KB kb, String prefixTarget, String fileWithSubjects)
			throws Exception {
		System.out.println("Extract pairs for " + r);
		String querystr = "  PREFIX owl: <http://www.w3.org/2002/07/owl#> \n"
				+ "select distinct ?x2  where {graph <" + kb.name + "> {\n";
		querystr += (r.isDirect) ? "?x  <" + r.uri + "> ?y. \n" : "?y <" + r.uri + "> ?x. \n";
		querystr += "?x  owl:sameAs ?x2. \n";
		querystr += "FILTER ( strstarts(str(?x2), \"" + prefixTarget + "\") ).\n";
		querystr += "   } } ";

		// System.out.println(querystr);

		BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(fileWithSubjects), "UTF-8"));

		QueryEngineHTTP query = new QueryEngineHTTP(kb.endpoint, querystr);

		ResultSet rst = query.execSelect();
		if (rst != null) {
			while (rst.hasNext()) {
				QuerySolution qs = rst.next();
				RDFNode x2 = (RDFNode) qs.get("?x2");
				//System.out.println(x2 );
				writer.write(x2 + " \n");
			}
			writer.close();
			System.out.println("				.... finished extracting pairs for " + r);
		}else{
			System.err.println("				.... No results for " + r);
		}
	}
	
	/***************************************************************************************/
	/** GET POSSIBLE EL RELATIONS FOR SET OF SUBJECTS **/
	/***************************************************************************************/
	public static final HashMap<Relation,  Integer>  getAllELRelationsForSetOfSubjects (int tuplesPerQuery, String fileWithSubjects,   KB target) throws Exception{
		IteratorFromFile it= new IteratorFromFile();
	
		HashMap<Relation,  Integer> relationsAtOther=new HashMap<Relation,  Integer>();
		ArrayList<String> lines=null;
		
		
		it.init(fileWithSubjects);
		while((lines=it.getNextLines(tuplesPerQuery))!=null){
			System.out.print("+");
			
			/** overlap **/
			HashMap<Relation,  Integer> overlapp=  getAllCandidateTargetRelationsForSubGroupOfSubjects(target, lines); //testShared(target, lines); 
			for(Relation r:overlapp.keySet()){
				Integer no=relationsAtOther.get(r);
				if(no==null) {
					relationsAtOther.put(r, overlapp.get(r));
				}
				else relationsAtOther.put(r, new Integer(no+overlapp.get(r))); 
			}
			
		}
		it.close();
		
			
		System.out.println(" ");
		return  relationsAtOther;
	}
	
	
	
	public static final HashMap<Relation,  Integer> getAllCandidateTargetRelationsForSubGroupOfSubjects(KB target, ArrayList<String> lines) throws Exception{
		 HashMap<Relation,  Integer> overlapp=new HashMap<Relation,  Integer> ();
		 
		 String querystr=" PREFIX owl: <http://www.w3.org/2002/07/owl#> \n"
				 +"select ?r  (COUNT(distinct ?x) as ?n)   where {graph <" + target.name + "> {\n";
		 querystr+=" values ?x {\n";
		 		for(String line:lines){
		 				line=line.trim();
		 				querystr+="  "+"<"+line+">  "+"  \n";
			}
			querystr+="\t}\n";
			querystr+= " ?x ?r ?y. ";
			querystr+= "FILTER isLiteral(?y). ";
			querystr+="}} group by ?r  ";
			
			//System.out.println("Query for extracting EL relations at target \n\t "+querystr);
			
			QueryEngineHTTP query = new QueryEngineHTTP(target.endpoint, querystr);
			ResultSet rst = query.execSelect();
			if (rst != null) {
				while (rst.hasNext()) {
					QuerySolution qs = rst.next();
					
					String n= qs.get("?n").asLiteral().getString().trim();
					int noSubjectsWithR=Integer.valueOf(n);
					if(noSubjectsWithR==0) continue;
					
					RDFNode r = (RDFNode) qs.get("?r");
					Relation rel=new Relation(r.toString(), true);
					overlapp.put(rel, new Integer(noSubjectsWithR));
					
					//System.out.println("Extract "+r+"    Xs="+n);
				}
			}else{
				
			}	
		 return overlapp;
	}
	
	 /*************************/
    /*** get all the types **/
    /************************/
    public static final void getTypesForELRelations(KB kb, String relation ){
    	
    		String queryStr=" select  ?t  \n";
    		queryStr+=" where { ?x <"+relation+"> ?y.  \n";
    		queryStr+="  FILTER isLiteral(?y).  ";
    		queryStr+="  BIND (datatype(?y) as ?t) ";
    		queryStr+=" } group by ?t ";
    		
    		
    		QueryEngineHTTP query = new QueryEngineHTTP(kb.endpoint, queryStr);
			ResultSet rst = query.execSelect();
			if (rst != null) {
				while (rst.hasNext()) {
					QuerySolution qs = rst.next();
					RDFNode t = (RDFNode) qs.get("?t");
					System.out.println(relation+"        "+t);
				}
			}else{
				
			}	
    		
    }
	
	public static void main(String[] args) throws Exception {	
	
		String dir ="/Users/adi/Dropbox/DBP/feb-sofya/"; //"feb-sofya/";
		String tmpDir ="/Users/adi/Dropbox/DBP/"; //"tmpDir";// 

		KB source = new KB("dbpedia", "http://s6.adam.uvsq.fr:8892/sparql", "http://dbpedia.org");
		KB target = new KB("yago", "http://s6.adam.uvsq.fr:8892/sparql", "http://yago-knowledge.org");
		
	
		String fileWithRelations = dir + source.name + "/" + source.name + "_funct_el.txt";
		ArrayList<Relation> relations = loadELRelationsFromFilesWithFunctionality(fileWithRelations, true, 0, 4, 5);

		for(Relation r: relations){
			getTypesForELRelations(source, r.uri);
		}
		System.exit(0);
		
		String fileWithSubjects = tmpDir + "subjects_el.txt";
		String fileWithAlignements =  tmpDir +source.name+"_"+target.name+"_align_el.txt";
		int tuplesPerQuery = 1000;
		
		BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(fileWithAlignements), "UTF-8"));
	
		String header="source target subjectsWithTargetRelation ";
		System.out.println(header);
		for (Relation r : relations) {
			System.out.println(r);
			extractSubjects(r, source, target.resourcesDomain,  fileWithSubjects);
		
			HashMap<Relation,  Integer> atTarget=getAllELRelationsForSetOfSubjects(tuplesPerQuery, fileWithSubjects, target);
			
			for(Relation rt: atTarget.keySet()){
				String line=r+"     "+ rt+"    "+atTarget.get(rt);
				System.out.println(line);
				writer.write(line+"\n");
				writer.flush();
			}
			System.out.println();
			
		}
		writer.close();

	}
	
}
