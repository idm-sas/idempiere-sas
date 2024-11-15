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
package id.co.databiz.awn.model;

import java.math.BigDecimal;
import java.sql.Timestamp;
import org.compiere.model.*;
import org.compiere.util.KeyNamePair;

/** Generated Interface for Z_ScheduleLine
 *  @author iDempiere (generated) 
 *  @version Release 2.0 b494
 */
@SuppressWarnings("all")
public interface I_Z_ScheduleLine 
{

    /** TableName=Z_ScheduleLine */
    public static final String Table_Name = "Z_ScheduleLine";

    /** AD_Table_ID=550015 */
    public static final int Table_ID = 550015;

    KeyNamePair Model = new KeyNamePair(Table_ID, Table_Name);

    /** AccessLevel = 1 - Org 
     */
    BigDecimal accessLevel = BigDecimal.valueOf(1);

    /** Load Meta Data */

    /** Column name AD_Client_ID */
    public static final String COLUMNNAME_AD_Client_ID = "AD_Client_ID";

	/** Get Client.
	  * Client/Tenant for this installation.
	  */
	public int getAD_Client_ID();

    /** Column name AD_Org_ID */
    public static final String COLUMNNAME_AD_Org_ID = "AD_Org_ID";

	/** Set Organization.
	  * Organizational entity within client
	  */
	public void setAD_Org_ID (int AD_Org_ID);

	/** Get Organization.
	  * Organizational entity within client
	  */
	public int getAD_Org_ID();

    /** Column name ArriveOnPortActDate */
    public static final String COLUMNNAME_ArriveOnPortActDate = "ArriveOnPortActDate";

	/** Set Arrive On Port Actual Date	  */
	public void setArriveOnPortActDate (Timestamp ArriveOnPortActDate);

	/** Get Arrive On Port Actual Date	  */
	public Timestamp getArriveOnPortActDate();

    /** Column name ArriveOnPortEstDate */
    public static final String COLUMNNAME_ArriveOnPortEstDate = "ArriveOnPortEstDate";

	/** Set Arrive On Port Estimated Date	  */
	public void setArriveOnPortEstDate (Timestamp ArriveOnPortEstDate);

	/** Get Arrive On Port Estimated Date	  */
	public Timestamp getArriveOnPortEstDate();

    /** Column name C_OrderLine_ID */
    public static final String COLUMNNAME_C_OrderLine_ID = "C_OrderLine_ID";

	/** Set Sales Order Line.
	  * Sales Order Line
	  */
	public void setC_OrderLine_ID (int C_OrderLine_ID);

	/** Get Sales Order Line.
	  * Sales Order Line
	  */
	public int getC_OrderLine_ID();

	public org.compiere.model.I_C_OrderLine getC_OrderLine() throws RuntimeException;

    /** Column name C_Order_ID */
    public static final String COLUMNNAME_C_Order_ID = "C_Order_ID";

	/** Set Order.
	  * Order
	  */
	public void setC_Order_ID (int C_Order_ID);

	/** Get Order.
	  * Order
	  */
	public int getC_Order_ID();

	public org.compiere.model.I_C_Order getC_Order() throws RuntimeException;

    /** Column name C_UOM_ID */
    public static final String COLUMNNAME_C_UOM_ID = "C_UOM_ID";

	/** Set UOM.
	  * Unit of Measure
	  */
	public void setC_UOM_ID (int C_UOM_ID);

	/** Get UOM.
	  * Unit of Measure
	  */
	public int getC_UOM_ID();

	public org.compiere.model.I_C_UOM getC_UOM() throws RuntimeException;

    /** Column name Created */
    public static final String COLUMNNAME_Created = "Created";

	/** Get Created.
	  * Date this record was created
	  */
	public Timestamp getCreated();

    /** Column name CreatedBy */
    public static final String COLUMNNAME_CreatedBy = "CreatedBy";

	/** Get Created By.
	  * User who created this records
	  */
	public int getCreatedBy();

    /** Column name DatePromised */
    public static final String COLUMNNAME_DatePromised = "DatePromised";

	/** Set Date Promised.
	  * Date Order was promised
	  */
	public void setDatePromised (Timestamp DatePromised);

	/** Get Date Promised.
	  * Date Order was promised
	  */
	public Timestamp getDatePromised();

