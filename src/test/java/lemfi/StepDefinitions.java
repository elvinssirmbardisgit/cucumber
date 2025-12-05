package lemfi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.text.DecimalFormat;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

public class StepDefinitions {

    private final RequestSpecification request;
    private Response response;
    private String sessionId;
    private int paymentId;

    public StepDefinitions() {
        request = given()
                .baseUri("http://localhost:8080");
    }

    @Given("a logged in user with random email and password")
    public void randomUser() {
        String user = java.util.UUID.randomUUID().toString();
        String password = java.util.UUID.randomUUID().toString();
        String payload = String.format("""
                { "email": "%s@gmail.com", "password": "%s"}
                """, user, password);

        var response = request
                .contentType(ContentType.JSON)
                .body(payload)
                .post("public/sign-up");

        response.then()
                .statusCode(200);

        sessionId = response.getCookie("JSESSIONID");
        System.out.println(sessionId);
    }

    @When("person is updated with name {string} and surname {string} and person id {int}")
    public void updatePerson(String name, String surname, int id) {
        String payload = String.format("""
                { "firstName": "%s", "surname": "%s", "personalId": %d }
                """, name, surname, id);

        response = request
                .contentType(ContentType.JSON)
                .cookie("JSESSIONID", sessionId)
                .body(payload)
                .post("api/personal-data");
    }

    @Then("the person update succeeds and account balance is {double}")
    public void personUpdateSucceeds(double balance) {
        response.then().statusCode(201);

        request
                .cookie("JSESSIONID", sessionId)
                .get("api/balance")
                .then()
                .statusCode(200)
                .body(equalTo(new DecimalFormat("0.00").format(balance).replace(',', '.')));
    }

    @When("{double} {string} is added to the account")
    public void addFunds(double amount, String currency) {
        String strAmount = new DecimalFormat("0.00").format(amount).replace(',', '.');
        String payload = String.format("""
                { "accountHolderFullName": "Qwerty Asdfgh",
                    "accountHolderPersonalId": 123,
                    "transactionType": "FUNDING",
                    "investorId": "123",
                    "amount": {
                    "currency": "%s",
                    "amount": %s
                    },
                    "bookingDate": "2025-11-07",
                    "accountNumber": "123" }
                """, currency, strAmount);

        response = request
                .contentType(ContentType.JSON)
                .cookie("JSESSIONID", sessionId)
                .body(payload)
                .post("api/add-funds");

        System.out.println(payload);

        response.then()
                .statusCode(200);

        var text = response.getBody().asString();
        String[] parts = text.split("id:");
        paymentId = Integer.parseInt(parts[1].trim());

    }

    @Then("the account balance is {double}")
    public void verifyFunds(double balance) {
        request
                .cookie("JSESSIONID", sessionId)
                .get("api/balance")
                .then()
                .statusCode(200)
                .body(equalTo(new DecimalFormat("0.00").format(balance).replace(',', '.')));
    }

    @And("the payment contains {double} {string}")
    public void verifyPayments(double amount, String currency) {
        response = request
                .cookie("JSESSIONID", sessionId)
                .get("api/payments");
                response.then().statusCode(200);

        String rawResponse = response.jsonPath().getString("find { it.id == " + paymentId + " }.rawResponse");

        JsonPath rawJson = new JsonPath(rawResponse);

        String respCurrency = rawJson.getString("amount.currency");
        Double respAmount = rawJson.getDouble("amount.amount");

        assertThat(respAmount, equalTo(amount));
        assertThat(respCurrency, equalTo(currency));
    }

}