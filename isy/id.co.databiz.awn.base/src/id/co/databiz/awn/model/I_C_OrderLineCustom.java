package id.co.databiz.awn.model;

import java.math.BigDecimal;

public interface I_C_OrderLineCustom {
	public int getM_RequisitionLine_ID();
	public void setM_RequisitionLine_ID(int requisitionLineID);
	
	public BigDecimal getWeight();
	public void setWeight(BigDecimal weight);
	
	public BigDecimal getCBMValue();
	public void setCBMValue(BigDecimal cbmValue);
}