    /** Column name Description */
    public static final String COLUMNNAME_Description = "Description";

	/** Set Description.
	  * Optional short description of the record
	  */
	public void setDescription (String Description);

	/** Get Description.
	  * Optional short description of the record
	  */
	public String getDescription();

    /** Column name IsActive */
    public static final String COLUMNNAME_IsActive = "IsActive";

	/** Set Active.
	  * The record is active in the system
	  */
	public void setIsActive (boolean IsActive);

	/** Get Active.
	  * The record is active in the system
	  */
	public boolean isActive();

    /** Column name M_Product_ID */
    public static final String COLUMNNAME_M_Product_ID = "M_Product_ID";

	/** Set Product.
	  * Product, Service, Item
	  */
	public void setM_Product_ID (int M_Product_ID);

	/** Get Product.
	  * Product, Service, Item
	  */
	public int getM_Product_ID();

	public org.compiere.model.I_M_Product getM_Product() throws RuntimeException;

    /** Column name Qty */
    public static final String COLUMNNAME_Qty = "Qty";

	/** Set Quantity.
	  * Quantity
	  */
	public void setQty (BigDecimal Qty);

	/** Get Quantity.
	  * Quantity
	  */
	public BigDecimal getQty();

    /** Column name QtyEntered */
    public static final String COLUMNNAME_QtyEntered = "QtyEntered";

	/** Set Quantity.
	  * The Quantity Entered is based on the selected UoM
	  */
	public void setQtyEntered (BigDecimal QtyEntered);

	/** Get Quantity.
	  * The Quantity Entered is based on the selected UoM
	  */
	public BigDecimal getQtyEntered();

    /** Column name ShipDocReceivedDate */
    public static final String COLUMNNAME_ShipDocReceivedDate = "ShipDocReceivedDate";

	/** Set Shipment Doc Received Date	  */
	public void setShipDocReceivedDate (Timestamp ShipDocReceivedDate);

	/** Get Shipment Doc Received Date	  */
	public Timestamp getShipDocReceivedDate();

    /** Column name ShipVendActDate */
    public static final String COLUMNNAME_ShipVendActDate = "ShipVendActDate";

	/** Set Shipment Vendor Actual Date	  */
	public void setShipVendActDate (Timestamp ShipVendActDate);

	/** Get Shipment Vendor Actual Date	  */
	public Timestamp getShipVendActDate();

    /** Column name ShipVendPromDate */
    public static final String COLUMNNAME_ShipVendPromDate = "ShipVendPromDate";

	/** Set Shipment Vendor Promised Date	  */
	public void setShipVendPromDate (Timestamp ShipVendPromDate);

	/** Get Shipment Vendor Promised Date	  */
	public Timestamp getShipVendPromDate();

    /** Column name Updated */
    public static final String COLUMNNAME_Updated = "Updated";

	/** Get Updated.
	  * Date this record was updated
	  */
	public Timestamp getUpdated();

    /** Column name UpdatedBy */
    public static final String COLUMNNAME_UpdatedBy = "UpdatedBy";

	/** Get Updated By.
	  * User who updated this records
	  */
	public int getUpdatedBy();

    /** Column name Z_ScheduleLine_ID */
    public static final String COLUMNNAME_Z_ScheduleLine_ID = "Z_ScheduleLine_ID";

	/** Set Delivery Schedule Line	  */
	public void setZ_ScheduleLine_ID (int Z_ScheduleLine_ID);

	/** Get Delivery Schedule Line	  */
	public int getZ_ScheduleLine_ID();

    /** Column name Z_ScheduleLine_UU */
    public static final String COLUMNNAME_Z_ScheduleLine_UU = "Z_ScheduleLine_UU";

	/** Set Z_ScheduleLine_UU	  */
	public void setZ_ScheduleLine_UU (String Z_ScheduleLine_UU);

	/** Get Z_ScheduleLine_UU	  */
	public String getZ_ScheduleLine_UU();

    /** Column name Z_Schedule_ID */
    public static final String COLUMNNAME_Z_Schedule_ID = "Z_Schedule_ID";

	/** Set Delivery Schedule	  */
	public void setZ_Schedule_ID (int Z_Schedule_ID);

	/** Get Delivery Schedule	  */
	public int getZ_Schedule_ID();

	public I_Z_Schedule getZ_Schedule() throws RuntimeException;
}
