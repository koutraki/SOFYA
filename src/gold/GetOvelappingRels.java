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




public class GetOvelappingRels {

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

	/****************************************************************************/
	/** EXTRACT ALL PAIRS R(X,Y) FOR A GIVEN R **/
	/****************************************************************************/
	public static final void extractPairs(Relation r, KB kb, String prefixTarget, String fileWithPairs)
			throws Exception {

		System.out.println("Extract pairs for " + r);

		String querystr = "  PREFIX owl: <http://www.w3.org/2002/07/owl#> \n"
				+ "select distinct ?x2 ?y2 where {graph <" + kb.name + "> {\n";
		querystr += (r.isDirect) ? "?x  <" + r.uri + "> ?y. \n" : "?y <" + r.uri + "> ?x. \n";
		querystr += "?x  owl:sameAs ?x2. \n";
		querystr += "FILTER ( strstarts(str(?x2), \"" + prefixTarget + "\") ).\n";
		querystr += "?y  owl:sameAs ?y2. \n";
		querystr += "FILTER ( strstarts(str(?y2), \"" + prefixTarget + "\") ).\n";
		querystr += "   } } ";

		// System.out.println(querystr);

		BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(fileWithPairs), "UTF-8"));

		QueryEngineHTTP query = new QueryEngineHTTP(kb.endpoint, querystr);

		ResultSet rst = query.execSelect();
		if (rst != null) {
			while (rst.hasNext()) {
				QuerySolution qs = rst.next();
				RDFNode x2 = (RDFNode) qs.get("?x2");
				RDFNode y2 = (RDFNode) qs.get("?y2");
				// System.out.println(x2);
				writer.write(x2 + "\t" + y2 + " \n");
			}
			writer.close();
			System.out.println("				.... finished extracting pairs for " + r);
		}else{
			System.err.println("				.... No results for " + r);
		}
	}
	
	/**************************************************************************************************************/
	/** FOR THE PAIRS OF A RELATION,  FIND OVERLAPPING RELATIONS IN THE TARGET **/
	/**************************************************************************************************************/
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
				 +"select ?r ?d  (COUNT(*) as ?n)   where {graph <" + target.name + "> {\n";
		 querystr+=" values (?x ?y) {\n";
		 		for(String line:lines){
		 				line=line.trim();
		 				String[] parts=line.split(separatorSpace);
		 				querystr+="\t (  "+"<"+parts[0]+">  "+"  <"+parts[1]+">"+" ) \n";
			}
			querystr+="\t}\n";
			querystr+= getSubQueryForDirection(true);
			querystr+=" UNION \n";
			querystr+= getSubQueryForDirection(false);
			querystr+="}} group by ?r ?d  ";
			
			//System.out.println("Relations with overlapp "+querystr);
			
			QueryEngineHTTP query = new QueryEngineHTTP(target.endpoint, querystr);
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
	
	
	/********************************/
	/** PCA Denominator **/
	/*******************************/
	/****   V1 ***/
	public static final HashMap<Relation,  Alignment>  iterateAndComputePCADenominator_V1(int tuplesPerQuery, String fileWithPairs,   KB target, String prefixAtSource, HashMap<Relation,  Alignment> relationsAtOther) throws Exception{
		IteratorFromFile it= new IteratorFromFile();
		ArrayList<String> lines=null;
		
		/** process the file again in order to extract the PCA denominator **/
		it.init(fileWithPairs);
		int totalPairs=0;
		while((lines=it.getNextLines(tuplesPerQuery))!=null){	
			System.out.print("-");
			totalPairs+=lines.size();
			/** PCA direct**/
			HashMap<Relation,  Integer> denominator=getPCADenominatorForGroupOfPairs_V1(lines, target, prefixAtSource, false);
			for(Relation r:denominator.keySet()){
				Alignment struct=relationsAtOther.get(r);
				if(struct==null) continue;
				struct.pcaDenominatorSourceToTarget+=denominator.get(r);
			}
			
			/** PCA direct with counterpart **/
		   HashMap<Relation,  Integer> denominatorWithCounterPart=getPCADenominatorForGroupOfPairs_V1(lines, target, prefixAtSource, true);
			for(Relation r:denominatorWithCounterPart.keySet()){
				Alignment struct=relationsAtOther.get(r);
				if(struct==null) continue;
				struct.pcaDenominatorSourceToTargetWithCounterPartForObject+=denominatorWithCounterPart.get(r);
			}
		}
		it.close();
		
		System.out.println(" ");
		return  relationsAtOther;
	}
	
	public static final  HashMap<Relation,  Integer>  getPCADenominatorForGroupOfPairs_V1(ArrayList<String> lines, KB target, String prefixAtSource, boolean checkCounterpartForExistentialObject) throws Exception{
		 HashMap<Relation,  Integer> denominator=new HashMap<Relation,  Integer> ();
		 String querystr= " PREFIX owl: <http://www.w3.org/2002/07/owl#> \n"
								+  "select ?r ?d  (COUNT(?x) as ?n)   where {graph <" + target.name + "> {\n";
			querystr+=" values ?x {\n";
						for(String line:lines){
									line=line.trim();
									String[] parts=line.split(separatorSpace);
									querystr+="\t   "+"<"+parts[0]+">  \n";
								}
			querystr+="\t}\n";
			
			querystr+=(checkCounterpartForExistentialObject) ?  getExistentialSubQueryForDirectionWithCheckOfCounterpartForObject_V1(true, target, prefixAtSource): getExistentialSubQueryForDirection_V1(true, target);
			querystr+=" UNION \n";
			querystr+=(checkCounterpartForExistentialObject) ? getExistentialSubQueryForDirectionWithCheckOfCounterpartForObject_V1(false, target, prefixAtSource): getExistentialSubQueryForDirection_V1(false, target);
			querystr+="}} group by ?r ?d  ";
			//if (checkCounterpartForExistentialObject) System.out.println("Q Denominator: "+querystr);
			QueryEngineHTTP query = new QueryEngineHTTP(target.endpoint, querystr);
			ResultSet rst = query.execSelect();
			if (rst != null) {
				while (rst.hasNext()) {
					QuerySolution qs = rst.next();
					String n= qs.get("?n").asLiteral().getString().trim();
					int XYs=Integer.valueOf(n);
					if(XYs==0) continue;
					
					RDFNode r = (RDFNode) qs.get("?r");
					String d = qs.get("?d").asLiteral().getString().trim();
					boolean dir=(d.equals("direct"))?true:false;
					Relation rel=new Relation(r.toString(), dir);
					denominator.put(rel, new Integer(XYs));
				}
			}
	    System.out.print("*");
		return denominator;
	}
	
	
	public static final String getExistentialSubQueryForDirection_V1(boolean direction, KB target){
						String query=" ";
						query+=" { select distinct ?x ?r ?d where { ";
						query+=" values  ?d { ";
						query+=(direction)?" \"direct\" ":" \"indirect\" ";
						query+= " } ";
						query+= ((direction)?" { ?x ?r ?y } ":" { ?y ?r ?x } ");
						query+=" }} \n";
		return query;		
	}
	
	
	public static final String getExistentialSubQueryForDirectionWithCheckOfCounterpartForObject_V1(boolean direction, KB target, String prefixAtOther){
		String query=" ";
		query+=" { select distinct ?x ?r ?d where { ";
		query+=" values  ?d { ";
		query+=(direction)?" \"direct\" ":" \"indirect\" ";
		query+= " } ";
		query+= ((direction)?" { ?x ?r ?y } ":" { ?y ?r ?x } ");
		query+=" ?y owl:sameAs ?y2. ";
		//query += " FILTER (strstarts(str(?y2), \"" + prefixAtOther + "\") ). ";
		query+=" }} \n";
		return query;		
	}


	/****   V2 ***/
	public static final  void  iterateAndComputePCADenominator_V2(int tuplesPerQuery, String fileWithPairs,   KB target, String prefixAtSource, HashMap<Relation,  Alignment> relationsAtOther) throws Exception{
		IteratorFromFile it= new IteratorFromFile();
		ArrayList<String> lines=null;
		
	
		/** split the relations at target into direct and inverse **/
		ArrayList<Relation> direct=null;
		for(Relation r: relationsAtOther.keySet()){
			if(r.isDirect==true){
				if(direct==null) direct= new ArrayList<Relation>();
				direct.add(r);
			}
		}
 		
		ArrayList<Relation> inverse=null;
		for(Relation r: relationsAtOther.keySet()){
			if(r.isDirect==false){
				if(inverse==null) inverse= new ArrayList<Relation>();
				inverse.add(r);
			}
		}
	

		/** process the file again in order to extract the PCA denominator **/
		int totalPairs=0;
		it.init(fileWithPairs);
		while((lines=it.getNextLines(tuplesPerQuery))!=null){	
			System.out.print("-");
			totalPairs+=lines.size();
			
			/** PCA direct
			//System.out.println(" PCA Direct ");
			HashMap<Relation,  Integer> denominator=getPCADenominatorForGroupOfPairs_V2(lines, direct, inverse, target, prefixAtSource, false);
			for(Relation r:denominator.keySet()){
				Alignment struct=relationsAtOther.get(r);
				if(struct==null) continue;
				struct.pcaDenominatorSourceToTarget+=denominator.get(r);
			}**/
			
			/** PCA direct with counterpart **/
			//System.out.println(" PCA Direct Counterpart");
		   HashMap<Relation,  Integer> denominatorWithCounterPart=getPCADenominatorForGroupOfPairs_V2(lines, direct, inverse, target, prefixAtSource, true);
			for(Relation r:denominatorWithCounterPart.keySet()){
				Alignment struct=relationsAtOther.get(r);
				if(struct==null) continue;
				struct.pcaDenominatorSourceToTargetWithCounterPartForObject+=denominatorWithCounterPart.get(r);
			}
		}
		it.close();
		
		/** set the total number of pairs translated to the target **/
		for(Relation rel:relationsAtOther.keySet()){
			Alignment struct=relationsAtOther.get(rel);
			struct.originalSamples=totalPairs;
		}		
		
		System.out.println(" ");
		
	}
	
	public static final  HashMap<Relation,  Integer>  getPCADenominatorForGroupOfPairs_V2(ArrayList<String> lines, Collection<Relation> relationsDirect, Collection<Relation> relationsInv, KB target, String prefixAtSource, boolean checkCounterpartForExistentialObject) throws Exception{
		 HashMap<Relation,  Integer> denominator=new HashMap<Relation,  Integer> ();
		 String querystr= " PREFIX owl: <http://www.w3.org/2002/07/owl#> \n"
								+  "select ?r ?d  (COUNT(?x) as ?n)   where {graph <" + target.name + "> {\n";
			querystr+=" values ?x {\n";
						for(String line:lines){
									line=line.trim();
									String[] parts=line.split(separatorSpace);
									querystr+="\t   "+"<"+parts[0]+">  \n";
								}
			querystr+="\t}\n";
			
			if(relationsDirect!=null) querystr+=(checkCounterpartForExistentialObject) ?  getExistentialSubQueryForDirectionWithCheckOfCounterpartForObject_V2(true, target, relationsDirect,prefixAtSource): getExistentialSubQueryForDirection_V2(true, target, relationsDirect);
			if(relationsDirect!=null && relationsInv!=null) querystr+=" UNION \n";
			if(relationsInv!=null) querystr+=(checkCounterpartForExistentialObject) ? getExistentialSubQueryForDirectionWithCheckOfCounterpartForObject_V2(false, target, relationsInv, prefixAtSource): getExistentialSubQueryForDirection_V2(false, target, relationsInv);
			querystr+="}} group by ?r ?d  ";
			
			
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
					String d = qs.get("?d").asLiteral().getString().trim();
					boolean dir=(d.equals("direct"))?true:false;
					Relation rel=new Relation(r.toString(), dir);
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
	
	public static final String getExistentialSubQueryForDirection_V2(boolean direction, KB target, Collection<Relation> relations){
		String query=" ";
		query+=" { select distinct ?x ?r ?d where { ";
		query+=" values  ?d { ";
		query+=(direction)?" \"direct\" ":" \"indirect\" ";
		query+= " } ";
		
		query+=" values  ?r { ";
		for(Relation r: relations){
			if(r.isDirect==direction) 	query+="  <"+r.uri+"> ";
		}
		query+= " } ";
		
		query+= ((direction)?" { ?x ?r ?y } ":" { ?y ?r ?x } ");
		query+=" }} \n";
        return query;		
	}
	
	public static final String getExistentialSubQueryForDirectionWithCheckOfCounterpartForObject_V2(boolean direction, KB target, Collection<Relation> relations, String domain){
		String query=" ";
		query+=" { select distinct ?x ?r ?d where { ";
		query+=" values  ?d { ";
		query+=(direction)?" \"direct\" ":" \"indirect\" ";
		query+= " } ";
		
		query+=" values  ?r { ";
		for(Relation r: relations){
			if(r.isDirect==direction) 	query+="  <"+r.uri+"> ";
		}
		query+= " } ";
		
		query+= ((direction)?" { ?x ?r ?y } ":" { ?y ?r ?x } ");
		query+=" ?y owl:sameAs ?y2. ";
		query += " FILTER (strstarts(str(?y2), \"" + domain + "\") ). ";
		query+=" }} \n";
		return query;		
	}

	
	
	/*************************************/
	/** test**/
	/**************************************/
	public static final void test(KB kb){
		
		String queryForGettingGoodXs=" PREFIX owl: <http://www.w3.org/2002/07/owl#> "
											+" select distinct  ?y2 where {graph <yago> {"
											+" <http://yago-knowledge.org/resource/Abu_Dhabi> owl:sameAs ?y2. "
											+ "}}";
		

		QueryEngineHTTP query = new QueryEngineHTTP(kb.endpoint, queryForGettingGoodXs);
		ResultSet rst = query.execSelect();
		if (rst != null) {
			System.out.println(" Iterator ");
			while (rst.hasNext()) {
				
				QuerySolution qs = rst.next();
				
				
				RDFNode y2= (RDFNode) qs.get("?y2");
				System.out.println(" "+y2);
			}}
		else {
			System.out.println("No results ");
		}
		
		
	}
	
	
	
	
	public static final void align(String dir, String tmpDir, KB S, KB T) throws Exception{
		String fileWithRelations = dir + S.name + "/" + S.name + "_functionality_ee.txt";
		ArrayList<Relation> relations= new ArrayList<Relation>();
		loadRelationsFromFilesWithFunctionality(relations, fileWithRelations, true, 0, 4, 5, 3);

		String fileWithPairs = tmpDir + "pairs.txt";
		String fileWithAlignements =  tmpDir +S.name+"_"+T.name+"_align_2.txt";
		int tuplesPerQuery = 500; // changed!!!
		
		BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(fileWithAlignements), "UTF-8"));
	
		String header="source target sharedXY originalXY pcaDenRelDirectCheckCounterpartForObject ";
		System.out.println(header);
		for (Relation r : relations) {
			System.out.println(r);
			extractPairs(r, S, T.resourcesDomain,  fileWithPairs);
			HashMap<Relation,  Alignment> relationsAtOther=iterateAndComputeSharedPairs(2*tuplesPerQuery, fileWithPairs,  T);
			
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
	
	
	/*************************************************************************************************************/
	/**  SPEED-UP PROCESSING BY GETTING PARTIAL RESULTS FOR S TO T FROM T TO S **/
	/**************************************************************************************************************/
	public static final void alignByCompletingPartialResults(String dir, String tmpDir, KB S, KB T) throws Exception{
		String fileWithPairs = tmpDir + "pairs.txt";
		String fileWithAlignements =  tmpDir +S.name+"_"+T.name+"_align_2.txt";
		int tuplesPerQuery = 500; // changed!!!
		
		
		/** read the relations for the target, in the correct direction according to the functionality **/
		String fileWithRelations = dir + S.name + "/" + S.name + "_functionality_ee.txt";
		ArrayList<Relation> sortedRelations= new ArrayList<Relation>();
		loadRelationsFromFilesWithFunctionality(sortedRelations, fileWithRelations, true, 0, 4, 5, 3);
		//Collections.sort(sortedRelations, new Relation.RelationCompBasedOnTupleNo());
		
		
		/** get the target to source results from the file **/
		HashSet<Relation> relSetAtS=new HashSet<Relation>();
		relSetAtS.addAll(sortedRelations);
		HashMap<Relation, HashMap<Relation, Alignment>>   StoT_partialResults= read_TtoS_results(dir, S, relSetAtS, T);
		
			
		BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(fileWithAlignements), "UTF-8"));
	
		String header="source target sharedXY originalXY pcaDenRelDirectCheckCounterpartForObject ";
		System.out.println(header);
		
		for (Relation rS : sortedRelations) {
			if(! StoT_partialResults.containsKey(rS)) continue;
			HashMap<Relation, Alignment> relationsAtOther=StoT_partialResults.get(rS);
			if(relationsAtOther.keySet().isEmpty()) continue;
			 
			System.out.println(rS);
			extractPairs(rS, S, T.resourcesDomain,  fileWithPairs);
			
			//System.exit(0);
			iterateAndComputePCADenominator_V2(tuplesPerQuery, fileWithPairs, T, S.resourcesDomain, relationsAtOther);
			
			boolean hasSolutions=false;
			for(Relation rO:relationsAtOther.keySet()){
				Alignment struct=relationsAtOther.get(rO);
				String line=rS+" "+rO+" "+struct.sharedXY+" "+struct.originalSamples+" "+struct.pcaDenominatorSourceToTargetWithCounterPartForObject;
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
	
	

	
	public static final HashMap<Relation, HashMap<Relation, Alignment>>  read_TtoS_results(String dir, KB S, HashSet<Relation> relSetAtS,  KB T) throws Exception{
		//read the relations for the target, in the correct direction according to the functionality
		String fileWithResults = dir +"_gold/" +T.name+ "->" + S.name +"/" + T.name+"_"+S.name + "_align.txt";
		
		File f = new File(fileWithResults);
		if (!f.exists()) {
			System.err.println("  File does not exit " + fileWithResults);
			System.exit(0);
		} 

		HashMap<Relation, HashMap<Relation, Alignment>>  StoT_partialResults= new HashMap<Relation, HashMap<Relation, Alignment>> ();
		
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
					   System.err.println("Big problem: I couldn't find rel "+rS);
					   System.exit(0);
				   }
			}
		 
			Alignment a=new Alignment(0);  //the number of original samples is unknown 
			a.sharedXY=shared;
			
			/** insert the alignment in the partial results**/
			HashMap<Relation, Alignment> alignmentsFor_rS=StoT_partialResults.get(rS);
			if(alignmentsFor_rS==null) {
				alignmentsFor_rS= new HashMap<Relation, Alignment> ();
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
	public static class  Alignment{
		public int sharedXY=0;
		public int originalSamples=0;
		public int pcaDenominatorSourceToTarget=0;
		public int pcaDenominatorSourceToTargetWithCounterPartForObject=0;
		
		public Alignment(int originalSamples){
			this.originalSamples=originalSamples;
		}
	}
	
	public static void main(String[] args) throws Exception {	
	
		String dir = "/Users/adi/Dropbox/DBP/feb-sofya/"; //"feb-sofya/";
		String tmpDir ="/Users/adi/Dropbox/DBP/"; // "tmpDir/";  

		KB yago = new KB("yago", "http://s6.adam.uvsq.fr:8892/sparql", "http://yago-knowledge.org");
		KB dbpedia = new KB("dbpedia", "http://s6.adam.uvsq.fr:8892/sparql", "http://dbpedia.org");
		KB freebase = new KB("freebase", "http://s6.adam.uvsq.fr:8892/sparql", "http://rdf.freebase.com");
		
		KB S=freebase;
		KB T=yago;
		//align(dir, tmpDir, S, T);
		
		alignByCompletingPartialResults(dir, tmpDir, S, T);
		
		//test(target);
		//System.exit(0);
	}
	
	
	public static final HashMap<Relation,  Integer> testShared(KB target, ArrayList<String> lines, String domainAtSource) throws Exception{
		 HashMap<Relation,  Integer> overlapp=new HashMap<Relation,  Integer> ();
		 
		 String querystr=" PREFIX owl: <http://www.w3.org/2002/07/owl#> \n"
				 +"select ?r ?d  ?x ?y  where {graph <" + target.name + "> {\n";
		 querystr+=" values (?x ?y) {\n";
		 		for(String line:lines){
		 				line=line.trim();
		 				String[] parts=line.split(separatorSpace);
		 				querystr+="\t (  "+"<"+parts[0]+">  "+"  <"+parts[1]+">"+" ) \n";
			}
			querystr+="\t}\n";
			querystr+= getSubQueryForDirection(true);
			querystr+=" UNION \n";
			querystr+= getSubQueryForDirection(false);
			querystr+="}}  order by ?r, ?x, ?y ";
			
			//System.out.println("Relations with overlapp "+querystr);
			
			QueryEngineHTTP query = new QueryEngineHTTP(target.endpoint, querystr);
			ResultSet rst = query.execSelect();
			if (rst != null) {
				while (rst.hasNext()) {
					QuerySolution qs = rst.next();
					
					RDFNode x= (RDFNode) qs.get("?x");
					RDFNode y= (RDFNode) qs.get("?y");
					RDFNode r = (RDFNode) qs.get("?r");
					String d = qs.get("?d").asLiteral().getString().trim();
					System.out.println(x+" "+y+" "+r);
				}
			}else{
				
			}	
		 return overlapp;
	}
}
