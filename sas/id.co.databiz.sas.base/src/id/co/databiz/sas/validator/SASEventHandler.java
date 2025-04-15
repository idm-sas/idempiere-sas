package id.co.databiz.sas.validator;

import id.co.databiz.awn.model.MPayment;
import id.co.databiz.awn.model.MPaymentAllocate;
import id.co.databiz.awn.model.wrapper.ICOrder;
import id.co.databiz.sas.SASSystemID;
import id.co.databiz.sas.model.MBPRule;
import id.co.databiz.sas.model.MRequestBundle;

import id.co.databiz.sas.model.SASPromotionReward;
import id.co.databiz.sas.model.SASPromotionRule;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.adempiere.base.event.AbstractEventHandler;
import org.adempiere.base.event.IEventTopics;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.exceptions.DBException;
import org.adempiere.exceptions.FillMandatoryException;
import org.adempiere.model.POWrapper;
import org.compiere.acct.Doc;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MBPartner;
import org.compiere.model.MClient;
import org.compiere.model.MConversionRate;
import org.compiere.model.MDiscountSchemaBreak;
import org.compiere.model.MDocType;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MInOutLineMA;
import org.compiere.model.MInventory;
import org.compiere.model.MInventoryLine;
import org.compiere.model.MInvoice;
import org.compiere.model.MLocator;
import org.compiere.model.MMovement;
import org.compiere.model.MMovementLine;
import org.compiere.model.MMovementLineMA;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MOrg;
import org.compiere.model.MPaymentTerm;
import org.compiere.model.MProduct;
import org.compiere.model.MProductPricing;
import org.compiere.model.MRequest;
import org.compiere.model.MStorageOnHand;
import org.compiere.model.MTimeExpenseLine;
import org.compiere.model.MWarehouse;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;
import org.compiere.util.Trx;
import org.compiere.wf.MWFScenario;
import org.osgi.service.event.Event;

public class SASEventHandler extends AbstractEventHandler {
	
	protected transient CLogger	log = CLogger.getCLogger (getClass());
	private MAcctSchema as = null;

