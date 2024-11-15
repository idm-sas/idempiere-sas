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

import id.co.databiz.awn.form.GenerateTaxInvoice;
import id.co.databiz.awn.model.SystemID;

import java.sql.Timestamp;
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
import org.compiere.model.MColumn;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.util.CLogger;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Space;

/**
 * Generate Invoice (manual) view class
 * 
 */
public class WGenerateTaxInvoice extends GenerateTaxInvoice implements IFormController, EventListener<Event>, ValueChangeListener, WTableModelListener
{
	private WGenForm form;
	private int m_WindowNo;
	
	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(WGenerateTaxInvoice.class);
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
	private Label	lBarcode = new Label();
	private Textbox	fBarcode = new Textbox(); 

	private Checkbox fIsConsolidate = new Checkbox();
	private Label	lIsConsolidate = new Label(); 
	private Label	lCurrency = new Label();
	private WTableDirEditor fCurrency;
	
	private Label	lTaxInvoice = new Label();
	private WSearchEditor fTaxInvoice;
	private Label	lDocumentNo = new Label();
	private Textbox	fDocumentNo = new Textbox(); 
	
	private Checkbox fIsSOTrx = new Checkbox();
	private Label	lIsSOTrx = new Label(); 
	
	private List<String> columnList = new ArrayList<String>();
	
	public WGenerateTaxInvoice()
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
			executeQuery();
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
		columnList.add("ID");
		columnList.add(Msg.translate(Env.getCtx(), "AD_Org_ID"));
		columnList.add(Msg.translate(Env.getCtx(), "C_DocType_ID"));
		columnList.add(Msg.translate(Env.getCtx(), "DocumentNo"));
		columnList.add(Msg.translate(Env.getCtx(), "C_BPartner_ID"));
		columnList.add(Msg.translate(Env.getCtx(), "DateInvoiced"));
		columnList.add(Msg.translate(Env.getCtx(), "TotalLines"));
		
		lOrg.setText(Msg.translate(Env.getCtx(), "AD_Org_ID"));
		lBPartner.setText(Msg.translate(Env.getCtx(), "C_BPartner_ID"));
		lDocType.setText(Msg.translate(Env.getCtx(), "C_DocType_ID"));
		lDateInvoiced.setText(Msg.translate(Env.getCtx(), "DateInvoiced"));
		lDateInvoicedTo.setText(Msg.translate(Env.getCtx(), " - "));
		lDataStatus.setText(Msg.translate(Env.getCtx(), "Total selected"));
		lBarcode.setText(Msg.translate(Env.getCtx(), "Barcode"));
		lIsConsolidate.setText(Msg.translate(Env.getCtx(), "IsConsolidate"));
		lCurrency.setText(Msg.translate(Env.getCtx(), "C_Currency_ID"));
		lTaxInvoice.setText(Msg.translate(Env.getCtx(), "Tax Invoice"));
		lIsSOTrx.setText(Msg.translate(Env.getCtx(), "IsSOTrx"));
		lDocumentNo.setText(Msg.translate(Env.getCtx(), "DocumentNo"));
		
		ZKUpdateUtil.setHflex(fOrg.getComponent(), "true");
		ZKUpdateUtil.setHflex(fBPartner.getComponent(), "true");
		ZKUpdateUtil.setHflex(fDocType.getComponent(), "true");
		ZKUpdateUtil.setHflex(fDateInvoiced.getComponent(), "true");
		ZKUpdateUtil.setHflex(fDateInvoiced_To.getComponent(), "true");
		ZKUpdateUtil.setHflex(fBarcode, "true");
		ZKUpdateUtil.setHflex(fCurrency.getComponent(), "true");
		ZKUpdateUtil.setHflex(fTaxInvoice.getComponent(), "true");
		ZKUpdateUtil.setHflex(fDocumentNo, "true");
		
		lDocumentNo.setVisible(false);
		fDocumentNo.setVisible(false);
		
