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

import org.adempiere.webui.apps.form.WGenForm;
import org.compiere.apps.form.GenForm;

/**
 * Generate custom form window
 * 
 */
public class WGenFormCustom extends WGenForm
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -8669256486969882958L;

	private GenForm genForm;
	
	public WGenFormCustom(GenForm genForm) {
		super(genForm);
		// TODO Auto-generated constructor stub
	}
	
	public void onAfterProcess()
	{
		
	}
}
