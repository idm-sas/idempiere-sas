
package id.co.databiz.awn.model.wrapper;


public interface ICInvoice 
{
	public void setZ_TaxInvoice_ID (int Z_TaxInvoice_ID);
	public int getZ_TaxInvoice_ID();
	
	public int getZ_WFScenario_ID();
	public void setZ_WFScenario_ID(int Z_WFScenario_ID);
	
	public String getTaxInvoiceNo();
	public void setTaxInvoiceNo(String taxInvoiceNo);
	
	public String getCreateTaxInvoice();
	public void setCreateTaxInvoice(String createTaxInvoice);
}
