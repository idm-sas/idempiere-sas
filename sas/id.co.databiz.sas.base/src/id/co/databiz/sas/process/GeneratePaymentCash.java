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

import id.co.databiz.awn.model.MPayment;
import id.co.databiz.awn.model.MPaymentAllocate;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.exceptions.FillMandatoryException;
import org.compiere.model.MBankAccount;
import org.compiere.model.Query;
import org.compiere.process.DocAction;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;

/**
 *	Generate Payment Cash
 *	
 *  @author Anozi Mada
 */
public class GeneratePaymentCash extends SvrProcess
{	
	private int			p_AD_Org_ID = 0;
	private int			p_C_BankAccount_ID = 0;
	private int			p_C_DocType_ID = 0;
	private int			p_C_BPartner_ID = 0;
	private int			p_C_Charge_ID = 0;
	private Timestamp	p_DateTrx = null;
	private Boolean		p_IsConsolidate = Boolean.TRUE;
	private String		p_DocAction = DocAction.ACTION_None;
	private String		p_Description = null;
	
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
			else if (name.equals("C_BPartner_ID"))
				p_C_BPartner_ID = para[i].getParameterAsInt();
			else if (name.equals("C_Charge_ID"))
				p_C_Charge_ID = para[i].getParameterAsInt();
			else if (name.equals("DateTrx"))
				p_DateTrx = para[i].getParameterAsTimestamp();
			else if (name.equals("ConsolidateDocument"))
				p_IsConsolidate = para[i].getParameterAsBoolean();
			else if (name.equals("DocAction"))
				p_DocAction = para[i].getParameterAsString();
			else if (name.equals("Description"))
				p_Description = para[i].getParameterAsString();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
		
		if (p_DocAction == null) {
			p_DocAction = DocAction.ACTION_None;
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
		if (p_C_BPartner_ID <= 0) {
			throw new FillMandatoryException("C_BPartner_ID");
		}
		if (p_C_Charge_ID <= 0) {
			throw new FillMandatoryException("C_Charge_ID");
		}
		if (p_DateTrx == null) {
			throw new FillMandatoryException("DateTrx");
		}
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT p.C_Payment_ID ");
		sql.append("FROM C_Payment p ");
		sql.append("INNER JOIN T_Selection s ON (s.T_Selection_ID = p.C_Payment_ID) ");
		sql.append("WHERE s.AD_PInstance_ID = ? AND p.IsInTransit = 'N' AND p.IsCashed='N' ");
		
		List<MPayment> paymentList = new ArrayList<MPayment>();
		MPayment payment = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement (sql.toString(), get_TrxName());
			DB.setParameters(pstmt, new Object[]{getAD_PInstance_ID()});
		    rs = pstmt.executeQuery();
		    while(rs.next()) {
		    	if (payment == null || !p_IsConsolidate) {
		    		payment = new MPayment(getCtx(), 0, get_TrxName());
					payment.setAD_Org_ID(p_AD_Org_ID);
					payment.setIsReceipt(false);
					payment.setC_BankAccount_ID(p_C_BankAccount_ID);
					payment.setC_Currency_ID(MBankAccount.get(getCtx(), p_C_BankAccount_ID).getC_Currency_ID());
					payment.setC_DocType_ID(p_C_DocType_ID);
					payment.setC_BPartner_ID(p_C_BPartner_ID);
					payment.setDateTrx(p_DateTrx);
					payment.setDateAcct(p_DateTrx);
					payment.setPayAmt(Env.ZERO);
					payment.setTenderType(MPayment.TENDERTYPE_DirectDeposit);
					payment.setDescription(p_Description);
					payment.saveEx();
					paymentList.add(payment);
					addLog(payment.get_ID(), null, null, payment.getDocumentNo(), 
							MPayment.Table_ID, payment.get_ID());
		    	}
			    	
		    	int paymentID = rs.getInt("C_Payment_ID");
		    	MPayment giro = new MPayment(getCtx(), paymentID, get_TrxName());
		    	
		    	if (m_C_Currency_ID == 0) {
		    		m_C_Currency_ID = giro.getC_Currency_ID();
		    	}
		    	if (m_C_Currency_ID != giro.getC_Currency_ID()) {
		    		throw new AdempiereException("ISY-244.\n Different currency selected.");
		    	}
		    	giro.set_ValueOfColumn("IsInTransit", "Y");
		    	giro.set_ValueOfColumn("C_PaymentCash_ID", payment.get_ID());
		    	giro.saveEx();
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
		
		for (MPayment paymentCash : paymentList) {
			List<MPayment> giroList = new Query(getCtx(), MPayment.Table_Name, "C_PaymentCash_ID = ?", get_TrxName())
				.setParameters(paymentCash.get_ID())
				.list();
			for (MPayment giro : giroList) {
				MPaymentAllocate pa = new MPaymentAllocate(getCtx(), 0, get_TrxName());
				pa.setAD_Org_ID(paymentCash.getAD_Org_ID());
				pa.setC_Payment_ID(paymentCash.get_ID());
				pa.set_ValueOfColumn("C_Charge_ID", p_C_Charge_ID);
				pa.setAmount(giro.getPayAmt());
				pa.set_ValueOfColumn("C_PaymentGiro_ID", giro.get_ID());
				pa.set_ValueOfColumn("CheckNo", giro.getCheckNo());
				pa.saveEx();
			}
			
			if (!p_DocAction.equals(DocAction.ACTION_None)) {
				paymentCash.setDocAction(p_DocAction);
				paymentCash.processIt(p_DocAction);
				paymentCash.saveEx();
			}
		}
		
		return "@Created@: " + paymentList.size();
	}	//	doIt

}	//	Generate Payment Cash