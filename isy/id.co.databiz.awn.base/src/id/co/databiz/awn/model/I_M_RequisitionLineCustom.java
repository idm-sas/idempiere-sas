package id.co.databiz.awn.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

public interface I_M_RequisitionLineCustom {
	public void setDateRequired (Timestamp DateRequired);
	public Timestamp getDateRequired ();
	
	public int getC_Project_ID();
	public void setC_Project_ID(int projectID);
	
	public int getUser1_ID();
	public void setUser1_ID(int user1ID);
	
	public BigDecimal getQtyEntered();
	public void setQtyEntered(BigDecimal qtyEntered);
	
	public int getC_UOM_ID();
	public void setC_UOM_ID(int uomID);
}
