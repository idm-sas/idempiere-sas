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

import java.io.File;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.logging.Level;

import javax.mail.internet.InternetAddress;

import org.compiere.model.MClient;
import org.compiere.model.MDocType;
import org.compiere.model.MMailText;
import org.compiere.model.MOrgInfo;
import org.compiere.model.MRecentItem;
import org.compiere.model.MRole;
import org.compiere.model.MTab;
import org.compiere.model.MTable;
import org.compiere.model.MUser;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.model.X_C_Invoice;
import org.compiere.model.X_C_Order;
import org.compiere.model.X_C_Project;
import org.compiere.model.X_M_InOut;
import org.compiere.print.MPrintFormat;
import org.compiere.print.ReportEngine;
import org.compiere.process.DocAction;
import org.compiere.process.ProcessInfo;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.ServerProcessCtl;
import org.compiere.process.SvrProcess;
import org.compiere.util.EMail;
import org.compiere.util.Env;
import org.compiere.wf.MWFNode;
import org.compiere.wf.MWFResponsible;
import org.eevolution.model.X_PP_Order;

/**
 *  Send Mail to Interest Area Subscribers
 *
 *  @author Jorg Janke
 *  @version $Id: SendMailText.java,v 1.2 2006/07/30 00:51:01 jjanke Exp $
 */
public class ISYSendMailText extends SvrProcess
{
	/** What to send			*/
	private int				m_R_MailText_ID = -1;
	/**	Mail Text				*/
	private MMailText		m_MailText = null;

	/**	From (sender)			*/
	private int				m_AD_User_ID = -1;
	/** Client Info				*/
	private MClient			m_client = null;
	/**	From					*/
	private MUser			m_from = null;
	/** Recipient List to prevent duplicate mails	*/
	
	/** Persistent Object			*/
	private PO					m_po = null;

	private int 			m_counter = 0;
	private int 			m_errors = 0;
	
