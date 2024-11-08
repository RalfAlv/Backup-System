package com.example.Client.src;

import com.proto.backupservice.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Scanner;

public class BackupServiceClient {
    public static void main(String[] args) {
        BackupServiceClient client = new BackupServiceClient("localhost", 50051);
        client.startCLI();
    }

    private ManagedChannel channel;
    private BackupServiceServiceGrpc.BackupServiceServiceBlockingStub blockingStub;

    public BackupServiceClient(String host, int port) {
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        blockingStub = BackupServiceServiceGrpc.newBlockingStub(channel);
    }

    /*ADD FILE*/
    public void addFile(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                System.out.println("Error: El archivo no existe en la ruta especificada");
                return;
            }

            byte[] fileBytes = Files.readAllBytes(file.toPath());
            int blockSize = 1024 * 1024; //1MB

            System.out.println("Subiendo Archivos en bloques de 1MB ...");

            for (int i = 0; i < fileBytes.length; i += blockSize) {
                int end = Math.min(fileBytes.length, i + blockSize);
                byte[] block = new byte[end - i];
                System.arraycopy(fileBytes, i, block, 0, block.length);

                AddFileRequest request = AddFileRequest.newBuilder()
                        .setFileName(file.getName())
                        .setFileContent(com.google.protobuf.ByteString.copyFrom(block))
                        .build();

                AddFileResponse response = blockingStub.addFile(request);
                System.out.println("Servidor: " + response.getMessage());
            }
            System.out.println("Archivo " + file.getName() + " Subido Exitosamente");

        } catch (IOException err) {
            System.out.println("Error al leer el archivo: " + err.getMessage());
        }

    }

    /*Method LIST FILES ..*/
    public void listFiles() {
        ListFileRequest request = ListFileRequest.newBuilder().build();
        try {
            ListFileResponse response = blockingStub.listFile(request);
            for (ListFileResponse.FileInfo fileInfo : response.getFilesList()) {
                System.out.println("FileId: " + fileInfo.getFileId());
                System.out.println("FileName: " + fileInfo.getFileName());
                System.out.println("________-------________-------________");
            }
        } catch (StatusRuntimeException err) {
            System.err.println("Error al recuperar los archivos: " + err.getStatus());
        }
    }

    /*Method DELETE FILE*/
    public void deleteFile(String fileId, String fileName) {
        DeleteFileRequest request = DeleteFileRequest.newBuilder()
                .setFileId(fileId != null ? fileId : "")
                .setFileName(fileName != null ? fileName : "")
                .build();
        try {
            DeleteFileResponse response = blockingStub.deleteFile(request);
            System.out.println(response.getMessage()); // Muestra el mensaje de confirmación
        } catch (StatusRuntimeException e) {
            System.err.println("Error al eliminar el archivo: " + e.getStatus());
        }
    }

    /*CLI*/

    public void startCLI() {
        Scanner Sentinel = new Scanner(System.in);
        System.out.println("CLI de BackupAlvarezBartolo iniciado. Escribe 'help' para ver los comandos disponibles.");

        while (true) {
            System.out.print("Alv>");
            String commandLine = Sentinel.nextLine();
            String[] commandParts = commandLine.split(" ");
            String command = commandParts[0];

            switch (command) {
                case "help":
                    printHelp();
                    break;
                case "add_file":
                    if (commandParts.length < 2) {
                        System.out.println("Error: Debes especificar la ruta del archivo. Uso: add_file <ruta_del_archivo>");
                    } else {
                        addFile(commandParts[1]);
                    }
                    break;
                case "ls":
                    listFiles();
                    break;
                case "rm":
                    if (commandParts.length == 2) {
                        String input = commandParts[1].trim();  // Eliminamos espacios adicionales
                        if (input.isEmpty()) {
                            System.out.println("Uso incorrecto del comando. Usa: rm <file_id>");
                        } else {
                            if (input.matches("[0-9a-fA-F-]{36}")) {
                                deleteFile(input, null);
                            } else {
                                deleteFile(null, input);
                            }
                        }
                    } else {
                        System.out.println("Uso incorrecto del comando. Usa: rm <file_id>");
                    }
                    break;
                case "exit":
                    System.out.println("Saliendo de la aplicacion");
                    channel.shutdown();
                    return;
                default:
                    System.out.print("");
            }
        }

    }

    /*HELP*/
    private void printHelp() {
        System.out.println("Comandos disponibles:");
        System.out.println("help           - Muestra esta ayuda.");
        System.out.println("add_file       - Sube un archivo al servidor. Uso: add_file <ruta_del_archivo>");
        System.out.println("ls     - Muestra todos los archivos subidos en la base de datos");
        System.out.println("rm    - Elimina un archivo. Uso: rm <file_id>");
        System.out.println("exit           - Cierra la aplicación.");
    }


}
