/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
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
 * Copyright (C) 2003-2007 e-Evolution,SC. All Rights Reserved.               *
 * Contributor(s): Victor Perez www.e-evolution.com                           *
 *                 Teo Sarca, www.arhipac.ro                                  *
 *****************************************************************************/

package org.eevolution.model;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.I_AD_WF_Node;
import org.compiere.model.I_S_Resource;
import org.compiere.model.MResource;
import org.compiere.model.MResourceAssignment;
import org.compiere.model.MResourceType;
import org.compiere.model.MSysConfig;
import org.compiere.model.Query;
import org.compiere.model.X_AD_Workflow;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;
import org.libero.exceptions.CRPException;
/**
 * Capacity Requirement Planning
 * 
 * @author Gunther Hoppe, tranSIT GmbH Ilmenau/Germany (Original by Victor Perez, e-Evolution, S.C.)
 * @version 1.0, October 14th 2005
 * 
 * @author Teo Sarca, www.arhipac.ro
 */
public class CRP extends SvrProcess
{
	public static final String FORWARD_SCHEDULING = "F";
	public static final String BACKWARD_SCHEDULING = "B";

	private int p_S_Resource_ID;        
	private String p_ScheduleType;
	
	/** SysConfig parameter - maximum number of algorithm iterations */ 
	private int p_MaxIterationsNo = -1;
	public static final String SYSCONFIG_MaxIterationsNo = "CRP.MaxIterationsNo";
	public static final int DEFAULT_MaxIterationsNo = 1000;
	
	public RoutingService routingService = null;
	
	/** CRP Reasoner */
	private CRPReasoner reasoner;
	
	//-->FERRY local variable
	int mTotalPPOrder;
	int mSkippedPPOrder;
	int mProcessedPPOrder;
	//<--

	protected void prepare()
	{
		for (ProcessInfoParameter para : getParameter())
		{
			String name = para.getParameterName();
			if (para.getParameter() == null)
				;			
			if (name.equals("S_Resource_ID")) {
				p_S_Resource_ID = para.getParameterAsInt();
			}
			else if (name.equals("ScheduleType")) {
				p_ScheduleType = (String)para.getParameter();				 		
			}
			else {
				log.log(Level.SEVERE, "prepare - Unknown Parameter: " + name);
			}
		}
		//
		p_MaxIterationsNo = MSysConfig.getIntValue(SYSCONFIG_MaxIterationsNo, DEFAULT_MaxIterationsNo, getAD_Client_ID());
	}

	protected String doIt() throws Exception
	{
		reasoner = new CRPReasoner();
		routingService = RoutingServiceFactory.get().getRoutingService(getAD_Client_ID());
		return runCRP();
	} 

	private String runCRP()
	{
		//-->FERRY
		mTotalPPOrder = 0;
		mSkippedPPOrder = 0;
		mProcessedPPOrder = 0;		
		//<--
		Iterator<MPPOrder> it = reasoner.getPPOrdersNotCompletedQuery(p_S_Resource_ID, get_TrxName()).iterate();
		while(it.hasNext())
		{
			MPPOrder order = it.next();
			try
			{
				runCRP(order);
			}
			catch (Exception e)
			{
				CRPException crpEx;
				if (e instanceof CRPException)
				{
					crpEx = (CRPException)e;
					crpEx.setPP_Order(order);
					throw crpEx;
				}
				else
				{
					crpEx = new CRPException(e);
				}
				throw crpEx;
			}
		}

		return "Total Orders: " + Integer.toString(mTotalPPOrder) + 
			   " Processed: " + Integer.toString(mProcessedPPOrder) +
			   " Skip: " + Integer.toString(mSkippedPPOrder);			//FERRY return "OK";
	}
	
