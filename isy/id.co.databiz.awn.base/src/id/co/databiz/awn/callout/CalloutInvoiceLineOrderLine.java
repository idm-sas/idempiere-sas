package id.co.databiz.awn.callout;

import java.math.BigDecimal;
import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MOrderLine;

public class CalloutInvoiceLineOrderLine implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {
		if(value==null){
			return "";
		}
		Integer orderLineID = (Integer) value;
		if(orderLineID>0){
			// AWN-30 AWN-32 AWN-109
			MOrderLine orderLine = new MOrderLine(ctx, orderLineID, null);
			BigDecimal qtyToInvoice = orderLine.getQtyOrdered().subtract(orderLine.getQtyInvoiced());
			BigDecimal qtyEntered = qtyToInvoice;
			if (orderLine.getQtyEntered().compareTo(orderLine.getQtyOrdered()) != 0)
				qtyEntered = qtyToInvoice
					.multiply(orderLine.getQtyEntered())
					.divide(orderLine.getQtyOrdered(), 12, BigDecimal.ROUND_HALF_UP);
			mTab.setValue("QtyEntered", qtyEntered);
			mTab.setValue("QtyInvoiced", qtyToInvoice);
			
			mTab.setValue("Description", orderLine.getDescription());
			if(orderLine.getM_Product_ID()==0){
				mTab.setValue("C_Charge_ID", orderLine.getC_Charge_ID());				
			} else {
				mTab.setValue("M_Product_ID", orderLine.getM_Product_ID());
			}
			mTab.setValue("M_AttributeSetInstance_ID", orderLine.getM_AttributeSetInstance_ID());
			mTab.setValue("C_UOM_ID", orderLine.getC_UOM_ID());
			mTab.setValue("C_Tax_ID", orderLine.getC_Tax_ID());
			
			mTab.setValue("PriceEntered", orderLine.getPriceEntered());
			mTab.setValue("PriceActual", orderLine.getPriceActual());
			mTab.setValue("PriceLimit", orderLine.getPriceLimit());
			mTab.setValue("PriceList", orderLine.getPriceList());	
			
			mTab.setValue("C_Project_ID", orderLine.getC_Project_ID());
			mTab.setValue("C_Activity_ID", orderLine.getC_Activity_ID());
			mTab.setValue("C_Campaign_ID", orderLine.getC_Campaign_ID());
			mTab.setValue("AD_OrgTrx_ID", orderLine.getAD_OrgTrx_ID());			
			mTab.setValue("User1_ID", orderLine.getUser1_ID());
			mTab.setValue("User2_ID", orderLine.getUser2_ID());
		}
		return "";
	}

}