	@Override
	protected void doHandleEvent(Event event) {
		PO po = getPO(event);
		
		if (event.getTopic().equals(IEventTopics.PO_BEFORE_NEW) || event.getTopic().equals(IEventTopics.PO_BEFORE_CHANGE)) {
			
			if (event.getTopic().equals(IEventTopics.PO_BEFORE_CHANGE)) {
				if (po.get_TableName().equals(MOrder.Table_Name)){
					MOrder order = (MOrder) po;
					// SAS-31 update line's bp location from header if it get changed
					if (order.isSOTrx() && order.is_ValueChanged(MOrder.COLUMNNAME_C_BPartner_Location_ID)) {
						int bpLocationID = order.getC_BPartner_Location_ID();
						List<MOrderLine> lines = Arrays.asList(order.getLines());
						for (MOrderLine line : lines) {
							line.setC_BPartner_Location_ID(bpLocationID);
							line.saveEx();
						}
					}
				}
			}
			
			if (po.get_TableName().equals(MRequest.Table_Name)){
				MRequest request = (MRequest) po;
				if (request.is_ValueChanged(MRequest.COLUMNNAME_Processed) || request.is_new()) {
					if (request.isProcessed() && request.getR_Status().isOpen()) {
						throw new AdempiereException("Open status document cannot be processed");
					}
					
					// Delivery journal with invoice payment term 0 cannot be processed if BPKNO is not filled
					if (request.isProcessed() && request.getR_RequestType_ID() == SASSystemID.REQUEST_TYPE_DELIVERY_JOURNAL) {
						int invoiceID = request.getC_Invoice_ID();
						if (invoiceID > 0) {
							MInvoice invoice = MInvoice.get(Env.getCtx(), invoiceID);
							MPaymentTerm paymentTerm = new MPaymentTerm(Env.getCtx(), invoice.getC_PaymentTerm_ID(), po.get_TrxName());
							String bpkNo = request.get_ValueAsString("BPKNo");
							if (paymentTerm.getNetDays() == 0 && bpkNo.isEmpty()) {
								throw new AdempiereException("BPK No is mandatory for invoice " + invoice.getDocumentNo());
							}
						}
					}
				}
				
				if (request.is_ValueChanged("C_BPartner_Location_ID")) {
					if (request.get_ValueAsInt("C_BPartner_Location_ID") > 0 && request.getC_BPartner_ID() == 0) {
						request.set_ValueOfColumn("C_BPartner_Location_ID", null);
					}
				}
				
				// SAS-60 cannot create new request line if bundle is already departed
				if (request.is_new() && request.getR_RequestType_ID() == SASSystemID.REQUEST_TYPE_DELIVERY_JOURNAL) {
					int bundleID = request.get_ValueAsInt("SAS_RequestBundle_ID");
					if (bundleID > 0) {
						MRequestBundle bundle = new MRequestBundle(Env.getCtx(), bundleID, po.get_TrxName());
						if (bundle.getDepart().equals("Y")) {
							throw new AdempiereException("Document is already departed");
						}
					}
				}
			} else if (po.get_TableName().equals(MInvoice.Table_Name)){
				MInvoice invoice = (MInvoice) po;
				invoice.set_ValueOfColumn("InvoicePrintedBy", null);
				invoice.set_ValueOfColumn("TTHPrintedBy", null);
				invoice.set_ValueOfColumn("LampiranPrintedBy", null);
			} else if (po.get_TableName().equals(MDiscountSchemaBreak.Table_Name)){
				MDiscountSchemaBreak dsb = (MDiscountSchemaBreak) po;
				if (dsb.isActive()) {
					String group1 = dsb.get_ValueAsString("Group1");
					if (group1 != null && !group1.isEmpty()) {
						int count = new Query(Env.getCtx(), MDiscountSchemaBreak.Table_Name, 
								"M_DiscountSchema_ID = ? AND M_DiscountSchemaBreak_ID <> ? AND Group1 = ?", po.get_TrxName())
							.setOnlyActiveRecords(true)
							.setParameters(dsb.getM_DiscountSchema_ID(), dsb.get_ID(),group1)
							.count();
						if (count > 0) {
							throw new AdempiereUserError(group1 + " already exists.");
						}
					}
				}
			} else if (po.get_TableName().equals(MPaymentAllocate.Table_Name)){
				MPaymentAllocate payAllocate = (MPaymentAllocate) po;
				MPayment payment = new MPayment(Env.getCtx(), payAllocate.getC_Payment_ID(), po.get_TrxName());
				
				if(payAllocate.get_ValueAsInt("C_Activity_ID")>0 && payment.getC_Activity_ID() != payAllocate.get_ValueAsInt("C_Activity_ID")){
					payment.setC_Activity_ID(payAllocate.get_ValueAsInt("C_Activity_ID"));
					payment.saveEx();
				}
			} else if (po.get_TableName().equals(MRequestBundle.Table_Name)){
				MRequestBundle rb = (MRequestBundle) po;
				if (rb.getAD_Org_ID() == 0) {
					throw new FillMandatoryException("AD_Org_ID");
				}
			} else if (po.get_TableName().equals(MTimeExpenseLine.Table_Name)){
				MTimeExpenseLine el = (MTimeExpenseLine) po;
				
				int C_Currency_From_ID = el.getC_Currency_ID();
				int C_Currency_To_ID = Env.getContextAsInt(Env.getCtx(), Env.C_CURRENCY_ID);
				int CurrencyType = el.get_ValueAsInt("C_ConversionType_ID");
				
				BigDecimal ConvertedAmt = el.getExpenseAmt();
				
				if (C_Currency_From_ID != C_Currency_To_ID) {
					int AD_Client_ID = el.getAD_Client_ID();
					int AD_Org_ID = el.getAD_Org_ID();
					if(CurrencyType > 0) {
					ConvertedAmt = MConversionRate.convert (Env.getCtx(),
							ConvertedAmt, C_Currency_From_ID, C_Currency_To_ID, 
							el.getDateExpense(), CurrencyType, AD_Client_ID, AD_Org_ID);
					}
					else {
						ConvertedAmt = MConversionRate.convert (Env.getCtx(),
								ConvertedAmt, C_Currency_From_ID, C_Currency_To_ID, 
								el.getDateExpense(), 0, AD_Client_ID, AD_Org_ID);
					}
					el.setConvertedAmt(ConvertedAmt);					
				}
			}
		} else if (event.getTopic().equals(IEventTopics.PO_BEFORE_DELETE)) {
			if (po.get_TableName().equals(MRequest.Table_Name)){
				MRequest request = (MRequest) po;
				
				// SAS-60 cannot delete request line if bundle is already departed
				if (request.getR_RequestType_ID() == SASSystemID.REQUEST_TYPE_DELIVERY_JOURNAL) {
					int bundleID = request.get_ValueAsInt("SAS_RequestBundle_ID");
					if (bundleID > 0) {
						MRequestBundle bundle = new MRequestBundle(Env.getCtx(), bundleID, po.get_TrxName());
						if (bundle.getDepart().equals("Y")) {
							throw new AdempiereException("Document is already departed");
						}
					}
				}
			}
		} else if (event.getTopic().equals(IEventTopics.PO_AFTER_DELETE)) {
			if (po.get_TableName().equals(MOrderLine.Table_Name)){
				MOrderLine ol = (MOrderLine) po;
				MOrder order = ol.getParent();
				String promotionCode = (String)order.get_Value("PromotionCode");
				if (ol.getC_Charge_ID() > 0) {
					Integer promotionID = (Integer) ol.get_Value("M_Promotion_ID");
					if (promotionID != null && promotionID.intValue() > 0) {

						int M_PromotionPreCondition_ID = SASPromotionRule.findPromotionPreConditionId(
								order, promotionCode, promotionID);
						if (M_PromotionPreCondition_ID > 0) {
							String update = "UPDATE M_PromotionPreCondition SET PromotionCounter = PromotionCounter - 1 "
									+ "WHERE M_PromotionPreCondition_ID = ?";
							DB.executeUpdate(update, M_PromotionPreCondition_ID, order.get_TrxName());
						}
					}
				}
			
			}
		}
		
		if (event.getTopic().equals(IEventTopics.DOC_BEFORE_PREPARE)) {
			if (po.get_TableName().equals(MOrder.Table_Name)){
				MOrder order = (MOrder) po;
				ICOrder orderCustom = POWrapper.create(order, ICOrder.class);
				MDocType docType = MDocType.get(Env.getCtx(), order.getC_DocTypeTarget_ID());
				
				// SAS-31 DocType active validation
				if (!docType.isActive()) {
					throw new AdempiereException("Document type is not active");
				}
				
				// SAS-82 require to calculate for discount calculation
				if (order.getTotalLines().equals(Env.ZERO)) {
					order.calculateTaxTotal();
				}
				
				// SAS-59 Run promotion only in Sales Order and only if not shipped/invoiced yet
				if (order.isSOTrx() &&
						docType.getDocBaseType().equals(MDocType.DOCBASETYPE_SalesOrder) && 
						(docType.getDocSubTypeSO().equals(MDocType.DOCSUBTYPESO_StandardOrder) || 
								docType.getDocSubTypeSO().equals(MDocType.DOCSUBTYPESO_Proposal) ||
								docType.getDocSubTypeSO().equals(MDocType.DOCSUBTYPESO_Quotation)) &&
						order.getQuotationOrder_ID() <= 0 &&
						order.getC_DocTypeTarget_ID() != SASSystemID.DOCTYPE_ORDER_LAIN_LAIN &&
						!isShipped(order) && !isInvoiced(order) && !docType.get_ValueAsBoolean("IsInternal") ) {
					
					try {
						SASPromotionReward.generateRewards(order);
						order.getLines(true, null);
						order.calculateTaxTotal();
						order.saveEx();
						
						SASPromotionRule.applyPromotions(order);
						order.getLines(true, null);
						order.calculateTaxTotal();
						order.saveEx();
						SASPromotionRule.increasePromotionCounter(order);
					} catch (Exception e) {
						if (e instanceof RuntimeException)
							throw (RuntimeException)e;
						else
							throw new AdempiereException(e.getLocalizedMessage(), e);
					}
				}
				
				// moved to MOrder.prepareIt()
//				if (order.isSOTrx() && !docType.isProposal()) {
//					// SAS-27 validation availability per Org
//					MWarehouse wh = MWarehouse.get(Env.getCtx(), order.getM_Warehouse_ID());
//					if (wh.getM_ReserveLocator_ID() > 0) {
//						MLocator loc = MLocator.get(Env.getCtx(), wh.getM_ReserveLocator_ID());
//						if (loc.getM_LocatorType_ID() > 0 && loc.getM_LocatorType().isAvailableForReservation()) {
//							StringBuilder sql = new StringBuilder();
//							sql.append("SELECT ol.M_Product_ID, wh.AD_Org_ID, ");
//							sql.append("SUM(ol.QtyOrdered) QtyOrdered, SUM(ol.QtyReserved) QtyReserved ");
//							sql.append("FROM C_OrderLine ol ");
//							sql.append("INNER JOIN C_Order o ON (o.C_Order_ID = ol.C_Order_ID) ");
//							sql.append("INNER JOIN M_Product p ON (p.M_Product_ID = ol.M_Product_ID) ");
//							sql.append("INNER JOIN M_Warehouse wh ON (wh.M_Warehouse_ID = o.M_Warehouse_ID) ");
//							sql.append("WHERE ol.C_Order_ID = ? AND p.IsStocked='Y' ");
//							sql.append("GROUP BY ol.M_Product_ID, wh.AD_Org_ID ");
//							
//							PreparedStatement pstmt = null;
//							ResultSet rs = null;
//							
//							try
//							{
//								pstmt = DB.prepareStatement(sql.toString(), po.get_TrxName());
//								DB.setParameters(pstmt, new Object[]{order.get_ID()});
//								rs = pstmt.executeQuery();
//								while (rs.next()) {
//									int productID = rs.getInt("M_Product_ID");
//									int orgID = rs.getInt("AD_Org_ID");
//									BigDecimal qtyOrdered = rs.getBigDecimal("QtyOrdered");
//									BigDecimal qtyReserved = rs.getBigDecimal("QtyReserved");
//									BigDecimal qtyDiff = qtyOrdered.subtract(qtyReserved);
//									if (qtyDiff.compareTo(Env.ZERO) > 0) {
//										StringBuilder sqlAvailable = new StringBuilder();
//										sqlAvailable.append("SELECT COALESCE(SUM(s.QtyOnHand - s.QtyReserved),0) ");
//										sqlAvailable.append("FROM M_Storage s ");
//										sqlAvailable.append("INNER JOIN M_Locator l ON (l.M_Locator_ID = s.M_Locator_ID) ");
//										sqlAvailable.append("INNER JOIN M_LocatorType lt ON (lt.M_LocatorType_ID = l.M_LocatorType_ID) ");
//										sqlAvailable.append("WHERE lt.IsAvailableForReservation='Y' AND s.M_Product_ID = ? AND l.AD_Org_ID = ? ");
//										BigDecimal availableQty = DB.getSQLValueBD(po.get_TrxName(), sqlAvailable.toString(), 
//												productID, orgID);
//										if (qtyDiff.compareTo(availableQty) > 0) {
//											throw new AdempiereException("SAS-27 Cannot reserve " + qtyDiff + " of " +
//													MProduct.get(Env.getCtx(), productID).getValue() +
//													" in " + MOrg.get(Env.getCtx(), orgID).getName() + 
//													". Available Qty: " + availableQty);
//										}
//									}
//								}
//							}
//							catch (SQLException e)
//							{
//								throw new DBException(e, sql.toString());
//							}
//							finally
//							{
//								DB.close(rs, pstmt);
//								rs = null; pstmt = null;
//							}
//						}
//					}
//				}
				
				List<MOrderLine> lines = new Query(Env.getCtx(), MOrderLine.Table_Name, "C_Order_ID=?", po.get_TrxName())
					.setParameters(order.get_ID())
					.list();
				
				if (order.isSOTrx() && !docType.get_ValueAsBoolean("IsInternal")) {
					/* Drop inconsistent discount list validation
					// SAS-31 discount list must be identic
					int discountListID = -1;
					for (MOrderLine line : lines) {
						if (line.getM_Product_ID() > 0) {
							if (discountListID < 0) {
								discountListID = line.get_ValueAsInt("SAS_DiscountList_ID");
							}
							
							if (discountListID != line.get_ValueAsInt("SAS_DiscountList_ID")) {
								throw new AdempiereUserError("Inconsistent discount list");
							}
						}
					}
					*/
					
					// SAS-31 List Price Validation
					if (order.getC_DocTypeTarget_ID() != SASSystemID.DOCTYPE_SO_RETURN_A &&
							order.getC_DocTypeTarget_ID() != SASSystemID.DOCTYPE_SO_RETURN_B &&
							order.getC_DocTypeTarget_ID() != SASSystemID.DOCTYPE_ORDER_LAIN_LAIN) {
						for (MOrderLine line : lines) {
							if (line.getM_Product_ID() > 0) {
								MProductPricing pp = new MProductPricing (line.getM_Product_ID(), line.getC_BPartner_ID(), line.getQtyOrdered(), order.isSOTrx());
								int M_PriceList_ID = order.getM_PriceList_ID();
								pp.setM_PriceList_ID(M_PriceList_ID);
								Timestamp orderDate = order.getDateOrdered();
								int M_PriceList_Version_ID = 0;
								if ( M_PriceList_Version_ID == 0 && M_PriceList_ID > 0)
								{
									String sql = "SELECT plv.M_PriceList_Version_ID "
										+ "FROM M_PriceList_Version plv "
										+ "WHERE plv.M_PriceList_ID=? "						//	1
										+ " AND plv.ValidFrom <= ? "
										+ "ORDER BY plv.ValidFrom DESC";
									//	Use newest price list - may not be future
	
									M_PriceList_Version_ID = DB.getSQLValueEx(null, sql, M_PriceList_ID, orderDate);
								}
								pp.setM_PriceList_Version_ID(M_PriceList_Version_ID);
								pp.setPriceDate(orderDate);
								
								if (line.getPriceList().compareTo(pp.getPriceStd()) < 0) {
									throw new AdempiereUserError("Line " + line.getLine() + 
											": List Price [" + line.getPriceList() + 
											"] is lower than standard [" + pp.getPriceList() + "]");
								}
							}
						}
					}
					
//					// SAS-82 set activity based on total discount
//					BigDecimal totalPrice = Env.ZERO;
//					
//					for (MOrderLine line : lines) {
//						totalPrice = totalPrice.add(line.getPriceList()
//								.multiply(line.getQtyOrdered()));
//					}
//
//					BigDecimal discount = Env.ZERO;
//					if (totalPrice.compareTo(Env.ZERO) != 0) {
//						discount = (totalPrice.subtract(order
//								.getTotalLines())).multiply(Env.ONEHUNDRED).divide(
//								totalPrice, 2, RoundingMode.HALF_UP);
//					}
//					order.set_ValueOfColumn("TotalDiscount", discount);
//					if (discount.compareTo(BigDecimal.valueOf(15)) <= 0) {
//						order.setC_Activity_ID(SASSystemID.ACTIVITY_DISC01);
//					} else if (discount.compareTo(BigDecimal.valueOf(25)) <= 0) {
//						order.setC_Activity_ID(SASSystemID.ACTIVITY_DISC02);
//					} else {
//						order.setC_Activity_ID(SASSystemID.ACTIVITY_DISC03);
//					}
//					order.saveEx();
					
					// SAS-69 Credit Limit logic
					if (docType.getDocBaseType().equals(MDocType.DOCBASETYPE_SalesOrder) && 
						(docType.getDocSubTypeSO().equals(MDocType.DOCSUBTYPESO_StandardOrder) || docType.getDocSubTypeSO().equals(MDocType.DOCSUBTYPESO_Quotation))) {
						
						MBPRule bpRule = new Query(Env.getCtx(), MBPRule.Table_Name, "C_BPartner_ID = ? AND AD_OrgTrx_ID = ?", po.get_TrxName())
							.setOnlyActiveRecords(true)
							.setParameters(order.getBill_BPartner_ID(), order.getAD_OrgTrx_ID())
							.firstOnly();
						
						BigDecimal totalCreditLimit = new Query(Env.getCtx(), MBPRule.Table_Name, "C_BPartner_ID = ?", po.get_TrxName())
							.setOnlyActiveRecords(true)
							.setParameters(order.getBill_BPartner_ID())
							.sum(MBPRule.COLUMNNAME_SO_CreditLimit);
						
						if (bpRule != null && bpRule.getSO_CreditLimit().compareTo(Env.ZERO) > 0) {
							
							if(docType.getDocSubTypeSO().equals(MDocType.DOCSUBTYPESO_StandardOrder)) {
									StringBuilder sqlSOAmt = new StringBuilder();
									sqlSOAmt.append("SELECT COALESCE(SUM(o.GrandTotal),0) ");
									sqlSOAmt.append("FROM C_Order o ");
									sqlSOAmt.append("INNER JOIN C_DocType dt ON (dt.C_DocType_ID = o.C_DocType_ID) ");
									sqlSOAmt.append("WHERE o.IsSOTrx = 'Y' AND o.DocStatus IN ('CO','IP') ");
									sqlSOAmt.append("AND dt.DocBaseType = 'SOO' AND dt.IsInternal = 'N' AND dt.DocSubTypeSO IN ('SO','OB') ");
									sqlSOAmt.append("AND o.C_Order_ID <> ? AND o.Bill_BPartner_ID = ? ");
								
								BigDecimal soTotalAmt = DB.getSQLValueBD(po.get_TrxName(), sqlSOAmt.toString(), 
										order.get_ID(), order.getBill_BPartner_ID());
								soTotalAmt = soTotalAmt.add(order.getGrandTotal());
								
								sqlSOAmt.append("AND o.AD_OrgTrx_ID = ? ");
								BigDecimal soAmt = DB.getSQLValueBD(po.get_TrxName(), sqlSOAmt.toString(), 
										order.get_ID(), order.getBill_BPartner_ID(), order.getAD_OrgTrx_ID());
								soAmt = soAmt.add(order.getGrandTotal());
								
								StringBuilder sqlSOClosedAmt = new StringBuilder();
								sqlSOClosedAmt.append("SELECT COALESCE(SUM((ol.QtyOrdered * ol.PriceActual) + ");
								sqlSOClosedAmt.append("CASE WHEN pl.IsTaxIncluded = 'N' THEN (ol.QtyOrdered * ol.PriceActual * t.Rate / 100) ELSE 0 END),0) ");
								sqlSOClosedAmt.append("FROM C_Order o ");
								sqlSOClosedAmt.append("INNER JOIN C_OrderLine ol ON (ol.C_Order_ID = o.C_Order_ID) ");
								sqlSOClosedAmt.append("INNER JOIN C_Tax t ON (t.C_Tax_ID = ol.C_Tax_ID) ");
								sqlSOClosedAmt.append("INNER JOIN C_DocType dt ON (dt.C_DocType_ID = o.C_DocType_ID) ");
								sqlSOClosedAmt.append("INNER JOIN M_PriceList pl ON (pl.M_PriceList_ID = o.M_PriceList_ID) ");
								sqlSOClosedAmt.append("WHERE o.IsSOTrx = 'Y' AND dt.IsInternal = 'N' AND o.DocStatus IN ('CL') ");
								sqlSOClosedAmt.append("AND dt.DocBaseType = 'SOO' AND dt.DocSubTypeSO = 'SO' ");
								sqlSOClosedAmt.append("AND o.C_Order_ID <> ? AND o.Bill_BPartner_ID = ? ");
								
								BigDecimal soTotalClosedAmt = DB.getSQLValueBD(po.get_TrxName(), sqlSOClosedAmt.toString(), 
										order.get_ID(), order.getBill_BPartner_ID());
								soTotalAmt = soTotalAmt.add(soTotalClosedAmt);
								
								sqlSOClosedAmt.append("AND o.AD_OrgTrx_ID = ? ");
								BigDecimal soClosedAmt = DB.getSQLValueBD(po.get_TrxName(), sqlSOClosedAmt.toString(), 
										order.get_ID(), order.getBill_BPartner_ID(), order.getAD_OrgTrx_ID());
								soAmt = soAmt.add(soClosedAmt);
								
								StringBuilder sqlTotalAllocationAmt = new StringBuilder();
								sqlTotalAllocationAmt.append("SELECT COALESCE(SUM(al.Amount),0) ");
								sqlTotalAllocationAmt.append("FROM C_AllocationLine al ");
								sqlTotalAllocationAmt.append("INNER JOIN C_AllocationHdr ah ON (ah.C_AllocationHdr_ID = al.C_AllocationHdr_ID) ");
								sqlTotalAllocationAmt.append("INNER JOIN C_Invoice i ON (i.C_Invoice_ID = al.C_Invoice_ID) ");
								sqlTotalAllocationAmt.append("INNER JOIN C_DocType dt ON (dt.C_DocType_ID = i.C_DocType_ID) ");
								sqlTotalAllocationAmt.append("LEFT JOIN C_Payment py ON (py.C_Payment_ID = al.C_Payment_ID) ");
								sqlTotalAllocationAmt.append("WHERE i.IsSOTrx='Y' AND ah.DocStatus IN ('CO','CL') ");
								sqlTotalAllocationAmt.append("AND dt.DocBaseType NOT IN ('APC','ARC') ");
								sqlTotalAllocationAmt.append("AND al.C_BPartner_ID = ? ");
								
								BigDecimal totalAllocationAmt = DB.getSQLValueBD(po.get_TrxName(), sqlTotalAllocationAmt.toString(), 
										order.getBill_BPartner_ID());
								
								StringBuilder sqlAllocationAmt = new StringBuilder(sqlTotalAllocationAmt.toString());
								sqlAllocationAmt.append("AND i.AD_OrgTrx_ID = ? ");
								BigDecimal allocationAmt = DB.getSQLValueBD(po.get_TrxName(), sqlAllocationAmt.toString(), 
										order.getBill_BPartner_ID(), order.getAD_OrgTrx_ID());
								
								String futureAllocationClause = "AND ADDDAYS(ah.DateTrx,2) > CURRENT_DATE AND py.TenderType = 'K' ";
								sqlTotalAllocationAmt.append(futureAllocationClause);
								sqlAllocationAmt.append(futureAllocationClause);
								
								BigDecimal totalFutureAllocationAmt = DB.getSQLValueBD(po.get_TrxName(), sqlTotalAllocationAmt.toString(), 
										order.getBill_BPartner_ID());
								
								BigDecimal futureAllocationAmt = DB.getSQLValueBD(po.get_TrxName(), sqlAllocationAmt.toString(), 
										order.getBill_BPartner_ID(), order.getAD_OrgTrx_ID());
								
								BigDecimal totalOpenAmt = soTotalAmt.subtract(totalAllocationAmt).add(totalFutureAllocationAmt);
								BigDecimal openAmt = soAmt.subtract(allocationAmt).add(futureAllocationAmt);
								
								if (totalOpenAmt.compareTo(totalCreditLimit) > 0) {
									orderCustom.setSO_CreditOver(totalOpenAmt.subtract(totalCreditLimit));
									order.saveEx();
									MWFScenario.setScenario(order, MWFScenario.SCENARIO_CREDIT_OVER);
								} else if (openAmt.compareTo(bpRule.getSO_CreditLimit()) > 0) {
									orderCustom.setSO_CreditOver(openAmt.subtract(bpRule.getSO_CreditLimit()));
									order.saveEx();
									MWFScenario.setScenario(order, MWFScenario.SCENARIO_CREDIT_OVER);
								} else {
									orderCustom.setSO_CreditOver(Env.ZERO);
									orderCustom.setZ_WFScenarioCredit_ID(0);
								}
								order.saveEx();
							} else if (docType.getDocSubTypeSO().equals(MDocType.DOCSUBTYPESO_Quotation)) {

								StringBuilder sqlSOAmt = new StringBuilder();
								sqlSOAmt.append("SELECT COALESCE(SUM(o.GrandTotal),0) ");
								sqlSOAmt.append("FROM C_Order o ");
								sqlSOAmt.append("INNER JOIN C_DocType dt ON (dt.C_DocType_ID = o.C_DocType_ID) ");
								sqlSOAmt.append("WHERE o.IsSOTrx = 'Y' AND o.DocStatus IN ('CO','IP') ");
								sqlSOAmt.append("AND dt.DocBaseType = 'SOO' AND dt.IsInternal = 'N' AND dt.DocSubTypeSO IN ('SO','OB') ");
								sqlSOAmt.append("AND o.C_Order_ID <> ? AND o.Bill_BPartner_ID = ? ");
							
							BigDecimal soTotalAmt = DB.getSQLValueBD(po.get_TrxName(), sqlSOAmt.toString(), 
									order.get_ID(), order.getBill_BPartner_ID());
							soTotalAmt = soTotalAmt.add(order.getGrandTotal());
							
							sqlSOAmt.append("AND o.AD_OrgTrx_ID = ? ");
							BigDecimal soAmt = DB.getSQLValueBD(po.get_TrxName(), sqlSOAmt.toString(), 
									order.get_ID(), order.getBill_BPartner_ID(), order.getAD_OrgTrx_ID());
							soAmt = soAmt.add(order.getGrandTotal());
							
							StringBuilder sqlSOClosedAmt = new StringBuilder();
							sqlSOClosedAmt.append("SELECT COALESCE(SUM((ol.QtyOrdered * ol.PriceActual) + ");
							sqlSOClosedAmt.append("CASE WHEN pl.IsTaxIncluded = 'N' THEN (ol.QtyOrdered * ol.PriceActual * t.Rate / 100) ELSE 0 END),0) ");
							sqlSOClosedAmt.append("FROM C_Order o ");
							sqlSOClosedAmt.append("INNER JOIN C_OrderLine ol ON (ol.C_Order_ID = o.C_Order_ID) ");
							sqlSOClosedAmt.append("INNER JOIN C_Tax t ON (t.C_Tax_ID = ol.C_Tax_ID) ");
							sqlSOClosedAmt.append("INNER JOIN C_DocType dt ON (dt.C_DocType_ID = o.C_DocType_ID) ");
							sqlSOClosedAmt.append("INNER JOIN M_PriceList pl ON (pl.M_PriceList_ID = o.M_PriceList_ID) ");
							sqlSOClosedAmt.append("WHERE o.IsSOTrx = 'Y' AND o.DocStatus IN ('CL') ");
							sqlSOClosedAmt.append("AND dt.DocBaseType = 'SOO' AND dt.IsInternal = 'N' AND dt.DocSubTypeSO = 'OB' ");
							sqlSOClosedAmt.append("AND o.C_Order_ID <> ? AND o.Bill_BPartner_ID = ? ");
							
							BigDecimal soTotalClosedAmt = DB.getSQLValueBD(po.get_TrxName(), sqlSOClosedAmt.toString(), 
									order.get_ID(), order.getBill_BPartner_ID());
							soTotalAmt = soTotalAmt.add(soTotalClosedAmt);
							
							sqlSOClosedAmt.append("AND o.AD_OrgTrx_ID = ? ");
							BigDecimal soClosedAmt = DB.getSQLValueBD(po.get_TrxName(), sqlSOClosedAmt.toString(), 
									order.get_ID(), order.getBill_BPartner_ID(), order.getAD_OrgTrx_ID());
							soAmt = soAmt.add(soClosedAmt);
							
							StringBuilder sqlTotalAllocationAmt = new StringBuilder();
							sqlTotalAllocationAmt.append("SELECT COALESCE(SUM(al.Amount),0) ");
							sqlTotalAllocationAmt.append("FROM C_AllocationLine al ");
							sqlTotalAllocationAmt.append("INNER JOIN C_AllocationHdr ah ON (ah.C_AllocationHdr_ID = al.C_AllocationHdr_ID) ");
							sqlTotalAllocationAmt.append("INNER JOIN C_Invoice i ON (i.C_Invoice_ID = al.C_Invoice_ID) ");
							sqlTotalAllocationAmt.append("INNER JOIN C_DocType dt ON (dt.C_DocType_ID = i.C_DocType_ID) ");
							sqlTotalAllocationAmt.append("LEFT JOIN C_Payment py ON (py.C_Payment_ID = al.C_Payment_ID) ");
							sqlTotalAllocationAmt.append("WHERE i.IsSOTrx='Y' AND ah.DocStatus IN ('CO','CL') ");
							sqlTotalAllocationAmt.append("AND dt.DocBaseType NOT IN ('APC','ARC') ");
							sqlTotalAllocationAmt.append("AND al.C_BPartner_ID = ? ");
							
							BigDecimal totalAllocationAmt = DB.getSQLValueBD(po.get_TrxName(), sqlTotalAllocationAmt.toString(), 
									order.getBill_BPartner_ID());
							
							StringBuilder sqlAllocationAmt = new StringBuilder(sqlTotalAllocationAmt.toString());
							sqlAllocationAmt.append("AND i.AD_OrgTrx_ID = ? ");
							BigDecimal allocationAmt = DB.getSQLValueBD(po.get_TrxName(), sqlAllocationAmt.toString(), 
									order.getBill_BPartner_ID(), order.getAD_OrgTrx_ID());
							
							String futureAllocationClause = "AND ADDDAYS(ah.DateTrx,2) > CURRENT_DATE AND py.TenderType = 'K' ";
							sqlTotalAllocationAmt.append(futureAllocationClause);
							sqlAllocationAmt.append(futureAllocationClause);
							
							BigDecimal totalFutureAllocationAmt = DB.getSQLValueBD(po.get_TrxName(), sqlTotalAllocationAmt.toString(), 
									order.getBill_BPartner_ID());
							
							BigDecimal futureAllocationAmt = DB.getSQLValueBD(po.get_TrxName(), sqlAllocationAmt.toString(), 
									order.getBill_BPartner_ID(), order.getAD_OrgTrx_ID());
							
							BigDecimal totalOpenAmt = soTotalAmt.subtract(totalAllocationAmt).add(totalFutureAllocationAmt);
							BigDecimal openAmt = soAmt.subtract(allocationAmt).add(futureAllocationAmt);
							
							if (totalOpenAmt.compareTo(totalCreditLimit) > 0) {
								orderCustom.setSO_CreditOver(totalOpenAmt.subtract(totalCreditLimit));
								order.saveEx();
								MWFScenario.setScenario(order, MWFScenario.SCENARIO_CREDIT_OVER);
							} else if (openAmt.compareTo(bpRule.getSO_CreditLimit()) > 0) {
								orderCustom.setSO_CreditOver(openAmt.subtract(bpRule.getSO_CreditLimit()));
								order.saveEx();
								MWFScenario.setScenario(order, MWFScenario.SCENARIO_CREDIT_OVER);
							} else {
								orderCustom.setSO_CreditOver(Env.ZERO);
								orderCustom.setZ_WFScenarioCredit_ID(0);
							}
							order.saveEx();
							}
						} else {
							throw new AdempiereUserError("Credit limit in BP Rule is not set");
						}
						
						// SAS-118 Aging Over logic
						StringBuilder sql = new StringBuilder();
						sql.append("SELECT i.C_Invoice_ID ");
						sql.append("FROM C_Invoice i ");
						sql.append("LEFT JOIN C_AllocationLine al ON (al.C_Invoice_ID = i.C_Invoice_ID) ");
						sql.append("LEFT JOIN C_AllocationHdr ah ON (ah.C_AllocationHdr_ID = al.C_AllocationHdr_ID) ");
						sql.append("LEFT JOIN C_Payment p ON (p.C_Payment_ID = al.C_Payment_ID) ");
						sql.append("LEFT JOIN c_paymentterm pt ON (pt.c_paymentterm_id = i.c_paymentterm_id) ");
						sql.append("WHERE i.IsSOTrx='Y' ");
						sql.append("AND i.DocStatus IN ('CO','CL') ");
						sql.append("AND i.C_DocType_ID NOT IN (550289,1000004,550294,550293) ");
						sql.append("AND i.C_BPartner_ID = ? ");
						sql.append("AND (i.IsPaid='N' OR ");
						sql.append("	(i.IsPaid='Y' AND ah.DocStatus IN ('CO','CL')  ");
						sql.append("	AND p.DocStatus IN ('CO','CL') ");
						sql.append("	AND p.TenderType='K' ");
						sql.append("	AND p.IsCashed='N' ");
						sql.append(" 	)) ");
						sql.append("ORDER BY TRUNC(i.dateinvoiced ) + pt.NetDays + pt.gracedays ");
						sql.append("FETCH FIRST 1 ROWS ONLY ");
						
						int invoiceID = DB.getSQLValue(po.get_TrxName(), sql.toString(), order.getBill_BPartner_ID());
						
//						MInvoice invoice = new Query(Env.getCtx(), MInvoice.Table_Name, 
//								"C_BPartner_ID = ? AND IsSOTrx='Y' AND DocStatus IN ('CO','CL') AND IsPaid='N' AND C_DocType_ID NOT IN (550289,1000004,550294,550293)", po.get_TrxName())
//							.setParameters(order.getBill_BPartner_ID())
//							.setOrderBy(MInvoice.COLUMNNAME_DateInvoiced)
//							.first();
						if (invoiceID > 0) {
							MInvoice invoice = MInvoice.get(po.getCtx(), invoiceID);
							MPaymentTerm paymentTerm = new MPaymentTerm(Env.getCtx(), invoice.getC_PaymentTerm_ID(), po.get_TrxName());
							
							Timestamp dueDate = invoice.getDateInvoiced();
							dueDate = TimeUtil.addDays(dueDate, paymentTerm.getNetDays() + paymentTerm.getGraceDays());
							
							Timestamp currentDate = new Timestamp(System.currentTimeMillis());
							currentDate = TimeUtil.addDays(currentDate, 0);
							
							if (dueDate.before(currentDate)) {
								int daysBetween = TimeUtil.getDaysBetween(dueDate, currentDate);
								orderCustom.setSO_AgingOver(BigDecimal.valueOf(daysBetween));
								order.saveEx();
								MWFScenario.setScenario(order, MWFScenario.SCENARIO_AGING_OVER);
							} else {
								orderCustom.setSO_AgingOver(Env.ZERO);
								orderCustom.setZ_WFScenarioAging_ID(0);
							}
						} else {
							orderCustom.setSO_AgingOver(Env.ZERO);
							orderCustom.setZ_WFScenarioAging_ID(0);
						}
						order.saveEx();
					}
					
					// Reset price limit approval from ISY
//					orderCustom.setZ_WFScenarioPrice_ID(0);
//					order.saveEx();
					
//					// Price Limit Approval SAS-148
					// Moved to after prepare
//					BigDecimal discountMin = DB.getSQLValueBD(order.get_TrxName(), 
//							"(SELECT MIN(ol.Std_Discount - ol.Discount) FROM C_OrderLine ol WHERE ol.C_Order_ID = ? AND ol.M_Product_ID > 0)", 
//							order.get_ID());
//					if(discountMin != null && discountMin.setScale(2,BigDecimal.ROUND_HALF_UP).compareTo(Env.ZERO.setScale(2,BigDecimal.ROUND_HALF_UP)) < 0){
//						MWFScenario.setScenario(order,MWFScenario.SCENARIO_PRICE_LIMIT);
//					} else {
//						orderCustom.setZ_WFScenarioPrice_ID(0);
//						order.saveEx();
//					}
				}
				
				//https://databiz.atlassian.net/browse/SAS-251 Aktualisasi Disc K Saldo pada Sales Order
				MBPartner bp = MBPartner.get(Env.getCtx(), order.getC_BPartner_ID());
				if (order.isSOTrx() && order.get_ValueAsBoolean("IsAffectPromo") && bp.get_ValueAsInt("SAS_C_BPartnerNGK_ID") > 0) {
					SalesOrderActualization.PrepareActualizationOrder(order);
				}
				
				// SAS-82 set activity based on total discount
				BigDecimal totalPrice = Env.ZERO;
				BigDecimal discount = Env.ZERO;

				if (order.isSOTrx() && !docType.get_ValueAsBoolean("IsInternal")) {
					
					List<MOrderLine> orderlines = new Query(Env.getCtx(), MOrderLine.Table_Name, "C_Order_ID=?", po.get_TrxName())
							.setParameters(order.get_ID())
							.list();
					
					for (MOrderLine line : orderlines) {
						totalPrice = totalPrice.add(line.getPriceList()
								.multiply(line.getQtyOrdered()));
					}
					
					
					BigDecimal totallines = new Query(Env.getCtx(), MOrderLine.Table_Name, "C_Order_ID = ? ", po.get_TrxName())
							.setOnlyActiveRecords(true)
							.setParameters(new Object[]{order.getC_Order_ID()})
							.sum(MOrderLine.COLUMNNAME_LineNetAmt);
					
	//				BigDecimal totallines = order.getTotalLines();
					
					if (totalPrice.compareTo(Env.ZERO) != 0) {
						discount = (totalPrice.subtract(totallines)).multiply(Env.ONEHUNDRED).divide(
								totalPrice, 2, RoundingMode.HALF_UP);
					}
					order.set_ValueOfColumn("TotalDiscount", discount);
					if (discount.compareTo(BigDecimal.valueOf(15)) <= 0) {
						order.setC_Activity_ID(SASSystemID.ACTIVITY_DISC01);
					} else if (discount.compareTo(BigDecimal.valueOf(25)) <= 0) {
						order.setC_Activity_ID(SASSystemID.ACTIVITY_DISC02);
					} else {
						order.setC_Activity_ID(SASSystemID.ACTIVITY_DISC03);
					}
					order.saveEx();
				}
				//END SAS-82
				
				//https://databiz.atlassian.net/browse/SAS-255 Validasi / Cek Disc K Saldo pada Sales Order
				if (order.isSOTrx() && order.getC_DocType_ID() != 550420) {
					
					SalesOrderActualization.CheckPriceDiscount(order);
					
				}
				
				////SAS-31
				//Set Campaign To Null
//				String deletecampaign = " update c_order set c_campaign_id = null "
//						+ "where c_order_id = "+order.getC_Order_ID();
//				DB.executeUpdateEx(deletecampaign.toString(), po.get_TrxName());
//				try {
//					DB.commit(false, po.get_TrxName());
//				} catch (IllegalStateException | SQLException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
				
				int doctypeID = order.getC_DocType_ID();
				
				if(doctypeID == 550265 || doctypeID == 1000030 || doctypeID == 550269 || 
						doctypeID == 1000026 || doctypeID == 550270 || doctypeID == 1000027) {
				
					//Count Charge
					String countksql ="select count(*) "
							+ "from c_orderline ol "
							+ "join c_charge c on c.c_charge_id = ol.c_charge_id "
							+ "where c.description = 'K' and ol.c_order_id = ? ";
					int countk = DB.getSQLValue(po.get_TrxName(), countksql, order.getC_Order_ID());
					MOrg orgtrx = MOrg.get(Env.getCtx(), order.getAD_OrgTrx_ID());
									
					if (countk > 0) {
						
						//Count Group3
						String countgroup3sqls ="select count(*) "
								+ "from c_orderline ol "
								+ "join c_order o on o.c_order_id = ol.c_order_id  "
								+ "join m_product p on p.m_product_id = ol.m_product_id  "
								+ "join ad_ref_list arl on arl.value = p.group3 and arl.ad_reference_id = 550208 "
								+ "where p.group3 is not null  "
								+ "and ol.c_order_id = ? ";
						int countgroup3s = DB.getSQLValue(po.get_TrxName(), countgroup3sqls, order.getC_Order_ID());
						
						//Count Group3 With TotalDiscount > Limit
						String countgroup3sql ="select count(*) "
								+ "from c_orderline ol "
								+ "join c_order o on o.c_order_id = ol.c_order_id  "
								+ "join m_product p on p.m_product_id = ol.m_product_id  "
								+ "join ad_ref_list arl on arl.value = p.group3 and arl.ad_reference_id = 550208 "
								+ "where p.group3 is not null  "
								+ "and o.totaldiscount > coalesce(CAST(arl.description AS NUMERIC),0) "
								+ "and ol.c_order_id = ? ";
						int countgroup3 = DB.getSQLValue(po.get_TrxName(), countgroup3sql, order.getC_Order_ID());
						
						if (countgroup3 > 0) {
							//Update Campaign
	//						String sqlcampaign = " update c_order set c_campaign_id = (select c.c_campaign_id  from c_campaign c where c.value = 'Discount-K' fetch first 1 row only) "
	//								+ "where c_order_id = "+order.getC_Order_ID();
	//						DB.executeUpdateEx(sqlcampaign.toString(), po.get_TrxName());
	//						try {
	//							DB.commit(false, po.get_TrxName());
	//						} catch (IllegalStateException | SQLException e) {
	//							// chroo
	//							e.printStackTrace();
	//						}
							
							///Dinonaktifkan 20240807 diganti error
//							String campaignsql ="select c.c_campaign_id  from c_campaign c where c.value = 'Discount-K' fetch first 1 row only ";
//							int campaignsqls = DB.getSQLValue(po.get_TrxName(), campaignsql);
//							
//							order.setC_Campaign_ID(campaignsqls);
//							order.saveEx();
							
							throw new AdempiereException("Error. TotalDiscount > Limit");
						}
						
						else if (countgroup3s == 0 && discount.compareTo(BigDecimal.valueOf(orgtrx.get_ValueAsInt("Limit_Discount"))) >0 ) {
							//Update Campaign
	//						String sqlcampaign = " update c_order set c_campaign_id = (select c.c_campaign_id  from c_campaign c where c.value = 'Discount-K' fetch first 1 row only) "
	//								+ "where c_order_id = "+order.getC_Order_ID();
	//						DB.executeUpdateEx(sqlcampaign.toString(), po.get_TrxName());
	//						try {
	//							DB.commit(false, po.get_TrxName());
	//						} catch (IllegalStateException | SQLException e) {
	//							// TODO Auto-generated catch block
	//							e.printStackTrace();
	//						}
							
							///Dinonaktifkan 20240807 diganti error
//							String campaignsql ="select c.c_campaign_id  from c_campaign c where c.value = 'Discount-K' fetch first 1 row only ";
//							int campaignsqls = DB.getSQLValue(po.get_TrxName(), campaignsql);
//							
//							order.setC_Campaign_ID(campaignsqls);
//							order.saveEx();
							throw new AdempiereException("Error. TotalDiscount > Limit");

						}
						else {
//							order.setC_Campaign_ID(0);
//							order.saveEx();
						}
					} else {
//						order.setC_Campaign_ID(0);
//						order.saveEx();
					}
				
				}
				
				//End SAS-31
				
						
			} else if (po.get_TableName().equals(MMovement.Table_Name)){
				MMovement movement = (MMovement) po;
				
				if (movement.getReversal_ID() == 0 && movement.getC_DocType_ID() != SASSystemID.DOCTYPE_IM_TERIMA) {
					// SAS-146 Qty On Hand Validation
					StringBuilder sql = new StringBuilder();
					sql.append("SELECT ml.M_Locator_ID, ml.M_Product_ID, SUM(ml.MovementQty) AS MovementQty ");
					sql.append("FROM M_MovementLine ml ");
					sql.append("INNER JOIN M_Product p ON (p.M_Product_ID = ml.M_Product_ID) ");
					sql.append("WHERE ml.M_Movement_ID = ? AND p.IsStocked = 'Y' ");
					sql.append("GROUP BY ml.M_Locator_ID, ml.M_Product_ID ");
					
					PreparedStatement pstmt = null;
					ResultSet rs = null;
					
					try
					{
						pstmt = DB.prepareStatement(sql.toString(), po.get_TrxName());
						DB.setParameters(pstmt, new Object[]{movement.get_ID()});
						rs = pstmt.executeQuery();
						while (rs.next()) {
							int productID = rs.getInt("M_Product_ID");
							int locatorID = rs.getInt("M_Locator_ID");
							BigDecimal movementQty = rs.getBigDecimal("MovementQty");
							if (movementQty.compareTo(Env.ZERO) > 0) {
								StringBuilder sqlAvailable = new StringBuilder();
								sqlAvailable.append("SELECT COALESCE(SUM(s.QtyOnHand),0) ");
								sqlAvailable.append("FROM M_Storage s ");
								sqlAvailable.append("INNER JOIN M_Locator l ON (l.M_Locator_ID = s.M_Locator_ID) ");
								sqlAvailable.append("WHERE s.M_Product_ID = ? AND l.M_Locator_ID = ? ");
								BigDecimal availableQty = DB.getSQLValueBD(po.get_TrxName(), sqlAvailable.toString(), 
										productID, locatorID);
								if (movementQty.compareTo(availableQty) > 0) {
									throw new AdempiereException("SAS-146 Cannot move " +
											MProduct.get(Env.getCtx(), productID).getValue() +
											". Qty On Hand: " + availableQty);
								}
							}
						}
					}
					catch (SQLException e)
					{
						throw new DBException(e, sql.toString());
					}
					finally
					{
						DB.close(rs, pstmt);
						rs = null; pstmt = null;
					}
					
					//DOC BEFORE PREPARE
					// SAS-146 Available Qty Validation
					sql = new StringBuilder();
					sql.append("SELECT l.AD_Org_ID, ml.M_Product_ID, SUM(ml.MovementQty) AS MovementQty ");
					sql.append("FROM M_MovementLine ml ");
					sql.append("INNER JOIN M_Product p ON (p.M_Product_ID = ml.M_Product_ID) ");
					sql.append("INNER JOIN M_Locator l ON (l.M_Locator_ID = ml.M_Locator_ID) ");
					sql.append("INNER JOIN M_Locator lto ON (lto.M_Locator_ID = ml.M_LocatorTo_ID) ");
					sql.append("INNER JOIN M_LocatorType lt ON (lt.M_LocatorType_ID = l.M_LocatorType_ID) ");
					sql.append("WHERE ml.M_Movement_ID = ? AND p.IsStocked = 'Y' ");
					sql.append("AND lt.isAvailableForReservation = 'Y' AND l.AD_Org_ID <> lto.AD_Org_ID ");
					sql.append("GROUP BY l.AD_Org_ID, ml.M_Product_ID ");
					
					BigDecimal qtyIP = Env.ZERO;
					pstmt = null;
					rs = null;
					
					try
					{
						pstmt = DB.prepareStatement(sql.toString(), po.get_TrxName());
						DB.setParameters(pstmt, new Object[]{movement.get_ID()});
						rs = pstmt.executeQuery();
						while (rs.next()) {
							int productID = rs.getInt("M_Product_ID");
							int orgID = rs.getInt("AD_Org_ID");
							BigDecimal movementQty = rs.getBigDecimal("MovementQty");
							if (movementQty.compareTo(Env.ZERO) > 0) {
								StringBuilder sqlAvailable = new StringBuilder();
								sqlAvailable.append("SELECT COALESCE(SUM(s.QtyOnHand - s.QtyReserved),0) ");
								sqlAvailable.append("FROM M_Storage s ");
								sqlAvailable.append("INNER JOIN M_Locator l ON (l.M_Locator_ID = s.M_Locator_ID) ");
								sqlAvailable.append("INNER JOIN M_LocatorType lt ON (lt.M_LocatorType_ID = l.M_LocatorType_ID) ");
								sqlAvailable.append("WHERE lt.isAvailableForReservation = 'Y' AND s.M_Product_ID = ? AND l.AD_Org_ID = ? ");
								BigDecimal availableQty = DB.getSQLValueBD(po.get_TrxName(), sqlAvailable.toString(), 
										productID, orgID);
								
								MMovementLine [] lines = movement.getLines(false);
								for(MMovementLine ml : lines) {
									int M_Warehouse_ID = ml.getM_Locator().getM_Warehouse_ID();
									String sqlMovement = "SELECT COALESCE(SUM(ml.movementqty),0) " + 
											"FROM M_MovementLine ml  " + 
											"INNER JOIN M_Movement m ON (m.M_Movement_ID=ml.M_Movement_ID) " + 
											"INNER JOIN M_Locator l ON (ml.M_Locator_ID=l.M_Locator_ID)  " + 
											"WHERE l.M_Warehouse_ID= ? AND ml.M_Product_ID= ? AND m.DocStatus = 'IP'";
									qtyIP = DB.getSQLValueBD(po.get_TrxName(), sqlMovement, M_Warehouse_ID,productID);
								}
								
								availableQty = availableQty.subtract(qtyIP);
								if (movementQty.compareTo(availableQty) > 0) {
									throw new AdempiereException("SAS-146 Cannot move " + movementQty + " of " +
											MProduct.get(Env.getCtx(), productID).getValue() +
											" in " + MOrg.get(Env.getCtx(), orgID).getName() + 
											". Available Qty: " + availableQty);
								}
							}
						}
					}
					catch (SQLException e)
					{
						throw new DBException(e, sql.toString());
					}
					finally
					{
						DB.close(rs, pstmt);
						rs = null; pstmt = null;
					}
					
					List<MMovementLine> lines = new Query(Env.getCtx(), MMovementLine.Table_Name, "M_Movement_ID = ?", po.get_TrxName())
						.setParameters(movement.get_ID())
						.list();
					// SAS-146 IM Terima cannot be greater than IM Kirim
					if (movement.get_ValueAsInt("Ref_Movement_ID") > 0) {
						for (MMovementLine line : lines) {
							if (line.get_ValueAsInt("Ref_MovementLine_ID") > 0) {
								MMovementLine refLine = new MMovementLine(Env.getCtx(), line.get_ValueAsInt("Ref_MovementLine_ID"), po.get_TrxName());
								if (line.getMovementQty().compareTo(refLine.getMovementQty()) > 0) {
									throw new AdempiereException("SAS-146.\n Line: " + line.getLine() + ", Max Qty=" + refLine.getMovementQty());
								}
							}
						}
					}
					
					// SAS-146 inconsistent locator validation
					sql = new StringBuilder();
					sql.append("SELECT COUNT(DISTINCT ml.M_Locator_ID) ");
					sql.append("FROM M_MovementLine ml ");
					sql.append("WHERE ml.M_Movement_ID = ? ");
					int count = DB.getSQLValue(po.get_TrxName(), sql.toString(), movement.get_ID());
					if (count > 1) {
						throw new AdempiereException("SAS-146.\nInconsistent locator from found in lines");
					}
					
					sql = new StringBuilder();
					sql.append("SELECT COUNT(DISTINCT ml.M_LocatorTo_ID) ");
					sql.append("FROM M_MovementLine ml ");
					sql.append("WHERE ml.M_Movement_ID = ? ");
					count = DB.getSQLValue(po.get_TrxName(), sql.toString(), movement.get_ID());
					if (count > 1) {
						throw new AdempiereException("SAS-146.\nInconsistent locator to found in lines");
					}
					
					// SAS-146 set activity if inter org movement
					if (movement.getC_DocType_ID() == SASSystemID.DOCTYPE_IM_KIRIM) {
						for (MMovementLine line : lines) {
							if (line.getM_Locator().getAD_Org_ID() != line.getM_LocatorTo().getAD_Org_ID()) {
								movement.setC_Activity_ID(SASSystemID.ACTIVITY_INTER_ORG_MOVEMENT);
								movement.saveEx();
								break;
							}
						}
					}
					
					// SAS-108 set activity if locator from hadiah
					if (movement.getC_DocType_ID() == SASSystemID.DOCTYPE_IM_KIRIM) {
						for (MMovementLine line : lines) {
							if (line.getM_Locator().getValue().toUpperCase().contains("HADIAH")) {
								movement.setC_Activity_ID(SASSystemID.ACTIVITY_WH_REWARD);
								movement.saveEx();
								break;
							}
						}
					}
				}
			} else if (po.get_TableName().equals(MInventory.Table_Name)){
				MInventory inventory = (MInventory) po;
				
				MDocType dt = MDocType.get(Env.getCtx(), inventory.getC_DocType_ID());
				String docSubTypeInv = dt.getDocSubTypeInv();
				// Internal Use Inventory validations
				if (MDocType.DOCSUBTYPEINV_InternalUseInventory.equals(docSubTypeInv)) {
					List<MInventoryLine> lines = Arrays.asList(inventory.getLines(true));
					for (MInventoryLine line : lines) {
						if (line.getC_Charge_ID() == 0)
						{
							throw new AdempiereException("SAS-160\nLine:" + line.getLine() +". @InternalUseNeedsCharge@");
						}
					}
				}
			} else if (po.get_TableName().equals(MInOut.Table_Name)){
				MInOut io = (MInOut) po;
				MDocType dt = MDocType.get(Env.getCtx(), io.getC_DocType_ID());
				
				//	SAS-36
				if (Doc.DOCTYPE_MatReceipt.equals(dt.getDocBaseType()) && !io.isSOTrx() && !io.isReversal()) {
					List<MInOutLine> lines = Arrays.asList(io.getLines(true));
					for (MInOutLine line : lines) {
						int orderLineID = 0;
						orderLineID = line.getC_OrderLine_ID();
						
						if(orderLineID>0){
							//MInOut io = new MInOut(line.getCtx(), line.getM_InOut_ID(), line.get_TrxName());
							MOrderLine oLine = new MOrderLine(Env.getCtx(), orderLineID, po.get_TrxName());
							MProduct product = new MProduct(Env.getCtx(), line.getM_Product_ID(), po.get_TrxName());
							
							BigDecimal qtyPO = oLine.getQtyOrdered();
							
							String sql = "SELECT COALESCE(SUM(iol.MovementQty),0)" +
										 " FROM M_InOutLine iol" +
										 " INNER JOIN M_InOut io ON (io.M_InOut_ID=iol.M_InOut_ID)" + 
										 " WHERE iol.C_OrderLine_ID = ?" +
										 " AND iol.M_InOutLine_ID<>?" +
										 " AND iol.IsActive='Y'" +
										 " AND io.DocStatus NOT IN ('VO','RE')" +
										 " AND iol.M_Product_ID = ?";
									
							Double totalQtyReceipt = DB.getSQLValueBD(po.get_TrxName(), sql, orderLineID, 
									line.getM_InOutLine_ID(), line.getM_Product_ID()).doubleValue();
							
							if((totalQtyReceipt+ line.getMovementQty().doubleValue()) > qtyPO.doubleValue()){
								throw new AdempiereUserError("SAS-36\nLine: " + line.getLine() + ". Total Movement Qty Exceed PO Qty - Allowed Qty="+ (qtyPO.doubleValue() - totalQtyReceipt));
							}
						}
					}
				}
				
				// SAS-218 regenerate attributes for reversed material receipt & customer return 
				// to prevent negative stock caused by original doc date material policy
				if (io.isReversal() && 
						(io.getMovementType().equals(MInOut.MOVEMENTTYPE_VendorReceipts) ||
						 io.getMovementType().equals(MInOut.MOVEMENTTYPE_CustomerReturns))){
					List<MInOutLine> lineList = Arrays.asList(io.getLines());
					Map<String,BigDecimal> storageMap = new HashMap<String, BigDecimal>();
					for (MInOutLine line : lineList) {
						BigDecimal qty = line.getMovementQty();
						if (qty.compareTo(Env.ZERO) < 0) {
							int no = MInOutLineMA.deleteInOutLineMA(line.getM_InOutLine_ID(), po.get_TrxName());
							if (no > 0)
								if (log.isLoggable(Level.CONFIG)) log.config("Delete old #" + no);
							MProduct product = line.getProduct();
							if (product != null && line.getM_AttributeSetInstance_ID() == 0) {
								String MMPolicy = product.getMMPolicy();
								Timestamp minGuaranteeDate = io.getMovementDate();
								MStorageOnHand[] storages = MStorageOnHand.getWarehouse(Env.getCtx(), io.getM_Warehouse_ID(), line.getM_Product_ID(), line.getM_AttributeSetInstance_ID(),
										minGuaranteeDate, MClient.MMPOLICY_FiFo.equals(MMPolicy), true, line.getM_Locator_ID(), po.get_TrxName(), false);
								BigDecimal qtyToReturn = qty.negate();
								for (MStorageOnHand storage: storages)
								{
									BigDecimal qtyOnHand = storage.getQtyOnHand();
									if (storageMap.containsKey(storage.getM_StorageOnHand_UU())) {
										qtyOnHand = storageMap.get(storage.getM_StorageOnHand_UU());
									}
									if (qtyOnHand.compareTo(qtyToReturn) >= 0)
									{
										MInOutLineMA ma = new MInOutLineMA (line,
												storage.getM_AttributeSetInstance_ID(),
												qtyToReturn.negate(),storage.getDateMaterialPolicy(),true);
										ma.saveEx();
										qtyOnHand = qtyOnHand.subtract(qtyToReturn);
										qtyToReturn = Env.ZERO;
									}
									else if (qtyOnHand.compareTo(Env.ZERO) > 0)
									{
										MInOutLineMA ma = new MInOutLineMA (line,
												storage.getM_AttributeSetInstance_ID(),
												qtyOnHand.negate(),storage.getDateMaterialPolicy(),true);
										ma.saveEx();
										qtyToReturn = qtyToReturn.subtract(qtyOnHand);
										qtyOnHand = Env.ZERO;
										if (log.isLoggable(Level.FINE)) log.fine( ma + ", QtyToDeliver=" + qtyToReturn);
									}

									storageMap.put(storage.getM_StorageOnHand_UU(), qtyOnHand);
									if (qtyToReturn.signum() == 0)
										break;
								}
							}
						}
					}
				}
			}
		} else if (event.getTopic().equals(IEventTopics.DOC_AFTER_PREPARE)) {
			if (po.get_TableName().equals(MOrder.Table_Name)){
				MOrder order = (MOrder) po;
				ICOrder orderCustom = POWrapper.create(order, ICOrder.class);
				if (order.isSOTrx()) {
					// Reset price limit approval from ISY
					orderCustom.setZ_WFScenarioPrice_ID(0);
					order.saveEx();
					
					// Price Limit Approval SAS-148
					BigDecimal discountMin = DB.getSQLValueBD(order.get_TrxName(), 
							"(SELECT MIN(ol.Std_Discount - ol.Discount) FROM C_OrderLine ol WHERE ol.C_Order_ID = ? AND ol.M_Product_ID > 0)", 
							order.get_ID());
					if(discountMin != null && discountMin.setScale(2,BigDecimal.ROUND_HALF_UP).compareTo(Env.ZERO.setScale(2,BigDecimal.ROUND_HALF_UP)) < 0){
						MWFScenario.setScenario(order,MWFScenario.SCENARIO_PRICE_LIMIT);
					} else {
						orderCustom.setZ_WFScenarioPrice_ID(0);
						order.saveEx();
					}
				}
			}
		} else if (event.getTopic().equals(IEventTopics.DOC_BEFORE_COMPLETE)) {
			if (po.get_TableName().equals(MOrder.Table_Name)){
				MOrder order = (MOrder) po;
				ICOrder orderCustom = POWrapper.create(order, ICOrder.class);
				MDocType docType = MDocType.get(Env.getCtx(), order.getC_DocTypeTarget_ID());
				if (order.isSOTrx() &&
						docType.getDocBaseType().equals(MDocType.DOCBASETYPE_SalesOrder) && 
						docType.getDocSubTypeSO().equals(MDocType.DOCSUBTYPESO_StandardOrder)) {
					
					// SAS-49 set priority to high if normal for a specific org
					if ((order.getPriorityRule().equals(MOrder.PRIORITYRULE_Medium) || order.getPriorityRule().equals(MOrder.PRIORITYRULE_High))
							&& (order.getAD_Org_ID() == SASSystemID.ORG_SUNTER || order.getAD_Org_ID() == SASSystemID.ORG_TEBET)) {
						Timestamp currentTime = new Timestamp(System.currentTimeMillis());
						Calendar cal = Calendar.getInstance();
						cal.setTimeInMillis(currentTime.getTime());
						cal.set(Calendar.HOUR_OF_DAY, 0);
						if (order.getAD_Org_ID() == SASSystemID.ORG_SUNTER) {
							cal.set(Calendar.HOUR_OF_DAY, 14);
						} else if (order.getAD_Org_ID() == SASSystemID.ORG_TEBET){
							cal.set(Calendar.HOUR_OF_DAY, 12);
						}
						cal.set(Calendar.MINUTE, 0);
						cal.set(Calendar.SECOND, 0);
						cal.set(Calendar.MILLISECOND, 0);
						Timestamp deadlineTime = new Timestamp(cal.getTimeInMillis());
						if (currentTime.before(deadlineTime) && order.getDatePromised().before(deadlineTime) 
								&& order.getPriorityRule().equals(MOrder.PRIORITYRULE_Medium)) {
							order.setPriorityRule(MOrder.PRIORITYRULE_High);
							order.saveEx();
						}
						if (currentTime.after(deadlineTime) 
								&& order.getPriorityRule().equals(MOrder.PRIORITYRULE_High)) {
							order.setPriorityRule(MOrder.PRIORITYRULE_Medium);
							order.saveEx();
						}
					}
				}
			} else if (po.get_TableName().equals(MMovement.Table_Name)){
				MMovement movement = (MMovement) po;
				if (movement.getReversal_ID() > 0) {
					// SAS-218 recreate attribute lines even though it's reversal
					List<MMovementLine> lines = new Query(Env.getCtx(), MMovementLine.Table_Name, 
							"M_Movement_ID = ?", po.get_TrxName())
						.setOnlyActiveRecords(true)
						.setParameters(movement.get_ID())
						.list();
					for (MMovementLine line : lines) {
						checkMaterialPolicyReversal(line);
					}
				}
			}
		} else if (event.getTopic().equals(IEventTopics.DOC_AFTER_COMPLETE)) {
//			if (po.get_TableName().equals(MOrder.Table_Name)){
//				MOrder order = (MOrder) po;
//				MDocType dt = MDocType.get(Env.getCtx(), order.getC_DocType_ID());
//				Timestamp currentDate = new Timestamp(System.currentTimeMillis());
//				// SAS-223 Create Shipment & Invoice for Org Kenari on complete
//				if (order.isSOTrx() && order.getAD_Org_ID() == SASSystemID.ORG_KENARI 
//						&& dt.getDocSubTypeSO().equals(MOrder.DocSubTypeSO_Standard)) {
//					if (!order.getDeliveryRule().equals(MOrder.DELIVERYRULE_Force)) {
//						MWarehouse wh = new MWarehouse (Env.getCtx(), order.getM_Warehouse_ID(), po.get_TrxName());
//						if (!wh.isDisallowNegativeInv()) {
//							order.setDeliveryRule(MOrder.DELIVERYRULE_Force);
//						}
//					}
//					
//					MInOut shipment = id.co.databiz.sas.model.MOrder.createShipment (dt, currentDate, order);
//					MInvoice invoice = id.co.databiz.sas.model.MOrder.createInvoice (dt, shipment, currentDate, order);
//					order.saveEx();
//				}
//			}
		} else if (event.getTopic().equals(IEventTopics.DOC_AFTER_VOID)) {
			if (po.get_TableName().equals(MOrder.Table_Name)){
				MOrder order = (MOrder) po;
				SASPromotionRule.decreasePromotionCounter(order);
			}
		} else if (event.getTopic().equals(IEventTopics.DOC_BEFORE_REVERSECORRECT) || 
				event.getTopic().equals(IEventTopics.DOC_BEFORE_REVERSEACCRUAL)) {
			if (po.get_TableName().equals(MMovement.Table_Name)){
				MMovement imk = (MMovement) po;
				if (imk.getC_DocType_ID() != SASSystemID.DOCTYPE_IM_TERIMA) {
					MMovement imt = new Query(Env.getCtx(), MMovement.Table_Name, 
							"Ref_Movement_ID = ? AND DocStatus NOT IN ('VO','RE')", po.get_TrxName())
						.setParameters(imk.get_ID())
						.first();
					if (imt != null && imt.get_ID() > 0) {
						throw new AdempiereUserError("Document already received. Plese void the document first: " + imt.getDocumentNo());
					}
				}
			} else if (po.get_TableName().equals(MInvoice.Table_Name)){
				MInvoice invoice = (MInvoice) po;
				
				// SAS-147 allocation exists validation
				StringBuilder sql = new StringBuilder();
				sql.append("SELECT COUNT(al.C_AllocationLine_ID) ");
				sql.append("FROM C_AllocationLine al ");
				sql.append("INNER JOIN C_AllocationHdr ah ON (ah.C_AllocationHdr_ID = al.C_AllocationHdr_ID)");
				sql.append("WHERE ah.DocStatus NOT IN ('VO','RE') ");
				sql.append("AND al.C_Invoice_ID = ?");
				int count = DB.getSQLValue(po.get_TrxName(), sql.toString(), invoice.get_ID());
				if (count > 0) {
					throw new AdempiereUserError("SAS-147 \n" + count 
							+ " allocation(s) exists for " + invoice.getDocumentNo());
				}
			}
		}
	}

