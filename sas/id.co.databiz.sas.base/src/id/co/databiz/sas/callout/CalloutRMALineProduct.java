package id.co.databiz.sas.callout;

import id.co.databiz.sas.SASSystemID;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.adempiere.base.IColumnCallout;
import org.adempiere.exceptions.DBException;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MInOutLine;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MRMA;
import org.compiere.model.MRMALine;
import org.compiere.util.DB;
import org.compiere.util.Env;

public class CalloutRMALineProduct implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {
		if(value == null){
			return "";
		}
		Integer productID = (Integer) value;
		int bpID = Env.getContextAsInt(ctx, WindowNo, "C_BPartner_ID");
		int rmaID = Env.getContextAsInt(ctx, WindowNo, "M_RMA_ID");
		int docTypeID = Env.getContextAsInt(ctx, WindowNo, "C_DocType_ID");
		int orgTrxID = Env.getContextAsInt(ctx, WindowNo, "AD_OrgTrx_ID");
		String soTrx = Env.getContext(ctx, WindowNo, "IsSOTrx");
		
        int inOutLineID = -1;
        int invoiceLineID = -1;
        
        List<Integer> invoiceDocTypeIDs = new ArrayList<>();

        if (docTypeID == SASSystemID.DOCTYPE_CUSTOMER_RMA_A) {	
        	invoiceDocTypeIDs.add(SASSystemID.DOCTYPE_AR_INVOICE_TAX);
        	invoiceDocTypeIDs.add(SASSystemID.DOCTYPE_AR_INVOICE_SF_TAX);
		} else if (docTypeID == SASSystemID.DOCTYPE_CUSTOMER_RMA_B) {			
			invoiceDocTypeIDs.add(SASSystemID.DOCTYPE_AR_INVOICE_NON_TAX);
			invoiceDocTypeIDs.add(SASSystemID.DOCTYPE_AR_INVOICE_SF_NON_TAX);
		} else if (docTypeID == SASSystemID.DOCTYPE_VENDOR_RMA) {			
			invoiceDocTypeIDs.add(SASSystemID.DOCTYPE_AP_INVOICE);
		}

		if(productID > 0 && bpID > 0 && docTypeID > 0 && orgTrxID > 0){
			String placeholders = invoiceDocTypeIDs.stream()
			        .map(i -> "?")
			        .collect(Collectors.joining(","));
			
			// SAS-21 look for amount in last invoice
			StringBuilder sqlIO = new StringBuilder();
            sqlIO.append("SELECT il.M_InOutLine_ID, ill.C_InvoiceLine_ID ");
            sqlIO.append("FROM M_InOutLine il ");
            sqlIO.append("INNER JOIN M_InOut i ON (i.M_InOut_ID = il.M_InOut_ID) ");
            sqlIO.append("INNER JOIN C_InvoiceLine ill ON (ill.M_InOutLine_ID = il.M_InOutLine_ID) ");
            sqlIO.append("INNER JOIN C_Invoice iv ON (iv.C_Invoice_ID = ill.C_Invoice_ID) ");
            sqlIO.append("WHERE i.DocStatus IN ('CO','CL') ");
            sqlIO.append("AND i.MovementType IN ('C-','V+') ");
            sqlIO.append("AND i.C_DocType_ID NOT IN (550292,550268,550295) ");
            sqlIO.append("AND i.IsSOTrx = ? ");
            sqlIO.append("AND i.C_BPartner_ID = ? ");
            sqlIO.append("AND il.M_Product_ID = ? ");
            sqlIO.append("AND i.AD_OrgTrx_ID = ? ");
            sqlIO.append("AND iv.C_DocType_ID IN (" + placeholders + ") ");
            sqlIO.append("ORDER BY iv.DateInvoiced DESC, iv.Created DESC ");
            sqlIO.append("FETCH FIRST 1 ROW ONLY");
			
			PreparedStatement pstmt = null;
			ResultSet rs = null;		
			
			try
			{
				List<Object> params = new ArrayList<>();

				params.add(soTrx);
				params.add(bpID);
				params.add(productID);
				params.add(orgTrxID);
				params.addAll(invoiceDocTypeIDs);
				
				pstmt = DB.prepareStatement(sqlIO.toString(), null);
				DB.setParameters(pstmt, params);
				rs = pstmt.executeQuery();
				if (rs.next())
				{
					inOutLineID = rs.getInt("M_InOutLine_ID");
					invoiceLineID = rs.getInt("C_InvoiceLine_ID");
					
					mTab.setValue("M_InOutLine_ID", inOutLineID);
					MInOutLine ioLine = new MInOutLine(ctx, inOutLineID, null);
					MRMA rma = new MRMA(ctx, rmaID, null);
					
					if (rma.getInOut_ID() != ioLine.getM_InOut_ID()) {
						DB.executeUpdateEx("UPDATE M_RMA SET InOut_ID = ? WHERE M_RMA_ID = ?",
								new Object[]{ioLine.getM_InOut_ID(),rma.get_ID()}, null);
					}
					
					MInvoiceLine invoiceLine = new MInvoiceLine(Env.getCtx(), invoiceLineID, null);
			        MInvoice invoice = MInvoice.get(Env.getCtx(), invoiceLine.getC_Invoice_ID());
			        int precision = invoice.getPrecision();
			        mTab.setValue("C_InvoiceLine_ID", invoiceLine.get_ID());
			        mTab.setValue(MRMALine.COLUMNNAME_Qty, Env.ONE);
			        mTab.setValue(MRMALine.COLUMNNAME_Amt, invoiceLine.getPriceActual());		
			        if (invoiceLine.getC_Tax_ID() != 0)
			            mTab.setValue(MRMALine.COLUMNNAME_C_Tax_ID, invoiceLine.getC_Tax_ID());
			        
			        BigDecimal lineNetAmt = Env.ONE.multiply(invoiceLine.getPriceActual());
			        if (lineNetAmt.scale() > precision)
			            lineNetAmt = lineNetAmt.setScale(precision, BigDecimal.ROUND_HALF_UP);
			        mTab.setValue(MRMALine.COLUMNNAME_LineNetAmt, lineNetAmt);
				}
			}
			catch (SQLException e)
			{
				throw new DBException(e, sqlIO.toString());
			}
			finally
			{
				DB.close(rs, pstmt);
				rs = null; pstmt = null;
			}
		}
		return "";
	}

}
