package gold;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class IteratorFromFile {
	
	BufferedReader reader=null;

	public void init(String file) throws Exception{
		reader = new BufferedReader(new InputStreamReader(new FileInputStream(file),"UTF8"));
	}
	
	public final ArrayList<String> getNextLines(int max) throws Exception{
		ArrayList<String> list=new   ArrayList<String>();;
		String line=null;
		int i=0;
		while (i<max && (line = reader.readLine()) != null) {
				i++;
				if(line.isEmpty()) continue;
				list.add(line); 
		}
		
		if(list.size()==0){
			reader.close();
			return null;  
		}
		return list;
	}
	
	public void close()throws Exception{
		if(reader!=null) reader.close();
		reader=null;
		return;
	}
	
}
