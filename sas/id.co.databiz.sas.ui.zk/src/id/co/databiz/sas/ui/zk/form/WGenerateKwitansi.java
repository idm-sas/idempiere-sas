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
package id.co.databiz.sas.ui.zk.form;

import id.co.databiz.awn.model.MKwitansi;
import id.co.databiz.awn.model.SystemID;
import id.co.databiz.sas.form.GenerateKwitansi;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.webui.apps.form.WGenForm;
import org.adempiere.webui.component.Checkbox;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.ListModelTable;
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
import org.adempiere.webui.window.FDialog;
import org.compiere.minigrid.IDColumn;
import org.compiere.model.MBPartner;
import org.compiere.model.MColumn;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MUser;
import org.compiere.util.CLogger;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Trx;
import org.compiere.util.TrxRunnable;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Space;

/**
 * Generate Invoice (manual) view class
 * 
 */
public class WGenerateKwitansi extends GenerateKwitansi implements IFormController, EventListener, ValueChangeListener, WTableModelListener
{
	private WGenForm form;
	private int m_WindowNo;
	
	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(WGenerateKwitansi.class);
	//
	private Label lOrg = new Label();
	private WTableDirEditor fOrg;
	private Label lBPartner = new Label();
	private WSearchEditor fBPartner;	
	private Label     lDocType = new Label();
	private WTableDirEditor  fDocType = null;	
	//
	private Label lDateInvoiced = new Label();
	private Label lDateInvoicedTo = new Label();
	private WDateEditor fDateInvoiced = new WDateEditor();
	private WDateEditor fDateInvoiced_To = new WDateEditor();
	private Label dataStatus = new Label();
	private Label lDataStatus = new Label();
	private Label	lDocumentNo = new Label();
	private Textbox	fDocumentNo = new Textbox(); 

	private Checkbox fConsolidate = new Checkbox();
	private Label	lConsolidate = new Label(); 
	private Label	lCurrency = new Label();
	private WTableDirEditor fCurrency;
	
	private Label	lKwitansi = new Label();
	private WSearchEditor fKwitansi;
	private Label lDateDoc = new Label();
	private WDateEditor fDateDoc;
	
	private Label lDueDate = new Label();
	private Label lDueDateTo = new Label();
	private WDateEditor fDueDate = new WDateEditor();
	private WDateEditor fDueDate_To = new WDateEditor();
	
	private Label lBilling = new Label();
	private WTableDirEditor fBilling;
	
	private List<String> columnList = new ArrayList<String>();
	
