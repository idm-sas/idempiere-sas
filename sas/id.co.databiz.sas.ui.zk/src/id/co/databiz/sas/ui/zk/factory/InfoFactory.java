/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2010 Heng Sin Low                							  *
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
package id.co.databiz.sas.ui.zk.factory;


import id.co.databiz.sas.ui.zk.panel.InfoProductPanel;
import id.co.databiz.sas.ui.zk.panel.InfoProductWindow;

import org.adempiere.webui.factory.IInfoFactory;
import org.adempiere.webui.info.InfoWindow;
import org.adempiere.webui.panel.InfoPanel;
import org.compiere.model.GridField;
import org.compiere.model.Lookup;
import org.compiere.model.MInfoWindow;
import org.compiere.util.Env;

/**
 *
 * @author Anozi Mada
 *
 */
public class InfoFactory implements IInfoFactory {

	@Override
	public InfoPanel create(int WindowNo, String tableName, String keyColumn,
			String value, boolean multiSelection, String whereClause, int AD_InfoWindow_ID, boolean lookup) {
		InfoPanel info = null;

        if (tableName.equals("M_Product")) {
        	info = new InfoProductWindow(WindowNo, tableName, keyColumn, value, multiSelection, whereClause, AD_InfoWindow_ID, lookup);
    		if (!info.loadedOK()) {
	            info = new InfoProductPanel ( WindowNo,
	            		Env.getContextAsInt(Env.getCtx(), WindowNo, "M_Warehouse_ID"),
	    				Env.getContextAsInt(Env.getCtx(), WindowNo, "M_PriceList_ID"),
	                    multiSelection, value,whereClause, lookup);
    		}
        }
        //
        return info;
	}

	@Override
	public InfoPanel create(Lookup lookup, GridField field, String tableName,
			String keyColumn, String queryValue, boolean multiSelection,
			String whereClause, int AD_InfoWindow_ID) {
		InfoPanel info = null;
		
		String col = lookup.getColumnName();		//	fully qualified name

		if (col.indexOf('.') != -1)
			col = col.substring(col.indexOf('.')+1);
						
		if (col.equals("M_Product_ID"))
		{
			InfoWindow infoWindow = new InfoProductWindow(lookup.getWindowNo(), tableName, keyColumn, queryValue, multiSelection, whereClause, AD_InfoWindow_ID);
			if (infoWindow.loadedOK())
				return infoWindow;
			
			//	Reset
			Env.setContext(Env.getCtx(), lookup.getWindowNo(), Env.TAB_INFO, "M_Product_ID", "0");
			Env.setContext(Env.getCtx(), lookup.getWindowNo(), Env.TAB_INFO, "M_AttributeSetInstance_ID", "0");
			Env.setContext(Env.getCtx(), lookup.getWindowNo(), Env.TAB_INFO, "M_Lookup_ID", "0");

			int M_Warehouse_ID = Env.getContextAsInt(Env.getCtx(), lookup.getWindowNo(), "M_Warehouse_ID");
			int M_PriceList_ID = Env.getContextAsInt(Env.getCtx(), lookup.getWindowNo(), "M_PriceList_ID");

			//	Show Info
			info = new InfoProductPanel (lookup.getWindowNo(),
					M_Warehouse_ID, M_PriceList_ID, true, queryValue, whereClause);

			info.setTitle("Product Info");
		}
		return info;
	}

	@Override
	public InfoWindow create(int AD_InfoWindow_ID) {
		MInfoWindow infoWindow = new MInfoWindow(Env.getCtx(), AD_InfoWindow_ID, (String)null);
		String tableName = infoWindow.getAD_Table().getTableName();
		String keyColumn = tableName + "_ID";
		InfoPanel info = create(-1, tableName, keyColumn, null, false, null, AD_InfoWindow_ID, false);
		if (info instanceof InfoWindow)
			return (InfoWindow) info;
		else
			return null;
	}

	@Override
	public InfoPanel create(int WindowNo, String tableName, String keyColumn, String value, boolean multiSelection,
			String whereClause, int AD_InfoWindow_ID, Lookup lookup) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InfoPanel create(int WindowNo, String tableName, String keyColumn, String value, boolean multiSelection,
			String whereClause, int AD_InfoWindow_ID, boolean lookup, GridField field) {
		// TODO Auto-generated method stub
		return null;
	}

}
