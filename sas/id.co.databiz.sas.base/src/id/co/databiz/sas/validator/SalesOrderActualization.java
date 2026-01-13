package id.co.databiz.sas.validator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBPartner;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MOrg;
import org.compiere.model.MOrgInfo;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.util.Env;

public class SalesOrderActualization {
	
	public static void PrepareActualizationOrder(MOrder order) {
		
		//taruh di eventhandler
//		if (!order.get_ValueAsBoolean("IsAffectPromo"))
//			return;
		
		MOrg orgtrx = MOrg.get(Env.getCtx(), order.getAD_OrgTrx_ID());
		MBPartner bp = MBPartner.get(Env.getCtx(), order.getC_BPartner_ID());
		MOrgInfo orginfo = MOrgInfo.get(Env.getCtx(), order.getAD_OrgTrx_ID());
		MOrg parentorg = MOrg.get(Env.getCtx(), orginfo.getParent_Org_ID());
		
		int ngkID = bp.get_ValueAsInt("SAS_C_BPartnerNGK_ID");
		
		//taruh di eventhandler
//		if (ngkID <= 0)
//			return;
		
		
		//Delete Record If Find
//		String sql = " delete from c_orderline co where c_charge_id = 2200031 and c_campaign_id is not null and "
//				+ "c_order_id ="+order.getC_Order_ID();
//		DB.executeUpdateEx(sql.toString(), order.get_TrxName());
//		try {
//			DB.commit(false, order.get_TrxName());
//		} catch (IllegalStateException | SQLException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		List<MOrderLine> deletelines = new Query(Env.getCtx(), MOrderLine.Table_Name, "C_Charge_ID = 1000114 And C_Campaign_ID is not null And C_Order_ID=?", order.get_TrxName())
				.setParameters(order.get_ID())
				.list();
		
		for (MOrderLine deleteline : deletelines) {
			deleteline.deleteEx(true);
		}
		
		
		
		// 	Cari Diskon Awal (Tanpa Disc K) --> Same As SAS-82
		BigDecimal totalPrice = Env.ZERO;
		
		List<MOrderLine> orderlines = new Query(Env.getCtx(), MOrderLine.Table_Name, "C_Order_ID=?", order.get_TrxName())
				.setParameters(order.get_ID())
				.list();
		
		for (MOrderLine line : orderlines) {
			totalPrice = totalPrice.add(line.getPriceList()
					.multiply(line.getQtyOrdered()));
		}
		
		BigDecimal discount = Env.ZERO;
		
		BigDecimal totallines = new Query(Env.getCtx(), MOrderLine.Table_Name, "C_Order_ID = ? ", order.get_TrxName())
				.setOnlyActiveRecords(true)
				.setParameters(new Object[]{order.getC_Order_ID()})
				.sum(MOrderLine.COLUMNNAME_LineNetAmt);
				
		if (totalPrice.compareTo(Env.ZERO) != 0) {
			discount = (totalPrice.subtract(totallines)).multiply(Env.ONEHUNDRED).divide(
					totalPrice, 2, RoundingMode.HALF_UP);
		}
		///////////

		int max = orgtrx.get_ValueAsInt("Limit_Discount");
		BigDecimal maxs = new BigDecimal(max);

		
		//Check MAX
		String maxsql ="select coalesce(CAST(arl.description AS NUMERIC),0) AS maxs "
				+ "from c_orderline ol "
				+ "join m_product p on p.m_product_id = ol.m_product_id "
				+ "left join ad_ref_list arl on arl.value = p.group3 and arl.ad_reference_id = 550208 "
				+ "where ol.c_order_id = ? "
				+ "order by ol.c_orderline_id fetch first 1 row only ";
		BigDecimal check = DB.getSQLValueBD(order.get_TrxName(), maxsql, order.getC_Order_ID());
		
		if(check.compareTo(Env.ZERO) > 0) {
			maxs = check;
		}
		
		if (maxs.compareTo(discount) < 0) {
			return;
		}

//		BigDecimal orderAmt = new Query(Env.getCtx(), MOrderLine.Table_Name, "C_Order_ID = ? ", order.get_TrxName())
//				.setOnlyActiveRecords(true)
//				.setParameters(new Object[]{order.getC_Order_ID()})
//				.sum(MOrderLine.COLUMNNAME_LineNetAmt);
		
//	 	Cari Nilai Order (Tanpa Disc K)
		BigDecimal orderAmt = Env.ZERO;
			
		List<MOrderLine> orderline = new Query(Env.getCtx(), MOrderLine.Table_Name, "C_Order_ID=? AND LineNetAmt > 0 ", order.get_TrxName())
					.setParameters(order.get_ID())
					.list();
			
		for (MOrderLine oline : orderline) {
			orderAmt = orderAmt.add(oline.getPriceList()
						.multiply(oline.getQtyOrdered()));
		}
		
//		BigDecimal maxs = new BigDecimal(max);
		BigDecimal PotonganDiscK = (maxs.subtract(discount).divide(Env.ONEHUNDRED)).multiply(orderAmt);
		
		//dibulatkan ke bawah dalam ribuan 
		PotonganDiscK = PotonganDiscK.divide(BigDecimal.valueOf(1000), 0, RoundingMode.DOWN).multiply(BigDecimal.valueOf(1000));        
//		PotonganDiscK = PotonganDiscK.setScale(2);

		StringBuilder sql1 = new StringBuilder();

			sql1.append("select snr.c_campaign_id, sum(ol.linenetamt) as saldo_budget ");
			sql1.append("from c_orderline ol ");
			sql1.append("join c_order o on o.c_order_id = ol.c_order_id ");
			sql1.append("join c_bpartner bp on bp.c_bpartner_id = o.c_bpartner_id  ");
//			if(orginfo.getParent_Org_ID() == 1000005) {
//				sql1.append("join sas_ngk_rule snr on snr.sas_c_bpartnerngk_id = bp.sas_c_bpartnerngk_id and snr.ad_org_id = 1000005 and snr.c_campaign_id = ol.c_campaign_id ");
//			}
			if(parentorg.getName() != null && parentorg.getName().startsWith("Philips")) {
				sql1.append("join sas_ngk_rule snr on snr.sas_c_bpartnerngk_id = bp.sas_c_bpartnerngk_id and snr.ad_org_id = "+parentorg.getAD_Org_ID()+" and snr.c_campaign_id = ol.c_campaign_id ");

			}
			else {
				sql1.append("join sas_ngk_rule snr on snr.sas_c_bpartnerngk_id = bp.sas_c_bpartnerngk_id and snr.ad_orgtrx_id = "+order.getAD_OrgTrx_ID()+" and snr.c_campaign_id = ol.c_campaign_id ");
			}
			sql1.append("join c_campaign c on c.c_campaign_id = snr.c_campaign_id ");
			sql1.append("WHERE 1=1 ");
//			if(orginfo.getParent_Org_ID() == 1000005) {
//				sql1.append("AND o.ad_orgtrx_id IN (select org.ad_org_id  from ad_org org "
//						+ "join ad_orginfo ao on ao.ad_org_id = org.ad_org_id "
//						+ "where ao.parent_org_id = 1000005 and org.isorgtrxdim = 'Y')");
//			}
//			if(parentorg.getName() != null && parentorg.getName().startsWith("Philips")) {
//				sql1.append("AND o.ad_orgtrx_id IN (select org.ad_org_id  from ad_org org "
//						+ "join ad_orginfo ao on ao.ad_org_id = org.ad_org_id "
//						+ "where ao.parent_org_id = "+parentorg.getAD_Org_ID()+" and org.isorgtrxdim = 'Y')");
//			}
//			else {
//				sql1.append("AND o.ad_orgtrx_id = ").append(order.getAD_OrgTrx_ID());
//			}
			sql1.append(" AND bp.sas_c_bpartnerngk_id = ").append(ngkID);
			sql1.append(" AND ").append(DB.TO_DATE(order.getDateOrdered(), true)).append(" >= TRUNC(COALESCE(snr.StartDate, C.StartDate)) ");
			sql1.append(" AND ").append(DB.TO_DATE(order.getDateOrdered(), true)).append(" <= TRUNC(COALESCE(snr.EndDate, C.EndDate)) ");
//			sql1.append(" AND o.dateordered BETWEEN c.startdate AND c.enddate ");
			sql1.append(" AND ol.c_charge_id in (2200031, 1000114) "
					+ "   AND("
					+ "    (o.c_doctype_id = 550420 and o.docstatus in ('CO','CL')) OR "
					+ "    (o.c_doctype_id in (550265,1000030) and o.docstatus in ('IP','CO','CL')) OR "
					+ "    (o.c_doctype_id in (1000046,1000047) and o.docstatus in ('IP','CO','CL')) OR "
					+ "    (o.c_doctype_id in (550270,1000027) and o.docstatus in ('IP','CO')) OR "
					+ "    (o.c_doctype_id in (550269,1000026) and o.docstatus in ('IP')) )  ");
			sql1.append("group by snr.c_campaign_id, c.startdate  ");
			sql1.append("HAVING SUM(ol.linenetamt) > 0  ");
			sql1.append("order by c.startdate ");
		

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement (sql1.toString(), order.get_TrxName());
			rs = pstmt.executeQuery();
			    
		    while(rs.next()) {
		    	
			    int CampaignID = rs.getInt(1);
				BigDecimal saldo = rs.getBigDecimal(2);
				
				if(PotonganDiscK.compareTo(saldo) > 0 ) {
					
					PotonganDiscK = PotonganDiscK.subtract(saldo);
		    		
					//Create Sales OrderLine
					MOrderLine orderLine = new MOrderLine(order);
					orderLine.setDatePromised(order.getDatePromised());
					orderLine.setAD_Org_ID(order.getAD_Org_ID());
					orderLine.setQtyOrdered(Env.ONE);
					orderLine.setQtyEntered(Env.ONE);
					orderLine.setPriceEntered(saldo.negate());
					orderLine.setPriceList(Env.ZERO);
					orderLine.setPriceActual(saldo.negate());
					orderLine.setC_Charge_ID(1000114);
					orderLine.setC_Project_ID(order.getC_Project_ID());
					orderLine.setDescription(order.getDescription());
					orderLine.setC_Campaign_ID(CampaignID);
					orderLine.setLineNetAmt(saldo.negate());
					orderLine.saveEx();
					
		    	}
		    	else {
		    		
		    		//Create Sales OrderLine
					MOrderLine orderLine = new MOrderLine(order);
					orderLine.setDatePromised(order.getDatePromised());
					orderLine.setAD_Org_ID(order.getAD_Org_ID());
					orderLine.setQtyOrdered(Env.ONE);
					orderLine.setQtyEntered(Env.ONE);
					orderLine.setPriceEntered(PotonganDiscK.negate());
					orderLine.setPriceList(Env.ZERO);
					orderLine.setPriceActual(PotonganDiscK.negate());
					orderLine.setC_Charge_ID(1000114);
					orderLine.setC_Project_ID(order.getC_Project_ID());
					orderLine.setDescription(order.getDescription());
					orderLine.setC_Campaign_ID(CampaignID);
					orderLine.setLineNetAmt(PotonganDiscK.negate());
					orderLine.saveEx();
		    		
		    		break;
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
		
	}
	public static void CheckPriceDiscount(MOrder order) {
				
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT COUNT(ol.c_orderline_id) FROM c_orderline ol ");
		sb.append("join c_campaign c on c.c_campaign_id = ol.c_campaign_id  ");
		sb.append("WHERE ol.C_Charge_ID = 1000114 AND ol.C_Campaign_ID is not null ");
		sb.append("and c.discountmodel is not null AND ol.C_Order_ID = ?    ");
		int count = DB.getSQLValue(order.get_TrxName(), sb.toString(), order.getC_Order_ID());
		
		if(count > 0){
			
			MBPartner bp = MBPartner.get(Env.getCtx(), order.getC_BPartner_ID());
			MOrgInfo orginfo = MOrgInfo.get(Env.getCtx(), order.getAD_OrgTrx_ID());
			MOrg parentorg = MOrg.get(Env.getCtx(), orginfo.getParent_Org_ID());
			
			int ngkID = bp.get_ValueAsInt("SAS_C_BPartnerNGK_ID");
			String error = null;
			
			if (ngkID <= 0)
			{
				throw new AdempiereException("No BP Join For Discount K Saldo");	
			}
		
			StringBuilder sql1 = new StringBuilder();
			sql1.append("select ol.line, ol.c_campaign_id, ol.linenetamt, coalesce(snr.startdate, c.startdate) as startdate, coalesce(snr.enddate, c.enddate) as enddate ");
			sql1.append("from c_orderline ol ");
			sql1.append("join c_campaign c on c.c_campaign_id = ol.c_campaign_id  ");
			sql1.append("join c_bpartner bp on bp.c_bpartner_id = ol.c_bpartner_id ");
			sql1.append("join sas_ngk_rule snr on snr.sas_c_bpartnerngk_id = bp.sas_c_bpartnerngk_id and snr.c_campaign_id = ol.c_campaign_id ");
			sql1.append("where ol.c_charge_id = 1000114 and ol.c_campaign_id is not null ");
			sql1.append("and c.discountmodel is not null and ol.c_order_id = ").append(order.getC_Order_ID());
			
			PreparedStatement pstmt = null;
			ResultSet rs = null;
			try
			{
				pstmt = DB.prepareStatement (sql1.toString(), order.get_TrxName());
				rs = pstmt.executeQuery();
				
				// Check if the ResultSet is empty
//			    if (!rs.isBeforeFirst()) { // isBeforeFirst() returns false if the ResultSet is empty
//			        throw new AdempiereException("No data found in the BP Join Rule MasterData");
////			    	error = "No data found in the BP Join Rule MasterData";
//			    }
				    
			    while(rs.next()) {
			    	
			    	int line = rs.getInt(1);
				    int CampaignID = rs.getInt(2);
					BigDecimal amt = rs.getBigDecimal(3);
					Timestamp startdate = rs.getTimestamp(4);
					Timestamp enddate = rs.getTimestamp(5);
					
					if (amt.compareTo(Env.ZERO) > 0) {
						throw new AdempiereException("-Disc Khusus- LineNetAmt Must Be < 0 ");
					}
					
//					if(!(order.getDateOrdered().after(startdate)&& order.getDateOrdered().before(enddate))) {
//						throw new AdempiereException("DateOrdered is not in range the campaign date, on Line (" + line +") ");
//					}
					
			        if (!(order.getDateOrdered().compareTo(startdate) >= 0 && order.getDateOrdered().compareTo(enddate) <= 0)) {
						throw new AdempiereException("DateOrdered is not in range the campaign date, on Line (" + line +") ");
					}
					
					StringBuilder sqlcheck = new StringBuilder();

					sqlcheck.append("select sum(ol.linenetamt) as saldo_budget ");
					sqlcheck.append("from c_orderline ol ");
					sqlcheck.append("join c_order o on o.c_order_id = ol.c_order_id ");
					sqlcheck.append("join c_bpartner bp on bp.c_bpartner_id = o.c_bpartner_id  ");
//					if(orginfo.getParent_Org_ID() == 1000005) {
//						sqlcheck.append("join sas_ngk_rule snr on snr.sas_c_bpartnerngk_id = bp.sas_c_bpartnerngk_id and snr.ad_org_id = 1000005 and snr.c_campaign_id = ol.c_campaign_id ");
//					}
					if(parentorg.getName() != null && parentorg.getName().startsWith("Philips")) {
						sqlcheck.append("join sas_ngk_rule snr on snr.sas_c_bpartnerngk_id = bp.sas_c_bpartnerngk_id and snr.ad_org_id = "+parentorg.getAD_Org_ID()+" and snr.c_campaign_id = ol.c_campaign_id ");

					}
					else {
						sqlcheck.append("join sas_ngk_rule snr on snr.sas_c_bpartnerngk_id = bp.sas_c_bpartnerngk_id and snr.ad_orgtrx_id = "+order.getAD_OrgTrx_ID()+" and snr.c_campaign_id = ol.c_campaign_id ");
					}
					sqlcheck.append("join c_campaign c on c.c_campaign_id = snr.c_campaign_id ");
					sqlcheck.append("WHERE 1=1 ");
//					if(orginfo.getParent_Org_ID() == 1000005) {
//						sqlcheck.append("AND o.ad_orgtrx_id IN (select org.ad_org_id  from ad_org org "
//								+ "join ad_orginfo ao on ao.ad_org_id = org.ad_org_id "
//								+ "where ao.parent_org_id = 1000005 and org.isorgtrxdim = 'Y')");
//					}
//					if(parentorg.getName() != null && parentorg.getName().startsWith("Philips")) {
//						sqlcheck.append("AND o.ad_orgtrx_id IN (select org.ad_org_id  from ad_org org "
//								+ "join ad_orginfo ao on ao.ad_org_id = org.ad_org_id "
//								+ "where ao.parent_org_id = "+parentorg.getAD_Org_ID()+" and org.isorgtrxdim = 'Y')");
//					}
//					else {
//						sqlcheck.append("AND o.ad_orgtrx_id = ").append(order.getAD_OrgTrx_ID());
//					}
					sqlcheck.append(" AND bp.sas_c_bpartnerngk_id = ").append(ngkID);
//					sqlcheck.append(" AND o.dateordered BETWEEN c.startdate AND c.enddate ");
					sqlcheck.append(" AND snr.c_campaign_id = ? ");
					sqlcheck.append(" AND ol.c_charge_id in (2200031, 1000114) "
							+ "   AND("
							+ "    (o.c_doctype_id = 550420 and o.docstatus in ('CO','CL')) OR "
							+ "    (o.c_doctype_id in (550265,1000030) and o.docstatus in ('IP','CO','CL')) OR "
							+ "    (o.c_doctype_id in (1000046,1000047) and o.docstatus in ('IP','CO','CL')) OR "
							+ "    (o.c_doctype_id in (550270,1000027) and o.docstatus in ('IP','CO')) OR "
							+ "    (o.c_doctype_id in (550269,1000026) and o.docstatus in ('IP')) )  ");
					sqlcheck.append("group by snr.c_campaign_id, c.startdate ");
					sqlcheck.append("order by c.startdate desc ");
					
					BigDecimal saldo = DB.getSQLValueBD(order.get_TrxName(), sqlcheck.toString(), CampaignID);
					
					if(saldo == null) {
						throw new AdempiereException("No Budget Balance Available for the campaign on Line (" + line +") ");
					}
					
					if(order.getDocStatus().equals("IP")) {
						saldo = saldo.add(amt.negate());
					}
					
					if((amt.negate()).compareTo(saldo) > 0){
//						error = "LineNetAmt ("+amt+") Exceeds the Budget Balance ("+saldo+") in Line  " + line;
						throw new AdempiereException("LineNetAmt ("+amt+") Exceeds the Budget Balance ("+saldo+") in Line  " + line);					}
					
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
						
		}
	}

}