package id.co.databiz.sas.model;

import java.sql.ResultSet;

import org.adempiere.base.IModelFactory;
import org.compiere.model.PO;
import org.compiere.util.Env;

public class ModelFactory implements IModelFactory {

	@Override
	public Class<?> getClass(String tableName) {
		Class<?> clazz = null;
		if(tableName.equals(MBPRule.Table_Name)){
			clazz = MBPRule.class;
		} else if(tableName.equals(MDiscountSchemaBreakLine.Table_Name)){
			clazz = MDiscountSchemaBreakLine.class;
		} else if(tableName.equals(MOrder.Table_Name)){
			clazz = MOrder.class;
		} else if(tableName.equals(MInOut.Table_Name)){
			clazz = MInOut.class;
		} else if(tableName.equals(MRequestBundle.Table_Name)){
			clazz = MRequestBundle.class;
		} else if(tableName.equals(MRMALine.Table_Name)){
			clazz = MRMALine.class;
		} else if(tableName.equals(MRMA.Table_Name)){
			clazz = MRMA.class;
		} else if(tableName.equals(MInvoice.Table_Name)){
			clazz = MInvoice.class;
		} else if(tableName.equals(MPromotionBP.Table_Name)){
			clazz = MPromotionBP.class;
		} else if(tableName.equals(MInventoryLine.Table_Name)){
			clazz = MInventoryLine.class;
		} else if(tableName.equals(MTimeExpense.Table_Name)){
			clazz = MTimeExpense.class;
		} else if(tableName.equals(MOrderLine.Table_Name)){
			clazz = MOrderLine.class;
		}
		return clazz;
	}

	@Override
	public PO getPO(String tableName, int Record_ID, String trxName) {
		PO po = null;
		if(tableName.equals(MBPRule.Table_Name)){
			po = new MBPRule(Env.getCtx(), Record_ID, trxName);
		} else if(tableName.equals(MDiscountSchemaBreakLine.Table_Name)){
			po = new MDiscountSchemaBreakLine(Env.getCtx(), Record_ID, trxName);
		} else if(tableName.equals(MOrder.Table_Name)){
			po = new MOrder(Env.getCtx(), Record_ID, trxName);
		} else if(tableName.equals(MInOut.Table_Name)){
			po = new MInOut(Env.getCtx(), Record_ID, trxName);
		} else if(tableName.equals(MRequestBundle.Table_Name)){
			po = new MRequestBundle(Env.getCtx(), Record_ID, trxName);
		} else if(tableName.equals(MRMALine.Table_Name)){
			po = new MRMALine(Env.getCtx(), Record_ID, trxName);
		} else if(tableName.equals(MRMA.Table_Name)){
			po = new MRMA(Env.getCtx(), Record_ID, trxName);
		} else if(tableName.equals(MInvoice.Table_Name)){
			po = new MInvoice(Env.getCtx(), Record_ID, trxName);
		} else if(tableName.equals(MPromotionBP.Table_Name)){
			po = new MPromotionBP(Env.getCtx(), Record_ID, trxName);
		} else if(tableName.equals(MInventoryLine.Table_Name)){
			po = new MInventoryLine(Env.getCtx(), Record_ID, trxName);
		} else if(tableName.equals(MTimeExpense.Table_Name)){
			po = new MTimeExpense(Env.getCtx(), Record_ID, trxName);
		} else if(tableName.equals(MOrderLine.Table_Name)){
			po = new MOrderLine(Env.getCtx(), Record_ID, trxName);
		}
		return po;
	}

	@Override
	public PO getPO(String tableName, ResultSet rs, String trxName) {
		PO po = null;
		if(tableName.equals(MBPRule.Table_Name)){
			po = new MBPRule(Env.getCtx(), rs, trxName);
		} else if(tableName.equals(MDiscountSchemaBreakLine.Table_Name)){
			po = new MDiscountSchemaBreakLine(Env.getCtx(), rs, trxName);
		} else if(tableName.equals(MOrder.Table_Name)){
			po = new MOrder(Env.getCtx(), rs, trxName);
		} else if(tableName.equals(MInOut.Table_Name)){
			po = new MInOut(Env.getCtx(), rs, trxName);
		} else if(tableName.equals(MRequestBundle.Table_Name)){
			po = new MRequestBundle(Env.getCtx(), rs, trxName);
		} else if(tableName.equals(MRMALine.Table_Name)){
			po = new MRMALine(Env.getCtx(), rs, trxName);
		} else if(tableName.equals(MRMA.Table_Name)){
			po = new MRMA(Env.getCtx(), rs, trxName);
		} else if(tableName.equals(MInvoice.Table_Name)){
			po = new MInvoice(Env.getCtx(), rs, trxName);
		} else if(tableName.equals(MPromotionBP.Table_Name)){
			po = new MPromotionBP(Env.getCtx(), rs, trxName);
		} else if(tableName.equals(MInventoryLine.Table_Name)){
			po = new MInventoryLine(Env.getCtx(), rs, trxName);
		} else if(tableName.equals(MTimeExpense.Table_Name)){
			po = new MTimeExpense(Env.getCtx(), rs, trxName);
		} else if(tableName.equals(MOrderLine.Table_Name)){
			po = new MOrderLine(Env.getCtx(), rs, trxName);
		}
		return po;
	}
}
