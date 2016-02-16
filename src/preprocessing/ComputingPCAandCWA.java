package preprocessing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

public class ComputingPCAandCWA {
	
	/*
	 * Class that computes pca and cwa form a file of form:
	 * subRelation superRelation NumeratorForBothPCAandCWA DenominatorForCWA DenominatorForPCA
	 * */
	
	public static void parseAlignmentFile(String pathToFile, String pathToNewFile) throws NumberFormatException, IOException{
		File f=new File(pathToFile);
		if(!f.exists()) {
			System.err.println("  File does not exit "+pathToFile);
			System.exit(0);
		}else {
			/** System.out.println(" Load : "+file);**/
		}
		
		BufferedReader alignReader = new BufferedReader(new InputStreamReader(new FileInputStream(pathToFile),"UTF8"));
		BufferedWriter pca_file=new BufferedWriter(new FileWriter(pathToNewFile));
		
		pca_file.write("subRelation \t superRelation \t NumeratorPcaCwa \t DenominatorCwa \t DenominatorPca \t PCA \t CWA \n");
		
		float pca;
		float cwa;
		

		String newLine;
		String line=null;
		while ((line = alignReader.readLine()) != null) {
				if(line.isEmpty()){ 
					pca_file.write("\n");
					continue;
				}
				String[] e = line.split("\\s+");
				
				
				pca = Float.parseFloat(e[2])  / Float.parseFloat(e[4]) ;
				cwa = Float.parseFloat(e[2])  / Float.parseFloat(e[3]) ;
				
				newLine = line + "\t" + Float.toString(pca) + "\t" +Float.toString(cwa)+"\n";
				
				pca_file.write(newLine);
				pca_file.flush();
				
				
		}
		alignReader.close();
		pca_file.close(); 
	}
	

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		
		String dirToFile = "/Users/mary/Dropbox/feb-sofya/_gold/yago->dbpedia/";
		String allignmentFile = "yago_dbpedia_align.txt";
		
		/*Output File name*/
		String fileWithPcaCwa = allignmentFile.split(".txt")[0]+"_pca_cwa.txt";
		
		try {
			parseAlignmentFile(dirToFile+allignmentFile, dirToFile+fileWithPcaCwa);
		} catch (NumberFormatException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
