package postprocess;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import crawd.CheckInstances;
import gold.GetSelfPCAConf;
import gold.GetSelfPCAConf.CompleteAlignment;
import postprocess.ComputingPCAandCWA.AlignmentPCA_CWA;
import postprocess.ComputingPCAandCWA.Comp_Alignment_Max_CWA;
import gold.KB;
import gold.Relation;


public class QuadrupleAlignmentsGenerate {
	public static final DecimalFormat df = new DecimalFormat("0.00");
	
	/************************************************************************************************************************/
	/**** Load Alignments **/
	/************************************************************************************************************************/
	public static final HashMap<String, ArrayList<CompleteAlignment>> getSymetricAlignments(String file) throws Exception {
		
		   HashMap<String, ArrayList<CompleteAlignment>>  map=new   HashMap<String, ArrayList<CompleteAlignment>>();
	       
		   BufferedReader pairReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
	
			String line = null;
			while ((line = pairReader.readLine()) != null) {
				line=line.trim();
				if (line.isEmpty())	continue;
				String[] e = line.split("\\s+");
				
				Relation rS=Relation.getRelationFromStringDesc(e[0].trim());
				Relation rT=Relation.getRelationFromStringDesc(e[1].trim());
				
				
			    int shared=Integer.valueOf(e[2].trim());
			    int original=Integer.valueOf(e[3].trim());
			    int pcaDenDir=Integer.valueOf(e[4].trim());
			    int pcaDenInv=Integer.valueOf(e[5].trim()); 
			    
			    GetSelfPCAConf.CompleteAlignment a=new GetSelfPCAConf.CompleteAlignment(rS, rT, shared, original, pcaDenDir, pcaDenInv);
			    String key=getUniqueKeyForPair(rS, rT);
			    addToMap(key, map, a);
			    
			    /**
				Relation rSInv=new Relation(rS.uri, !rS.isDirect);
				Relation rTInv=new Relation(rT.uri, !rT.isDirect);
				GetSelfPCAConf.PartialAlignment aInv=new GetSelfPCAConf.PartialAlignment(rSInv, rTInv, shared, original,  pcaDenInv, pcaDenDir);
			    String keyInv=getUniqueKeyForPair(rSInv, rTInv);
			    addToMap(keyInv, map, aInv);  **/
			
			}
			pairReader.close();
			return map;
		}
		
	public static final String getUniqueKeyForPair(Relation rS, Relation rT){
		String key=(rS.toString().compareTo(rT.toString())<0)?rS.uri+rT.uri:rT.uri+rS.uri;
		return key+((rS.isDirect==rT.isDirect)?"":"-");
	}

	public static final  void addToMap(String key, HashMap<String, ArrayList<CompleteAlignment>> map, CompleteAlignment a){
		ArrayList<CompleteAlignment> list=map.get(key);
		if(list==null){
			list= new  ArrayList<CompleteAlignment> ();
			map.put(key, list);
		}
		list.add(a);
	}
	
	/************************************************************************************************************************/
	/**** Filter those where at least one of the PCA is higher than threshold and print **/
	/************************************************************************************************************************/
   public static final void filterMinPCA(HashMap<String, ArrayList<CompleteAlignment>> map, double thrsPCA){
	   Iterator<Entry<String, ArrayList<CompleteAlignment>>> it=map.entrySet().iterator();
	   while(it.hasNext()){
		   Entry<String, ArrayList<CompleteAlignment>> entry=it.next();
		  
		   double maxPCA=0;
		   for(CompleteAlignment a: entry.getValue()){
			   if(maxPCA<a.pcaDirRS) maxPCA=a.pcaDirRS;
			   if(maxPCA<a.pcaDirInvRS) maxPCA=a.pcaDirInvRS;
		   }
		   
		   if(maxPCA<thrsPCA) it.remove();
	   }
	 
   }
	
