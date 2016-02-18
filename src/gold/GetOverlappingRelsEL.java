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
import java.util.HashMap;
import java.util.HashSet;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;

import gold.GetOvelappingRels.Alignment;
import string.SPARQLStringEncoding;



public class GetOverlappingRelsEL {

	
	/****************************************************************************/
	/** LOAD RELATIONS WITH FUNCT **/
	/****************************************************************************/
	public static final String separatorSpace = "\\s+";

	public static final ArrayList<Relation> loadELRelationsFromFilesWithFunctionality(String file, boolean skipFirstLine,
			int columnRelation, int columnDirectFunc, int columnInvFunc, int columnTupleNo) throws Exception {

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
			double tupleNo = Double.valueOf(e[columnTupleNo]);

			Relation r =  new Relation(relation, true, functOfDirect, functOfInverse, tupleNo);
					

			System.out.println(relation + " " + functOfDirect + " " + functOfInverse);

			if (!list.contains(relation)) {
				list.add(r);
			}
		}
		pairReader.close();
		return list;
	}
	
	
	
	
	/****************************************************************************/
	/** EXTRACT SUBJECTS/PAIRS  FOR EL RELATIONS **/
	/****************************************************************************/
	public static final String separatorForELPairs=" %%% ";
	
	public static final void extractPairsWithSubjectsMappedToTarget(String r, KB kb, String prefixTarget, String fileWithPairs)
			throws Exception {
		System.out.println("Extract pairs for " + r);
		String querystr = "  PREFIX owl: <http://www.w3.org/2002/07/owl#> \n"
				+ "select  ?x2 ?y  where {graph <" + kb.name + "> {\n";
		querystr +=  "?x  <" + r + "> ?y. \n" ;
		querystr += "?x  owl:sameAs ?x2. \n";
		querystr += "FILTER ( strstarts(str(?x2), \"" + prefixTarget + "\") ).\n";
		querystr += "   } } ";
		//System.out.println(querystr);
		BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(fileWithPairs), "UTF-8"));

		QueryEngineHTTP query = new QueryEngineHTTP(kb.endpoint, querystr);
		query.setTimeout(100000000000l);
		ResultSet rst = query.execSelect();
		if (rst != null) {
			while (rst.hasNext()) {
				QuerySolution qs = rst.next();
				RDFNode x2 = (RDFNode) qs.get("?x2");
				String y=qs.get("?y").asLiteral().getString();
				String line=x2+separatorForELPairs+y;
				//System.out.println(line);
				writer.write(line + "\n");
			}
			writer.close();
			System.out.println("				.... finished extracting pairs for " + r);
		}else{
			System.err.println("				.... No results for " + r);
		}
	}
	
	
	public static final void extractSubjectsMappedToTarget(Relation r, KB kb, String prefixTarget, String fileWithSubjects)
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
		query.setTimeout(100000000000l);
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
	
	public static final void extractSubjectsFromKB(Relation r, KB kb,  String fileWithSubjects)
			throws Exception {
		System.out.println("Extract pairs for " + r);
		String querystr = "  PREFIX owl: <http://www.w3.org/2002/07/owl#> \n"
				+ "select distinct ?x  where {graph <" + kb.name + "> {\n";
		querystr += (r.isDirect) ? "?x  <" + r.uri + "> ?y. \n" : "?y <" + r.uri + "> ?x. \n";
		querystr += "   } } ";

		// System.out.println(querystr);
		BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(fileWithSubjects), "UTF-8"));

		QueryEngineHTTP query = new QueryEngineHTTP(kb.endpoint, querystr);
		query.setTimeout(100000000000l);
		ResultSet rst = query.execSelect();
		if (rst != null) {
			while (rst.hasNext()) {
				QuerySolution qs = rst.next();
				RDFNode x = (RDFNode) qs.get("?x");
				//System.out.println(x2 );
				writer.write(x + " \n");
			}
			writer.close();
			System.out.println("				.... finished extracting pairs for " + r);
		}else{
			System.err.println("				.... No results for " + r);
		}
	}
	
	
	/****************************************************************************/
	/** GET EL PAIR-RELATIONS WITH MATCHING SUBJECTS **/
	/****************************************************************************/
	public static final void alignRelationsWithMatchingSubjects(int tuplesPerQuery, String dir, String tmpDir, KB source, KB target) throws Exception{
		String fileWithRelations = dir + source.name + "/" + source.name + "_functionality_el.txt";
		ArrayList<Relation> relations = loadELRelationsFromFilesWithFunctionality(fileWithRelations, true, 0, 4, 5, 3);
		getPairRelationsWithMatcingSubjects(tuplesPerQuery, tmpDir, relations, source, target);
		
	}
	
	
	 public static final void getPairRelationsWithMatcingSubjects(int tuplesPerQuery, String tmpDir, ArrayList<Relation> relations, KB source, KB target) throws Exception{
 		String fileWithSubjects = tmpDir + "subjects_el.txt";
		String fileWithAlignements =  tmpDir +source.name+"_"+target.name+"_align_el.txt";
	
		BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(fileWithAlignements), "UTF-8"));
		for (Relation r : relations) {
			System.out.println(r);
			extractSubjectsMappedToTarget(r, source, target.resourcesDomain,  fileWithSubjects);
		
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
			query.setTimeout(100000000000l);
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
	
	 /****************************************************************/
    /*** GET TYPES FOR OBJECTS OF EL RELATIONS **/
    /****************************************************************/
	public static final void getTypesForELRelations(String dir, String tmpDir, KB kb) throws Exception{
		String fileWithRelations = dir + kb.name + "/" + kb.name + "_functionality_el.txt";
		String fileWithTypes =  tmpDir +kb.name+"_types_el.txt";
		ArrayList<Relation> relations = loadELRelationsFromFilesWithFunctionality(fileWithRelations, true, 0, 4, 5, 3);
		int Xno=1000;
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileWithTypes), "UTF-8"));
		 for(Relation r: relations){
			 HashSet<String> types=null;
			
			 types=getTypesForELRelationAndGroupOfSubjects(kb, r.uri, Xno);
		
			 for(String t: types){
				 String line=r+"  "+t;
				 System.out.println(line);
				 writer.write(line+"\n");
				 writer.flush();
			 }
			 System.out.println(" ");
			 writer.write("\n");
		}
		writer.close();
	}
	
	public static final int getNoOfPairsForRelation(KB kb, String relation ){
		String queryStr=" select  (count(*) as ?n) where {graph <" + kb.name + "> {\n";
		queryStr+=" ?x <"+relation+"> ?y. ";
		queryStr+=" } }  \n";
	
		//System.out.println("Query: "+queryStr);
		QueryEngineHTTP query = new QueryEngineHTTP(kb.endpoint, queryStr);
		query.setTimeout(100000000000l);
		ResultSet rst = query.execSelect();
		if (rst != null) {
			while (rst.hasNext()) {
				QuerySolution qs = rst.next();
				RDFNode n = (RDFNode) qs.get("?n");
				System.out.println("Found "+n.asLiteral().getString());
				return Integer.getInteger(n.asLiteral().getString());
			}
		}
		return -1;
	}
	
	
    public static final  HashSet<String>  getTypesForELRelationAndGroupOfSubjects(KB kb, String relation, int Xno ){
    	HashSet<String> typesForObjectsGivenGroupOfSubjects= new HashSet<String>();
    		String queryStr=" select  ?t  where {graph <" + kb.name + "> {\n";
    		queryStr+=" {select  ?x ?y  where {\n";
    		queryStr+=" ?x <"+relation+"> ?y. ";
    		queryStr+=" } order by ?x ?y limit "+Xno+"}  \n";
    		queryStr+="  FILTER isLiteral(?y).  \n";
    		queryStr+="  BIND (datatype(?y) as ?t) \n";
    		queryStr+=" }} group by ?t ";
    		//System.out.println("Query: "+queryStr);
    		QueryEngineHTTP query = new QueryEngineHTTP(kb.endpoint, queryStr);
    		query.setTimeout(100000000000l);
    		ResultSet rst = query.execSelect();
			if (rst != null) {
				while (rst.hasNext()) {
					QuerySolution qs = rst.next();
					RDFNode t = (RDFNode) qs.get("?t");
					System.out.println(relation+"        "+t);
					typesForObjectsGivenGroupOfSubjects.add((t==null)?null:t.toString());
				}
			}else{
			}	
    		return typesForObjectsGivenGroupOfSubjects;
    }
    
    
	/****************************************************************************/
	/** FIND EXCT MATCH FOR EL RELATIONS **/
	/****************************************************************************/
    public static final void alignWithExactMatch(int tuplesPerQuery , String fileWithRelations, String tmpDir, KB S, KB T) throws Exception{
		
		ArrayList<Relation> relations= loadELRelationsFromFilesWithFunctionality(fileWithRelations, true, 0, 4, 5, 3);

		String fileWithPairs =tmpDir +S.name+"_pairs_el.txt";
		String fileWithAlignements =  tmpDir +S.name+"_"+T.name+"_align_exact_el.txt";
		
		BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(fileWithAlignements), "UTF-8"));
	
		String header="source target sharedXY originalXY pcaDenRelDirectCheckCounterpartForObject ";
		System.out.println(header);
		for (Relation r : relations) {
			System.out.println(r);
			extractPairsWithSubjectsMappedToTarget(r.uri, S, T.resourcesDomain, fileWithPairs);
			HashMap<Relation,  Alignment> relationsAtOther=iterateAndComputeSharedPairs(tuplesPerQuery, fileWithPairs,  T);
			
			//System.exit(0);
			if(relationsAtOther.keySet().isEmpty()) continue;
			iterateAndComputePCADenominator_V2(tuplesPerQuery, fileWithPairs, T, S.resourcesDomain, relationsAtOther);
			
			boolean hasSolutions=false;
			for(Relation rO:relationsAtOther.keySet()){
				Alignment struct=relationsAtOther.get(rO);
				String line=r+" "+rO+" "+struct.sharedXY+" "+struct.originalSamples+" "+struct.pcaDenominatorSourceToTargetWithCounterPartForObject;
				System.out.println(line);
				writer.write(line+"\n");
				hasSolutions=true;
				writer.flush();
			}
			if(hasSolutions) {
				System.out.println();
				writer.write("\n");
				writer.flush();
			}
		}
		writer.close();
	}
    
    public static final HashMap<Relation,  Alignment>  iterateAndComputeSharedPairs(int tuplesPerQuery, String fileWithPairs,   KB target) throws Exception{
		IteratorFromFile it= new IteratorFromFile();
	
		HashMap<Relation,  Alignment> relationsAtOther=new HashMap<Relation,  Alignment>();
		ArrayList<String> lines=null;
		
		int totalPairs=0;
		it.init(fileWithPairs);
		while((lines=it.getNextLines(tuplesPerQuery))!=null){
			System.out.print("+");
			totalPairs+=lines.size();
			/** overlap **/
			HashMap<Relation,  Integer> overlapp= getSharedForGroupOfPairs(target, lines); //testShared(target, lines); 
			
			for(Relation r:overlapp.keySet()){
				Alignment struct=relationsAtOther.get(r);
				if(struct==null) {
					struct=new Alignment(0);
					relationsAtOther.put(r, struct);
				}
				struct.sharedXY+=overlapp.get(r);	
			}
			
		}
		it.close();
		
		/** set the total number of pairs translated to the target **/
		for(Relation rel:relationsAtOther.keySet()){
			Alignment struct=relationsAtOther.get(rel);
			struct.originalSamples=totalPairs;
		}		
		System.out.println(" ");
		return  relationsAtOther;
	}
	
	
	public static final HashMap<Relation,  Integer> getSharedForGroupOfPairs(KB target, ArrayList<String> lines) throws Exception{
		 HashMap<Relation,  Integer> overlapp=new HashMap<Relation,  Integer> ();
		 
		 String querystr=" PREFIX owl: <http://www.w3.org/2002/07/owl#> \n"
				 +"select ?r   (COUNT(*) as ?n)   where {graph <" + target.name + "> {\n";
		 querystr+=" values (?x ?ys) {\n";
		 		for(String line:lines){
		 				//line=line.trim();
		 				String[] parts=line.split(separatorForELPairs);
		 				querystr+="\t (  "+"<"+parts[0]+">  "+  SPARQLStringEncoding.getSPARQLEncodingForLiteral(parts[1]) +" ) \n";
			}
			querystr+="\t}\n";
			querystr+="\t ?x ?r ?y. \n";
			querystr+=" FILTER ( str(?y) = ?ys )";
			//querystr+="\t FILTER regex(str(?y), ?ys, \"i\" ). \n";
			querystr+="\t }} group by ?r   ";
		
			//System.out.println("Relations with overlap "+querystr);
			
			try{
			QueryEngineHTTP query = new QueryEngineHTTP(target.endpoint, querystr);
			ResultSet rst = query.execSelect();
			if (rst != null) {
				while (rst.hasNext()) {
					QuerySolution qs = rst.next();
					
					String n= qs.get("?n").asLiteral().getString().trim();
					int sharedXY=Integer.valueOf(n);
					if(sharedXY==0) continue;
					
					RDFNode r = (RDFNode) qs.get("?r");
					Relation rel=new Relation(r.toString(), true);
					overlapp.put(rel, new Integer(sharedXY));
					
					//System.out.println(r+"  "+sharedXY);
				}
			}else{	
			}	
			}
			catch (Exception e) {
						System.err.println(querystr);
						System.exit(0);
			}
			
		//System.exit(0);
		return overlapp;
	}
	
	public static final  void  iterateAndComputePCADenominator_V2(int tuplesPerQuery, String fileWithPairs,   KB target, String prefixAtSource, HashMap<Relation,  Alignment> relationsAtOther) throws Exception{
		IteratorFromFile it= new IteratorFromFile();
		ArrayList<String> lines=null;
		
		/** process the file again in order to extract the PCA denominator **/
		int totalPairs=0;
		it.init(fileWithPairs);
		while((lines=it.getNextLines(tuplesPerQuery))!=null){	
			
			totalPairs+=lines.size();
			
			/** PCA direct with counterpart **/
			//System.out.println(" PCA Direct Counterpart");
		   HashMap<Relation,  Integer> denominatorWithCounterPart=getPCADenominatorForGroupOfPairs_V2(lines, relationsAtOther.keySet(), target, prefixAtSource);
			for(Relation r:denominatorWithCounterPart.keySet()){
				Alignment struct=relationsAtOther.get(r);
				if(struct==null) continue;
				struct.pcaDenominatorSourceToTargetWithCounterPartForObject+=denominatorWithCounterPart.get(r);
			}
			System.out.print("-");
		}
		it.close();
		
		/** set the total number of pairs translated to the target **/
		for(Relation rel:relationsAtOther.keySet()){
			Alignment struct=relationsAtOther.get(rel);
			struct.originalSamples=totalPairs;
		}		
		
		System.out.println(" ");
		
	}
	
	public static final  HashMap<Relation,  Integer>  getPCADenominatorForGroupOfPairs_V2(ArrayList<String> lines, Collection<Relation> relationsDirect,  KB target, String prefixAtSource) throws Exception{
		 HashMap<Relation,  Integer> denominator=new HashMap<Relation,  Integer> ();
		 String querystr= " PREFIX owl: <http://www.w3.org/2002/07/owl#> \n"
								+  "select ?r  (COUNT(?x) as ?n)   where {graph <" + target.name + "> {\n";
			querystr+=" values ?x {\n";
						for(String line:lines){
									line=line.trim();
									String[] parts=line.split(separatorSpace);
									querystr+="\t   "+"<"+parts[0]+">  \n";
								}
			querystr+="\t}\n";
			querystr+="\t { select distinct ?x ?r where { \n";
			querystr+="\t values  ?r { ";
			for(Relation r: relationsDirect){
				 	querystr+="  <"+r.uri+"> ";
			}
			querystr+= "\t } \n";
			querystr+="\t ?x ?r ?y. \n";
			querystr+="\t }}\n";
			
			querystr+="}} group by ?r   ";
			
			//System.out.println("Q Denominator: \n"+querystr);
			try{
			QueryEngineHTTP query = new QueryEngineHTTP(target.endpoint, querystr);
			query.setTimeout(100000000000l);
			ResultSet rst = query.execSelect();
			if (rst != null) {
				while (rst.hasNext()) {
					QuerySolution qs = rst.next();
					String n= qs.get("?n").asLiteral().getString().trim();
					int XYs=Integer.valueOf(n);
					if(XYs==0) continue;
					
					RDFNode r = (RDFNode) qs.get("?r");
					Relation rel=new Relation(r.toString(), true);
					denominator.put(rel, new Integer(XYs));
				}
			}
	    System.out.print("*");
			}catch(Exception e){
				System.err.println(querystr);
				System.exit(0);
			}
		return denominator;
		
	}
 
	public static void main(String[] args) throws Exception {	
	
		String dir ="/Users/adi/Dropbox/DBP/feb-sofya/"; //"feb-sofya/";
		String tmpDir ="/Users/adi/Dropbox/DBP/"; //"tmpDir";// 

		KB dbpedia = new KB("dbpedia", "http://s6.adam.uvsq.fr:8892/sparql", "http://dbpedia.org");
		KB yago = new KB("yago", "http://s6.adam.uvsq.fr:8892/sparql", "http://yago-knowledge.org");
		KB freebase = new KB("freebase", "http://s6.adam.uvsq.fr:8892/sparql", "http://rdf.freebase.com");
		
		
		//"http://dbpedia.org/ontology/birthDate";
		String fileWithRelations = tmpDir  + dbpedia.name + "_functionality_test_el.txt";
		alignWithExactMatch(100, fileWithRelations, tmpDir, dbpedia, yago);
		
		/*** get the types for the first Xno tuples **/
		//getTypesForELRelations(dir, tmpDir, freebase);
		
		/** Align relations with matching subjects **/
		//alignRelationsWithMatchingSubjects(500, dir, tmpDir, dbpedia, yago);
		
	
		
		
	}
	
}
