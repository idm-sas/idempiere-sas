package id.co.databiz.awn.controller;

import id.co.databiz.awn.model.SystemID;
import id.co.databiz.awn.model.wrapper.ICOrder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.model.POWrapper;
import org.compiere.model.I_M_Product;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.Query;
import org.compiere.util.AdempiereSystemError;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.wf.MWFScenario;

public class OrderController {
	
	public static void beforePrepare(MOrder order){
		ICOrder orderCustom = POWrapper.create(order, ICOrder.class);
		
		// Price Limit Approval SYNC-256
		BigDecimal priceLimitAmt = DB.getSQLValueBD(order.get_TrxName(), 
				"(SELECT MIN(ol.PriceActual - ol.PriceLimit) FROM C_OrderLine ol WHERE ol.C_Order_ID = ? AND ol.M_Product_ID > 0)", 
				order.get_ID());
		if(priceLimitAmt!=null && priceLimitAmt.compareTo(Env.ZERO)<0){
			MWFScenario.setScenario(order,MWFScenario.SCENARIO_PRICE_LIMIT);
		} else {
			orderCustom.setZ_WFScenarioPrice_ID(0);
			order.saveEx();
		}
		// END Price Limit Approval SYNC-256
		
		// AWN-153 DP
		List<MOrderLine> lines = new Query(Env.getCtx(), MOrderLine.Table_Name, "IsActive = 'Y' AND C_Order_ID = ?", order.get_TrxName())
									.setParameters(new Object[]{order.get_ID()})
									.list();
		List<MOrderLine> dpList = new ArrayList<MOrderLine>();
		for(MOrderLine line : lines){
			if(line.getM_Product_ID()>0){
				I_M_Product product = line.getM_Product();
				int productCategoryID = product.getM_Product_Category_ID();
				if(productCategoryID==SystemID.PRODUCT_CATEGORY_DP_PEMBELIAN || productCategoryID==SystemID.PRODUCT_CATEGORY_DP_PENJUALAN){
					dpList.add(line);
				}
			}
		}
		
		if(!dpList.isEmpty()){
//			if(dpList.size()>1){
//				throw new AdempiereException("AWN-153 \nOnly one DP allowed in order lines");
//			}
			BigDecimal totalLines = order.getTotalLines();
			if(totalLines.compareTo(Env.ZERO) > 0){
				BigDecimal totalQtyDP = Env.ZERO;
				BigDecimal totalAmtDP = Env.ZERO;
				for (MOrderLine dpLine : dpList) {
					
//					dpLine.setQty(Env.ZERO);
					dpLine.setPrice(totalLines.divide(BigDecimal.valueOf(100), 12, BigDecimal.ROUND_HALF_UP));
					dpLine.saveEx();
					
					totalQtyDP = totalQtyDP.add(dpLine.getQtyEntered());
					totalAmtDP = totalAmtDP.add(dpLine.getLineNetAmt());
				}
				
				if (totalQtyDP.compareTo(Env.ZERO) != 0 || totalAmtDP.compareTo(Env.ZERO) != 0) {
					throw new AdempiereException("ISY-63 \nInvalid DP quantity / amount");
				}
			}
		}
	}
	
	public static void beforeChangeAndNew(MOrder order){
		if(order.getC_DocType_ID() == 0){
			order.setC_DocType_ID(order.getC_DocTypeTarget_ID());
			order.saveEx();
		}
	}

	public static void beforeVoid(MOrder order){
		// https://databiz.atlassian.net/browse/ISY-402
		// block Purchase Order Void if qtyDelivered or qtyInvoiced not 0 (zero)
		if (!order.isSOTrx()) {
			MOrderLine[] lines = order.getLines(false, null);
			for(MOrderLine line : lines) {
				if (line.getQtyDelivered().compareTo(Env.ZERO)!=0 || line.getQtyInvoiced().compareTo(Env.ZERO)!=0) {
					throw new AdempiereUserError(" | ISY-402 \nPlease cancel Material Receipt / Invoice first on line:"
				+line.getLine()+" | qtyDelivered :" + line.getQtyDelivered().intValue() 
				+ " | qtyInvoiced: "+line.getQtyInvoiced().intValue() , "Please cancel Material Receipt / Invoice first");
				}
			}
		}
		boolean isAllocated = order.get_ValueAsBoolean("IsAllocated");
		if (isAllocated == true) {
			throw new AdempiereException("<b>ISY-444 Failed to Re-activate/Void because there is already an MR/Shipment and Invoice</b>");
		}
	}
	
	public static void beforeReactivate(MOrder order) {
		boolean isAllocated = order.get_ValueAsBoolean("IsAllocated");
		if (isAllocated == true) {
			throw new AdempiereException("<b>ISY-444 Failed to Re-activate/Void because there is already an MR/Shipment and Invoice</b>");
		}
	}
}
