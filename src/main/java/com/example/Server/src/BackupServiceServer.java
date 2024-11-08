package com.example.Server.src;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

public class BackupServiceServer {
    public static void main(String[] args) throws IOException, InterruptedException {

        BackupServiceServerImpl backupServiceServer = new BackupServiceServerImpl();

        Server server = ServerBuilder.forPort(50051)
                .addService(backupServiceServer)
                .build();

        server.start();
        System.out.println("...Start GRPC SERVER ...");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Received shutdown Request");
            server.shutdown();
            System.out.println("Successfully stoped the server");
        }));

        server.awaitTermination();
    }
}
