package preprocessing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.jena.atlas.AtlasException;

import gold.KB;
import gold.Relation;


public class ComputingPCAandCWA {
	
	/*
	 * Class that computes pca and cwa form a file of form:
	 * subRelation superRelation NumeratorForBothPCAandCWA DenominatorForCWA DenominatorForPCA
	 * */
	
	public static final DecimalFormat df = new DecimalFormat("0.00");
		
	public static HashMap<Relation, ArrayList<AlignmentPCA_CWA>>  parseAlignmentFile(String pathToFile, double thresholdForFilter) throws Exception{
		
		BufferedReader alignReader = new BufferedReader(new InputStreamReader(new FileInputStream(pathToFile),"UTF8"));
		HashMap<Relation, ArrayList<AlignmentPCA_CWA>> allAlignements=new HashMap<Relation, ArrayList<AlignmentPCA_CWA>> ();
		String line=null;
		while ((line = alignReader.readLine()) != null) {
				if(line.isEmpty()){ 
					continue;
				}
				String[] e = line.split("\\s+");
				
				String rS=e[0].trim(); 
				String rT=e[1].trim(); 
				int sharedXY=Integer.parseInt(e[2]);
				int originalSamples=Integer.parseInt(e[3]);
				int pcaDenominator=Integer.parseInt(e[4]);
				
				AlignmentPCA_CWA align=new AlignmentPCA_CWA(rS, rT, sharedXY, originalSamples, pcaDenominator);
				
				/** filter out alignments that are below a given threshold **/
				if(align.pca<thresholdForFilter && align.cwa<thresholdForFilter) continue;
				
				Relation relS=getRelationFromStringDescr(rS);
				relS.tupleNo=originalSamples;
				
				/** insert the alignment **/
				ArrayList<AlignmentPCA_CWA> list=allAlignements.get(relS);
				if(list==null){
					list=new ArrayList<AlignmentPCA_CWA> ();
					allAlignements.put(relS, list);
				}
				list.add(align);
				
		}
		alignReader.close();
		return allAlignements;
	}
	
	
	public static final Relation getRelationFromStringDescr(String s)throws Exception{
		String uri=(s.endsWith("-"))?s.substring(0,s.length()-1): s;
		Relation r=(s.endsWith("-"))?new Relation(uri, false):new Relation(uri, true);
		return r;	
	}
	
	
	
	public static final void printResults(String pathToNewFile, HashMap<Relation, ArrayList<AlignmentPCA_CWA>> allAlignements, KB source, KB target) throws Exception{
		BufferedWriter pca_file=new BufferedWriter(new FileWriter(pathToNewFile));
		pca_file.write("subRelation \t superRelation \t shared \t Den_Cwa \t Den_Pca \t PCA \t CWA \n");		
		
		ArrayList<Relation> relationsAtSource=new ArrayList<Relation>();
		relationsAtSource.addAll(allAlignements.keySet());
		Collections.sort(relationsAtSource, new Relation.RelationCompBasedOnTupleNoDesc());
		
		for(Relation rS: relationsAtSource){
			ArrayList<AlignmentPCA_CWA> list=allAlignements.get(rS);
			Collections.sort(list, new Comp_AlignmentPCA_CWA());
			for(AlignmentPCA_CWA align: list){
					pca_file.write(align.toStringAllShort(source, target));
					pca_file.flush();
			}
			pca_file.write("\n");
		}
		pca_file.close(); 
	}
	
	public static final void filterSkipRelationsAtSource(HashMap<Relation, ArrayList<AlignmentPCA_CWA>> allAlignments, HashSet<String> skipRelSource){
		if(skipRelSource.size()==0) return;
		Iterator<Relation> it=allAlignments.keySet().iterator();
		while(it.hasNext()){
			Relation r=it.next();
			if(skipRelSource.contains(r.uri)) it.remove();
		}
	}
	
	public static final void filterSkipRelationsAtTarget(HashMap<Relation, ArrayList<AlignmentPCA_CWA>> allAlignments, HashSet<String> skipRelTarget){
		if(skipRelTarget.size()==0) return;
		for(ArrayList<AlignmentPCA_CWA> list: allAlignments.values()){
			Iterator<AlignmentPCA_CWA> it=list.iterator();
			while(it.hasNext()){
				AlignmentPCA_CWA a=it.next();
				String rT=a.rT.endsWith("-")?a.rT.substring(0, a.rT.length()-1):a.rT;
				if(skipRelTarget.contains(rT)) it.remove();
			}
		}
	}
	
