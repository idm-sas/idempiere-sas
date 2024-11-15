package id.co.databiz.sas.process;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.process.DocAction;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;

public class SAS250_BenefitDiscKSaldo extends SvrProcess {
	
	/** Invoice Date From		*/
	private Timestamp	p_DateInvoiced_From = null;
	/** Invoice Date To			*/
	private Timestamp	p_DateInvoiced_To = null;
	/** Invoice Date Ordered			*/
	private Timestamp	p_DateOrdered = null;
	private int p_AD_OrgTrx_ID = 0;
	private int p_AD_Org_ID = 0;
	private int p_C_Campaign_ID = 0;
	private boolean 	p_Processed = false;
	private int AD_User_ID = Env.getContextAsInt(Env.getCtx(), "#AD_User_ID");
	private MOrder m_order = null;
	private int	m_created = 0;

	
	@Override
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else if (name.equals("AD_Org_ID"))
			{
				p_AD_Org_ID = para[i].getParameterAsInt();
			}
			else if (name.equals("AD_OrgTrx_ID"))
			{
				p_AD_OrgTrx_ID = para[i].getParameterAsInt();
			}
			else if (name.equals("C_Campaign_ID"))
			{
				p_C_Campaign_ID = para[i].getParameterAsInt();
			}
			else if (name.equals("DateInvoiced"))
			{
				p_DateInvoiced_From = para[i].getParameterAsTimestamp();
				p_DateInvoiced_To = para[i].getParameter_ToAsTimestamp();
			}
			else if (name.equals("DateOrdered"))
			{
				p_DateOrdered = para[i].getParameterAsTimestamp();
			}
			else if (name.equals("Processed"))
			{
				p_Processed = "Y".equals(para[i].getParameter());
			}
			else
				log.log(Level.SEVERE, "prepare - Unknown Parameter: " + name);
		}
		
	}

	@Override
	protected String doIt() throws Exception {
		
		if(p_AD_Org_ID <=0 && p_AD_OrgTrx_ID <=0) {
			throw new AdempiereException("Param Org or OrgTrx Must must be filled in");
		}
		
		//Start
		insert_sas_calculate_saldo();
		
		statusUpdate("Create Order");

		//Create Order
			StringBuilder sql1 = new StringBuilder();
			sql1.append("select scs.sas_c_bpartnerngk_id, scs.ad_orgtrx_id, scs.c_campaign_id, coalesce(scs.percentage, 0) as percentage, ");
			sql1.append("coalesce(scs.valuemin, 0) as valuemin, coalesce(scs.valuemax, 0) as valuemax, scs.discountmodel, ");
			sql1.append("sum(scs.linenetamt) as totallinenetamt, sum(scs.qtyinvoiced) as totalqtyinvoiced ");
			sql1.append("from sas_calculate_saldo scs ");
			sql1.append("WHERE 1=1 ");
			sql1.append("AND scs.ad_pinstance_id = ").append(getAD_PInstance_ID());
			sql1.append(" group by 1,2,3,4,5,6,7 ");

			PreparedStatement pstmt = null;
			ResultSet rs = null;
			try
			{
				pstmt = DB.prepareStatement (sql1.toString(), get_TrxName());
				rs = pstmt.executeQuery();
				    
			    while(rs.next()) {
			    	
			    int NGKID = rs.getInt(1);
			    int OrgTrxID = rs.getInt(2);
			    int CampaignID = rs.getInt(3);
				BigDecimal Percentage = rs.getBigDecimal(4);
				BigDecimal ValueMin = rs.getBigDecimal(5);
				BigDecimal ValueMax = rs.getBigDecimal(6);
			    String DiscModel = rs.getString(7);
				BigDecimal LineNetAmt = rs.getBigDecimal(8);
				BigDecimal QtyInvoiced = rs.getBigDecimal(9);
				
				BigDecimal saldo = Env.ZERO;

			    
			    //Find BP
			    String bpsql ="select sc.c_bpartner_id   "
			    		+ "from sas_calculate_saldo sc "
			    		+ "join m_product p on p.m_product_id = sc.m_product_id  "
			    		+ "join c_bpartner bp on bp.c_bpartner_id = sc.c_bpartner_id and bp.sas_c_bpartnerngk_id = sc.sas_c_bpartnerngk_id "
			    		+ "left join z_campaignproductgroup zc on zc.c_campaign_id = sc.c_campaign_id "
			    		+ "WHERE bp.isactive = 'Y' "
			    		+ "AND TRUNC(sc.datefrom) >= " +DB.TO_DATE(p_DateInvoiced_From, true)+ " "
			    		+ "AND TRUNC(sc.dateto) <= " +DB.TO_DATE(p_DateInvoiced_To, true)+ " "
					    + "and sc.ad_orgtrx_id = ? "
			    		+ "and sc.c_campaign_id = ? "
			    		+ "and sc.sas_c_bpartnerngk_id = ? "
			    		+ "order by sc.c_bpartner_id fetch first 1 row only ";
		    	int bpartnerID = DB.getSQLValue(get_TrxName(), bpsql, OrgTrxID, CampaignID, NGKID);
		    	
		    	if (bpartnerID < 0) {
					throw new AdempiereException("BP NULL");
		    	}
		    	
		    	//Nilai Saldo
		    	if (DiscModel.contains("Benefit")) {
		            if((LineNetAmt.multiply(Percentage.divide(Env.ONEHUNDRED))).compareTo(ValueMax) > 0) {
		            	saldo = ValueMax;
		            } else {
		            	saldo = LineNetAmt.multiply(Percentage.divide(Env.ONEHUNDRED));
		            }
		        } else if (DiscModel.contains("LockIn")){
		        	if(QtyInvoiced.compareTo(ValueMin) >= 0) {
		            	saldo = LineNetAmt.multiply(Percentage.divide(Env.ONEHUNDRED));
		            } else {
		            	saldo = Env.ZERO;
		            }
		        } else if (DiscModel.contains("Trade")){
		        	if(LineNetAmt.compareTo(ValueMin) >= 0) {
		            	saldo = LineNetAmt.multiply(Percentage.divide(Env.ONEHUNDRED));
		            } else {
		            	saldo = Env.ZERO;
		            }
		        } 
			    
			    //Header Sales Order
			    MOrder order = new MOrder(Env.getCtx(), 0, get_TrxName());
				order.setAD_Org_ID(1000001); //sunter
				order.setAD_OrgTrx_ID(OrgTrxID);
				order.setIsSOTrx(true);
				order.setDateOrdered(p_DateOrdered);
				order.setDatePromised(p_DateOrdered);
				order.setC_DocTypeTarget_ID(550420); //(Saldo Disc K)
				order.setC_DocType_ID(550420);
				order.setC_BPartner_ID(bpartnerID);
				order.setM_PriceList_ID(1000005);
				order.setSalesRep_ID(1000002);
				order.setC_PaymentTerm_ID(1000009);
				order.setM_Warehouse_ID(1000019);
//				order.set_ValueOfColumn("FLNStatus", "Lain-lain");
//				order.setC_Project_ID(1000000);
				order.setC_Campaign_ID(p_C_Campaign_ID);
				order.setDescription(String.valueOf(getAD_PInstance_ID()));
				order.saveEx();
			    
				//Sales OrderLine
				MOrderLine orderLine = new MOrderLine(order);
				orderLine.setDatePromised(p_DateOrdered);
				orderLine.setAD_Org_ID(order.getAD_Org_ID());
				orderLine.setQtyOrdered(Env.ONE);
				orderLine.setQtyEntered(Env.ONE);
				orderLine.setPriceEntered(saldo);
				orderLine.setPriceList(saldo);
				orderLine.setPriceActual(saldo);
				orderLine.setLineNetAmt(saldo);
				orderLine.setC_Charge_ID(1000114);
				orderLine.setC_Project_ID(order.getC_Project_ID());
				orderLine.setDescription(order.getDescription());
				orderLine.setC_Campaign_ID(order.getC_Campaign_ID());
				orderLine.saveEx();
				
				order.setDocAction(DocAction.ACTION_Complete);
				// added AdempiereException 
				if (!order.processIt(DocAction.ACTION_Complete))
					throw new AdempiereException("Failed when processing document - " + order.getProcessMsg());
				// end added
				order.saveEx();
				
				statusUpdate("Update Data SAS_calculate_saldo");
		        StringBuffer sqlupdatedata = new StringBuffer(
						" update sas_calculate_saldo set c_orderline_id = "+orderLine.getC_OrderLine_ID()+""
						+ "where ad_pinstance_id = "+getAD_PInstance_ID()+" "
						+ "and sas_c_bpartnerngk_id = "+NGKID+" "
						+ "and ad_orgtrx_id = "+OrgTrxID+" "
						+ "and c_campaign_id = "+p_C_Campaign_ID+" "
		        		);
		        if (p_DateInvoiced_From != null && p_DateInvoiced_To != null)
		        	sqlupdatedata.append(" AND TRUNC(DateFrom) >= ").append(DB.TO_DATE(p_DateInvoiced_From, true))
						.append(" AND TRUNC(DateTo) <= ").append(DB.TO_DATE(p_DateInvoiced_To, true));
				if (p_DateInvoiced_From != null && p_DateInvoiced_To == null)
					sqlupdatedata.append(" AND TRUNC(DateFrom) >= ").append(DB.TO_DATE(p_DateInvoiced_From, true));
				if (p_DateInvoiced_From == null && p_DateInvoiced_To != null)
					sqlupdatedata.append(" AND TRUNC(DateTo) <= ").append(DB.TO_DATE(p_DateInvoiced_To, true));
		        PreparedStatement pstmtinject2 = null;
		        pstmtinject2 = DB.prepareStatement(sqlupdatedata.toString(), get_TrxName());
		        pstmtinject2.executeUpdate();
		        pstmtinject2.close();
			    
		        if(m_order == null || order.get_ID() != m_order.get_ID()){
		        	m_order = order;
					addLog(m_order.getC_Order_ID(), m_order.getDateOrdered(), null, m_order.getDocumentNo(), m_order.get_Table_ID(), m_order.getC_Order_ID());
					m_created++;
				}
			    
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
			
			return "@Created@ = " + m_created;
	}
	
	protected void insert_sas_calculate_saldo() throws IllegalStateException, SQLException {
		//Fill Table
		statusUpdate("Fill Data To SAS_calculate_saldo");
        StringBuffer sqlinjectdata = new StringBuffer(
				" INSERT INTO sas_calculate_saldo ("
				+ "ad_client_id, ad_org_id, created, createdby, updated, updatedby, ad_pinstance_id, ad_orgtrx_id, "
				+ "c_campaign_id, datefrom, dateto, c_invoiceline_id, m_product_id, qtyinvoiced, linenetamt, "
				+ "percentage, valuemin, valuemax, discountmodel, c_bpartner_id, sas_c_bpartnerngk_id )"
				+ "select distinct "
				+ "i.ad_client_id, "
				+ "i.ad_org_id, "
				+ "now(), "
				+ ""+AD_User_ID+",  "
				+ "now(), "
				+ ""+AD_User_ID+",  "
				+ ""+getAD_PInstance_ID()+",  "
				+ "i.ad_orgtrx_id,  "
				+ "snr.c_campaign_id, "
				+ "'"+p_DateInvoiced_From+"'::date, "
				+ "'"+p_DateInvoiced_To+"'::date, "
				+ "il.c_invoiceline_id, "
				+ "il.m_product_id, "
				+ "il.qtyinvoiced, "
				+ "il.linenetamt, "
				+ "coalesce (snr.percentage, 0) as percentage, "
				+ "coalesce (snr.valuemin, 0) as valuemin, "
				+ "coalesce (snr.valuemax, 0) as valuemax, "
				+ "c.discountmodel, "
				+ "i.c_bpartner_id, "
				+ "bp.sas_c_bpartnerngk_id "
				+ "from c_invoiceline il "
				+ "join c_invoice i on i.c_invoice_id = il.c_invoice_id "
				+ "join ad_orginfo oi on oi.ad_org_id = i.ad_orgtrx_id "
				+ "join m_product p on p.m_product_id = il.m_product_id "
				+ "join c_bpartner bp on bp.c_bpartner_id = i.c_bpartner_id "
				+ "join sas_c_bpartnerngk scb on scb.sas_c_bpartnerngk_id = bp.sas_c_bpartnerngk_id and scb.isactive = 'Y' "
				+ "join sas_ngk_rule snr on snr.sas_c_bpartnerngk_id = scb.sas_c_bpartnerngk_id "
				+ ((p_AD_Org_ID > 0) ? "and snr.ad_org_id = oi.parent_org_id " : "and snr.ad_orgtrx_id = i.ad_orgtrx_id ")
				+ "join c_campaign c on c.c_campaign_id = snr.c_campaign_id "
				+ "left join z_campaignproductgroup zc on zc.c_campaign_id = snr.c_campaign_id and zc.isactive = 'Y'"
				+ "WHERE i.issotrx = 'Y' AND i.docstatus in ('CO', 'CL') AND i.c_doctype_id in (1000003, 1000002) "
				+ "AND bp.sas_c_bpartnerngk_id is not null "
				+ "AND (zc.group2 IS NULL OR p.group2 = zc.group2) "
        		);
        if (p_DateInvoiced_From != null && p_DateInvoiced_To != null)
        	sqlinjectdata.append(" AND TRUNC(i.DateInvoiced) >= ").append(DB.TO_DATE(p_DateInvoiced_From, true))
				.append(" AND TRUNC(i.DateInvoiced) <= ").append(DB.TO_DATE(p_DateInvoiced_To, true));
		if (p_DateInvoiced_From != null && p_DateInvoiced_To == null)
			sqlinjectdata.append(" AND TRUNC(i.DateInvoiced) >= ").append(DB.TO_DATE(p_DateInvoiced_From, true));
		if (p_DateInvoiced_From == null && p_DateInvoiced_To != null)
			sqlinjectdata.append(" AND TRUNC(i.DateInvoiced) <= ").append(DB.TO_DATE(p_DateInvoiced_To, true));
        if (p_C_Campaign_ID > 0)
        	sqlinjectdata.append(" AND snr.c_campaign_id = ").append(p_C_Campaign_ID);
        if (p_AD_Org_ID > 0)
        	sqlinjectdata.append(" AND oi.parent_org_id = ").append(p_AD_Org_ID);
        if (p_AD_OrgTrx_ID > 0)
        	sqlinjectdata.append(" AND i.AD_OrgTrx_ID = ").append(p_AD_OrgTrx_ID);
        PreparedStatement pstmtinject1 = null;
        pstmtinject1 = DB.prepareStatement(sqlinjectdata.toString(), get_TrxName());
        pstmtinject1.executeUpdate();
        pstmtinject1.close();
        
	}
}