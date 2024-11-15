/******************************************************************************
 * Copyright (C) 2008 Low Heng Sin                                            *
 * Copyright (C) 2008 Idalica Corporation                                     *
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
package org.adempiere.webui.apps.wf;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.webui.LayoutUtils;
import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.apps.ProcessModalDialog;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.ListHeader;
import org.adempiere.webui.component.ListItem;
import org.adempiere.webui.component.ListModelTable;
import org.adempiere.webui.component.Listbox;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.component.Textbox;
import org.adempiere.webui.component.WListItemRenderer;
import org.adempiere.webui.component.WListbox;
import org.adempiere.webui.component.Window;
import org.adempiere.webui.editor.WSearchEditor;
import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.panel.StatusBarPanel;
import org.adempiere.webui.session.SessionManager;
import org.adempiere.webui.theme.ThemeManager;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.adempiere.webui.window.FDialog;
import org.compiere.model.MColumn;
import org.compiere.model.MDocType;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MQuery;
import org.compiere.model.MRecentItem;
import org.compiere.model.MRefList;
import org.compiere.model.MRole;
import org.compiere.model.MSysConfig;
import org.compiere.model.MUserRoles;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.print.MPrintFormat;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Trx;
import org.compiere.util.ValueNamePair;
import org.compiere.wf.MWFActivity;
import org.compiere.wf.MWFNode;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.North;
import org.zkoss.zul.South;

/**
 * Direct port from WFActivity
 * @author hengsin
 *
 */
