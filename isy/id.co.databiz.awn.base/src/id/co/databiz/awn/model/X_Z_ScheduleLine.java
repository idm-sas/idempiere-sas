/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2012 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
/** Generated Model - DO NOT CHANGE */
package id.co.databiz.awn.model;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;
import org.compiere.model.*;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;

/** Generated Model for Z_ScheduleLine
 *  @author iDempiere (generated) 
 *  @version Release 2.0 b494 - $Id$ */
public class X_Z_ScheduleLine extends PO implements I_Z_ScheduleLine, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20141026L;

    /** Standard Constructor */
    public X_Z_ScheduleLine (Properties ctx, int Z_ScheduleLine_ID, String trxName)
    {
      super (ctx, Z_ScheduleLine_ID, trxName);
      /** if (Z_ScheduleLine_ID == 0)
        {
			setC_OrderLine_ID (0);
			setC_Order_ID (0);
			setQty (Env.ZERO);
// 0
			setQtyEntered (Env.ZERO);
// 0
			setZ_ScheduleLine_ID (0);
			setZ_Schedule_ID (0);
        } */
    }

    /** Load Constructor */
    public X_Z_ScheduleLine (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 1 - Org 
      */
    protected int get_AccessLevel()
    {
      return accessLevel.intValue();
    }

    /** Load Meta Data */
    protected POInfo initPO (Properties ctx)
    {
      POInfo poi = POInfo.getPOInfo (ctx, Table_ID, get_TrxName());
      return poi;
    }

    public String toString()
    {
      StringBuffer sb = new StringBuffer ("X_Z_ScheduleLine[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set Arrive On Port Actual Date.
		@param ArriveOnPortActDate Arrive On Port Actual Date	  */
	public void setArriveOnPortActDate (Timestamp ArriveOnPortActDate)
	{
		set_Value (COLUMNNAME_ArriveOnPortActDate, ArriveOnPortActDate);
	}

	/** Get Arrive On Port Actual Date.
		@return Arrive On Port Actual Date	  */
	public Timestamp getArriveOnPortActDate () 
	{
		return (Timestamp)get_Value(COLUMNNAME_ArriveOnPortActDate);
	}

	/** Set Arrive On Port Estimated Date.
		@param ArriveOnPortEstDate Arrive On Port Estimated Date	  */
	public void setArriveOnPortEstDate (Timestamp ArriveOnPortEstDate)
	{
		set_Value (COLUMNNAME_ArriveOnPortEstDate, ArriveOnPortEstDate);
	}

	/** Get Arrive On Port Estimated Date.
		@return Arrive On Port Estimated Date	  */
	public Timestamp getArriveOnPortEstDate () 
	{
		return (Timestamp)get_Value(COLUMNNAME_ArriveOnPortEstDate);
	}

	public org.compiere.model.I_C_OrderLine getC_OrderLine() throws RuntimeException
    {
		return (org.compiere.model.I_C_OrderLine)MTable.get(getCtx(), org.compiere.model.I_C_OrderLine.Table_Name)
			.getPO(getC_OrderLine_ID(), get_TrxName());	}

	/** Set Sales Order Line.
		@param C_OrderLine_ID 
		Sales Order Line
	  */
	public void setC_OrderLine_ID (int C_OrderLine_ID)
	{
		if (C_OrderLine_ID < 1) 
			set_Value (COLUMNNAME_C_OrderLine_ID, null);
		else 
			set_Value (COLUMNNAME_C_OrderLine_ID, Integer.valueOf(C_OrderLine_ID));
	}

	/** Get Sales Order Line.
		@return Sales Order Line
	  */
	public int getC_OrderLine_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_OrderLine_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_C_Order getC_Order() throws RuntimeException
    {
		return (org.compiere.model.I_C_Order)MTable.get(getCtx(), org.compiere.model.I_C_Order.Table_Name)
			.getPO(getC_Order_ID(), get_TrxName());	}

	/** Set Order.
		@param C_Order_ID 
		Order
	  */
	public void setC_Order_ID (int C_Order_ID)
	{
		if (C_Order_ID < 1) 
			set_Value (COLUMNNAME_C_Order_ID, null);
		else 
			set_Value (COLUMNNAME_C_Order_ID, Integer.valueOf(C_Order_ID));
	}

	/** Get Order.
		@return Order
	  */
	public int getC_Order_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Order_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_C_UOM getC_UOM() throws RuntimeException
    {
		return (org.compiere.model.I_C_UOM)MTable.get(getCtx(), org.compiere.model.I_C_UOM.Table_Name)
			.getPO(getC_UOM_ID(), get_TrxName());	}

	/** Set UOM.
		@param C_UOM_ID 
		Unit of Measure
	  */
	public void setC_UOM_ID (int C_UOM_ID)
	{
		if (C_UOM_ID < 1) 
			set_Value (COLUMNNAME_C_UOM_ID, null);
		else 
			set_Value (COLUMNNAME_C_UOM_ID, Integer.valueOf(C_UOM_ID));
	}

	/** Get UOM.
		@return Unit of Measure
	  */
	public int getC_UOM_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_UOM_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Date Promised.
		@param DatePromised 
		Date Order was promised
	  */
	public void setDatePromised (Timestamp DatePromised)
	{
		set_Value (COLUMNNAME_DatePromised, DatePromised);
	}

	/** Get Date Promised.
		@return Date Order was promised
	  */
	public Timestamp getDatePromised () 
	{
		return (Timestamp)get_Value(COLUMNNAME_DatePromised);
	}

	/** Set Description.
		@param Description 
		Optional short description of the record
	  */
	public void setDescription (String Description)
	{
		set_Value (COLUMNNAME_Description, Description);
	}

	/** Get Description.
		@return Optional short description of the record
	  */
	public String getDescription () 
	{
		return (String)get_Value(COLUMNNAME_Description);
	}

	public org.compiere.model.I_M_Product getM_Product() throws RuntimeException
    {
		return (org.compiere.model.I_M_Product)MTable.get(getCtx(), org.compiere.model.I_M_Product.Table_Name)
			.getPO(getM_Product_ID(), get_TrxName());	}

	/** Set Product.
		@param M_Product_ID 
		Product, Service, Item
	  */
	public void setM_Product_ID (int M_Product_ID)
	{
		if (M_Product_ID < 1) 
			set_Value (COLUMNNAME_M_Product_ID, null);
		else 
			set_Value (COLUMNNAME_M_Product_ID, Integer.valueOf(M_Product_ID));
	}

	/** Get Product.
		@return Product, Service, Item
	  */
	public int getM_Product_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_Product_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

    /** Get Record ID/ColumnName
        @return ID/ColumnName pair
      */
    public KeyNamePair getKeyNamePair() 
    {
        return new KeyNamePair(get_ID(), String.valueOf(getM_Product_ID()));
    }

	/** Set Quantity.
		@param Qty 
		Quantity
	  */
	public void setQty (BigDecimal Qty)
	{
		set_Value (COLUMNNAME_Qty, Qty);
	}

	/** Get Quantity.
		@return Quantity
	  */
	public BigDecimal getQty () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_Qty);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Quantity.
		@param QtyEntered 
		The Quantity Entered is based on the selected UoM
	  */
	public void setQtyEntered (BigDecimal QtyEntered)
	{
		set_Value (COLUMNNAME_QtyEntered, QtyEntered);
	}

	/** Get Quantity.
		@return The Quantity Entered is based on the selected UoM
	  */
	public BigDecimal getQtyEntered () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_QtyEntered);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Shipment Doc Received Date.
		@param ShipDocReceivedDate Shipment Doc Received Date	  */
	public void setShipDocReceivedDate (Timestamp ShipDocReceivedDate)
	{
		set_Value (COLUMNNAME_ShipDocReceivedDate, ShipDocReceivedDate);
	}

	/** Get Shipment Doc Received Date.
		@return Shipment Doc Received Date	  */
	public Timestamp getShipDocReceivedDate () 
	{
		return (Timestamp)get_Value(COLUMNNAME_ShipDocReceivedDate);
	}

	/** Set Shipment Vendor Actual Date.
		@param ShipVendActDate Shipment Vendor Actual Date	  */
	public void setShipVendActDate (Timestamp ShipVendActDate)
	{
		set_Value (COLUMNNAME_ShipVendActDate, ShipVendActDate);
	}

	/** Get Shipment Vendor Actual Date.
		@return Shipment Vendor Actual Date	  */
	public Timestamp getShipVendActDate () 
	{
		return (Timestamp)get_Value(COLUMNNAME_ShipVendActDate);
	}

	/** Set Shipment Vendor Promised Date.
		@param ShipVendPromDate Shipment Vendor Promised Date	  */
	public void setShipVendPromDate (Timestamp ShipVendPromDate)
	{
		set_Value (COLUMNNAME_ShipVendPromDate, ShipVendPromDate);
	}

	/** Get Shipment Vendor Promised Date.
		@return Shipment Vendor Promised Date	  */
	public Timestamp getShipVendPromDate () 
	{
		return (Timestamp)get_Value(COLUMNNAME_ShipVendPromDate);
	}

	/** Set Delivery Schedule Line.
		@param Z_ScheduleLine_ID Delivery Schedule Line	  */
	public void setZ_ScheduleLine_ID (int Z_ScheduleLine_ID)
	{
		if (Z_ScheduleLine_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_Z_ScheduleLine_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_Z_ScheduleLine_ID, Integer.valueOf(Z_ScheduleLine_ID));
	}

	/** Get Delivery Schedule Line.
		@return Delivery Schedule Line	  */
	public int getZ_ScheduleLine_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Z_ScheduleLine_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Z_ScheduleLine_UU.
		@param Z_ScheduleLine_UU Z_ScheduleLine_UU	  */
	public void setZ_ScheduleLine_UU (String Z_ScheduleLine_UU)
	{
		set_Value (COLUMNNAME_Z_ScheduleLine_UU, Z_ScheduleLine_UU);
	}

	/** Get Z_ScheduleLine_UU.
		@return Z_ScheduleLine_UU	  */
	public String getZ_ScheduleLine_UU () 
	{
		return (String)get_Value(COLUMNNAME_Z_ScheduleLine_UU);
	}

	public I_Z_Schedule getZ_Schedule() throws RuntimeException
    {
		return (I_Z_Schedule)MTable.get(getCtx(), I_Z_Schedule.Table_Name)
			.getPO(getZ_Schedule_ID(), get_TrxName());	}

	/** Set Delivery Schedule.
		@param Z_Schedule_ID Delivery Schedule	  */
	public void setZ_Schedule_ID (int Z_Schedule_ID)
	{
		if (Z_Schedule_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_Z_Schedule_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_Z_Schedule_ID, Integer.valueOf(Z_Schedule_ID));
	}

	/** Get Delivery Schedule.
		@return Delivery Schedule	  */
	public int getZ_Schedule_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Z_Schedule_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}
}