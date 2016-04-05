package crawd;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;

import gold.KB;
import gold.Relation;

public class CheckInstances {
   
	public static final void printReport(String fileWithResults, Relation rS, Relation rT, KB kb) throws Exception{
		   BufferedWriter writer = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(fileWithResults), "UTF-8"));
		   
		   
		   writer.write("Counter Examples (common x):   \n"+rS+" \t "+rT+"\n");
		   writer.write("x  \t y_rS  \t y_rT ");
		   ArrayList<TripleResult>  triples=getCounterExamples(rS, rT, kb);
		   for(TripleResult r: triples){
			   writer.write(r.toString()+"\n");
		   }
		   writer.write("\n\n\n");
		   
		  Relation rSInv=new Relation(rS.uri, !rS.isDirect);
		  Relation rTInv=new Relation(rT.uri, !rT.isDirect);
		  writer.write("Counter Examples (common y):   \n"+rSInv+" \t "+rTInv+"\n");
		  writer.write("y  \t x_rS  \t x_rT ");
		  triples=getCounterExamples(rSInv, rTInv, kb);
		   for(TripleResult r: triples){
			   writer.write(r.toString()+"\n");
		   }
		   writer.write("\n\n\n");
          
		   
		   
		   writer.write("OVERLAP:   \n"+rS+"\t"+rT+"\n");
		   ArrayList<TupleResult> overlap=getOverlapp(rS, rT, kb);
		   for(TupleResult r: overlap){
			   writer.write(r.x_label+"\t\t "+r.y_label+"\n");
		   }
		   writer.write("\n\n\n");
		   		   
		   writer.write(" ONLY     \n"+rS+"\n");
		   ArrayList<TupleResult> diff=getOnly(rS, rT, kb);
		   for(TupleResult r: diff){
			   writer.write(r.x_label+"\t "+r.y_label+"\n");
		   }
		   writer.write("\n\n\n");
		   
		   writer.write("ONLY:   \n"+rT+"\n");
		   diff=getOnly(rT, rS, kb);
		   for(TupleResult r: diff){
			   writer.write(r.x_label+"\t "+r.y_label+"\n");
		   }
		   writer.write("\n\n\n");
		    
		   writer.close();
	}
	
	
 
	public static final  ArrayList<TupleResult> getOverlapp(Relation rS, Relation rT,  KB kb) throws Exception{
	 ArrayList<TupleResult> results=new ArrayList<TupleResult>();
	 
	 String querystr= "  PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n";
	 querystr+= "select distinct  ?x ?y  ?xl  ?yl    where {graph <" + kb.name + "> {\n";
	 
	 querystr+=(rS.isDirect)?" ?x <"+rS.uri+">  ?y. ":" ?y <"+rS.uri+">  ?x. ";
	 querystr+=(rT.isDirect)?" ?x <"+rT.uri+">  ?y. ":" ?y <"+rT.uri+">  ?x. ";
	 
	 querystr+="   ?x  rdfs:label ?xl. ?y rdfs:label ?yl. ";
	 querystr+=" FILTER ((lang(?xl) = \"\" || lang(?xl) = \"en\") && (lang(?yl) = \"\" || lang(?yl) = \"en\"))  \n";
	 querystr+=" }}  limit 20 ";
	//System.out.println("Overlap: "+querystr);
	
	try{
		QueryEngineHTTP query = new QueryEngineHTTP(kb.endpoint, querystr);
		query.setTimeout(100000000000l);
		ResultSet rst = query.execSelect();
		if (rst != null) {
			while (rst.hasNext()) {
				QuerySolution qs = rst.next();
				String x= qs.get("?x").toString();
				String y= qs.get("?y").toString();
				
				String xl= qs.get("?xl").asLiteral().getString().trim();
				String yl= qs.get("?yl").asLiteral().getString().trim();
				results.add(new TupleResult(x, y, xl, yl));
				
			}
		}
    System.out.print("*");
		}catch(Exception e){
			System.out.println(querystr);
			e.printStackTrace();
			System.exit(0);
		}
	
	return results;
} 
 
	public static final  ArrayList<TripleResult> getCounterExamples(Relation rS, Relation rT,  KB kb) throws Exception{
		 ArrayList<TripleResult> results=new ArrayList<TripleResult>();
		 
		 String querystr= "  PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n";
		 querystr+= "SELECT distinct  ?x ?y1 ?y2  ?xl  ?yl1  ?yl2       where {graph <" + kb.name + "> {\n";
		 
		 querystr+=(rS.isDirect)?" ?x <"+rS.uri+">  ?y1. ":" ?y1 <"+rS.uri+">  ?x. ";
		 querystr+="\n";
		 querystr+=(rT.isDirect)?" ?x <"+rT.uri+">  ?y2. ":" ?y2 <"+rT.uri+">  ?x. "; 
		 querystr+="\n";
		 querystr+=" OPTIONAL {   ";
		 querystr+=(rS.isDirect)?" ?x <"+rS.uri+">  ?y. ":" ?y <"+rS.uri+">  ?x. ";
		 querystr+=(rT.isDirect)?" ?x <"+rT.uri+">  ?y. ":" ?y <"+rT.uri+">  ?x. ";
		 querystr+=" }  \n";
		 querystr+=" ?x  rdfs:label ?xl. ?y1 rdfs:label ?yl1.  ?y2 rdfs:label ?yl2.   \n";
		 querystr+=" FILTER (! bound(?y) && (lang(?xl) = \"\" || lang(?xl) = \"en\") && (lang(?yl1) = \"\" || lang(?yl1) = \"en\") && (lang(?yl2) = \"\" || lang(?yl2) = \"en\"))  \n";
		 querystr+=" }}  limit 20 ";
		System.out.println("\n Counter Example : \n"+querystr);
		
		
		try{
			QueryEngineHTTP query = new QueryEngineHTTP(kb.endpoint, querystr);
			query.setTimeout(100000000000l);
			ResultSet rst = query.execSelect();
			if (rst != null) {
				while (rst.hasNext()) {
					QuerySolution qs = rst.next();
					String x= qs.get("?x").toString();
					String y1= qs.get("?y1").toString();
					String y2= qs.get("?y2").toString();
					
					String xl= qs.get("?xl").asLiteral().getString().trim();
					String yl1= qs.get("?yl1").asLiteral().getString().trim();
					String yl2= qs.get("?yl2").asLiteral().getString().trim();
					TripleResult t=new TripleResult(x, y1, y2, xl, yl1, yl2);
					results.add(t);
					System.out.println(t.toString());
				}
			}
	    System.out.print("*");
			}catch(Exception e){
				System.out.println(querystr);
				e.printStackTrace();
				System.exit(0);
			}
		
		return results;
	} 
	
	
	public static final  ArrayList<TupleResult> getEntities(Relation rS, Relation rT,  KB kb) throws Exception{
		 ArrayList<TupleResult> results=new ArrayList<TupleResult>();
		 
		 String querystr= "  PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n";
		 querystr+= "select distinct  ?x ?y  ?xl  ?yl    where {graph <" + kb.name + "> {\n";
		 
		 querystr+=(rS.isDirect)?" ?x <"+rS.uri+">  ?y. ":" ?y <"+rS.uri+">  ?x. ";
		 querystr+=(rT.isDirect)?" ?x <"+rT.uri+">  ?y. ":" ?y <"+rT.uri+">  ?x. ";
		 
		 querystr+=" }}  limit 20 ";
		//System.out.println("Overlap: "+querystr);
		
		try{
			QueryEngineHTTP query = new QueryEngineHTTP(kb.endpoint, querystr);
			query.setTimeout(100000000000l);
			ResultSet rst = query.execSelect();
			if (rst != null) {
				while (rst.hasNext()) {
					QuerySolution qs = rst.next();
					String x= qs.get("?x").toString();
					String y= qs.get("?y").toString();
				
					results.add(new TupleResult(x, y, null, null));
					
				}
			}
	    System.out.print("*");
			}catch(Exception e){
				System.out.println(querystr);
				e.printStackTrace();
				System.exit(0);
			}
		
		return results;
	} 
	
	
	public static final  ArrayList<TupleResult> getOnly(Relation r, Relation rBad,  KB kb) throws Exception{
		 ArrayList<TupleResult> results=new ArrayList<TupleResult>();
		 
		 String querystr= "  PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n";
		 querystr+= "select distinct ?x ?y  ?xl  ?yl    where {graph <" + kb.name + "> {\n";
		 
		 querystr+=(r.isDirect)?" ?x <"+r.uri+">  ?y. ":" ?y <"+r.uri+">  ?x. ";
		 querystr+="FILTER NOT EXISTS { ";
		 querystr+=((rBad.isDirect)?" ?x <"+rBad.uri+">  ?y. ":" ?y <"+rBad.uri+">  ?x. ");
		 querystr+=" }";
		 
		 querystr+="   ?x  rdfs:label ?xl. ?y rdfs:label ?yl. FILTER ((lang(?xl) = \"\" || lang(?xl) = \"en\") && (lang(?yl) = \"\" || lang(?yl) = \"en\"))  \n";
		 querystr+=" }}  limit 20 ";
		//System.out.println("Diff query : "+querystr);
		
		try{
			QueryEngineHTTP query = new QueryEngineHTTP(kb.endpoint, querystr);
			query.setTimeout(100000000000l);
			ResultSet rst = query.execSelect();
			if (rst != null) {
				while (rst.hasNext()) {
					QuerySolution qs = rst.next();
					String xl= qs.get("?xl").asLiteral().getString().trim();
					String yl= qs.get("?yl").asLiteral().getString().trim();
					String x= qs.get("?x").toString();
					String y= qs.get("?y").toString();
					
					results.add(new TupleResult(x, y, xl, yl));
				}
			}
			
	
	    System.out.print("*");
			}catch(Exception e){
				System.out.println(querystr);
				e.printStackTrace();
				System.exit(0);
			}
		
		return results;
	} 
 
	
	
	
 public static final class TupleResult{
	 
	 
	 public final String x_label;
	 public final String y_label;
	 
	 public final String x;
	 public final String y;
	 
	 
	
	 public TupleResult(String x,  String y, String x_label, String y_label){
		
		 this.x=x;
		 this.y=y;
		 
		 this.x_label=x_label;
		 this.y_label=y_label;
		 
	 }
	 
	 @Override
	 public String toString(){
		 return x+"\t "+y+"\t "+x_label+"\t "+y_label;
	 }
	 
 }
 
 
 
public static final class TripleResult{
	 
	 
	 public final String x_label;
	 public final String y1_label;
	 public final String y2_label;
	 
	 public final String x;
	 public final String y1;
	 public final String y2;
	 
	
	 public TripleResult(String x,  String y1, String y2, String x_label, String y1_label, String y2_label){
		
		 this.x=x;
		 this.y1=y1;
		 this.y2=y2;
		 
		 this.x_label=x_label;
		 this.y1_label=y1_label;
		 this.y2_label=y2_label;
		 
	 }
	 
	 @Override
	 public String toString(){
		 return x_label+"\t "+y1_label+"\t "+y2_label+"\t "+x+"\t "+y1+"\t "+y2;
	 }
	 
 }
 
 
}