	@Override
	protected void initialize() {
		// Model
		registerTableEvent(IEventTopics.PO_BEFORE_NEW, MDiscountSchemaBreak.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_NEW, MInvoice.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_NEW, MRequest.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_NEW, MPaymentAllocate.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_NEW, MRequestBundle.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_NEW, MTimeExpenseLine.Table_Name);
		
		registerTableEvent(IEventTopics.PO_BEFORE_CHANGE, MDiscountSchemaBreak.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_CHANGE, MRequest.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_CHANGE, MPaymentAllocate.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_CHANGE, MOrder.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_CHANGE, MTimeExpenseLine.Table_Name);

		
		registerTableEvent(IEventTopics.PO_BEFORE_DELETE, MRequest.Table_Name);
		
		registerTableEvent(IEventTopics.PO_AFTER_DELETE, MOrderLine.Table_Name);
		
		// Doc
		registerTableEvent(IEventTopics.DOC_BEFORE_PREPARE, MOrder.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_PREPARE, MInventory.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_PREPARE, MMovement.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_PREPARE, MInOut.Table_Name);
		
		registerTableEvent(IEventTopics.DOC_AFTER_PREPARE, MOrder.Table_Name);
		
		registerTableEvent(IEventTopics.DOC_BEFORE_COMPLETE, MOrder.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_COMPLETE, MMovement.Table_Name);
		
		registerTableEvent(IEventTopics.DOC_AFTER_COMPLETE, MOrder.Table_Name);
		
		registerTableEvent(IEventTopics.DOC_AFTER_VOID, MOrder.Table_Name);
		
		registerTableEvent(IEventTopics.DOC_BEFORE_REVERSECORRECT, MInvoice.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_REVERSEACCRUAL, MInvoice.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_REVERSECORRECT, MMovement.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_REVERSEACCRUAL, MMovement.Table_Name);
	}
	
