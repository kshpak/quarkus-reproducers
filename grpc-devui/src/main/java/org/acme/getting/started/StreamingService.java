package org.acme.getting.started;

import com.google.protobuf.Empty;
import io.quarkus.example.HelloReply;
import io.quarkus.example.HelloRequest;
import io.quarkus.example.Streaming;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import java.time.Duration;

@GrpcService
public class StreamingService implements Streaming {

    @Override
    public Multi<HelloReply> source(HelloRequest request){
        return Multi.createFrom().ticks().every(Duration.ofMillis(100))
                .select().first(10)
                .map(l -> HelloReply.newBuilder().setMessage("Helloy " + request.getName()).build());
    }

    @Override
    public Uni<HelloReply> sink(Multi<HelloRequest> request) {
        return request
                .map(HelloRequest::getName)
                .collect().first()
                .map(l -> HelloReply.newBuilder().setMessage("Done").build());
    }

    @Override
    public Multi<HelloReply> pipe(Multi<HelloRequest> request) {
        throw new UnsupportedOperationException();
    }
}