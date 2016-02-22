package string;

public class SPARQLStringEncoding {

	public static final String getSPARQLEncodingForLiteral(String s){
	
		boolean hasDoubleQuotationMark=s.contains("\"");
		if(hasDoubleQuotationMark) {
			if(s.endsWith("'"))  s=s+" "; 
			if(s.startsWith("'")) s=" "+s;
			return "'''"+s+ "'''";
		}
		
		if(s.contains("\n")) return "'''"+s+ "'''";
		
		if(s.contains("'")){
			if(!hasDoubleQuotationMark) return "\""+s+ "\"";
			if(s.endsWith("'"))  s=s+" "; 
			if(s.startsWith("'")) s=" "+s;
			return "'''"+s+ "'''";
		}
		
		return  "\""+s+ "\"";
	}
	
	
}

