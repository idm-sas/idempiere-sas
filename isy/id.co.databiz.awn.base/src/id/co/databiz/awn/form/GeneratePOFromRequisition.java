/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
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
 * Copyright (C) 2003-2007 e-Evolution,SC. All Rights Reserved.               *
 * Contributor(s): Victor Perez www.e-evolution.com                           *
 *                 Teo Sarca, http://www.arhipac.ro                           *
 ******************************************************************************/

package id.co.databiz.awn.form;

import id.co.databiz.awn.model.wrapper.ICOrderLine;
import id.co.databiz.awn.model.wrapper.IMRequisition;
import id.co.databiz.awn.model.wrapper.IMRequisitionLine;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.model.POWrapper;
import org.compiere.apps.form.GenForm;
import org.compiere.minigrid.IDColumn;
import org.compiere.minigrid.IMiniTable;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProductPO;
import org.compiere.model.MRequisition;
import org.compiere.model.MRequisitionLine;
import org.compiere.model.MUOM;
import org.compiere.model.MUOMConversion;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;

/**
 * 
 * @author Anozi Mada
 */

public abstract class GeneratePOFromRequisition extends GenForm {

	/** Logger */
	private static CLogger log = CLogger.getCLogger(GeneratePOFromRequisition.class);
	StringBuffer sql = new StringBuffer();
 
	protected int AD_Org_ID = 0;
	protected String productValue;
	protected int C_BPartner_ID = 0;
	protected String classification;
	protected int M_PriceList_ID = 0;
	protected String group1;
	protected String group2;
	protected int C_DocType_ID = 0;
	protected Date dateRequired = null;
	protected int User1_ID = 0;
	protected int C_Project_ID = 0;
	protected int M_Requisition_ID = 0;
	
	private DecimalFormat   amountFormat = DisplayType.getNumberFormat(DisplayType.Amount);
	private DateFormat   dateFormat = new SimpleDateFormat("dd-MM-yyyy");
	

	public void configureMiniTable(IMiniTable table) {
		table.addColumn(MRequisitionLine.COLUMNNAME_M_RequisitionLine_ID); // 0
		table.addColumn(MRequisition.COLUMNNAME_DocumentNo); // 1
		table.addColumn(MRequisitionLine.COLUMNNAME_C_BPartner_ID); // 2
		table.addColumn(MRequisitionLine.COLUMNNAME_M_Product_ID); // 3
		table.addColumn(MRequisition.COLUMNNAME_DateRequired); // 4
		table.addColumn(MRequisitionLine.COLUMNNAME_Qty); // 5
		table.addColumn(MOrderLine.COLUMNNAME_QtyOrdered); // 6		
		table.addColumn("OpenQty"); // 7		
		table.setMultiSelection(true);
		// issue.setRowSelectionAllowed(true);

		// set details
		table.setColumnClass(0, IDColumn.class, false, " ");
		table.setColumnClass(1, String.class, true, Msg.translate(Env.getCtx(), "DocumentNo"));
		table.setColumnClass(2, String.class, true, Msg.translate(Env.getCtx(), "C_BPartner_ID"));
		table.setColumnClass(3, String.class, true, Msg.translate(Env.getCtx(), "M_Product_ID"));
		table.setColumnClass(4, String.class, true, Msg.translate(Env.getCtx(), "DateRequired"));
		table.setColumnClass(5, BigDecimal.class, true, Msg.translate(Env.getCtx(), "QtyRequired"));
		table.setColumnClass(6, BigDecimal.class, false, Msg.translate(Env.getCtx(), "QtyOrdered"));
		table.setColumnClass(7, BigDecimal.class, true, "OpenQty");

		table.autoSize();
	} // dynInit

