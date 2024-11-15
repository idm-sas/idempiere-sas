
package id.co.databiz.awn.ui.zk.panel;

import id.co.databiz.awn.model.MTaxInvoice;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.adempiere.webui.component.Borderlayout;
import org.adempiere.webui.component.Checkbox;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.GridFactory;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.component.Textbox;
import org.adempiere.webui.editor.WDateEditor;
import org.adempiere.webui.editor.WSearchEditor;
import org.adempiere.webui.editor.WTableDirEditor;
import org.adempiere.webui.panel.InfoPanel;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.compiere.minigrid.ColumnInfo;
import org.compiere.minigrid.IDColumn;
import org.compiere.model.MColumn;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
import org.zkoss.zul.North;
import org.zkoss.zul.Separator;
import org.zkoss.zul.South;
import org.zkoss.zul.Vbox;

/**
 *  Info Tax Invoice
 *
 *  @author Anozi Mada
 */
public class InfoTaxInvoicePanel extends InfoPanel
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 3687140032475146445L;

	public InfoTaxInvoicePanel(int WindowNo, String value,
            boolean multiSelection, String whereClause)
    {
    	this(WindowNo, value, multiSelection, whereClause, true);
    }
	
	/**
	 *  Detail Protected Contructor
	 *  @param frame parent frame
	 *  @param modal modal
	 *  @param WindowNo window no
	 *  @param value query value
	 *  @param multiSelection multiple selections
	 *  @param whereClause where clause
	 */
	public InfoTaxInvoicePanel(int WindowNo, String value,
		boolean multiSelection, String whereClause, boolean lookup)
	{
		super (WindowNo, "ti", "Z_TaxInvoice_ID", multiSelection, whereClause, lookup);
		m_WindowNo = WindowNo;
		log.info( "Tax Invoice");
		setTitle(Msg.getMsg(Env.getCtx(), "Tax Invoice"));
		//
		try
		{
			initComponents();
	        init();
	        p_loadedOK = initInfo ();
		}
		catch (Exception e)
		{
			return;
		}
		//
		int no = contentPanel.getRowCount();
		setStatusLine(Integer.toString(no) + " " + Msg.getMsg(Env.getCtx(), "SearchRows_EnterQuery"), false);
		setStatusDB(Integer.toString(no));
		if (value != null && value.length() > 0)
		{
			fDocumentNo.setValue(value);
			executeQuery();
			renderItems();
		}
	}   //  InfoTaxInvoice
	
	int m_WindowNo = 0;

	//  Static Info
	private Label lDocumentNo;
	private Textbox fDocumentNo;
	//
	private Label lDateInvoiced;
	private WDateEditor fDateInvoiced;
	private Label lDateInvoicedTo;
	private WDateEditor fDateInvoicedTo;
	
	private Label lIsSOTrx;
	private Checkbox fIsSOTrx;
	private Label lCurrency;
	private WTableDirEditor fCurrency;
	private Label lBPartner;
	private WSearchEditor fBPartner;
	
	private Borderlayout layout;
	private Vbox southBody;

	/**  Array of Column Info    */
	private static final ColumnInfo[] s_invoiceLayout = {
		new ColumnInfo(" ", "ti.Z_TaxInvoice_ID", IDColumn.class),
		new ColumnInfo(Msg.translate(Env.getCtx(), "DocumentNo"), "ti.DocumentNo", String.class),
		new ColumnInfo(Msg.translate(Env.getCtx(), "C_DocType_ID"), "(SELECT Name FROM C_DocType dt WHERE dt.C_DocType_ID = ti.C_DocType_ID)", String.class),		
		new ColumnInfo(Msg.translate(Env.getCtx(), "DateInvoiced"), "ti.DateInvoiced", Timestamp.class),
		new ColumnInfo(Msg.translate(Env.getCtx(), "IsSOTrx"), "ti.IsSOTrx", Boolean.class),
		new ColumnInfo(Msg.translate(Env.getCtx(), "C_Currency_ID"), "parse('C_Currency',ti.C_Currency_ID,0)", String.class),
		new ColumnInfo(Msg.translate(Env.getCtx(), "C_BPartner_ID"), "parse('C_BPartner',ti.C_BPartner_ID,1)", String.class)
	};

	public void initComponents(){
		lDocumentNo = new Label(Msg.translate(Env.getCtx(), "DocumentNo"));
		lDateInvoiced = new Label(Msg.translate(Env.getCtx(), "DateInvoiced"));
		lDateInvoicedTo = new Label("-");
		lIsSOTrx = new Label(Msg.translate(Env.getCtx(), "IsSOTrx"));
		lCurrency = new Label(Msg.translate(Env.getCtx(), "C_Currency_ID"));
		lBPartner = new Label(Msg.translate(Env.getCtx(), "C_BPartner_ID"));
		
		fDocumentNo = new Textbox();
		fDateInvoiced = new WDateEditor();
		fDateInvoicedTo = new WDateEditor();
		fIsSOTrx = new Checkbox();
		fIsSOTrx.setChecked(true);
		
		MLookup currencyLookup = MLookupFactory.get (Env.getCtx(), 
				m_WindowNo, 
				0, 
				MColumn.getColumn_ID(MTaxInvoice.Table_Name, "C_Currency_ID"), 
				DisplayType.TableDir);
		fCurrency = new WTableDirEditor ("C_Currency_ID", false, false, true, currencyLookup);
		
		MLookup bpartnerLookup = MLookupFactory.get (Env.getCtx(), 
				m_WindowNo, 
				0, 
				MColumn.getColumn_ID(MTaxInvoice.Table_Name, "C_BPartner_ID"), 
				DisplayType.Search);
		fBPartner = new WSearchEditor ("C_BPartner_ID", false, false, true, bpartnerLookup);
		
		ZKUpdateUtil.setHflex(fDocumentNo, "true");
		ZKUpdateUtil.setHflex(fDateInvoiced.getComponent(), "true");
		ZKUpdateUtil.setHflex(fDateInvoicedTo.getComponent(), "true");
		ZKUpdateUtil.setHflex(fIsSOTrx, "true");
		ZKUpdateUtil.setHflex(fCurrency.getComponent(), "true");
		ZKUpdateUtil.setHflex(fBPartner.getComponent(), "true");
	}
	/**
	 *	Static Setup - add fields to parameterPanel
	 *  @throws Exception if Lookups cannot be initialized
	 */
	public void init() throws Exception
	{
		Grid grid = GridFactory.newGridLayout();
		
		Rows rows = new Rows();
		grid.appendChild(rows);
		
		Row row = new Row();
		rows.appendChild(row);
		row.appendChild(lDocumentNo.rightAlign());
		row.appendChild(fDocumentNo);
		row.appendChild(lDateInvoiced.rightAlign());
		row.appendChild(fDateInvoiced.getComponent());
		row.appendChild(fDateInvoicedTo.getComponent());
		
		row = new Row();
		rows.appendChild(row);		
		row.appendChild(lCurrency.rightAlign());
		row.appendChild(fCurrency.getComponent());
		row.appendChild(lBPartner.rightAlign());
		row.appendChild(fBPartner.getComponent());
		row.appendChild(lIsSOTrx.rightAlign());
		row.appendChild(fIsSOTrx);
			
		layout = new Borderlayout();
		ZKUpdateUtil.setWidth(layout, "100%");
		ZKUpdateUtil.setHeight(layout, "100%");
        if (!isLookup())
        {
        	layout.setStyle("position: absolute");
        }
        this.appendChild(layout);

        North north = new North();
        layout.appendChild(north);
        ZKUpdateUtil.setHflex(north, "true");
		north.appendChild(grid);

        Center center = new Center();
		layout.appendChild(center);
		ZKUpdateUtil.setHflex(center, "true");
		ZKUpdateUtil.setVflex(center, "true");
		Div div = new Div();
		div.appendChild(contentPanel);
		if (isLookup())
			ZKUpdateUtil.setWidth(contentPanel, "99%");
        else
        	contentPanel.setStyle("width: 99%; margin: 0px auto;");
        contentPanel.setVflex(true);
		div.setStyle("width :100%; height: 100%");
		center.appendChild(div);
        
		South south = new South();
		layout.appendChild(south);
		southBody = new Vbox();
		ZKUpdateUtil.setWidth(southBody, "100%");
		south.appendChild(southBody);
		southBody.appendChild(confirmPanel);
		southBody.appendChild(new Separator());
		southBody.appendChild(statusBar);
	}	//	statInit

	/**
	 *	General Init
	 *	@return true, if success
	 */
	private boolean initInfo ()
	{		

		//  prepare table
		StringBuffer where = new StringBuffer("ti.IsActive='Y'");
		if (p_whereClause.length() > 0)
			where.append(" AND ").append(Util.replace(p_whereClause, "Z_TaxInvoice.", "ti."));
		prepareTable(s_invoiceLayout,
			" Z_TaxInvoice ti",
			where.toString(),
			"ti.DocumentNo");

		return true;
	}	//	initInfo

	
	/**************************************************************************
	 *	Construct SQL Where Clause and define parameters.
	 *  (setParameters needs to set parameters)
	 *  Includes first AND
	 *  @return sql
	 */
	protected String getSQLWhere()
	{
		StringBuffer sql = new StringBuffer();
		if (fDocumentNo.getText().length() > 0)
			sql.append(" AND UPPER(ti.DocumentNo) LIKE ?");
		//
		if (fDateInvoiced.getValue() != null || fDateInvoicedTo.getValue() != null)
		{
			Timestamp from = (Timestamp)fDateInvoiced.getValue();
			Timestamp to = (Timestamp)fDateInvoicedTo.getValue();
			if (from == null && to != null)
				sql.append(" AND TRUNC(ti.DateInvoiced, 'DD') <= ?");
			else if (from != null && to == null)
				sql.append(" AND TRUNC(ti.DateInvoiced, 'DD') >= ?");
			else if (from != null && to != null)
				sql.append(" AND TRUNC(ti.DateInvoiced, 'DD') BETWEEN ? AND ?");
		}
		
		if(fCurrency.getValue()!=null){
			sql.append(" AND ti.C_Currency_ID = ?");
		}
		
		if(fBPartner.getValue()!=null){
			sql.append(" AND ti.C_BPartner_ID = ?");
		}
		
		sql.append(" AND ti.IsSOTrx = ?");

		log.finer(sql.toString());
		return sql.toString();
	}	//	getSQLWhere

	/**
	 *  Set Parameters for Query.
	 *  (as defined in getSQLWhere)
	 *  @param pstmt statement
	 *  @param forCount for counting records
	 *  @throws SQLException
	 */
	protected void setParameters(PreparedStatement pstmt, boolean forCount) throws SQLException
	{
		int index = 1;
		if (fDocumentNo.getText().length() > 0)
			pstmt.setString(index++, getSQLText(fDocumentNo));		
		//
		if (fDateInvoiced.getValue() != null || fDateInvoicedTo.getValue() != null)
		{
			Timestamp from = (Timestamp)fDateInvoiced.getValue();
			Timestamp to = (Timestamp)fDateInvoicedTo.getValue();
			log.fine("Date From=" + from + ", To=" + to);
			if (from == null && to != null)
				pstmt.setTimestamp(index++, to);
			else if (from != null && to == null)
				pstmt.setTimestamp(index++, from);
			else if (from != null && to != null)
			{
				pstmt.setTimestamp(index++, from);
				pstmt.setTimestamp(index++, to);
			}
		}
		
		if(fCurrency.getValue()!=null){
			pstmt.setInt(index++, (Integer) fCurrency.getValue());
		}
		
		if(fBPartner.getValue()!=null){
			pstmt.setInt(index++, (Integer) fBPartner.getValue());
		}
		
		pstmt.setString(index++,fIsSOTrx.isChecked() ? "Y" : "N");
		
	}   //  setParameters

	/**
	 *  Get SQL WHERE parameter
	 *  @param f field
	 *  @return sql
	 */
	private String getSQLText (Textbox f)
	{
		String s = f.getText().toUpperCase();
		if (!s.endsWith("%"))
			s += "%";
		log.fine("String=" + s);
		return s;
	}   //  getSQLText

	/**
	 *	Has Zoom
	 *  @return true
	 */
	protected boolean hasZoom()
	{
		return true;
	}	//	hasZoom
	
}   //  InfoOrder
