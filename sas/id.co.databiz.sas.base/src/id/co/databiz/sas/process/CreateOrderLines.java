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

import id.co.databiz.sas.model.MBPRule;
import id.co.databiz.sas.model.MDiscountList;

import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.exceptions.FillMandatoryException;
import org.compiere.model.MCurrency;
import org.compiere.model.MDiscountSchema;
import org.compiere.model.MDiscountSchemaBreak;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MPriceList;
import org.compiere.model.MProduct;
import org.compiere.model.MProductPricing;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;

/**
 *	Create Order Lines
 *	
 *  @author Anozi Mada
 *  
 */
public class CreateOrderLines extends SvrProcess
{
	private int			p_C_OrderLine_ID = 0;
	private int			p_M_Product_ID = 0;
	private int			p_SAS_DiscountList_ID = 0;
	private int			p_C_Campaign_ID = 0;
	private int			p_C_Charge_ID = 0;
	
	private BigDecimal	p_Qty = Env.ZERO;
	
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
			else if (name.equals("C_OrderLine_ID"))
				p_C_OrderLine_ID = para[i].getParameterAsInt();
			else if (name.equals("M_Product_ID"))
				p_M_Product_ID = para[i].getParameterAsInt();
			else if (name.equals("SAS_DiscountList_ID"))
				p_SAS_DiscountList_ID = para[i].getParameterAsInt();
			else if (name.equals("C_Campaign_ID"))
				p_C_Campaign_ID = para[i].getParameterAsInt();
			else if (name.equals("C_Charge_ID"))
				p_C_Charge_ID = para[i].getParameterAsInt();
			else if (name.equals("Qty"))
				p_Qty = para[i].getParameterAsBigDecimal();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
		
