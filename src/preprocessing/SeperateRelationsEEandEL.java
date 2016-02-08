package preprocessing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;

public class SeperateRelationsEEandEL {
	/**
     * *************************
     */
    /**
     * load the relations *
     */
    /**
     * *************************
     */
    public static final HashMap<String, String> loadRelations(String file) throws Exception {
        File f = new File(file);
        if (!f.exists()) {
            System.err.println("  File does not exit " + file);
            return null;
        } else {
            /**
             * System.out.println(" Load : "+file);*
             */
        }

        BufferedReader pairReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));

        /*HashMap with the relation as a key, and the whole line of the functionalities document as value*/
        HashMap<String, String> list = new HashMap<String, String>();;
        String line = null;
        int i=0;
        while ((line = pairReader.readLine()) != null) {
            if (line.isEmpty() || i==0) {
                i++;
                continue;
            }
            String[] e = line.split("\\s+");
            String relation = e[0];

            if (!list.containsKey(relation)) {
                list.put(relation, line);
            }
        }
        pairReader.close();
        return list;
    }

    /**
     * read line by line the file with relations and separate them into two
     * files : one for ent-ent and one for ent-lit *
     */
    public static final void separateRelations(HashMap<String, String> relations, String filename, String endpoint, String graph) throws Exception {
        BufferedWriter ent_ent = new BufferedWriter(new FileWriter(filename + "_ee.txt"));
        BufferedWriter ent_lit = new BufferedWriter(new FileWriter(filename + "_el.txt"));

        Iterator it = relations.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            String rel = "<" + pair.getKey() + ">";
            System.out.println("Process " + rel);

            boolean test = isObjectAnEntity(rel,endpoint,graph);
            if (test) {
                ent_ent.write(pair.getValue() + "\n");
            } else {
                ent_lit.write(pair.getValue() + "\n");
            }

        }
        ent_ent.close();
        ent_lit.close();
    }

    /**
     * check if the object is an entity*
     */
    public static boolean isObjectAnEntity(String rel, String endpoint, String graph) throws Exception {
        int count = 0;
        int entitiesCount = 0;
       
        String query ="SELECT ?x ?y where { graph <"+graph+"> { ?x  "+rel+" ?y. } } limit 10";
       
        ResultSet result = queryVirtuoso(endpoint, query);

        while (result.hasNext()) {
            QuerySolution soln = result.nextSolution();
            RDFNode object = soln.get("?y");
            if (object != null) {
                count++;
                if (!object.isLiteral()) {
                    entitiesCount++;
                }
            }
        }
    //    c.close();

        if (count > 0 && ((float) entitiesCount) / count > 0.5) {
            return true;
        }
        return false;
    }
    
    public static ResultSet queryVirtuoso(String endpoint, String querystr){
        QueryEngineHTTP query = new QueryEngineHTTP(endpoint, querystr);

            ResultSet rst = query.execSelect();
            
            return rst;
    }

    public static void main(String[] args) {
        
        String graph = "freebase"; //dbpedia //freebase
        String endpoint= "http://s6.adam.uvsq.fr:8890/sparql";  //we use this from the lab
           
        String dir ="/Users/mary/Dropbox/Mary_SOFYA_code et al/statistics/freebase/";
        String fileWithRelations = "freebase_functionality.txt";
       

       
            HashMap<String, String> relations;
			try {
				relations = loadRelations(dir+fileWithRelations);
				 separateRelations(relations, dir+fileWithRelations, endpoint, graph);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            
            
           
            
            
        

    }

}
