package id.co.databiz.awn.process;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.model.POWrapper;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MRequisition;
import org.compiere.model.MRequisitionLine;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;

import id.co.databiz.awn.model.I_C_OrderLineCustom;
import id.co.databiz.awn.model.I_M_RequisitionCustom;
import id.co.databiz.awn.model.I_M_RequisitionLineCustom;

/**
 * 	Generate PO from Requisition 
 *  @author Anozi Mada
 */
public class GeneratePurchaseOrder extends SvrProcess
{
	private int			p_AD_Org_ID = 0;
	private int 		p_M_Requisition_ID = 0;
	private int			p_AD_User_ID = 0;
	private int 		p_M_PriceList_ID = 0;
	private int 		p_C_Project_ID = 0;
	private int 		p_C_Campaign_ID = 0;
	private int 		p_User1_ID = 0;
	private Timestamp	p_DateDoc_From;
	private Timestamp	p_DateDoc_To;
	private Timestamp	p_DateRequired_From;
	private Timestamp	p_DateRequired_To;
	private int 		p_C_BPartner_ID = 0;
	private int 		p_C_DocTypeTarget_ID = 0;
	
	
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
			else if (name.equals("M_Requisition_ID"))
				p_M_Requisition_ID = para[i].getParameterAsInt();
			else if (name.equals("AD_User_ID"))
				p_AD_User_ID = para[i].getParameterAsInt();
			else if (name.equals("M_PriceList"))
				p_M_PriceList_ID = para[i].getParameterAsInt();
			else if (name.equals("C_Project_ID"))
				p_C_Project_ID = para[i].getParameterAsInt();
			else if (name.equals("C_Campaign_ID"))
				p_C_Campaign_ID = para[i].getParameterAsInt();
			else if (name.equals("User1_ID"))
				p_User1_ID = para[i].getParameterAsInt();
			else if (name.equals("DateDoc"))
			{
				p_DateDoc_From = (Timestamp)para[i].getParameter();
				p_DateDoc_To = (Timestamp)para[i].getParameter_To();
			}
			else if (name.equals("DateRequired"))
			{
				p_DateRequired_From = (Timestamp)para[i].getParameter();
				p_DateRequired_To = (Timestamp)para[i].getParameter_To();
			}
			else if (name.equals("C_BPartner_ID"))
				p_C_BPartner_ID = para[i].getParameterAsInt();
			else if (name.equals("C_DocTypeTarget_ID"))
				p_C_DocTypeTarget_ID = para[i].getParameterAsInt();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
	}	//	prepare
	
	/**
	 * 	Process
	 *	@return info
	 *	@throws Exception
	 */
	protected String doIt() throws Exception
	{
		log.info("AD_Org_ID=" + p_AD_Org_ID
				+ ", M_Requisition_ID=" + p_M_Requisition_ID
				+ ", M_PriceList_ID=" + p_M_PriceList_ID
				+ ", C_Project_ID=" + p_C_Project_ID
				+ ", C_Campaign_ID=" + p_C_Campaign_ID
				+ ", User1_ID=" + p_User1_ID
				+ ", DateDoc=" + p_DateDoc_From + "/" + p_DateDoc_To
				+ ", DateRequired=" + p_DateRequired_From + "/" + p_DateRequired_To
				+ ", C_BPartner_ID=" + p_C_BPartner_ID
				+ ", C_DocTypeTarget_ID=" + p_C_DocTypeTarget_ID);
		
		if (p_C_BPartner_ID <= 0) {
			throw new AdempiereException("@Mandatory@ @C_BPartner_ID@");
		}
		
		if (p_C_DocTypeTarget_ID <= 0) {
			throw new AdempiereException("@Mandatory@ @C_DocTypeTarget_ID@");
		}
		
		if (Env.getAD_Org_ID(Env.getCtx()) <= 0) {
			throw new AdempiereException("Please choose an Organization in login screen");
		}
		
		if (Env.getContextAsInt(Env.getCtx(), "#M_Warehouse_ID") <= 0) {
			throw new AdempiereException("Please choose a Warehouse in login screen");
		}
		
		if (p_M_Requisition_ID > 0) {
			MRequisition requisition = new MRequisition(getCtx(), p_M_Requisition_ID, get_TrxName());
			if (!isFullOrdered(requisition)) {
				createOrder(requisition);
			}
		} else {
			List<Object> params = new ArrayList<Object>();
			StringBuffer whereClause = new StringBuffer();
			whereClause.append("DocStatus = 'CO' ");
			
			if (p_AD_Org_ID > 0) {
				whereClause.append(" AND AD_Org_ID = ? ");
				params.add(p_AD_Org_ID);
			}
			
			if (p_AD_User_ID > 0) {
				whereClause.append(" AND AD_User_ID = ? ");
				params.add(p_AD_User_ID);
			}
			
			if (p_M_PriceList_ID > 0) {
				whereClause.append(" AND M_PriceList_ID = ? ");
				params.add(p_M_PriceList_ID);
			}
			
			if (p_C_Project_ID > 0) {
				whereClause.append(" AND C_Project_ID = ? ");
				params.add(p_C_Project_ID);
			}
			
			if (p_C_Campaign_ID > 0) {
				whereClause.append(" AND C_Campaign_ID = ? ");
				params.add(p_C_Campaign_ID);
			}
			
			if (p_User1_ID > 0) {
				whereClause.append(" AND User1_ID = ? ");
				params.add(p_User1_ID);
			}
			
			if (p_DateDoc_From != null) {
				whereClause.append(" AND r.DateDoc >= ?");
				params.add(p_DateDoc_From);
			}
			
			if (p_DateDoc_To != null) {
				whereClause.append(" AND r.DateDoc <= ?");
				params.add(p_DateDoc_To);
			}
			
			if (p_DateRequired_From != null) {
				whereClause.append(" AND r.DateRequired >= ?");
				params.add(p_DateRequired_From);
			}
			
			if (p_DateRequired_To != null) {
				whereClause.append(" AND r.DateRequired <= ?");
				params.add(p_DateRequired_To);
			}
			
			List<MRequisition> requisitionList = new Query(getCtx(), MRequisition.Table_Name, whereClause.toString(), get_TrxName())
				.setClient_ID()
				.setParameters(params)
				.list();
			for (MRequisition requisition : requisitionList) {
				if (!isFullOrdered(requisition)) {
					createOrder(requisition);
				}
			}
		}
		
		return "";
	}	//	doit
	
	private MOrder createOrder(MRequisition requisition) {
		
		int orgID = Env.getAD_Org_ID(Env.getCtx());
		int warehouseID = Env.getContextAsInt(Env.getCtx(), "#M_Warehouse_ID");
		
		I_M_RequisitionCustom requisitionCustom = POWrapper.create(requisition, I_M_RequisitionCustom.class);
		
		MOrder order = new MOrder(Env.getCtx(), 0, get_TrxName());
		order.setAD_Org_ID(orgID);
		order.setIsSOTrx(false);
		order.setDatePromised(requisition.getDateRequired());
		if(p_C_DocTypeTarget_ID > 0){
			order.setC_DocTypeTarget_ID(p_C_DocTypeTarget_ID);
		} else {
			order.setC_DocTypeTarget_ID();
		}
		order.setC_BPartner_ID(p_C_BPartner_ID);
		order.setM_PriceList_ID(requisition.getM_PriceList_ID());
		order.setSalesRep_ID(requisition.getAD_User_ID());
		order.setM_Warehouse_ID(warehouseID);
		order.setC_Project_ID(requisitionCustom.getC_Project_ID());
		order.setC_Campaign_ID(requisitionCustom.getC_Campaign_ID());
		order.setUser1_ID(requisitionCustom.getUser1_ID());
		order.setDescription(requisition.getDescription());
		order.saveEx();
		
		// Create lines
		List<MRequisitionLine> lines = new Query(getCtx(), MRequisitionLine.Table_Name, "M_Requisition_ID = ?", get_TrxName())
			.setOnlyActiveRecords(true)
			.setParameters(requisition.get_ID())
			.list();
		for (MRequisitionLine line : lines) {
			createOrderLine(order, line);
		}
		addLog(order.get_ID(), order.getDateOrdered(), order.getGrandTotal(), order.getDocumentNo(), order.get_Table_ID(), order.getC_Order_ID());
		return order;
	}
	
	private MOrderLine createOrderLine(MOrder order, MRequisitionLine requisitionLine) {
		MOrderLine orderLine = null;
		StringBuffer sql = new StringBuffer();
		sql.append("SELECT COALESCE(SUM(ol.QtyOrdered),0) ");
		sql.append("FROM C_OrderLine ol ");
		sql.append("INNER JOIN C_Order o ON (o.C_Order_ID = ol.C_Order_ID) ");
		sql.append("WHERE o.DocStatus NOT IN ('VO','RE') ");
		sql.append("AND ol.M_RequisitionLine_ID = ? ");
		BigDecimal qtyOrdered = DB.getSQLValueBD(get_TrxName(), sql.toString(), requisitionLine.get_ID());
		BigDecimal qtyToOrder = requisitionLine.getQty().subtract(qtyOrdered);
		
		if (qtyToOrder.compareTo(Env.ZERO) > 0) {
			orderLine = new MOrderLine(order);
			I_C_OrderLineCustom orderLineCustom = POWrapper.create(orderLine, I_C_OrderLineCustom.class);
			I_M_RequisitionLineCustom requisitionLineCustom = POWrapper.create(requisitionLine, I_M_RequisitionLineCustom.class);
			if(requisitionLineCustom.getDateRequired()!=null){
				orderLine.setDatePromised(requisitionLineCustom.getDateRequired());
			} else {
				orderLine.setDatePromised(requisitionLine.getParent().getDateRequired());
			}
			if (requisitionLine.getM_Product_ID() > 0) {
				orderLine.setM_Product_ID(requisitionLine.getM_Product_ID());
			}
			orderLine.setM_AttributeSetInstance_ID(requisitionLine.getM_AttributeSetInstance_ID());
			orderLine.setAD_Org_ID(order.getAD_Org_ID());
			orderLine.setQtyOrdered(qtyToOrder);
			orderLine.setQtyEntered(qtyToOrder.multiply(requisitionLineCustom.getQtyEntered()).divide(requisitionLine.getQty(), 12, BigDecimal.ROUND_HALF_UP));
			orderLine.setPrice();
			if (requisitionLine.getC_Charge_ID() > 0) {
				orderLine.setC_Charge_ID(requisitionLine.getC_Charge_ID());
				orderLine.setPrice(requisitionLine.getC_Charge().getChargeAmt());
			}
			if (requisitionLine.getC_UOM_ID() > 0 && requisitionLine.getM_Product_ID() > 0) {
				orderLine.setC_UOM_ID(requisitionLine.getC_UOM_ID());
			}
			orderLine.setC_Project_ID(requisitionLineCustom.getC_Project_ID());
			orderLine.setUser1_ID(requisitionLineCustom.getUser1_ID());
			orderLine.setDescription(requisitionLine.getDescription());
			orderLineCustom.setM_RequisitionLine_ID(requisitionLine.get_ID());
			orderLine.setC_Campaign_ID(requisitionLine.getParent().get_ValueAsInt("C_Campaign_ID"));
			orderLine.saveEx();
			requisitionLine.setC_OrderLine_ID(orderLine.getC_OrderLine_ID());
			requisitionLine.saveEx();
		}
		
		return orderLine;
	}
	
	private boolean isFullOrdered(MRequisition requisition) {
		boolean isFullOrdered = false;
		int fullOrderedLineCount = 0;
		
		StringBuffer sql = new StringBuffer();
		sql.append("SELECT COUNT(*) FROM M_RequisitionLine rl ");
		sql.append("INNER JOIN C_OrderLine ol ON (ol.M_RequisitionLine_ID = rl.M_RequisitionLine_ID) ");
		sql.append("INNER JOIN C_Order o ON (o.C_Order_ID = ol.C_Order_ID) ");
		sql.append("WHERE o.DocStatus NOT IN ('VO','RE') AND rl.M_RequisitionLine_ID = ? ");
		sql.append("GROUP BY rl.M_RequisitionLine_ID, rl.Qty ");
		sql.append("HAVING rl.Qty <= SUM(ol.QtyOrdered) ");
		
		List<MRequisitionLine> lines = Arrays.asList(requisition.getLines());
		for (MRequisitionLine line : lines) {
			int count = DB.getSQLValue(get_TrxName(), sql.toString(), line.get_ID());
			if (count > 0) {
				fullOrderedLineCount++;
			}
		}
		
		if (fullOrderedLineCount >= lines.size()) {
			isFullOrdered = true;
		}
		
		return isFullOrdered;
	}
}