	private MAcctSchema getAcctSchema() {
		if (as == null) {
			int asID = Env.getContextAsInt(Env.getCtx(), "$C_AcctSchema_ID");
			as = MAcctSchema.get(Env.getCtx(), asID);
		}
		return as;
	}
	
	private boolean isShipped(MOrder order) {
		boolean isShipped = false;
		StringBuffer sql = new StringBuffer();
		sql.append("SELECT COUNT(iol.M_InOutLine_ID) ");
		sql.append("FROM M_InOutLine iol ");
		sql.append("INNER JOIN C_OrderLine ol ON (ol.C_OrderLine_ID = iol.C_OrderLine_ID) ");
		sql.append("WHERE ol.C_Order_ID = ? ");
		int count = DB.getSQLValue(order.get_TrxName(), sql.toString(), order.get_ID());
		if (count > 0) {
			isShipped = true;
		}
		return isShipped;
	}
	
	private boolean isInvoiced(MOrder order) {
		boolean isInvoiced = false;
		StringBuffer sql = new StringBuffer();
		sql.append("SELECT COUNT(il.C_InvoiceLine_ID) ");
		sql.append("FROM C_InvoiceLine il ");
		sql.append("INNER JOIN C_OrderLine ol ON (ol.C_OrderLine_ID = il.C_OrderLine_ID) ");
		sql.append("WHERE ol.C_Order_ID = ? ");
		int count = DB.getSQLValue(order.get_TrxName(), sql.toString(), order.get_ID());
		if (count > 0) {
			isInvoiced = true;
		}
		return isInvoiced;
	}
	
