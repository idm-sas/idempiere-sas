package id.co.databiz.sas.utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.compiere.model.MSysConfig;
import org.compiere.util.Env;

public class ApiUtil {
	
	private static final Logger log = Logger.getLogger(ApiUtil.class.getName());

    /**
     * Send PUT request to Sales Order API with token from SysConfig.
     *
     * @param host         API host (e.g. https://example.com)
     * @param noDocuments  Sales order document number
     * @param status       Delivery status (e.g. Delivered)
     * @return boolean     true if request was successful (HTTP 200-299)
     */
    public static boolean updateSalesOrderStatus(String host, String noDocuments, String status) {
        // Get token from SysConfig
        String token = MSysConfig.getValue("API_SALES_ORDER_TOKEN", "", Env.getAD_Client_ID(Env.getCtx()));

        if (token == null || token.isEmpty()) {
            log.log(Level.SEVERE, "API token not found in SysConfig (API_SALES_ORDER_TOKEN)");
            return false;
        }

        String apiUrl = host + "/api/sales-order/" + noDocuments;
        String jsonBody = "{\"status_delivery\": \"" + status + "\"}";

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            log.log(Level.INFO, "PUT " + apiUrl);
            log.log(Level.INFO, "Status Code: " + response.statusCode());
            log.log(Level.INFO, "Response: " + response.body());

            return response.statusCode() >= 200 && response.statusCode() < 300;

        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to send PUT request to Sales Order API", e);
            return false;
        }
    }
}
