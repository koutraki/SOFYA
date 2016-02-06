package virtuoso;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;

public class QueryVirtuosoBasic {

	public static void queryVirtuoso() {
		try {
			String graph = "yago"; // dbpedia //freebase
			String querystr = "select * where {graph <" + graph + "> {?s ?p ?o}} LIMIT 10";

			/*
			 * If we run our code from the lab's network we can run directly
			 * from eclipse/netbeans and the endpoint we use is:
			 * http://s6.adam.uvsq.fr:8890/sparql
			 */
			/*
			 * If we want to run our code from home then we have to transfer our
			 * .jar and the endpoint we use is: http://localhost:8890/sparql
			 */
			String endpoint = "http://s6.adam.uvsq.fr:8891/sparql";
			// "http://s6.adam.uvsq.fr:8890/sparql";

			QueryEngineHTTP query = new QueryEngineHTTP(endpoint, querystr);

			ResultSet rst = query.execSelect();
			while (rst.hasNext()) {
				QuerySolution qs = rst.next();

				RDFNode resource_node = (RDFNode) qs.get("?s");
				RDFNode predicate_node = (RDFNode) qs.get("?p");
				RDFNode object_node = (RDFNode) qs.get("?o");

				System.out.println(resource_node + "\t" + predicate_node + "\t" + object_node);

				// TODO: Do something here
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	public static void main(String[] args) {
		// TODO code application logic here

		queryVirtuoso();
	}

}
