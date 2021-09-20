package org.acme.infinispan.client;

import io.quarkus.infinispan.client.Remote;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.commons.util.CloseableIterator;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;
import org.infinispan.query.dsl.QueryResult;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Path("/items")
public class ShopItemResource {

    private static final int CACHE_BATCH_SIZE = 50;

    private AtomicInteger invocationsCounter = new AtomicInteger(0);


    @Inject
    @Remote("mycache")
    RemoteCache<String, ShopItem> cache_items;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ShopItem createItem(ShopItem item, @QueryParam("lifespan") String lifespan, @QueryParam("maxIdleTime") String maxIdleTime){
        if(!cache_items.containsValue(item)){
            String key = item.getTitle().replaceAll("\\s+","").toLowerCase();
            if (lifespan != null && maxIdleTime != null) {
                cache_items.put(key, item,
                        Integer.valueOf(lifespan), TimeUnit.SECONDS, Integer.valueOf(maxIdleTime), TimeUnit.SECONDS);
                System.out.println("Idle add called");
            }
            else if (lifespan != null && maxIdleTime == null) {
                cache_items.put(key, item,
                        Integer.valueOf(lifespan), TimeUnit.SECONDS);
            }
            else {
                cache_items.put(key, item);
            }
        }
        return item;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<ShopItem> listItems(@QueryParam("query") String query){
        QueryFactory qf = org.infinispan.client.hotrod.Search.getQueryFactory(cache_items);
        Query<ShopItem> searchQuery;
        if (query != null) {
            searchQuery = qf.create(query);
        }
        else {
            searchQuery = qf.create("from quarkus_qe.ShopItem");
        }
        QueryResult<ShopItem> queryResult = searchQuery.execute();
        return queryResult.list();
    }

    @GET
    @Path("/list")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ShopItem> listItemsNew(){
        List<ShopItem> items = new ArrayList<>();
        try(CloseableIterator<Map.Entry<Object, Object>> iterator = cache_items.retrieveEntries(null, CACHE_BATCH_SIZE)){
            iterator.forEachRemaining(entry -> items.add((ShopItem) entry.getValue()));
        }
        return items;
    }


    @Path("{key}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ShopItem getCachedValue(@PathParam("key") String key) {
        ShopItem item = cache_items.get(key);
        invocationsCounter.incrementAndGet();
        return item;
    }

    @Path("/invocation-counter")
    @GET
    public boolean isInvocationThresholdReached(){
        if (invocationsCounter.get() == 10)
            return true;
        return false;
    }

    /*
        List<ShopItem> items = new ArrayList<>();
        try(CloseableIterator<Map.Entry<Object, Object>> iterator = cache_items.retrieveEntries(null, CACHE_BATCH_SIZE)){
            iterator.forEachRemaining(entry -> items.add((ShopItem) entry.getValue()));
        }
     */
}
