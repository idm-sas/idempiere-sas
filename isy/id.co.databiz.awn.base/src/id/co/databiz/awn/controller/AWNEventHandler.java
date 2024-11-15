package id.co.databiz.awn.controller;


import id.co.databiz.awn.model.I_C_DocTypeCustom;
import id.co.databiz.awn.model.I_C_InvoiceCustom;
import id.co.databiz.awn.model.MJournalBatch;
import id.co.databiz.awn.model.SystemID;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpSession;

import org.adempiere.base.event.AbstractEventHandler;
import org.adempiere.base.event.IEventTopics;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.exceptions.PeriodClosedException;
import org.adempiere.model.POWrapper;
import org.compiere.model.I_C_AllocationHdr;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MAllocationHdr;
import org.compiere.model.MAllocationLine;
import org.compiere.model.MBPartnerLocation;
import org.compiere.model.MBankStatement;
import org.compiere.model.MConversionRate;
import org.compiere.model.MCost;
import org.compiere.model.MCostElement;
import org.compiere.model.MDocType;
import org.compiere.model.MInOut;
import org.compiere.model.MInventory;
import org.compiere.model.MInventoryLine;
import org.compiere.model.MInvoice;
import org.compiere.model.MJournal;
import org.compiere.model.MMovement;
import org.compiere.model.MMovementLine;
import org.compiere.model.MOrder;
import org.compiere.model.MPayment;
import org.compiere.model.MPaymentAllocate;
import org.compiere.model.MPeriod;
import org.compiere.model.MPeriodControl;
import org.compiere.model.MProduct;
import org.compiere.model.MRMA;
import org.compiere.model.MRMALine;
import org.compiere.model.MRequisition;
import org.compiere.model.MSysConfig;
import org.compiere.model.MTimeExpense;
import org.compiere.model.MTimeExpenseLine;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.model.X_C_Payment;
import org.compiere.process.DocAction;
import org.compiere.util.CLogger;
import org.compiere.util.CacheMgt;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.wf.MWFScenario;
import org.eevolution.model.MPPCostCollector;
import org.osgi.service.event.Event;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;

public class AWNEventHandler extends AbstractEventHandler{
	
	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(AWNEventHandler.class);
	private MAcctSchema as = null;

