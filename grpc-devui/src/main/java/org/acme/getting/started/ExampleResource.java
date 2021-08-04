package org.acme.getting.started;

import io.quarkus.example.Greeter;
import io.quarkus.example.HelloRequest;
import io.quarkus.example.MutinyGreeterGrpc;
import io.quarkus.example.Streaming;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/hello")
public class ExampleResource {

    @GrpcClient
    Greeter hello;

    @GrpcClient
    Streaming streaming;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "hello";
    }

    @GET
    @Path("/{name}")
    public Uni<String> hello(@PathParam("name") String name) {
        return hello.sayHello(HelloRequest.newBuilder().setName(name).build())
                .onItem().transform(helloReply -> helloReply.getMessage());
    }

    @GET
    @Path("/multi/{name}")
    public Multi<String> multiHello(@PathParam("name") String name){
        return streaming.source(HelloRequest.newBuilder().setName(name).build())
                .onItem().transform(helloReply -> helloReply.getMessage());
    }
}