	public WGenerateKwitansi()
	{
		log.info("");
		
		form = new WGenForm(this);
		m_WindowNo = form.getWindowNo();
		Env.setContext(Env.getCtx(), form.getWindowNo(), "IsSOTrx", "Y");
		
		try
		{
			super.dynInit();
			dynInit();
			zkInit();
			configureMiniTable(form.getMiniTable());
			form.getMiniTable().setData(new ListModelTable(), columnList);
//			executeQuery();
//			form.postQueryEvent();
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
		columnList.add("ID");
		columnList.add(Msg.translate(Env.getCtx(), "AD_Org_ID"));
		columnList.add(Msg.translate(Env.getCtx(), "AD_OrgTrx_ID"));
		columnList.add(Msg.translate(Env.getCtx(), "C_DocType_ID"));
		columnList.add(Msg.translate(Env.getCtx(), "DocumentNo"));
		columnList.add(Msg.translate(Env.getCtx(), "C_BPartner_ID"));
		columnList.add(Msg.translate(Env.getCtx(), "DateInvoiced"));
		columnList.add(Msg.translate(Env.getCtx(), "TotalLines"));
		columnList.add("Tax Invoice");	
		columnList.add(Msg.translate(Env.getCtx(), "DueDate"));
		
		lOrg.setText(Msg.translate(Env.getCtx(), "AD_Org_ID"));
		lBPartner.setText(Msg.translate(Env.getCtx(), "C_BPartner_ID"));
		lDocType.setText(Msg.translate(Env.getCtx(), "C_DocType_ID"));
		lDateInvoiced.setText(Msg.translate(Env.getCtx(), "DateInvoiced"));
		lDateInvoicedTo.setText(Msg.translate(Env.getCtx(), " - "));
		lDataStatus.setText(Msg.translate(Env.getCtx(), "Total selected"));
		lDocumentNo.setText(Msg.translate(Env.getCtx(), "DocumentNo"));
		lConsolidate.setText(Msg.translate(Env.getCtx(), "IsConsolidate"));
		lCurrency.setText(Msg.translate(Env.getCtx(), "C_Currency_ID"));
		lKwitansi.setText(Msg.translate(Env.getCtx(), "Z_Kwitansi_ID"));
		lDateDoc.setText(Msg.translate(Env.getCtx(), "DateDoc"));
		lDueDate.setText(Msg.translate(Env.getCtx(), "DueDate"));
		lDueDateTo.setText(Msg.translate(Env.getCtx(), " - "));
		lBilling.setText(Msg.translate(Env.getCtx(), "Billing_ID"));
		
		ZKUpdateUtil.setHflex(lDateInvoicedTo,"1");
		ZKUpdateUtil.setHflex(lDueDateTo,"1");
		
		ZKUpdateUtil.setHflex(fOrg.getComponent(),"true");
		ZKUpdateUtil.setHflex(fBPartner.getComponent(),"true");
		ZKUpdateUtil.setHflex(fDocType.getComponent(),"true");
		ZKUpdateUtil.setHflex(fDateInvoiced.getComponent(),"true");
		ZKUpdateUtil.setHflex(fDateInvoiced_To.getComponent(),"true");
		ZKUpdateUtil.setHflex(fDocumentNo,"true");
		ZKUpdateUtil.setHflex(fCurrency.getComponent(),"true");
		ZKUpdateUtil.setHflex(fKwitansi.getComponent(),"true");
		ZKUpdateUtil.setHflex(fDateDoc.getComponent(),"true");
		ZKUpdateUtil.setHflex(fDueDate.getComponent(),"true");
		ZKUpdateUtil.setHflex(fDueDate_To.getComponent(),"true");
		ZKUpdateUtil.setHflex(fBilling.getComponent(),"true");
		
		// 1st row
		Row row = form.getParameterPanel().newRows().newRow();
		row.appendChild(lOrg.rightAlign());
		row.appendChild(fOrg.getComponent());
		//row.appendChild(new Space());
		row.appendChild(lBPartner.rightAlign());
		row.appendChild(fBPartner.getComponent());
		//row.appendChild(new Space());
		row.appendChild(lDocType.rightAlign());
		row.appendChild(fDocType.getComponent());
		row.appendChild(lDocumentNo.rightAlign());
		row.appendChild(fDocumentNo);
		
		// 2nd row
		row = new Row();
		form.getParameterPanel().getRows().appendChild(row);
		row.appendChild(lDateInvoiced.rightAlign());
		row.appendChild(fDateInvoiced.getComponent());
		//row.appendChild(new Space());
		//row.appendChild(lDateInvoicedTo.rightAlign());
		row.appendChild(fDateInvoiced_To.getComponent());
		row.appendChild(new Space());
		row.appendChild(lDueDate.rightAlign());
		row.appendChild(fDueDate.getComponent());
		//row.appendChild(new Space());
		//row.appendChild(lDueDateTo.rightAlign());
		row.appendChild(fDueDate_To.getComponent());
		
		// 3rd row
		row = new Row();
		form.getParameterPanel().getRows().appendChild(row);
		row.appendChild(lDateDoc.rightAlign());
		row.appendChild(fDateDoc.getComponent());
		row.appendChild(lKwitansi.rightAlign());
		row.appendChild(fKwitansi.getComponent());
		row.appendChild(lCurrency.rightAlign());
		row.appendChild(fCurrency.getComponent());
		row.appendChild(lConsolidate.rightAlign());
		row.appendChild(fConsolidate);
		
		// 4th row
		row = new Row();
		form.getParameterPanel().getRows().appendChild(row);
		row.appendChild(lBilling.rightAlign());
		row.appendChild(fBilling.getComponent());
		row.appendChild(lDataStatus.rightAlign());
		row.appendChild(dataStatus);
		
	}	//	jbInit

	/**
	 *	Fill Picks.
	 *		Column_ID from C_Order
	 *  @throws Exception if Lookups cannot be initialized
	 */
	public void dynInit() throws Exception
	{
		Properties ctx = Env.getCtx();
		
		MLookup orgLookup = MLookupFactory.get (ctx, 
				m_WindowNo, 
				0, 
				MColumn.getColumn_ID(MInvoice.Table_Name, MInvoice.COLUMNNAME_AD_Org_ID), 
				DisplayType.TableDir);
		fOrg = new WTableDirEditor("AD_Org_ID", false, false, true, orgLookup);
		int loginOrg = Env.getContextAsInt(Env.getCtx(), "#AD_Org_ID");
		if(loginOrg>0){
			fOrg.setValue(loginOrg);
			setOrgID(loginOrg);
		}
		
		MLookup bpartnerLookup = MLookupFactory.get (ctx, 
				m_WindowNo, 
				0, 
				MColumn.getColumn_ID(MInvoice.Table_Name, MInvoice.COLUMNNAME_C_BPartner_ID), 
				DisplayType.Search);
		fBPartner = new WSearchEditor ("C_BPartner_ID", false, false, true, bpartnerLookup);		
		
		MLookup docTypeLookup = MLookupFactory.get(ctx, 
				m_WindowNo, 
				MColumn.getColumn_ID(MDocType.Table_Name, MDocType.COLUMNNAME_C_DocType_ID), 
				DisplayType.Table, 
				Env.getLanguage(ctx), 
				MDocType.COLUMNNAME_C_DocType_ID, 
				0, 
				false, 
				"IsActive='Y' AND DocBaseType IN ('ARI','ARC','APC') ");
        lDocType.setText(Msg.translate(Env.getCtx(), "C_DocType_ID"));
        fDocType = new WTableDirEditor("C_DocType_ID", false, false, true, docTypeLookup);               
        
        MLookup currencyLookup = MLookupFactory.get (ctx, 
        		m_WindowNo, 
        		0, 
        		MColumn.getColumn_ID(MInvoice.Table_Name, MInvoice.COLUMNNAME_C_Currency_ID), 
        		DisplayType.Table);
		fCurrency = new WTableDirEditor ("C_Currency_ID", true, false, true, currencyLookup);
		fCurrency.setValue(Env.getContextAsInt(ctx, m_WindowNo, "$C_Currency_ID"));
		setCurrencyID(fCurrency.getValue());
		
		MLookup kwitansiLookup = MLookupFactory.get(ctx, 
				m_WindowNo, 
				MColumn.getColumn_ID(MInvoice.Table_Name, "Z_Kwitansi_ID"), 
				DisplayType.Search, 
				Env.getLanguage(ctx), 
				MKwitansi.COLUMNNAME_Z_Kwitansi_ID, 
				550077,
				false, 
				"NOT EXISTS(SELECT 1 FROM C_Invoice WHERE C_Invoice.Z_Kwitansi_ID = Z_Kwitansi.Z_Kwitansi_ID)");
		fKwitansi = new WSearchEditor("Z_Kwitansi_ID", false, false, true, kwitansiLookup);
		
		MLookup billingLookup = MLookupFactory.get (ctx, 
				m_WindowNo, 
				MColumn.getColumn_ID(MUser.Table_Name, MUser.COLUMNNAME_AD_User_ID), 
				DisplayType.Table,
				Env.getLanguage(ctx),
				MColumn.getColumnName(Env.getCtx(), MColumn.getColumn_ID(MUser.Table_Name, MUser.COLUMNNAME_AD_User_ID)),
				0,
				false,
				"C_Job_ID = 1000000");
		fBilling = new WTableDirEditor ("AD_User_ID", false, false, true, billingLookup);	
		
		fConsolidate.setSelected(false);
		setIsConsolidate(false);
		
		fOrg.addValueChangeListener(this);
		fDocType.addValueChangeListener(this);
		fCurrency.addValueChangeListener(this);
		fBPartner.addValueChangeListener(this);
		fDateInvoiced.addValueChangeListener(this);
        fDateInvoiced_To.addValueChangeListener(this);
        fDueDate.addValueChangeListener(this);
        fDueDate_To.addValueChangeListener(this);
        fKwitansi.addValueChangeListener(this);
        fBilling.addValueChangeListener(this);
        fDateDoc = new WDateEditor("DateDoc", "DateDoc", true, false, true);
        lDateDoc.setText(Msg.translate(Env.getCtx(), "DateDoc"));
        fDateDoc.addValueChangeListener(this);
        fDateDoc.setValue(Env.getContextAsDate(Env.getCtx(), "#Date"));
        setDateDoc(fDateDoc.getValue());
        fDocumentNo.addEventListener(Events.ON_OK, this);
        fConsolidate.addActionListener(this);
        form.getMiniTable().getModel().addTableModelListener(this);
        
        form.getStatusBar().setStatusLine("");
	}	//	fillPicks
    
	/**
	 *  Query Info
	 */
	public void executeQuery()
	{
		if(getCurrencyID()!=null && getDateDoc()!=null){
			executeQuery(form.getMiniTable());
			form.getMiniTable().setData(form.getMiniTable().getModel(), columnList);
			form.getMiniTable().setMultiSelection(true);
			form.getMiniTable().setMultiple(true);
			form.getMiniTable().repaint();
			form.getMiniTable().getModel().addTableModelListener(this);
			//form.invalidate();
		}
	}   //  executeQuery

	/**
	 *	Action Listener
	 *  @param e event
	 */
	public void onEvent(Event e)
	{
		log.info("Cmd=" + e.getTarget().getId());
		//
		if(Events.ON_OK.equals(e.getName())) {
			if(e.getTarget().equals(fDocumentNo)){
				int rows = form.getMiniTable().getRowCount();
				List<Integer> selectedList = new ArrayList<Integer>();
				for (int i = 0; i < rows; i++)
				{
					IDColumn id = (IDColumn)form.getMiniTable().getValueAt(i, 0);     //  ID in column 0
					String tbl_docno = form.getMiniTable().getValueAt(i, 4).toString();     //  DocumentNo in column 4
					String sel_docno = fDocumentNo.getValue().toString();
					if (tbl_docno.compareTo(sel_docno)==0)
						{
							id.setSelected(true);
							form.getMiniTable().setValueAt((IDColumn)id, i, 0);
							form.getMiniTable().setSelectedIndex(i);
							form.getMiniTable().setFocus(true);
						}
					if (id.isSelected()) {
						selectedList.add(i);
					}
				}
				int[] selectedArray = new int[selectedList.size()];
				for (Integer selected : selectedList) {
					selectedArray[selectedList.indexOf(selected)] = selected;
				}
				form.getMiniTable().setSelectedIndices(selectedArray);
				fDocumentNo.setValue(null); //refresh field
				return;
			}
		} else if(Events.ON_CHECK.equals(e.getName())) {
			if(fConsolidate.equals(e.getTarget()))
			{
				setIsConsolidate(fConsolidate.isChecked());
				return;
			}
		}
		//
		validate();
	}	//	actionPerformed
	
	public void validate()
	{
		form.saveSelection();
		
		ArrayList<Integer> selection = getSelection();
		
		if (selection != null && selection.size() > 0 && isSelectionActive()){
			Trx.run(new TrxRunnable() {				
				@Override
				public void run(String trxName) {
					String info = generate(trxName);
					FDialog.info(m_WindowNo, null, "ISY-224", "Kwitansi: \n" + info);
					form.dispose();					
				}
			});
		}
	}

	/**
	 *	Value Change Listener - requery
	 *  @param e event
	 */
	public void valueChange(ValueChangeEvent e)
	{
		log.info(e.getPropertyName() + "=" + e.getNewValue());
		Object newValue = e.getNewValue();
		if (e.getSource().equals(fOrg))
			setOrgID(newValue);
		if (e.getSource().equals(fBPartner))
		{
			setBpartnerID(newValue);
			fBPartner.setValue(newValue);	//	display value
		}
		if( e.getSource().equals(fDateInvoiced) ) 
			setDateInvoiced(newValue);		
		if( e.getSource().equals(fDateInvoiced_To) ) 
			setDateInvoicedTo(newValue);	
		if( e.getSource().equals(fDueDate) ) 
			setDueDate(newValue);		
		if( e.getSource().equals(fDueDate_To) ) 
			setDueDateTo(newValue);
		if( e.getSource().equals(fConsolidate) ) {
			setIsConsolidate(newValue);
		}
		if( e.getSource().equals(fCurrency) ) 
			setCurrencyID(newValue);
		if( e.getSource().equals(fDocType) ) 
			setDocTypeID(newValue);
		if( e.getSource().equals(fKwitansi) ) {
			setKwitansiID(newValue);
			if(newValue!=null){
				fConsolidate.setSelected(true);
				setIsConsolidate(true);
			}
		}
		if( e.getSource().equals(fDateDoc) ) 
			setDateDoc(newValue);
		if (e.getSource().equals(fBilling))
		{
			setBillingID(newValue);
			fBilling.setValue(newValue);	//	display value
		}
//		executeQuery();
//		form.postQueryEvent();
	}	//	vetoableChange
	
	/**************************************************************************
	 *	Generate Shipments
	 */
	public String generate()
	{
		return super.generate();
	}	//	generateTaxInvoice
	
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