	@Override
	protected void doHandleEvent(Event event) {
		if (event.getTopic().equals(IEventTopics.AFTER_LOGIN)) {
			// ISY-183 IDEMPIERE-2110
			if (Executions.getCurrent() != null && Executions.getCurrent().getDesktop()!= null) {
				Session currSess = Executions.getCurrent().getDesktop().getSession();
				HttpSession httpSess = (HttpSession) currSess.getNativeSession();
				int timeout = MSysConfig.getIntValue("ZK_SESSION_TIMEOUT_IN_SECONDS", 3600, Env.getAD_Client_ID(Env.getCtx()));
	        	httpSess.setMaxInactiveInterval(timeout);
			}
        	
//			LoginEventData eventData = getEventData(event);
//			System.out.println("AWN topic="+event.getTopic()+" AD_Client_ID="+eventData.getAD_Client_ID()
//					+" AD_Org_ID="+eventData.getAD_Org_ID()+" AD_Role_ID="+eventData.getAD_Role_ID()
//					+" AD_User_ID="+eventData.getAD_User_ID());
			try {
				log.info("Invoke Cache Reset on login");
				CacheMgt.get().reset();
			} catch (Exception e) {
				log.warning("Cache Reset Failed");
			}
		} else if (event.getTopic().equals(IEventTopics.PO_BEFORE_NEW)) {
			beforeNew(event);
		} else if (event.getTopic().equals(IEventTopics.PO_AFTER_NEW)) {
			afterNew(event);
		} else if (event.getTopic().equals(IEventTopics.PO_BEFORE_CHANGE)) {
			beforeChange(event);
		} else if (event.getTopic().equals(IEventTopics.PO_AFTER_CHANGE)) {
			afterChange(event);
		} else if (event.getTopic().equals(IEventTopics.PO_BEFORE_DELETE)) {
			beforeDelete(event);
		} else if (event.getTopic().equals(IEventTopics.PO_AFTER_DELETE)) {
			afterDelete(event);
		} else if (event.getTopic().equals(IEventTopics.DOC_BEFORE_PREPARE)) {
			beforePrepare(event);
		} else if (event.getTopic().equals(IEventTopics.DOC_AFTER_PREPARE)) {
			afterPrepare(event);
		} else if (event.getTopic().equals(IEventTopics.DOC_BEFORE_COMPLETE)) {
			beforeComplete(event);
		} else if (event.getTopic().equals(IEventTopics.DOC_AFTER_COMPLETE)) {
			afterComplete(event);
		} else if (event.getTopic().equals(IEventTopics.DOC_BEFORE_VOID)) {
			beforeVoid(event);
		} else if (event.getTopic().equals(IEventTopics.DOC_AFTER_VOID)) {
			afterVoid(event);
		} else if (event.getTopic().equals(IEventTopics.DOC_BEFORE_CLOSE)) {
			beforeClose(event);
		} else if (event.getTopic().equals(IEventTopics.DOC_AFTER_CLOSE)) {
			afterClose(event);
		} else if (event.getTopic().equals(IEventTopics.DOC_BEFORE_REACTIVATE)) {
			beforeReactivate(event);
		} else if (event.getTopic().equals(IEventTopics.DOC_AFTER_REACTIVATE)) {
			afterReactivate(event);
		} else if (event.getTopic().equals(IEventTopics.DOC_BEFORE_REVERSECORRECT)) {
			beforeReverseCorrect(event);
		} else if (event.getTopic().equals(IEventTopics.DOC_AFTER_REVERSECORRECT)) {
			afterReverseCorrect(event);
		} else if (event.getTopic().equals(IEventTopics.DOC_BEFORE_REVERSEACCRUAL)) {
			beforeReverseAccrual(event);
		} else if (event.getTopic().equals(IEventTopics.DOC_AFTER_REVERSEACCRUAL)) {
			afterReverseAccrual(event);
		} else if (event.getTopic().equals(IEventTopics.DOC_BEFORE_POST)) {
			beforePost(event);
		} else if (event.getTopic().equals(IEventTopics.DOC_AFTER_POST)) {
			afterPost(event);
		}
	}