		// 1st row
		Row row = form.getParameterPanel().newRows().newRow();
		row.appendChild(lOrg.rightAlign());
		row.appendChild(fOrg.getComponent());
//		row.appendChild(new Space());
		row.appendChild(lBPartner.rightAlign());
		row.appendChild(fBPartner.getComponent());
//		row.appendChild(new Space());
		row.appendChild(lDocType.rightAlign());
		row.appendChild(fDocType.getComponent());
		
		// 2nd row
		row = new Row();
		form.getParameterPanel().getRows().appendChild(row);
		row.appendChild(lDateInvoiced.rightAlign());
		row.appendChild(fDateInvoiced.getComponent());
//		row.appendChild(new Space());
//		row.appendChild(lDateInvoicedTo.rightAlign());
		row.appendChild(fDateInvoiced_To.getComponent());
		row.appendChild(new Space());
		row.appendChild(lIsSOTrx.rightAlign());
		row.appendChild(fIsSOTrx);
		
		// 3rd row
		row = new Row();
		form.getParameterPanel().getRows().appendChild(row);
		row.appendChild(lCurrency.rightAlign());
		row.appendChild(fCurrency.getComponent());
//		row.appendChild(new Space());
		row.appendChild(lBarcode.rightAlign());
		row.appendChild(fBarcode);
//		row.appendChild(new Space());
		row.appendChild(lIsConsolidate.rightAlign());
		row.appendChild(fIsConsolidate);
//		row.appendChild(new Space());
		