	public void runCRP(MPPOrder order)
	{
		mTotalPPOrder += 1; //FERRY
		log.info("PP_Order DocumentNo:" + order.getDocumentNo());
		MPPOrderWorkflow owf = order.getMPPOrderWorkflow();
		if (owf == null)
		{
			// TODO: generate notice
			addLog("WARNING: No workflow found - "+order);
			return;
		}
		log.info("PP_Order Workflow:" + owf.getName());
		
		final ArrayList<Integer> visitedNodes = new ArrayList<Integer>();

		//-->FERRY check MRP for to create/update resource assignment
		String whereClause = MPPMRP.COLUMNNAME_PP_Order_ID+"=? AND AD_Client_ID=? AND OrderType = ?";
		String m_name;
		RoutingService routingService = RoutingServiceFactory.get().getRoutingService(getCtx());
		MPPMRP mrp = new Query(getCtx(), MPPMRP.Table_Name, whereClause, get_TrxName())
				.setParameters(new Object[]{order.get_ID(), order.getAD_Client_ID(),MPPMRP.ORDERTYPE_SalesOrder})
				.firstOnly();
		if (mrp == null) {
			//MO created manually
			whereClause = whereClause + " AND " + MPPMRP.COLUMNNAME_TypeMRP + "=?";
			mrp = new Query(getCtx(), MPPMRP.Table_Name, whereClause, get_TrxName())
					.setParameters(new Object[]{order.get_ID(), order.getAD_Client_ID(), MPPMRP.ORDERTYPE_ManufacturingOrder, MPPMRP.TYPEMRP_Supply})
					.firstOnly();			
		}
		
		if (mrp == null) {
			log.info("MRP Order of PP Order " + order.getDocumentNo() + " not found !!!");
			mSkippedPPOrder += 1;
			return;
		}
		
		mProcessedPPOrder += 1;
		//<--
		
		// Schedule Fordward
		if (p_ScheduleType.equals(FORWARD_SCHEDULING))
		{
			Timestamp date = order.getDateStartSchedule(); 
			int nodeId = owf.getPP_Order_Node_ID();
			MPPOrderNode node = null;

			while(nodeId != 0)
			{
				node = owf.getNode(nodeId);
				if (visitedNodes.contains(nodeId))
				{
					throw new CRPException("Cyclic transition found").setPP_Order_Node(node);
				}
				visitedNodes.add(nodeId);
				log.info("PP_Order Node:" + node.getName() != null ? node.getName() : ""  + " Description:" + node.getDescription() != null ? node.getDescription() : "");
				//
				MResource resource = MResource.get(getCtx(), node.getS_Resource_ID());
				
				// Skip this node if there is no resource
				if(resource == null)
				{						
					nodeId = owf.getNext(nodeId, getAD_Client_ID());
					continue;
				}
				
				if(!reasoner.isAvailable(resource))
				{
					throw new CRPException("@ResourceNotInSlotDay@").setS_Resource(resource);
				}

				long nodeMillis = routingService.calculateMillisFor(node, owf.getDurationBaseSec());
				Timestamp dateFinish = scheduleForward(date, nodeMillis ,resource);

				node.setDateStartSchedule(date);
				node.setDateFinishSchedule(dateFinish);	
				node.saveEx();
				
				//-->FERRY, create resource assignment
				BigDecimal duration = BigDecimal.valueOf(nodeMillis * 1000 * 60); //in minutes		
				MResourceAssignment ra = routingService.createResourceAssign(mrp, getCtx(), duration, node.getAD_WF_Node(), date, dateFinish);
				//<--				

				date = node.getDateFinishSchedule();
				nodeId = owf.getNext(nodeId, getAD_Client_ID());
			}
			// Update order finish date
			if (node != null && node.getDateFinishSchedule()!= null)
			{
				order.setDateFinishSchedule(node.getDateFinishSchedule());
			}
		}
		// Schedule backward
		else if (p_ScheduleType.equals(BACKWARD_SCHEDULING))
		{
			Timestamp date = order.getDateFinishSchedule();
			int nodeId = owf.getNodeLastID(getAD_Client_ID());
			MPPOrderNode node = null;

			while(nodeId != 0)
			{
				node = owf.getNode(nodeId);
				if (visitedNodes.contains(nodeId))
				{
					throw new CRPException("Cyclic transition found - ").setPP_Order_Node(node);
				}
				visitedNodes.add(nodeId);
				log.info("PP_Order Node:" + node.getName() != null ? node.getName() : ""  + " Description:" + node.getDescription() != null ? node.getDescription() : "");
				//
				MResource resource = MResource.get(getCtx(), node.getS_Resource_ID());
				
				// Skip this node if there is no resource
				if(resource == null)
				{						
					nodeId = owf.getPrevious(nodeId, getAD_Client_ID());
					continue;
				}

				if(!reasoner.isAvailable(resource))
				{
					throw new CRPException("@ResourceNotInSlotDay@").setS_Resource(resource);
				}

				long nodeMillis = routingService.calculateMillisFor(node, owf.getDurationBaseSec());
				Timestamp dateStart = scheduleBackward(date, nodeMillis ,resource);

				node.setDateStartSchedule(dateStart);
				node.setDateFinishSchedule(date);
				node.saveEx();
				
				//-->FERRY, create resource assignment
				BigDecimal duration = BigDecimal.valueOf(nodeMillis / 1000 / 60); //in minutes		
				MResourceAssignment ra = routingService.createResourceAssign(mrp, getCtx(), duration, node.getAD_WF_Node(), dateStart, date);
				//<--
				
				date = node.getDateStartSchedule();
				nodeId = owf.getPrevious(nodeId, getAD_Client_ID());
			}
			// Update order start date
			if (node != null && node.getDateStartSchedule() != null)
			{
				order.setDateStartSchedule(node.getDateStartSchedule()) ;
			}
		}
		else
		{
			throw new CRPException("Unknown scheduling method - "+p_ScheduleType);
		}

		order.saveEx(get_TrxName());
		
		//-->FERRY update MRP DateStartSchedule & DateFinishSchedule based on MO
		whereClause = MPPOrder.Table_Name+"_ID=? AND AD_Client_ID=? AND ( DocStatus=? OR DocStatus=? ) AND ( OrderType=? OR OrderType=?)";
		List<MPPMRP>mrpset = new Query(getCtx(), MPPMRP.Table_Name, whereClause, get_TrxName())
				.setParameters(new Object[]{order.get_ID(), order.getAD_Client_ID(),MPPMRP.DOCSTATUS_Drafted,MPPMRP.DOCSTATUS_InProgress,MPPMRP.ORDERTYPE_SalesOrder,MPPMRP.ORDERTYPE_ManufacturingOrder})
				.list();	
		for (MPPMRP mrps:mrpset){
			mrps.setDateStartSchedule(order.getDateStartSchedule());
			mrps.setDateFinishSchedule(order.getDateFinishSchedule());
			mrps.saveEx(get_TrxName());
		}
		//<--
	}
	
