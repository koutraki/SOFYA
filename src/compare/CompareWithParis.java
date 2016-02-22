package compare;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import config.Alignment;
import gold.GetOvelappingRels;
import gold.KB;
import gold.Relation;

public class CompareWithParis {
	
	public static Relation getRelationFromString(String r) throws Exception{
		return (r.endsWith("-"))?new Relation(r.substring(0, r.length()-1),false):new Relation(r, true); 
	}
	
	public static HashSet<Alignment>  loadParis(String pathToFile) throws Exception{
		File f=new File(pathToFile);
		if(!f.exists()) {
			System.err.println("  File does not exit "+pathToFile);
			System.exit(0);
		}else {
			/** System.out.println(" Load : "+file);**/
		}
		
		BufferedReader alignReader = new BufferedReader(new InputStreamReader(new FileInputStream(pathToFile),"UTF8"));
		HashSet<Alignment> allAlignements=new HashSet<Alignment>();
		String line=null;
		while ((line = alignReader.readLine()) != null) {
				if(line.isEmpty()){ 
					continue;
				}
				String[] e = line.split("\\s+");
			
				Relation relS=getRelationFromString(e[0].trim());
				Relation relT=getRelationFromString(e[1].trim());
				
				Integer sharedXY=Integer.valueOf(e[2].trim());
				Alignment align=new Alignment(relS, relT, sharedXY, 0, 0);
				
				if(allAlignements.contains(align)) {
					System.out.println(" Alignment : "+line+" is a double ");
				}
				else  allAlignements.add(align);
				
		}
		alignReader.close();
		return allAlignements;
	}
	
	
	
	public static HashSet<Alignment>  loadNewManualGoldSet(String pathToFile, boolean skipFirstLine) throws Exception{
		File f=new File(pathToFile);
		if(!f.exists()) {
			System.err.println("  File does not exit "+pathToFile);
			System.exit(0);
		}else {
			/** System.out.println(" Load : "+file);**/
		}
		
		BufferedReader alignReader = new BufferedReader(new InputStreamReader(new FileInputStream(pathToFile),"UTF8"));
		HashSet<Alignment> allAlignements=new HashSet<Alignment>();
		
		if(skipFirstLine) alignReader.readLine();
		
		String line=null;
		while ((line = alignReader.readLine()) != null) {
				if(line.isEmpty()){ 
					continue;
				}
				String[] e = line.split("\\s+");
				
	
				Integer sharedXY=Integer.valueOf(e[2].trim());
				Integer originalSamples=Integer.valueOf(e[3].trim());
				Integer PCA_denominator=Integer.valueOf(e[3].trim());
				
				Relation relS=getRelationFromString(e[0].trim());
				Relation relT=getRelationFromString(e[1].trim());
						
				Alignment align=new Alignment(relS, relT, sharedXY, originalSamples, PCA_denominator);
				
				if(allAlignements.contains(align)) {
					System.out.println(" Alignment : "+line+" is a double ");
				}
				else  allAlignements.add(align);
				
		}
		alignReader.close();
		return allAlignements;
	}
	
	/***********************************************************************************************/
	/*** GET DIFFERENCE **/
	/***********************************************************************************************/
	public static final HashSet<Alignment>  difference(HashSet<Alignment> op1, HashSet<Alignment> op2){
		HashSet<Alignment>  diff=new HashSet<Alignment> ();
		for(Alignment a: op1){
			if(!op2.contains(a)) diff.add(a);
		}
		return diff;
	}
	
	public static final HashSet<Alignment> filterAlignmentsBasedOnSource(String sourcePrefix, String targetPrefix,  HashSet<Alignment> allAlignments){
		HashSet<Alignment> newAlignments= new HashSet<Alignment>();
		
		for(Alignment a: allAlignments){
			if(a.rS.uri.startsWith(sourcePrefix))  newAlignments.add(a);
			else{
				if(a.rT.uri.startsWith(targetPrefix))  newAlignments.add(a);
			}
		}
		return newAlignments;
	}
	
	public static final HashSet<Alignment> eliminateRelationsAtSource(HashSet<Alignment> allAlignments, Collection<Relation> wantedRelationsAtSource ){
		HashSet<Alignment> newAlignments= new HashSet<Alignment>();
		
		for(Alignment a: allAlignments){
				if(wantedRelationsAtSource.contains(a.rS) ) newAlignments.add(a);
		}
		return newAlignments;
	}
	
	public static void main(String[] args) throws Exception {
		KB yago = new KB("yago", null, "http://yago-knowledge.org/resource/");
		KB dbpedia = new KB("dbpedia",null, "http://dbpedia.org/ontology/");
		KB freebase = new KB("freebase",null, "http://rdf.freebase.com/ns/");
		
		 KB source=dbpedia;
		 KB target=yago;
		 String user="nico";
		 
		 /** get only the EE relations at the source **/
		String fileWithRelations = "/Users/adi/Dropbox/DBP/feb-sofya/" + source.name + "/" + source.name + "_functionality_ee.txt";
		ArrayList<Relation> relationsEEAtSource= new ArrayList<Relation>();
		GetOvelappingRels.loadRelationsFromFilesWithFunctionality(relationsEEAtSource, fileWithRelations, true, 0, 4, 5, 3);
		ArrayList<Relation> shortNamesRelationsEEAtSource=new ArrayList<Relation>();
		for(Relation r: relationsEEAtSource){
			Relation rNew=new Relation(r.uri.replace(source.resourcesDomain, source.name+":"), r.isDirect);
			shortNamesRelationsEEAtSource.add(rNew);
		}
		
		String fileWithParisAlignments="/Users/adi/Dropbox/DBP/feb-sofya/_gold/_paris_alignment/paris_gold_simple.txt";
		HashSet<Alignment>  parisAlignments=loadParis(fileWithParisAlignments);
		HashSet<Alignment>  parisAlignmentsSourceToTarget=filterAlignmentsBasedOnSource(source.name+":", target.name+":", parisAlignments);
		HashSet<Alignment>  parisEEAlignmentsSourceToTarget=eliminateRelationsAtSource(parisAlignmentsSourceToTarget, shortNamesRelationsEEAtSource);
		
		
		
		String dirWithNewManualGoldSet="/Users/adi/Dropbox/DBP/feb-sofya/_gold/";
		String fileWithNewManualAlignments=  dirWithNewManualGoldSet+"_"+user+"/"+user+"_"+source.name+"_"+target.name+"_pca_cwa.txt";
		HashSet<Alignment>  newManualAlignments=loadNewManualGoldSet(fileWithNewManualAlignments, true);
		
		HashSet<Alignment>  onlyParis=difference(parisEEAlignmentsSourceToTarget, newManualAlignments);
		
		 ArrayList<Alignment> onlyParisList= new ArrayList<Alignment>();
		 onlyParisList.addAll(onlyParis);
		 Collections.sort(onlyParisList, new Alignment.Comp_Alignment_Shared_Based());
		 
		 System.out.println("Only paris list: ");
		for(Alignment a: onlyParis){
			System.out.println("        "+a.toStringWithShared());
		}
		
	}
	
	
	
}