	@Override
	protected void initialize() {
		registerEvent(IEventTopics.AFTER_LOGIN);
		
		// Model
		registerTableEvent(IEventTopics.PO_BEFORE_NEW, MBPartnerLocation.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_CHANGE, MBPartnerLocation.Table_Name);
		
		registerTableEvent(IEventTopics.PO_BEFORE_CHANGE, MPayment.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_CHANGE, MPaymentAllocate.Table_Name);
		
		registerTableEvent(IEventTopics.PO_BEFORE_NEW, MConversionRate.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_CHANGE, MConversionRate.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_CHANGE, MOrder.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_CHANGE, MInvoice.Table_Name);
		
		registerTableEvent(IEventTopics.PO_AFTER_NEW, MProduct.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_NEW, MOrder.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_NEW, MInvoice.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_NEW, MPaymentAllocate.Table_Name);
		
		registerTableEvent(IEventTopics.PO_BEFORE_DELETE, MTimeExpenseLine.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_DELETE, MRMALine.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_DELETE, MMovementLine.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_DELETE, MInventoryLine.Table_Name);
		
		// Doc
		registerTableEvent(IEventTopics.DOC_BEFORE_PREPARE, MJournal.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_PREPARE, MInOut.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_PREPARE, MInventory.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_PREPARE, MInvoice.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_PREPARE, MMovement.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_PREPARE, MOrder.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_PREPARE, MPayment.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_PREPARE, MRMA.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_PREPARE, MTimeExpense.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_PREPARE, MRequisition.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_PREPARE, MPPCostCollector.Table_Name);
		
		registerTableEvent(IEventTopics.DOC_AFTER_PREPARE, MJournal.Table_Name);
		registerTableEvent(IEventTopics.DOC_AFTER_PREPARE, MInOut.Table_Name);
		registerTableEvent(IEventTopics.DOC_AFTER_PREPARE, MInventory.Table_Name);
		registerTableEvent(IEventTopics.DOC_AFTER_PREPARE, MInvoice.Table_Name);
		registerTableEvent(IEventTopics.DOC_AFTER_PREPARE, MMovement.Table_Name);
		registerTableEvent(IEventTopics.DOC_AFTER_PREPARE, MOrder.Table_Name);
		registerTableEvent(IEventTopics.DOC_AFTER_PREPARE, MPayment.Table_Name);
		registerTableEvent(IEventTopics.DOC_AFTER_PREPARE, MRMA.Table_Name);
		registerTableEvent(IEventTopics.DOC_AFTER_PREPARE, MTimeExpense.Table_Name);
		registerTableEvent(IEventTopics.DOC_AFTER_PREPARE, MRequisition.Table_Name);
		
		registerTableEvent(IEventTopics.DOC_BEFORE_COMPLETE, MAllocationHdr.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_COMPLETE, MInvoice.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_COMPLETE, MInventory.Table_Name);
		registerTableEvent(IEventTopics.DOC_AFTER_COMPLETE, MPayment.Table_Name);
		registerTableEvent(IEventTopics.DOC_AFTER_COMPLETE, MTimeExpense.Table_Name);
		
		registerTableEvent(IEventTopics.DOC_AFTER_CLOSE, MTimeExpense.Table_Name);
		
		registerTableEvent(IEventTopics.DOC_BEFORE_VOID, MAllocationHdr.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_VOID, MInOut.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_VOID, MInventory.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_VOID, MInvoice.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_VOID, MMovement.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_VOID, MPayment.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_VOID, MBankStatement.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_VOID, MJournal.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_VOID, MJournalBatch.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_VOID, MOrder.Table_Name);
		
		registerTableEvent(IEventTopics.DOC_AFTER_VOID, MTimeExpense.Table_Name);
				
		registerTableEvent(IEventTopics.DOC_BEFORE_REVERSECORRECT, MInOut.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_REVERSEACCRUAL, MInOut.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_REVERSECORRECT, MInvoice.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_REVERSEACCRUAL, MInvoice.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_REVERSECORRECT, MPayment.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_REVERSEACCRUAL, MPayment.Table_Name);
		
		registerTableEvent(IEventTopics.DOC_AFTER_REVERSEACCRUAL, MTimeExpense.Table_Name);
		registerTableEvent(IEventTopics.DOC_AFTER_REVERSECORRECT, MTimeExpense.Table_Name);
		registerTableEvent(IEventTopics.DOC_AFTER_REVERSEACCRUAL, MInOut.Table_Name);
		registerTableEvent(IEventTopics.DOC_AFTER_REVERSECORRECT, MInOut.Table_Name);
		
	}
	
	private void beforeNew(Event event){
		PO po = getPO(event);
		if (po.get_TableName().equals(MBPartnerLocation.Table_Name)){
			BPartnerLocationController.beforeNew((MBPartnerLocation)po);
		} else if (po.get_TableName().equals(MConversionRate.Table_Name)){
			ConversionRateController.beforeNew((MConversionRate)po);
		}		
	}
	
	private void afterNew(Event event){
		PO po = getPO(event);
		if (po.get_TableName().equals(MPaymentAllocate.Table_Name)){
			PaymentAllocateController.afterNew((MPaymentAllocate)po);
		} else if (po.get_TableName().equals(MProduct.Table_Name)){
			ProductController.afterNew((MProduct)po);
		}else if (po.get_TableName().equals(MOrder.Table_Name)){
			OrderController.beforeChangeAndNew((MOrder)po);
		}else if (po.get_TableName().equals(MInvoice.Table_Name)){
			InvoiceController.beforeChangeAndNew((MInvoice)po);
		}
	}
	
