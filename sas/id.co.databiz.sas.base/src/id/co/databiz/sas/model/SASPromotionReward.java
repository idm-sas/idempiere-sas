/******************************************************************************
 * Copyright (C) 2009 Low Heng Sin                                            *
 * Copyright (C) 2009 Idalica Corporation                                     *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *****************************************************************************/
package id.co.databiz.sas.model;

import id.co.databiz.sas.SASSystemID;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.model.MPromotion;
import org.adempiere.model.MPromotionDistribution;
import org.adempiere.model.MPromotionGroupLine;
import org.adempiere.model.MPromotionLine;
import org.adempiere.model.MPromotionReward;
import org.adempiere.model.POWrapper;
import org.compiere.model.I_M_Promotion;
import org.compiere.model.I_M_PromotionDistribution;
import org.compiere.model.I_M_PromotionLine;
import org.compiere.model.I_M_PromotionReward;
import org.compiere.model.MCampaign;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProduct;
import org.compiere.model.MProductPricing;
import org.compiere.model.MTable;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.util.Env;

/**
 *
 * @author hengsin
 *
 */
public class SASPromotionReward {

	public static void generateRewards(MOrder order) throws Exception {
		//key = C_OrderLine, value = Qty to distribution
		Map<Integer, BigDecimal> orderLineQty = new LinkedHashMap<Integer, BigDecimal>();
		Map<Integer, BigDecimal> orderLineAmt = new LinkedHashMap<Integer, BigDecimal>();
		Map<Integer, MOrderLine> orderLineIndex = new HashMap<Integer, MOrderLine>();
		List<MOrderLine> list = new Query(Env.getCtx(), MOrderLine.Table_Name, 
				"C_Order_ID = ? AND IsAffectPromo = 'Y'", order.get_TrxName())
			.setOnlyActiveRecords(true)
			.setParameters(order.get_ID())
			.list();
		MOrderLine[] lines = list.toArray(new MOrderLine[list.size()]);
		for (MOrderLine ol : lines) {
			if (ol.getM_Product_ID() > 0 && ol.get_ValueAsInt("M_PromotionReward_ID") <= 0) {
				if (ol.getQtyOrdered().signum() > 0) {
					orderLineQty.put(ol.getC_OrderLine_ID(), ol.getQtyOrdered());
					orderLineIndex.put(ol.getC_OrderLine_ID(), ol);
					if (ol.getM_Promotion_ID() > 0) {
						orderLineAmt.put(ol.getC_OrderLine_ID(), Env.ONE);
					} else {
						orderLineAmt.put(ol.getC_OrderLine_ID(), ol.getLineNetAmt());
					}
				}
			}
		}
		if (orderLineQty.isEmpty()) return;

		Map<Integer, List<Integer>> promotions = SASPromotionReward.findM_Promotion_ID(order);

		if (promotions == null || promotions.isEmpty()) return;

//		BigDecimal orderAmount = order.getGrandTotal();

		//key = M_PromotionDistribution_ID, value = C_OrderLine_ID and Qty
		Map<Integer, DistributionSet> distributions = new LinkedHashMap<Integer, DistributionSet>();

		//<M_PromotionDistribution_ID, DistributionSorting>
		Map<Integer, String> sortingType = new HashMap<Integer, String>();
		OrderLineComparator olComparator = new OrderLineComparator(orderLineIndex);
		//distribute order lines
		for (Map.Entry<Integer, List<Integer>> entry : promotions.entrySet()) {
			MPromotion promotion = new MPromotion(Env.getCtx(), entry.getKey(), null);
			List<Integer> processedProductList = new ArrayList<Integer>();
			boolean isNatura = promotion.get_ValueAsBoolean("IsNatura");
			BigDecimal orderAmount = SASPromotionRule.getAmount(order, promotion.get_ValueAsBoolean("IsDiscountAllowedOnTotal"));
			Query query = new Query(Env.getCtx(), MTable.get(order.getCtx(), I_M_PromotionDistribution.Table_ID),
					"M_PromotionDistribution.M_Promotion_ID = ? AND M_PromotionDistribution.IsActive = 'Y'", order.get_TrxName());
			query.setParameters(new Object[]{entry.getKey()});
			query.setOrderBy("SeqNo");
			List<MPromotionDistribution> distributionList = query.<MPromotionDistribution>list();

			Query rewardQuery = new Query(Env.getCtx(), MTable.get(order.getCtx(), I_M_PromotionReward.Table_ID),
					"M_PromotionReward.M_Promotion_ID = ? AND M_PromotionReward.IsActive = 'Y'", order.get_TrxName());
			rewardQuery.setParameters(new Object[]{entry.getKey()});
			rewardQuery.setOrderBy("SeqNo");
			List<MPromotionReward> rewardList = rewardQuery.<MPromotionReward>list();

			List<MPromotionLine> promotionLines = new ArrayList<MPromotionLine>();
			for (Integer M_PromotionLine_ID : entry.getValue()) {
				MPromotionLine promotionLine = new MPromotionLine(order.getCtx(), M_PromotionLine_ID, order.get_TrxName());
				promotionLines.add(promotionLine);
			}
			
			// reset qty per promotion
			for (MOrderLine line : lines) {
				if (line.getM_Product_ID() > 0 && line.get_ValueAsInt("M_PromotionReward_ID") <= 0) {
					if (line.getQtyOrdered().signum() > 0) {
						orderLineQty.put(line.getC_OrderLine_ID(), line.getQtyOrdered());
						if (line.getM_Promotion_ID() > 0) {
							orderLineAmt.put(line.getC_OrderLine_ID(), Env.ONE);
						} else {
							BigDecimal amount = SASPromotionRule.getAmount(line, promotion.get_ValueAsBoolean("IsDiscountAllowedOnTotal"));
							orderLineAmt.put(line.getC_OrderLine_ID(), amount);
						}
					}
				}
			}
			
			// maps used for grouping generated charge
			Map<String,RewardData> percentageRewardMap = new HashMap<String, SASPromotionReward.RewardData>();
//			Map<Integer,RewardData> flatDiscountRewardMap = new HashMap<Integer, SASPromotionReward.RewardData>();
			
			List<Integer> orderLineIdList = new ArrayList<Integer>();
			orderLineIdList.addAll(orderLineQty.keySet());
			while (true) {
				boolean hasDistributionSet = false;
				Set<Integer>promotionLineSet = new HashSet<Integer>();
				Set<Integer>mandatoryLineSet = new HashSet<Integer>();
				boolean mandatoryLineNotFound = false;
				List<Integer> validPromotionLineIDs = new ArrayList<Integer>();
				for (MPromotionLine promotionLine : promotionLines) {
					if (promotionLine.getM_PromotionGroup_ID() == 0 && promotionLine.getMinimumAmt() != null && promotionLine.getMinimumAmt().signum() >= 0) {
						if (orderAmount.compareTo(promotionLine.getMinimumAmt()) >= 0) {
							orderAmount = orderAmount.subtract(promotionLine.getMinimumAmt());
							validPromotionLineIDs.add(promotionLine.getM_PromotionLine_ID());
						} else if (promotionLine.isMandatoryPL()) {
							mandatoryLineNotFound = true;
							break;
						}
					}
				}
				if (mandatoryLineNotFound) {
					break;
				}
				
				for (MPromotionDistribution pd : distributionList) {
					if (entry.getValue().contains(pd.getM_PromotionLine_ID())) {
						//sort available orderline base on distribution sorting type
						if (!isNatura) {
							orderLineIdList = new ArrayList<Integer>();
							orderLineIdList.addAll(orderLineQty.keySet());
						}
						if (pd.getDistributionSorting() != null) {
							Comparator<Integer> cmp = olComparator;
							if (pd.getDistributionSorting().equals(MPromotionDistribution.DISTRIBUTIONSORTING_Descending))
								cmp = Collections.reverseOrder(cmp);
							Collections.sort(orderLineIdList, cmp);
						}
						DistributionSet prevSet = distributions.get(pd.getM_PromotionDistribution_ID());
						if (isNatura) {
							prevSet = null;
						}
						DistributionSet distributionSet = SASPromotionReward.calculateDistributionQty(pd, prevSet, validPromotionLineIDs, orderLineQty, orderLineAmt, orderLineIdList, processedProductList, order.get_TrxName());
						if (distributionSet != null && distributionSet.setQty.signum() > 0) {
							hasDistributionSet = true;
							promotionLineSet.add(pd.getM_PromotionLine_ID());
//							for(Map.Entry<Integer, BigDecimal> d : distributionSet.orderLines.entrySet()) {
//								System.out.println("Key:" + d.getKey() +", Value:" + d.getValue());
//							}
						} else {
							if (pd.getM_PromotionLine().isMandatoryPL()) {
								mandatoryLineSet.add(pd.getM_PromotionLine_ID());
							}
						}
						distributions.put(pd.getM_PromotionDistribution_ID(), distributionSet);
						sortingType.put(pd.getM_PromotionDistribution_ID(), pd.getDistributionSorting());
					}
				}
				if (!hasDistributionSet)
					break;

				if (mandatoryLineSet != null) {
					mandatoryLineNotFound = false;
					for(Integer id : mandatoryLineSet) {
						if (!promotionLineSet.contains(id)) {
							mandatoryLineNotFound = true;
							break;
						}
					}
					if (mandatoryLineNotFound) {
						break;
					}
				}

				for (MPromotionReward pr : rewardList) {
					if (pr.isForAllDistribution()) {
//						Collection<DistributionSet> all = distributions.values();
//						BigDecimal totalPrice = BigDecimal.ZERO;
//						for(DistributionSet distributionSet : all) {
//							for(Map.Entry<Integer, BigDecimal> olMap : distributionSet.orderLines.entrySet()) {
//								BigDecimal qty = (BigDecimal) olMap.getValue();
//								int C_OrderLine_ID = (Integer) olMap.getKey();
//								for (MOrderLine ol : lines) {
//									if (ol.getC_OrderLine_ID() == C_OrderLine_ID) {
//										totalPrice = totalPrice.add(ol.getPriceActual().multiply(qty));
//										break;
//									}
//								}
//								distributionSet.orderLines.put(olMap.getKey(), BigDecimal.ZERO);
//							}
//						}
//						BigDecimal discount = BigDecimal.ZERO;
//						if (pr.getRewardType().equals(MPromotionReward.REWARDTYPE_AbsoluteAmount)) {
//							if (pr.getAmount().compareTo(totalPrice) < 0) {
//								discount = totalPrice.subtract(pr.getAmount());
//							}
//						} else if (pr.getRewardType().equals(MPromotionReward.REWARDTYPE_FlatDiscount)) {
//							discount = pr.getAmount();
//						} else if (pr.getRewardType().equals(MPromotionReward.REWARDTYPE_Percentage)) {
//							discount = pr.getAmount().divide(Env.ONEHUNDRED).multiply(totalPrice);
//						}
//						if (discount.signum() > 0) {
//							addProductLine(order, null, discount, Env.ONE, pr.getC_Charge_ID(), pr.getM_Promotion(), pr);
//						}
					} else {
						int M_PromotionDistribution_ID = pr.getM_PromotionDistribution_ID();
						if (!distributions.containsKey(M_PromotionDistribution_ID))
							continue;
						int targetDistributionID = M_PromotionDistribution_ID;
						if (!pr.isSameDistribution()) {
							targetDistributionID = pr.getM_TargetDistribution_ID();
							if (!distributions.containsKey(targetDistributionID))
								continue;
						}
						DistributionSet distributionSet = distributions.get(targetDistributionID);
//						for(Map.Entry<Integer, BigDecimal> d : distributionSet.orderLines.entrySet()) {
//							System.out.println("==Key:" + d.getKey() +", Value:" + d.getValue());
//						}

						//sort by reward distribution sorting
						if (pr.getDistributionSorting() != null ) {
							Comparator<Integer> cmp = new OrderLineComparator(orderLineIndex);
							if (pr.getDistributionSorting().equals(MPromotionReward.DISTRIBUTIONSORTING_Descending))
								cmp = Collections.reverseOrder(cmp);
							Set<Integer> keySet = distributionSet.orderLines.keySet();
							List<Integer> keyList = new ArrayList<Integer>();
							keyList.addAll(keySet);
							Collections.sort(keyList, cmp);
							Map<Integer, BigDecimal>sortedMap = new LinkedHashMap<Integer, BigDecimal>();
							for(Integer id : keyList) {
								sortedMap.put(id, distributionSet.orderLines.get(id));
							}
							distributionSet.orderLines = sortedMap;
						}


						BigDecimal setBalance = distributionSet.setQty;
						BigDecimal toApply = pr.getQty();
						if (toApply == null || toApply.signum() == 0)
							toApply = BigDecimal.valueOf(-1.0);

						BigDecimal totalPrice  = BigDecimal.ZERO;

						for(Map.Entry<Integer, BigDecimal> olMap : distributionSet.orderLines.entrySet()) {
							BigDecimal qty = olMap.getValue();
							int C_OrderLine_ID = olMap.getKey();
							if (qty == null || qty.signum() <= 0)
								continue;
							if (qty.compareTo(setBalance) >= 0) {
								qty = setBalance;
								setBalance = BigDecimal.ZERO;
							} else {
								setBalance = setBalance.subtract(qty);
							}
							if (toApply.signum() > 0) {
								if (toApply.compareTo(qty) <= 0) {
									qty = toApply;
									toApply = BigDecimal.ZERO;
								} else {
									toApply = toApply.subtract(qty);
								}
								BigDecimal newQty = olMap.getValue();
								newQty = newQty.subtract(qty);
								distributionSet.orderLines.put(olMap.getKey(), newQty);
							}
							for (MOrderLine ol : lines) {
								if (ol.getC_OrderLine_ID() == C_OrderLine_ID) {
									if (pr.getRewardType().equals(MPromotionReward.REWARDTYPE_Percentage)) {
//										BigDecimal priceActual = ol.getPriceActual();
//										BigDecimal discount = priceActual.multiply(pr.getAmount().divide(Env.ONEHUNDRED));
//										addDiscountLine(order, ol, discount, qty, pr.getC_Charge_ID(), pr.getM_Promotion(),pr);
										RewardData rd = percentageRewardMap.get(pr.get_ID() + ol.getM_Product().getValue());
										if (rd == null) {
											rd = new RewardData(ol, pr, Env.ZERO);
											percentageRewardMap.put(pr.get_ID() + ol.getM_Product().getValue(), rd);
										}
										rd.setQty(rd.getQty().add(qty));
									} //else if (pr.getRewardType().equals(MPromotionReward.REWARDTYPE_FlatDiscount)) {
//										addDiscountLine(order, ol, pr.getAmount(), Env.ONE, pr.getC_Charge_ID(), pr.getM_Promotion(),pr);
//										RewardData rd = flatDiscountRewardMap.get(ol.getC_OrderLine_ID());
//										if (rd == null) {
//											rd = new RewardData(ol, pr, Env.ZERO);
//											flatDiscountRewardMap.put(C_OrderLine_ID, rd);
//										}
//										rd.setQty(rd.getQty().add(Env.ONE));
//									} else if (pr.getRewardType().equals(MPromotionReward.REWARDTYPE_AbsoluteAmount)) {
//										BigDecimal priceActual = ol.getPriceActual();
//										totalPrice = totalPrice.add(priceActual.multiply(qty));
//									}
								}
							}

							if (toApply.signum() == 0)
								break;
							if (setBalance.signum() == 0)
								break;
						}
//						if (pr.getRewardType().equals(MPromotionReward.REWARDTYPE_AbsoluteAmount))  {
//							if (pr.getAmount().compareTo(totalPrice) < 0) {
//								addProductLine(order, null, totalPrice.subtract(pr.getAmount()), Env.ONE, pr.getC_Charge_ID(), pr.getM_Promotion(),pr);
//							}
//						}
					}
				}
			}
			// SAS-59
//			for (RewardData rd : flatDiscountRewardMap.values()) {
//				addProductLine(order, rd.getOrderLine(), rd.getPromotionReward().getAmount(), rd.getQty(), 
//						rd.getPromotionReward().getC_Charge_ID(), rd.getPromotionReward().getM_Promotion(),
//						rd.getPromotionReward());
//			}
			
			for (RewardData rd : percentageRewardMap.values()) {
				BigDecimal discount = rd.getOrderLine().getPriceActual()
						.multiply(rd.getPromotionReward().getAmount().divide(Env.ONEHUNDRED));
				addProductLine(order, rd.getOrderLine(), discount, rd.getQty(), 
						rd.getPromotionReward().getC_Charge_ID(), rd.getPromotionReward().getM_Promotion(),
						rd.getPromotionReward());
			}
		}
	}

