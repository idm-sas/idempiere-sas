package id.co.databiz.awn.process;

import java.util.logging.Level;

import org.compiere.model.MJournal;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

public class ISY387_DeleteGLJournalLine extends SvrProcess{
	
	private int p_GL_Journal_ID=0;

	@Override
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else if (name.equals("GL_Journal_ID"))
				p_GL_Journal_ID = para[i].getParameterAsInt();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
	}

	@Override
	protected String doIt() throws Exception {
	
		//Query for delete GL Jorunal Line 
		String sql = "DELETE FROM GL_JournalLine WHERE GL_Journal_ID=?";
		DB.executeUpdate(sql, p_GL_Journal_ID, get_TrxName());
		
		//Query for set = 0 on header GL Journal totalDR & totalCR
		sql = "UPDATE GL_Journal SET totalCR = 0, totalDR = 0 WHERE GL_Journal_ID = ?";
		DB.executeUpdate(sql, p_GL_Journal_ID, get_TrxName());
		
		return "Deleted GL Journal Line";
	}

}
