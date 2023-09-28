package io.nuvalence.platform.audit;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.nuvalence.platform.audit.client.ApiClient;
import io.nuvalence.platform.audit.client.generated.controllers.AuditEventsApi;
import io.nuvalence.platform.audit.client.generated.models.AuditEvent;
import io.nuvalence.platform.audit.client.generated.models.AuditEventRequest;
import io.nuvalence.platform.audit.client.generated.models.AuditEventsPage;
import io.nuvalence.platform.audit.utils.cucumber.contexts.ApiClientContext;
import io.nuvalence.platform.audit.utils.cucumber.contexts.ScenarioContext;
import io.nuvalence.platform.audit.utils.cucumber.transformers.ActivityEventTableTransformer;
import io.nuvalence.platform.audit.utils.cucumber.transformers.StateChangeEventTableTransformer;
import io.nuvalence.platform.audit.utils.matchers.AuditEventMatcher;
import io.nuvalence.platform.audit.utils.matchers.IsOrderedByTimestamp;
import org.awaitility.Awaitility;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Cucumber test step definitions.
 */
@SuppressWarnings({"PMD.DataflowAnomalyAnalysis", "PMD.BeanMembersShouldSerialize"})
public class BusinessObjectAuditEventsStepDefinitions {
    private final AuditEventsApi client;
    private final List<AuditEventRequest> requests = new LinkedList<>();
    private final ScenarioContext scenarioContext;
    private final ApiClientContext apiClientContext;
    private Map<String, String> queryParams;
    private AuditEventsPage results;

    /**
     * Constructor is invoked for each scenario.
     *
     * @param scenarioContext  shared scenario context, injected by cucumber-picocontainer
     * @param apiClientContext shared context that tracks results of last call
     */
    public BusinessObjectAuditEventsStepDefinitions(
            ScenarioContext scenarioContext, ApiClientContext apiClientContext) {
        this.scenarioContext = scenarioContext;
        this.apiClientContext = apiClientContext;
        var apiClient = new ApiClient();
        apiClient.updateBaseUri(scenarioContext.getBaseUri() + "/api/v1");
        apiClient.setRequestInterceptor(scenarioContext::applyAuthorization);
        client = new AuditEventsApi(apiClient);
    }

    @And("a collection of activity events")
    public void collectionOfActivityEvents(DataTable dataTable) {
        requests.addAll(new ActivityEventTableTransformer().transform(dataTable));
    }

    @And("a collection of state change events")
    public void collectionOfStateChangeEvents(DataTable dataTable) {
        requests.addAll(new StateChangeEventTableTransformer().transform(dataTable));
    }

    @When("the events for the business object are published")
    public void theEventsForTheBusinessObjectArePublished() {
        for (AuditEventRequest r : requests) {
            apiClientContext.capture(
                    () ->
                            client.postEventWithHttpInfo(
                                    scenarioContext.getBusinessObjectMetadata().getType(),
                                    scenarioContext.getBusinessObjectMetadata().getId(),
                                    r));
        }
    }

    @Then("getting business object audit events should return as many events as were published")
    public void theBusinessObjectShouldHaveAsManyEventsAsWerePublished(DataTable data) {
        theBusinessObjectShouldHaveNEvents(requests.size(), data);
    }

    @Then("getting business object audit events should return {int} events")
    public void theBusinessObjectShouldHaveNEvents(int expected, DataTable data) {
        queryParams = data.asMap(String.class, String.class);

        Function<String, OffsetDateTime> asOffset =
                (String key) ->
                        Optional.ofNullable(queryParams.get(key))
                                .map(OffsetDateTime::parse)
                                .orElse(null);

        Function<String, Integer> asInt =
                (String key) ->
                        Optional.ofNullable(queryParams.get(key))
                                .map(Integer::parseInt)
                                .orElse(null);

        Awaitility.await()
                .atMost(Duration.ofMinutes(5))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> {
                            apiClientContext.capture(
                                    () ->
                                            client.getEventsWithHttpInfo(
                                                    scenarioContext
                                                            .getBusinessObjectMetadata()
                                                            .getType(),
                                                    scenarioContext
                                                            .getBusinessObjectMetadata()
                                                            .getId(),
                                                    queryParams.get("sortOrder"),
                                                    queryParams.get("sortby"),
                                                    asOffset.apply("startTime"),
                                                    asOffset.apply("endTime"),
                                                    asInt.apply("pageNumber"),
                                                    asInt.apply("pageSize")));
                            Assertions.assertNotNull(apiClientContext.getLastResponseBody());
                            results = apiClientContext.getLastResponseBody(AuditEventsPage.class);
                            Assertions.assertEquals(
                                    expected, results.getPagingMetadata().getTotalCount());
                        });
    }

    @Then("getting business object audit events should fail with status {int}")
    public void gettingBusinessObjectAuditEventsShouldFailWithStatus(int expected, DataTable data) {
        queryParams = data.asMap(String.class, String.class);

        Function<String, OffsetDateTime> asOffset =
                (String key) ->
                        Optional.ofNullable(queryParams.get(key))
                                .map(OffsetDateTime::parse)
                                .orElse(null);

        Function<String, Integer> asInt =
                (String key) ->
                        Optional.ofNullable(queryParams.get(key))
                                .map(Integer::parseInt)
                                .orElse(null);

        apiClientContext.capture(
                () ->
                        client.getEventsWithHttpInfo(
                                scenarioContext.getBusinessObjectMetadata().getType(),
                                scenarioContext.getBusinessObjectMetadata().getId(),
                                queryParams.get("sortOrder"),
                                queryParams.get("sortby"),
                                asOffset.apply("startTime"),
                                asOffset.apply("endTime"),
                                asInt.apply("pageNumber"),
                                asInt.apply("pageSize")));

        Assertions.assertEquals(expected, apiClientContext.getLastResponseStatus());
    }

    @And("events should be on a single page")
    @And("result should not have a next page")
    public void resultShouldNotHaveANextPage() {
        Assertions.assertNull(results.getPagingMetadata().getNextPage());
    }

    @And("result should have a next page")
    public void resultShouldHaveANextPage() {
        Assertions.assertNotNull(results.getPagingMetadata().getNextPage());
    }

    @And("events should be ordered by timestamp")
    public void eventsShouldBeOrderedByTimestamp() {
        boolean isDescending = "DESC".equals(queryParams.get("sortOrder"));
        MatcherAssert.assertThat(results.getEvents(), new IsOrderedByTimestamp(isDescending));
    }

    @And("events should contain all of the data defined in the requests")
    public void eventsShouldContainAllOfTheDataDefinedInTheRequests() {
        // sort requests and events so they line up to be compared
        var sortedRequests = new ArrayList<>(requests);
        sortedRequests.sort(Comparator.comparing(AuditEventRequest::getTimestamp));

        var sortedEvents = new ArrayList<>(results.getEvents());
        sortedEvents.sort(Comparator.comparing(AuditEvent::getTimestamp));

        for (int i = 0; i < sortedRequests.size(); i++) {
            MatcherAssert.assertThat(
                    sortedEvents.get(i),
                    new AuditEventMatcher(
                            scenarioContext.getBusinessObjectMetadata(), sortedRequests.get(i)));
        }
    }

    @Then("the api response should have status code {int}")
    public void theApiResponseShouldHaveStatusCode(int expectedStatusCode) {
        Assertions.assertEquals(expectedStatusCode, apiClientContext.getLastResponseStatus());
    }
}