	protected String createHTMLTable(MOrder order) {
		List<MOrderLine> orderLineList = new Query(Env.getCtx(),MOrderLine.Table_Name,"C_Order_ID = ?",null)
				.setParameters(new Object[]{order.get_ID()})
				.list();
		String[][] data = new String[orderLineList.size() + 1][3];
		data[0][0] = "Line";
		data[0][1] = "Product";
		data[0][2] = "Qty";
		for(int i=1; i<=orderLineList.size(); i++){
			MOrderLine orderLine = orderLineList.get(i-1);
			data[i][0] = new Integer(orderLine.getLine()).toString();
			if(orderLine.getM_Product_ID()>0){
				data[i][1] = orderLine.getM_Product().getName();								
			}
			data[i][2] = orderLine.getQtyOrdered().toString();
		}
		
		StringBuffer html = new StringBuffer("<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"0\">");

		for (int i = 0; i < data.length; i++) {
			if (data[i] != null) {
				html.append("<tr>");
				for (int j = 0; j < data[i].length; j++) {
					html.append("<td>");
					if (data[i][j] != null) {
						html.append(data[i][j]);
					}
					html.append("</td>");
				}
				html.append("</tr>");
			}
		}
		html.append("</table>");

		return html.toString();
	}


	/**
	 * Query Info
	 */
	public void executeQuery(IMiniTable miniTable) {
		sql = new StringBuffer();
		sql.append("SELECT")
			.append(" rl.M_RequisitionLine_ID,")
			.append(" r.DocumentNo,")			
			.append(" (SELECT bp.Name FROM C_BPartner bp WHERE bp.C_BPartner_ID = CASE WHEN rl.C_BPartner_ID IS NULL THEN (SELECT MAX(ppo.C_BPartner_ID) FROM M_Product_PO ppo WHERE ppo.IsCurrentVendor = 'Y' AND ppo.M_Product_ID = rl.M_Product_ID) ELSE rl.C_BPartner_ID END),")
			.append(" (SELECT p.Name FROM M_Product p WHERE p.M_Product_ID = rl.M_Product_ID),")
			.append(" COALESCE(rl.DateRequired,r.DateRequired),")
			.append(" rl.Qty,")
			.append(" rl.Qty - COALESCE((SELECT SUM(ol.QtyOrdered) FROM C_OrderLine ol INNER JOIN C_Order o ON (o.C_Order_ID = ol.C_Order_ID) WHERE o.DocStatus <> 'VO' AND ol.M_RequisitionLine_ID = rl.M_RequisitionLine_ID),0) QtyOrdered")			
			.append(" FROM M_RequisitionLine rl")
			.append(" INNER JOIN M_Requisition r ON (r.M_Requisition_ID = rl.M_Requisition_ID)")
			.append(" LEFT JOIN M_Product p ON (p.M_Product_ID = rl.M_Product_ID)")
			.append(" WHERE rl.AD_Client_ID = ? AND r.DocStatus = 'CO'") // 1
			.append(" AND rl.Qty > COALESCE((SELECT SUM(ol.QtyOrdered) FROM C_OrderLine ol INNER JOIN C_Order o ON (o.C_Order_ID = ol.C_Order_ID) WHERE o.DocStatus <> 'VO' AND ol.M_RequisitionLine_ID = rl.M_RequisitionLine_ID),0)");
		if(getAD_Org_ID()>0){
			sql.append(" AND rl.AD_Org_ID = ").append(getAD_Org_ID());
		}
		if(getProductValue()!=null && !getProductValue().isEmpty()){
			sql.append(" AND UPPER(p.Value) LIKE UPPER('").append(getProductValue()).append("')");
		}
		if(getClassification()!=null && !getClassification().isEmpty()){
			sql.append(" AND UPPER(p.Classification) LIKE UPPER('").append(getClassification()).append("')");
		}
		if(getM_PriceList_ID()>0){
			sql.append(" AND r.M_PriceList_ID = ").append(getM_PriceList_ID());
		}
		if(getGroup1()!=null && !getGroup1().isEmpty()){
			sql.append(" AND UPPER(p.Group1) LIKE UPPER('").append(getGroup1()).append("')");
		}
		if(getGroup2()!=null && !getGroup2().isEmpty()){
			sql.append(" AND UPPER(p.Group2) LIKE UPPER('").append(getGroup2()).append("')");
		}
//		if(getC_DocType_ID()>0){
//			sql.append(" AND r.C_DocType_ID = ").append(getC_DocType_ID());
//		}
		if(getDateRequired()!=null){
			sql.append(" AND (CASE WHEN rl.DateRequired IS NULL THEN r.DateRequired ELSE rl.DateRequired END) = ?"); // 2
		}
		if(getC_Project_ID()>0){
			sql.append(" AND CASE WHEN rl.C_Project_ID IS NULL THEN r.C_Project_ID ELSE rl.C_Project_ID END = ").append(getC_Project_ID());
		}
		if(getUser1_ID()>0){
			sql.append(" AND CASE WHEN rl.User1_ID IS NULL THEN r.User1_ID ELSE rl.User1_ID END = ").append(getUser1_ID());
		}
		if(getM_Requisition_ID()>0){
			sql.append(" AND r.M_Requisition_ID = ").append(getM_Requisition_ID());
		}
		sql.append(" ORDER BY r.DocumentNo,rl.Line");
		
		//  reset table
		int row = 0;
		miniTable.setRowCount(row);
		//  Execute
		try
		{
			PreparedStatement pstmt = DB.prepareStatement(sql.toString(), null);
			int AD_Client_ID = Env.getAD_Client_ID(Env.getCtx());
			pstmt.setInt(1, AD_Client_ID);
			if(getDateRequired()!=null){
				pstmt.setDate(2, new java.sql.Date(getDateRequired().getTime()));	
			}
			ResultSet rs = pstmt.executeQuery();
			//
			while (rs.next())
			{
				//  extend table
				miniTable.setRowCount(row+1);
				//  set values
				miniTable.setValueAt(new IDColumn(rs.getInt(1)), row, 0);   //  M_RequisitionLine_ID
				miniTable.setValueAt(rs.getString(2), row, 1);              //  DocumentNo
				miniTable.setValueAt(rs.getString(3), row, 2);              //  C_BPartner_ID
				miniTable.setValueAt(rs.getString(4), row, 3);              //  M_Product_ID
				miniTable.setValueAt(dateFormat.format(rs.getTimestamp(5)), row, 4);              //  DateRequired
				miniTable.setValueAt(rs.getBigDecimal(6), row, 5);           //  OpenQty
				miniTable.setValueAt(rs.getBigDecimal(7), row, 6);          //  QtyOrdered
				miniTable.setValueAt(rs.getBigDecimal(7), row, 7);          //  OpenQty
				//  prepare next
				row++;
			}
			rs.close();
			pstmt.close();
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, sql.toString(), e);
		}
		//
		miniTable.autoSize();
	} // executeQuery

	public String generateSummaryTable(IMiniTable requisitionLine, String productField,
			String uomField, String attribute, String toDeliverQty,
			String deliveredQtyField, String scrapQtyField,
			boolean isBackflush, boolean isOnlyIssue, boolean isOnlyReceipt) {
		//TODO
		return "";
	}
	
	public MOrder generatePO(IMiniTable table) throws Exception{
		MOrder order = null;
		int count = table.getRowCount();
		if(count>0){
			Map<Integer,BigDecimal> requisitionMap = new HashMap<Integer,BigDecimal>();
			for(int i = 0; i < count; i++){
				IDColumn id = (IDColumn) table.getValueAt(i, 0);
				if(id.isSelected()){
					BigDecimal qty = (BigDecimal)table.getValueAt(i, 6);
					requisitionMap.put(id.getRecord_ID(), qty);
				}
			}
			
			if(!requisitionMap.isEmpty()){
				Timestamp dateRequired = new Timestamp(-1);
				
				for(Integer requisitionLineID : requisitionMap.keySet()){
					MRequisitionLine requisitionLine = new MRequisitionLine(Env.getCtx(), requisitionLineID, null);
					IMRequisitionLine requisitionLineCustom = POWrapper.create(requisitionLine, IMRequisitionLine.class);
					MRequisition requisition = new MRequisition(Env.getCtx(), requisitionLine.getM_Requisition_ID(), null);
					IMRequisition requisitionCustom = POWrapper.create(requisition, IMRequisition.class);
					BigDecimal qtyOrder = requisitionMap.get(requisitionLineID);
					if(order==null){
						order = new MOrder(Env.getCtx(), 0, null);
						order.setIsSOTrx(false);
						if(requisitionLineCustom.getDateRequired()!=null){
							order.setDatePromised(requisitionLineCustom.getDateRequired());
							if(requisitionLineCustom.getDateRequired().after(dateRequired)){
								dateRequired = requisitionLineCustom.getDateRequired();
							}
						} else {
							order.setDatePromised(requisitionLine.getParent().getDateRequired());
						}
						if(requisitionLine.getParent().getDateRequired().after(dateRequired)){
							dateRequired = requisitionLine.getParent().getDateRequired();
						}
						if(getC_DocType_ID()>0){
							order.setC_DocTypeTarget_ID(getC_DocType_ID());
						} else {
							order.setC_DocTypeTarget_ID();
						}
						order.setC_BPartner_ID(getC_BPartner_ID());
						order.setM_PriceList_ID(requisition.getM_PriceList_ID());
						order.setSalesRep_ID(requisition.getAD_User_ID());
						order.setM_Warehouse_ID(requisition.getM_Warehouse_ID());
						order.setC_Project_ID(requisitionCustom.getC_Project_ID());
						order.setUser1_ID(requisitionCustom.getUser1_ID());
						order.setDescription(requisition.getDescription());
						
						order.setC_Campaign_ID(requisition.get_ValueAsInt("C_Campaign_ID"));
						order.setC_Activity_ID(requisition.get_ValueAsInt("C_Activity_ID"));
						order.set_ValueOfColumn("AD_OrgTrx_ID", requisition.get_ValueAsInt("AD_OrgTrx_ID"));
						
						order.saveEx();
					}
					MOrderLine orderLine = new MOrderLine(order);
					ICOrderLine orderLineCustom = POWrapper.create(orderLine, ICOrderLine.class);
					if(requisitionLineCustom.getDateRequired()!=null){
						orderLine.setDatePromised(requisitionLineCustom.getDateRequired());
						if(requisitionLineCustom.getDateRequired().after(dateRequired)){
							dateRequired = requisitionLine.getDateRequired();
						}
					} else {
						orderLine.setDatePromised(requisitionLine.getParent().getDateRequired());
					}
					int uomID = requisitionLine.getC_UOM_ID();
					if (requisitionLine.getM_Product_ID() > 0) {
						orderLine.setM_Product_ID(requisitionLine.getM_Product_ID());
						
						MProductPO ppo = new Query(Env.getCtx(), MProductPO.Table_Name, 
								"M_Product_ID=? AND C_UOM_ID > 0", null)
							.setParameters(requisitionLine.getM_Product_ID())
							.setOnlyActiveRecords(true)
							.setOrderBy("IsCurrentVendor DESC")
							.first();
						if (ppo != null) {
							uomID = ppo.getC_UOM_ID();
						} else if (requisitionLine.getC_UOM_ID() > 0) {
							uomID = requisitionLine.getC_UOM_ID();
						} else {
							uomID = requisitionLine.getM_Product().getC_UOM_ID();
						}
					}
					orderLine.setM_AttributeSetInstance_ID(requisitionLine.getM_AttributeSetInstance_ID());
					orderLine.setAD_Org_ID(requisitionLine.getAD_Org_ID());
					orderLine.setQty(qtyOrder);
					BigDecimal qtyEntered = MUOMConversion.convertProductTo(Env.getCtx(), requisitionLine.getM_Product_ID(), uomID, qtyOrder);
					if (qtyEntered == null) {
						throw new AdempiereException("NoUOMConversion. Product=" + requisitionLine.getM_Product().getValue() + 
								", UOM=" + MUOM.get(Env.getCtx(), uomID).getName());
					}
					orderLine.setQtyEntered(qtyEntered);
//					orderLine.setQtyEntered(qtyOrder.multiply((BigDecimal)requisitionLine.get_Value("QtyEntered")).divide(requisitionLine.getQty(), 12, BigDecimal.ROUND_HALF_UP));
					orderLine.setPrice();
					if (requisitionLine.getC_Charge_ID() > 0) {
						orderLine.setC_Charge_ID(requisitionLine.getC_Charge_ID());
						orderLine.setPrice(requisitionLine.getC_Charge().getChargeAmt());
					}
					orderLine.setC_UOM_ID(uomID);
					orderLine.setC_Project_ID(requisitionLineCustom.getC_Project_ID());
					orderLine.setUser1_ID(requisitionLineCustom.getUser1_ID());
					orderLine.setDescription(requisitionLine.getDescription());
					orderLineCustom.setM_RequisitionLine_ID(requisitionLine.get_ID());
					orderLine.setC_Campaign_ID(order.getC_Campaign_ID());
					orderLine.setC_Activity_ID(order.getC_Activity_ID());
					orderLine.set_ValueOfColumn("AD_OrgTrx_ID", requisitionLine.get_ValueAsInt("AD_OrgTrx_ID"));
					
					orderLine.saveEx();
//					requisitionLine.setC_OrderLine_ID(orderLine.getC_OrderLine_ID());
					requisitionLine.saveEx();
				}
				
				List<MOrderLine> orderLineList = new Query(Env.getCtx(),MOrderLine.Table_Name,"C_Order_ID = ?",null)
								.setParameters(new Object[]{order.get_ID()})
								.list();
				order.setDatePromised(dateRequired);
				order.saveEx();
				for(MOrderLine line : orderLineList){
					line.setDatePromised(dateRequired);
					line.saveEx();
				}
			}
		}
		return order;
	}
	

	private BigDecimal getValueBigDecimal(IMiniTable table, int row, int col) {
		BigDecimal bd = (BigDecimal) table.getValueAt(row, col);
		return bd == null ? Env.ZERO : bd;
	}


	/**
	 * Save Selection & return selecion Query or ""
	 * 
	 * @return where clause like C_Order_ID IN (...)
	 */
	public void saveSelection(IMiniTable miniTable) {
		log.info("");
		// Array of Integers
		ArrayList<Integer> results = new ArrayList<Integer>();
		setSelection(null);

		// Get selected entries
		int rows = miniTable.getRowCount();
		for (int i = 0; i < rows; i++) {
			IDColumn id = (IDColumn) miniTable.getValueAt(i, 0); // ID in column
																	// 0
			// log.fine( "Row=" + i + " - " + id);
			if (id != null && id.isSelected())
				results.add(id.getRecord_ID());
		}

		if (results.size() == 0)
			return;
		log.config("Selected #" + results.size());
		setSelection(results);
	} // saveSelection	

	public void showMessage(String message, boolean error) {
	}

	public int getAD_Org_ID() {
		return AD_Org_ID;
	}

	public void setAD_Org_ID(int aD_Org_ID) {
		AD_Org_ID = aD_Org_ID;
	}

	public String getProductValue() {
		return productValue;
	}

	public void setProductValue(String productValue) {
		this.productValue = productValue;
	}

	public int getC_BPartner_ID() {
		return C_BPartner_ID;
	}

	public void setC_BPartner_ID(int c_BPartner_ID) {
		C_BPartner_ID = c_BPartner_ID;
	}

	public String getClassification() {
		return classification;
	}

	public void setClassification(String classification) {
		this.classification = classification;
	}

	public int getM_PriceList_ID() {
		return M_PriceList_ID;
	}

	public void setM_PriceList_ID(int m_PriceList_ID) {
		M_PriceList_ID = m_PriceList_ID;
	}

	public String getGroup1() {
		return group1;
	}

	public void setGroup1(String group1) {
		this.group1 = group1;
	}

	public String getGroup2() {
		return group2;
	}

	public void setGroup2(String group2) {
		this.group2 = group2;
	}

	public int getC_DocType_ID() {
		return C_DocType_ID;
	}

	public void setC_DocType_ID(int c_DocType_ID) {
		C_DocType_ID = c_DocType_ID;
	}
	
	public Date getDateRequired() {
		return dateRequired;
	}

	public void setDateRequired(Date date) {
		this.dateRequired = date;
	}
	
	public int getUser1_ID() {
		return User1_ID;
	}
	
	public void setUser1_ID(int User1_ID){
		this.User1_ID = User1_ID;
	}
	
	public int getC_Project_ID() {
		return C_Project_ID;
	}
	
	public void setC_Project_ID(int C_Project_ID){
		this.C_Project_ID = C_Project_ID;
	}
	
	public int getM_Requisition_ID() {
		return M_Requisition_ID;
	}

	public void setM_Requisition_ID(int M_Requisition_ID) {
		this.M_Requisition_ID = M_Requisition_ID;
	}
	
}
