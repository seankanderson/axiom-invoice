package com.datavirtue.axiom.services.integrations;

import com.datavirtue.axiom.models.integrations.PayPal.PayPalAuthenticationResponse;
import com.datavirtue.axiom.models.integrations.PayPal.PayPalInvoice;
import com.datavirtue.axiom.models.integrations.PayPal.PayPalInvoicePaymentRequest;
import com.datavirtue.axiom.models.integrations.PayPal.PayPalLinkDescription;
import com.datavirtue.axiom.models.integrations.PayPal.PayPalListInvoicesResponse;
import com.datavirtue.axiom.models.invoices.Invoice;
import com.datavirtue.axiom.services.JsonHelper;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

/**
 * July 16, 2022
 *
 * @author sean.anderson
 */
@Getter
@Setter
public class PayPalApiService {

    private String baseUrl = "https://api-m.sandbox.paypal.com";
    private String accountId = "sb-4p5qe15864702@business.example.com";
    private String userNameClientId = "Ab505eorhV18Uu73zHnzZS-6BMfhwJIWju6Yhxb8yE8E7LSHsBtZMZxd3p0vw53ZBAsCsaLc6Sk3FYw5";
    private String userPasswordSecret = "EBy0RTbVf3gmYKLPY5k2kPjf0BTx1jzq0K6PtxBOET9d2zN5_0Eq2raNEGt-g0WrkBhtxHnMSwlB50Ei";

    private static String authToken = null;

