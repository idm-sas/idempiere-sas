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
package id.co.databiz.awn.ui.zk.form;

import id.co.databiz.awn.form.GeneratePOFromRequisition;
import id.co.databiz.awn.model.SystemID;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.webui.component.Borderlayout;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.GridFactory;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.ListModelTable;
import org.adempiere.webui.component.ListboxFactory;
import org.adempiere.webui.component.Panel;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.component.Textbox;
import org.adempiere.webui.component.WListbox;
import org.adempiere.webui.editor.WDateEditor;
import org.adempiere.webui.editor.WSearchEditor;
import org.adempiere.webui.editor.WTableDirEditor;
import org.adempiere.webui.event.ValueChangeEvent;
import org.adempiere.webui.event.ValueChangeListener;
import org.adempiere.webui.event.WTableModelEvent;
import org.adempiere.webui.event.WTableModelListener;
import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.panel.CustomForm;
import org.adempiere.webui.panel.IFormController;
import org.adempiere.webui.panel.StatusBarPanel;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.adempiere.webui.window.FDialog;
import org.compiere.model.MBPartner;
import org.compiere.model.MColumn;
import org.compiere.model.MDocType;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MOrder;
import org.compiere.model.MOrg;
import org.compiere.model.MPriceList;
import org.compiere.model.MProject;
import org.compiere.model.MRequisition;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.zkoss.zk.ui.UiException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Center;
import org.zkoss.zul.North;
import org.zkoss.zul.Separator;
import org.zkoss.zul.South;

/**
 * Generate PO Form Requisition
 *
 * @author  Anozi Mada
 */
