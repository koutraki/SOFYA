package compare;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

import org.omg.Messaging.SYNC_WITH_TRANSPORT;

import compare.CompareWithParis.AlignmentWithComments;
import config.Alignment;
import gold.GetOvelappingRels;
import gold.KB;
import gold.Relation;
import preprocessing.ComputingIntraCWA;
import preprocessing.LoadEquivalentIntraRelations;
import preprocessing.ComputingIntraCWA.AlignmentCWA;
import preprocessing.ComputingPCAandCWA.AlignmentPCA_CWA;
import preprocessing.ComputingPCAandCWA.Comp_AlignmentPCA_CWA;
import preprocessing.LoadEquivalentIntraRelations.EquivPair;

public class LoadManualGolsets {
	/***************************************************************************************/
	/** LOAD **/
	/***************************************************************************************/
	public static final void abortIfFileDoesNotExist(String pathToFile){
		File f=new File(pathToFile);
		if(!f.exists()) {
			System.err.println("  File does not exit "+pathToFile);
			System.exit(0);
		}else {
			/** System.out.println(" Load : "+file);**/
		}
	}

	/** the key is the source relation **/
	public static final HashMap<Relation, ArrayList<AlignmentWithComments>>  loadManualGoldSet(String pathToFile, boolean skipFirstLine, KB source, KB target) throws Exception{
	
		BufferedReader alignReader = new BufferedReader(new InputStreamReader(new FileInputStream(pathToFile),"UTF8"));
		HashMap<Relation, ArrayList<AlignmentWithComments>>   allAlignements=new HashMap<Relation, ArrayList<AlignmentWithComments>>  ();
		
		if(skipFirstLine) alignReader.readLine();
		
		String line=null;
		while ((line = alignReader.readLine()) != null) {
				line=line.trim();
			    if(line.isEmpty()){ 
					continue;
				}
				
				//System.out.println(line);
				String[] e = line.split("\\s+");
			
				Integer sharedXY=Integer.valueOf(e[2].trim());
				Integer originalSamples=Integer.valueOf(e[3].trim());
				Integer PCA_denominator=Integer.valueOf(e[4].trim());
				
				Relation relS=getRelationFromString(e[0].trim(), source);
				relS.tupleNo=originalSamples;
				
				Relation relT=getRelationFromString(e[1].trim(), target);
								
				boolean isUndecided=line.contains("?");
				String comment=(line.contains("?") && (line.split("\\?").length>1))?line.split("\\?")[1].trim():" ";
						
				AlignmentWithComments align=new AlignmentWithComments(relS, relT, sharedXY, originalSamples, PCA_denominator, isUndecided, comment);
				
				ArrayList<AlignmentWithComments> list=allAlignements.get(relS);
				if(list==null){
					list= new ArrayList<AlignmentWithComments>();
					allAlignements.put(relS, list);
				}
				list.add(align);
					
		}
		alignReader.close();
		return allAlignements;
	}
	
	public static final Relation getRelationFromString(String r, KB kb) throws Exception{
		if(kb!=null && r.startsWith(kb.name+":")) r=r.replaceFirst(kb.name+":", kb.resourcesDomain);
		return (r.endsWith("-"))?new Relation(r.substring(0, r.length()-1),false):new Relation(r, true); 
	}

	/***************************************************************************************/
	/** SWAP KEYS:  **/
	/***************************************************************************************/	
	/** do not use the ancient map, better re-swap **/
   public static final HashMap<Relation, ArrayList<AlignmentWithComments>>   swapFromSourceToTargetKey(HashMap<Relation, ArrayList<AlignmentWithComments>>  allAlignments){
	   
	   HashMap<Relation, ArrayList<AlignmentWithComments>>  newMap=new HashMap<Relation, ArrayList<AlignmentWithComments>> ();
	   
	   Iterator<ArrayList<AlignmentWithComments>>  it=allAlignments.values().iterator();
	   while(it.hasNext()){
		   Iterator<AlignmentWithComments> listIt=it.next().iterator();
		   while(listIt.hasNext()){
			   AlignmentWithComments a=listIt.next();
			   ArrayList<AlignmentWithComments> newList=newMap.get(a.rT);
			   if(newList==null) {
				   newList=new ArrayList<AlignmentWithComments> ();
				   newMap.put(a.rT, newList);
			   }
			   newList.add(a);
			   listIt.remove();
		   }
		   
		   it.remove();
		   
	   }
	   return newMap;
   }

   
	/** do not use the ancient map, better re-swap  **/
   public static final HashMap<Relation, ArrayList<AlignmentWithComments>>   swapFromTargetToSourceKey(HashMap<Relation, ArrayList<AlignmentWithComments>>  allAlignments){
	   
	   HashMap<Relation, ArrayList<AlignmentWithComments>>  newMap=new HashMap<Relation, ArrayList<AlignmentWithComments>> ();
	   
	   Iterator<ArrayList<AlignmentWithComments>>  it=allAlignments.values().iterator();
	   while(it.hasNext()){
		   Iterator<AlignmentWithComments> listIt=it.next().iterator();
		   while(listIt.hasNext()){
			   AlignmentWithComments a=listIt.next();
			   ArrayList<AlignmentWithComments> newList=newMap.get(a.rS);
			   if(newList==null) {
				   newList=new ArrayList<AlignmentWithComments> ();
				   newMap.put(a.rS, newList);
			   }
			   newList.add(a);
			   listIt.remove();
		   }
		   
		   it.remove();
		   
	   }
	   return newMap;
   }

   
	
