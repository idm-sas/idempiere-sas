package id.co.databiz.awn.callout;

import java.math.BigDecimal;
import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.I_C_InvoiceLine;
import org.compiere.model.MInOutLine;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MOrderLine;
import org.compiere.model.MRMALine;
import org.compiere.model.Query;


public class CalloutRMALineInOutLine implements IColumnCallout{

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value, Object oldValue) {
		Integer M_InOutLine_ID = (Integer) value;
		if (M_InOutLine_ID == null || M_InOutLine_ID.intValue() == 0)
			return "";

		MInOutLine iol = new MInOutLine(ctx, M_InOutLine_ID, null);

		int invoiceLine_ID = new Query(ctx, I_C_InvoiceLine.Table_Name,
				"M_InOutLine_ID=?", null).setParameters(M_InOutLine_ID)
				.firstId();
		if (invoiceLine_ID <= 0)
			invoiceLine_ID = 0;

		if (invoiceLine_ID != 0) 
		{
			MInvoiceLine invoiceLine = new MInvoiceLine(ctx, invoiceLine_ID, null);
			if (invoiceLine.getM_Product_ID() != 0) {
				mTab.setValue(MRMALine.COLUMNNAME_M_Product_ID, invoiceLine.getM_Product_ID());
				mTab.setValue(MRMALine.COLUMNNAME_C_Charge_ID, null);
			}
			if (invoiceLine.getC_Charge_ID() != 0) {
				mTab.setValue(MRMALine.COLUMNNAME_C_Charge_ID, invoiceLine.getC_Charge_ID());
				mTab.setValue(MRMALine.COLUMNNAME_M_Product_ID, null);
			}
			mTab.setValue(MRMALine.COLUMNNAME_Qty, invoiceLine.getQtyInvoiced());
			mTab.setValue(MRMALine.COLUMNNAME_Amt, invoiceLine.getPriceActual());
			mTab.setValue(MRMALine.COLUMNNAME_C_Tax_ID, invoiceLine.getC_Tax_ID());
			
			BigDecimal lineNetAmt = invoiceLine.getQtyInvoiced().multiply(invoiceLine.getPriceActual());
			int precision = invoiceLine.getPrecision();
			if (lineNetAmt.scale() > precision)
				lineNetAmt = lineNetAmt.setScale(precision, BigDecimal.ROUND_HALF_UP);
			mTab.setValue(MRMALine.COLUMNNAME_LineNetAmt, lineNetAmt);
		} 
		else if (iol.getC_OrderLine_ID() != 0) 
		{
			MOrderLine orderLine = new MOrderLine(ctx, iol.getC_OrderLine_ID(), null);
			if (orderLine.getM_Product_ID() != 0) {
				mTab.setValue(MRMALine.COLUMNNAME_M_Product_ID, orderLine.getM_Product_ID());
			    mTab.setValue(MRMALine.COLUMNNAME_C_Charge_ID, null);
			}
			if (orderLine.getC_Charge_ID() != 0) {
				mTab.setValue(MRMALine.COLUMNNAME_C_Charge_ID, orderLine.getC_Charge_ID());
				mTab.setValue(MRMALine.COLUMNNAME_M_Product_ID, null);
			}
			mTab.setValue(MRMALine.COLUMNNAME_Qty, orderLine.getQtyOrdered());
			mTab.setValue(MRMALine.COLUMNNAME_Amt, orderLine.getPriceActual());
			mTab.setValue(MRMALine.COLUMNNAME_C_Tax_ID, orderLine.getC_Tax_ID());
			
			BigDecimal lineNetAmt = orderLine.getQtyOrdered().multiply(orderLine.getPriceActual());
			int precision = orderLine.getPrecision();
			if (lineNetAmt.scale() > precision)
				lineNetAmt = lineNetAmt.setScale(precision, BigDecimal.ROUND_HALF_UP);
			mTab.setValue(MRMALine.COLUMNNAME_LineNetAmt, lineNetAmt);
		}

		return "";
	}

}
