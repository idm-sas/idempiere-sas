package id.co.databiz.awn.model;

import java.sql.Timestamp;

public interface I_PP_OrderCustom {
	public int getZ_WFScenario_ID();
	public void setZ_WFScenario_ID(int Z_WFScenario_ID);
	
	public Timestamp getClosedDate();
	public void setClosedDate(Timestamp closedDate);
}