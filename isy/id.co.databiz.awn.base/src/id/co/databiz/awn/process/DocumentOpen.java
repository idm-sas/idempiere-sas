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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MAllocationHdr;
import org.compiere.model.MBankStatement;
import org.compiere.model.MChangeLog;
import org.compiere.model.MColumn;
import org.compiere.model.MDocType;
import org.compiere.model.MInOut;
import org.compiere.model.MInventory;
import org.compiere.model.MInvoice;
import org.compiere.model.MJournal;
import org.compiere.model.MMovement;
import org.compiere.model.MPayment;
import org.compiere.model.MRequisition;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.process.DocAction;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.AdempiereSystemError;
 
/**
 *	Re-Open Document Process (from Closed to Completed)
 *	
 *  @author Anozi Mada
 */
public class DocumentOpen extends SvrProcess
{
	/**	The Document				*/
	private int		p_C_DocType_ID = 0;
	private String	p_DocumentNo = "";

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
			else if (name.equals("C_DocType_ID"))
				p_C_DocType_ID = para[i].getParameterAsInt();
			else if (name.equals("DocumentNo"))
				p_DocumentNo = para[i].getParameter().toString();
			else
				log.log(Level.SEVERE, "prepare - Unknown Parameter: " + name);
		}
	}	//	prepare

	/**
	 *  Perform process.
	 *  @return Message
	 *  @throws Exception if not successful
	 */
	protected String doIt() throws AdempiereSystemError
	{
		log.info("doIt - Open C_DocType_ID=" + p_C_DocType_ID + ", DocumentNo=" + p_DocumentNo);
		if (p_C_DocType_ID == 0 || p_DocumentNo == null || p_DocumentNo.isEmpty())
			return "";
		p_DocumentNo = p_DocumentNo.trim();
		//
		MTable table = MTable.get(getCtx(), getAD_Table_ID(p_C_DocType_ID));
		MColumn column = MColumn.get(getCtx(), table.getTableName(), "DocStatus");
		
		List<Object> parameters = new ArrayList<Object>();
		StringBuilder sql = new StringBuilder();
		sql.append(getDocumentNoColumnName(table)).append(" = ? ");
		parameters.add(p_DocumentNo);
		
		if (!(table.getTableName().equals(MAllocationHdr.Table_Name) || 
			  table.getTableName().equals(MBankStatement.Table_Name))) {
			sql.append("AND C_DocType_ID = ? ");
			parameters.add(p_C_DocType_ID);
		}
		
		PO document = new Query(getCtx(), table, sql.toString(), get_TrxName())
			.setClient_ID()
			.setOnlyActiveRecords(true)
			.setParameters(parameters)
			.firstOnly();
		
		if (document == null) {
			throw new AdempiereException(p_DocumentNo + " is not found");
		}
		
		if (!document.get_ValueAsString("DocStatus").equals(DocAction.STATUS_Closed)) {
			throw new AdempiereException("Not closed - can't reopen");
		}
		
		MChangeLog changeLog = new Query(getCtx(), MChangeLog.Table_Name, 
				"AD_Table_ID = ? AND AD_Column_ID = ? AND Record_ID = ? AND OldValue = 'CO'", get_TrxName())
			.setClient_ID()
			.setOnlyActiveRecords(true)
			.setParameters(table.get_ID(), column.get_ID(), document.get_ID())
			.setOrderBy("Created DESC")
			.first();
		
		if (changeLog == null) {
			throw new AdempiereException("Previous status is not completed - can't reopen");
		}
		
		document.set_ValueOfColumn("DocStatus", DocAction.STATUS_Completed);
		document.set_ValueOfColumn("DocAction", DocAction.ACTION_Close);
		addLog(document.get_ID(), null, null, document.get_ValueAsString(getDocumentNoColumnName(table)), table.get_ID(), document.get_ID());
		
		return document.save() ? "@OK@" : "@Error@";
	}	//	doIt
	
	private int getAD_Table_ID(int C_DocType_ID) {
		int tableID = 0;
		MDocType docType = MDocType.get(getCtx(), C_DocType_ID);
		String docBaseType = docType.getDocBaseType();
		if (docBaseType.equals(MDocType.DOCBASETYPE_ARInvoice) || 
				docBaseType.equals(MDocType.DOCBASETYPE_ARProFormaInvoice) ||
				docBaseType.equals(MDocType.DOCBASETYPE_ARCreditMemo) ||
				docBaseType.equals(MDocType.DOCBASETYPE_APInvoice) ||
				docBaseType.equals(MDocType.DOCBASETYPE_APCreditMemo)) {
			
			tableID = MInvoice.Table_ID;
		} else if (docBaseType.equals(MDocType.DOCBASETYPE_APPayment) ||
				docBaseType.equals(MDocType.DOCBASETYPE_ARReceipt)) {
			
			tableID = MPayment.Table_ID;
		} else if (docBaseType.equals(MDocType.DOCBASETYPE_MaterialDelivery) ||
				docBaseType.equals(MDocType.DOCBASETYPE_MaterialReceipt)) {
			tableID = MInOut.Table_ID;
		} else if (docBaseType.equals(MDocType.DOCBASETYPE_MaterialPhysicalInventory)) {
			tableID = MInventory.Table_ID;
		} else if (docBaseType.equals(MDocType.DOCBASETYPE_MaterialMovement)) {
			tableID = MMovement.Table_ID;
		} else if (docBaseType.equals(MDocType.DOCBASETYPE_BankStatement)) {
			tableID = MBankStatement.Table_ID;
		} else if (docBaseType.equals(MDocType.DOCBASETYPE_GLJournal)) {
			tableID = MJournal.Table_ID;
		} else if (docBaseType.equals(MDocType.DOCBASETYPE_PaymentAllocation)) {
			tableID = MAllocationHdr.Table_ID;
		} else if (docBaseType.equals(MDocType.DOCBASETYPE_PurchaseRequisition)) {
			tableID = MRequisition.Table_ID;
		}
		
		if (tableID == 0) {
			throw new AdempiereException("Unsupported document type");
		}
		
		return tableID;
	}
	
	private String getDocumentNoColumnName(MTable table) {
		String documentNoColumnName = "DocumentNo";
		if (table.getTableName().equals(MBankStatement.Table_Name)) {
			documentNoColumnName = "Name";
		}
		return documentNoColumnName;
	}
	
	
}	//	DocumentOpen
