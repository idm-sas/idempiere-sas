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
package id.co.databiz.awn.ui.zk.form;

import id.co.databiz.awn.form.InOutGenInvoice;

import java.util.ArrayList;
import java.util.logging.Level;

import org.adempiere.webui.apps.form.WGenForm;
import org.adempiere.webui.component.Checkbox;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.Listbox;
import org.adempiere.webui.component.ListboxFactory;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Textbox;
import org.adempiere.webui.editor.WDateEditor;
import org.adempiere.webui.editor.WSearchEditor;
import org.adempiere.webui.editor.WTableDirEditor;
import org.adempiere.webui.event.ValueChangeEvent;
import org.adempiere.webui.event.ValueChangeListener;
import org.adempiere.webui.event.WTableModelEvent;
import org.adempiere.webui.event.WTableModelListener;
import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.panel.IFormController;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.compiere.minigrid.IDColumn;
import org.compiere.model.MColumn;
import org.compiere.model.MInOut;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MOrg;
import org.compiere.model.MRMA;
import org.compiere.process.DocAction;
import org.compiere.util.CLogger;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.Msg;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;

/**
 * Generate Invoice (manual) view class
 * 
 */
public class WInOutGenInvoice extends InOutGenInvoice implements IFormController, EventListener<Event>, ValueChangeListener, WTableModelListener
{
	private WGenFormCustom form;
	private int m_WindowNo = 0;
	
	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(WInOutGenInvoice.class);
	//
	private Label lOrg = new Label();
	private WTableDirEditor fOrg;
	private Label lOrgTrx = new Label();
	private WTableDirEditor fOrgTrx;
	private Label lBPartner = new Label();
	private WSearchEditor fBPartner;
	private Label     lDocType = new Label();
	private Listbox  cmbDocType = ListboxFactory.newDropdownListbox();
	private Label   lDocAction = new Label();
	private WTableDirEditor docAction;
	
	private Label   lMvDate = new Label();
	private Label   lMvDateTo = new Label();
	private WDateEditor dtMvDate = new WDateEditor();
	private WDateEditor dtMvDate_To = new WDateEditor();
	private Label   dataStatus = new Label();
	private Label   lDataStatus = new Label();
	private Label   lDocNo = new Label();
	private Textbox DocumentNo = new Textbox();
	
	private Checkbox isConsolidate = new Checkbox();
	private Label lblConsolidate = new Label();
	private Label lCurrency = new Label();
	private WTableDirEditor fCurrency;
	
	public WInOutGenInvoice()
	{
		log.info("");
		
		form = new WGenFormCustom(this);
		Env.setContext(Env.getCtx(), form.getWindowNo(), "IsSOTrx", "Y");
		m_WindowNo = form.getWindowNo();
		try
		{
			super.dynInit();
			dynInit();
			zkInit();
//			executeQuery();
			form.postQueryEvent();
		}
		catch(Exception ex)
		{
			log.log(Level.SEVERE, "init", ex);
		}
	}	//	init
	