	private static void addProductLine(MOrder order, MOrderLine ol, BigDecimal discount,
			BigDecimal qty, int C_Charge_ID, I_M_Promotion promotion, MPromotionReward promotionReward) throws Exception {
		boolean isNatura = false;
		int count = new Query(Env.getCtx(), MOrderLine.Table_Name, "C_Order_ID = ? AND M_PromotionReward_ID = ?", null)
			.setClient_ID()
			.setOnlyActiveRecords(true)
			.setParameters(order.get_ID(),promotionReward.get_ID())
			.count();
		if (count > 0) {
			return;
		}
		int productID = 0;
		int targetDistributionID = promotionReward.getM_PromotionDistribution_ID();
		if (promotionReward.getM_TargetDistribution_ID() > 0) {
			targetDistributionID = promotionReward.getM_TargetDistribution_ID();
		}
		
		if (promotionReward.getM_TargetDistribution_ID() > 0) {
			int sourceGroupID = promotionReward.getM_PromotionDistribution().getM_PromotionLine().getM_PromotionGroup_ID();
			int targetGroupID = promotionReward.getM_TargetDistribution().getM_PromotionLine().getM_PromotionGroup_ID();
			if (sourceGroupID == targetGroupID) {
				isNatura = true;
			}
		}
		
		if (isNatura) {
			productID = ol.getM_Product_ID();
		} else if (targetDistributionID > 0) {
			MPromotionDistribution distribution = new MPromotionDistribution(Env.getCtx(), targetDistributionID, null);
			ISASMPromotionDistribution sasDistribution = POWrapper.create(distribution, ISASMPromotionDistribution.class);
			if (sasDistribution.isReward() && distribution.getM_PromotionLine_ID() > 0) {
				MPromotionLine promotionLine = new MPromotionLine(Env.getCtx(), distribution.getM_PromotionLine_ID(), null);
				if (promotionLine.getM_PromotionGroup_ID() > 0) {
					MPromotionGroupLine pgLine = new Query(Env.getCtx(), MPromotionGroupLine.Table_Name, "M_PromotionGroup_ID = ?", null)
						.setOnlyActiveRecords(true)
						.setParameters(promotionLine.getM_PromotionGroup_ID())
						.first();
					if (pgLine != null) {
						productID = pgLine.getM_Product_ID();
					}
				}
			}
		}
		
		
		if (productID == 0) {
			return;
		}
		
//		if (isNatura) {
//			// replace dummy line to actual line
//			MOrderLine oln = new Query(order.getCtx(), MOrderLine.Table_Name, "C_Order_ID = ? AND M_Product_ID = ?", order.get_TrxName())
//				.setOnlyActiveRecords(true)
//				.setParameters(order.get_ID(),productID)
//				.first();
//			if (oln != null) {
//				ol = oln;
//			}
//		}
		
		MOrderLine nol = new MOrderLine(order.getCtx(), 0, order.get_TrxName());
		MOrderLine.copyValues(ol, nol);
		nol.setC_Order_ID(order.getC_Order_ID());
		nol.setOrder(order);
		nol.setProduct(MProduct.get(Env.getCtx(), productID));
		nol.setQty(qty);
		nol.setLine(0);
		
		if (!isNatura) {
			// set price from price list
			MProductPricing pp = new MProductPricing(productID, order.getC_BPartner_ID(), qty, order.isSOTrx());
			pp.setM_PriceList_ID(order.getM_PriceList_ID());
			pp.setPriceDate(order.getDateOrdered());
			nol.setPriceList(pp.getPriceList());
			nol.setPriceLimit(pp.getPriceLimit());
			nol.setPriceActual(pp.getPriceStd());
			nol.setPriceEntered(pp.getPriceStd());
		}
		
		if (ol != null && Integer.toString(ol.getLine()).endsWith("0")) {
			for(int i = 0; i < 9; i++) {
				int line = ol.getLine() + i + 1;
				int r = DB.getSQLValue(order.get_TrxName(), "SELECT C_OrderLine_ID FROM C_OrderLine WHERE C_Order_ID = ? AND Line = ?", order.getC_Order_ID(), line);
				if (r <= 0) {
					nol.setLine(line);
					break;
				}
			}
		}
		String description = promotion.getName();
		if (ol != null)
			description += (", " + ol.getName());
		nol.setDescription(description);
		nol.set_ValueOfColumn("M_Promotion_ID", promotion.getM_Promotion_ID());
		nol.set_ValueOfColumn("M_PromotionReward_ID", promotionReward.get_ID());
		
		int campaignID = promotionReward.get_ValueAsInt("C_Campaign_ID");
		if (campaignID == 0) {
			campaignID = promotion.getC_Campaign_ID();
		}
		if (order.getC_DocTypeTarget_ID() == SASSystemID.DOCTYPE_PK_A || order.getC_DocTypeTarget_ID() == SASSystemID.DOCTYPE_PK_B) {
			MCampaign campaign = new Query(order.getCtx(), MCampaign.Table_Name, "Value=?", order.get_TrxName())
				.setClient_ID()
				.setOnlyActiveRecords(true)
				.setParameters(SASSystemID.CAMPAIGN_PENGAJUAN_KHUSUS_NATURA_VALUE)
				.first();
			if (campaign == null) {
				throw new AdempiereException("Campaign PK not found");
			} else {
				campaignID = campaign.get_ID();
			}
		}
		if (campaignID > 0) {
			nol.setC_Campaign_ID(campaignID);
		}
		if (!nol.save())
			throw new AdempiereException("Failed to add product line to order");
	}