	private void beforeChange(Event event){
		PO po = getPO(event);
		if (po.get_TableName().equals(MPaymentAllocate.Table_Name)){
			PaymentAllocateController.beforeChange((MPaymentAllocate)po);
		} else if (po.get_TableName().equals(MBPartnerLocation.Table_Name)){
			BPartnerLocationController.beforeChange((MBPartnerLocation)po);
		} else if (po.get_TableName().equals(MConversionRate.Table_Name)){
			ConversionRateController.beforeChange((MConversionRate)po);
		} else if(po.get_TableName().equals(MOrder.Table_Name)){
			OrderController.beforeChangeAndNew((MOrder)po);
		} else if(po.get_TableName().equals(MInvoice.Table_Name)){
			InvoiceController.beforeChangeAndNew((MInvoice)po);
		} else if(po.get_TableName().equals(MPayment.Table_Name)){
			MPayment payment = (MPayment) po;
			if (payment.is_ValueChanged("IsInTransit") || payment.is_ValueChanged("IsCashed")) {
				if (payment.get_ValueAsInt("C_PaymentCash_ID") > 0 &&
						!payment.get_ValueAsBoolean("IsInTransit") &&
						!payment.get_ValueAsBoolean("IsCashed")) {
					payment.set_ValueOfColumn("C_PaymentCash_ID", null);
					payment.saveEx();
					
					List<MPaymentAllocate> paList = new Query(Env.getCtx(), MPaymentAllocate.Table_Name, "C_PaymentGiro_ID = ?", po.get_TrxName())
						.setParameters(payment.get_ID())
						.list();
					for (MPaymentAllocate pa : paList) {
						pa.set_ValueOfColumn("C_PaymentGiro_ID", null);
						pa.set_ValueOfColumn("CheckNo", null);
						pa.saveEx();
					}
				}
			}
		}
	}
	
	private void afterChange(Event event){

	}
	
	private void beforeDelete(Event event){
		PO po = getPO(event);
		if (po.get_TableName().equals(MTimeExpenseLine.Table_Name)){
			MTimeExpenseLine line = (MTimeExpenseLine) po;
			if (line.getS_TimeExpense_ID() > 0) {
				if (!line.getS_TimeExpense().getDocStatus().equals(DocAction.STATUS_Drafted)) {
					throw new AdempiereException("ISY-233 Document status is not Drafted.");
				}
			}
		} else if (po.get_TableName().equals(MRMALine.Table_Name)){
			MRMALine line = (MRMALine) po;
			if (line.getM_RMA_ID() > 0) {
				if (!line.getM_RMA().getDocStatus().equals(DocAction.STATUS_Drafted)) {
					throw new AdempiereException("ISY-233 Document status is not Drafted.");
				}
			}
		} else if (po.get_TableName().equals(MMovementLine.Table_Name)){
			MMovementLine line = (MMovementLine) po;
			if (line.getM_Movement_ID() > 0) {
				if (!line.getM_Movement().getDocStatus().equals(DocAction.STATUS_Drafted)) {
					throw new AdempiereException("ISY-233 Document status is not Drafted.");
				}
			}
		} else if (po.get_TableName().equals(MInventoryLine.Table_Name)){
			MInventoryLine line = (MInventoryLine) po;
			if (line.getM_Inventory_ID() > 0) {
				if (!line.getM_Inventory().getDocStatus().equals(DocAction.STATUS_Drafted)) {
					throw new AdempiereException("ISY-233 Document status is not Drafted.");
				}
			}
		}
	}
	
	private void afterDelete(Event event){
		
	}
	