	/***************************************************************************************/
	/** MERGE ALIGNMENTS **/
	/***************************************************************************************/	
	public static final void mergeEquivRelations (String rootDir, KB source, KB target, HashMap<Relation, ArrayList<AlignmentWithComments>>  allAlignments, boolean keyIsSourceRel) throws Exception{
		KB processedKB=(keyIsSourceRel)?source:target;
	
		String fileWithQuivalences=rootDir+processedKB.name+"/"+processedKB.name+"_"+processedKB.name+"_equi_ee.txt";
		boolean exist=LoadEquivalentIntraRelations.doesFileExist(fileWithQuivalences);
		if(!exist) return;
		
		//the relations to keep correspond to the left part of the alignment, the relations to eliminate correspond to the right part of the alignment
		ArrayList<EquivPair> equiPairs=LoadEquivalentIntraRelations.loadEquivalentRelations(fileWithQuivalences, true);
		HashSet<String> leftHand=new HashSet<String>();
		HashSet<String> rightHand=new HashSet<String>();
		LoadEquivalentIntraRelations.getLeftRightSet(leftHand, rightHand, equiPairs);
		
		
		ArrayList<Relation> relToEliminate=new ArrayList<Relation>();
		for(Relation r: allAlignments.keySet()){
			//the relations on the left side are not eliminated 		
			if(leftHand.contains(r.uri)) continue;

			//if the relation has no equivalent relations, nothing to do
			if(!rightHand.contains(r.uri)) continue;
			
			//find the equivalent relations of this relation
			for(EquivPair p: equiPairs){
				if(p.rightHand.uri.equals(r.uri)){
					
					
					Relation pLeftHandInv=new Relation(p.leftHand.uri, !p.leftHand.isDirect);
					pLeftHandInv.tupleNo=p.leftHand.tupleNo;
				
					System.out.println(" Found equiv relation for "+p.rightHand.toStringShort(processedKB)+"   =>  "+p.leftHand.toStringShort(processedKB)+"    " +p.rightHand.tupleNo );
					
					
					if(!(allAlignments.containsKey(p.leftHand) || allAlignments.containsKey(pLeftHandInv))) {
						System.out.println(" ....UPS, the requivalent of  "+r.toStringShort(processedKB)+" is absent ");
						continue;
					}
					
					
					boolean dirMarches=(allAlignments.containsKey(p.leftHand) && p.rightHand.isDirect==r.isDirect  ||  allAlignments.containsKey(pLeftHandInv) && r.isDirect!=p.rightHand.isDirect)?true:false;
					if(!dirMarches) {
						System.out.println("  .....DIRECTION DOES NOT MATCH:  is changed    ");
					}
					
					Relation actualRelationToEnrich=(allAlignments.containsKey(p.leftHand))?p.leftHand:pLeftHandInv;
					System.out.println("  Replace   "+r+"    by "+actualRelationToEnrich);
					ArrayList<AlignmentWithComments> listToEnrich=allAlignments.get(actualRelationToEnrich);
					
					relToEliminate.add(r);
	
					ArrayList<AlignmentWithComments> rToAppend=allAlignments.get(r);
					for(AlignmentWithComments a: rToAppend){
							AlignmentWithComments aNew=new AlignmentWithComments((keyIsSourceRel)?actualRelationToEnrich:((dirMarches)?a.rS:new Relation(a.rS.uri, ! a.rS.isDirect)), (keyIsSourceRel)?((dirMarches)?a.rT:new Relation(a.rT.uri, ! a.rT.isDirect)):actualRelationToEnrich, a.sharedXY, a.originalSamples, a.pcaDenominator, a.isUndecided, a.comment);
							if(!dirMarches) {
								System.out.println("  ....ORIGINAL alignment "+a.toStringAll(source, target));		
								System.out.println("  ....REPLACED BY  "+aNew.toStringAll(source, target));		
									
								for(AlignmentWithComments ae: listToEnrich  ){
									System.out.println("  ....EXISTS   "+ae.toStringAll(source, target));		
								}
							}
							
							if(! listToEnrich.contains(aNew))  listToEnrich.add(aNew);		
					}	
				}
			}	
		}
			
		int no=0;
		Iterator<Relation> itE =allAlignments.keySet().iterator();
		while(itE.hasNext()){
			Relation r=itE.next();
			if(relToEliminate.contains(r)){ itE.remove();
															System.out.println("I eliminate "+r);
															no++;
			}
			
		}
		
		System.out.println("No of elim rel "+no);
		
	}
	
	
	/***************************************************************************************/
	/** ELIMINATE RELATIONS **/
	/***************************************************************************************/	
	public static final void filterSkipRelationsAtTarget(HashMap<Relation, ArrayList<AlignmentWithComments>> allAlignments, HashSet<String> skipRelTarget){
		if(skipRelTarget.size()==0) return;
		for(ArrayList<AlignmentWithComments> list: allAlignments.values()){
			Iterator<AlignmentWithComments> it=list.iterator();
			while(it.hasNext()){
				AlignmentWithComments a=it.next();
				String rT=a.rT.uri.endsWith("-")?a.rT.uri.substring(0, a.rT.uri.length()-1):a.rT.uri;
				if(skipRelTarget.contains(rT)) it.remove();
			}
		}
	}
	