	/**
	 *
	 * @param order
	 * @return Map<M_Promotion_ID, List<M_PromotionLine_ID>>
	 * @throws Exception
	 */
	private static Map<Integer, List<Integer>> findM_Promotion_ID(MOrder order) throws Exception {
		String select = "SELECT M_Promotion.M_Promotion_ID From M_Promotion Inner Join M_PromotionPreCondition "
			+ " ON (M_Promotion.M_Promotion_ID = M_PromotionPreCondition.M_Promotion_ID)";

		String bpFilter = "M_PromotionPreCondition.C_BPartner_ID = ? OR M_PromotionPreCondition.C_BP_Group_ID = ? OR (M_PromotionPreCondition.C_BPartner_ID IS NULL AND M_PromotionPreCondition.C_BP_Group_ID IS NULL)";
		String priceListFilter = "M_PromotionPreCondition.M_PriceList_ID IS NULL OR M_PromotionPreCondition.M_PriceList_ID = ?";
		String warehouseFilter = "M_PromotionPreCondition.M_Warehouse_ID IS NULL OR M_PromotionPreCondition.M_Warehouse_ID = ?";
		String dateFilter = "M_PromotionPreCondition.StartDate <= ? AND (M_PromotionPreCondition.EndDate >= ? OR M_PromotionPreCondition.EndDate IS NULL)";

		//optional promotion code filter
		String promotionCode = (String)order.get_Value("PromotionCode");

		StringBuilder sql = new StringBuilder();
		sql.append(select)
			.append(" WHERE")
			.append(" (" + bpFilter + ")")
			.append(" AND (").append(priceListFilter).append(")")
			.append(" AND (").append(warehouseFilter).append(")")
			.append(" AND (").append(dateFilter).append(")");
		if (promotionCode != null && promotionCode.trim().length() > 0) {
			sql.append(" AND (M_PromotionPreCondition.PromotionCode = ?)");
		} else {
			sql.append(" AND (M_PromotionPreCondition.PromotionCode IS NULL)");
		}
		
		//optional activity filter
		int C_Activity_ID = order.getC_Activity_ID();
		if (C_Activity_ID > 0) {
			sql.append(" AND (M_PromotionPreCondition.C_Activity_ID = ? OR M_PromotionPreCondition.C_Activity_ID IS NULL)");
		} else {
			sql.append(" AND (M_PromotionPreCondition.C_Activity_ID IS NULL)");
		}
		
		//optional org trx filter
		int AD_OrgTrx_ID = order.getAD_OrgTrx_ID();
		if (AD_OrgTrx_ID > 0) {
			sql.append(" AND (M_PromotionPreCondition.AD_OrgTrx_ID = ? OR M_PromotionPreCondition.AD_OrgTrx_ID IS NULL)");
		} else {
			sql.append(" AND (M_PromotionPreCondition.AD_OrgTrx_ID IS NULL)");
		}
		
		sql.append(" AND (M_Promotion.AD_Client_ID in (0, ?))")
			.append(" AND (M_Promotion.AD_Org_ID in (0, ?))")
			.append(" AND (M_Promotion.IsActive = 'Y')")
			.append(" AND (M_PromotionPreCondition.IsActive = 'Y')")
			.append(" ORDER BY M_Promotion.PromotionPriority Desc ");

		PreparedStatement stmt = null;
		ResultSet rs = null;
		//Key = M_Promotion_ID, value = List<M_PromotionLine_ID>
		Map<Integer, List<Integer>> promotions = new LinkedHashMap<Integer, List<Integer>>();
		try {
			int pindex = 1;
			stmt = DB.prepareStatement(sql.toString(), order.get_TrxName());
			stmt.setInt(pindex++, order.getC_BPartner_ID());
			stmt.setInt(pindex++, order.getC_BPartner().getC_BP_Group_ID());
			stmt.setInt(pindex++, order.getM_PriceList_ID());
			stmt.setInt(pindex++, order.getM_Warehouse_ID());
			stmt.setTimestamp(pindex++, order.getDateOrdered());
			stmt.setTimestamp(pindex++, order.getDateOrdered());
			if (promotionCode != null && promotionCode.trim().length() > 0) {
				stmt.setString(pindex++, promotionCode);
			}
			if (C_Activity_ID > 0) {
				stmt.setInt(pindex++, C_Activity_ID);
			}
			if (AD_OrgTrx_ID > 0) {
				stmt.setInt(pindex++, AD_OrgTrx_ID);
			}
			stmt.setInt(pindex++, order.getAD_Client_ID());
			stmt.setInt(pindex++, order.getAD_Org_ID());
			rs = stmt.executeQuery();
			while(rs.next()) {
				int M_Promotion_ID = rs.getInt(1);
				if (SASPromotionRule.isValidBP(M_Promotion_ID, order.getC_BPartner_ID())) {
					List<Integer> promotionLineIDs = findPromotionLine(M_Promotion_ID, order);
					if (!promotionLineIDs.isEmpty()) {
						promotions.put(M_Promotion_ID, promotionLineIDs);
					}
				}
			}
		} finally {
			DB.close(rs, stmt);
		}

		return promotions;
	}

