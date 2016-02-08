package gold;

import java.text.DecimalFormat;

public class Relation {

	    public static final DecimalFormat df = new DecimalFormat("0.000");
	
		public final String uri;
		public final boolean isDirect;
		
		public double funct;
		public double invFunct; 

		
		public  Relation(String uri, boolean isDirect, double funct, double invFunct) throws Exception {
			this.uri=uri;
			this.isDirect=isDirect;
			this.funct=funct;
			this.invFunct=invFunct;
		}
		
		@Override
		public boolean equals(Object o){
			Relation r=(Relation)o;
			return uri.equalsIgnoreCase(r.uri) && r.isDirect==isDirect;
		}
		

		@Override
		public int hashCode(){
			return (uri+isDirect).hashCode();
			
		}

		
		public String toString(){
			return uri+((isDirect)?"":"-")+"  funct="+df.format(funct)+" invFunct="+df.format(invFunct);
		}
}
