package id.co.databiz.awn.controller;

import id.co.databiz.awn.model.AWNSysConfig;
import id.co.databiz.awn.model.wrapper.ICInvoice;

import java.math.BigDecimal;
import java.util.List;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.model.POWrapper;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MSysConfig;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.util.Env;

public class InvoiceController {
	
	public static void beforeComplete(MInvoice invoice){
		List<MInvoiceLine> invoiceLines = new Query(Env.getCtx(), MInvoiceLine.Table_Name, "IsActive = 'Y' AND C_Invoice_ID = ?", null)
				.setParameters(new Object[]{invoice.get_ID()})
				.list();
		for(MInvoiceLine line : invoiceLines){
			// AWN-79
			if(line.getM_Product_ID()==0
			&& line.getC_Charge_ID()==0
			&& line.getLineNetAmt().compareTo(Env.ZERO)!=0
			&& !line.isDescription()){
				throw new IllegalStateException ("AWN-79\nInvalid line " + line.getLine() + " with no Product / Charge / Description Only.");
			}
		}
		
		if (MSysConfig.getValue(AWNSysConfig.ISY_VALIDATION_DP_INVOICE, "Y").equals("Y")) {
		// ISY-12
			StringBuffer sql = new StringBuffer();
			sql.append("SELECT COALESCE(SUM(il.QtyInvoiced),0) FROM C_InvoiceLine il ");
			sql.append("INNER JOIN C_Invoice i ON (i.C_Invoice_ID = il.C_Invoice_ID) ");
			sql.append("INNER JOIN M_Product p ON (p.M_Product_ID = il.M_Product_ID) ");
			sql.append("INNER JOIN M_Product_Category pc ON (pc.M_Product_Category_ID = p.M_Product_Category_ID) ");
			sql.append("WHERE (i.DocStatus IN ('CO','CL') OR i.C_Invoice_ID = ?) ");
			sql.append("AND i.C_Order_ID = ? ");
			sql.append("AND UPPER(pc.Name) LIKE '%DP%' ");
			BigDecimal invoicedDPQty = DB.getSQLValueBD(invoice.get_TrxName(), sql.toString(), invoice.get_ID(), invoice.getC_Order_ID());
			if (invoicedDPQty.compareTo(Env.ONEHUNDRED) > 0) {
				throw new IllegalStateException ("ISY-12\nOver DP: " + invoicedDPQty.setScale(0));
			}
		}	
	}
	
	public static void beforeReverse(MInvoice invoice){
		ICInvoice invoiceCustom = POWrapper.create(invoice, ICInvoice.class);
		if(invoiceCustom.getZ_TaxInvoice_ID()>0){
			throw new AdempiereException("ISY-33 Invoice cannot be voided because it has reference to Tax Invoice");
		}
	}
	
	public static void beforeChangeAndNew(MInvoice invoice){
		if(invoice.getC_DocType_ID() == 0){
			invoice.setC_DocType_ID(invoice.getC_DocTypeTarget_ID());
			invoice.saveEx();
		}
	}
}