	private void beforePrepare(Event event){
		PO po = getPO(event);
//		MWFScenario.setScenario(po, null);
		if (po.get_TableName().equals(MOrder.Table_Name)){
			OrderController.beforePrepare((MOrder)po);
		}
		else if (po.get_TableName().equals(MInventory.Table_Name)){
			MInventory invent = (MInventory) po;
			List<MInventoryLine> lines = Arrays.asList(invent.getLines(true));
			
			BigDecimal approvalAmt = Env.ZERO;
			BigDecimal currentCostPrice = Env.ZERO;
			MProduct product = null;
			
			//	ISY-47
			for (MInventoryLine line : lines) {
				product = MProduct.get(Env.getCtx(), line.getM_Product_ID());
				
				currentCostPrice = MCost.getCurrentCost(product, line.getM_AttributeSetInstance_ID(), getAcctSchema(), line.getAD_Org_ID(), 
						MAcctSchema.COSTINGMETHOD_StandardCosting, Env.ONE, 0, 
						true, po.get_TrxName());
				
				approvalAmt = approvalAmt.add(
						(((line.getQtyBook().subtract(line.getQtyCount())).abs())
								.add(line.getQtyInternalUse().abs()))
								.multiply(currentCostPrice));
			}
			invent.setApprovalAmt(approvalAmt);
			invent.saveEx();
		}
		else if (po.get_TableName().equals(MMovement.Table_Name)){
			// ISY-416
			MMovement movement = (MMovement) po;
			MovementController.beforePrepare(movement);
		}
//		if (po.get_TableName().equals(MInvoice.Table_Name)){
//			MInvoice invoice = (MInvoice) po;
//			
//			//	ISY-56
//			if(invoice.get_ValueAsBoolean("CreateTaxInvoice") && !invoice.isReversal()){
//				StringBuffer sqlTax = new StringBuffer();
//				sqlTax.append("SELECT COUNT(*) ");
//				sqlTax.append("FROM C_InvoiceTax it ");
//				sqlTax.append("INNER JOIN C_Tax t ON (t.C_Tax_ID = it.C_Tax_ID) ");
//				sqlTax.append("INNER JOIN C_TaxCategory tc ON (tc.C_TaxCategory_ID = t.C_TaxCategory_ID) ");
//				sqlTax.append("WHERE it.C_Invoice_ID = ? AND t.C_TaxCategory_ID NOT IN (?) AND UPPER(tc.Name) NOT LIKE 'PPN%'");
//				int count = DB.getSQLValue(po.get_TrxName(), sqlTax.toString(), SystemID.TAX_CATEGORY_TAX_EXEMPT);
//				if(count > 0) {
//					throw new AdempiereException("ISY-56 Invalid Tax Category for Tax Invoice");
//				}	
//			}
//		}
		else if (po.get_TableName().equals(MPPCostCollector.Table_Name)) {
			 MPPCostCollector cc = (MPPCostCollector) po;
				if(cc.getCostCollectorType()!=null){
					cc.setC_DocType_ID(MPPCostCollector.getDocType(cc.getCostCollectorType()));
					cc.setC_DocTypeTarget_ID(MPPCostCollector.getDocType(cc.getCostCollectorType()));
					cc.saveEx(po.get_TrxName());
				}
		 }
	}
	
	private void afterPrepare(Event event){
		PO po = getPO(event);
		MWFScenario.setScenario(po, null);
	}
	
	private void beforeComplete(Event event){
		PO po = getPO(event);
		if (po.get_TableName().equals(MInvoice.Table_Name)){
			InvoiceController.beforeComplete((MInvoice)po);
		}
		else if (po.get_TableName().equals(MAllocationHdr.Table_Name)){
			// M_AllocationHdr.BeforeComplete
			MAllocationHdr alloc = (MAllocationHdr) po;
			I_C_AllocationHdr allocCustom = POWrapper.create(alloc, I_C_AllocationHdr.class);
			List<MAllocationLine> lines = Arrays.asList(alloc.getLines(true));
			Set<String> doc = new HashSet<String>();
			
			for (MAllocationLine line : lines) {
				//  GET PaymentDocNo
				if(line.getC_Payment_ID() > 0)
					doc.add(line.getC_Payment().getDocumentNo());
				//	GET InvoiceDocNo
				if(line.getC_Invoice_ID() > 0)
					doc.add(line.getC_Invoice().getDocumentNo());
			}
			//	ISY-130 Description Allocation
			allocCustom.setDescription(doc.toString());
			alloc.saveEx();
		} else if (po.get_TableName().equals(MInventory.Table_Name)){
			MInventory inventory = (MInventory) po;
			
			MDocType dt = MDocType.get(Env.getCtx(), inventory.getC_DocType_ID());
			String parentDocSubTypeInv = dt.getDocSubTypeInv();
			String docCostingMethod = inventory.getCostingMethod();
			if (MDocType.DOCSUBTYPEINV_CostAdjustment.equals(parentDocSubTypeInv)) {
				List<MInventoryLine> lines = Arrays.asList(inventory.getLines(true));
				for (MInventoryLine line : lines) {
					MProduct product = MProduct.get(Env.getCtx(), line.getM_Product_ID());
					String costingLevel = product.getCostingLevel(getAcctSchema());
					int orgId = line.getAD_Org_ID();
					int asiId = line.getM_AttributeSetInstance_ID();
					if (MAcctSchema.COSTINGLEVEL_Client.equals(costingLevel))
					{
						orgId = 0;
						asiId = 0;
					}
					else if (MAcctSchema.COSTINGLEVEL_Organization.equals(costingLevel))
						asiId = 0;
					else if (MAcctSchema.COSTINGLEVEL_BatchLot.equals(costingLevel))
						orgId = 0;
					MCostElement ce = MCostElement.getMaterialCostElement(Env.getCtx(), docCostingMethod, orgId);
					MCost cost = MCost.get(product, asiId, getAcctSchema(), 
							orgId, ce.getM_CostElement_ID(), po.get_TrxName());					
					DB.getDatabase().forUpdate(cost, 120);
					BigDecimal currentQty = cost.getCurrentQty();
					line.set_ValueOfColumn("QtyCostAdjustment", currentQty);
					line.saveEx();
				}
			}
		}
	}
	
