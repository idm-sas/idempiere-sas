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

import id.co.databiz.sas.model.MAging;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.logging.Level;

import org.adempiere.exceptions.DBException;
import org.compiere.model.MBPartner;
import org.compiere.model.MBPartnerLocation;
import org.compiere.model.MInvoice;
import org.compiere.model.MRole;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.TimeUtil;

/**
 *	Invoice Aging Report.
 *	Based on RV_Aging.
 *  @author Jorg Janke
 *  @author victor.perez@e-evolution.com  FR 1933937  Is necessary a new Aging to Date
 *  @see http://sourceforge.net/tracker/index.php?func=detail&aid=1933937&group_id=176962&atid=879335 
 *  @author Carlos Ruiz - globalqss  BF 2655587  Multi-org not supported in Aging
 *  @see https://sourceforge.net/tracker2/?func=detail&aid=2655587&group_id=176962&atid=879332 
 *  @version $Id: Aging.java,v 1.5 2006/10/07 00:58:44 jjanke Exp $
 */
public class Aging extends SvrProcess
{
	/** The date to calculate the days due from			*/
	private Timestamp	p_StatementDate = null;
	//FR 1933937
	private boolean		p_DateAcct = false;
	private boolean 	p_IsSOTrx = false;
	private int			p_C_Currency_ID = 0;
	private int			p_AD_Org_ID = 0;
	private int			p_AD_OrgTrx_ID = 0;
	private int			p_C_BP_Group_ID = 0;
	private int			p_C_BPartner_ID = 0;
	private boolean		p_IsListInvoices = false;
	private String 		p_IsAROther="";
	private int			p_SalesRep_ID = 0;
	/** Number of days between today and statement date	*/
	private int			m_statementOffset = 0;
	
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
			else if (name.equals("StatementDate"))
				p_StatementDate = (Timestamp)para[i].getParameter();
			else if (name.equals("DateAcct"))
				p_DateAcct = "Y".equals(para[i].getParameter());
			else if (name.equals("IsSOTrx"))
				p_IsSOTrx = "Y".equals(para[i].getParameter());
			else if (name.equals("C_Currency_ID"))
				p_C_Currency_ID = ((BigDecimal)para[i].getParameter()).intValue();
			else if (name.equals("AD_Org_ID"))
				p_AD_Org_ID = ((BigDecimal)para[i].getParameter()).intValue();
			else if (name.equals("AD_OrgTrx_ID"))
				p_AD_OrgTrx_ID = ((BigDecimal)para[i].getParameter()).intValue();
			else if (name.equals("C_BP_Group_ID"))
				p_C_BP_Group_ID = ((BigDecimal)para[i].getParameter()).intValue();
			else if (name.equals("C_BPartner_ID"))
				p_C_BPartner_ID = ((BigDecimal)para[i].getParameter()).intValue();
			else if (name.equals("IsListInvoices"))
				p_IsListInvoices = "Y".equals(para[i].getParameter());
			else if (name.equals("IsAROther"))
				p_IsAROther = para[i].getParameter().toString();
			else if (name.equals("SalesRep_ID"))
				p_SalesRep_ID = ((BigDecimal)para[i].getParameter()).intValue();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
		if (p_StatementDate == null)
			p_StatementDate = new Timestamp (System.currentTimeMillis());
		else
			m_statementOffset = TimeUtil.getDaysBetween( 
				new Timestamp(System.currentTimeMillis()), p_StatementDate);
	}	//	prepare

	/**
	 * 	DoIt
	 *	@return Message
	 *	@throws Exception
	 */
	protected String doIt() throws Exception
	{
		if (log.isLoggable(Level.INFO)) log.info("StatementDate=" + p_StatementDate + ", IsSOTrx=" + p_IsSOTrx
			+ ", C_Currency_ID=" + p_C_Currency_ID + ", AD_Org_ID=" + p_AD_Org_ID + ", AD_OrgTrx_ID=" + p_AD_OrgTrx_ID
			+ ", C_BP_Group_ID=" + p_C_BP_Group_ID + ", C_BPartner_ID=" + p_C_BPartner_ID
			+ ", IsListInvoices=" + p_IsListInvoices + ", IsAROther=" + p_IsAROther + ", SalesRep_ID=" + p_SalesRep_ID);
		//FR 1933937
		String dateacct = DB.TO_DATE(p_StatementDate);  
		 
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT bp.C_BP_Group_ID, oi.C_BPartner_ID,oi.C_Invoice_ID,oi.C_InvoicePaySchedule_ID, "  // 1..4 
			+ "oi.C_Currency_ID, oi.IsSOTrx, "								//	5..6
			+ "oi.DateInvoiced, oi.NetDays,oi.DueDate,oi.DaysDue, ");		//	7..10
		if (p_C_Currency_ID == 0)
		{
			if (!p_DateAcct)//FR 1933937
			{
				sql.append(" oi.GrandTotal, oi.PaidAmt, oi.OpenAmt ");			//	11..13
			}
			else
			{
				sql.append(" oi.GrandTotal, invoicePaidToDate(oi.C_Invoice_ID, oi.C_Currency_ID, 1,"+dateacct+") AS PaidAmt, invoiceOpenToDate(oi.C_Invoice_ID,oi.C_InvoicePaySchedule_ID,"+dateacct+") AS OpenAmt ");			//	11..13
			}
		}
		else
		{
			String s = ",oi.C_Currency_ID," + p_C_Currency_ID + ",oi.DateAcct,oi.C_ConversionType_ID,oi.AD_Client_ID,oi.AD_Org_ID)";
			sql.append("currencyConvert(oi.GrandTotal").append(s);		//	11
			if (!p_DateAcct)
			{
				sql.append(", currencyConvert(oi.PaidAmt").append(s)  // 12
				.append(", currencyConvert(oi.OpenAmt").append(s);  // 13
			}
			else
			{
				sql.append(", currencyConvert(invoicePaidToDate(oi.C_Invoice_ID, oi.C_Currency_ID, 1,"+dateacct+")").append(s) // 12
				.append(", currencyConvert(invoiceOpenToDate(oi.C_Invoice_ID,oi.C_InvoicePaySchedule_ID,"+dateacct+")").append(s);  // 13
			}
		}
		sql.append(",oi.C_Activity_ID,oi.C_Campaign_ID,oi.C_Project_ID,oi.AD_Org_ID,oi.AD_OrgTrx_ID,i.DateReceived,i.C_PaymentTerm_ID,i.DunningGrace,i.User1_ID,oi.Z_Kwitansi_ID ");	//	14..23
		if (!p_DateAcct)//FR 1933937
		{
			sql.append(" FROM RV_OpenItem oi");
		}
		else
		{
			sql.append(" FROM RV_OpenItemToDate oi");
		}
		
		sql.append(" INNER JOIN C_BPartner bp ON (oi.C_BPartner_ID=bp.C_BPartner_ID) "
			+ "LEFT JOIN C_Invoice i ON (i.C_Invoice_ID = oi.C_Invoice_ID) "
			+ "LEFT JOIN C_Doctype d ON (d.C_Doctype_ID = i.C_Doctype_ID) "
			+ "LEFT JOIN C_BPartner_Location bpl ON (bpl.C_BPartner_Location_ID=i.C_BPartner_Location_ID) "
			+ "LEFT JOIN C_Order o ON (o.C_Order_ID=i.C_Order_ID) "
			+ "WHERE oi.ISSoTrx=").append(p_IsSOTrx ? "'Y'" : "'N'");
		if (!p_IsAROther.isEmpty()){
			sql.append(" AND d.IsAROther=").append("'"+p_IsAROther+"'");
		}
		if (p_C_BPartner_ID > 0)
		{
			sql.append(" AND oi.C_BPartner_ID=").append(p_C_BPartner_ID);
		}
		else if (p_C_BP_Group_ID > 0)
		{
			sql.append(" AND bp.C_BP_Group_ID=").append(p_C_BP_Group_ID);
		}
		if (p_AD_Org_ID > 0) // BF 2655587
		{
			sql.append(" AND oi.AD_Org_ID=").append(p_AD_Org_ID);
		}
		if (p_AD_OrgTrx_ID > 0) // ISY-41
		{
			sql.append(" AND oi.AD_OrgTrx_ID=").append(p_AD_OrgTrx_ID);
		}
		if (p_SalesRep_ID > 0)
		{
			sql.append(" AND oi.SalesRep_ID=").append(p_SalesRep_ID);
		}
		
		if (p_DateAcct)//FR 1933937
		{
			sql.append(" AND invoiceOpenToDate(oi.C_Invoice_ID,oi.C_InvoicePaySchedule_ID,"+dateacct+") <> 0 ");
		}
		
		sql.append(" ORDER BY oi.C_BPartner_ID, oi.C_Currency_ID, oi.C_Invoice_ID");
		
		if (log.isLoggable(Level.FINEST)) log.finest(sql.toString());
		String finalSql = MRole.getDefault(getCtx(), false).addAccessSQL(
			sql.toString(), "oi", MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO);	
		log.finer(finalSql);

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		//
		MAging aging = null;
		int counter = 0;
		int rows = 0;
		int AD_PInstance_ID = getAD_PInstance_ID();
		//
		try
		{
			pstmt = DB.prepareStatement(finalSql, get_TrxName());
			rs = pstmt.executeQuery();
			while (rs.next())
			{
				int C_BP_Group_ID = rs.getInt(1);
				int C_BPartner_ID = rs.getInt(2);
				int C_Invoice_ID = p_IsListInvoices ? rs.getInt(3) : 0;
				int C_InvoicePaySchedule_ID = p_IsListInvoices ? rs.getInt(4) : 0;
				int C_Currency_ID = rs.getInt(5);
				boolean IsSOTrx = "Y".equals(rs.getString(6));
				//
				//Timestamp DateInvoiced = rs.getTimestamp(7);
				//int NetDays = rs.getInt(8);
				Timestamp DueDate = rs.getTimestamp(9);
				//	Days Due
				int DaysDue = rs.getInt(10)		//	based on today
					+ m_statementOffset;
				//
				BigDecimal GrandTotal = rs.getBigDecimal(11);
				//BigDecimal PaidAmt = rs.getBigDecimal(12);
				BigDecimal OpenAmt = rs.getBigDecimal(13);
				//
				int C_Activity_ID = p_IsListInvoices ? rs.getInt(14) : 0;
				int C_Campaign_ID = p_IsListInvoices ? rs.getInt(15) : 0;
				int C_Project_ID = p_IsListInvoices ? rs.getInt(16) : 0;
				int AD_Org_ID = rs.getInt(17);
				int AD_OrgTrx_ID = rs.getInt(18);
				Timestamp DateReceived = rs.getTimestamp(19);
				int C_PaymentTerm_ID = rs.getInt(20);
				Timestamp DunningGrace = rs.getTimestamp(21);
				int user1_ID = rs.getInt(22);
				int Z_Kwitansi_ID = rs.getInt(23);
				
				rows++;
				//	New Aging Row
				if (aging == null 		//	Key
					|| AD_PInstance_ID != aging.getAD_PInstance_ID()
					|| C_BPartner_ID != aging.getC_BPartner_ID()
					|| C_Currency_ID != aging.getC_Currency_ID()
					|| C_Invoice_ID != aging.getC_Invoice_ID()
					|| C_InvoicePaySchedule_ID != aging.getC_InvoicePaySchedule_ID())
				{
					if (aging != null)
					{
						aging.saveEx();
						if (log.isLoggable(Level.FINE)) log.fine("#" + ++counter + " - " + aging);
					}
					aging = new MAging (getCtx(), AD_PInstance_ID, p_StatementDate, 
						C_BPartner_ID, C_Currency_ID, 
						C_Invoice_ID, C_InvoicePaySchedule_ID, 
						C_BP_Group_ID, AD_Org_ID, DueDate, IsSOTrx, get_TrxName());
					aging.setC_Activity_ID(C_Activity_ID);
					aging.setC_Campaign_ID(C_Campaign_ID);
					aging.setC_Project_ID(C_Project_ID);
					aging.setDateAcct(p_DateAcct);
					if (AD_OrgTrx_ID > 0)
						aging.set_ValueOfColumn("AD_OrgTrx_ID", AD_OrgTrx_ID);
					
					// ISY-41
					if (C_Invoice_ID > 0) {
						MInvoice invoice = MInvoice.get(getCtx(), C_Invoice_ID);
						MBPartner bPartner = MBPartner.get(getCtx(), invoice.getC_BPartner_ID());
						MBPartnerLocation bpl = new MBPartnerLocation(getCtx(), invoice.getC_BPartner_Location_ID(), get_TrxName());
						
						aging.set_ValueOfColumn("DateInvoiced", invoice.getDateInvoiced());
						aging.set_ValueOfColumn("POReference", invoice.getPOReference());
						aging.set_ValueOfColumn("SalesRep_ID", invoice.getSalesRep_ID());
						aging.set_ValueOfColumn("DateReceived", DateReceived);
						aging.set_ValueOfColumn("C_PaymentTerm_ID", C_PaymentTerm_ID);
						aging.set_ValueOfColumn("DunningGrace", DunningGrace);
						aging.set_ValueOfColumn("C_BPartner_Location_ID", invoice.getC_BPartner_Location_ID());
						aging.set_ValueOfColumn("bp_location_name", bpl.getName());
						if(user1_ID > 0)
							aging.set_ValueOfColumn("User1_ID", user1_ID);
						aging.set_ValueOfColumn("Billing_ID", bPartner.get_Value("Billing_ID"));
						aging.set_ValueOfColumn("Collector_ID", bPartner.get_Value("Collector_ID"));
						aging.set_ValueOfColumn("RegionCollection", bpl.get_Value("RegionCollection"));
						aging.set_ValueOfColumn("Order_SalesRep_ID", invoice.getC_Order().getSalesRep_ID());
						aging.set_ValueOfColumn("BP_SalesRep_ID", invoice.getC_BPartner().getSalesRep_ID());
						aging.set_ValueOfColumn("DocStatus", invoice.getDocStatus());					
						if (Z_Kwitansi_ID > 0)
							aging.set_ValueOfColumn("Z_Kwitansi_ID", Z_Kwitansi_ID);
					}
				}
				//	Fill Buckets
				aging.add (DueDate, DaysDue, GrandTotal, OpenAmt);
			}
			if (aging != null)
			{
				aging.saveEx();
				counter++;
				if (log.isLoggable(Level.FINE)) log.fine("#" + counter + " - " + aging);
			}
		}
		catch (SQLException e)
		{
			throw new DBException(e, finalSql);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}
		//	
		if (log.isLoggable(Level.INFO)) log.info("#" + counter + " - rows=" + rows);
		return "";
	}	//	doIt

}	//	Aging