	/** To Mail Recipient		*/
	private String				m_EmailRecipient = null;
	/** To Mail Recipient		*/
	private String				m_CCRecipient = null;
	/** List of email recipients	*/
	private ArrayList<String> 	m_emails = new ArrayList<String>();
	
	
	/** To Workflow Responsible		*/
	private int				m_WF_Responsible_ID = -1;
	/** To Workflow Responsible CC		*/
	private int				m_CC_Responsible_ID = -1;
	/** To Purchaser of Product	*/
	//	comes here


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
			else if (name.equals("R_MailText_ID"))
				m_R_MailText_ID = para[i].getParameterAsInt();
			else if (name.equals("EMailRecipient"))
				m_EmailRecipient = (String) para[i].getParameter();
			else if (name.equals("CCRecipient"))
				m_CCRecipient = (String) para[i].getParameter();
			else if (name.equals("AD_WF_Responsible_ID"))
				m_WF_Responsible_ID = para[i].getParameterAsInt();
			else if (name.equals("CC_Responsible_ID"))
				m_CC_Responsible_ID = para[i].getParameterAsInt();
			else if (name.equals("AD_User_ID"))
				m_AD_User_ID = para[i].getParameterAsInt();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
	}	//	prepare

	/**
	 *  Perform process.
	 *  @return Message
	 *  @throws Exception
	 */
	protected String doIt() throws Exception
	{
		if (log.isLoggable(Level.INFO)) log.info("R_MailText_ID=" + m_R_MailText_ID);
		//	Mail Test
		m_MailText = new MMailText (getCtx(), m_R_MailText_ID, get_TrxName());
		if (m_MailText.getR_MailText_ID() == 0)
			throw new Exception ("Not found @R_MailText_ID@=" + m_R_MailText_ID);
		//	Client Info
		m_client = MClient.get (getCtx());
		if (m_client.getAD_Client_ID() == 0)
			throw new Exception ("Not found @AD_Client_ID@");
		if (m_client.getSMTPHost() == null || m_client.getSMTPHost().length() == 0)
			throw new Exception ("No SMTP Host found");
		//
		if (m_AD_User_ID > 0)
		{
			m_from = new MUser (getCtx(), m_AD_User_ID, get_TrxName());
			if (m_from.getAD_User_ID() == 0)
				throw new Exception ("No found @AD_User_ID@=" + m_AD_User_ID);
		}
		if (log.isLoggable(Level.FINE)) log.fine("From " + m_from);
		long start = System.currentTimeMillis();
		
		sendEMail();
		
		StringBuilder msgreturn = new StringBuilder("@Created@=").append(m_counter).append(", @Errors@=").append(m_errors).append(" - ")
				.append((System.currentTimeMillis()-start)).append("ms");
		return msgreturn.toString();
	}	//	doIt
	
	/*********************************
	 * 	Send EMail
	 */
	private void sendEMail()
	{
		MTable table = MTable.get (getCtx(), getTable_ID());
		m_po = table.getPO(getRecord_ID(), get_TrxName());
		DocAction doc = (DocAction)m_po;
		MMailText text = new MMailText (getCtx(), m_R_MailText_ID, null);
		text.setPO(m_po, true);
		//
		String subject = doc.getDocumentInfo()
			+ ": " + parse(text.getMailHeader());
		
		StringBuilder sb = new StringBuilder();
		String s = text.get_ValueAsString("MailText"); 
		sb.append(s);
		s = text.get_ValueAsString("MailText2");
		if (s != null && s.length() > 0)
			sb.append("\n").append(s);
		s = text.get_ValueAsString("MailText3");
		if (s != null && s.length() > 0)
			sb.append("\n").append(s);
		String message = sb.toString();
		message = parse(message)
			+ "\n-----\n" + doc.getDocumentInfo()
			+ "\n" + doc.getSummary();
		File pdf = createReport(doc);
		//
		MClient client = MClient.get(doc.getCtx(), doc.getAD_Client_ID());
		MUser user = MUser.get(Env.getCtx(), m_AD_User_ID);

		//	Explicit EMail
		sendEMail(client, 0, user.getEMail(), subject, message, pdf, text.isHtml());
		//	Recipient Type
		String recipient = m_EmailRecipient;
		//	email to document user
		if (recipient == null || recipient.length() == 0)
			sendEMail(client, doc.getDoc_User_ID(), null, subject, message, pdf, text.isHtml());
		else if (recipient.equals(MWFNode.EMAILRECIPIENT_DocumentBusinessPartner))
		{
			int index = m_po.get_ColumnIndex("AD_User_ID");
			if (index > 0)
			{
				Object oo = m_po.get_Value(index);
				if (oo instanceof Integer)
				{
					int AD_User_ID = ((Integer)oo).intValue();
					if (AD_User_ID != 0)
						sendEMail(client, AD_User_ID, null, subject, message, pdf, text.isHtml());
					else
						log.fine("No User in Document");
				}
				else
					log.fine("Empty User in Document");
			}
			else
				log.fine("No User Field in Document");
		}
		else if (recipient.equals(MWFNode.EMAILRECIPIENT_DocumentOwner))
			sendEMail(client, doc.getDoc_User_ID(), null, subject, message, pdf, text.isHtml());
		else if (recipient.equals(MWFNode.EMAILRECIPIENT_WFResponsible))
		{
			MWFResponsible resp = getResponsible();
			if (resp.isInvoker())
				sendEMail(client, doc.getDoc_User_ID(), null, subject, message, pdf, text.isHtml());
			else if (resp.isHuman())
				sendEMail(client, resp.getAD_User_ID(), null, subject, message, pdf, text.isHtml());
			else if (resp.isRole())
			{
				MRole role = resp.getRole();
				if (role != null)
				{
					MUser[] users = MUser.getWithRole(role);
					for (int i = 0; i < users.length; i++)
						sendEMail(client, users[i].getAD_User_ID(), null, subject, message, pdf, text.isHtml());
				}
			}
			else if (resp.isOrganization())
			{
				MOrgInfo org = MOrgInfo.get(getCtx(), m_po.getAD_Org_ID(), get_TrxName());
				if (org.getSupervisor_ID() == 0) {
					if (log.isLoggable(Level.FINE)) log.fine("No Supervisor for AD_Org_ID=" + m_po.getAD_Org_ID());
				} else {
					sendEMail(client, org.getSupervisor_ID(), null, subject, message, pdf, text.isHtml());
				}
			}
		}
	}	//	sendEMail
	
	/**
	 * 	Send actual EMail
	 *	@param client client
	 *	@param AD_User_ID user
	 *	@param email email string
	 *	@param subject subject
	 *	@param message message
	 *	@param pdf attachment
	 *  @param  isHtml isHtml
	 */
	private void sendEMail (MClient client, int AD_User_ID, String email,
		String subject, String message, File pdf, boolean isHtml)
	{
		if (AD_User_ID != 0)
		{
			MUser user = MUser.get(getCtx(), AD_User_ID);
			email = user.getEMail();
			if (email != null && email.length() > 0)
			{
				email = email.trim();
				if (!m_emails.contains(email))
				{
					sendEMail(client, null, user, subject, message, pdf,isHtml);
					m_emails.add(email);
				}
			}
			else
				if (log.isLoggable(Level.INFO)) log.info("No EMail for User " + user.getName());
		}
		else if (email != null && email.length() > 0)
		{
			//	Just one
			if (email.indexOf(';') == -1)
			{
				email = email.trim();
				if (!m_emails.contains(email))
				{
					sendEMail(client, email, subject, message, pdf, isHtml);
					m_emails.add(email);
				}
				return;
			}
			//	Multiple EMail
			StringTokenizer st = new StringTokenizer(email, ";");
			while (st.hasMoreTokens())
			{
				String email1 = st.nextToken().trim();
				if (email1.length() == 0)
					continue;
				if (!m_emails.contains(email1))
				{
					sendEMail(client, email1, subject, message, pdf, isHtml);
					m_emails.add(email1);
				}
			}
		}
	}	//	sendEMail
	
	public boolean sendEMail (MClient client, MUser from, MUser to,
			String subject, String message, File attachment, boolean isHtml)
		{
			EMail email = client.createEMail(from, to, subject, message, isHtml);
			if (email == null)
				return false;
			
			if (attachment != null)
				email.addAttachment(attachment);
			if (m_CCRecipient != null)
				addCc(email, m_CCRecipient);
			InternetAddress emailFrom = email.getFrom();
			try
			{
				return client.sendEmailNow(from, to, email);
			}
			catch (Exception ex)
			{
				log.severe(getName() + " - from " + emailFrom
					+ " to " + to + ": " + ex.getLocalizedMessage());
				return false;
			}
		}	//	sendEMail
	
	public boolean sendEMail (MClient client, String to,
			String subject, String message, File attachment, boolean html)
		{
			EMail email = client.createEMail(to, subject, message, html);
			if (email == null)
				return false;
			if (attachment != null)
				email.addAttachment(attachment);
			try
			{
				String msg = null;
//				if (client.isServerEMail())
//				{
//					msg = CConnection.get().getServer().sendEMail(Env.getRemoteCallCtx(Env.getCtx()), email);
//				}
//				else
//				{
					msg = email.send();
//				}
				if (EMail.SENT_OK.equals (msg))
				{
					if (log.isLoggable(Level.INFO)) log.info("Sent EMail " + subject + " to " + to);
					return true;
				}
				else
				{
					log.warning("Could NOT Send Email: " + subject
						+ " to " + to + ": " + msg
						+ " (" + getName() + ")");
					return false;
				}
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
				log.log(Level.SEVERE, getName() + " - " + ex.getLocalizedMessage(), ex);
				return false;
			}
		}	//	sendEMail
	
	private File createTempFile(){
		try
		{
			File temp = File.createTempFile(MTable.getTableName(Env.getCtx(), getTable_ID())+getRecord_ID()+"_", ".pdf");
			return temp;
		}
		catch (Exception e)
		{
			log.severe("Could not create PDF - " + e.getMessage());
		}
		return null;
	}
	
	private File createReport(DocAction doc){
		File file = createTempFile();
		int reportType = -1;
		if(doc instanceof X_C_Order){
			reportType = ReportEngine.ORDER;
		} else if (doc instanceof X_M_InOut){
			reportType = ReportEngine.SHIPMENT;
		} else if (doc instanceof X_C_Invoice){
			reportType = ReportEngine.INVOICE;
		} else if (doc instanceof X_C_Project){
			reportType = ReportEngine.PROJECT;
		} else if (doc instanceof X_PP_Order){
			reportType = ReportEngine.MANUFACTURING_ORDER;
		}
		
		if(reportType >= 0){
			ReportEngine re = ReportEngine.get (getCtx(), reportType, doc.get_ID(), get_TrxName());
			if (re == null)
				return null;
			MPrintFormat format = re.getPrintFormat();
			// We have a Jasper Print Format
			// ==============================
			if(format.getJasperProcess_ID() > 0)
			{
				ProcessInfo pi = new ProcessInfo ("", format.getJasperProcess_ID());
				pi.setRecord_ID ( doc.get_ID() );
				pi.setIsBatch(true);
				pi.setExport(true);
				
				ServerProcessCtl.process(pi, null);
				
				return pi.getExportFile();
			}
			// Standard Print Format (Non-Jasper)
			// ==================================
			return re.getPDF(file);
		} else {
			int processID = 0;
			MRecentItem recentItem = new Query(Env.getCtx(), MRecentItem.Table_Name, "AD_Table_ID = ? AND Record_ID = ?", null)
									.setParameters(new Object[]{getTable_ID(), getRecord_ID()})
									.setOrderBy(MRecentItem.COLUMNNAME_Updated + " DESC")
									.first();
			MDocType docType = new MDocType(getCtx(), m_po.get_ValueAsInt("C_DocType_ID"), null);
			if(docType != null && docType.getAD_PrintFormat_ID() > 0 && docType.getAD_PrintFormat().getJasperProcess_ID() > 0){
				processID = docType.getAD_PrintFormat().getJasperProcess_ID();
			} else if (recentItem !=null){
				MTab tab = new Query(getCtx(), MTab.Table_Name, "AD_Window_ID = ? AND AD_Table_ID = ?", null)
								.setParameters(new Object[]{recentItem.getAD_Window_ID(), m_po.get_Table_ID()})
								.setOnlyActiveRecords(true)
								.first();
				if(tab != null && tab.getAD_Process_ID() > 0){
					processID = tab.getAD_Process_ID();
				}
			}
			if(processID > 0){
				ProcessInfo pi = new ProcessInfo ("", processID);
				pi.setRecord_ID ( doc.get_ID() );
				pi.setIsBatch(true);
				pi.setPrintPreview(false);
				pi.setExport(true);
				
				ServerProcessCtl.process(pi, null);
				
				return pi.getExportFile();
			} else {
				return doc.createPDF();
			}
		
		}
			
	}// Create Report
	
	public MWFResponsible getResponsible()
	{
		MWFResponsible resp = MWFResponsible.get(getCtx(), m_WF_Responsible_ID);
		return resp;
	}	//	isInvoker
	
	public void addCc(EMail email, String ccRecipient){
		if (ccRecipient.equals(MWFNode.EMAILRECIPIENT_DocumentBusinessPartner))
		{
			int index = m_po.get_ColumnIndex("AD_User_ID");
			if (index > 0)
			{
				Object oo = m_po.get_Value(index);
				if (oo instanceof Integer)
				{
					int AD_User_ID = ((Integer)oo).intValue();
					if (AD_User_ID != 0){
						MUser user = MUser.get(getCtx(), AD_User_ID);
						email.addCc(user.getEMail());
					}
					else
						log.fine("No User in Document");
				}
				else
					log.fine("Empty User in Document");
			}
			else
				log.fine("No User Field in Document");
		}
		else if (ccRecipient.equals(MWFNode.EMAILRECIPIENT_DocumentOwner)){
			DocAction doc = (DocAction)m_po;
			MUser user = MUser.get(getCtx(), doc.getDoc_User_ID());
			email.addCc(user.getEMail());
		}
		else if (ccRecipient.equals(MWFNode.EMAILRECIPIENT_WFResponsible))
		{
			MWFResponsible resp = MWFResponsible.get(getCtx(), m_CC_Responsible_ID);
			if (resp.isInvoker())
				email.addCc(resp.getAD_User().getEMail());
			else if (resp.isHuman())
				email.addCc(resp.getAD_User().getEMail());
			else if (resp.isRole())
			{
				MRole role = resp.getRole();
				if (role != null)
				{
					MUser[] users = MUser.getWithRole(role);
					for (int i = 0; i < users.length; i++){
						MUser user = MUser.get(getCtx(), users[i].getAD_User_ID());
						email.addCc(user.getEMail());
					}
				}
			}
			else if (resp.isOrganization())
			{
				MOrgInfo org = MOrgInfo.get(getCtx(), m_po.getAD_Org_ID(), get_TrxName());
				if (org.getSupervisor_ID() == 0) {
					if (log.isLoggable(Level.FINE)) log.fine("No Supervisor for AD_Org_ID=" + m_po.getAD_Org_ID());
				} else {
					email.addCc(org.getSupervisor().getEMail());
				}
			}
		}
	} // Add Cc
	
	public String parse(String message){
		String temp = "";
		while (message.contains("@")){
			temp = Env.parseVariable(message, m_po, get_TrxName(), false);
			if (temp.equalsIgnoreCase(message))
				break;
			else
				message = temp;
		}
		return message;
	} // Parse Variable
}	//	SendMailText
