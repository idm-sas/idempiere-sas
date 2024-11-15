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
package id.co.databiz.awn.process;

import id.co.databiz.awn.model.MPaymentAllocate;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.exceptions.FillMandatoryException;
import org.compiere.model.MBankAccount;
import org.compiere.model.MInvoice;
import org.compiere.model.MPayment;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;

/**
 *	Generate Payment Selection
 *	
 *  @author Anozi Mada
 */
public class GeneratePaymentSelection extends SvrProcess
{	
	private int			p_AD_Org_ID = 0;
	private int			p_C_BankAccount_ID = 0;
	private int			p_C_DocType_ID = 0;
	private Timestamp	p_DateTrx = null;
	private Boolean		p_IsConsolidate = Boolean.TRUE;
	
	private int			m_C_Currency_ID = 0;
	
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
			else if (name.equals("C_BankAccount_ID"))
				p_C_BankAccount_ID = para[i].getParameterAsInt();
			else if (name.equals("C_DocType_ID"))
				p_C_DocType_ID = para[i].getParameterAsInt();
			else if (name.equals("DateTrx"))
				p_DateTrx = para[i].getParameterAsTimestamp();
			else if (name.equals("ConsolidateDocument"))
				p_IsConsolidate = para[i].getParameterAsBoolean();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
	}	//	prepare

	/**
	 *  Perform process.
	 *  @return Message (clear text)
	 *  @throws Exception if not successful
	 */
	protected String doIt() throws Exception
	{
		if (p_AD_Org_ID <= 0) {
			throw new FillMandatoryException("AD_Org_ID");
		}
		if (p_C_BankAccount_ID <= 0) {
			throw new FillMandatoryException("C_BankAccount_ID");
		}
		if (p_C_DocType_ID <= 0) {
			throw new FillMandatoryException("C_DocType_ID");
		}
		if (p_DateTrx == null) {
			throw new FillMandatoryException("DateTrx");
		}
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT i.C_Invoice_ID, i.C_BPartner_ID ");
		sql.append("FROM C_Invoice i ");
		sql.append("INNER JOIN T_Selection s ON (s.T_Selection_ID = i.C_Invoice_ID) ");
		sql.append("WHERE s.AD_PInstance_ID = ? ");
		
		int m_bpartnerID = 0;
		List<MPayment> paymentList = new ArrayList<MPayment>();
		MPayment payment = null;
		MPaymentAllocate payAllocate = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement (sql.toString(), get_TrxName());
			DB.setParameters(pstmt, new Object[]{getAD_PInstance_ID()});
		    rs = pstmt.executeQuery();
		    while(rs.next()) {
		    	int invoiceID = rs.getInt(1);
		    	int bpartnerID = rs.getInt(2);
		    	
		    	MInvoice invoice = new MInvoice(Env.getCtx(), invoiceID, get_TrxName());
		    	
		    	if (payment == null || !p_IsConsolidate || m_bpartnerID!=bpartnerID) {
		    		m_bpartnerID = bpartnerID;
		    		payment = new MPayment(getCtx(), 0, get_TrxName());
					payment.setAD_Org_ID(p_AD_Org_ID);
					payment.setIsReceipt(false);
					payment.setC_BPartner_ID(m_bpartnerID);
					payment.setC_BankAccount_ID(p_C_BankAccount_ID);
					payment.setC_Currency_ID(MBankAccount.get(getCtx(), p_C_BankAccount_ID).getC_Currency_ID());
					payment.setC_DocType_ID(p_C_DocType_ID);
					payment.setDateTrx(p_DateTrx);
					payment.setDateAcct(p_DateTrx);
					payment.setPayAmt(Env.ZERO);
					payment.setC_Project_ID(invoice.getC_Project_ID()); // isy-248 dy 20180518
					payment.setAD_OrgTrx_ID(invoice.getAD_OrgTrx_ID()); 
					payment.setC_Activity_ID(invoice.getC_Activity_ID()); 
					payment.setC_Campaign_ID(invoice.getC_Campaign_ID()); 
					payment.setUser1_ID(invoice.getUser1_ID()); 
					payment.setUser2_ID(invoice.getUser2_ID()); //end of isy-248 dy 20180518
					payment.saveEx();
					paymentList.add(payment);
					addLog(payment.get_ID(), null, null, payment.getDocumentNo(), 
							MPayment.Table_ID, payment.get_ID());
		    	}
		    	//payment.setPayAmt(payment.getPayAmt().add(invoice.getTotalLines()));
				//payment.saveEx();
				
		    	String openAmtSql ="SELECT invoiceOpen(i.C_Invoice_ID,i.C_InvoicePaySchedule_ID) " +
		    					   	"FROM C_Invoice_V i WHERE i.C_Invoice_ID=?";
		    	BigDecimal openAmt = DB.getSQLValueBD(get_TrxName(), openAmtSql, invoiceID);
				if(openAmt==null)
					openAmt = Env.ZERO;
		    	
		    	payAllocate = new MPaymentAllocate(getCtx(), 0, get_TrxName());
				payAllocate.setAD_Org_ID(payment.getAD_Org_ID());
				payAllocate.setC_Payment_ID(payment.get_ID());
				payAllocate.setC_Invoice_ID(invoiceID);
				payAllocate.setAmount(openAmt);
				payAllocate.setInvoiceAmt(openAmt);
				payAllocate.saveEx();
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
		
		return "@Created@: " + paymentList.size();
	}	//	doIt

}	//	Generate Payment Cash