	private void afterComplete(Event event){
		PO po = getPO(event);
		if (po.get_TableName().equals(MPayment.Table_Name)){
			PaymentController.afterComplete((X_C_Payment)po);
			MPayment payment = (MPayment) po;
			List<MPayment> giroList = new Query(Env.getCtx(), MPayment.Table_Name, "C_PaymentCash_ID = ?", po.get_TrxName())
				.setParameters(payment.get_ID())
				.list();
			for (MPayment giro : giroList) {
				giro.set_ValueOfColumn("IsCashed", true);
				giro.saveEx();
			}
		}else if (po.get_TableName().equals(MTimeExpense.Table_Name)){
			TimeExpenseController.setProcessed((MTimeExpense)po);
		}
	}
	
	private void beforeVoid(Event event){
		PO po = getPO(event);
		if (po.get_TableName().equals(MInOut.Table_Name)){
			MInOut io = (MInOut) po;
			
			// ISY-154
			StringBuilder sb = new StringBuilder();
			sb.append("SELECT COUNT(il.M_InOutLine_ID) FROM C_InvoiceLine il ");
			sb.append("INNER JOIN C_Invoice i ON (i.C_Invoice_ID = il.C_Invoice_ID) ");
			sb.append("INNER JOIN M_InOutLine iol ON (iol.M_InOutLine_ID = il.M_InOutLine_ID) ");
			sb.append("INNER JOIN M_InOut io ON (io.M_InOut_ID = iol.M_InOut_ID) ");
			sb.append("WHERE i.DocStatus IN ('CO') AND i.Processed='Y' AND io.M_InOut_ID = ? ");
			int count = DB.getSQLValue(po.get_TrxName(), sb.toString(), io.get_ID());
			if (count > 0){
				throw new AdempiereException ("ISY-154\n You have to void the invoice first");
			}
		}
		else if (po.get_TableName().equals(MInvoice.Table_Name)){
			MInvoice invoice = (MInvoice) po;	
			I_C_InvoiceCustom invoiceCustom = POWrapper.create(invoice, I_C_InvoiceCustom.class);
			MDocType docType = new MDocType(Env.getCtx(), invoice.getC_DocType_ID(), null);
			I_C_DocTypeCustom docTypeCustom = POWrapper.create(docType, I_C_DocTypeCustom.class);
			
			// SYNC-314
			if(docTypeCustom.getC_DocTypeKwitansi_ID()>0 && invoiceCustom.getZ_Kwitansi_ID()>0){
				throw new AdempiereException("ISY-225 Invoice cannot be voided because it has reference to Kwitansi");
			}
			
			//	ISY-274
			if (invoice.getDocStatus().equals("DR")
				|| invoice.getDocStatus().equals("IN")
				|| invoice.getDocStatus().equals("IP")
				|| invoice.getDocStatus().equals("AP")
				|| invoice.getDocStatus().equals("NA") )
			{
				invoice.setGrandTotal(Env.ZERO);
				//	Delete Taxes
				StringBuilder msgdb = new StringBuilder("DELETE FROM C_InvoiceTax WHERE C_Invoice_ID=").append(invoice.getC_Invoice_ID());
				DB.executeUpdateEx(msgdb.toString(), po.get_TrxName());
			}
		} else if (po.get_TableName().equals(MPayment.Table_Name)){
			MPayment payment = (MPayment) po;
			List<MPayment> giroList = new Query(Env.getCtx(), MPayment.Table_Name, "C_PaymentCash_ID = ?", po.get_TrxName())
				.setParameters(payment.get_ID())
				.list();
			for (MPayment giro : giroList) {
				giro.set_ValueOfColumn("C_PaymentCash_ID", null);
				giro.set_ValueOfColumn("IsInTransit", false);
				giro.set_ValueOfColumn("IsCashed", false);
				giro.saveEx();
			}
			
			List<MPaymentAllocate> paList = Arrays.asList(MPaymentAllocate.get(payment));
			for (MPaymentAllocate pa : paList) {
				pa.set_ValueOfColumn("C_PaymentGiro_ID", null);
				pa.set_ValueOfColumn("CheckNo", null);
				pa.saveEx();
			}
		} else if (po.get_TableName().equals(MOrder.Table_Name)){
			OrderController.beforeVoid((MOrder)po);
		}

	}
	
