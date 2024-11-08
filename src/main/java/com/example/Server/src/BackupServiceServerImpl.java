package com.example.Server.src;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.proto.backupservice.*;
import io.grpc.stub.StreamObserver;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;

public class BackupServiceServerImpl extends BackupServiceServiceGrpc.BackupServiceServiceImplBase {

    private CqlSession session;

    public BackupServiceServerImpl() {
        connect("localhost", 9042);
    }

    private void connect(String node, int port) {
        try {
            session = CqlSession.builder()
                    .addContactPoint(new InetSocketAddress(node, port))
                    .withLocalDatacenter("datacenter1") // Cambia esto según tu configuración
                    .build();
            System.out.println("Conectado a Cassandra en " + node + ":" + port);
            // Mostrar la versión de Cassandra
            String cassandraVersion = session.getMetadata().getNodes().values().iterator().next().getCassandraVersion().toString(); // Obtener la versión de Cassandra
            System.out.println("Version de Cassandra: " + cassandraVersion);
        } catch (Exception e) {
            System.err.println("Error al conectar a Cassandra: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void close() {
        if (session != null) {
            session.close();
            System.out.println("Conexión a Cassandra cerrada.");
        }
    }

    /* Method ADD FILE*/
    @Override
    public void addFile(AddFileRequest request, StreamObserver<AddFileResponse> responseObserver) {
        String fileName = request.getFileName();
        ByteBuffer fileContent = request.getFileContent().asReadOnlyByteBuffer();
        UUID fileId = UUID.randomUUID();

        String inserrQuery = "INSERT INTO backup.files (file_id, filename, file_content) VALUES (?, ?, ?)";
        session.execute(inserrQuery, fileId, fileName, fileContent);

        AddFileResponse response = AddFileResponse.newBuilder()
                .setMessage("Archivo " + fileName + " subio existosamente con Id: " + fileId)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    /*Method LIST FILE*/
    @Override
    public void listFile(ListFileRequest request, StreamObserver<ListFileResponse> responseObserver) {

        try {
            String selectQuery = "SELECT file_id, filename from backup.files";
            List<Row> rows = session.execute(selectQuery).all();

            ListFileResponse.Builder responseBuilder = ListFileResponse.newBuilder();

            for (Row row : rows) {
                String fileId = row.getUuid("file_id").toString();
                String filename = row.getString("filename");

                responseBuilder.addFiles(ListFileResponse.FileInfo.newBuilder()
                        .setFileId(fileId)
                        .setFileName(filename)
                        .build());
            }

            ListFileResponse response = responseBuilder.build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception err) {
            err.printStackTrace();
            responseObserver.onError(err);
        }

    }

    /*Method DELETE FILE*/

    @Override
    public void deleteFile(DeleteFileRequest request, StreamObserver<DeleteFileResponse> responseObserver) {

        String fileId = request.getFileId();
        String fileName = request.getFileName();
        String deleteQuery;

        try {
            if (fileId != null && !fileId.isEmpty()) {
                deleteQuery = "DELETE FROM backup.files WHERE file_id = ?";
                session.execute(SimpleStatement.newInstance(deleteQuery, UUID.fromString(fileId)));
                System.out.println(("Archivo con ID: " + fileId + " eliminado exitosamente"));
            } else if (fileName != null && !fileName.isEmpty()) {
                String selectQuery = "Select file_id FROM backup.files WHERE filename = ?";
                ResultSet resultSet = session.execute(SimpleStatement.newInstance(selectQuery, fileName));

                System.out.println("RESULTSET: " + resultSet);

                // Verificar si el resultado de la consulta es null
                Row row = resultSet.one();
                if (row == null) {
                    throw new IllegalArgumentException("No se encontró un archivo con el nombre: " + fileName);
                }

                //UUID idFromFilename = resultSet.one().getUuid("file_id");
                UUID idFromFilename = row.getUuid("file_id");

                System.out.println("UUID: " + idFromFilename);

                deleteQuery = "DELETE FROM backup.files WHERE file_id = ?";
                session.execute(SimpleStatement.newInstance(deleteQuery, idFromFilename));
                System.out.println("Archivo con nombre: " + fileName + " eliminado exitosamente");
            } else {
                throw new IllegalArgumentException("Debe proporcionarse un file_id o un filename.");
            }

            DeleteFileResponse response = DeleteFileResponse.newBuilder()
                    .setMessage("Archivo eliminado exitosamente")
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception err) {
            responseObserver.onError(new Throwable("Error al eliminar el archivo: " + err.getMessage()));
            err.printStackTrace();
        }
    }

}
