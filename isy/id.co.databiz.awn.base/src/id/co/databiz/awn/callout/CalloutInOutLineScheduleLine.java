package id.co.databiz.awn.callout;

import id.co.databiz.awn.model.MScheduleLine;

import java.math.BigDecimal;
import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MUOMConversion;
import org.compiere.util.DB;

public class CalloutInOutLineScheduleLine implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {
		if(value==null){
			return "";
		}
		Integer lineID = (Integer) value;
		if(lineID > 0){
			// ISY-40
			MScheduleLine line = new MScheduleLine(ctx, lineID, null);
			mTab.setValue("C_OrderLine_ID", line.getC_OrderLine_ID());
			mTab.setValue("M_Product_ID", line.getM_Product_ID());
			mTab.setValue("C_UOM_ID", line.getC_UOM_ID());
			
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT COALESCE(SUM(iol.QtyEntered),0) FROM M_InOutLine iol ");
			sql.append("INNER JOIN M_InOut io ON (io.M_InOut_ID = iol.M_InOut_ID) ");
			sql.append("WHERE io.DocStatus NOT IN ('VO','RE') ");
			sql.append("AND iol.Z_ScheduleLine_ID = ? ");
			BigDecimal qtyDelivered = DB.getSQLValueBD(null, sql.toString(), lineID);
			
			BigDecimal qtyEntered = line.getQtyEntered().subtract(qtyDelivered);
			mTab.setValue("QtyEntered", qtyEntered);
			
			BigDecimal movementQty = MUOMConversion.convertProductFrom (ctx, line.getM_Product_ID(),
					line.getC_UOM_ID(), qtyEntered);
			if (movementQty == null)
				movementQty = qtyEntered;
			mTab.setValue("MovementQty", movementQty);
		}
		return "";
	}

}