		if (getRecord_ID() > 0 && p_C_OrderLine_ID == 0) {
			p_C_OrderLine_ID = getRecord_ID();
		}
	}	//	prepare

	/**
	 *  Perform process.
	 *  @return Message (clear text)
	 *  @throws Exception if not successful
	 */
	protected String doIt() throws Exception
	{
		if (log.isLoggable(Level.INFO)) log.info("M_Product_ID=" + p_M_Product_ID 
			+ ", Qty=" + p_Qty
			+ ", SAS_DiscountList_ID=" + p_SAS_DiscountList_ID
			+ ", C_Charge_ID=" + p_C_Charge_ID
			+ ", C_Campaign_ID=" + p_C_Campaign_ID);
		
		if (p_Qty.compareTo(Env.ZERO) == 0)
			throw new IllegalArgumentException("Zero Qty");
		if (p_M_Product_ID == 0) {
			throw new FillMandatoryException("@M_Product_ID@");
		}
		
		MOrderLine orderLine = new MOrderLine(getCtx(), p_C_OrderLine_ID, get_TrxName());
		MOrder order = new MOrder(getCtx(), orderLine.getC_Order_ID(), get_TrxName());
		MProduct product = MProduct.get(getCtx(), p_M_Product_ID);
		MCurrency currency = MCurrency.get(getCtx(), order.getC_Currency_ID()) ;
		MPriceList mPriceList = MPriceList.get(getCtx(), order.getM_PriceList_ID(), null);
		MOrderLine lineProduct = new MOrderLine(order);
		lineProduct.setM_Product_ID(p_M_Product_ID, true);
		lineProduct.setQty(p_Qty);
		if (p_C_Campaign_ID > 0) {
			lineProduct.setC_Campaign_ID(p_C_Campaign_ID);
		}
		MProductPricing pp = new MProductPricing (p_M_Product_ID, order.getC_BPartner_ID(), p_Qty, order.isSOTrx());
		pp.setM_PriceList_ID(order.getM_PriceList_ID());
		String sql = "SELECT plv.M_PriceList_Version_ID "
				+ "FROM M_PriceList_Version plv "
				+ "WHERE plv.M_PriceList_ID=? "						//	1
				+ " AND plv.ValidFrom <= ? "
				+ "ORDER BY plv.ValidFrom DESC";
		int M_PriceList_Version_ID = DB.getSQLValueEx(null, sql, order.getM_PriceList_ID(), order.getDateOrdered());
		pp.setM_PriceList_Version_ID(M_PriceList_Version_ID);
		pp.setPriceDate(order.getDateOrdered());
		lineProduct.setPriceList(pp.getPriceList());
		lineProduct.setPriceLimit(pp.getPriceLimit());
		lineProduct.setPriceActual(pp.getPriceStd());
		lineProduct.setPriceEntered(pp.getPriceStd());
		lineProduct.setDiscount(pp.getDiscount());
		String description = product.getName();
		if(product.getDescription()!=null && !product.getDescription().isEmpty()){
			description += ", " + product.getDescription();
		}
		lineProduct.setDescription(description);
		MBPRule bpRule = new Query(getCtx(), MBPRule.Table_Name, "C_BPartner_ID = ? AND AD_OrgTrx_ID = ?", null)
			.setOnlyActiveRecords(true)
			.setParameters(order.getC_BPartner_ID(), order.getAD_OrgTrx_ID())
			.first();
		if (bpRule != null && bpRule.getM_DiscountSchema_ID() > 0) {
			MDiscountSchema schema = new MDiscountSchema(Env.getCtx(), bpRule.getM_DiscountSchema_ID(), null);
			List<MDiscountSchemaBreak> breakList = new Query(Env.getCtx(), MDiscountSchemaBreak.Table_Name, 
					"M_DiscountSchema_ID = ?", null)
				.setOnlyActiveRecords(true)
				.setParameters(schema.get_ID())
				.setOrderBy(MDiscountSchemaBreak.COLUMNNAME_SeqNo)
				.list();
			BigDecimal discount = Env.ZERO;
			MDiscountSchemaBreak discountSchemaBreak = null;
			for (MDiscountSchemaBreak dsb : breakList) {
				if (dsb.get_ValueAsString("Group1") != null && !dsb.get_ValueAsString("Group1").isEmpty()) {
					if (dsb.get_ValueAsString("Group1").equals(product.getGroup1())) {
						discount = dsb.getBreakDiscount();
						discountSchemaBreak = dsb;
						break;
					}
				}
			}
			if (discountSchemaBreak != null) {
				int discountListID = discountSchemaBreak.get_ValueAsInt("SAS_DiscountList_ID");
				if (discountListID > 0) {
					int precision = currency.getStdPrecision();
					if (mPriceList.getStandardPrecision() > precision) {
						precision = mPriceList.getStandardPrecision();
					}
					BigDecimal calculatedPrice = MDiscountList.getCalculatedPrice(discountListID, lineProduct.getPriceActual(), precision);
					lineProduct.setPriceActual(calculatedPrice);
					lineProduct.setPriceEntered(calculatedPrice);
					lineProduct.set_ValueOfColumn("Discount", discount);
					lineProduct.set_ValueOfColumn("Std_Discount", discount);
					lineProduct.set_ValueOfColumn("SAS_DiscountList_ID", discountListID);
				}
			}
		}
		
		if (p_SAS_DiscountList_ID > 0) {
			lineProduct.set_ValueOfColumn("SAS_DiscountList_ID", p_SAS_DiscountList_ID);
			int precision = order.getC_Currency().getStdPrecision();
			if (mPriceList.getStandardPrecision() > precision) {
				precision = mPriceList.getStandardPrecision();
			}
			BigDecimal calculatedPrice = MDiscountList.getCalculatedPrice(p_SAS_DiscountList_ID, lineProduct.getPriceList(), precision);
			lineProduct.setPriceActual(calculatedPrice);
			lineProduct.setPriceEntered(calculatedPrice);
			
			if (lineProduct.getPriceList().compareTo(Env.ZERO) != 0) {
				BigDecimal discount = lineProduct.getPriceList().subtract(calculatedPrice)
						.multiply(Env.ONEHUNDRED)
						.divide(lineProduct.getPriceList(), precision, BigDecimal.ROUND_HALF_UP);
				lineProduct.setDiscount(discount);
			}
		}
		
		lineProduct.setLine(orderLine.getLine() + 1);
		lineProduct.saveEx();
		
		if (p_C_Charge_ID > 0) {
			MOrderLine lineCharge = new MOrderLine(order);
			lineCharge.setC_Charge_ID(p_C_Charge_ID);
			lineCharge.setQty(p_Qty);
			lineCharge.setPrice(lineProduct.getPriceActual().negate());
			lineCharge.setC_Campaign_ID(lineProduct.getC_Campaign_ID());
			lineCharge.setLine(lineProduct.getLine() + 1);
			lineCharge.saveEx();
		}
		
		return "@C_OrderLine_ID@ @Created@";
	}	//	doIt

}	//	CopyOrder