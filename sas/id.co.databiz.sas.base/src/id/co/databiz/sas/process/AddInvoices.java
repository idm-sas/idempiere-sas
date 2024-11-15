/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
package id.co.databiz.sas.process;

import id.co.databiz.sas.SASSystemID;
import id.co.databiz.sas.model.MInvoice;
import id.co.databiz.sas.model.MRequestBundle;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MOrder;
import org.compiere.model.MRequest;
import org.compiere.model.MUser;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

/**
 *	Add Invoices
 *	
 *  @author Anozi Mada
 */
public class AddInvoices extends SvrProcess
{	
	
	private int			p_AD_Org_ID = 0;
	private int			p_C_BPartner_ID = 0;
	private Timestamp	p_DateStart = null;
	private Timestamp	p_DateEnd = null;
	
	private MRequestBundle requestBundle = null;
	private MUser user = null;
	StringBuilder sql = null;
	List<Object> paramList = null;
	
	/**
	 *  Prepare - e.g., get Parameters.
	 */
	protected void prepare()
	{
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else if (name.equals("AD_Org_ID"))
				p_AD_Org_ID = para[i].getParameterAsInt();
			else if (name.equals("C_BPartner_ID"))
				p_C_BPartner_ID = para[i].getParameterAsInt();
			else if (name.equals("DateProcessed")) {
				p_DateStart = para[i].getParameterAsTimestamp();
				p_DateEnd = para[i].getParameter_ToAsTimestamp();
			}
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}	
		