    /**
     * Creates an HttpRequest, ensuring that a valid auth token is in use.
     *
     * @param path The API url path that is appended to the base url.
     * @param body POST body (Likely a JSON string)
     * @return
     * @throws URISyntaxException
     */
    private HttpRequest createHttpPostRequest(String path, String body) throws URISyntaxException {
        authenticate();
        HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(new URI(baseUrl + path))
                .version(HttpClient.Version.HTTP_2)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + authToken)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return postRequest;
    }

    private void authenticate() {
        if (StringUtils.isBlank(authToken)) {
            try {
                requestAuthToken();
            } catch (URISyntaxException ex) {
                Logger.getLogger(PayPalApiService.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(PayPalApiService.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InterruptedException ex) {
                Logger.getLogger(PayPalApiService.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void requestAuthToken() throws URISyntaxException, IOException, InterruptedException {

        var authValue = HttpHelper.getBasicAuthorizationHeaderValue(userNameClientId, userPasswordSecret);

        HttpRequest payPalAuthTokenRequest = HttpRequest.newBuilder()
                .uri(new URI(baseUrl + "/v1/oauth2/token"))
                .version(HttpClient.Version.HTTP_2)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Authorization", authValue) //uses a slightly different auth method than standard requests
                .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
                .build();

        var response = HttpClient.newHttpClient().send(payPalAuthTokenRequest, BodyHandlers.ofString());
        var payPalAuthResponse = JsonHelper.getGson().fromJson((String) response.body(), PayPalAuthenticationResponse.class);
        authToken = payPalAuthResponse.getAccessToken();
    }

    /**
     * Takes a URL and places a GET call for that URL.
     *
     * @param url
     * @return The response body (Likely should be parsed as JSON, the caller
     * will know the class type.)
     * @throws URISyntaxException
     * @throws IOException
     * @throws InterruptedException
     */
    public String getByUrl(String url) throws URISyntaxException, IOException, InterruptedException {
        authenticate();
        HttpRequest getCall = HttpRequest.newBuilder()
                .uri(new URI(url))
                .version(HttpClient.Version.HTTP_2)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + authToken)
                .GET()
                .build();

        var response = HttpClient.newHttpClient().send(getCall, BodyHandlers.ofString());
        return response.body();
    }

    /**
     * First step to sending an invoice. It is created in draft form.
     *
     * @param invoice PayPalInvoice object.
     * @return
     * @throws URISyntaxException
     * @throws IOException
     * @throws InterruptedException
     */
    public PayPalLinkDescription createDraftInvoice(PayPalInvoice invoice) throws URISyntaxException, IOException, InterruptedException {

        var invoiceJson = JsonHelper.getGson().toJson(invoice);

        var request = createHttpPostRequest("/v2/invoicing/invoices", invoiceJson);

        var response = HttpClient.newHttpClient().send(request, BodyHandlers.ofString());
        var body = response.body();

        var createdInvoice = JsonHelper.getGson().fromJson(body, PayPalLinkDescription.class);
        return createdInvoice;
    }

    /**
     * Second step to sending an invoice. This actually posts and sends
     * appropriate notifications.
     *
     * @param draftInvoiceId Invoice ID of a current draft invoice.
     * @return PayPal Payer link URL. An openly accessible public web page that
     * will accept payment for the invoice.
     * @throws URISyntaxException
     * @throws IOException
     * @throws InterruptedException
     */
    public PayPalLinkDescription sendDraftInvoice(String draftInvoiceId) throws URISyntaxException, IOException, InterruptedException {

        var sendBody = "{\"send_to_invoicer\": false}";
        var request = createHttpPostRequest("/v2/invoicing/invoices/" + draftInvoiceId + "/send", sendBody);
        var response = HttpClient.newHttpClient().send(request, BodyHandlers.ofString());
        var body = response.body();

        var payerLink = JsonHelper.getGson().fromJson(body, PayPalLinkDescription.class);

        return payerLink;
    }

    /**
     * TODO: revise this to page or search to get a specific invoice.
     *
     * @return
     * @throws URISyntaxException
     * @throws IOException
     * @throws InterruptedException
     */
    public ArrayList<PayPalInvoice> listInvoices() throws URISyntaxException, IOException, InterruptedException {
        authenticate();
        HttpRequest listInvoices = HttpRequest.newBuilder()
                .uri(new URI(baseUrl + "/v2/invoicing/invoices"))
                .version(HttpClient.Version.HTTP_2)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + authToken)
                .GET()
                .build();

        var response = HttpClient.newHttpClient().send(listInvoices, BodyHandlers.ofString());
        var payPalinvoices = JsonHelper.getGson().fromJson((String) response.body(), PayPalListInvoicesResponse.class);
        return payPalinvoices.getItems();

    }

    /**
     * Search for a specific invoice that includes the referenceValue in the
     * reference field. Axiom tries to maintain a single PayPal invoice that
     * maps to each invoice number. That invoice number is stored in the
     * reference field.
     *
     * @param referenceValue The value you are searching for in the reference
     * field. The PayPal example is: PO#.
     * @return
     * @throws InterruptedException
     * @throws URISyntaxException
     * @throws IOException
     */
    public ArrayList<PayPalInvoice> getPayPalInvoicesByPrivateBookkeepingMemo(String axiomInvoiceNumber) throws InterruptedException, URISyntaxException, IOException {
        var queryBody = "{\"memo\": \"" + axiomInvoiceNumber + "\"}";
        var searchInvoices = createHttpPostRequest("/v2/invoicing/search-invoices", queryBody);
        var response = HttpClient.newHttpClient().send(searchInvoices, BodyHandlers.ofString());
        var payPalinvoices = JsonHelper.getGson().fromJson((String) response.body(), PayPalListInvoicesResponse.class);
        return payPalinvoices.getItems();
    }

    public void recordExternalPaymentAgainstPayPalInvoice(String payPalInvoiceId, PayPalInvoicePaymentRequest paymentRequest)
            throws URISyntaxException, IOException, InterruptedException {
        var body = JsonHelper.getGson().toJson(paymentRequest);
        var postRequest = createHttpPostRequest("/v2/invoicing/invoices/" + payPalInvoiceId + "/payments", body);
        var response = HttpClient.newHttpClient().send(postRequest, BodyHandlers.ofString());

    }

    public void deleteExternalPaymentAgainstPayPalInvoice(String payPalInvoiceId, PayPalInvoicePaymentRequest paymentRequest)
            throws URISyntaxException, IOException, InterruptedException {
        var body = JsonHelper.getGson().toJson(paymentRequest);
        var postRequest = createHttpPostRequest("/v2/invoicing/invoices/" + payPalInvoiceId + "/payments", body);
        var response = HttpClient.newHttpClient().send(postRequest, BodyHandlers.ofString());

    }

    public void synchronizePaymentsFromPayPalToAxiom(Invoice invoice) {
        // get corresponding invoice from PayPal
        // iterate over payment array
        // check each payment for an Axiom reference
        // if no axiom reference exists, record a payment against the axiom invoice
    }

    /**
     * First integration I tested due to simplicity...not really in use. Invoice
     * numbers are automatically generated on create.
     *
     * @return
     * @throws URISyntaxException
     * @throws IOException
     * @throws InterruptedException
     */
    public String generateInvoiceNumber() throws URISyntaxException, IOException, InterruptedException {
        authenticate();
        HttpRequest generateInvoiceNumber = HttpRequest.newBuilder()
                .uri(new URI(baseUrl + "/v2/invoicing/generate-next-invoice-number"))
                .version(HttpClient.Version.HTTP_2)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + authToken)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        var response = HttpClient.newHttpClient().send(generateInvoiceNumber, BodyHandlers.ofString());

        return (String) JsonHelper.getSingleValueFromJsonProperty((String) response.body(), "invoice_number");

    }

}
