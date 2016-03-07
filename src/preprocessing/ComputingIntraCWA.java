package preprocessing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

import gold.GetOvelappingRels;
import gold.KB;
import gold.Relation;

public class ComputingIntraCWA {

	public static final DecimalFormat df = new DecimalFormat("0.00");

	
	
	public static final void comptuteCWA(KB kb, String fileWithFunctionality, String fileWithAlignments, String newFile, double thrs) throws Exception {
	
		abortIfFileDoesNotExist(fileWithFunctionality);
		abortIfFileDoesNotExist(fileWithAlignments);
		
		
	     ArrayList<Relation> relations= new ArrayList<Relation>();
		GetOvelappingRels.loadRelationsFromFilesWithFunctionality(relations,fileWithFunctionality, true, 0, 4, 5, 3);
		
		// extract relation, tuple numbers since is needed further
		HashMap<String, Integer> tupleNumber=new HashMap<String, Integer> ();
		for(Relation r: relations){
			tupleNumber.put(r.uri, new Integer((int)r.tupleNo));
		}
		relations.clear();
		
		BufferedReader alignReader = new BufferedReader(new InputStreamReader(new FileInputStream(fileWithAlignments),"UTF8"));
	    HashSet<AlignmentCWA> allAlignements=new HashSet<AlignmentCWA>();
		String line=null;
		while ((line = alignReader.readLine()) != null) {
				if(line.isEmpty()){ 
					continue;
				}
				String[] e = line.split("\\s+");
				
				System.out.println(line);
				
				Relation rS=getRelationFromStringDescr(e[0].trim(), tupleNumber);
				Relation rT=getRelationFromStringDescr(e[1].trim(), tupleNumber);
				
				if(rS.uri.equals(rT.uri) && rS.isDirect==!(rT.isDirect)) continue;
		
				int sharedXY=Integer.parseInt(e[2]);
				AlignmentCWA align=new AlignmentCWA(rS, rT, sharedXY);
				
				 if(align.getDirectImplication()<thrs && align.getReverseImplication()<thrs) continue;
				
				 if(align.getDirectImplication()>= thrs && align.getReverseImplication()>= thrs) continue;
					
				/** insert the alignment **/
				if(allAlignements.contains(align)) continue;
				allAlignements.add(align);		
		}
		alignReader.close();
		
		
		BufferedWriter pca_file=new BufferedWriter(new FileWriter(newFile));
		pca_file.write("subRelation \t superRelation \t shared \t -->CWA \t  <--CWA \n");
		
		
		ArrayList<AlignmentCWA> list=new ArrayList<AlignmentCWA>();
		list.addAll(allAlignements);
		Collections.sort(list, new Comp_AlignmentCWA());
		for(AlignmentCWA align: list){
						pca_file.write(align.toStringAll()+"\n");
						pca_file.flush();
						System.out.println(align.toStringAll());
				}
					
		pca_file.close(); 
		
	}
	
	public static final Relation getRelationFromStringDescr(String s, HashMap<String, Integer> tupleNumber)throws Exception{
	
		String uri=(s.endsWith("-"))?s.substring(0,s.length()-1): s;
		Relation r=(s.endsWith("-"))?new Relation(uri, false):new Relation(uri, true);
		
		r.tupleNo=tupleNumber.get(uri);
		
		return r;
		
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
	
	/**************************************************************************/
	/*** CLASS ALIGNMENT ***/
	/**************************************************************************/

	public static class  AlignmentCWA{
		public final Relation rS;
		public final Relation rT;
		
		public final int sharedXY;
		
			
		public AlignmentCWA(Relation rS, Relation rT, int sharedXY){
			this.sharedXY=sharedXY;
			this.rS= (rS.tupleNo<=rT.tupleNo)?rS:rT;
			this.rT=(rS.tupleNo<=rT.tupleNo)?rT:rS;
		}
		
		@Override
		public final String toString(){
			return  rS + "  " + rT+ "   "+ sharedXY + "  " + ((int) rS.tupleNo)+ "   " +((int) rT.tupleNo)+ "   " +df.format(((double)sharedXY)/rS.tupleNo)+ "   " +df.format(((double)sharedXY)/rT.tupleNo);
}

		
		public final String toStringAll(){
					return  rS + "  " + rT+ "   "+ sharedXY + "   " + ((int) rS.tupleNo)+ "   " +((int) rT.tupleNo)+ "  " +df.format(((double)sharedXY)/rS.tupleNo)+ "  " +df.format(((double)sharedXY)/rT.tupleNo);
		}
		
		public double getDirectImplication(){
			return ((double)sharedXY)/rS.tupleNo;
		}
		
		public double getReverseImplication(){
			return ((double)sharedXY)/rT.tupleNo;
		}
		
		
		
		@Override
		public boolean equals(Object other){
			AlignmentCWA o=(AlignmentCWA)other;
		
			return o.sharedXY==sharedXY && o.getSignature().equals(getSignature());
		}
		
		
		@Override
		public int hashCode(){
			return getSignature().hashCode();
			
		}
		
		public String getSignature(){
			int minuses=((rS.isDirect)?0:-1)+((rT.isDirect)?0:-1);
			if(minuses==-2) minuses=0;
			return ((rS.uri.compareTo(rT.uri)<0)?rS.uri+" "+rT.uri:rT.uri+" "+rS.uri)+minuses;
		}
		
	}
	
	public static final class Comp_AlignmentCWA implements Comparator<AlignmentCWA> {

		@Override
		public int compare(AlignmentCWA o1, AlignmentCWA o2) {
			
			
			int directImplication = (int)(-(o1.getDirectImplication()-o2.getDirectImplication())*100000);
			if(directImplication!=0) return directImplication;
			
			int tupleNoDiff=-(int)(o1.rS.tupleNo-o2.rS.tupleNo);
			if(tupleNoDiff!=0) return tupleNoDiff;
			 
			int inverseImplication =(int) ((-(o1.getReverseImplication()-o2.getReverseImplication())*100000));
			if(inverseImplication!=0) return  inverseImplication;
			
			return (int)(o1.rT.tupleNo-o2.rT.tupleNo);
		}
	}
	
	public static void main(String[] args) throws Exception {
		
		KB yago = new KB("yago", null, "http://yago-knowledge.org/resource/");
		KB dbpedia = new KB("dbpedia",null, "http://dbpedia.org/ontology/");
		KB freebase = new KB("freebase",null, "http://rdf.freebase.com/ns/");
		
		KB kb=dbpedia;
		
		String dir="/Users/adi/Dropbox/DBP/feb-sofya/";
		String fileWithFunctionality = dir + kb.name + "/" + kb.name + "_functionality_ee.txt";
		String fileWithAlignments=dir + kb.name + "/" + kb.name + "_"+ kb.name +"_align.txt";
		String newFile=dir + kb.name + "/" + kb.name + "_"+ kb.name +"_align_CWA.txt";	
		comptuteCWA(kb,  fileWithFunctionality, fileWithAlignments, newFile, 0.95);
	
	}
}