	/**
	 *
	 * @param distribution
	 * @param prevSet
	 * @param orderLineQty
	 * @param orderLineQty2
	 * @param orderLineIdList
	 * @param qtyAvailable
	 * @return Distribution Qty
	 * @throws Exception
	 */
	private static DistributionSet calculateDistributionQty(MPromotionDistribution distribution,
			DistributionSet prevSet, List<Integer> validPromotionLineIDs, Map<Integer, BigDecimal> orderLineQty, 
			Map<Integer, BigDecimal> orderLineAmt, List<Integer> orderLineIdList, 
			List<Integer> processedProductList, String trxName) throws Exception {

			String sql = "SELECT C_OrderLine.C_OrderLine_ID FROM M_PromotionLine"
				+ " INNER JOIN M_PromotionGroup ON (M_PromotionLine.M_PromotionGroup_ID = M_PromotionGroup.M_PromotionGroup_ID AND M_PromotionGroup.IsActive = 'Y')"
				+ " INNER JOIN M_PromotionGroupLine ON (M_PromotionGroup.M_PromotionGroup_ID = M_PromotionGroupLine.M_PromotionGroup_ID AND M_PromotionGroupLine.IsActive = 'Y')"
				+ " INNER JOIN C_OrderLine ON (M_PromotionGroupLine.M_Product_ID = C_OrderLine.M_Product_ID)"
				+ " WHERE M_PromotionLine.M_PromotionLine_ID = ? AND C_OrderLine.C_OrderLine_ID = ?"
				+ " AND M_PromotionLine.IsActive = 'Y'"
				+ " AND C_OrderLine.IsAffectPromo = 'Y'";
		if (processedProductList != null && !processedProductList.isEmpty()) {
			sql += " AND C_OrderLine.M_Product_ID NOT IN(";
			for (Integer processedProductID : processedProductList) {
				if (processedProductList.indexOf(processedProductID) > 0) {
					sql += ",";
				}
				sql += processedProductID; 
			}
			sql += ")";
		}	

		DistributionSet distributionSet = new DistributionSet();
		List<Integer>eligibleOrderLineIDs = new ArrayList<Integer>();
		ISASMPromotionDistribution sasDistribution = POWrapper.create(distribution, ISASMPromotionDistribution.class);
		boolean isAmtBased = sasDistribution.isAmtBased();
		MPromotion promotion = new MPromotion(Env.getCtx(), distribution.getM_Promotion_ID(), trxName);
		boolean isNatura = promotion.get_ValueAsBoolean("IsNatura");
		if (distribution.getM_PromotionLine().getM_PromotionGroup_ID() == 0) {
			if (validPromotionLineIDs.contains(distribution.getM_PromotionLine_ID())) {
				eligibleOrderLineIDs.addAll(orderLineIdList);
			}
		} else {
			int productID = 0;
			for(int C_OrderLine_ID : orderLineIdList) {
				BigDecimal availableQty = isAmtBased ? orderLineAmt.get(C_OrderLine_ID) : orderLineQty.get(C_OrderLine_ID);
				if (sasDistribution.isReward() && !isNatura) {
					// reward is auto included
					availableQty = availableQty.add(distribution.getQty());
					eligibleOrderLineIDs.add(C_OrderLine_ID);
				} else {
					if (availableQty.signum() <= 0 && !isNatura) continue;
					MOrderLine orderLine = new MOrderLine(Env.getCtx(), C_OrderLine_ID, trxName);
					if (isNatura && productID != 0 && orderLine.getM_Product_ID() != productID) {
						continue;
					}
					PreparedStatement stmt = null;
					ResultSet rs = null;
					try {
						stmt = DB.prepareStatement(sql, trxName);
						stmt.setInt(1, distribution.getM_PromotionLine_ID());
						stmt.setInt(2, C_OrderLine_ID);
						rs = stmt.executeQuery();
						if (rs.next()) {
							eligibleOrderLineIDs.add(C_OrderLine_ID);
							productID = orderLine.getM_Product_ID();
						}
					} catch (Exception e) {
						throw new AdempiereException(e.getLocalizedMessage(), e);
					} finally {
						DB.close(rs, stmt);
					}
				}
			}
		}

		if (eligibleOrderLineIDs.isEmpty()) {
			distributionSet.setQty = BigDecimal.ZERO;
			return distributionSet;
		}

		BigDecimal compareQty = distribution.getQty();

		BigDecimal setQty = BigDecimal.ZERO;
		BigDecimal totalOrderLineQty = BigDecimal.ZERO;
		for (int C_OrderLine_ID : eligibleOrderLineIDs) {
			BigDecimal availableQty = isAmtBased ? orderLineAmt.get(C_OrderLine_ID) : orderLineQty.get(C_OrderLine_ID);
			if (sasDistribution.isReward())
				availableQty = availableQty.add(distribution.getQty());
			if (availableQty.signum() <= 0) continue;
			totalOrderLineQty = totalOrderLineQty.add(availableQty);
		}
		int compare = totalOrderLineQty.compareTo(compareQty);
		boolean match = false;
		if (compare <= 0 && "<=".equals(distribution.getOperation())) {
			match = true;
		} else if (compare >= 0 && ">=".equals(distribution.getOperation())) {
			match = true;
		}
		if (match) {
			if (MPromotionDistribution.DISTRIBUTIONTYPE_Max.equals(distribution.getDistributionType())) {
				setQty = compare > 0 ? totalOrderLineQty : distribution.getQty();
			} else if (MPromotionDistribution.DISTRIBUTIONTYPE_Min.equals(distribution.getDistributionType())) {
				setQty = compare < 0 ? totalOrderLineQty : distribution.getQty();
			} else {
				setQty = compare > 0 ? totalOrderLineQty.subtract(distribution.getQty())
						: distribution.getQty().subtract(totalOrderLineQty);
			}
			distributionSet.setQty = setQty;
			while (setQty.signum() > 0) {
				if (prevSet != null) {
					BigDecimal recycleQty = BigDecimal.ZERO;
					for(Map.Entry<Integer, BigDecimal> entry : prevSet.orderLines.entrySet()) {
						if (entry.getValue().signum() > 0) {
							setQty = setQty.subtract(entry.getValue());
							distributionSet.orderLines.put(entry.getKey(), entry.getValue());
							recycleQty = recycleQty.add(entry.getValue());
						}
					}
					if (recycleQty.signum() > 0) {
						for (int C_OrderLine_ID : eligibleOrderLineIDs) {
							BigDecimal availableQty = isAmtBased ? orderLineAmt.get(C_OrderLine_ID) : orderLineQty.get(C_OrderLine_ID);
							if (sasDistribution.isReward())
								availableQty = availableQty.add(recycleQty);
							if (availableQty.signum() <= 0) continue;
							if (availableQty.compareTo(recycleQty) < 0) {
								recycleQty = recycleQty.subtract(availableQty);
								if (isAmtBased) {
									orderLineAmt.put(C_OrderLine_ID, BigDecimal.ZERO);
								} else {
									orderLineQty.put(C_OrderLine_ID, BigDecimal.ZERO);
								}
							} else {
								availableQty = availableQty.subtract(recycleQty);
								if (isAmtBased) {
									orderLineAmt.put(C_OrderLine_ID, availableQty);
								} else {
									orderLineQty.put(C_OrderLine_ID, availableQty);
								}
								recycleQty = BigDecimal.ZERO;
							}
							if (recycleQty.signum() <= 0)
								break;
						}
					}
					if (setQty.signum() == 0) break;
				}
				List<Integer> toBeRemoveOrderLineIDs = new ArrayList<Integer>(); 
				for (int C_OrderLine_ID : eligibleOrderLineIDs) {
					BigDecimal availableQty = isAmtBased ? orderLineAmt.get(C_OrderLine_ID) : orderLineQty.get(C_OrderLine_ID);
					BigDecimal originalAvailableQty = availableQty;
					if (sasDistribution.isReward())
						availableQty = availableQty.add(setQty);
					if (availableQty.signum() <= 0) continue;
					if (availableQty.compareTo(setQty) < 0) {
						setQty = setQty.subtract(availableQty);
						distributionSet.orderLines.put(C_OrderLine_ID, availableQty);
						if (isAmtBased) {
							orderLineAmt.put(C_OrderLine_ID, BigDecimal.ZERO);
						} else {
							orderLineQty.put(C_OrderLine_ID, BigDecimal.ZERO);
						}
						if (sasDistribution.isReward() && isNatura) {
							toBeRemoveOrderLineIDs.add(Integer.valueOf(C_OrderLine_ID));
						}
					} else {
						availableQty = availableQty.subtract(setQty);
						originalAvailableQty = originalAvailableQty.subtract(setQty);
						distributionSet.orderLines.put(C_OrderLine_ID, setQty);
						if (isAmtBased) {
							orderLineAmt.put(C_OrderLine_ID, availableQty);
						} else {
							orderLineQty.put(C_OrderLine_ID, availableQty);
						}
						setQty = BigDecimal.ZERO;
						if (originalAvailableQty.compareTo(Env.ZERO) <= 0 && sasDistribution.isReward() && isNatura) {
//							orderLineIdList.remove(Integer.valueOf(C_OrderLine_ID));
							toBeRemoveOrderLineIDs.add(Integer.valueOf(C_OrderLine_ID));
						}
					}
					if (!toBeRemoveOrderLineIDs.isEmpty()){
						orderLineIdList.removeAll(toBeRemoveOrderLineIDs);
					}
					if (setQty.signum() <= 0)
						break;
				}
			}
		} else if (isNatura) {
			MOrderLine ol = new MOrderLine(Env.getCtx(), eligibleOrderLineIDs.get(0), trxName);
			processedProductList.add(ol.getM_Product_ID());
			List<MOrderLine> olList = new Query(Env.getCtx(), MOrderLine.Table_Name, 
					"M_Product_ID = ? AND IsAffectPromo = 'Y'", trxName)
				.setParameters(ol.getM_Product_ID())
				.list();
			for (MOrderLine orderLine : olList) {
				orderLineIdList.remove(Integer.valueOf(orderLine.get_ID()));
			}
			return calculateDistributionQty(distribution, prevSet, validPromotionLineIDs, orderLineQty, orderLineAmt, orderLineIdList, processedProductList, trxName);
		}

		return distributionSet ;
	}