	/**
	 *	Static Init.
	 *  <pre>
	 *  selPanel (tabbed)
	 *      fOrg, fBPartner
	 *      scrollPane & miniTable
	 *  genPanel
	 *      info
	 *  </pre>
	 *  @throws Exception
	 */
	void zkInit() throws Exception
	{
		lOrg.setText(Msg.translate(Env.getCtx(), "AD_Org_ID"));
		lOrgTrx.setText(Msg.translate(Env.getCtx(), "AD_OrgTrx_ID"));
		lBPartner.setText(Msg.translate(Env.getCtx(), "C_BPartner_ID"));
		lMvDate.setText(Msg.translate(Env.getCtx(), "MovementDate"));
		lMvDateTo.setText("-");
		lDataStatus.setText("Total Selected");
		dataStatus.setText("0");
		lDocNo.setText(Msg.translate(Env.getCtx(), "DocumentNo"));
		lblConsolidate.setText(Msg.translate(Env.getCtx(), "ConsolidateDocument"));
		lCurrency.setText(Msg.translate(Env.getCtx(), "C_Currency_ID"));
		
		ZKUpdateUtil.setHflex(fOrg.getComponent(), "true");
		ZKUpdateUtil.setHflex(fOrgTrx.getComponent(), "true");
		ZKUpdateUtil.setHflex(fBPartner.getComponent(), "true");
		ZKUpdateUtil.setHflex(dtMvDate.getComponent(), "true");
		ZKUpdateUtil.setHflex(dtMvDate_To.getComponent(), "true");
		ZKUpdateUtil.setHflex(DocumentNo, "true");
		ZKUpdateUtil.setHflex(fCurrency.getComponent(), "true");
		ZKUpdateUtil.setHflex(docAction.getComponent(), "true");
		
		Row row = form.getParameterPanel().newRows().newRow();
		row.appendChild(lOrg.rightAlign());
		row.appendChild(fOrg.getComponent());
//		row.appendChild(new Space());
		row.appendChild(lBPartner.rightAlign());
		row.appendChild(fBPartner.getComponent());
//		row.appendChild(new Space());
		row.appendChild(lDocType.rightAlign());
		row.appendChild(cmbDocType);
//		row.appendChild(new Space());
		
		row = new Row();
		form.getParameterPanel().getRows().appendChild(row);		
		row.appendChild(lOrgTrx.rightAlign());
		row.appendChild(fOrgTrx.getComponent());
//		row.appendChild(new Space());
		row.appendChild(lMvDate.rightAlign());
		row.appendChild(dtMvDate.getComponent());
//		row.appendChild(new Space());
		row.appendChild(lMvDateTo.rightAlign());
		row.appendChild(dtMvDate_To.getComponent());
//		row.appendChild(new Space());
		
		row = new Row();
		form.getParameterPanel().getRows().appendChild(row);		
		row.appendChild(lDocAction.rightAlign());
		row.appendChild(docAction.getComponent());
//		row.appendChild(new Space());
		row.appendChild(lDocNo.rightAlign());
		row.appendChild(DocumentNo);
//		row.appendChild(new Space());
		row.appendChild(lblConsolidate.rightAlign());
		row.appendChild(isConsolidate);
//		row.appendChild(new Space());
		
		row = new Row();
		form.getParameterPanel().getRows().appendChild(row);
		row.appendChild(lCurrency.rightAlign());
		row.appendChild(fCurrency.getComponent());
		row.appendChild(lDataStatus.rightAlign());
		row.appendChild(dataStatus);
//		row.appendChild(new Space());
	}	//	jbInit

	/**
	 *	Fill Picks.
	 *		Column_ID from C_Order
	 *  @throws Exception if Lookups cannot be initialized
	 */
	public void dynInit() throws Exception
	{
		MLookup orgL = MLookupFactory.get (Env.getCtx(), form.getWindowNo(), 0, 2163, DisplayType.TableDir);
		fOrg = new WTableDirEditor ("AD_Org_ID", false, false, true, orgL);
		fOrg.setValue(Env.getContext(Env.getCtx(), "#AD_Org_ID"));
		m_AD_Org_ID = Env.getContext(Env.getCtx(), "#AD_Org_ID");
	//	lOrg.setText(Msg.translate(Env.getCtx(), "AD_Org_ID"));
		fOrg.addValueChangeListener(this);
		
		MLookup orgTrxL = MLookupFactory.get(Env.getCtx(), 
				form.getWindowNo(), 
				9331, 
				DisplayType.Table,
				Env.getLanguage(Env.getCtx()), 
				"AD_OrgTrx_ID",
				130,
				false,
				"UPPER(AD_Org.Value) LIKE 'T%'");
		fOrgTrx = new WTableDirEditor ("AD_OrgTrx_ID", false, false, true, orgTrxL);
//		fOrgTrx.setValue(Env.getContext(Env.getCtx(), "AD_OrgTrx_ID"));
		fOrgTrx.addValueChangeListener(this);
		
		//
		MLookup bpL = MLookupFactory.get (Env.getCtx(), form.getWindowNo(), 0, 2762, DisplayType.Search);
		fBPartner = new WSearchEditor ("C_BPartner_ID", false, false, true, bpL);
	//	lBPartner.setText(Msg.translate(Env.getCtx(), "C_BPartner_ID"));
		fBPartner.addValueChangeListener(this);
		//      Document Action Prepared/ Completed
		lDocAction.setText(Msg.translate(Env.getCtx(), "DocAction"));
		MLookup docActionL = MLookupFactory.get(Env.getCtx(), form.getWindowNo(), 3495 /* C_Invoice.DocAction */,
				DisplayType.List, Env.getLanguage(Env.getCtx()), "DocAction", 135 /* _Document Action */,
				false, "AD_Ref_List.Value IN ('PR','CO')");
		docAction = new WTableDirEditor("DocAction", true, false, true,docActionL);
		docAction.setValue(DocAction.ACTION_Prepare);
		docAction.addValueChangeListener(this);
        
//      Document Type Sales Order/Vendor RMA
        lDocType.setText(Msg.translate(Env.getCtx(), "C_DocType_ID"));
        cmbDocType.addItem(new KeyNamePair(MInOut.Table_ID, Msg.translate(Env.getCtx(), "Shipment")));
//        cmbDocType.addItem(new KeyNamePair(MRMA.Table_ID, Msg.translate(Env.getCtx(), "CustomerRMA"))); // ISY-6
        cmbDocType.addActionListener(this);
        cmbDocType.setSelectedIndex(0);
        
        dtMvDate.addValueChangeListener(this);
        dtMvDate_To.addValueChangeListener(this);
        form.getMiniTable().getModel().addTableModelListener(this);
        
        form.getStatusBar().setStatusLine(Msg.getMsg(Env.getCtx(), "InvGenerateSel"));//@@
        
        DocumentNo.addEventListener(Events.ON_OK, this);
        isConsolidate.addActionListener(this);
        
        MLookup curr = MLookupFactory.get (Env.getCtx(), m_WindowNo, 0, 3505, DisplayType.Table);
		fCurrency = new WTableDirEditor ("C_Currency_ID", true, false, true, curr);
		fCurrency.setValue(Env.getContext(Env.getCtx(), "$C_Currency_ID"));
		m_C_Currency_ID = fCurrency.getValue();
		fCurrency.addValueChangeListener(this);
	}	//	fillPicks
    