		// 4th row
		row = new Row();
		form.getParameterPanel().getRows().appendChild(row);
		row.appendChild(lDataStatus.rightAlign());
		row.appendChild(dataStatus);
//		row.appendChild(new Space());
		row.appendChild(lTaxInvoice.rightAlign());
		row.appendChild(fTaxInvoice.getComponent());
//		row.appendChild(new Space());
		row.appendChild(lDocumentNo.rightAlign());
		row.appendChild(fDocumentNo);
		
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
				0,
				MColumn.getColumn_ID(MInvoice.Table_Name,MInvoice.COLUMNNAME_C_DocTypeTarget_ID), 
				DisplayType.Table);
		docTypeLookup = MLookupFactory.get(ctx, 
				m_WindowNo, 
				MColumn.getColumn_ID(MInvoice.Table_Name,MInvoice.COLUMNNAME_C_DocTypeTarget_ID), 
				DisplayType.Table, 
				Env.getLanguage(Env.getCtx()), 
				MInvoice.COLUMNNAME_C_DocType_ID, 
				170, // C_DocType 
				false, 
				"DocBaseType IN ('ARI','API','ARC','APC') AND C_DocTypeTaxInvoice_ID IS NOT NULL");
        lDocType.setText(Msg.translate(Env.getCtx(), "C_DocType_ID"));
        fDocType = new WTableDirEditor("C_DocType_ID", false, false, true, docTypeLookup);               
        
        MLookup currencyLookup = MLookupFactory.get (ctx, 
        		m_WindowNo, 
        		0, 
        		MColumn.getColumn_ID(MInvoice.Table_Name, MInvoice.COLUMNNAME_C_Currency_ID), 
        		DisplayType.Table);
		fCurrency = new WTableDirEditor ("C_Currency_ID", true, false, true, currencyLookup);
		fCurrency.setValue(Env.getContextAsInt(ctx, m_WindowNo, "$C_Currency_ID"));
		setCurrencyID((Integer)fCurrency.getValue());
		
		//Enhance Penggunaan SystemID diganti agar case ID yang berbeda di client tidak error
		
		int COLUMN_INVOICE_Z_TAXINVOICE_ID =  MColumn.getColumn_ID(MInvoice.Table_Name, "Z_TaxInvoice_ID");
		
		MColumn columntaxinvoice = new MColumn(ctx, COLUMN_INVOICE_Z_TAXINVOICE_ID, null);

		MLookup taxInvoiceLookup = MLookupFactory.get(ctx, 
				m_WindowNo, 
//				SystemID.COLUMN_C_INVOICE_Z_TAXINVOICE_IDS, 
				COLUMN_INVOICE_Z_TAXINVOICE_ID,
				DisplayType.Search, 
				Env.getLanguage(ctx), 
				"Z_TaxInvoice_ID", 
//				SystemID.REFERENCE_Z_TAXINVOICES, 
				columntaxinvoice.getAD_Reference_Value_ID(),
				false, 
				"NOT EXISTS(SELECT 1 FROM C_Invoice WHERE C_Invoice.Z_TaxInvoice_ID = Z_TaxInvoice.Z_TaxInvoice_ID)");
		fTaxInvoice = new WSearchEditor("Z_TaxInvoice_ID", false, false, true, taxInvoiceLookup);
		
		fIsConsolidate.setSelected(false);
		setIsConsolidate(false);
		
		fIsSOTrx.setSelected(true);
		setIsSOTrx(true);
		
		fOrg.addValueChangeListener(this);
		fDocType.addValueChangeListener(this);
		fCurrency.addValueChangeListener(this);
		fBPartner.addValueChangeListener(this);
		fDateInvoiced.addValueChangeListener(this);
        fDateInvoiced_To.addValueChangeListener(this);
        fTaxInvoice.addValueChangeListener(this);
        fBarcode.addEventListener(Events.ON_OK, this);
        fIsConsolidate.addActionListener(this);
        fIsSOTrx.addActionListener(this);
        fDocumentNo.addEventListener(Events.ON_BLUR, this);
        form.getMiniTable().getModel().addTableModelListener(this);
        
        form.getStatusBar().setStatusLine("");
	}	//	fillPicks
    
	/**
	 *  Query Info
	 */
	public void executeQuery()
	{
		executeQuery(form.getMiniTable());
		form.getMiniTable().setData(form.getMiniTable().getModel(), columnList);
		form.getMiniTable().setMultiSelection(true);
		form.getMiniTable().setMultiple(true);
		form.getMiniTable().repaint();
		form.getMiniTable().getModel().addTableModelListener(this);
//		form.invalidate();
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
			if(e.getTarget().equals(fBarcode)){
				int rows = form.getMiniTable().getRowCount();
				List<Integer> selectedList = new ArrayList<Integer>();
				for (int i = 0; i < rows; i++)
				{
					IDColumn id = (IDColumn)form.getMiniTable().getValueAt(i, 0);     //  ID in column 0
					String tbl_docno = form.getMiniTable().getValueAt(i, 3).toString();     //  DocumentNo in column 3
					String sel_docno = fBarcode.getValue().toString();
					if (tbl_docno.compareTo(sel_docno)==0)
					{
						id.setSelected(true);
						form.getMiniTable().setValueAt(id, i, 0);
//						form.getMiniTable().repaint();
						form.getMiniTable().setFocus(true);
//						form.invalidate();
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
				fBarcode.setValue(null); //refresh field
				return;
			}
		} else if(Events.ON_CHECK.equals(e.getName())) {
			if(fIsConsolidate.equals(e.getTarget())) {
				setIsConsolidate(fIsConsolidate.isChecked());
				setTaxInvoiceID(0);
				return;
			} else if(fIsSOTrx.equals(e.getTarget())) {
				setIsSOTrx(fIsSOTrx.isChecked());
				setDocTypeID(0);
				executeQuery();
				form.postQueryEvent();
				return;
			}
		} else if(Events.ON_BLUR.equals(e.getName())) {
			if(fDocumentNo.equals(e.getTarget())) {
				if(fDocumentNo.getValue().isEmpty()){
					setDocumentNo(null);
				} else {
					setDocumentNo(fDocumentNo.getValue());
				}
			}
		}
		//
		validate();
	}	//	actionPerformed
	
	public void validate()
	{
		form.saveSelection();
		
//		ArrayList<Integer> selection = getSelection();
//		if (selection != null && selection.size() > 0 && isSelectionActive())
//			form.generate();
//		else
//			form.dispose();
		
		ArrayList<Integer> selection = getSelection();
		
		if (selection != null && selection.size() > 0 && isSelectionActive()){
			FDialog.info(m_WindowNo, null, super.generate());
			form.getMiniTable().clearSelection();
			executeQuery();	
		} else {
			form.dispose();	
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
		if(newValue!=null){
			if (e.getSource().equals(fOrg))
				setOrgID((Integer)newValue);
			if (e.getSource().equals(fBPartner))
			{
				setBpartnerID((Integer)newValue);
			}
			if( e.getSource().equals(fDateInvoiced) ) 
				setDateInvoiced((Timestamp)newValue);
			if( e.getSource().equals(fDateInvoiced_To) ) 
				setDateInvoicedTo((Timestamp)newValue);		
			if( e.getSource().equals(fIsConsolidate) )
				setIsConsolidate(fIsConsolidate.isChecked());
			if( e.getSource().equals(fIsSOTrx) )
				setIsSOTrx(fIsSOTrx.isChecked());
			if( e.getSource().equals(fCurrency) ) 
				setCurrencyID((Integer)newValue);
			if( e.getSource().equals(fDocType) ){
				setDocTypeID((Integer)newValue);
				if(getDocTypeID()!=null){
					MDocType docType = new MDocType(Env.getCtx(), (Integer)getDocTypeID(), null);
					setIsSOTrx(docType.isSOTrx());
					if(docType.getDocBaseType().equals(MDocType.DOCBASETYPE_APInvoice) || docType.getDocBaseType().equals(MDocType.DOCBASETYPE_ARCreditMemo)){
						lDocumentNo.setVisible(true);
						fDocumentNo.setVisible(true);
					} else {
						lDocumentNo.setVisible(false);
						fDocumentNo.setVisible(false);
						setDocumentNo(null);
					}
				}
			}
			if( e.getSource().equals(fTaxInvoice) ) {
				setTaxInvoiceID((Integer)newValue);
				if(newValue!=null){
					setIsConsolidate(true);
				}
			}
			
			executeQuery();
			form.postQueryEvent();
		}
	}	//	vetoableChange
	

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

	public void setOrgID(Object orgID) {
		super.setOrgID(orgID);
		fOrg.setValue(orgID);
	}

	public void setBpartnerID(Object bpartnerID) {
		super.setBpartnerID(bpartnerID);
		fBPartner.setValue(bpartnerID);
	}

	public void setDocTypeID(Object docTypeID) {
		super.setDocTypeID(docTypeID);
		fDocType.setValue(docTypeID);
	}

	public void setDateInvoiced(Object dateInvoiced) {
		super.setDateInvoiced(dateInvoiced);
		fDateInvoiced.setValue(dateInvoiced);
	}

	public void setDateInvoicedTo(Object dateInvoicedTo) {
		super.setDateInvoicedTo(dateInvoicedTo);
		fDateInvoiced_To.setValue(dateInvoicedTo);
	}

	public void setIsSOTrx(Object isSOTrx) {
		super.setIsSOTrx(isSOTrx);
		fIsSOTrx.setChecked((Boolean)isSOTrx);
	}

	public void setIsConsolidate(Object isConsolidate) {
		super.setIsConsolidate(isConsolidate);
		fIsConsolidate.setChecked((Boolean)isConsolidate);
	}

	public void setCurrencyID(Object currencyID) {
		super.setCurrencyID(currencyID);
		fCurrency.setValue(currencyID);
	}

	public void setTaxInvoiceID(Object taxInvoiceID) {
		super.setTaxInvoiceID(taxInvoiceID);
		fTaxInvoice.setValue(taxInvoiceID);
	}	
	
	public void setDocumentNo(Object documentNo) {
		super.setDocumentNo(documentNo);
		fDocumentNo.setValue(documentNo==null?null:documentNo.toString());
	}
}