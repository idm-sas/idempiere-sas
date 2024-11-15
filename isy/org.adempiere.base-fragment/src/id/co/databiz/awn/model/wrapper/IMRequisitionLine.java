
package id.co.databiz.awn.model.wrapper;

import java.sql.Timestamp;


public interface IMRequisitionLine 
{
	public void setDateRequired (Timestamp DateRequired);
	public Timestamp getDateRequired();
	
	public int getC_Project_ID();
	public void setC_Project_ID(int projectID);
	
	public int getUser1_ID();
	public void setUser1_ID(int user1ID);
}
