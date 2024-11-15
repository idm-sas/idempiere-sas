package id.co.databiz.awn.factory;

import id.co.databiz.awn.model.MFreight;
import id.co.databiz.awn.model.MInOut;
import id.co.databiz.awn.model.MInventory;
import id.co.databiz.awn.model.MInvoice;
import id.co.databiz.awn.model.MJournal;
import id.co.databiz.awn.model.MJournalBatch;
import id.co.databiz.awn.model.MKwitansi;
import id.co.databiz.awn.model.MMovement;
import id.co.databiz.awn.model.MOrder;
//import id.co.databiz.awn.model.MOrderLine;
import id.co.databiz.awn.model.MOrderRef;
import id.co.databiz.awn.model.MPayment;
import id.co.databiz.awn.model.MPaymentAllocate;
import id.co.databiz.awn.model.MProduction;
import id.co.databiz.awn.model.MReportCube;
import id.co.databiz.awn.model.MRequisition;
import id.co.databiz.awn.model.MRequisitionLine;
import id.co.databiz.awn.model.MTaxInvoice;
import id.co.databiz.awn.model.MVarianceDistribution;

import java.sql.ResultSet;

import org.adempiere.base.IModelFactory;
import org.compiere.model.PO;
import org.compiere.util.Env;

public class ModelFactory implements IModelFactory {

	@Override
	public Class<?> getClass(String tableName) {
		Class<?> clazz = null;
		if(tableName.equals(MPaymentAllocate.Table_Name)){
			clazz = MPaymentAllocate.class;
		} else if(tableName.equals(MPayment.Table_Name)){
			clazz = MPayment.class;
		} else if(tableName.equals(MVarianceDistribution.Table_Name)){
			clazz = MVarianceDistribution.class;
		} 
//		else if(tableName.equals(MAsset.Table_Name)){
//			clazz = MAsset.class;
//		} 
		else if(tableName.equals(MInOut.Table_Name)){
			clazz = MInOut.class;
		} else if(tableName.equals(MOrder.Table_Name)){
			clazz = MOrder.class;
		} else if(tableName.equals(MTaxInvoice.Table_Name)){
			clazz = MTaxInvoice.class;
		} else if(tableName.equals(MOrderRef.Table_Name)){
			clazz = MOrderRef.class;
		} else if(tableName.equals(MRequisition.Table_Name)){
			clazz = MRequisition.class;
		} else if(tableName.equals(MJournal.Table_Name)){
			clazz = MJournal.class;
		} else if(tableName.equals(MJournalBatch.Table_Name)){
			clazz = MJournalBatch.class;
		} else if(tableName.equals(MFreight.Table_Name)){
			clazz = MFreight.class;
		} else if(tableName.equals(MRequisitionLine.Table_Name)){
			clazz = MRequisitionLine.class;
		} else if(tableName.equals(MProduction.Table_Name)){
			clazz = MProduction.class;
		} else if(tableName.equals(MReportCube.Table_Name)){
			clazz = MReportCube.class;
		} else if(tableName.equals(MKwitansi.Table_Name)){
			clazz = MKwitansi.class;
		} else if(tableName.equals(MInvoice.Table_Name)){
			clazz = MInvoice.class;
		} else if(tableName.equals(MInventory.Table_Name)){
			clazz = MInventory.class;
		} else if(tableName.equals(MMovement.Table_Name)){
			clazz = MMovement.class;
//		} else if(tableName.equals(MOrderLine.Table_Name)){
//			clazz = MOrderLine.class;
		}
		return clazz;
	}