public class WWFActivity extends ADForm implements EventListener<Event>
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -8405802852868437716L;
	/**	Window No					*/
	private int         		m_WindowNo = 0;
	/**	Open Activities				*/
	private MWFActivity[] 		m_activities = null;
	/**	Current Activity			*/
	private MWFActivity 		m_activity = null;
	/**	Current Activity			*/
	private int	 				m_index = 0;
	/**	Set Column					*/
	private	MColumn 			m_column = null;
	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(WWFActivity.class);

	//
	private Label lNode = new Label(Msg.translate(Env.getCtx(), "AD_WF_Node_ID"));
	private Textbox fNode = new Textbox();
	private Label lDesctiption = new Label(Msg.translate(Env.getCtx(), "Description"));
	private Textbox fDescription = new Textbox();
	private Label lHelp = new Label(Msg.translate(Env.getCtx(), "Help"));
	private Textbox fHelp = new Textbox();
	private Label lHistory = new Label(Msg.translate(Env.getCtx(), "History"));
	private Html fHistory = new Html();
	private Label lAnswer = new Label(Msg.getMsg(Env.getCtx(), "Answer"));
	private Textbox fAnswerText = new Textbox();
	private Listbox fAnswerList = new Listbox();
	private Button fAnswerButton = new Button();
	private Button bZoom = new Button();
	private Button bPrint = new Button();
	private Label lTextMsg = new Label(Msg.getMsg(Env.getCtx(), "Messages"));
	private Textbox fTextMsg = new Textbox();
	private Button bOK = new Button();
	private WSearchEditor fForward = null;	//	dynInit
	private Label lForward = new Label(Msg.getMsg(Env.getCtx(), "Forward"));
	private Label lOptional = new Label("(" + Msg.translate(Env.getCtx(), "Optional") + ")");
	private StatusBarPanel statusBar = new StatusBarPanel();

	private ListModelTable model = null;
	private WListbox listbox = new WListbox();

	private final static String HISTORY_DIV_START_TAG = "<div style='width: 100%; height: 100px; border: 1px solid #7F9DB9;overflow: scroll;'>";
	public WWFActivity()
	{
		super();
		LayoutUtils.addSclass("workflow-activity-form", this);
	}

    protected void initForm()
    {
        loadActivities();

        fAnswerList.setMold("select");

    	bZoom.setImage(ThemeManager.getThemeResource("images/Zoom16.png"));
    	bOK.setImage(ThemeManager.getThemeResource("images/Ok24.png"));
    	bPrint.setImage(ThemeManager.getThemeResource("images/Print16.png"));

        MLookup lookup = MLookupFactory.get(Env.getCtx(), m_WindowNo,
                0, 10443, DisplayType.Search);
        fForward = new WSearchEditor(lookup, Msg.translate(
                Env.getCtx(), "AD_User_ID"), "", true, false, true);

        init();
        display(-1);
    }

	private void init()
	{
		Grid grid = new Grid();
		ZKUpdateUtil.setWidth(grid, "100%");
		ZKUpdateUtil.setHeight(grid, "100%");
        grid.setStyle("margin:0; padding:0; position: absolute; align: center; valign: center;");
        grid.makeNoStrip();
        grid.setOddRowSclass("even");

		Rows rows = new Rows();
		grid.appendChild(rows);

		Row row = new Row();
		rows.appendChild(row);
		Div div = new Div();
		div.setStyle("text-align: right;");
		div.appendChild(lNode);
		row.appendChild(div);
		row.appendChild(fNode);
		ZKUpdateUtil.setWidth(fNode, "100%");
		ZKUpdateUtil.setHflex(fNode, "true");
		fNode.setReadonly(true);

//		row = new Row();
//		rows.appendChild(row);
//		row.setValign("top");
//		div = new Div();
//		div.setStyle("text-align: right;");
//		div.appendChild(lDesctiption);
//		row.appendChild(div);
//		row.appendChild(fDescription);
//		fDescription.setMultiline(true);
//		ZKUpdateUtil.setWidth(fDescription, "100%");
//		ZKUpdateUtil.setHflex(fDescription, "true");
//		fDescription.setReadonly(true);

//		row = new Row();
//		rows.appendChild(row);
//		div = new Div();
//		div.setStyle("text-align: right;");
//		div.appendChild(lHelp);
//		row.appendChild(div);
//		row.appendChild(fHelp);
//		fHelp.setMultiline(true);
//		fHelp.setRows(3);
//		ZKUpdateUtil.setWidth(fHelp, "100%");
//		ZKUpdateUtil.setHeight(fHelp, "100%");
//		ZKUpdateUtil.setHflex(fHelp, "true");
//		fHelp.setReadonly(true);
//		row.appendChild(new Label());

		row = new Row();
		rows.appendChild(row);
		div = new Div();
		div.setStyle("text-align: right;");
		div.appendChild(lHistory);
		row.appendChild(div);
		row.appendChild(fHistory);
		ZKUpdateUtil.setHflex(fHistory, "true");
		row.appendChild(new Label());

		row = new Row();
		rows.appendChild(row);
		div = new Div();
		div.setStyle("text-align: right;");
		div.appendChild(lAnswer);
		row.appendChild(div);
		Hbox hbox = new Hbox();
		hbox.appendChild(fAnswerText);
		ZKUpdateUtil.setHflex(fAnswerText, "true");
		hbox.appendChild(fAnswerList);
		hbox.appendChild(fAnswerButton);
		fAnswerButton.addEventListener(Events.ON_CLICK, this);
		row.appendChild(hbox);
		row.appendChild(bZoom);
		bZoom.addEventListener(Events.ON_CLICK, this);

		row = new Row();
		rows.appendChild(row);
		div = new Div();
		div.setStyle("text-align: right;");
		div.appendChild(lTextMsg);
		row.appendChild(div);
		row.appendChild(fTextMsg);
		ZKUpdateUtil.setHflex(fTextMsg, "true");
		fTextMsg.setMultiline(true);
		ZKUpdateUtil.setWidth(fTextMsg, "100%");
		row.appendChild(bPrint);
		bPrint.addEventListener(Events.ON_CLICK, this);

		row = new Row();
		rows.appendChild(row);
		div = new Div();
		div.setStyle("text-align: right;");
		div.appendChild(lForward);
		row.appendChild(div);
		hbox = new Hbox();
		hbox.appendChild(fForward.getComponent());
		hbox.appendChild(lOptional);
		row.appendChild(hbox);
		row.appendChild(bOK);
		bOK.addEventListener(Events.ON_CLICK, this);

		Borderlayout layout = new Borderlayout();
		ZKUpdateUtil.setWidth(layout, "100%");
		ZKUpdateUtil.setHeight(layout, "100%");
		layout.setStyle("background-color: transparent; position: absolute;");

		North north = new North();
		north.appendChild(listbox);
		north.setSplittable(true);
		ZKUpdateUtil.setVflex(listbox, "1");
		ZKUpdateUtil.setHflex(listbox, "1");
		ZKUpdateUtil.setHeight(north, "30%");
		layout.appendChild(north);
		north.setStyle("background-color: transparent");
		listbox.addEventListener(Events.ON_SELECT, this);

		Center center = new Center();
		center.appendChild(grid);
		center.setAutoscroll(true);
		layout.appendChild(center);
		center.setStyle("background-color: transparent");
		ZKUpdateUtil.setVflex(grid, "1");
		ZKUpdateUtil.setHflex(grid, "1");

		South south = new South();
		south.appendChild(statusBar);
		layout.appendChild(south);
		south.setStyle("background-color: transparent");

		this.appendChild(layout);
		this.setStyle("height: 100%; width: 100%; position: absolute;");
	}

	public void onEvent(Event event) throws Exception
	{
		Component comp = event.getTarget();
        String eventName = event.getName();

        if(eventName.equals(Events.ON_CLICK))
        {
    		if (comp == bZoom)
    			cmd_zoom();
    		else if (comp == bPrint)
    			cmd_print();
    		else if (comp == bOK)
    		{
    			Clients.showBusy(Msg.getMsg(Env.getCtx(), "Processing"));
    			Events.echoEvent("onOK", this, null);
    		}
    		else if (comp == fAnswerButton)
    			cmd_button();
        } 
        else if (Events.ON_SELECT.equals(eventName) && comp == listbox)
        {
        	m_index = listbox.getSelectedIndex();
        	if (m_index >= 0)
    			display(m_index);
        }
        else
        {
    		super.onEvent(event);
        }
	}

	/**
	 * Get active activities count
	 * @return int
	 */
	public int getActivitiesCount()
	{
		int count = 0;

		String sql = "SELECT COUNT(*) FROM AD_WF_Activity a "
			+ "WHERE " + getWhereActivities()
			//https://databiz.atlassian.net/browse/SABA-676?focusedCommentId=111056
			+ (MSysConfig.getBooleanValue("ISY_FILTER_USER_ORG_ACCESS", false) ? getWhereOrgTrx() : "");
		int AD_User_ID = Env.getAD_User_ID(Env.getCtx());
		int AD_Client_ID = Env.getAD_Client_ID(Env.getCtx());
		MRole role = MRole.get(Env.getCtx(), Env.getAD_Role_ID(Env.getCtx()));
		sql = role.addAccessSQL(sql, "a", true, false);
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement (sql, null);
			pstmt.setInt (1, AD_User_ID);
			pstmt.setInt (2, AD_User_ID);
			pstmt.setInt (3, AD_User_ID);
			pstmt.setInt (4, AD_User_ID);
			pstmt.setInt (5, AD_User_ID);
			pstmt.setInt (6, AD_Client_ID);
			//https://databiz.atlassian.net/browse/SABA-676?focusedCommentId=111056
			if (MSysConfig.getBooleanValue("ISY_FILTER_USER_ORG_ACCESS", false)) {
				pstmt.setInt (7, AD_User_ID);
				pstmt.setInt (8, AD_User_ID);
			}
			
			rs = pstmt.executeQuery ();
			if (rs.next ()) {
				count = rs.getInt(1);
			}
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, sql, e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}

		return count;

	}

	/**
	 * 	Load Activities
	 * 	@return int
	 */
	public int loadActivities()
	{
		long start = System.currentTimeMillis();

		int MAX_ACTIVITIES_IN_LIST = MSysConfig.getIntValue(MSysConfig.MAX_ACTIVITIES_IN_LIST, 200, Env.getAD_Client_ID(Env.getCtx()));

		model = new ListModelTable();

		ArrayList<MWFActivity> list = new ArrayList<MWFActivity>();
		String sql = "SELECT * FROM AD_WF_Activity a "
			+ "WHERE " + getWhereActivities() 
			//https://databiz.atlassian.net/browse/SABA-676?focusedCommentId=111056
			+ (MSysConfig.getBooleanValue("ISY_FILTER_USER_ORG_ACCESS", false) ? getWhereOrgTrx() : "")
			+ " ORDER BY a.Priority DESC, Created";
		int AD_User_ID = Env.getAD_User_ID(Env.getCtx());
		int AD_Client_ID = Env.getAD_Client_ID(Env.getCtx());
		MRole role = MRole.get(Env.getCtx(), Env.getAD_Role_ID(Env.getCtx()));
		sql = role.addAccessSQL(sql, "a", true, false);
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement (sql, null);
			pstmt.setInt (1, AD_User_ID);
			pstmt.setInt (2, AD_User_ID);
			pstmt.setInt (3, AD_User_ID);
			pstmt.setInt (4, AD_User_ID);
			pstmt.setInt (5, AD_User_ID);
			pstmt.setInt (6, AD_Client_ID);
			//https://databiz.atlassian.net/browse/SABA-676?focusedCommentId=111056
			if (MSysConfig.getBooleanValue("ISY_FILTER_USER_ORG_ACCESS", false)) {
				pstmt.setInt (7, AD_User_ID);
				pstmt.setInt (8, AD_User_ID);
			}
            
			rs = pstmt.executeQuery();
			while (rs.next ())
			{
				MWFActivity activity = new MWFActivity(Env.getCtx(), rs, null);
				list.add (activity);
				List<Object> rowData = new ArrayList<Object>();
				rowData.add(activity.getPriority());
				rowData.add(activity.getNodeName());
				rowData.add(activity.getSummary());
				rowData.add(activity.get_ID());
				model.add(rowData);
				if (list.size() > MAX_ACTIVITIES_IN_LIST && MAX_ACTIVITIES_IN_LIST > 0)
				{
					log.warning("More then 200 Activities - ignored");
					break;
				}
			}
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, sql, e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}
		m_activities = new MWFActivity[list.size ()];
		list.toArray (m_activities);
		//
		if (log.isLoggable(Level.FINE)) log.fine("#" + m_activities.length
			+ "(" + (System.currentTimeMillis()-start) + "ms)");
		m_index = 0;

		String[] columns = new String[]{Msg.translate(Env.getCtx(), "Priority"),
				Msg.translate(Env.getCtx(), "AD_WF_Node_ID"),
				Msg.translate(Env.getCtx(), "Summary"), "ID"};

		WListItemRenderer renderer = new WListItemRenderer(Arrays.asList(columns));
		ListHeader header = new ListHeader();