	public static final void filterSkipRelationsAtSource(HashMap<Relation, ArrayList<AlignmentWithComments>> allAlignments, HashSet<String> skipRelSource){
		if(skipRelSource.size()==0) return;
		Iterator<Relation> it=allAlignments.keySet().iterator();
		while(it.hasNext()){
			Relation r=it.next();
			if(skipRelSource.contains(r.uri)) it.remove();
		}
	}
	
	
	/***************************************************************************************/
	/** PRINT **/
	/***************************************************************************************/	
	public static final void printResults(String pathToNewFile, HashMap<Relation, ArrayList<AlignmentWithComments>> allAlignements, KB source, KB target) throws Exception{
		BufferedWriter pca_file=new BufferedWriter(new FileWriter(pathToNewFile));
		pca_file.write("subRelation \t superRelation \t shared \t Den_Cwa \t Den_Pca \t PCA \t CWA \n");		
		
		int no=0;
		ArrayList<Relation> relationsAtSource=new ArrayList<Relation>();
		relationsAtSource.addAll(allAlignements.keySet());
		Collections.sort(relationsAtSource, new Relation.RelationCompBasedOnTupleNoDesc());
		
		for(Relation rS: relationsAtSource){
			ArrayList<AlignmentWithComments> list=allAlignements.get(rS);
			Collections.sort(list, new CompareWithParis.Comp_Alignment_Comp_Shared_Based() );
			for(AlignmentWithComments align: list){
					pca_file.write(align.toStringAll(source, target)+"\n");
					pca_file.flush();
					no++;
			}
			pca_file.write("\n");
		}
		pca_file.close();
		
		System.out.println("All alignments : "+no);
	}
	
	/***************************************************************************************/
	/** MAIN **/
	/***************************************************************************************/		
	public static void reorderRelations(KB source, KB target, String rootDir) throws Exception{
		
		String pathToAlignement=rootDir+"_gold/_nico/nico_"+source.name+"_"+target.name+"_pca_cwa.txt";
		HashMap<Relation, ArrayList<AlignmentWithComments>>  allAlignments=loadManualGoldSet(pathToAlignement, true, source, target);
		mergeEquivRelations(rootDir, source, target, allAlignments, true);
		
		allAlignments=swapFromSourceToTargetKey(allAlignments);
		mergeEquivRelations(rootDir, source, target, allAlignments, false);
		
		allAlignments=swapFromTargetToSourceKey(allAlignments);
		
		String resultFile=rootDir+"_gold/_nico/new_"+source.name+"_"+target.name+"_pca_cwa.txt";
		printResults(resultFile, allAlignments, source, target);
	}

	
	public static void main(String[] args) throws Exception {
		KB yago = new KB("yago", null, "http://yago-knowledge.org/resource/");
		KB dbpedia = new KB("dbpedia",null, "http://dbpedia.org/ontology/");
		KB freebase = new KB("freebase",null, "http://rdf.freebase.com/ns/");
		
		 KB source=freebase;
		 KB target=yago;
		 
		String rootDir="/Users/adi/Dropbox/DBP/feb-sofya/";
		//reorderRelations(freebase, yago, rootDir);
		//reorderRelations(yago, freebase, rootDir);
			
		reorderRelations(dbpedia,freebase, rootDir);
		
	}
	
	
	
}
