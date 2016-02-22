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

import gold.KB;


public class ComputingPCAandCWA {
	
	/*
	 * Class that computes pca and cwa form a file of form:
	 * subRelation superRelation NumeratorForBothPCAandCWA DenominatorForCWA DenominatorForPCA
	 * */
	
	public static final DecimalFormat df = new DecimalFormat("0.00");
		
	public static void parseAlignmentFile(String pathToFile, String pathToNewFile, KB source, KB target) throws NumberFormatException, IOException{
		File f=new File(pathToFile);
		if(!f.exists()) {
			System.err.println("  File does not exit "+pathToFile);
			System.exit(0);
		}else {
			/** System.out.println(" Load : "+file);**/
		}
		
		BufferedReader alignReader = new BufferedReader(new InputStreamReader(new FileInputStream(pathToFile),"UTF8"));
		HashMap<String, ArrayList<AlignmentPCA_CWA>> allAlignements=new HashMap<String, ArrayList<AlignmentPCA_CWA>> ();
		String line=null;
		while ((line = alignReader.readLine()) != null) {
				if(line.isEmpty()){ 
					continue;
				}
				String[] e = line.split("\\s+");
				
				String rS=getShortFormRelation(e[0].trim(), source);
				String rT=getShortFormRelation(e[1].trim(), target);
				int sharedXY=Integer.parseInt(e[2]);
				int originalSamples=Integer.parseInt(e[3]);
				int pcaDenominator=Integer.parseInt(e[4]);
				
				AlignmentPCA_CWA align=new AlignmentPCA_CWA(rS, rT, sharedXY, originalSamples, pcaDenominator);
				
				/** insert the alignment **/
				ArrayList<AlignmentPCA_CWA> list=allAlignements.get(rS);
				if(list==null){
					list=new ArrayList<AlignmentPCA_CWA> ();
					allAlignements.put(rS, list);
				}
				list.add(align);
				
		}
		alignReader.close();
		
		BufferedWriter pca_file=new BufferedWriter(new FileWriter(pathToNewFile));
		pca_file.write("subRelation \t superRelation \t shared \t Den_Cwa \t Den_Pca \t PCA \t CWA \n");
		
		for(ArrayList<AlignmentPCA_CWA> list: allAlignements.values()){
				Collections.sort(list, new Comp_AlignmentPCA_CWA());
				for(AlignmentPCA_CWA align: list){
						pca_file.write(align.toStringAll());
						pca_file.flush();
				}
				pca_file.write("\n");
		}
	
		pca_file.close(); 
	}
	
	
	public static final String getShortFormRelation(String r, KB kb){
		return (r.startsWith(kb.resourcesDomain))? r.replaceFirst(kb.resourcesDomain, kb.name+":"):r;
	}

    public static final void computePCA_CWA_fromNominatorAndDenominatorValues(String rootDir, String outputDir, String prefix, KB source, KB target ){
    		String alignmentFile = rootDir+source.name+"->"+target.name+"/"+source.name+"_"+target.name+"_align.txt"; 
		String resultFile=outputDir+prefix+"_"+source.name+"_"+target.name+"_pca_cwa.txt"; 
		try {
			parseAlignmentFile(alignmentFile, resultFile, source, target);
		} catch (NumberFormatException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String rootDir="/Users/adi/Dropbox/DBP/feb-sofya/_gold/";
	
		KB yago = new KB("yago", null, "http://yago-knowledge.org/resource/");
		KB dbpedia = new KB("dbpedia",null, "http://dbpedia.org/ontology/");
		KB freebase = new KB("freebase",null, "http://rdf.freebase.com/ns/");
		
		String outputDir="/Users/adi/Dropbox/DBP/feb-sofya/_gold/_user/";
		computePCA_CWA_fromNominatorAndDenominatorValues(rootDir, outputDir, "user", dbpedia, yago);
		computePCA_CWA_fromNominatorAndDenominatorValues(rootDir, outputDir, "user",  yago, dbpedia);
		
		computePCA_CWA_fromNominatorAndDenominatorValues(rootDir, outputDir, "user", dbpedia, freebase);
		
		computePCA_CWA_fromNominatorAndDenominatorValues(rootDir, outputDir, "user",  yago, freebase);
		
		
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