public class WGeneratePOFromRequisition extends GeneratePOFromRequisition
	implements IFormController, EventListener<Event>, WTableModelListener, ValueChangeListener
{
	protected static CLogger log = CLogger.getCLogger(WGeneratePOFromRequisition.class);
	
	private CustomForm form = new CustomForm();

	/**
	 *	Initialize Panel
	 *  @param WindowNo window
	 *  @param frame frame
	 */
	public WGeneratePOFromRequisition()
	{
		Env.setContext(Env.getCtx(), form.getWindowNo(), "IsSOTrx", "N");   //  defaults to no
		try
		{
			super.dynInit();
			dynInit();
			zkInit();
			southPanel.appendChild(new Separator());
			southPanel.appendChild(statusBar);
			configureMiniTable(table);
			List<String> columnList = new ArrayList<String>();
			columnList.add("ID");
			columnList.add(Msg.translate(Env.getCtx(), "DocumentNo"));
			columnList.add(Msg.translate(Env.getCtx(), "C_BPartner_ID"));
			columnList.add(Msg.translate(Env.getCtx(), "M_Product_ID"));
			columnList.add(Msg.translate(Env.getCtx(), "DateRequired"));
			columnList.add(Msg.translate(Env.getCtx(), "QtyRequired"));
			columnList.add( Msg.translate(Env.getCtx(), "QtyOrdered"));
			columnList.add("Open Qty");
			table.setData(new ListModelTable(), columnList);
		}
		catch(Exception e)
		{
			log.log(Level.SEVERE, "", e);
		}
	}	//	init
	
	//
	private Borderlayout mainLayout = new Borderlayout();
	private Panel parameterPanel = new Panel();
	private Panel tablePanel = new Panel();
	private Grid parameterLayout = GridFactory.newGridLayout();
	private Borderlayout tableLayout = new Borderlayout();
	private StatusBarPanel statusBar = new StatusBarPanel();	
	private Panel southPanel = new Panel();
	
	private WListbox table = ListboxFactory.newDataTable();
	
	private Label lOrg = new Label();
	private WTableDirEditor fOrg;
	private Label lBPartner = new Label();
	private WSearchEditor fBpartner;
	private Label lPriceList = new Label();
	private WTableDirEditor fPriceList;
	
	private Label lRequisition = new Label();
	private WSearchEditor fRequisition;
	private Label lProject = new Label();
	private WTableDirEditor fProject;
	private Label lUser1 = new Label();
	private WTableDirEditor fUser1;
	
	private Label lDocType = new Label();
	private WTableDirEditor fDocType;
	private Label lProductValue = new Label();
	private Textbox fProductValue = new Textbox();
	private Label lProductClass = new Label();
	private Textbox fProductClass = new Textbox();
	
	private Label lDateRequired = new Label();
	private WDateEditor fDateRequired = new WDateEditor();
	private Label lGroup1 = new Label();
	private Textbox fGroup1 = new Textbox();
	private Label lGroup2 = new Label();
	private Textbox fGroup2 = new Textbox();
	
	private Button processButton = new Button("Process");
	/**
	 *  Static Init
	 *  @throws Exception
	 */
	private void zkInit() throws Exception
	{
		form.appendChild(mainLayout);
		ZKUpdateUtil.setWidth(mainLayout, "99%");
		ZKUpdateUtil.setHeight(mainLayout, "100%");
		
		parameterPanel.appendChild(parameterLayout);

		North north = new North();
		north.setStyle("border: none");
		mainLayout.appendChild(north);
		north.appendChild(parameterPanel);
		
		lOrg.setText(Msg.translate(Env.getCtx(), "AD_Org_ID"));
		lBPartner.setText(Msg.translate(Env.getCtx(), "C_BPartner_ID"));
		lPriceList.setText(Msg.translate(Env.getCtx(), "M_PriceList_ID"));
		lRequisition.setText(Msg.translate(Env.getCtx(), "M_Requisition_ID"));
		lProject.setText(Msg.translate(Env.getCtx(), "C_Project_ID"));
		lUser1.setText("Cost Center");
		lDocType.setText(Msg.translate(Env.getCtx(), "C_DocTypeTarget_ID"));
		lProductValue.setText("Product Code");
		lProductClass.setText(Msg.translate(Env.getCtx(), "Classification"));
		lDateRequired.setText(Msg.translate(Env.getCtx(), "DateRequired"));
		lGroup1.setText(Msg.translate(Env.getCtx(), "Group1"));
		lGroup2.setText(Msg.translate(Env.getCtx(), "Group2"));
		
		ZKUpdateUtil.setHflex(fOrg.getComponent(), "true");
		ZKUpdateUtil.setHflex(fBpartner.getComponent(), "true");
		ZKUpdateUtil.setHflex(fPriceList.getComponent(), "true");
		ZKUpdateUtil.setHflex(fRequisition.getComponent(), "true");
		ZKUpdateUtil.setHflex(fProject.getComponent(), "true");
		ZKUpdateUtil.setHflex(fUser1.getComponent(), "true");
		ZKUpdateUtil.setHflex(fDocType.getComponent(), "true");
		ZKUpdateUtil.setHflex(fProductValue, "true");
		ZKUpdateUtil.setHflex(fProductClass, "true");
		ZKUpdateUtil.setHflex(fDateRequired.getComponent(), "true");
		ZKUpdateUtil.setHflex(fGroup1, "true");
		ZKUpdateUtil.setHflex(fGroup2, "true");
		
		Rows rows = null;
		Row row = null;
		
		rows = parameterLayout.newRows();
		row = rows.newRow();
		row.appendChild(lOrg.rightAlign());
		row.appendChild(fOrg.getComponent());
		row.appendChild(lBPartner.rightAlign());
		row.appendChild(fBpartner.getComponent());
		row.appendChild(lPriceList.rightAlign());
		row.appendChild(fPriceList.getComponent());
		
		row = rows.newRow();
		row.appendChild(lRequisition.rightAlign());
		row.appendChild(fRequisition.getComponent());
		row.appendChild(lProject.rightAlign());
		row.appendChild(fProject.getComponent());
		row.appendChild(lUser1.rightAlign());
		row.appendChild(fUser1.getComponent());
		
		row = rows.newRow();
		row.appendChild(lDocType.rightAlign());
		row.appendChild(fDocType.getComponent());
		row.appendChild(lProductValue.rightAlign());
		row.appendChild(fProductValue);
		row.appendChild(lProductClass.rightAlign());
		row.appendChild(fProductClass);
		
		row = rows.newRow();
		row.appendChild(lDateRequired.rightAlign());
		row.appendChild(fDateRequired.getComponent());
		row.appendChild(lGroup1.rightAlign());
		row.appendChild(fGroup1);
		row.appendChild(lGroup2.rightAlign());
		row.appendChild(fGroup2);
		
		Center center = new Center();
		center.setAutoscroll(true);
        center.setFlex(true);
		mainLayout.appendChild(center);
		center.appendChild(table);
		ZKUpdateUtil.setWidth(table, "99%");
		ZKUpdateUtil.setHeight(table, "99%");
				
		South south = new South();
		south.setStyle("border: none");
		mainLayout.appendChild(south);
		south.appendChild(southPanel);
		southPanel.setStyle("text-align: center");
		southPanel.appendChild(processButton);
	}   //  jbInit

	/**
	 *  Dynamic Init (prepare dynamic fields)
	 *  @throws Exception if Lookups cannot be initialized
	 */
	public void dynInit() throws Exception
	{
		Properties ctx = Env.getCtx();
		
		MLookup orgLookup = MLookupFactory.get(ctx, 
				form.getWindowNo(), 
				MColumn.getColumn_ID(MOrg.Table_Name,MOrg.COLUMNNAME_AD_Org_ID), 
				DisplayType.TableDir,
				Env.getLanguage(ctx), 
				MOrg.COLUMNNAME_AD_Org_ID,
				322,
				false,
				"IsOrgTrxDim='N' AND IsSummary='N' AND AD_Org_ID <> 0");
		fOrg = new WTableDirEditor("AD_Org_ID", false, false, true, orgLookup);		
				
		MLookup bpartnerLookup = MLookupFactory.get(ctx, 
				form.getWindowNo(),
				0,
				MColumn.getColumn_ID(MBPartner.Table_Name,MBPartner.COLUMNNAME_C_BPartner_ID), 
				DisplayType.Search);
		fBpartner = new WSearchEditor("C_BPartner_ID", true, false, true, bpartnerLookup);		
		
		MLookup pricelistLookup = MLookupFactory.get(ctx, 
				form.getWindowNo(), 
				MColumn.getColumn_ID(MPriceList.Table_Name,MPriceList.COLUMNNAME_M_PriceList_ID), 
				DisplayType.TableDir,
				Env.getLanguage(ctx), 
				MPriceList.COLUMNNAME_M_PriceList_ID,
				166,
				false,
				"IsInternal='N' AND IsSOPriceList='N'");
		fPriceList = new WTableDirEditor("M_PriceList_ID", false, false, true, pricelistLookup);
		
		MLookup projectLookup = MLookupFactory.get(ctx, 
				form.getWindowNo(), 
				0,
				MColumn.getColumn_ID(MProject.Table_Name,MProject.COLUMNNAME_C_Project_ID), 
				DisplayType.TableDir);
		fProject = new WTableDirEditor("C_Project_ID", false, false, true, projectLookup);
		
		MLookup user1Lookup = MLookupFactory.get(ctx, 
				form.getWindowNo(), 
				SystemID.COLUMN_M_REQUISITION_USER1_ID, 
				DisplayType.TableDir,
				Env.getLanguage(ctx), 
				"C_ElementValue_ID",
				134,
				false,
				"C_ElementValue.IsActive='Y' AND C_ElementValue.IsSummary='N' AND C_ElementValue.C_Element_ID IN (SELECT C_Element_ID FROM C_AcctSchema_Element ase WHERE ase.ElementType='U1' AND ase.AD_Client_ID="+Env.getAD_Client_ID(ctx)+")");
		fUser1 = new WTableDirEditor("User1_ID", false, false, true, user1Lookup);
		
		MLookup docTypeLookup = MLookupFactory.get(ctx, 
				form.getWindowNo(), 
				MColumn.getColumn_ID(MOrder.Table_Name,MOrder.COLUMNNAME_C_DocTypeTarget_ID), 
				DisplayType.Table,
				Env.getLanguage(ctx), 
				MDocType.COLUMNNAME_C_DocType_ID,
				170,
				false,
				"C_DocType.DocBaseType = 'POO' AND C_DocType.DocSubTypeSO IS NULL AND C_DocType.AD_Client_ID = " + Env.getAD_Client_ID(ctx));
		fDocType = new WTableDirEditor("C_DocType_ID", true, false, true, docTypeLookup);
		MDocType defaultDocType = new Query(ctx, MDocType.Table_Name, "C_DocType.DocBaseType = 'POO' AND C_DocType.DocSubTypeSO IS NULL AND C_DocType.AD_Client_ID = " + Env.getAD_Client_ID(ctx), null).first();
		if(defaultDocType!=null){
			fDocType.setValue(defaultDocType.get_ID());
			setC_DocType_ID(defaultDocType.get_ID());
		}
		
		MLookup requisitionLookup = MLookupFactory.get(ctx, 
				form.getWindowNo(), 
				MColumn.getColumn_ID(MRequisition.Table_Name,MRequisition.COLUMNNAME_M_Requisition_ID),
				DisplayType.Search,
				Env.getLanguage(ctx), 
				MRequisition.COLUMNNAME_M_Requisition_ID,
				0,
				false,
				"M_Requisition.DocStatus IN ('CO','CL') AND EXISTS(SELECT 1 FROM M_RequisitionLine rl WHERE rl.M_Requisition_ID=M_Requisition.M_Requisition_ID AND rl.Qty > (SELECT COALESCE(SUM(ol.QtyOrdered),0) FROM C_OrderLine ol WHERE ol.M_RequisitionLine_ID = rl.M_RequisitionLine_ID))");
		fRequisition = new WSearchEditor("M_Requisition_ID", false, false, true, requisitionLookup);
		
		fOrg.addValueChangeListener(this);
		fBpartner.addValueChangeListener(this);
		fPriceList.addValueChangeListener(this);
		fDateRequired.addValueChangeListener(this);
		fProject.addValueChangeListener(this);
		fUser1.addValueChangeListener(this);
		fRequisition.addValueChangeListener(this);
		fDocType.addValueChangeListener(this);
		
		fProductValue.addEventListener(Events.ON_BLUR, this);
		fProductClass.addEventListener(Events.ON_BLUR, this);
		fGroup1.addEventListener(Events.ON_BLUR, this);
		fGroup2.addEventListener(Events.ON_BLUR, this);
		
		fProductValue.addEventListener(Events.ON_OK, this);
		fProductClass.addEventListener(Events.ON_OK, this);
		fGroup1.addEventListener(Events.ON_OK, this);
		fGroup2.addEventListener(Events.ON_OK, this);
		
		processButton.addActionListener(this);
		
		table.getModel().addTableModelListener(this);
	}   //  dynInit
	
	/**************************************************************************
	 *  Action Listener.
	 *  - MultiCurrency
	 *  - Allocate
	 *  @param e event
	 */
	public void onEvent(Event e)
	{
		log.config("");
		if (e.getTarget().equals(processButton)){
			MOrder order;
			try {
				order = generatePO(table);
				if(order!=null && order.getLines()!=null){
					FDialog.info(form.getWindowNo(), form, "", "PO " + order.getDocumentNo() + "\n" + createHTMLTable(order));
					executeQuery();
				}
			} catch (UiException ex) {
				log.warning(ex.getMessage());
			} catch (Exception ex) {
				FDialog.error(form.getWindowNo(), ex.getMessage());
			}
		}
		
		if(Events.ON_BLUR.equals(e.getName())) {
			if(e.getTarget().equals(fProductValue)){
				setProductValue(fProductValue.getText());
			} else if(e.getTarget().equals(fProductClass)){
				setClassification(fProductClass.getText());
			} else if(e.getTarget().equals(fGroup1)){
				setGroup1(fGroup1.getText());
			} else if(e.getTarget().equals(fGroup2)){
				setGroup2(fGroup2.getText());
			}
			executeQuery();
		} else if(Events.ON_OK.equals(e.getName())) {
			if(e.getTarget().equals(fProductValue)){
				setProductValue(fProductValue.getText());
			} else if(e.getTarget().equals(fProductClass)){
				setClassification(fProductClass.getText());
			} else if(e.getTarget().equals(fGroup1)){
				setGroup1(fGroup1.getText());
			} else if(e.getTarget().equals(fGroup2)){
				setGroup2(fGroup2.getText());
			}
			executeQuery();
		}
		
	}   //  actionPerformed
	
	public void executeQuery() {
		if(getC_BPartner_ID()>0 && (getM_PriceList_ID()>0 || getM_Requisition_ID()>0)){
			super.executeQuery(table);
			statusBar.setStatusLine(table.getRowCount() + " records found");
		}
		if(getM_PriceList_ID()==0 && getM_Requisition_ID()==0){
			table.setRowCount(0);
		}
		table.setMultiSelection(true);
		table.setMultiple(true);
	} // executeQuery

	/**
	 *  Table Model Listener.
	 *  - Recalculate Totals
	 *  @param e event
	 */
	public void tableChanged(WTableModelEvent e)
	{
		int row = e.getFirstRow();
		if(table.getRowCount()>0 && row > -1){			
			BigDecimal qtyOrdered = (BigDecimal) table.getValueAt(row, 6);
			BigDecimal qtyOpen = (BigDecimal) table.getValueAt(row, 7);
			if(qtyOrdered!=null && qtyOpen!=null && qtyOrdered.compareTo(qtyOpen)>0){
				FDialog.error(form.getWindowNo(), "Max open qty is " + qtyOpen);
				table.setValueAt(qtyOpen, row, 6);
			}
		}		
	}   //  tableChanged
	
	/**
	 *  Vetoable Change Listener.
	 *  @param e event
	 */
	public void valueChange (ValueChangeEvent e)
	{
		Object source = e.getSource();
		Object value = e.getNewValue();
		log.fine("WGeneratePOFromRequisition.vetoableChange - " + source + "=" + value);
		if (value == null && !source.equals(fDateRequired)){
			value = new Integer(0);
		}			
		
		if(source.equals(fOrg)){
			setAD_Org_ID((Integer) value);
		} else if(source.equals(fBpartner)){
			setC_BPartner_ID((Integer) value);
		} else if(source.equals(fPriceList)){
			setM_PriceList_ID((Integer) value);
		} else if(source.equals(fDocType)){
			setC_DocType_ID((Integer) value);
		} else if(source.equals(fDateRequired)){
			setDateRequired((Date)value);
		} else if(source.equals(fProject)){
			setC_Project_ID((Integer) value);
		} else if(source.equals(fUser1)){
			setUser1_ID((Integer) value);
		} else if(source.equals(fRequisition)){
			setM_Requisition_ID((Integer) value);
			if((Integer)value>0){
				setM_PriceList_ID(0);
				fPriceList.setValue(null);
			}
		}
		
		executeQuery();
	}   //  vetoableChange

	
	/**
	 * Called by org.adempiere.webui.panel.ADForm.openForm(int)
	 * @return
	 */
	public ADForm getForm()
	{
		return form;
	}

}   //  VGeneratePOFromRequisition