	@Override
	public PO getPO(String tableName, int Record_ID, String trxName) {
		PO po = null;
		if(tableName.equals(MPaymentAllocate.Table_Name)){
			po = new MPaymentAllocate(Env.getCtx(), Record_ID, trxName);
		} else if(tableName.equals(MPayment.Table_Name)){
			po = new MPayment(Env.getCtx(), Record_ID, trxName);
		} else if(tableName.equals(MVarianceDistribution.Table_Name)){
			po = new MVarianceDistribution(Env.getCtx(), Record_ID, trxName);
		} 
//		else if(tableName.equals(MAsset.Table_Name)){
//			po = new MAsset(Env.getCtx(), Record_ID, trxName);
//		} 
		else if(tableName.equals(MInOut.Table_Name)){
			po = new MInOut(Env.getCtx(), Record_ID, trxName);
		} else if(tableName.equals(MOrder.Table_Name)){
			po = new MOrder(Env.getCtx(), Record_ID, trxName);
		} else if(tableName.equals(MTaxInvoice.Table_Name)){
			po = new MTaxInvoice(Env.getCtx(), Record_ID, trxName);
		} else if(tableName.equals(MOrderRef.Table_Name)){
			po = new MOrderRef(Env.getCtx(), Record_ID, trxName);
		} else if(tableName.equals(MRequisition.Table_Name)){
			po = new MRequisition(Env.getCtx(), Record_ID, trxName);
		} else if(tableName.equals(MJournal.Table_Name)){
			po = new MJournal(Env.getCtx(), Record_ID, trxName);
		} else if(tableName.equals(MJournalBatch.Table_Name)){
			po = new MJournalBatch(Env.getCtx(), Record_ID, trxName);
		} else if(tableName.equals(MFreight.Table_Name)){
			po = new MFreight(Env.getCtx(), Record_ID, trxName);
		} else if(tableName.equals(MRequisitionLine.Table_Name)){
			po = new MRequisitionLine(Env.getCtx(), Record_ID, trxName);
		} else if(tableName.equals(MProduction.Table_Name)){
			po = new MProduction(Env.getCtx(), Record_ID, trxName);
		} else if(tableName.equals(MReportCube.Table_Name)){
			po = new MReportCube(Env.getCtx(), Record_ID, trxName);
		} else if(tableName.equals(MKwitansi.Table_Name)){
			po = new MKwitansi(Env.getCtx(), Record_ID, trxName);
		} else if(tableName.equals(MInvoice.Table_Name)){
			po = new MInvoice(Env.getCtx(), Record_ID, trxName);
		} else if(tableName.equals(MInventory.Table_Name)){
			po = new MInventory(Env.getCtx(), Record_ID, trxName);
		} else if(tableName.equals(MMovement.Table_Name)){
			po = new MMovement(Env.getCtx(), Record_ID, trxName);
//		} else if(tableName.equals(MOrderLine.Table_Name)){
//			po = new MOrderLine(Env.getCtx(), Record_ID, trxName);
		}
		return po;
	}

	@Override
	public PO getPO(String tableName, ResultSet rs, String trxName) {
		PO po = null;
		if(tableName.equals(MPaymentAllocate.Table_Name)){
			po = new MPaymentAllocate(Env.getCtx(), rs, trxName);
		} else if(tableName.equals(MPayment.Table_Name)){
			po = new MPayment(Env.getCtx(), rs, trxName);
		} else if(tableName.equals(MVarianceDistribution.Table_Name)){
			po = new MVarianceDistribution(Env.getCtx(), rs, trxName);
		} 
//		else if(tableName.equals(MAsset.Table_Name)){
//			po = new MAsset(Env.getCtx(), rs, trxName);
//		} 
		else if(tableName.equals(MInOut.Table_Name)){
			po = new MInOut(Env.getCtx(), rs, trxName);
		} else if(tableName.equals(MOrder.Table_Name)){
			po = new MOrder(Env.getCtx(), rs, trxName);
		} else if(tableName.equals(MTaxInvoice.Table_Name)){
			po = new MTaxInvoice(Env.getCtx(), rs, trxName);
		} else if(tableName.equals(MOrderRef.Table_Name)){
			po = new MOrderRef(Env.getCtx(), rs, trxName);
		} else if(tableName.equals(MRequisition.Table_Name)){
			po = new MRequisition(Env.getCtx(), rs, trxName);
		} else if(tableName.equals(MJournal.Table_Name)){
			po = new MJournal(Env.getCtx(), rs, trxName);
		} else if(tableName.equals(MJournalBatch.Table_Name)){
			po = new MJournalBatch(Env.getCtx(), rs, trxName);
		} else if(tableName.equals(MFreight.Table_Name)){
			po = new MFreight(Env.getCtx(), rs, trxName);
		} else if(tableName.equals(MRequisitionLine.Table_Name)){
			po = new MRequisitionLine(Env.getCtx(), rs, trxName);
		} else if(tableName.equals(MProduction.Table_Name)){
			po = new MProduction(Env.getCtx(), rs, trxName);
		} else if(tableName.equals(MReportCube.Table_Name)){
			po = new MReportCube(Env.getCtx(), rs, trxName);
		} else if(tableName.equals(MKwitansi.Table_Name)){
			po = new MKwitansi(Env.getCtx(), rs, trxName);
		} else if(tableName.equals(MInvoice.Table_Name)){
			po = new MInvoice(Env.getCtx(), rs, trxName);
		} else if(tableName.equals(MInventory.Table_Name)){
			po = new MInventory(Env.getCtx(), rs, trxName);
		} else if(tableName.equals(MMovement.Table_Name)){
			po = new MMovement(Env.getCtx(), rs, trxName);
//		} else if(tableName.equals(MOrderLine.Table_Name)){
//			po = new MOrderLine(Env.getCtx(), rs, trxName);
		}
		return po;
	}

}
