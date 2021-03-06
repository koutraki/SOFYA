package gold;

import java.text.DecimalFormat;
import java.util.Comparator;

public class Relation {

	    public static final DecimalFormat df = new DecimalFormat("0.000");
	
		public final String uri;
		public final boolean isDirect;
		
		public double funct;
		public double invFunct; 
		
		public double tupleNo;

		
		public  Relation(String uri, boolean isDirect, double funct, double invFunct) throws Exception {
			this.uri=uri;
			this.isDirect=isDirect;
			this.funct=funct;
			this.invFunct=invFunct;
		}
		
		public  Relation(String uri, boolean isDirect, double funct, double invFunct, double tupleNo) throws Exception {
			this(uri, isDirect, funct, invFunct);
			this.tupleNo=tupleNo;
		}
		
		public  Relation(String uri, boolean isDirect) throws Exception {
			this.uri=uri;
			this.isDirect=isDirect;
		}
		
		public  Relation(String uri, boolean isDirect, int tupleNo) throws Exception {
			this.uri=uri;
			this.isDirect=isDirect;
			this.tupleNo=tupleNo;
		}
		
		public static final Relation getRelationFromStringDesc(String name) throws Exception{
			return name.endsWith("-")?new Relation(name.substring(0, name.length()-1), false): new Relation(name, true);
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
			return uri+((isDirect)?"":"-");
		}
		
		public String toStringFull(){
			return uri+((isDirect)?"":"-")+"  funct="+df.format(funct)+" invFunct="+df.format(invFunct)+" noTuples="+((int)tupleNo);
		}
		
		public String toStringShort(KB kb){
			String shortRS=((uri.startsWith(kb.name+":"))?uri: uri.replaceFirst( kb.resourcesDomain, kb.name+":"))+(isDirect?"":"-");	
			return shortRS;
		}
		
		public static final class RelationCompBasedOnTupleNo implements Comparator<Relation> {
			@Override
			public int compare(Relation o1, Relation o2) {
				return (int) (o1.tupleNo-o2.tupleNo);
			}
		}
		
		public static final class RelationCompBasedOnTupleNoDesc implements Comparator<Relation> {
			@Override
			public int compare(Relation o1, Relation o2) {
				return (int) (-(o1.tupleNo-o2.tupleNo));
			}
		}
		
		public static final class RelationCompStringBased implements Comparator<Relation> {
			@Override
			public int compare(Relation o1, Relation o2) {
				int v=o1.uri.compareTo(o2.uri);
				if(v!=0) return v;
				
				if(o1.isDirect) return -1;
				if(!o1.isDirect) return 1;
				return 0;
			}
		}
}
