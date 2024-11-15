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
import org.adempiere.model.MPromotionLine;
import org.adempiere.model.MPromotionReward;
import org.adempiere.model.POWrapper;
import org.compiere.model.I_C_OrderLine;
import org.compiere.model.I_M_Promotion;
import org.compiere.model.I_M_PromotionDistribution;
import org.compiere.model.I_M_PromotionLine;
import org.compiere.model.I_M_PromotionReward;
import org.compiere.model.MCampaign;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MTable;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.util.Env;

/**
 *
 * @author hengsin
 *
 */
public class SASPromotionRule {

	public static void applyPromotions(MOrder order) throws Exception {
//		System.out.println("========================================");
		//key = C_OrderLine, value = Qty to distribution
		Map<Integer, BigDecimal> orderLineQty = new LinkedHashMap<Integer, BigDecimal>();
		Map<Integer, BigDecimal> orderLineAmt = new LinkedHashMap<Integer, BigDecimal>();
		Map<Integer, MOrderLine> orderLineIndex = new HashMap<Integer, MOrderLine>();
		List<MOrderLine> list = new Query(Env.getCtx(), MOrderLine.Table_Name, 
				"C_Order_ID = ? AND IsAffectPromo = 'Y'", order.get_TrxName())
			.setOnlyActiveRecords(true)
//			.setOrderBy("QtyEntered DESC")
			.setParameters(order.get_ID())
			.list();
		MOrderLine[] lines = list.toArray(new MOrderLine[list.size()]);
		boolean hasDeleteLine = false;
		for (MOrderLine ol : lines) {
			if (ol.getM_Product_ID() > 0) {
				if (ol.getQtyOrdered().signum() > 0) {
					orderLineQty.put(ol.getC_OrderLine_ID(), ol.getQtyOrdered());
					orderLineIndex.put(ol.getC_OrderLine_ID(), ol);
					if (ol.getM_Promotion_ID() > 0) {
						orderLineAmt.put(ol.getC_OrderLine_ID(), Env.ZERO);
					} else {
						orderLineAmt.put(ol.getC_OrderLine_ID(), ol.getLineNetAmt());
					}
				}
			} else if (ol.getC_Charge_ID() > 0) {
				Number id = (Number) ol.get_Value("M_Promotion_ID");
				if (id != null && id.intValue() > 0) {
					ol.delete(false);
					hasDeleteLine = true;
				}
			}
		}
		if (orderLineQty.isEmpty()) return;

		//refresh order
		if (hasDeleteLine) {
			order.getLines(true, null);
			order.getTaxes(true);
			order.setGrandTotal(DB.getSQLValueBD(order.get_TrxName(), "SELECT GrandTotal From C_Order WHERE C_Order_ID = ?", order.getC_Order_ID()));
		}

		Map<Integer, List<Integer>> promotions = SASPromotionRule.findM_Promotion_ID(order);

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
				if (line.getM_Product_ID() > 0) {
					if (line.getQtyOrdered().signum() > 0) {
						// Top up natura
						if (isNatura && line.getM_Promotion_ID() > 0 && line.getM_Promotion_ID() != promotion.get_ID()) {
							orderLineQty.put(line.getC_OrderLine_ID(), Env.ZERO);
							orderLineAmt.put(line.getC_OrderLine_ID(), Env.ZERO);
							continue;
						}
						orderLineQty.put(line.getC_OrderLine_ID(), line.getQtyOrdered());
						if (line.getM_Promotion_ID() > 0) {
							orderLineAmt.put(line.getC_OrderLine_ID(), Env.ZERO);
						} else {
							BigDecimal amount = SASPromotionRule.getAmount(line, promotion.get_ValueAsBoolean("IsDiscountAllowedOnTotal"));
							orderLineAmt.put(line.getC_OrderLine_ID(), amount);
						}
					}
				}
			}
			
			// maps used for grouping generated charge
			Map<String,RewardData> percentageRewardMap = new HashMap<String, SASPromotionRule.RewardData>();
			Map<Integer,RewardData> flatDiscountRewardMap = new HashMap<Integer, SASPromotionRule.RewardData>();
			