	/**
	 * Calculate how many millis take to complete given qty on given node(operation).
	 * @param node operation
	 * @param commonBase multiplier to convert duration to seconds 
	 * @return duration in millis
	 */
	//FERRY Put in DefaultRoutingServiceImpl.java to centralize code
	/*private long calculateMillisFor(MPPOrderNode node, long commonBase)
	{
		final BigDecimal qty = node.getQtyToDeliver();
		// Total duration of workflow node (seconds) ...
		// ... its static single parts ...
		long totalDuration =
				+ node.getQueuingTime() 
				+ node.getSetupTimeRequired() // Use the present required setup time to notice later changes  
				+ node.getMovingTime() 
				+ node.getWaitingTime()
		;
		// ... and its qty dependend working time ... (Use the present required duration time to notice later changes)
		final BigDecimal workingTime = routingService.estimateWorkingTime(node, qty);
		totalDuration += workingTime.doubleValue();
		
		// Returns the total duration of a node in milliseconds.
		return (long)(totalDuration * commonBase * 1000);
	}*/

	/**
	 * Calculate duration in millis 
	 * @param dayStart
	 * @param dayEnd
	 * @param resource
	 * @return dayEnd - dayStart in millis
	 * @throws CRPException if dayStart > dayEnd
	 */
	private long getAvailableDurationMillis(Timestamp dayStart, Timestamp dayEnd, I_S_Resource resource)
	{
		long availableDayDuration = dayEnd.getTime() - dayStart.getTime();
		log.info("--> availableDayDuration  " + availableDayDuration);
		if (availableDayDuration < 0)
		{
			throw new CRPException("@TimeSlotStart@ > @TimeSlotEnd@ ("+dayEnd+" > "+dayStart+")")
					.setS_Resource(resource);
		}
		return availableDayDuration;
	}
	