    public static final void computePCA_CWA_fromNominatorAndDenominatorValues(String rootDir, String goldDir, String outputDir, String prefix, KB source, KB target ) throws Exception{
    		String alignmentFile = goldDir+source.name+"->"+target.name+"/"+source.name+"_"+target.name+"_align.txt"; 
		String resultFile=outputDir+prefix+"_"+source.name+"_"+target.name+"_pca_cwa.txt"; 
		
		abortIfFileDoesNotExist(alignmentFile);
		HashSet<String> skipRelSource=LoadEquivalentIntraRelations.getSkipRelations(source, rootDir);
		HashSet<String> skipRelTarget=LoadEquivalentIntraRelations.getSkipRelations(target, rootDir);
		
		try {
			HashMap<Relation, ArrayList<AlignmentPCA_CWA>> allAlignments=parseAlignmentFile(alignmentFile, 0.005);
			filterSkipRelationsAtSource(allAlignments, skipRelSource);
			filterSkipRelationsAtTarget(allAlignments, skipRelTarget);
			printResults(resultFile, allAlignments, source, target);
		} catch (Exception e  ) {
			e.printStackTrace();
		}
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

    
	public static void main(String[] args) throws Exception{
		
		KB yago = new KB("yago", null, "http://yago-knowledge.org/resource/");
		KB dbpedia = new KB("dbpedia",null, "http://dbpedia.org/ontology/");
		KB freebase = new KB("freebase",null, "http://rdf.freebase.com/ns/");
		
		String user="nico";
		String rootDir="/Users/adi/Dropbox/DBP/feb-sofya/";
		String goldDir=rootDir+"_gold/";
		String outputDir=goldDir+"_"+user+"/";
		
		
		
		computePCA_CWA_fromNominatorAndDenominatorValues(rootDir, goldDir, outputDir, user,   freebase, dbpedia);
		computePCA_CWA_fromNominatorAndDenominatorValues(rootDir, goldDir, outputDir, user,   freebase, yago);
		computePCA_CWA_fromNominatorAndDenominatorValues(rootDir, goldDir, outputDir, user, dbpedia, freebase);
		computePCA_CWA_fromNominatorAndDenominatorValues(rootDir, goldDir, outputDir, user,  yago, freebase); 
		   
	}

	
	public static class  AlignmentPCA_CWA{
		public final String rS;
		public final String rT;
		
		
		public final int sharedXY;
		public final int originalSamples;
		public final int pcaDenominator;
		
		public final double pca;
		public final double cwa;
	
		
		public AlignmentPCA_CWA(String rS, String rT, int sharedXY, int originalSamples, int pcaDenominator){
			this.sharedXY=sharedXY;
			this.originalSamples=originalSamples;
			this.pcaDenominator=pcaDenominator;
			this.rS=rS;
			this.rT=rT;
			
			this.pca=((double)sharedXY)/pcaDenominator;
			this.cwa=((double)sharedXY)/originalSamples;
			
		}
		
		public final String toStringAll(){
					return  rS + "  " + rT+ "\t"+ sharedXY+ "\t"+ originalSamples+ "\t"+ pcaDenominator+ "\t"+ df.format(pca)+ "\t" +df.format(cwa)+"\n";

		}
		
		public final String toStringAllShort(KB source, KB target){
			return  getShortFormRelation(rS, source) + "  " +  getShortFormRelation(rT, target)+ "\t"+ sharedXY+ "\t"+ originalSamples+ "\t"+ pcaDenominator+ "\t"+ df.format(pca)+ "\t" +df.format(cwa)+"\n";
		}
		
		public static final String getShortFormRelation(String r, KB kb){
			return (r.startsWith(kb.resourcesDomain))? r.replaceFirst(kb.resourcesDomain, kb.name+":"):r;
		}
		
		
		
	}
	
	public static final class Comp_AlignmentPCA_CWA implements Comparator<AlignmentPCA_CWA> {

		@Override
		public int compare(AlignmentPCA_CWA o1, AlignmentPCA_CWA o2) {
			 int diff=-(o1.sharedXY-o2.sharedXY);
			 if(diff!=0) return diff;
			 return -(o1.pcaDenominator-o2.pcaDenominator);
		}

		
		
	}

}
