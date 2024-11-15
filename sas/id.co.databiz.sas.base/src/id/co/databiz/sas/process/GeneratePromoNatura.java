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

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.exceptions.FillMandatoryException;
import org.adempiere.model.MPromotion;
import org.adempiere.model.MPromotionDistribution;
import org.adempiere.model.MPromotionGroup;
import org.adempiere.model.MPromotionGroupLine;
import org.adempiere.model.MPromotionLine;
import org.adempiere.model.MPromotionPreCondition;
import org.adempiere.model.MPromotionReward;
import org.compiere.model.MProduct;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;

/**
 *	Generate Promo Natura
 *	
 *  @author Anozi Mada
 */
public class GeneratePromoNatura extends SvrProcess
{	
	private String		p_Name = null;
	private int			p_C_Campaign_ID = 0;
	private int			p_AD_OrgTrx_ID = 0;
	private int			p_C_Charge_ID = 0;
	private Timestamp	p_StartDate = null;
	private Timestamp	p_EndDate = null;
	private BigDecimal	p_Qty = null;
	private BigDecimal	p_QtyReward = null;
	
	private int			counter = 0;
	
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
			else if (name.equals("Name"))
				p_Name = para[i].getParameterAsString();
			else if (name.equals("C_Campaign_ID"))
				p_C_Campaign_ID = para[i].getParameterAsInt();
			else if (name.equals("AD_OrgTrx_ID"))
				p_AD_OrgTrx_ID = para[i].getParameterAsInt();
			else if (name.equals("C_Charge_ID"))
				p_C_Charge_ID = para[i].getParameterAsInt();
			else if (name.equals("StartDate"))
				p_StartDate = para[i].getParameterAsTimestamp();
			else if (name.equals("EndDate"))
				p_EndDate = para[i].getParameterAsTimestamp();
			else if (name.equals("Qty"))
				p_Qty = para[i].getParameterAsBigDecimal();
			else if (name.equals("QtyReward"))
				p_QtyReward = para[i].getParameterAsBigDecimal();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
	}	//	prepare

	/**
	 *  Perform process.
	 *  @return Message (clear text)
	 *  @throws Exception if not successful
	 */
	protected String doIt() throws Exception
	{
		if (p_Name == null || p_Name.isEmpty()) {
			throw new FillMandatoryException("Name");
		}
		if (p_C_Charge_ID <= 0) {
			throw new FillMandatoryException("C_Charge_ID");
		}
		if (p_StartDate == null) {
			throw new FillMandatoryException("StartDate");
		}
		if (p_EndDate == null) {
			throw new FillMandatoryException("EndDate");
		}
		if (p_Qty == null) {
			throw new FillMandatoryException("Qty");
		}
		if (p_QtyReward == null) {
			throw new FillMandatoryException("QtyReward");
		}
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT p.M_Product_ID ");
		sql.append("FROM M_Product p ");
		sql.append("INNER JOIN T_Selection s ON (s.T_Selection_ID = p.M_Product_ID) ");
		sql.append("WHERE s.AD_PInstance_ID = ?");
		
		MProduct product = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement (sql.toString(), get_TrxName());
			DB.setParameters(pstmt, new Object[]{getAD_PInstance_ID()});
		    rs = pstmt.executeQuery();
		    while(rs.next()) {
		    	product = MProduct.get(getCtx(), rs.getInt("M_Product_ID"));
		    	MPromotionGroup promoGroup = null;
		    	
		    	// search existing promotion group if any
		    	List<MPromotionGroupLine> groupLineList = new Query(getCtx(), MPromotionGroupLine.Table_Name, "M_Product_ID = ?", get_TrxName())
		    		.setOnlyActiveRecords(true)
		    		.setParameters(product.get_ID())
		    		.list();
		    	for (MPromotionGroupLine groupLine : groupLineList) {
		    		int lineCount = new Query(getCtx(), MPromotionGroupLine.Table_Name, "M_PromotionGroup_ID = ?", get_TrxName())
		    			.setOnlyActiveRecords(true)
		    			.setParameters(groupLine.getM_PromotionGroup_ID())
		    			.count();
		    		if (lineCount == 1) {
		    			promoGroup = new MPromotionGroup(getCtx(), groupLine.getM_PromotionGroup_ID(), get_TrxName());
		    			break;
		    		}
		    	}
		    	
		    	// create one if not already exists
		    	if (promoGroup == null) {
	    			promoGroup = new MPromotionGroup(getCtx(), 0, get_TrxName());
	    			promoGroup.setName("P#" + product.getValue() + "#" + product.getName());
	    			promoGroup.saveEx();
	    			
	    			MPromotionGroupLine promoGroupLine = new MPromotionGroupLine(getCtx(), 0, get_TrxName());
	    			promoGroupLine.setM_PromotionGroup_ID(promoGroup.get_ID());
	    			promoGroupLine.setM_Product_ID(product.get_ID());
	    			promoGroupLine.saveEx();
	    		}
		    	
		    	// Begin create promotion
		    	MPromotion promotion = new MPromotion(getCtx(), 0, get_TrxName());
		    	promotion.setAD_Org_ID(0);
		    	promotion.setName(p_Name + " " + product.getValue());
		    	promotion.setDescription(p_Name + " " + promoGroup.getName());
		    	if (p_C_Campaign_ID > 0) {
		    		promotion.setC_Campaign_ID(p_C_Campaign_ID);
		    	}
		    	promotion.saveEx();
		    	
		    	// Pre Condition
		    	MPromotionPreCondition condition = new MPromotionPreCondition(getCtx(), 0, get_TrxName());
		    	condition.setM_Promotion_ID(promotion.get_ID());
		    	condition.setAD_Org_ID(promotion.getAD_Org_ID());
		    	if (p_AD_OrgTrx_ID > 0) {
		    		condition.set_ValueOfColumn("AD_OrgTrx_ID", p_AD_OrgTrx_ID);
		    	}
		    	condition.setStartDate(p_StartDate);
		    	condition.setEndDate(p_EndDate);
		    	condition.saveEx();
		    	
		    	// Promotion Line
		    	MPromotionLine lineCondition = new MPromotionLine(getCtx(), 0, get_TrxName());
		    	lineCondition.setM_Promotion_ID(promotion.get_ID());
		    	lineCondition.setAD_Org_ID(promotion.getAD_Org_ID());
		    	lineCondition.setM_PromotionGroup_ID(promoGroup.get_ID());
		    	lineCondition.set_ValueOfColumn("Line", 10);
		    	lineCondition.setIsMandatoryPL(true);
		    	lineCondition.saveEx();
		    	
		    	MPromotionLine lineReward = new MPromotionLine(getCtx(), 0, get_TrxName());
		    	lineReward.setM_Promotion_ID(promotion.get_ID());
		    	lineReward.setAD_Org_ID(promotion.getAD_Org_ID());
		    	lineReward.setM_PromotionGroup_ID(promoGroup.get_ID());
		    	lineReward.set_ValueOfColumn("Line", 20);
		    	lineReward.setIsMandatoryPL(false);
		    	lineReward.saveEx();
		    	
		    	// Promotion Distribution
		    	MPromotionDistribution distCondition = new MPromotionDistribution(getCtx(), 0, get_TrxName());
		    	distCondition.setM_Promotion_ID(promotion.get_ID());
		    	distCondition.setAD_Org_ID(promotion.getAD_Org_ID());
		    	distCondition.setSeqNo(10);
		    	distCondition.setM_PromotionLine_ID(lineCondition.get_ID());
		    	distCondition.setOperation(MPromotionDistribution.OPERATION_GtEq);
		    	distCondition.setQty(p_Qty);
		    	distCondition.setDistributionType(MPromotionDistribution.DISTRIBUTIONTYPE_Min);
		    	distCondition.setDistributionSorting(MPromotionDistribution.DISTRIBUTIONSORTING_Ascending);
		    	distCondition.set_ValueOfColumn("IsReward", false);
		    	distCondition.saveEx();
		    	
		    	MPromotionDistribution distReward = new MPromotionDistribution(getCtx(), 0, get_TrxName());
		    	distReward.setM_Promotion_ID(promotion.get_ID());
		    	distReward.setAD_Org_ID(promotion.getAD_Org_ID());
		    	distReward.setSeqNo(20);
		    	distReward.setM_PromotionLine_ID(lineReward.get_ID());
		    	distReward.setOperation(MPromotionDistribution.OPERATION_GtEq);
		    	distReward.setQty(p_QtyReward);
		    	distReward.setDistributionType(MPromotionDistribution.DISTRIBUTIONTYPE_Min);
		    	distReward.setDistributionSorting(MPromotionDistribution.DISTRIBUTIONSORTING_Ascending);
		    	distReward.set_ValueOfColumn("IsReward", true);
		    	distReward.saveEx();
		    	
		    	// Promotion Reward
		    	MPromotionReward reward = new MPromotionReward(getCtx(), 0, get_TrxName());
		    	reward.setM_Promotion_ID(promotion.get_ID());
		    	reward.setAD_Org_ID(promotion.getAD_Org_ID());
		    	reward.setSeqNo(10);
		    	reward.setIsForAllDistribution(false);
		    	reward.setIsSameDistribution(false);
		    	reward.setM_PromotionDistribution_ID(distCondition.get_ID());
		    	reward.setM_TargetDistribution_ID(distReward.get_ID());
		    	reward.setRewardType(MPromotionReward.REWARDTYPE_Percentage);
		    	reward.setAmount(Env.ONEHUNDRED);
		    	reward.setQty(Env.ONE);
		    	reward.setDistributionSorting(MPromotionReward.DISTRIBUTIONSORTING_Ascending);
		    	reward.setC_Charge_ID(p_C_Charge_ID);
		    	if (p_C_Campaign_ID > 0) {
		    		reward.set_ValueOfColumn("C_Campaign_ID", p_C_Campaign_ID);
		    	}
		    	reward.saveEx();
		    	
		    	addLog(promotion.get_ID(), null, null, promotion.getName(), MPromotion.Table_ID, promotion.get_ID());
		    	counter++;
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
		
		return "@Created@: " + counter;
	}	//	doIt

}	//	Generate Promo Natura