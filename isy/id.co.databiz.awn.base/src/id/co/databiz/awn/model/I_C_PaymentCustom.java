package id.co.databiz.awn.model;

import java.sql.Timestamp;

public interface I_C_PaymentCustom {
	public int getZ_WFScenario_ID();
	public void setZ_WFScenario_ID(int Z_WFScenario_ID);
	public void setEftValutaDate (Timestamp eftValutaDate);
	public Timestamp getEftValutaDate ();
	
	public void setIsAllocateLater (boolean IsAllocateLater);
	public boolean isAllocateLater();
}
