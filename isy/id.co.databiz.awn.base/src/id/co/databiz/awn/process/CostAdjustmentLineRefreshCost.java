package id.co.databiz.awn.process;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.exceptions.FillMandatoryException;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MCost;
import org.compiere.model.MCostElement;
import org.compiere.model.MDocType;
import org.compiere.model.MInventory;
import org.compiere.model.MInventoryLine;
import org.compiere.model.MProduct;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;

public class CostAdjustmentLineRefreshCost extends SvrProcess{

	private int	p_M_InventoryLine_ID = 0;
	private MAcctSchema as = null;
	
	@Override
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else if (name.equals("M_InventoryLine_ID"))
				p_M_InventoryLine_ID = para[i].getParameterAsInt();
			else
				log.log(Level.SEVERE, "prepare - Unknown Parameter: " + name);
		}
		
		if(p_M_InventoryLine_ID <= 0) {
			p_M_InventoryLine_ID = getRecord_ID();
		}
		
	}

	@Override
	protected String doIt() throws Exception {
		if (log.isLoggable(Level.INFO)) log.info("doIt - M_InventoryLine_ID=" + p_M_InventoryLine_ID);
		
		if (p_M_InventoryLine_ID <= 0) {
			throw new FillMandatoryException("M_InventoryLine_ID");
		}
		
		String sql = "SELECT M_Inventory_ID FROM M_InventoryLine WHERE M_InventoryLine_ID = ?";
		int M_Inventory_ID = DB.getSQLValue(get_TrxName(), sql, p_M_InventoryLine_ID);
		
		MInventory inventory = new MInventory(Env.getCtx(), M_Inventory_ID, get_TrxName());
		MDocType dt = MDocType.get(Env.getCtx(), inventory.getC_DocType_ID());
		String parentDocSubTypeInv = dt.getDocSubTypeInv();
		String docCostingMethod = inventory.getCostingMethod();
		if (MDocType.DOCSUBTYPEINV_CostAdjustment.equals(parentDocSubTypeInv)) {
			List<MInventoryLine> lines = Arrays.asList(inventory.getLines(true));
			for (MInventoryLine line : lines) {
				MProduct product = MProduct.get(Env.getCtx(), line.getM_Product_ID());
				String costingLevel = product.getCostingLevel(getAcctSchema());
				int orgId = line.getAD_Org_ID();
				int asiId = line.getM_AttributeSetInstance_ID();
				if (MAcctSchema.COSTINGLEVEL_Client.equals(costingLevel))
				{
					orgId = 0;
					asiId = 0;
				}
				else if (MAcctSchema.COSTINGLEVEL_Organization.equals(costingLevel))
					asiId = 0;
				else if (MAcctSchema.COSTINGLEVEL_BatchLot.equals(costingLevel))
					orgId = 0;
				MCostElement ce = MCostElement.getMaterialCostElement(Env.getCtx(), docCostingMethod, orgId);
				MCost cost = MCost.get(product, asiId, getAcctSchema(), 
						orgId, ce.getM_CostElement_ID(), get_TrxName());					
				DB.getDatabase().forUpdate(cost, 120);
				BigDecimal currentQty = cost.getCurrentQty();
				line.set_ValueOfColumn("QtyCostAdjustment", currentQty);
				line.saveEx();
			}
		}
		
		return "OK..";
	}
	
	public MAcctSchema getAcctSchema() {
		if (as == null) {
			int asID = Env.getContextAsInt(Env.getCtx(), "$C_AcctSchema_ID");
			as = MAcctSchema.get(Env.getCtx(), asID);
		}
		return as;
	}

}