	/**
	 *  Query Info
	 */
	public void executeQuery()
	{
		KeyNamePair docTypeKNPair = cmbDocType.getSelectedItem().toKeyNamePair();
		executeQuery(docTypeKNPair, form.getMiniTable());
		form.getMiniTable().repaint();
		form.invalidate();
	}   //  executeQuery

	/**
	 *	Action Listener
	 *  @param e event
	 */
	public void onEvent(Event e)
	{
		log.info("Cmd=" + e.getTarget().getId());
		//
		if(cmbDocType.equals(e.getTarget()))
		{
		    form.postQueryEvent();
		    return;
		}
		//		Check Barcode entry R#1000442 ~dar~
		if (DocumentNo.equals(e.getTarget()))
		{
			int rows = form.getMiniTable().getRowCount();
			for (int i = 0; i < rows; i++)
			{
				IDColumn id = (IDColumn)form.getMiniTable().getValueAt(i, 0);     //  ID in column 0
				String tbl_docno = form.getMiniTable().getValueAt(i, 3).toString();     //  DocumentNo in column 3
				String sel_docno = DocumentNo.getValue().toString();
				if (tbl_docno.compareTo(sel_docno)==0)
					{
					id.setSelected(true);
					form.getMiniTable().setValueAt((IDColumn)id, i, 0);
					}
			}
			DocumentNo.setValue(null); //refresh field
			return;
		}
		if( isConsolidate.equals(e.getTarget()) ) {
			m_IsConsolidate = isConsolidate.isChecked();
			return;
		}
			
		//	End R#1000442 ~dar~

		//
		validate();
	}	//	actionPerformed
	
	public void validate()
	{
		form.saveSelection();
		
		ArrayList<Integer> selection = getSelection();
		if (selection != null && selection.size() > 0 && isSelectionActive())
			form.generate();
		else
			form.dispose();
	}

	/**
	 *	Value Change Listener - requery
	 *  @param e event
	 */
	public void valueChange(ValueChangeEvent e)
	{
		log.info(e.getPropertyName() + "=" + e.getNewValue());
		if (e.getSource().equals(fOrg))
			m_AD_Org_ID = e.getNewValue();
		if (e.getSource().equals(fOrgTrx))
			m_AD_OrgTrx_ID = e.getNewValue();
		if (e.getSource().equals(fBPartner))
		{
			m_C_BPartner_ID = e.getNewValue();
			fBPartner.setValue(m_C_BPartner_ID);	//	display value
		}
		if( e.getSource().equals(dtMvDate) ) 
			m_MovementDate = e.getNewValue();
		if( e.getSource().equals(dtMvDate_To) ) 
			m_MovementDate_To = e.getNewValue();
		if( e.getSource().equals(isConsolidate) ) 
			m_IsConsolidate = isConsolidate.isChecked();
		if( e.getSource().equals(fCurrency) ) 
			m_C_Currency_ID = e.getNewValue();
		
		form.postQueryEvent();
	}	//	vetoableChange
	
	/**************************************************************************
	 *	Generate Shipments
	 */
	public String generate()
	{
		KeyNamePair docTypeKNPair = (KeyNamePair)cmbDocType.getSelectedItem().toKeyNamePair();
		String docActionSelected = (String)docAction.getValue();	
		return generate(form.getStatusBar(), docTypeKNPair, docActionSelected);
	}	//	generateShipments
	
	public ADForm getForm()
	{
		return form;
	}

	@Override
	public void tableChanged(WTableModelEvent e) {
		if (e.getColumn() == 0) {
			dataStatus.setText(calculateSelection(form.getMiniTable()));
		}
	}
}