package preprocessing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;

public class Functionality {
	public static final int getNumerator(String rel, String endpoint, String graph) throws Exception {
		  String q = "SELECT (COUNT(distinct ?x) AS ?n) where { graph <"+graph+"> "
                + " {  ?x " + "<"+rel+">" + " ?y} "
                + "}";
		  
		return executeQuery(q, endpoint);
	}
	
	
	public static final int getNumeratorForInverse(String rel, String endpoint, String graph) throws Exception {
		  String q = "SELECT (COUNT(distinct ?y) AS ?n) where { graph <"+graph+"> "
              + " {  ?x " +  "<"+rel+">"  + " ?y} "
              + "}";
		  
		return executeQuery(q, endpoint);
	}
      
      
	
	public static final int getDenominator(String rel, String endpoint, String graph) throws Exception {
		  String q = "SELECT (COUNT( *) AS ?n) where { graph <"+graph+"> "
              + "  { ?x " +  "<"+rel+">"  + " ?y} "
              + "}";
		  
		return executeQuery(q, endpoint);
	}
	
	
	public static final int executeQuery(String querystr, String endpoint) throws Exception{
		
          QueryEngineHTTP query = new QueryEngineHTTP(endpoint, querystr);

          ResultSet rst = query.execSelect();
          int val=-1;
          while (rst.hasNext()) {
              QuerySolution qs = rst.next();
              
              if(qs.get("?n")==null) continue;
              String n=qs.get("?n").asLiteral().getString();
              val=Integer.valueOf(n);
         
             
             
          }
          
           return val;
         
	}
	
	public static final void getFunctionality(ArrayList<String> relations, String dir, String newFileName, String endpoint, String graph) throws Exception{
		BufferedWriter func_file=new BufferedWriter(new FileWriter(dir+newFileName));
		
		func_file.write("Relation \t Numerator \t NumeratorForInverse \t Denominator \t Functionality \t InvFunctionality \n");
		
		for(String r: relations){
			String rel=   r; //"<"+r+">";
			System.out.println("Process " + rel); 
			
			int numerator=getNumerator(rel, endpoint,graph);
			
			if(numerator<0){
				func_file.write(r+"\t error \n");
				continue;
			}
			
			int numeratorForInverse=getNumeratorForInverse(rel, endpoint,graph);
			
			if(numeratorForInverse<0){
				func_file.write(r+"\t error \n");
				continue;
			}
			
			
			int denominator=getDenominator(rel, endpoint,graph);
			
			if(denominator<0){
				func_file.write(r+"\t error \n");
				continue;
			}
			
			double f=((double)numerator)/denominator;
			double f_inv=((double)numeratorForInverse)/denominator;
			
			String line=r+"\t "+numerator+"\t "+numeratorForInverse+"\t "+denominator+"\t "+f+"\t "+f_inv+"\n";
			func_file.write(line);
			func_file.flush();
			System.out.print(line);
			
			
		}
		
	 func_file.close(); 

	}
      
        public static final ArrayList<String> loadRelationsInOrder(String file) throws Exception{
		File f=new File(file);
		if(!f.exists()) {
			System.err.println("  File does not exit "+file);
			return null;
		}else {
			/** System.out.println(" Load : "+file);**/
		}
		
		BufferedReader pairReader = new BufferedReader(new InputStreamReader(new FileInputStream(file),"UTF8"));
		
		ArrayList<String> list=new   ArrayList<String>();;
		String line=null;
		while ((line = pairReader.readLine()) != null) {
				if(line.isEmpty()) continue;
				String[] e = line.split("\\s+");
				String relation=e[0];
				
				if(!list.contains(relation))  list.add(relation);		   
		}
		pairReader.close();
		return list;
	}
  
  public static void main(String[] args){
      
	         /*If we run our code from the lab's network we can run directly from eclipse/netbeans and the endpoint we use is: http://s6.adam.uvsq.fr:8890/sparql*/
              /*If we want to run our code from home then we have to transfer our .jar and the endpoint we use is: http://localhost:8890/sparql*/
                              
              String graph = "freebase"; //dbpedia //freebase //yago
              String endpoint= "http://s6.adam.uvsq.fr:8890/sparql"; //"http://localhost:8890/sparql";   // "http://s6.adam.uvsq.fr:8890/sparql"; //we use this from the lab
	        String dir = "/Users/mary/Dropbox/Mary_SOFYA_code et al/statistics/"+graph+"/";
	        		
	        String fileWithRelations="relationsWithNoOfTriplesFreebase.txt";
	        		
	        String newFileName= graph+"_functionality.txt";
	        
	        
	        ArrayList<String> relations;
			try {
				relations = loadRelationsInOrder(dir+fileWithRelations);
				
				getFunctionality(relations, dir, newFileName, endpoint,graph);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        
	        		
	        
  }
}