	/**
	 *
	 * @param promotion_ID
	 * @param order
	 * @return List<M_PromotionLine_ID>
	 * @throws SQLException
	 */
	private static List<Integer> findPromotionLine(int promotion_ID, MOrder order) throws SQLException {
		MPromotion promotion = new MPromotion(Env.getCtx(), promotion_ID, null);
		Query query = new Query(Env.getCtx(), MTable.get(order.getCtx(), I_M_PromotionLine.Table_ID), " M_PromotionLine.M_Promotion_ID = ? AND M_PromotionLine.IsActive = 'Y'", order.get_TrxName());
		query.setParameters(new Object[]{promotion_ID});
		List<MPromotionLine>plist = query.<MPromotionLine>list();
		//List<M_PromotionLine_ID>
		List<Integer>applicable = new ArrayList<Integer>();
		List<MOrderLine> list = new Query(Env.getCtx(), MOrderLine.Table_Name, 
				"C_Order_ID = ? AND IsAffectPromo = 'Y'", order.get_TrxName())
			.setOnlyActiveRecords(true)
			.setParameters(order.get_ID())
			.list();
		MOrderLine[] lines = list.toArray(new MOrderLine[list.size()]);
		String sql = "SELECT DISTINCT C_OrderLine.C_OrderLine_ID FROM M_PromotionGroup INNER JOIN M_PromotionGroupLine"
				+ " ON (M_PromotionGroup.M_PromotionGroup_ID = M_PromotionGroupLine.M_PromotionGroup_ID AND M_PromotionGroupLine.IsActive = 'Y')"
				+ " INNER JOIN C_OrderLine ON (M_PromotionGroupLine.M_Product_ID = C_OrderLine.M_Product_ID)"
				+ " INNER JOIN M_PromotionLine ON (M_PromotionLine.M_PromotionGroup_ID = M_PromotionGroup.M_PromotionGroup_ID)"
				+ " WHERE M_PromotionLine.M_PromotionLine_ID = ? AND C_OrderLine.C_Order_ID = ?"
				+ " AND M_PromotionLine.IsActive = 'Y'"
				+ " AND M_PromotionGroup.IsActive = 'Y'";
		for (MPromotionLine pl : plist) {
			boolean match = false;
			if (pl.getM_PromotionGroup_ID() > 0) {
				
				// Reward is auto added
				int rewardCount = new Query(Env.getCtx(), MPromotionDistribution.Table_Name, 
						"M_PromotionLine_ID = ? AND IsReward = 'Y'", order.get_TrxName())
					.setOnlyActiveRecords(true)
					.setParameters(pl.getM_PromotionLine_ID())
					.count();
				if (rewardCount > 0) {
					match = true;
				} else {
					PreparedStatement stmt = null;
					ResultSet rs = null;
					try {
						stmt = DB.prepareStatement(sql, order.get_TrxName());
						stmt.setInt(1, pl.getM_PromotionLine_ID());
						stmt.setInt(2, order.getC_Order_ID());
						rs = stmt.executeQuery();
						BigDecimal orderAmt = BigDecimal.ZERO;
						while(rs.next()) {
							if (pl.getMinimumAmt() != null && pl.getMinimumAmt().signum() > 0) {
								int C_OrderLine_ID = rs.getInt(1);
								for (MOrderLine ol : lines) {
									if (ol.getC_OrderLine_ID() == C_OrderLine_ID) {
//										orderAmt = orderAmt.add(ol.getLineNetAmt());
										orderAmt = orderAmt.add(SASPromotionRule.getAmount(ol, promotion.get_ValueAsBoolean("IsDiscountAllowedOnTotal")));
										break;
									}
								}
								if (orderAmt.compareTo(pl.getMinimumAmt()) >= 0) {
									match = true;
									break;
								}
							} else {
								match = true;
								break;
							}
						}
					} finally {
						DB.close(rs, stmt);
					}
				}
			} else if (pl.getMinimumAmt() != null && pl.getMinimumAmt().compareTo(SASPromotionRule.getAmount(order, promotion.get_ValueAsBoolean("IsDiscountAllowedOnTotal"))) <= 0 ) {
				match = true;
			}
			if (!match && pl.isMandatoryPL()) {
				applicable.clear();
				break;
			}
			if (match)
				applicable.add(pl.getM_PromotionLine_ID());
		}
		return applicable;
	}

