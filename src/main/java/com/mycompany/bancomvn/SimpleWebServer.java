package com.mycompany.bancomvn;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.*;
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
        server.createContext("/registro", new registroHandler());
        server.createContext("/traer-registro", new traerRegistroHandler());
        server.createContext("/login", new loginHandler());
        server.createContext("/consignar", new consignarHandler());
        server.createContext("/retirar", new retirarHandler());

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
    static class registroHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Manejo de solicitudes OPTIONS para CORS
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleOptions(exchange);
                return;
            }

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
                    sendResponse(exchange, 200, response);
                } catch (SQLException e) {
                    e.printStackTrace();
                    String response = "Error al registrar el usuario.";
                    sendResponse(exchange, 500, response);
                }
            } else {
                sendResponse(exchange, 405, "Method Not Allowed"); // 405 Method Not Allowed
            }
        }

        private void handleOptions(HttpExchange exchange) throws IOException {
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type");
            exchange.sendResponseHeaders(204, -1);
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(statusCode, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    static class traerRegistroHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Manejo de solicitudes OPTIONS para CORS
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleOptions(exchange);
                return;
            }

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
                    String jsonResponse = jsonArray.toString();
                    sendResponse(exchange, 200, jsonResponse);
                } catch (SQLException e) {
                    e.printStackTrace();
                    String response = "Error al obtener los registros de cliente.";
                    sendResponse(exchange, 500, response);
                }
            } else {
                sendResponse(exchange, 405, "Method Not Allowed"); // 405 Method Not Allowed
            }
        }

        private void handleOptions(HttpExchange exchange) throws IOException {
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type");
            exchange.sendResponseHeaders(204, -1);
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(statusCode, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    static class loginHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Manejo de solicitudes OPTIONS para CORS
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleOptions(exchange);
                return;
            }

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

                String correo = data.get("correo").getAsString();
                String contrasena = data.get("contrasena").getAsString();

                String queryCorreo = "SELECT correo FROM cliente WHERE correo = ?;";
                try {
                    PreparedStatement stmt = connection.prepareStatement(queryCorreo);                // Permite hacer la consulta en la BD
                    stmt.setString(1, correo);
                    ResultSet rs = stmt.executeQuery();
                    if (!rs.isBeforeFirst()) {
                        sendResponse(exchange, 400, "Correo NO encontrado");
                    }

                    String queryContrasena = "SELECT contrasena FROM cliente WHERE contrasena = ? AND correo = ?;";
                    PreparedStatement stmt1 = connection.prepareStatement(queryContrasena);                // Permite hacer la consulta en la BD
                    stmt1.setString(1, contrasena);
                    stmt1.setString(2, correo);
                    ResultSet rs1 = stmt1.executeQuery();
                    if (!rs1.isBeforeFirst()) {
                        sendResponse(exchange, 400, "Contrasena incorrecta");
                    }

                    sendResponse(exchange, 200, "Ingreso exitoso");
                } catch (SQLException e) {
                    e.printStackTrace();
                    String response = "Error al hacer login.";
                    sendResponse(exchange, 500, response);
                }
            } else {
                sendResponse(exchange, 405, "Method Not Allowed"); // 405 Method Not Allowed
            }
        }

        private void handleOptions(HttpExchange exchange) throws IOException {
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type");
            exchange.sendResponseHeaders(204, -1);
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(statusCode, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    
    
    static class consignarHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Manejo de solicitudes OPTIONS para CORS
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleOptions(exchange);
                return;
            }

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

                int numeroCuenta = data.get("numeroCuenta").getAsInt();
                int saldo = data.get("saldo").getAsInt();

                // Realizar la actualización del saldo en la base de datos
                String query = "UPDATE cliente SET saldo = saldo + ? WHERE numeroCuenta = ?";
                try {
                    PreparedStatement stmt = connection.prepareStatement(query);
                    stmt.setInt(1, saldo);
                    stmt.setInt(2, numeroCuenta);
                    int affectedRows = stmt.executeUpdate();
                    stmt.close();

                    if (affectedRows > 0) {
                        String response = "Consignación exitosa!";
                        sendResponse(exchange, 200, response);
                    } else {
                        String response = "Número de cuenta no encontrado.";
                        sendResponse(exchange, 404, response);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    String response = "Error al realizar la consignación.";
                    sendResponse(exchange, 500, response);
                }
            } else {
                sendResponse(exchange, 405, "Method Not Allowed"); // 405 Method Not Allowed
            }
        }

        private void handleOptions(HttpExchange exchange) throws IOException {
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type");
            exchange.sendResponseHeaders(204, -1);
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            byte[] responseBytes = response.getBytes("UTF-8");
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(responseBytes);
            os.close();
        }
    }
    
    
    
    static class retirarHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Manejo de solicitudes OPTIONS para CORS
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleOptions(exchange);
                return;
            }

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

                int numeroCuentaRetirar = data.get("numeroCuentaRetirar").getAsInt();
                int saldoRetirar = data.get("saldoRetirar").getAsInt();

                // Realizar la actualización del saldo en la base de datos
                String query = "UPDATE cliente SET saldo = saldo - ? WHERE numeroCuenta = ?";
                try {
                    PreparedStatement stmt = connection.prepareStatement(query);
                    stmt.setInt(1, saldoRetirar);
                    stmt.setInt(2, numeroCuentaRetirar);
                    int affectedRows = stmt.executeUpdate();
                    stmt.close();

                    if (affectedRows > 0) {
                        String response = "Retiro exitoso!";
                        sendResponse(exchange, 200, response);
                    } else {
                        String response = "Número de cuenta no encontrado.";
                        sendResponse(exchange, 404, response);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    String response = "Error al realizar el retiro.";
                    sendResponse(exchange, 500, response);
                }
            } else {
                sendResponse(exchange, 405, "Method Not Allowed"); // 405 Method Not Allowed
            }
        }

        private void handleOptions(HttpExchange exchange) throws IOException {
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type");
            exchange.sendResponseHeaders(204, -1);
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            byte[] responseBytes = response.getBytes("UTF-8");
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(responseBytes);
            os.close();
        }
    }

}