//		header.setWidth("60px");
//		renderer.setListHeader(0, header);
//		header = new ListHeader();
//		ZKUpdateUtil.setWidth(header, null);
//		renderer.setListHeader(1, header);
//		header = new ListHeader();
//		ZKUpdateUtil.setWidth(header, null);
//		renderer.setListHeader(2, header);
		ZKUpdateUtil.setWidth(header, null);
		header = new ListHeader();
		renderer.setListHeader(3, header);
		renderer.addTableValueChangeListener(listbox);
		model.setNoColumns(columns.length);
		listbox.setModel(model);
		listbox.setItemRenderer(renderer);
		listbox.repaint();
		listbox.setSizedByContent(false);

		return m_activities.length;
	}	//	loadActivities

	private String getWhereActivities() {
		final String where =
			"a.Processed='N' AND a.WFState='OS' AND ("
			//	Owner of Activity
			+ " a.AD_User_ID=?"	//	#1
			//	Invoker (if no invoker = all)
			+ " OR EXISTS (SELECT * FROM AD_WF_Responsible r WHERE a.AD_WF_Responsible_ID=r.AD_WF_Responsible_ID"
			+ " AND r.ResponsibleType='H' AND COALESCE(r.AD_User_ID,0)=0 AND COALESCE(r.AD_Role_ID,0)=0 AND (a.AD_User_ID=? OR a.AD_User_ID IS NULL))"	//	#2
			//  Responsible User
			+ " OR EXISTS (SELECT * FROM AD_WF_Responsible r WHERE a.AD_WF_Responsible_ID=r.AD_WF_Responsible_ID"
			+ " AND r.ResponsibleType='H' AND r.AD_User_ID=?)"		//	#3
			//	Responsible Role
			+ " OR EXISTS (SELECT * FROM AD_WF_Responsible r INNER JOIN AD_User_Roles ur ON (r.AD_Role_ID=ur.AD_Role_ID)"
			+ " WHERE a.AD_WF_Responsible_ID=r.AD_WF_Responsible_ID AND r.ResponsibleType='R' AND ur.AD_User_ID=? AND ur.isActive = 'Y')"	//	#4
			///* Manual Responsible */ 
			+ " OR EXISTS (SELECT * FROM AD_WF_ActivityApprover r "
			+ " WHERE a.AD_WF_Activity_ID=r.AD_WF_Activity_ID AND r.AD_User_ID=? AND r.isActive = 'Y')" 
			+ ") AND a.AD_Client_ID=?";	//	#5
		return where;
	}
	
	private String getWhereOrgTrx() {
		//	ISY-346 REF SABA-656
		//	the option can be added to a table with an orgtrx column
		final String where =
				"and exists ( "
				+"select * from ( "
				+ "select aa.ad_wf_activity_id "
				+ ",coalesce (i.ad_orgtrx_id, o.ad_orgtrx_id, pay.ad_orgtrx_id, j.ad_org_id, io.ad_orgtrx_id "
				+ "	, inv.ad_orgtrx_id, mov.ad_orgtrx_id, r.ad_orgtrx_id, rm.ad_org_id, st.ad_orgtrx_id,0) "
				+ "	as ad_orgtrx_id "
				+ ",coalesce (i.z_wfscenario_id, o.z_wfscenario_id, pay.z_wfscenario_id, j.z_wfscenario_id "
				+ "	, io.z_wfscenario_id, inv.z_wfscenario_id, mov.z_wfscenario_id, r.z_wfscenario_id "
				+ "	, rm.z_wfscenario_id, st.z_wfscenario_id,0) "
				+ " as z_wfscenario_id "
				+ "from ad_wf_activity aa "
				+ "left join c_invoice i on aa.record_id = i.c_invoice_id and aa.ad_table_id = 318 "
				+ "left join c_order o on aa.record_id = o.c_order_id and aa.ad_table_id = 259 "
				+ "left join c_payment pay on aa.record_id = pay.c_payment_id and aa.ad_table_id = 335 "
				+ "left join gl_journal j on aa.record_id = j.gl_journal_id and aa.ad_table_id = 224 "
				+ "left join m_inout io on aa.record_id = io.m_inout_id and aa.ad_table_id = 319 "
				+ "left join m_inventory inv on aa.record_id = inv.m_inventory_id and aa.ad_table_id = 321 "
				+ "left join m_movement mov on aa.record_id = mov.m_movement_id and aa.ad_table_id = 323 "
				+ "left join m_requisition r on aa.record_id = r.m_requisition_id and aa.ad_table_id = 702 "
				+ "left join m_rma rm on aa.record_id = rm.m_rma_id and aa.ad_table_id = 661 "
				+ "left join s_timeexpense st on aa.record_id = st.s_timeexpense_id and aa.ad_table_id = 486 "
				+ "		) aa "
				+ "		join z_wfscenarioline wfl on wfl.z_wfscenario_id = aa.z_wfscenario_id "
				+ "		join ad_wf_responsible wr on wr.ad_wf_responsible_id = wfl.ad_wf_responsible_id "
				+ "		join ad_user_roles ur on ur.ad_role_id = wr.ad_role_id "
				+ "		where aa.ad_orgtrx_id in (select ad_org_id from AD_User_OrgAccess where isactive = 'Y' and ad_user_id = ?) "
				+ "		and aa.ad_wf_activity_id = a.ad_wf_activity_id and ur.AD_User_ID=? AND ur.isActive = 'Y' "
				+ "		)";
			return where;
		}

	/**
	 * 	Reset Display
	 *	@param selIndex select index
	 *	@return selected activity
	 */
	private MWFActivity resetDisplay(int selIndex)
	{
		fAnswerText.setVisible(false);
		fAnswerList.setVisible(false);
		fAnswerButton.setVisible(false);
		fAnswerButton.setImage(ThemeManager.getThemeResource("images/mWindow.png"));
		fTextMsg.setReadonly(!(selIndex >= 0));
		bZoom.setEnabled(selIndex >= 0);
		bPrint.setEnabled(selIndex >= 0);
		bOK.setEnabled(selIndex >= 0);
		fForward.setValue(null);
		fForward.setReadWrite(selIndex >= 0);
		//
		statusBar.setStatusDB(String.valueOf(selIndex+1) + "/" + m_activities.length);
		m_activity = null;
		m_column = null;
		if (m_activities.length > 0)
		{
			if (selIndex >= 0 && selIndex < m_activities.length){
//				m_activity = m_activities[selIndex];
				Integer activityID = (Integer) model.getValueAt(selIndex, 3);
				if (activityID != null && activityID > 0) {
					m_activity = new MWFActivity(Env.getCtx(), activityID, null);
				}
			}
		}
		//	Nothing to show
		if (m_activity == null)
		{
			fNode.setText ("");
			fDescription.setText ("");
			fHelp.setText ("");
			fHistory.setContent(HISTORY_DIV_START_TAG + "&nbsp;</div>");
			statusBar.setStatusDB("0/0");
			statusBar.setStatusLine(Msg.getMsg(Env.getCtx(), "WFNoActivities"));
		}
		return m_activity;
	}	//	resetDisplay

	/**
	 * 	Display.
	 * 	Fill Editors
	 */
	public void display (int index)
	{
		if (log.isLoggable(Level.FINE)) log.fine("Index=" + index);
		//
		m_activity = resetDisplay(index);
		//	Nothing to show
		if (m_activity == null)
		{
			return;
		}
		//	Display Activity
		fNode.setText (m_activity.getNodeName());
		fDescription.setValue (m_activity.getNodeDescription());
		fHelp.setValue (m_activity.getNodeHelp());
		//
		fHistory.setContent (HISTORY_DIV_START_TAG+m_activity.getHistoryHTML()+"</div>");

		//	User Actions
		MWFNode node = m_activity.getNode();
		if (MWFNode.ACTION_UserChoice.equals(node.getAction()))
		{
			if (m_column == null)
				m_column = node.getColumn();
			if (m_column != null && m_column.get_ID() != 0)
			{
				fAnswerList.removeAllItems();
				int dt = m_column.getAD_Reference_ID();
				if (dt == DisplayType.YesNo)
				{
					ValueNamePair[] values = MRefList.getList(Env.getCtx(), 319, false);		//	_YesNo
					for(int i = 0; i < values.length; i++)
					{
						fAnswerList.appendItem(values[i].getName(), values[i].getValue());
					}
					fAnswerList.setVisible(true);
				}
				else if (dt == DisplayType.List)
				{
					ValueNamePair[] values = MRefList.getList(Env.getCtx(), m_column.getAD_Reference_Value_ID(), false);
					for(int i = 0; i < values.length; i++)
					{
						fAnswerList.appendItem(values[i].getName(), values[i].getValue());
					}
					fAnswerList.setVisible(true);
				}
				else	//	other display types come here
				{
					fAnswerText.setText ("");
					fAnswerText.setVisible(true);
				}
			}
		}
		//	--
		else if (MWFNode.ACTION_UserWindow.equals(node.getAction())
			|| MWFNode.ACTION_UserForm.equals(node.getAction())
			|| MWFNode.ACTION_UserInfo.equals(node.getAction()))
		{
			fAnswerButton.setLabel(node.getName());
			fAnswerButton.setTooltiptext(node.getDescription());
			fAnswerButton.setVisible(true);
		}
		else
			log.log(Level.SEVERE, "Unknown Node Action: " + node.getAction());

		statusBar.setStatusDB((m_index+1) + "/" + m_activities.length);
		statusBar.setStatusLine(Msg.getMsg(Env.getCtx(), "WFActivities"));
	}	//	display


	/**
	 * 	Zoom
	 */
	private void cmd_zoom()
	{
		if (log.isLoggable(Level.CONFIG)) log.config("Activity=" + m_activity);
		if (m_activity == null)
			return;
		AEnv.zoom(m_activity.getAD_Table_ID(), m_activity.getRecord_ID());
	}	//	cmd_zoom
	
	/**
	 * 	Zoom
	 */
	private void cmd_print()
	{
		if (log.isLoggable(Level.CONFIG)) log.config("Activity=" + m_activity);
		if (m_activity == null)
			return;
		int table_ID = m_activity.getAD_Table_ID();
		int record_ID = m_activity.getRecord_ID();
		int AD_Process_ID = -1;
		PO doc =  m_activity.getPO();
		int docType_ID = doc.get_ColumnIndex("C_DocTypeTarget_ID") > 0 ? doc.get_ValueAsInt("C_DocTypeTarget_ID") : doc.get_ValueAsInt("C_DocType_ID");
		
		if(docType_ID > 0){
			MDocType docType = new MDocType(Env.getCtx(), docType_ID, null);
			int printFormat_ID = docType.getAD_PrintFormat().getAD_PrintFormat_ID();
			if(printFormat_ID > 0){
				MPrintFormat pf = new MPrintFormat(Env.getCtx(), printFormat_ID, null);
				AD_Process_ID = pf.getJasperProcess_ID();
			}
		}
		
		if(AD_Process_ID <= 0){
			MRecentItem recentItem = new Query(Env.getCtx(), MRecentItem.Table_Name, "AD_Table_ID = ? AND Record_ID = ?", null)
					.setParameters(new Object[]{table_ID, record_ID})
					.setOrderBy(MRecentItem.COLUMNNAME_Updated + " DESC")
					.first();
			if(recentItem!=null){
				AD_Process_ID = recentItem.getAD_Tab().getAD_Process_ID();
			}
		}
		
		if(AD_Process_ID>0){
			ProcessModalDialog dialog = new ProcessModalDialog(this, getWindowNo(), AD_Process_ID,table_ID, record_ID, true);
			if (dialog.isValid()) {
				dialog.setWidth("500px");
				dialog.setBorder("normal");						
				getParent().appendChild(dialog);
				LayoutUtils.openOverlappedWindow(this, dialog, "middle_center");
				dialog.focus();
			}
		}
			
	}	//	cmd_print

	/**
	 * 	Answer Button
	 */
	private void cmd_button()
	{
		if (log.isLoggable(Level.CONFIG)) log.config("Activity=" + m_activity);
		if (m_activity == null)
			return;
		//
		MWFNode node = m_activity.getNode();
		if (MWFNode.ACTION_UserWindow.equals(node.getAction()))
		{
			int AD_Window_ID = node.getAD_Window_ID();		// Explicit Window
			String ColumnName = m_activity.getPO().get_TableName() + "_ID";
			int Record_ID = m_activity.getRecord_ID();
			MQuery query = MQuery.getEqualQuery(ColumnName, Record_ID);
			boolean IsSOTrx = m_activity.isSOTrx();
			//
			log.info("Zoom to AD_Window_ID=" + AD_Window_ID
				+ " - " + query + " (IsSOTrx=" + IsSOTrx + ")");

			AEnv.zoom(AD_Window_ID, query);
		}
		else if (MWFNode.ACTION_UserForm.equals(node.getAction()))
		{
			int AD_Form_ID = node.getAD_Form_ID();

			ADForm form = ADForm.openForm(AD_Form_ID);
			form.setAttribute(Window.MODE_KEY, form.getWindowMode());
			AEnv.showWindow(form);
		}else if (MWFNode.ACTION_UserInfo.equals(node.getAction())){
			SessionManager.getAppDesktop().openInfo(node.getAD_InfoWindow_ID());
		}
		else
			log.log(Level.SEVERE, "No User Action:" + node.getAction());
	}	//	cmd_button


	/**
	 * 	Save
	 */
	public void onOK()
	{
		if (log.isLoggable(Level.CONFIG)) log.config("Activity=" + m_activity);
		if (m_activity == null)
		{
			Clients.clearBusy();
			return;
		}
		int AD_User_ID = Env.getAD_User_ID(Env.getCtx());
		String textMsg = fTextMsg.getValue();
		//
		MWFNode node = m_activity.getNode();

		Object forward = fForward.getValue();

		// ensure activity is ran within a transaction - [ 1953628 ]
		Trx trx = null;
		try {
			trx = Trx.get(Trx.createTrxName("FWFA"), true);
			trx.setDisplayName(getClass().getName()+"_onOK");
			m_activity.set_TrxName(trx.getTrxName());

			if (forward != null)
			{
				if (log.isLoggable(Level.CONFIG)) log.config("Forward to " + forward);
				int fw = ((Integer)forward).intValue();
				if (fw == AD_User_ID || fw == 0)
				{
					log.log(Level.SEVERE, "Forward User=" + fw);
					trx.rollback();
					trx.close();
					return;
				}
				if (!m_activity.forwardTo(fw, textMsg))
				{
					FDialog.error(m_WindowNo, this, "CannotForward");
					trx.rollback();
					trx.close();
					return;
				}
			}
			//	User Choice - Answer
			else if (MWFNode.ACTION_UserChoice.equals(node.getAction()))
			{
				if (m_column == null)
					m_column = node.getColumn();
				//	Do we have an answer?
				int dt = m_column.getAD_Reference_ID();
				String value = fAnswerText.getText();
				if (dt == DisplayType.YesNo || dt == DisplayType.List)
				{
					ListItem li = fAnswerList.getSelectedItem();
					if(li != null) value = li.getValue().toString();
				}
				if (value == null || value.length() == 0)
				{
					FDialog.error(m_WindowNo, this, "FillMandatory", Msg.getMsg(Env.getCtx(), "Answer"));
					trx.rollback();
					trx.close();
					return;
				}
				//
				if (log.isLoggable(Level.CONFIG)) log.config("Answer=" + value + " - " + textMsg);
				try
				{
//					{-> 201108230840 COMPARE WFResposible 1st ~dar~
					boolean validApproval = true;
					int actUserID = m_activity.getResponsible().getAD_User_ID();
					int actRoleID = m_activity.getResponsible().getAD_Role_ID();
					
					if( actUserID > 0 && actUserID != AD_User_ID ) validApproval = false;
					else if( actRoleID > 0 ) {
						MUserRoles[] urs = MUserRoles.getOfRole(Env.getCtx(), 
								actRoleID);
						boolean exists = false;
						for (int i = 0; i < urs.length; i++)
						{
							if( urs[i].getAD_User_ID() > 0 
									&& urs[i].getAD_User_ID() == AD_User_ID ) {
								exists = true;
								break;
							}
						}
						if( !exists ) validApproval = false;
					}
					if( validApproval )
					//	}<- 201108230840 COMPARE WFResposible 1st ~dar~
					m_activity.setUserChoice(AD_User_ID, value, dt, textMsg);
				}
				catch (Exception e)
				{
					log.log(Level.SEVERE, node.getName(), e);
					FDialog.error(m_WindowNo, this, "Error", e.toString());
					trx.rollback();
					trx.close();
					return;
				}
			}
			//	User Action
			else
			{
				if (log.isLoggable(Level.CONFIG)) log.config("Action=" + node.getAction() + " - " + textMsg);
				try
				{
					// ensure activity is ran within a transaction
					m_activity.setUserConfirmation(AD_User_ID, textMsg);
				}
				catch (Exception e)
				{
					log.log(Level.SEVERE, node.getName(), e);
					FDialog.error(m_WindowNo, this, "Error", e.toString());
					trx.rollback();
					trx.close();
					return;
				}

			}

			trx.commit();
		}
		finally
		{
			Clients.clearBusy();
			if (trx != null)
				trx.close();
		}

		//	Next
		loadActivities();
		display(-1);
	}	//	onOK
}