	/**
	 * 	Check Material Policy
	 * 	Sets line ASI
	 */
	private void checkMaterialPolicyReversal(MMovementLine line)
	{
		MMovement movement = new MMovement(Env.getCtx(), line.getM_Movement_ID(), line.get_TrxName());
		int no = MMovementLineMA.deleteMovementLineMA(line.getM_MovementLine_ID(), movement.get_TrxName());
		if (no > 0)
			if (log.isLoggable(Level.CONFIG)) log.config("Delete old #" + no);
		
		boolean needSave = false;

		//	Attribute Set Instance
		if (line.getM_AttributeSetInstance_ID() == 0)
		{
			MProduct product = MProduct.get(movement.getCtx(), line.getM_Product_ID());
			String MMPolicy = product.getMMPolicy();
			MStorageOnHand[] storages = MStorageOnHand.getWarehouse(movement.getCtx(), 0, line.getM_Product_ID(), 0, 
					null, MClient.MMPOLICY_FiFo.equals(MMPolicy), true, line.getM_LocatorTo_ID(), movement.get_TrxName());

			BigDecimal qtyToDeliver = line.getMovementQty().negate();

			for (MStorageOnHand storage: storages)
			{
				if (storage.getQtyOnHand().compareTo(qtyToDeliver) >= 0)
				{
					MMovementLineMA ma = new MMovementLineMA (line, 
							storage.getM_AttributeSetInstance_ID(),
							qtyToDeliver.negate(),storage.getDateMaterialPolicy());
					ma.saveEx();		
					qtyToDeliver = Env.ZERO;
					if (log.isLoggable(Level.FINE)) log.fine( ma + ", QtyToDeliver=" + qtyToDeliver);		
				}
				else
				{	
					MMovementLineMA ma = new MMovementLineMA (line, 
								storage.getM_AttributeSetInstance_ID(),
								storage.getQtyOnHand().negate(),storage.getDateMaterialPolicy());
					ma.saveEx();	
					qtyToDeliver = qtyToDeliver.subtract(storage.getQtyOnHand());
					if (log.isLoggable(Level.FINE)) log.fine( ma + ", QtyToDeliver=" + qtyToDeliver);		
				}
				if (qtyToDeliver.signum() == 0)
					break;
			}
							
			//	No AttributeSetInstance found for remainder
			if (qtyToDeliver.signum() != 0)
			{
				MMovementLineMA ma = MMovementLineMA.addOrCreate(line, 0, qtyToDeliver, movement.getMovementDate()) ;
				ma.saveEx();
				if (log.isLoggable(Level.FINE)) log.fine("##: " + ma);
				
			}
		}	//	attributeSetInstance
		
		if (needSave)
		{
			line.saveEx();
		}
	}	//	checkMaterialPolicy
}