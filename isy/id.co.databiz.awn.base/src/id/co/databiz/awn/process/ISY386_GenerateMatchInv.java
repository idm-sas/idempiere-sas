package id.co.databiz.awn.process;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.logging.Level;

import org.compiere.model.MDocType;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MMatchInv;
import org.compiere.model.MPeriod;
import org.compiere.model.MPeriodControl;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.DB;

/**
 * 	ISY-386 Generate Match invoice
 * 	https://databiz.atlassian.net/browse/ISY-386
 *  @author yg
 */
public class ISY386_GenerateMatchInv extends SvrProcess {

	private int p_C_Period_ID=0;
	private int matchInv=0;

	@Override
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else if (name.equals("C_Period_ID"))
				p_C_Period_ID = para[i].getParameterAsInt();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
	}

	@Override
	protected String doIt() throws Exception {
		
		MPeriod period = new MPeriod(getCtx(), p_C_Period_ID, get_TrxName());

		//		ISY-386
//		Proses hanya membentuk document matched invoice dengan kondisi : 
//		    Status Document Invoice (vendor) credit memo Complete/Close dan belum memiliki Mathced invoice atas Invoice Line ybs
//		    Period Open untuk doc base type Match Invoice (MXI)//
//		    Memiliki Vendor return atas ref RMA Line yg sama dengan  Invoice (vendor) Line
//		M_MatchInv.DateTrx = C_invoice.DateInvoiced
//		M_inoutLine.M_product_id = C_InvoiceLine.M_Product_Id dan M_inoutline.movementqty = C_invoiceline.QtyInvoiced
		
		//cek period open
		MPeriodControl percon = period.getPeriodControl(MDocType.DOCBASETYPE_MatchInvoice);
		Boolean isPeriodOpen = percon.isOpen();
		if (!isPeriodOpen){
			throw new AdempiereUserError("Period is not open for DocBaseType Match Invoice");
		}
		
		//	#1	C_InvoiceLine_ID
		//	#2	M_InOutLine_ID
		//	#3	DateInvoiced
		//	#4	QtyInvoiced
		String sql = 
				"with il as ( "
				+ "select il.c_invoiceline_id , il.m_inoutline_id , il.m_rmaline_id , i.dateinvoiced , il.qtyinvoiced "
				+ "from c_invoiceline il  "
				+ "join c_invoice i on i.c_invoice_id = il.c_invoice_id and i.issotrx='N' and i.docstatus in ('CO','CL') and i.processed ='Y' " //processed and complete only 
				+ "join c_doctype dt on dt.c_doctype_id = i.c_doctype_id and dt.docbasetype ='APC' " //ap credit memo 
				+ "left join m_matchinv mi on mi.c_invoiceline_id = il.c_invoiceline_id  "
				+ "where mi.m_matchinv_id is null " // matchinv not found fo the invoiceline 
				+ "and i.dateinvoiced between "+DB.TO_DATE(period.getStartDate(), true)+" and "+DB.TO_DATE(period.getEndDate(), true)
				+ ") "
				+ "select il.c_invoiceline_id , iol.m_inoutline_id , il.dateinvoiced, il.qtyinvoiced  "
				+ "from il "
				+ "join m_inoutline iol on iol.m_inoutline_id = il.m_inoutline_id  "
				+ "	and iol.m_rmaline_id = il.m_rmaline_id " // il dan iol merujuk ke rmaline yg sama 
				+ "	and iol.movementqty = il.qtyinvoiced " //qty iol sama dng qty invoice line
				;
		
		//do it
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, get_TrxName());
			rs = pstmt.executeQuery();
			while(rs.next())
			{
				int c_invoiceline_id = rs.getInt("c_invoiceline_id");
//				int m_inoutline_id = rs.getInt("m_inoutline_id");
				Timestamp dateinvoiced = rs.getTimestamp("dateinvoiced");
				BigDecimal qtyinvoiced = rs.getBigDecimal("qtyinvoiced");
				MInvoiceLine invoiceline = new MInvoiceLine(getCtx(), c_invoiceline_id, get_TrxName());
				MMatchInv inv = new MMatchInv(invoiceline, dateinvoiced, qtyinvoiced);
				if (!inv.save(get_TrxName()))
				{
					addLog("cant save matchInv for C_InvoiceLine_ID = "+c_invoiceline_id);
				} else {
					matchInv++;
					inv.setProcessedOn("Processed", true, false);
					inv.saveEx();
					addLog(inv.getDocumentNo());
				}
			}
		}
		finally {
			DB.close(rs);
		}
		return "MatchInv created="+matchInv;
	}

}