	private Timestamp scheduleForward(final Timestamp start, final long nodeDurationMillis, MResource r)
	{
		//-->FERRY
		Calendar cal = Calendar.getInstance(); 
		int hour;
		int minute;
		int second;
		//<--
		MResourceType t = r.getResourceType();
		int iteration = 0; // statistical interation count
		Timestamp currentDate = start;
		Timestamp end = null;
		long remainingMillis = nodeDurationMillis;
		do
		{
			//-->FERRY because reasoner.getAvailableDate return 00:00:00
			cal.setTimeInMillis(currentDate.getTime());
			hour = cal.get(Calendar.HOUR_OF_DAY);
			minute = cal.get(Calendar.MINUTE);
			second = cal.get(Calendar.SECOND);			
			//<--
			currentDate = reasoner.getAvailableDate(r, currentDate, false);
			//-->FERRY set hour minute second
			cal.setTimeInMillis(currentDate.getTime());
			cal.set(Calendar.HOUR_OF_DAY, hour);
		    cal.set(Calendar.MINUTE, minute);
		    cal.set(Calendar.SECOND, second);
		    currentDate.setTime(cal.getTimeInMillis());
			//<--			
			
			Timestamp dayStart = t.getDayStart(currentDate);
			
			//-->FERRY dayEnd must be started from end param
			if (iteration == 0 && currentDate.compareTo(dayStart) > 0) {
				dayStart = currentDate;
			}
			//<--			
			Timestamp dayEnd = t.getDayEnd(currentDate);
			// If working has already began at this day and the value is in the range of the 
			// resource's availability, switch start time to the given again
			if(currentDate.after(dayStart) && currentDate.before(dayEnd))
			{
				//FERRY dayStart = currentDate;
			}
	
			// The available time at this day in milliseconds
			long availableDayDuration = getAvailableDurationMillis(dayStart, dayEnd, r);
	
			// The work can be finish on this day.
			if(availableDayDuration >= remainingMillis)
			{
				end = new Timestamp(dayStart.getTime() + remainingMillis);
				remainingMillis = 0;
				break;
			}
			// Otherwise recall with next day and the remained node duration.
			else
			{
				currentDate = TimeUtil.addDays(TimeUtil.getDayBorder(currentDate, null, false), 1);
				remainingMillis -= availableDayDuration;
			}
			
			iteration++;
			if (iteration > p_MaxIterationsNo)
			{
				throw new CRPException("Maximum number of iterations exceeded ("+p_MaxIterationsNo+")"
						+" - Date:"+currentDate+", RemainingMillis:"+remainingMillis);
			}
		} while (remainingMillis > 0);

		return end;
	}  	