	private void afterVoid(Event event){
		PO po = getPO(event);
		if (po.get_TableName().equals(MTimeExpense.Table_Name)){
			TimeExpenseController.setProcessed((MTimeExpense)po);
		}
	}
	
	private void beforeClose(Event event){
		
	}
	
	private void afterClose(Event event){
		PO po = getPO(event);
		if (po.get_TableName().equals(MTimeExpense.Table_Name)){
			TimeExpenseController.setProcessed((MTimeExpense)po);
		}
	}
	
	private void beforeReactivate(Event event){
		
	}
	
	private void afterReactivate(Event event){
		
	}
	
	private void beforeReverseCorrect(Event event){
		PO po = getPO(event);
		if (po.get_TableName().equals(MInOut.Table_Name)){
			InOutController.beforeReverseCorrect((MInOut)po);
		} else if (po.get_TableName().equals(MInvoice.Table_Name)){
			InvoiceController.beforeReverse((MInvoice)po);
		} else if (po.get_TableName().equals(MPayment.Table_Name)){
			MPayment payment = (MPayment) po;
			PaymentController.reverse(payment);
		}
	}
	
	private void afterReverseCorrect(Event event){
		PO po = getPO(event);
		if (po.get_TableName().equals(MTimeExpense.Table_Name)){
			TimeExpenseController.setProcessed((MTimeExpense)po);
		} else if (po.get_TableName().equals(MInOut.Table_Name)){
			InOutController.afterReverseCorrect((MInOut)po);
		}
	}
	
