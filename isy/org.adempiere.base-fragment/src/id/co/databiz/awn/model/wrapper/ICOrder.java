
package id.co.databiz.awn.model.wrapper;

import java.math.BigDecimal;


public interface ICOrder 
{
	public int getZ_WFScenario_ID();
	public void setZ_WFScenario_ID(int Z_WFScenario_ID);
	
	public int getZ_WFScenarioGrandtotal_ID();
	public void setZ_WFScenarioGrandtotal_ID(int Z_WFScenario_ID);
	public int getZ_WFScenarioCredit_ID();
	public void setZ_WFScenarioCredit_ID(int Z_WFScenario_ID);
	public int getZ_WFScenarioAging_ID();
	public void setZ_WFScenarioAging_ID(int Z_WFScenario_ID);
	public int getZ_WFScenarioPrice_ID();
	public void setZ_WFScenarioPrice_ID(int Z_WFScenario_ID);
	
	public BigDecimal getSO_CreditOver();
	public void setSO_CreditOver(BigDecimal SO_CreditOver);
	public BigDecimal getSO_AgingOver();
	public void setSO_AgingOver(BigDecimal SO_AgingOver);
}