	/**
	 * Calculate start date having duration and resource
	 * @param end end date
	 * @param nodeDurationMillis duration [millis]
	 * @param r resource
	 * @return start date
	 */
	private Timestamp scheduleBackward(final Timestamp end, final long nodeDurationMillis, MResource r)
	{
		
		//-->FERRY
		Calendar cal = Calendar.getInstance(); 		
		int hour;
		int minute;
		int second;
		//<--
		MResourceType t = r.getResourceType();
		log.info("--> ResourceType " + t);
		Timestamp start = null;
		Timestamp currentDate = end;
		long remainingMillis = nodeDurationMillis;
		int iteration = 0; // statistical iteration count
		do
		{
			log.info("--> end=" + currentDate);
			log.info("--> nodeDuration=" + remainingMillis);
	
			//-->FERRY because reasoner.getAvailableDate return 00:00:00
			cal.setTimeInMillis(currentDate.getTime());
			hour = cal.get(Calendar.HOUR_OF_DAY);
			minute = cal.get(Calendar.MINUTE);
			second = cal.get(Calendar.SECOND);			
			//<--
			currentDate = reasoner.getAvailableDate(r, currentDate, true);
			//-->FERRY set hour minute second
			cal.setTimeInMillis(currentDate.getTime());
			cal.set(Calendar.HOUR_OF_DAY, hour);
		    cal.set(Calendar.MINUTE, minute);
		    cal.set(Calendar.SECOND, second);
		    currentDate.setTime(cal.getTimeInMillis());
			//<--
			log.info("--> end(available)=" + currentDate);
			Timestamp dayEnd = t.getDayEnd(currentDate);
			//-->FERRY dayEnd must be started from end param
			if (iteration == 0 && currentDate.compareTo(dayEnd) < 0) {
				dayEnd = currentDate;
			}
			//<--
			Timestamp dayStart = t.getDayStart(currentDate);
			log.info("--> dayStart=" + dayStart + ", dayEnd=" + dayEnd);
			
			// If working has already began at this day and the value is in the range of the 
			// resource's availability, switch end time to the given again
			if(currentDate.before(dayEnd) && currentDate.after(dayStart))
			{
				//FERRY ??? dayEnd = currentDate;
			}
	
			// The available time at this day in milliseconds
			long availableDayDuration = getAvailableDurationMillis(dayStart, dayEnd, r);
	
			// The work can be finish on this day.
			if(availableDayDuration >= remainingMillis)
			{
				log.info("--> availableDayDuration >= nodeDuration true " + availableDayDuration + "|" + remainingMillis );
				start = new Timestamp(dayEnd.getTime() - remainingMillis);
				remainingMillis = 0;
				break;
			}
			// Otherwise recall with previous day and the remained node duration.
			else
			{
				log.info("--> availableDayDuration >= nodeDuration false " + availableDayDuration + "|" + remainingMillis );
				log.info("--> nodeDuration-availableDayDuration " + (remainingMillis-availableDayDuration) );
				
				currentDate = TimeUtil.addDays(TimeUtil.getDayBorder(currentDate, null, true), -1);
				remainingMillis -= availableDayDuration;
			}
			//
			iteration++;
			if (iteration > p_MaxIterationsNo)
			{
				throw new CRPException("Maximum number of iterations exceeded ("+p_MaxIterationsNo+")"
						+" - Date:"+start+", RemainingMillis:"+remainingMillis);
			}
		}
		while(remainingMillis > 0);
	
		log.info("         -->  start=" +  start + " <---------------------------------------- ");
		return start;
	}
	//-->FERRY
	/**
	 * Calculate start date having duration and resource
	 * @param end end date
	 * @param nodeDurationMillis duration [millis]
	 * @param r resource
	 * @param AD_Client_ID
	 * @return start date
	 */
	public Timestamp scheduleBackward(final Timestamp end, final long nodeDurationMillis, MResource r, int AD_Client_ID)
	{
		if (reasoner == null) reasoner = new CRPReasoner();
		p_MaxIterationsNo = MSysConfig.getIntValue(SYSCONFIG_MaxIterationsNo, DEFAULT_MaxIterationsNo, AD_Client_ID);
       
		return scheduleBackward(end, nodeDurationMillis, r);
	}
	//<--
}

