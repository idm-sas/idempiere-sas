package id.co.databiz.awn.callout;

import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MAttributeSetInstance;
import org.compiere.util.CLogger;
import org.compiere.util.Env;

public class DefaultCalloutInOutASI implements IColumnCallout {

	protected CLogger		log = CLogger.getCLogger(getClass());
	
	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {
		if (isCalloutActive(mTab))
			return "";
		Integer M_ASI_ID = (Integer)value;
		if (M_ASI_ID == null || M_ASI_ID.intValue() == 0)
			return "";
		//
		int M_Product_ID = Env.getContextAsInt(ctx, WindowNo, mTab.getTabNo(), "M_Product_ID");
		int M_Warehouse_ID = Env.getContextAsInt(ctx, WindowNo, "M_Warehouse_ID");
		int M_Locator_ID = Env.getContextAsInt(ctx, WindowNo, mTab.getTabNo(), "M_Locator_ID");
		if (log.isLoggable(Level.FINE)) log.fine("M_Product_ID=" + M_Product_ID
			+ ", M_ASI_ID=" + M_ASI_ID
			+ " - M_Warehouse_ID=" + M_Warehouse_ID
			+ ", M_Locator_ID=" + M_Locator_ID);
		//	Check Selection
		int M_AttributeSetInstance_ID =	Env.getContextAsInt(Env.getCtx(), WindowNo, Env.TAB_INFO, "M_AttributeSetInstance_ID");
		if (M_ASI_ID.intValue() == M_AttributeSetInstance_ID)
		{
			int selectedM_Locator_ID = Env.getContextAsInt(Env.getCtx(), WindowNo, Env.TAB_INFO, "M_Locator_ID");
			if (selectedM_Locator_ID != 0)
			{
				if (log.isLoggable(Level.FINE)) log.fine("Selected M_Locator_ID=" + selectedM_Locator_ID);
				mTab.setValue("M_Locator_ID", new Integer (selectedM_Locator_ID));
			}
		}
		MAttributeSetInstance asi = MAttributeSetInstance.get(ctx, M_ASI_ID.intValue(), 0);
		if (asi.getSerNo() != null) {
			// serialized ASI - force qty yo 1 - IDEMPIERE-1140
			mTab.setValue("MovementQty", Env.ONE);
			mTab.setValue("QtyEntered", Env.ONE);
		}
		return "";
	}
	
	protected boolean isCalloutActive(GridTab m_mTab)
	{
		//greater than 1 instead of 0 to discount this callout instance
		return m_mTab != null ? m_mTab.getActiveCallouts().length > 1 : false;
	}	

}
