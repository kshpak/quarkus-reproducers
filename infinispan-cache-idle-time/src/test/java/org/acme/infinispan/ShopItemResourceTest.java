package org.acme.infinispan;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.acme.infinispan.client.ShopItem;
import org.apache.http.HttpStatus;
import org.awaitility.Durations;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;

import static io.restassured.RestAssured.post;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsString;
import static org.awaitility.Awaitility.await;
import static io.restassured.RestAssured.get;

import java.time.Duration;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ShopItemResourceTest {

    private static final int CACHE_ENTRY_MAX = 5;
    private static final int CACHE_LIFESPAN_SEC = 10;
    private static final int CACHE_IDLE_TIME_SEC = 10;
    private static final String ALL = null;

    private Response response;

    private List<ShopItem> maxThresholdItemList = Arrays.asList(
            new ShopItem("Item 1", 100, ShopItem.Type.ELECTRONIC),
            new ShopItem("Item 2", 200, ShopItem.Type.ELECTRONIC),
            new ShopItem("Item 3", 300, ShopItem.Type.ELECTRONIC),
            new ShopItem("Item 4", 400, ShopItem.Type.MECHANICAL),
            new ShopItem("Item 5", 500, ShopItem.Type.MECHANICAL)
    );

    @AfterEach
    public void beforeEach() {
        System.out.println("BeforeEach method called");
        clearCache();
    }

    @Test
    @Disabled
    @Order(1)
    public void testCacheSizeEviction() {
        ShopItem additionalItem = new ShopItem("Item 6", 600, ShopItem.Type.MECHANICAL);
        whenAddCacheItems(maxThresholdItemList);
        whenAddCacheItems(Arrays.asList(additionalItem));
        whenQueryCachedItems(ALL);
        thenCacheSizeMustBe(is(CACHE_ENTRY_MAX));
        thenCacheBodyMust(containsString("Item 6"));
    }

    @Test
    @Disabled
    @Order(2)
    public void testCacheEvictionByLifespan() {
        whenAddCacheItemsWithLifespan(maxThresholdItemList, CACHE_LIFESPAN_SEC);

        await().atLeast(Duration.ofSeconds(CACHE_LIFESPAN_SEC)).atMost(Duration.ofSeconds(CACHE_LIFESPAN_SEC + 5))
                .pollInterval(Durations.ONE_SECOND).untilAsserted(() -> {
                    whenQueryCachedItems(ALL);
                    thenCacheIsEmpty();
            }
        );
    }

    @Test
    @Disabled
    @Order(3)
    public void testQueryOnSerializedObjects() {
        whenAddCacheItems(maxThresholdItemList);
        whenQueryCachedItems("from quarkus_qe.ShopItem where type = \"ELEC\"");
        thenCacheSizeMustBe(is(3));
        thenCacheBodyMust(not(containsString("MECHANICAL")));
    }


    /**
        THIS method should remove entries after idle time.
     */
    @Test
    @Order(4)
    public void testCacheEvictionByLifespanAndIdleTime() throws InterruptedException {
        whenAddCacheItemsWithLifespanAndIdleTime(maxThresholdItemList, CACHE_LIFESPAN_SEC + 120 , CACHE_IDLE_TIME_SEC);
        Thread.sleep(12 * 1000);
        whenQueryCachedItems(ALL);
        thenCacheIsEmpty();
    }

    private void clearCache(){
        given().auth().digest("admin", "password").when().post("http://localhost:11222/rest/v2/caches/mycache?action=clear").then().statusCode(HttpStatus.SC_NO_CONTENT);
    }

    private void whenAddCacheItems(List<ShopItem> items){
        items.forEach(item ->
                given()
                        .header("Content-Type", "application/json")
                        .body(item).when()
                        .post("/items")
                        .then().statusCode(HttpStatus.SC_OK)
        );
    }

    private void whenAddCacheItemsWithLifespan(List<ShopItem> items, int lifespan) {
        items.forEach(item ->
                given()
                        .header("Content-Type", "application/json")
                        .queryParam("lifespan", lifespan)
                        .body(item).when()
                        .post("/items")
                        .then().statusCode(HttpStatus.SC_OK)
        );
        System.out.println("Add completed");
    }

    private void whenAddCacheItemsWithLifespanAndIdleTime(List<ShopItem> items, int lifespan, int idleTime){
        items.forEach(item ->
                given()
                        .header("Content-Type", "application/json")
                        .queryParam("lifespan", lifespan)
                        .queryParam("maxIdleTime", idleTime)
                        .body(item).when()
                        .post("/items")
                        .then().statusCode(HttpStatus.SC_OK)
        );
    }

    private void whenQueryCachedItems(String query){
        if(query != null){
            response = given().queryParam("query", query).get("/items");
        } else {
            response = given().get("/items");
        }
    }

    private void thenCacheSizeMustBe(Matcher<?> matcher){
        response.then().body("size()", matcher);
    }

    private void thenCacheBodyMust(Matcher<?> matcher){
        response.then().body(matcher);
    }

    private void thenCacheIsEmpty(){
        response.then().body("isEmpty()", is(true));
    }

}
