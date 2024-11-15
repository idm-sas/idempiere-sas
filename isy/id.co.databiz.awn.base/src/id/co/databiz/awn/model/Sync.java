package id.co.databiz.awn.model;

import java.math.BigDecimal;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MProduct;
import org.compiere.model.MUOM;
import org.compiere.util.DB;
import org.compiere.util.Env;


public class Sync {
	public static final String REV = "1466";
	
	public static final String P_LOGCOMMENT = "LOGCOMMMENT";
	
	public static final int DEFAULT_COUNTRY = 209; // INDONESIA
	public static final int DEFAULT_CURRENCY = 303; // IDR
	
	public static int COLUMN_COST_CENTER = 200118;
	public static int COLUMN_TAX_INVOICE = 200027;
	public static int COLUMN_KWITANSI = 200465;
	
	public static int PROCESS_SHIPMENT_RETURN_GENERATE_INVOICES = 200034;
	public static int PROCESS_SHIPMENT_GENERATE_INVOICES = 200035;
	
	public static int REFERENCE_TAX_INVOICE = 200000;	
	
	public static int GL_CATEGORY_Manual = 1000002;
	public static int GL_CATEGORY_VarAdj  = 900007;
	
	public static int DOCTYPE_GL_Journal = 1000000;
	public static int DOCTYPE_GL_VarAdj = 900011;
	public static int DOCTYPE_MM_Shipment = 1000011;
	
	public static int DOCTYPE_CC_MaterialReceipt = 900001;
	public static int DOCTYPE_CC_ComponentIssue = 900002;
	public static int DOCTYPE_CC_UsegeVariance = 900003;
	public static int DOCTYPE_CC_MethodChangeVariance = 900004;
	public static int DOCTYPE_CC_RateVariance = 900005;
	public static int DOCTYPE_CC_MixVariance = 900006;
	public static int DOCTYPE_CC_ActivityControl = 900007;
	
	public static int COST_ELEMENT_STANDARD_COSTING = 1000000;
	
	public final static int TAX_PPN = 1000001;
	public final static int TAX_PPN_EXEMPT = 1000009;
	public final static int TAX_PPN_PLUS_MINUS = 1000006;
	public final static int TAX_PPN_PLUS = 1000007;
	public final static int TAX_PPN_MINUS = 1000008;
	
	public final static int TAX_CATEGORY_PPN = 1000000;
	public final static int TAX_CATEGORY_TAX_EXEMPT = 1000004;
	
	public final static int PAYMENT_TERM_IMMEDIATE = 1000000;
	
	public static final String SYS_Invoice_ReverseUseNewNumber = "Invoice_ReverseUseNewNumber";
	public static final String SYS_StandardCostingUseCurrentCost = "SYNC_StandardCostingUseCurrentCost";
	public static final String SYS_CostCollectorStandardCostingUseCurrentCost = 
			"SYNC_CostCollectorStandardCostingUseCurrentCost";
	public static final String SYS_Acct_WIPVarianceAllocation = "SYNC_Acct_WIPVarianceAllocation";
	public static final String SYS_Acct_FGVarianceAllocation = "SYNC_Acct_FGVarianceAllocation";
	public static final String SYS_Acct_RMVarianceAllocation = "SYNC_Acct_RMVarianceAllocation";
	
	public static BigDecimal getQtyBasic(int M_Product_ID, int C_UOMEntered_ID, BigDecimal QtyEntered) {
		BigDecimal qty = QtyEntered;
		MProduct product = MProduct.get(Env.getCtx(), M_Product_ID);
		if (product != null && product.getC_UOM_ID() != C_UOMEntered_ID) {
			BigDecimal divideRate = DB.getSQLValueBD(null, 
					"SELECT divideRate FROM C_UOM_Conversion WHERE M_Product_ID = ? AND C_UOM_ID = ? AND C_UOM_To_ID = ?", 
					M_Product_ID,product.getC_UOM_ID(),C_UOMEntered_ID);
			if(divideRate!=null){
				qty = qty.multiply(divideRate);
			} else {
				throw new AdempiereException("No UOM conversion found");
			}
			qty = qty.setScale(MUOM.getPrecision(Env.getCtx(), product.getC_UOM_ID()), BigDecimal.ROUND_HALF_UP);
		}
		return qty;
	}
	
	public static BigDecimal getQtyEntered(int M_Product_ID, int C_UOMEntered_ID, BigDecimal QtyBasic) {
		BigDecimal qty = QtyBasic;
		MProduct product = MProduct.get(Env.getCtx(), M_Product_ID);
		if (product != null && product.getC_UOM_ID() != C_UOMEntered_ID) {
			BigDecimal multiplyRate = DB.getSQLValueBD(null, 
					"SELECT MultiplyRate FROM C_UOM_Conversion WHERE M_Product_ID = ? AND C_UOM_ID = ? AND C_UOM_To_ID = ?", 
					M_Product_ID,MProduct.get(Env.getCtx(), M_Product_ID).getC_UOM_ID(),C_UOMEntered_ID);
			if(multiplyRate!=null){
				qty = qty.multiply(multiplyRate);
			} else {
				throw new AdempiereException("No UOM conversion found");
			}
			qty = qty.setScale(MUOM.getPrecision(Env.getCtx(), C_UOMEntered_ID), BigDecimal.ROUND_HALF_UP);
		}
		return qty;
	}
}