			HashMap<Integer, Integer> DiscPromoMap = new HashMap<>();
			
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
						DistributionSet distributionSet = SASPromotionRule.calculateDistributionQty(pd, prevSet, validPromotionLineIDs, orderLineQty, orderLineAmt, orderLineIdList, processedProductList, order.get_TrxName());
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
						Collection<DistributionSet> all = distributions.values();
						BigDecimal totalPrice = BigDecimal.ZERO;
						for(DistributionSet distributionSet : all) {
							for(Map.Entry<Integer, BigDecimal> olMap : distributionSet.orderLines.entrySet()) {
								BigDecimal qty = (BigDecimal) olMap.getValue();
								int C_OrderLine_ID = (Integer) olMap.getKey();
								for (MOrderLine ol : lines) {
									if (ol.getC_OrderLine_ID() == C_OrderLine_ID) {
										totalPrice = totalPrice.add(ol.getPriceActual().multiply(qty));
										break;
									}
								}
								distributionSet.orderLines.put(olMap.getKey(), BigDecimal.ZERO);
							}
						}
						BigDecimal discount = BigDecimal.ZERO;
						if (pr.getRewardType().equals(MPromotionReward.REWARDTYPE_AbsoluteAmount)) {
							if (pr.getAmount().compareTo(totalPrice) < 0) {
								discount = totalPrice.subtract(pr.getAmount());
							}
						} else if (pr.getRewardType().equals(MPromotionReward.REWARDTYPE_FlatDiscount)) {
							discount = pr.getAmount();
						} else if (pr.getRewardType().equals(MPromotionReward.REWARDTYPE_Percentage)) {
							discount = pr.getAmount().divide(Env.ONEHUNDRED).multiply(totalPrice);
						}
						if (discount.signum() > 0) {
							addDiscountLine(order, null, discount, Env.ONE, pr.getC_Charge_ID(), pr.getM_Promotion(), pr);
						}
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
							if (!promotion.get_ValueAsBoolean("IsDiscPromo"))  {
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
							}
							for (MOrderLine ol : lines) {
								if (ol.getC_OrderLine_ID() == C_OrderLine_ID && !promotion.get_ValueAsBoolean("IsDiscPromo")) {
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
									} else if (pr.getRewardType().equals(MPromotionReward.REWARDTYPE_FlatDiscount)) {
//										addDiscountLine(order, ol, pr.getAmount(), Env.ONE, pr.getC_Charge_ID(), pr.getM_Promotion(),pr);
										RewardData rd = flatDiscountRewardMap.get(pr.get_ID());
										if (rd == null) {
											rd = new RewardData(ol, pr, Env.ZERO);
											flatDiscountRewardMap.put(pr.get_ID(), rd);
										}
										rd.setQty(rd.getQty().add(Env.ONE));
									} else if (pr.getRewardType().equals(MPromotionReward.REWARDTYPE_AbsoluteAmount)) {
										BigDecimal priceActual = ol.getPriceActual();
										totalPrice = totalPrice.add(priceActual.multiply(qty));
									}
								}
								if (promotion.get_ValueAsBoolean("IsDiscPromo")) {
									if (pr.getRewardType().equals(MPromotionReward.REWARDTYPE_FlatDiscount)) {
//										addDiscountLine(order, ol, pr.getAmount(), Env.ONE, pr.getC_Charge_ID(), pr.getM_Promotion(),pr);
										RewardData rd = flatDiscountRewardMap.get(pr.get_ID());
										
										if (!DiscPromoMap.containsKey(ol.getC_OrderLine_ID()) && distributionSet.eligibleOrderLineID.contains(ol.getC_OrderLine_ID())) {

											if(ol.getQtyEntered().compareTo(setBalance) >= 0) {
											
												if (rd == null) {
													rd = new RewardData(ol, pr, Env.ZERO);
													flatDiscountRewardMap.put(pr.get_ID(), rd);
												}
													DiscPromoMap.put(ol.getC_OrderLine_ID(), pr.get_ID());
										            BigDecimal qtys = ol.getQtyEntered().subtract(setBalance).divide(setBalance, BigDecimal.ROUND_DOWN).add(BigDecimal.ONE);
													rd.setQty(rd.getQty().add(qtys));
												}
										}
									}
								}
							}

							if (!promotion.get_ValueAsBoolean("IsDiscPromo")) {
								if (toApply.signum() == 0)
									break;
								if (setBalance.signum() == 0)
									break;
							}
						}
						if (pr.getRewardType().equals(MPromotionReward.REWARDTYPE_AbsoluteAmount))  {
							if (pr.getAmount().compareTo(totalPrice) < 0) {
								addDiscountLine(order, null, totalPrice.subtract(pr.getAmount()), Env.ONE, pr.getC_Charge_ID(), pr.getM_Promotion(),pr);
							}
						}
					}
				}
			}
			// SAS-59
			for (RewardData rd : flatDiscountRewardMap.values()) {
				addDiscountLine(order, rd.getOrderLine(), rd.getPromotionReward().getAmount(), rd.getQty(), 
						rd.getPromotionReward().getC_Charge_ID(), rd.getPromotionReward().getM_Promotion(),
						rd.getPromotionReward());
			}
			
			for (RewardData rd : percentageRewardMap.values()) {
				BigDecimal discount = rd.getOrderLine().getPriceActual()
						.multiply(rd.getPromotionReward().getAmount().divide(Env.ONEHUNDRED));
				addDiscountLine(order, rd.getOrderLine(), discount, rd.getQty(), 
						rd.getPromotionReward().getC_Charge_ID(), rd.getPromotionReward().getM_Promotion(),
						rd.getPromotionReward());
			}
		}
	}

	private static void addDiscountLine(MOrder order, MOrderLine ol, BigDecimal discount,
			BigDecimal qty, int C_Charge_ID, I_M_Promotion promotion, MPromotionReward promotionReward) throws Exception {
		MOrderLine nol = new MOrderLine(order.getCtx(), 0, order.get_TrxName());
		nol.setC_Order_ID(order.getC_Order_ID());
		nol.setOrder(order);
		nol.setC_Charge_ID(C_Charge_ID);
		nol.setQty(qty);
		if (discount.scale() > 2)
			discount = discount.setScale(2, BigDecimal.ROUND_HALF_UP);
		nol.setPriceEntered(discount.negate());
		nol.setPriceActual(discount.negate());
		
		MPromotion promotions = new MPromotion(Env.getCtx(), promotion.getM_Promotion_ID(), null);

//		if (ol != null && Integer.toString(ol.getLine()).endsWith("0")) {
		if (ol != null) {
			if (!promotions.get_ValueAsBoolean("IsDiscPromo")) {
				for(int i = 0; i < 9; i++) {
					int line = ol.getLine() + i + 1;
					int r = DB.getSQLValue(order.get_TrxName(), "SELECT C_OrderLine_ID FROM C_OrderLine WHERE C_Order_ID = ? AND Line = ?", order.getC_Order_ID(), line);
					if (r <= 0) {
						nol.setLine(line);
						break;
					}
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
		if ((order.getC_DocTypeTarget_ID() == SASSystemID.DOCTYPE_PK_A || order.getC_DocTypeTarget_ID() == SASSystemID.DOCTYPE_PK_B) && !promotions.get_ValueAsBoolean("IsDiscPromo")) {
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
			throw new AdempiereException("Failed to add discount line to order");
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
				if (sasDistribution.isReward() && isAmtBased) {
					availableQty = availableQty.add(distribution.getQty());
				}
				if (availableQty.signum() <= 0) continue;
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

		if (eligibleOrderLineIDs.isEmpty()) {
			distributionSet.setQty = BigDecimal.ZERO;
			return distributionSet;
		}
		
		distributionSet.eligibleOrderLineID = eligibleOrderLineIDs;

		BigDecimal compareQty = distribution.getQty();

		BigDecimal setQty = BigDecimal.ZERO;
		BigDecimal totalOrderLineQty = BigDecimal.ZERO;
		for (int C_OrderLine_ID : eligibleOrderLineIDs) {
			BigDecimal availableQty = isAmtBased ? orderLineAmt.get(C_OrderLine_ID) : orderLineQty.get(C_OrderLine_ID);
			if (sasDistribution.isReward() && isAmtBased) {
				availableQty = availableQty.add(distribution.getQty());
			}
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
							if (sasDistribution.isReward() && isAmtBased) {
								availableQty = availableQty.add(recycleQty);
							}
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
				for (int C_OrderLine_ID : eligibleOrderLineIDs) {
					BigDecimal availableQty = isAmtBased ? orderLineAmt.get(C_OrderLine_ID) : orderLineQty.get(C_OrderLine_ID);
					if (sasDistribution.isReward() && isAmtBased) {
						availableQty = availableQty.add(setQty);
					}
					if (availableQty.signum() <= 0) continue;
					if (availableQty.compareTo(setQty) < 0) {
						setQty = setQty.subtract(availableQty);
						distributionSet.orderLines.put(C_OrderLine_ID, availableQty);
						if (isAmtBased) {
							orderLineAmt.put(C_OrderLine_ID, BigDecimal.ZERO);
						} else {
							orderLineQty.put(C_OrderLine_ID, BigDecimal.ZERO);
						}
					} else {
						availableQty = availableQty.subtract(setQty);
						distributionSet.orderLines.put(C_OrderLine_ID, setQty);
						if (isAmtBased) {
							orderLineAmt.put(C_OrderLine_ID, availableQty);
						} else {
							orderLineQty.put(C_OrderLine_ID, availableQty);
						}
						setQty = BigDecimal.ZERO;
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
//									orderAmt = orderAmt.add(ol.getLineNetAmt());
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
		List<Integer>eligibleOrderLineID = new ArrayList<Integer>();

	}

	static class OrderLineComparator implements Comparator<Integer> {
		Map<Integer, MOrderLine> index;
		OrderLineComparator(Map<Integer, MOrderLine> olIndex) {
			index = olIndex;
		}

		public int compare(Integer ol1, Integer ol2) {
			int compare = index.get(ol1).getPriceActual().compareTo(index.get(ol2).getPriceActual());
			if (compare == 0) {
				compare = index.get(ol1).getM_Product_ID() > (index.get(ol2).getM_Product_ID()) ? 1 : -1;
			}
			return compare;
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
	
	public static void increasePromotionCounter(MOrder order) {
		MOrderLine[] lines = order.getLines(false, null);
		String promotionCode = (String)order.get_Value("PromotionCode");
		for (MOrderLine ol : lines) {
			if (ol.getC_Charge_ID() > 0) {
				Integer promotionID = (Integer) ol.get_Value("M_Promotion_ID");
				if (promotionID != null && promotionID.intValue() > 0) {

					int M_PromotionPreCondition_ID = findPromotionPreConditionId(
							order, promotionCode, promotionID);
					if (M_PromotionPreCondition_ID > 0) {
						String update = "UPDATE M_PromotionPreCondition SET PromotionCounter = PromotionCounter + 1 WHERE M_PromotionPreCondition_ID = ?";
						DB.executeUpdate(update, M_PromotionPreCondition_ID, order.get_TrxName());
					}
				}
			}
		}
	}
	
	public static void decreasePromotionCounter(MOrder order) {
		MOrderLine[] lines = order.getLines(false, null);
		String promotionCode = (String)order.get_Value("PromotionCode");
		for (MOrderLine ol : lines) {
			if (ol.getC_Charge_ID() > 0) {
				Integer promotionID = (Integer) ol.get_Value("M_Promotion_ID");
				if (promotionID != null && promotionID.intValue() > 0) {

					int M_PromotionPreCondition_ID = findPromotionPreConditionId(
							order, promotionCode, promotionID);
					if (M_PromotionPreCondition_ID > 0) {
						String update = "UPDATE M_PromotionPreCondition SET PromotionCounter = PromotionCounter - 1 WHERE M_PromotionPreCondition_ID = ?";
						DB.executeUpdate(update, M_PromotionPreCondition_ID, order.get_TrxName());
					}
				}
			}
		}
	}
	
	public static int findPromotionPreConditionId(MOrder order, String promotionCode,
			Integer promotionID) {
		String bpFilter = "M_PromotionPreCondition.C_BPartner_ID = ? OR M_PromotionPreCondition.C_BP_Group_ID = ? OR (M_PromotionPreCondition.C_BPartner_ID IS NULL AND M_PromotionPreCondition.C_BP_Group_ID IS NULL)";
		String priceListFilter = "M_PromotionPreCondition.M_PriceList_ID IS NULL OR M_PromotionPreCondition.M_PriceList_ID = ?";
		String warehouseFilter = "M_PromotionPreCondition.M_Warehouse_ID IS NULL OR M_PromotionPreCondition.M_Warehouse_ID = ?";
		String dateFilter = "M_PromotionPreCondition.StartDate <= ? AND (M_PromotionPreCondition.EndDate >= ? OR M_PromotionPreCondition.EndDate IS NULL)";

		StringBuilder select = new StringBuilder();
		select.append(" SELECT M_PromotionPreCondition.M_PromotionPreCondition_ID FROM M_PromotionPreCondition ")
			.append(" WHERE")
			.append(" (" + bpFilter + ")")
			.append(" AND (").append(priceListFilter).append(")")
			.append(" AND (").append(warehouseFilter).append(")")
			.append(" AND (").append(dateFilter).append(")")
			.append(" AND (M_PromotionPreCondition.M_Promotion_ID = ?)")
			.append(" AND (M_PromotionPreCondition.IsActive = 'Y')");
		if (promotionCode != null && promotionCode.trim().length() > 0) {
			select.append(" AND (M_PromotionPreCondition.PromotionCode = ?)");
		} else {
			select.append(" AND (M_PromotionPreCondition.PromotionCode IS NULL)");
		}
		select.append(" ORDER BY M_PromotionPreCondition.C_BPartner_ID Desc, M_PromotionPreCondition.C_BP_Group_ID Desc, M_PromotionPreCondition.M_PriceList_ID Desc, M_PromotionPreCondition.M_Warehouse_ID Desc, M_PromotionPreCondition.StartDate Desc");
		int M_PromotionPreCondition_ID = 0;
		int C_BP_Group_ID = 0;
		try {
			C_BP_Group_ID = order.getC_BPartner().getC_BP_Group_ID();
		} catch (Exception e) {
		}
		if (promotionCode != null && promotionCode.trim().length() > 0) {
			M_PromotionPreCondition_ID = DB.getSQLValue(order.get_TrxName(), select.toString(), order.getC_BPartner_ID(),
					C_BP_Group_ID, order.getM_PriceList_ID(), order.getM_Warehouse_ID(), order.getDateOrdered(),
					order.getDateOrdered(), promotionID, promotionCode);
		} else {
			M_PromotionPreCondition_ID = DB.getSQLValue(order.get_TrxName(), select.toString(), order.getC_BPartner_ID(),
					C_BP_Group_ID, order.getM_PriceList_ID(), order.getM_Warehouse_ID(), order.getDateOrdered(),
					order.getDateOrdered(), promotionID);
		}
		return M_PromotionPreCondition_ID;
	}
	
	public static BigDecimal getAmount(MOrder order, Boolean isDiscountAllowedOnTotal) {
		BigDecimal amount = Env.ZERO;
		List<MOrderLine> orderLines = new Query(Env.getCtx(), MOrderLine.Table_Name, "C_Order_ID = ? AND COALESCE(M_PromotionReward_ID,0) = 0", order.get_TrxName())
			.setParameters(order.get_ID())
			.list();
		for (MOrderLine orderLine : orderLines) {
			if (isDiscountAllowedOnTotal) {
				amount = amount.add(orderLine.getLineNetAmt());
			} else {
				amount = amount.add(orderLine.getPriceList().multiply(orderLine.getQtyOrdered()));
			}
		}
		return amount;
	}
	
	public static BigDecimal getAmount(MOrderLine orderLine, Boolean isDiscountAllowedOnTotal) {
		BigDecimal amount = Env.ZERO;
		if (isDiscountAllowedOnTotal) {
			amount = amount.add(orderLine.getLineNetAmt());
		} else {
			amount = amount.add(orderLine.getPriceList().multiply(orderLine.getQtyOrdered()));
		}
		return amount;
	}
	
	public static boolean isValidBP(int M_Promotion_ID, int C_BPartner_ID) {
		boolean isValid = true;
		int bpFilterCount = new Query(Env.getCtx(), MPromotionBP.Table_Name, "M_Promotion_ID = ?", null)
			.setOnlyActiveRecords(true)
			.setParameters(M_Promotion_ID)
			.count();
		if (bpFilterCount > 0) {
			int bpExclusiveCount = new Query(Env.getCtx(), MPromotionBP.Table_Name, 
					"M_Promotion_ID = ? AND IsExclude='N'", null)
				.setOnlyActiveRecords(true)
				.setParameters(M_Promotion_ID)
				.count();
			if (bpExclusiveCount > 0) {
				isValid = false;
				bpExclusiveCount = new Query(Env.getCtx(), MPromotionBP.Table_Name, 
						"M_Promotion_ID = ? AND C_BPartner_ID = ? AND IsExclude='N'", null)
					.setOnlyActiveRecords(true)
					.setParameters(M_Promotion_ID, C_BPartner_ID)
					.count();
				if (bpExclusiveCount > 0) {
					isValid = true;
				}
			} else {
				int bpExcludedCount = new Query(Env.getCtx(), MPromotionBP.Table_Name, 
						"M_Promotion_ID = ? AND C_BPartner_ID = ? AND IsExclude='Y'", null)
					.setOnlyActiveRecords(true)
					.setParameters(M_Promotion_ID, C_BPartner_ID)
					.count();
				if (bpExcludedCount > 0) {
					isValid = false;
				}
			}
		}
		return isValid;
	}
}