		requestBundle = new MRequestBundle(getCtx(), getRecord_ID(), get_TrxName());
		user = new MUser(getCtx(), requestBundle.getAD_User_ID(), get_TrxName());
	}	//	prepare
	
	private void setDeliveryJournalSQL() {
		sql = new StringBuilder();
		sql.append("SELECT i.C_Invoice_ID, 0 AS R_Request_ID ");
		sql.append("FROM C_Invoice i ");
		sql.append("WHERE i.DocStatus IN ('CO','CL') ");
		sql.append("AND i.IsSOTrx = 'Y' ");
		sql.append("AND i.AD_Org_ID = ? ");						// 1	org
		sql.append("AND i.DateInvoiced BETWEEN ? AND ? ");		// 2,3	startdate,enddate
		sql.append("AND NOT EXISTS(SELECT 1 FROM R_Request r ");
		sql.append("WHERE r.C_Invoice_ID = i.C_Invoice_ID ");
		sql.append("AND r.R_Status_ID NOT IN (?) "); 			// 4 	status not delivered
		sql.append("AND r.R_RequestType_ID = ?) ");				// 5	request type delivery journal
		if (p_C_BPartner_ID > 0) {
			sql.append("AND i.C_BPartner_ID = ? ");				// 6	bp
		}
		sql.append("ORDER BY i.DocumentNo");
		
		paramList = new ArrayList<Object>();
		paramList.add(p_AD_Org_ID);
		paramList.add(p_DateStart);
		paramList.add(p_DateEnd);
		paramList.add(SASSystemID.DJ_STATUS_NOT_DELIVERED);
		paramList.add(requestBundle.getR_RequestType_ID());
		if (p_C_BPartner_ID > 0) {
			paramList.add(p_C_BPartner_ID);
		}
	}
	
	private void setSerahTerimaDeliverySQL() {
		sql = new StringBuilder();
		sql.append("SELECT i.C_Invoice_ID, rl.R_Request_ID ");
		sql.append("FROM C_Invoice i ");
		sql.append("INNER JOIN R_Request rl ON (rl.C_Invoice_ID = i.C_Invoice_ID) ");
		sql.append("INNER JOIN SAS_RequestBundle rb ON (rb.SAS_RequestBundle_ID = rl.SAS_RequestBundle_ID) ");
		sql.append("WHERE i.DocStatus IN ('CO','CL') ");
		sql.append("AND i.IsSOTrx = 'Y' ");
		sql.append("AND i.AD_Org_ID = ? ");						// 1	org
		sql.append("AND rb.DateDoc BETWEEN ? AND ? ");			// 2,3	startdate,enddate
		sql.append("AND rl.R_Request_ID = (SELECT MAX(r.R_Request_ID) FROM R_Request r ");
		sql.append("WHERE r.Processed = 'Y' ");
		sql.append("AND r.C_Invoice_ID = i.C_Invoice_ID ");
		sql.append("AND r.R_Status_ID IN (?,?) "); 				// 4,5 	status delivered,delivered with note
		sql.append("AND r.R_RequestType_ID = ?) "); 			// 6 	type delivery journal
		sql.append("AND NOT EXISTS(SELECT 1 FROM R_Request r ");
		sql.append("WHERE r.C_Invoice_ID = i.C_Invoice_ID ");
		sql.append("AND r.R_Status_ID IN (?,?) "); 				// 7,8	status drafted,delivery to billing
		sql.append("AND r.R_RequestType_ID = ?) ");				// 9	type serah terima delivery
		if (p_C_BPartner_ID > 0) {
			sql.append("AND i.C_BPartner_ID = ? ");				// 10	bp
		}
		sql.append("ORDER BY rb.DocumentNo, rl.DocumentNo ");
		
		paramList = new ArrayList<Object>();
		paramList.add(p_AD_Org_ID);
		paramList.add(p_DateStart);
		paramList.add(p_DateEnd);
		paramList.add(SASSystemID.DJ_STATUS_DELIVERED);
		paramList.add(SASSystemID.DJ_STATUS_DELIVERED_WITH_NOTE);
		paramList.add(SASSystemID.REQUEST_TYPE_DELIVERY_JOURNAL);
		paramList.add(SASSystemID.STD_STATUS_DRAFTED);
		paramList.add(SASSystemID.STD_STATUS_DELIVERY_TO_BILLING);
		paramList.add(requestBundle.getR_RequestType_ID());
		if (p_C_BPartner_ID > 0) {
			paramList.add(p_C_BPartner_ID);
		}
	}
	
	private void setSerahTerimaBillingSQL() {
		sql = new StringBuilder();
		sql.append("SELECT i.C_Invoice_ID, COALESCE(rl.R_Request_ID,0) AS R_Request_ID ");
		sql.append("FROM C_Invoice i ");
		sql.append("INNER JOIN C_BPartner bp ON (bp.C_BPartner_ID = i.C_BPartner_ID) "); 
		sql.append("LEFT JOIN R_Request rl ON (rl.C_Invoice_ID = i.C_Invoice_ID) ");
		sql.append("LEFT JOIN SAS_RequestBundle rb ON (rb.SAS_RequestBundle_ID = rl.SAS_RequestBundle_ID) ");
		sql.append("WHERE i.DocStatus IN ('CO','CL') ");
		sql.append("AND i.IsSOTrx = 'Y' ");
		sql.append("AND i.AD_Org_ID = ? ");								// 1 org
		sql.append("AND (bp.Billing_ID = ? ");							// 2 billing bp
		sql.append("OR i.C_DocType_ID IN (SELECT dt.C_DocType_ID FROM C_DocType dt WHERE dt.IsActive='Y' AND (dt.DocBaseType='ARC' OR dt.C_DocType_ID IN (?,?)))) ");
		
		sql.append("AND ((i.DateInvoiced BETWEEN ? AND ? AND ( ");		// 4,5 startdate,enddate
		sql.append("i.AD_Org_ID = ? "); 								// 6 Org Kenari
		sql.append("OR i.C_DocType_ID IN (SELECT dt.C_DocType_ID FROM C_DocType dt WHERE dt.IsActive='Y' AND (dt.DocBaseType='ARC' OR dt.C_DocType_ID IN (?,?))) ");
		sql.append("OR 'I' = COALESCE((SELECT o.InvoiceRule FROM C_Order o WHERE o.C_Order_ID = i.C_Order_ID),'X') ");
		sql.append(")) OR (rb.DateDoc BETWEEN ? AND ? ");				// 7,8 startdate,enddate
		sql.append("AND rl.R_Request_ID = (SELECT MAX(r.R_Request_ID) FROM R_Request r ");
		sql.append("WHERE r.C_Invoice_ID = i.C_Invoice_ID ");
		sql.append("AND r.Processed = 'Y' ");
		sql.append("AND r.R_Status_ID IN (?)"); 						// 9 status delivery to billing
		sql.append("AND r.R_RequestType_ID = ?))) "); 					// 10 type serah terima delivery
		
		sql.append("AND NOT EXISTS(SELECT 1 FROM R_Request r ");
		sql.append("WHERE r.C_Invoice_ID = i.C_Invoice_ID ");
		sql.append("AND r.R_Status_ID IN (?,?) "); 						// 10,11	status drafted,billing
		sql.append("AND r.R_RequestType_ID = ?) ");						// 12 type serah terima billing
		if (p_C_BPartner_ID > 0) {
			sql.append("AND i.C_BPartner_ID = ? ");						// 13 bp
		}
		sql.append("ORDER BY rb.DocumentNo, rl.DocumentNo ");
		
		paramList = new ArrayList<Object>();
		paramList.add(p_AD_Org_ID);
		paramList.add(user.getC_BPartner_ID());
		paramList.add(SASSystemID.DOCTYPE_AR_INVOICE_FCN);
		paramList.add(SASSystemID.DOCTYPE_AR_INVOICE_FCT);
		paramList.add(p_DateStart);
		paramList.add(p_DateEnd);
		paramList.add(SASSystemID.ORG_KENARI);
		paramList.add(SASSystemID.DOCTYPE_AR_INVOICE_FCN);
		paramList.add(SASSystemID.DOCTYPE_AR_INVOICE_FCT);
		paramList.add(p_DateStart);
		paramList.add(p_DateEnd);
		paramList.add(SASSystemID.STD_STATUS_DELIVERY_TO_BILLING);
		paramList.add(SASSystemID.REQUEST_TYPE_SERAH_TERIMA_DELIVERY);
		paramList.add(SASSystemID.STB_STATUS_DRAFTED);
		paramList.add(SASSystemID.STB_STATUS_BILLING);
		paramList.add(requestBundle.getR_RequestType_ID());
		if (p_C_BPartner_ID > 0) {
			paramList.add(p_C_BPartner_ID);
		}
	}
	
	// not used
	private void setDaftarTagihOfficeSQL() {
		sql = new StringBuilder();
		sql.append("SELECT i.C_Invoice_ID, 0 AS R_Request_ID ");
		sql.append("FROM C_Invoice i ");
		sql.append("WHERE i.DocStatus IN ('CO','CL') ");
		sql.append("AND i.IsSOTrx = 'Y' ");
		sql.append("AND i.AD_Org_ID = ? ");
		sql.append("AND i.DateInvoiced BETWEEN ? AND ? ");
		sql.append("AND EXISTS(SELECT 1 FROM R_Request r ");
		sql.append("WHERE r.C_Invoice_ID = i.C_Invoice_ID ");
		sql.append("AND r.Processed = 'Y' ");
		sql.append("AND r.R_Status_ID NOT IN (?)"); // drafted
		sql.append("AND r.R_RequestType_ID = ?) "); // serah terima billing
		sql.append("AND NOT EXISTS(SELECT 1 FROM R_Request r ");
		sql.append("AND r.R_Status_ID IN (?,?,?)"); // paid,giro,antar dokumen
		sql.append("WHERE r.C_Invoice_ID = i.C_Invoice_ID ");
		sql.append("AND r.R_RequestType_ID = ?) ");
		if (p_C_BPartner_ID > 0) {
			sql.append("AND i.C_BPartner_ID = ? ");
		}
		
		paramList = new ArrayList<Object>();
		paramList.add(p_AD_Org_ID);
		paramList.add(p_DateStart);
		paramList.add(p_DateEnd);
		paramList.add(SASSystemID.STB_STATUS_DRAFTED);
		paramList.add(SASSystemID.REQUEST_TYPE_SERAH_TERIMA_BILLING);
		paramList.add(SASSystemID.DTO_STATUS_PAID);
		paramList.add(SASSystemID.DTO_STATUS_GIRO);
		paramList.add(SASSystemID.DTO_STATUS_ANTAR_DOKUMEN);
		paramList.add(requestBundle.getR_RequestType_ID());
		if (p_C_BPartner_ID > 0) {
			paramList.add(p_C_BPartner_ID);
		}
	}

	/**
	 *  Perform process.
	 *  @return Message (clear text)
	 *  @throws Exception if not successful
	 */
	protected String doIt() throws Exception
	{
		int count = 0;

		if (requestBundle.getR_RequestType_ID() == SASSystemID.REQUEST_TYPE_DELIVERY_JOURNAL) {
			setDeliveryJournalSQL();
		} else if (requestBundle.getR_RequestType_ID() == SASSystemID.REQUEST_TYPE_SERAH_TERIMA_DELIVERY) {
			setSerahTerimaDeliverySQL();
		} else if (requestBundle.getR_RequestType_ID() == SASSystemID.REQUEST_TYPE_SERAH_TERIMA_BILLING) {
			setSerahTerimaBillingSQL();
		} else if (requestBundle.getR_RequestType_ID() == SASSystemID.REQUEST_TYPE_DAFTAR_TAGIH_OFFICE) {
			setDaftarTagihOfficeSQL();
		}

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement (sql.toString(), get_TrxName());
			DB.setParameters(pstmt, paramList);
		    rs = pstmt.executeQuery();
		    while(rs.next()) {
		          int invoiceID = rs.getInt("C_Invoice_ID");
		          int prevRequestID = rs.getInt("R_Request_ID");
		          MInvoice invoice = new MInvoice(getCtx(), invoiceID, get_TrxName());
		          MRequest request = new MRequest(getCtx(), 0, get_TrxName());
		          MRequest prevRequest = null;
		          if (prevRequestID > 0) {
		        	  prevRequest = new MRequest(getCtx(), prevRequestID, get_TrxName());
		          }
		          request.setAD_Org_ID(requestBundle.getAD_Org_ID());
		          request.set_ValueOfColumn("SAS_RequestBundle_ID", requestBundle.get_ID());
		          request.setR_RequestType_ID(requestBundle.getR_RequestType_ID());
		          request.setR_Status_ID(requestBundle.getR_Status_ID());
		          request.set_ValueOfColumn("DocumentType", "Invoice");
		          request.setC_Invoice_ID(invoiceID);
		          request.setC_BPartner_ID(invoice.getC_BPartner_ID());
		          BigDecimal openAmt = invoice.getOpenAmt();
		          request.set_ValueOfColumn("OpenAmt", openAmt);
		          request.set_ValueOfColumn("RemainingAmt", openAmt);
		          request.setSummary("-");
		          if (prevRequest != null) {
		        	  request.set_ValueOfColumn("BPKNo", prevRequest.get_Value("BPKNo"));
		        	  if (request.getR_RequestType_ID() == SASSystemID.REQUEST_TYPE_SERAH_TERIMA_DELIVERY) {
		        		  request.setSummary(prevRequest.getSummary());
		        	  }
		          }
		          if (invoice.getC_Order_ID() > 0) {
		        	  MOrder order = new MOrder(getCtx(), invoice.getC_Order_ID(), get_TrxName());
		        	  request.set_ValueOfColumn("PickListNote", order.get_Value("PickListNote"));
		          }
		          request.setSalesRep_ID(requestBundle.getAD_User_ID());
		          request.setConfidentialType(MRequest.CONFIDENTIALTYPE_PartnerConfidential);
		          request.setConfidentialTypeEntry(MRequest.CONFIDENTIALTYPEENTRY_PartnerConfidential);
		          request.setPriority(MRequest.PRIORITY_Medium);
		          request.saveEx();
		          count++;
		    }
		}
		catch (Exception e)
		{
			throw new AdempiereException(e);
		}
		finally
		{
		      DB.close(rs, pstmt);
		      rs = null; pstmt = null;
		}
		return "@Created@ " + count;
	}	//	doIt

}	//	Generate Invoice Request