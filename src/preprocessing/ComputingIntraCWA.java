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

import gold.GetOvelappingRels;
import gold.KB;
import gold.Relation;

public class ComputingIntraCWA {

	public static final DecimalFormat df = new DecimalFormat("0.00");

	
	
	public static final void comptuteCWA(KB kb, String fileWithFunctionality, String fileWithAlignments, String newFile) throws Exception {
	
		abortIfFileDoesNotExist(fileWithFunctionality);
		abortIfFileDoesNotExist(fileWithAlignments);
		
		
	     ArrayList<Relation> relations= new ArrayList<Relation>();
		GetOvelappingRels.loadRelationsFromFilesWithFunctionality(relations,fileWithFunctionality, true, 0, 4, 5, 3);
	
	   
		BufferedReader alignReader = new BufferedReader(new InputStreamReader(new FileInputStream(fileWithAlignments),"UTF8"));
		HashMap<Relation, ArrayList<AlignmentCWA>> allAlignements=new HashMap<Relation, ArrayList<AlignmentCWA>> ();
		String line=null;
		while ((line = alignReader.readLine()) != null) {
				if(line.isEmpty()){ 
					continue;
				}
				String[] e = line.split("\\s+");
				
				Relation rS=getRelationFromStringDescr(e[0].trim(), relations);
				Relation rT=getRelationFromStringDescr(e[1].trim(), relations);
				
		
				int sharedXY=Integer.parseInt(e[2]);
		
				AlignmentCWA align=new AlignmentCWA(rS, rT, sharedXY);
				
				/** insert the alignment **/
				ArrayList<AlignmentCWA> list=allAlignements.get(align.rS);
				if(list==null){
					list=new ArrayList<AlignmentCWA> ();
					allAlignements.put(rS, list);
				}else {
					if(list.contains(align)) continue;
				}
				list.add(align);
				
		}
		alignReader.close();
		
		
		BufferedWriter pca_file=new BufferedWriter(new FileWriter(newFile));
		pca_file.write("subRelation \t superRelation \t shared \t -->CWA \t  <--CWA \n");
		
		for(ArrayList<AlignmentCWA> list: allAlignements.values()){
				Collections.sort(list, new Comp_AlignmentCWA());
				for(AlignmentCWA align: list){
						pca_file.write(align.toStringAll()+"\n");
						pca_file.flush();
						System.out.println(align.toStringAll());
				}
				pca_file.write("\n");
				System.out.println();
		}
	
		pca_file.close(); 
		
	}
	
	public static final Relation getRelationFromStringDescr(String s, ArrayList<Relation> relations)throws Exception{
	
		Relation r=(s.endsWith("-"))?new Relation(s.substring(0,s.length()-1), false):new Relation(s, true);
		int indexr=relations.indexOf(r);
		if(indexr<0) {
			int indexInv=relations.indexOf(new Relation(r.uri, !r.isDirect));
			if( indexInv<0 ) {
			System.err.println("Unknown relation "+r);
			System.exit(0);
			}
			r.tupleNo=relations.indexOf(indexInv);
			return r;
		}
		return relations.get(indexr);
		
		
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
			this.rS= (rS.tupleNo<rT.tupleNo)?rS:rT;
			this.rT=(rS.tupleNo<rT.tupleNo)?rT:rS;
		}
		
		public final String toStringAll(){
					return  rS + "  " + rT+ " \t "+ sharedXY + " \t " + ((int) rS.tupleNo)+ " \t " +((int) rT.tupleNo)+ " \t " +df.format(((double)sharedXY)/rS.tupleNo)+ " \t " +df.format(((double)sharedXY)/rT.tupleNo);
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
			return rS.equals(o.rS) && rT.equals(o.rT) || rS.equals(o.rT) && rT.equals(o.rS) ;
		}
		
		
		
	}
	
	public static final class Comp_AlignmentCWA implements Comparator<AlignmentCWA> {

		@Override
		public int compare(AlignmentCWA o1, AlignmentCWA o2) {
			double directImplication = -(o1.getDirectImplication()-o2.getDirectImplication());
			if(directImplication!=0) return (int)directImplication*100000;
			return (int) (-(o1.getReverseImplication()-o2.getReverseImplication()*100000));
		}

		
		
	}
	
	public static void main(String[] args) throws Exception {
		
		KB yago = new KB("yago", null, "http://yago-knowledge.org/resource/");
		KB dbpedia = new KB("dbpedia",null, "http://dbpedia.org/ontology/");
		KB freebase = new KB("freebase",null, "http://rdf.freebase.com/ns/");
		
		KB kb=freebase;
		
		String dir="/Users/adi/Dropbox/DBP/feb-sofya/";
		String fileWithFunctionality = dir + kb.name + "/" + kb.name + "_functionality_ee.txt";
		String fileWithAlignments=dir + kb.name + "/" + kb.name + "_"+ kb.name +"_align_part_1.txt";
		String newFile=dir + kb.name + "/" + kb.name + "_"+ kb.name +"_align_CWA_part_1.txt";	
		comptuteCWA(kb,  fileWithFunctionality, fileWithAlignments, newFile);
	}
	
}
