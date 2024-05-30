package com.mycompany.bancomvn;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SimpleWebServer {

    private static Connection connection;

    public static void main(String[] args) throws IOException {
        // Conectar a la base de datos
        try {
            connection = DriverManager.getConnection(
                    "jdbc:mysql://127.0.0.1:3306/bancoData", "root", "root");
            System.out.println("Conexión a la base de datos establecida.");
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        // Crear el servidor en el puerto 8080
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // Definir los contextos (rutas) y los manejadores (handlers) que responderán a las peticiones
        server.createContext("/", new MyHandler());
        server.createContext("/registro", new RegistroHandler());
        server.createContext("/traer-registro", new TraerRegistroHandler());

        // Configurar el executor (null indica que usará el executor por defecto)
        server.setExecutor(null);

        // Iniciar el servidor
        server.start();
        System.out.println("Servidor iniciado en el puerto 8080");
    }

    // Crear una clase que implementa HttpHandler para manejar la ruta principal
    static class MyHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange t) throws IOException {
            String response = "Bienvenido al servidor!";
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    // Crear una clase que implementa HttpHandler para manejar el registro
    static class RegistroHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
                BufferedReader br = new BufferedReader(isr);
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                br.close();
                isr.close();

                String requestBody = sb.toString();
                Gson gson = new Gson();
                JsonObject data = gson.fromJson(requestBody, JsonObject.class);

                String nombre = data.get("nombre").getAsString();
                String correo = data.get("correo").getAsString();
                String contrasena = data.get("contrasena").getAsString();
                int edad = data.get("edad").getAsInt();

                String query = "INSERT INTO cliente (nombre, correo, contrasena, edad) VALUES (?, ?, ?, ?)";
                try {
                    PreparedStatement stmt = connection.prepareStatement(query);
                    stmt.setString(1, nombre);
                    stmt.setString(2, correo);
                    stmt.setString(3, contrasena);
                    stmt.setInt(4, edad);
                    stmt.executeUpdate();
                    stmt.close();

                    String response = "Registro exitoso!";
                    exchange.sendResponseHeaders(200, response.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    String response = "Error al registrar el usuario.";
                    exchange.sendResponseHeaders(500, response.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                }
            } else {
                exchange.sendResponseHeaders(405, -1); // 405 Method Not Allowed
            }
        }
    }

    static class TraerRegistroHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                try {
                    // Realizar la consulta SQL para obtener los registros de cliente
                    String query = "SELECT * FROM cliente";
                    PreparedStatement stmt = connection.prepareStatement(query);
                    ResultSet rs = stmt.executeQuery();

                    // Convertir los resultados a JSON
                    JsonArray jsonArray = new JsonArray();
                    while (rs.next()) {
                        JsonObject jsonObject = new JsonObject();
                        jsonObject.addProperty("id", rs.getInt("id"));
                        jsonObject.addProperty("nombre", rs.getString("nombre"));
                        jsonObject.addProperty("correo", rs.getString("correo"));
                        jsonObject.addProperty("edad", rs.getInt("edad"));
                        // Agregar más campos si es necesario
                        jsonArray.add(jsonObject);
                    }

                    // Enviar la respuesta JSON al cliente
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, jsonArray.toString().getBytes().length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(jsonArray.toString().getBytes());
                    os.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    String response = "Error al obtener los registros de cliente.";
                    exchange.sendResponseHeaders(500, response.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                }
            } else {
                exchange.sendResponseHeaders(405, -1); // 405 Method Not Allowed
            }
        }
    }
}
