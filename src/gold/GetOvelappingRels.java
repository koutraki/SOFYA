package gold;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;

public class GetOvelappingRels {

	/****************************************************************************/
	/** LOAD RELATIONS WITH FUNCT **/
	/****************************************************************************/
	public static final String separatorSpace = "\\s+";

	public static final ArrayList<Relation> loadRelationsFromFilesWithFunctionality(String file, boolean skipFirstLine,
			int columnRelation, int columnDirectFunc, int columnInvFunc) throws Exception {

		File f = new File(file);
		if (!f.exists()) {
			System.err.println("  File does not exit " + file);
			return null;
		} else {
			/** System.out.println(" Load : "+file); **/
		}

		BufferedReader pairReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
		if (skipFirstLine)
			pairReader.readLine();

		ArrayList<Relation> list = new ArrayList<Relation>();
		;
		String line = null;
		while ((line = pairReader.readLine()) != null) {
			if (line.isEmpty())
				continue;
			String[] e = line.split(separatorSpace);
			String relation = e[columnRelation];

			double functOfDirect = Double.valueOf(e[columnDirectFunc]);
			double functOfInverse = Double.valueOf(e[columnInvFunc]);

			Relation r = (functOfDirect >= functOfInverse) ? new Relation(relation, true, functOfDirect, functOfInverse)
					: new Relation(relation, false, functOfInverse, functOfDirect);

			System.out.println(relation + " " + functOfDirect + " " + functOfInverse);

			if (!list.contains(relation)) {
				list.add(r);
			}
		}
		pairReader.close();
		return list;
	}

	/****************************************************************************/

	/** EXTRACT ALL PAIRS R(X,Y) FOR A GIVEN R **/
	/****************************************************************************/
	public static final void twoStepExtraction(Relation r, KB kb, String prefixTarget, String fileWithSubjects,
			String fileWithPairs) throws Exception {
		// extractSubjects(r, kb, prefixTarget, fileWithSubjects);
		extractPairs(r, kb, prefixTarget, fileWithPairs);
		// extractPairsForSubjects(r, kb, fileWithSubjects, fileWithPairs);
	}

	public static final void extractPairs(Relation r, KB kb, String prefixTarget, String fileWithPairs)
			throws Exception {

		System.out.println("Extract pairs for " + r);

		String querystr = "  PREFIX owl: <http://www.w3.org/2002/07/owl#>  "
				+ "\n select distinct ?x2 ?y2 where {graph <" + kb.name + "> {\n";
		querystr += (r.isDirect) ? "?x  <" + r.uri + "> ?y. \n" : "?y <" + r.uri + "> ?x. \n";
		querystr += "?x  owl:sameAs ?x2. \n";
		querystr += "FILTER ( strstarts(str(?x2), \"" + prefixTarget + "\") ).\n";
		querystr += "?y  owl:sameAs ?y2. \n";
		querystr += "FILTER ( strstarts(str(?y2), \"" + prefixTarget + "\") ).\n";
		querystr += "   } } ";

		// System.out.println(querystr);

		BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(fileWithPairs), "UTF-8"));

		QueryEngineHTTP query = new QueryEngineHTTP(kb.endpoint, querystr);

		ResultSet rst = query.execSelect();
		if (rst != null) {
			while (rst.hasNext()) {
				QuerySolution qs = rst.next();

				RDFNode x2 = (RDFNode) qs.get("?x2");

				RDFNode y2 = (RDFNode) qs.get("?y2");

				// System.out.println(x2);
				writer.write(x2 + "\t" + y2 + " \n");
			}

			writer.close();

			System.out.println("				.... finished extracting pairs for " + r);
		}else{
			System.err.println("				.... No results for " + r);
		}
	}

	public static void main(String[] args) throws Exception {

		// String
		// dir="/Users/mary/Dropbox/feb-sofya/";//"/Users/adi/Dropbox/DBP/feb-sofya/dbpedia/";
		String dir = "feb-sofya/";
		String tmpDir = "tmpDir";// "/Users/adi/Dropbox/DBP/";

		KB source = new KB("dbpedia", "http://s6.adam.uvsq.fr:8891/sparql", "http://dbpedia.org");
		KB target = new KB("yago", "http://s6.adam.uvsq.fr:8891/sparql", "http://yago-knowledge.org");

		String fileWithRelations = dir + source.name + "/" + source.name + "_functionality_ee.txt";
		ArrayList<Relation> relations = loadRelationsFromFilesWithFunctionality(fileWithRelations, true, 0, 4, 5);

		String fileWithSubjects = tmpDir + "subjects.txt";
		String fileWithPairs = tmpDir + "pairs.txt";
		int tuplesPerQuery = 500;
		for (Relation r : relations) {
			System.out.println(r);
			twoStepExtraction(r, source, target.resourcesDomain, fileWithSubjects, fileWithPairs);

		}

	}
}