	static class DistributionSet {
		//<C_OrderLine_Id, DistributionQty>
		Map<Integer, BigDecimal> orderLines = new LinkedHashMap<Integer, BigDecimal>();
		BigDecimal setQty = BigDecimal.ZERO;
	}

	static class OrderLineComparator implements Comparator<Integer> {
		Map<Integer, MOrderLine> index;
		OrderLineComparator(Map<Integer, MOrderLine> olIndex) {
			index = olIndex;
		}

		public int compare(Integer ol1, Integer ol2) {
			return index.get(ol1).getPriceActual().compareTo(index.get(ol2).getPriceActual());
		}
	}
	
	static class RewardData {
		private MOrderLine orderLine;
		private MPromotionReward promotionReward;
		private BigDecimal qty;
		
		public RewardData(MOrderLine orderLine, MPromotionReward promotionReward, BigDecimal qty) {
			this.setOrderLine(orderLine);
			this.setPromotionReward(promotionReward);
			this.setQty(qty);
		}

		public MOrderLine getOrderLine() {
			return orderLine;
		}

		public void setOrderLine(MOrderLine orderLine) {
			this.orderLine = orderLine;
		}

		public MPromotionReward getPromotionReward() {
			return promotionReward;
		}

		public void setPromotionReward(MPromotionReward promotionReward) {
			this.promotionReward = promotionReward;
		}

		public BigDecimal getQty() {
			return qty;
		}

		public void setQty(BigDecimal qty) {
			this.qty = qty;
		}
	}
}