   public static final void filterMaxCWA(HashMap<String, ArrayList<CompleteAlignment>> map, double thrsCWA){
	   Iterator<Entry<String, ArrayList<CompleteAlignment>>> it=map.entrySet().iterator();
	   while(it.hasNext()){
		   Entry<String, ArrayList<CompleteAlignment>> entry=it.next();
		  
		   double maxCWA=0;
		   for(CompleteAlignment a: entry.getValue()){
			   if(maxCWA<a.cwa) maxCWA=a.cwa;
			
		   }
		   
		   if(maxCWA>thrsCWA) it.remove();
	   }
   }
   
   public static final void printResults(int fromIndex, String resultDir, HashMap<String, ArrayList<CompleteAlignment>> map, KB kb)throws Exception {
	   	String fileWithResults=resultDir+"_"+kb.name+"_promissing_PCAs.txt";
	   	BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(fileWithResults), "UTF-8"));
	   
	    ArrayList<ArrayList<CompleteAlignment>> listArrays=new ArrayList<ArrayList<CompleteAlignment>> ();
		listArrays.addAll(map.values());
		Collections.sort(listArrays, new CompAlignmentsBasedOnMAXPCA() );
		
		int id=0;
		for(ArrayList<CompleteAlignment> list: listArrays){
			id++;
			if(id<fromIndex) {
				continue;
			}
			

			/** generate a report file  **/
			String file=resultDir+kb.name+"/"+kb.name.substring(0,3)+"_"+id+".txt";
			CheckInstances.printReport(file, list.get(0).rS, list.get(0).rT, kb);
			
			
			/** print the alignment to to the report **/
			for(CompleteAlignment a : list){
				writer.write(id+"  "+a.toStringAll()+"\n");
				System.out.println(id+"  "+a.toStringAll());
			}
			System.out.println();
			writer.write("\n");
			writer.flush();
			
		}
	
	writer.close();
   }
   
   
   
   /************************************************************/
   /** class comparison **/
   /***********************************************************/
	public static final class CompAlignmentsBasedOnMAXPCA implements Comparator<ArrayList<CompleteAlignment>> {

		@Override
		public int compare(ArrayList<CompleteAlignment> o1, ArrayList<CompleteAlignment> o2) {
			
			double max1=getMaxPCA(o1);
			double max2=getMaxPCA(o2);
			
			int diff=-(int)((max1-max2)*1000);
			if(diff!=0) return diff;
			
			diff=-(int)(((o1.get(0).sharedXY-o2.get(0).sharedXY)));
			if(diff!=0)  return diff;
			
			return (int)(-((o1.get(0).cwa-o2.get(0).cwa)*1000));
		}
	
		
		private final double getMaxPCA(ArrayList<CompleteAlignment> o1){
			double maxPCA=0;
			for(CompleteAlignment a: o1){
				if(maxPCA<a.pcaDirRS) maxPCA=a.pcaDirRS;
				if(maxPCA<a.pcaDirInvRS) maxPCA=a.pcaDirInvRS;
			}
			return maxPCA;
		}
	}
   
    public static void main(String[] args) throws Exception {
		
    	KB yago = new KB("yago", "http://s6.adam.uvsq.fr:8892/sparql", "http://yago-knowledge.org");
	KB dbpedia = new KB("dbpedia", "http://s6.adam.uvsq.fr:8892/sparql", "http://dbpedia.org");
	KB freebase = new KB("freebase", "http://s6.adam.uvsq.fr:8892/sparql", "http://rdf.freebase.com");
				
	KB kb=freebase;
		
	String dir="/Users/adi/Dropbox/DBP/feb-sofya/"; //"./feb-sofya/";  
		
		
		String fileWithAligns=dir + kb.name + "/" + kb.name + "_"+ kb.name +"_PCA_CWA.txt";	
		HashMap<String, ArrayList<CompleteAlignment>> map=getSymetricAlignments(fileWithAligns);
		filterMinPCA(map, 0.7);
		filterMaxCWA(map, 0.3);
		
		String crawdDir=dir+"_crawd/";
		int fromIndex=(args.length==0)?-1:Integer.valueOf(args[0]);
		

		printResults(fromIndex, crawdDir, map, kb);
		
		
	}
	
	
	
}
