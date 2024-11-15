package id.co.databiz.awn.model;

import java.math.BigDecimal;

public interface I_M_RequisitionCustom {
	public boolean isReceipt();
	public void setIsReceipt(boolean isReceipt);
	public int getZ_WFScenario_ID();
	public void setZ_WFScenario_ID(int Z_WFScenario_ID);
	public int getC_Project_ID();
	public void setC_Project_ID(int C_Project_ID);
	public int getUser1_ID();
	public void setUser1_ID(int User1_ID);
	public int getC_Activity_ID();
	public void setC_Activity_ID(int C_Activity_ID);
	public int getC_Campaign_ID();
	public void setC_Campaign_ID(int C_Campaign_ID);
	public BigDecimal getGrandTotal();
	public void setGrandTotal(BigDecimal GrandTotal);
}
