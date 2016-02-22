package config;

import java.text.DecimalFormat;
import java.util.Comparator;

import preprocessing.ComputingPCAandCWA.AlignmentPCA_CWA;



public class Alignment {

	public static final DecimalFormat df = new DecimalFormat("0.00");
	
	public final String rS;
	public final String rT;
	
	
	public final int sharedXY;
	public final int originalSamples;
	public final int pcaDenominator;
	
	public final double pca;
	public final double cwa;

	
	public Alignment(String rS, String rT, int sharedXY, int originalSamples, int pcaDenominator){
		this.sharedXY=sharedXY;
		this.originalSamples=originalSamples;
		this.pcaDenominator=pcaDenominator;
		this.rS=rS;
		this.rT=rT;
		
		this.pca=((double)sharedXY)/pcaDenominator;
		this.cwa=((double)sharedXY)/originalSamples;
		
	}
	
	@Override
	public final String toString(){
		return  rS + " \t " + rT;
	}
	
	public final String toStringAll(){
				return  rS + " \t " + rT+ "\t"+ sharedXY+ "\t"+ originalSamples+ "\t"+ pcaDenominator+ "\t"+ df.format(pca)+ "\t" +df.format(cwa);
	}
	
	public final String toStringWithShared(){
		return  rS + " \t " + rT+ "\t"+ sharedXY;

  }
	
	@Override
	public boolean equals(Object o){
		if(! (o instanceof Alignment)) return false;
		Alignment other=(Alignment) o;
		if(other.rS.equals(rS) && other.rT.equals(rT)) return true;
		return false;
	}
	
	@Override
	public int hashCode(){
		return (rS+" "+rT).hashCode();
	}
	
	public static final class Comp_Alignment_Shared_Based implements Comparator<Alignment> {
		@Override
		public int compare(Alignment o1, Alignment o2) {
			 int diff=-(o1.sharedXY-o2.sharedXY);
			 if(diff!=0) return diff;
			 return -(o1.pcaDenominator-o2.pcaDenominator);
		}

	}
}