	private void beforeReverseAccrual(Event event){
		PO po = getPO(event);	
		if (po.get_TableName().equals(MInOut.Table_Name)){
			InOutController.beforeReverseAccrual((MInOut)po);
		} else if (po.get_TableName().equals(MAllocationHdr.Table_Name)){
			MAllocationHdr alloc = (MAllocationHdr) po;
			
			//	ISY-136 Check Period
			try{
				MPeriod.testPeriodOpen(Env.getCtx(), alloc.getDateTrx(), MPeriodControl.DOCBASETYPE_PaymentAllocation, alloc.getAD_Org_ID());
			}catch (PeriodClosedException e){
				throw new AdempiereException("ISY-136 Cannot Reverse Acruall with Void DocAction , Is Period Closed?");
			}
		}else if (po.get_TableName().equals(MInventory.Table_Name)){
			MInventory inv = (MInventory) po;
			
			//	ISY-136 Check Period
			try{
				MPeriod.testPeriodOpen(Env.getCtx(), inv.getMovementDate(), inv.getC_DocType_ID(), inv.getAD_Org_ID());
			}catch (PeriodClosedException e){
				throw new AdempiereException("ISY-136 Cannot Reverse Acruall with Void DocAction , Is Period Closed?");
			}
		}else if (po.get_TableName().equals(MInvoice.Table_Name)){
			MInvoice inv = (MInvoice) po;
			
			//	ISY-136 Check Period
			try{
				MPeriod.testPeriodOpen(Env.getCtx(), inv.getDateAcct(), inv.getC_DocType_ID(), inv.getAD_Org_ID());
			}catch (PeriodClosedException e){
				throw new AdempiereException("ISY-136 Cannot Reverse Acruall with Void DocAction , Is Period Closed?");
			}
		}else if (po.get_TableName().equals(MMovement.Table_Name)){
			MMovement mov = (MMovement) po;
			
			//	ISY-136 Check Period
			try{
				MPeriod.testPeriodOpen(Env.getCtx(), mov.getMovementDate(), mov.getC_DocType_ID(), mov.getAD_Org_ID());
			}catch (PeriodClosedException e){
				throw new AdempiereException("ISY-136 Cannot Reverse Acruall with Void DocAction , Is Period Closed?");
			}
		}else if (po.get_TableName().equals(MPayment.Table_Name)){
			MPayment payment = (MPayment) po;

			//	ISY-136 Check Period
			try{
				MPeriod.testPeriodOpen(Env.getCtx(), payment.getDateAcct(), payment.getC_DocType_ID(), payment.getAD_Org_ID());
			}catch (PeriodClosedException e){
				throw new AdempiereException("ISY-136 Cannot Reverse Acruall with Void DocAction , Is Period Closed?");
			}
			
			PaymentController.reverse(payment);
			
		}else if (po.get_TableName().equals(MBankStatement.Table_Name)){
			MBankStatement bs = (MBankStatement) po;
			
			//	ISY-136 Check Period
			try{
				MPeriod.testPeriodOpen(Env.getCtx(), bs.getStatementDate(), MDocType.DOCBASETYPE_BankStatement, bs.getAD_Org_ID());
			}catch (PeriodClosedException e){
				throw new AdempiereException("ISY-136 Cannot Reverse Acruall with Void DocAction , Is Period Closed?");
			}
		}else if (po.get_TableName().equals(MJournal.Table_Name)){
			MJournal ju = (MJournal) po;
			
			//	ISY-136 Check Period
			try{
				MPeriod.testPeriodOpen(Env.getCtx(), ju.getDateAcct(), ju.getC_DocType_ID(), ju.getAD_Org_ID());
			}catch (PeriodClosedException e){
				throw new AdempiereException("ISY-136 Cannot Reverse Acruall with Void DocAction , Is Period Closed?");
			}
		}else if (po.get_TableName().equals(MJournalBatch.Table_Name)){
			MJournalBatch jb = (MJournalBatch) po;
			
			//	ISY-136 Check Period
			try{
				MPeriod.testPeriodOpen(Env.getCtx(), jb.getDateAcct(), jb.getC_DocType_ID(), jb.getAD_Org_ID());
			}catch (PeriodClosedException e){
				throw new AdempiereException("ISY-136 Cannot Reverse Acruall with Void DocAction , Is Period Closed?");
			}
		}else{
			InvoiceController.beforeReverse((MInvoice)po);
		}
	}
	
	private void afterReverseAccrual(Event event){
		PO po = getPO(event);
		if (po.get_TableName().equals(MTimeExpense.Table_Name)){
			TimeExpenseController.setProcessed((MTimeExpense)po);
		} else if (po.get_TableName().equals(MInOut.Table_Name)){
			InOutController.afterReverseAccrual((MInOut)po);
		}
	}
	
	private void beforePost(Event event){
		
	}
	
	private void afterPost(Event event){
		
	}
	
	public MAcctSchema getAcctSchema() {
		if (as == null) {
			int asID = Env.getContextAsInt(Env.getCtx(), "$C_AcctSchema_ID");
			as = MAcctSchema.get(Env.getCtx(), asID);
		}
		return as;
	}

}
