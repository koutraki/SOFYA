package compare;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import config.Alignment;
import gold.KB;

public class CompareWithParis {
	
	public static HashSet<Alignment>  loadParis(String pathToFile) throws NumberFormatException, IOException{
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
				
				String rS=e[0].trim(); 
				String rT=e[1].trim();
				
				Integer sharedXY=Integer.valueOf(e[2].trim());
				Alignment align=new Alignment(rS, rT, sharedXY, 0, 0);
				
				if(allAlignements.contains(align)) {
					System.out.println(" Alignment : "+line+" is a double ");
				}
				else  allAlignements.add(align);
				
		}
		alignReader.close();
		return allAlignements;
	}
	
	public static final HashSet<Alignment> filterAlignmentsBasedOnSource(String sourcePrefix, String targetPrefix,  HashSet<Alignment> allAlignments){
		HashSet<Alignment> newAlignments= new HashSet<Alignment>();
		
		for(Alignment a: allAlignments){
			if(a.rS.startsWith(sourcePrefix))  newAlignments.add(a);
			else{
				if(a.rT.startsWith(targetPrefix))  newAlignments.add(a);
			}
		}
		return newAlignments;
	}
	
	public static HashSet<Alignment>  loadNewManualGoldSet(String pathToFile, boolean skipFirstLine) throws NumberFormatException, IOException{
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
				
				String rS=e[0].trim(); 
				String rT=e[1].trim();
				
				Integer sharedXY=Integer.valueOf(e[2].trim());
				Integer originalSamples=Integer.valueOf(e[3].trim());
				Integer PCA_denominator=Integer.valueOf(e[3].trim());
				Alignment align=new Alignment(rS, rT, sharedXY, originalSamples, PCA_denominator);
				
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
	
	
	public static void main(String[] args) throws Exception {
		KB yago = new KB("yago", null, "http://yago-knowledge.org/resource/");
		KB dbpedia = new KB("dbpedia",null, "http://dbpedia.org/ontology/");
		KB freebase = new KB("freebase",null, "http://rdf.freebase.com/ns/");
		
		 KB source=yago;
		 KB target=dbpedia;
		 String user="nico";
		 
		String fileWithParisAlignments="/Users/adi/Dropbox/DBP/feb-sofya/_gold/_paris_alignment/paris_gold_simple.txt";
		HashSet<Alignment>  allAlignments=loadParis(fileWithParisAlignments);
		HashSet<Alignment>  alignmentsForSource=filterAlignmentsBasedOnSource(source.name+":", target.name+":", allAlignments);
		 
		String dirWithNewManualGoldSet="/Users/adi/Dropbox/DBP/feb-sofya/_gold/";
		String fileWithNewManualAlignments=  dirWithNewManualGoldSet+"_"+user+"/"+user+"_"+source.name+"_"+target.name+"_pca_cwa.txt";
		HashSet<Alignment>  newManualAlignments=loadNewManualGoldSet(fileWithNewManualAlignments, true);
		 
	 
		 ArrayList<Alignment> list= new ArrayList<Alignment>();
		 list.addAll(alignmentsForSource);
		 Collections.sort(list, new Alignment.Comp_Alignment_Shared_Based());
		 
		for(Alignment a: list){
			System.out.println(a.toStringWithShared());
		}
		
	}
	
	
	
}
