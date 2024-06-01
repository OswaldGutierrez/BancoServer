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
        server.createContext("/transacciones", new TransaccionesHandler());

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

                String query = "SELECT numeroCuenta, estado, saldo FROM cliente WHERE correo = ? AND contrasena = ?;";
                try {
                    PreparedStatement stmt = connection.prepareStatement(query);
                    stmt.setString(1, correo);
                    stmt.setString(2, contrasena);
                    ResultSet rs = stmt.executeQuery();

                    if (rs.next()) {
                        JsonObject responseJson = new JsonObject();
                        responseJson.addProperty("numeroCuenta", rs.getString("numeroCuenta"));
                        responseJson.addProperty("estado", rs.getString("estado"));
                        responseJson.addProperty("saldo", rs.getDouble("saldo"));

                        sendResponse(exchange, 200, responseJson.toString());
                    } else {
                        JsonObject responseJson = new JsonObject();
                        responseJson.addProperty("mensaje", "Correo o contraseña incorrectos");
                        sendResponse(exchange, 400, responseJson.toString());
                    }
                    rs.close();
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    JsonObject responseJson = new JsonObject();
                    responseJson.addProperty("mensaje", "Error al hacer login.");
                    sendResponse(exchange, 500, responseJson.toString());
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
            headers.add("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, response.getBytes().length);
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

                double numeroCuenta = data.get("numeroCuenta").getAsDouble();
                double saldo = data.get("saldo").getAsDouble();

                if (saldo > 50000) {
                    // Realizar la actualización del saldo en la base de datos
                    try {
                        String query = "INSERT INTO consignaciones(clienteId, valorConsignacion, fecha, tipo) VALUES"
                                + "((SELECT id FROM cliente WHERE numeroCuenta = ?), ?, NOW(), 'consignacion')";
                        PreparedStatement stmtInsert = connection.prepareStatement(query);
                        stmtInsert.setDouble(1, numeroCuenta);
                        stmtInsert.setDouble(2, saldo);
                        int affectedRows = stmtInsert.executeUpdate();
                        stmtInsert.close();
                        if (affectedRows == 0) {
                            String response = "Falla al crear la consignación";
                            sendResponse(exchange, 404, response);
                        }

                        String queryUpdate = "UPDATE cliente SET saldo = saldo + ? - (? * 0.01) WHERE numeroCuenta = ?";
                        PreparedStatement stmt = connection.prepareStatement(queryUpdate);
                        stmt.setDouble(1, saldo);
                        stmt.setDouble(2, saldo);
                        stmt.setDouble(3, numeroCuenta);
                        int affectedRows1 = stmt.executeUpdate();
                        stmt.close();
                        if (affectedRows1 > 0) {
                            String response = "Consignación exitosa!";
                            sendResponse(exchange, 200, response);
                        } else {
                            String response = "Número de cuenta no encontrado.";
                            sendResponse(exchange, 404, response);
                        }

                        String queryUpdateImpuestosMov = "UPDATE cliente SET impuestosMov = impuestosMov + (? * 0.01) WHERE numeroCuenta = ?";
                        PreparedStatement stmtUpdateImpuestosMov = connection.prepareStatement(queryUpdateImpuestosMov);
                        stmtUpdateImpuestosMov.setDouble(1, saldo);
                        stmtUpdateImpuestosMov.setDouble(2, numeroCuenta);
                        int affectedRows2 = stmtUpdateImpuestosMov.executeUpdate();
                        stmtUpdateImpuestosMov.close();
                        if (affectedRows2 > 0) {
                            String response = "Retiro exitoso!";
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
                    // Realizar la actualización del saldo en la base de datos
                    try {
                        String query = "INSERT INTO consignaciones(clienteId, valorConsignacion, fecha, tipo) VALUES"
                                + "((SELECT id FROM cliente WHERE numeroCuenta = ?), ?, NOW(), 'consignacion')";
                        PreparedStatement stmtInsert = connection.prepareStatement(query);
                        stmtInsert.setDouble(1, numeroCuenta);
                        stmtInsert.setDouble(2, saldo);
                        int affectedRows = stmtInsert.executeUpdate();
                        stmtInsert.close();
                        if (affectedRows == 0) {
                            String response = "Falla al crear la consignación";
                            sendResponse(exchange, 404, response);
                        }

                        String queryUpdate = "UPDATE cliente SET saldo = saldo + ? - (100) WHERE numeroCuenta = ?";
                        PreparedStatement stmt = connection.prepareStatement(queryUpdate);
                        stmt.setDouble(1, saldo);
                        stmt.setDouble(2, numeroCuenta);
                        int affectedRows1 = stmt.executeUpdate();
                        stmt.close();
                        if (affectedRows1 > 0) {
                            String response = "Consignación exitosa!";
                            sendResponse(exchange, 200, response);
                        } else {
                            String response = "Número de cuenta no encontrado.";
                            sendResponse(exchange, 404, response);
                        }

                        String queryUpdateImpuestosMov = "UPDATE cliente SET impuestosMov = impuestosMov + (100) WHERE numeroCuenta = ?";
                        PreparedStatement stmtUpdateImpuestosMov = connection.prepareStatement(queryUpdateImpuestosMov);
                        stmtUpdateImpuestosMov.setDouble(1, numeroCuenta);
                        int affectedRows2 = stmtUpdateImpuestosMov.executeUpdate();
                        stmtUpdateImpuestosMov.close();
                        if (affectedRows2 > 0) {
                            String response = "Retiro exitoso!";
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

                double numeroCuentaRetirar = data.get("numeroCuentaRetirar").getAsDouble();
                double saldoRetirar = data.get("saldoRetirar").getAsDouble();

                if (saldoRetirar > 50000) {
                    try {
                        String query = "INSERT INTO consignaciones(clienteId, valorConsignacion, fecha, tipo) VALUES"
                                + "((SELECT id FROM cliente WHERE numeroCuenta = ?), ?, NOW(), 'retiro')";
                        PreparedStatement stmtInsert = connection.prepareStatement(query);
                        stmtInsert.setDouble(1, numeroCuentaRetirar);
                        stmtInsert.setDouble(2, saldoRetirar);
                        int affectedRows = stmtInsert.executeUpdate();
                        stmtInsert.close();
                        if (affectedRows == 0) {
                            String response = "Falla al crear la consignación";
                            sendResponse(exchange, 404, response);
                        }

                        String queryUpdate = "UPDATE cliente SET saldo = saldo - ? - (? * 0.01) WHERE numeroCuenta = ?";
                        PreparedStatement stmtUpdate = connection.prepareStatement(queryUpdate);
                        stmtUpdate.setDouble(1, saldoRetirar);
                        stmtUpdate.setDouble(2, saldoRetirar);
                        stmtUpdate.setDouble(3, numeroCuentaRetirar);
                        int affectedRows1 = stmtUpdate.executeUpdate();
                        stmtUpdate.close();
                        if (affectedRows1 > 0) {
                            String response = "Retiro exitoso!";
                            sendResponse(exchange, 200, response);
                        } else {
                            String response = "Número de cuenta no encontrado.";
                            sendResponse(exchange, 404, response);
                        }

                        String queryUpdateImpuestosMov = "UPDATE cliente SET impuestosMov = impuestosMov + (? * 0.01) WHERE numeroCuenta = ?";
                        PreparedStatement stmtUpdateImpuestosMov = connection.prepareStatement(queryUpdateImpuestosMov);
                        stmtUpdateImpuestosMov.setDouble(1, saldoRetirar);
                        stmtUpdateImpuestosMov.setDouble(2, numeroCuentaRetirar);
                        int affectedRows2 = stmtUpdateImpuestosMov.executeUpdate();
                        stmtUpdateImpuestosMov.close();
                        if (affectedRows2 > 0) {
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
                    try {
                        String query = "INSERT INTO consignaciones(clienteId, valorConsignacion, fecha, tipo) VALUES"
                                + "((SELECT id FROM cliente WHERE numeroCuenta = ?), ?, NOW(), 'retiro')";
                        PreparedStatement stmtInsert = connection.prepareStatement(query);
                        stmtInsert.setDouble(1, numeroCuentaRetirar);
                        stmtInsert.setDouble(2, saldoRetirar);
                        int affectedRows = stmtInsert.executeUpdate();
                        stmtInsert.close();
                        if (affectedRows == 0) {
                            String response = "Falla al crear la consignación";
                            sendResponse(exchange, 404, response);
                        }

                        String queryUpdate = "UPDATE cliente SET saldo = saldo - ? - (100) WHERE numeroCuenta = ?";
                        PreparedStatement stmtUpdate = connection.prepareStatement(queryUpdate);
                        stmtUpdate.setDouble(1, saldoRetirar);
                        stmtUpdate.setDouble(2, numeroCuentaRetirar);
                        int affectedRows1 = stmtUpdate.executeUpdate();
                        stmtUpdate.close();
                        if (affectedRows1 > 0) {
                            String response = "Retiro exitoso!";
                            sendResponse(exchange, 200, response);
                        } else {
                            String response = "Número de cuenta no encontrado.";
                            sendResponse(exchange, 404, response);
                        }

                        String queryUpdateImpuestosMov = "UPDATE cliente SET impuestosMov = impuestosMov + (100) WHERE numeroCuenta = ?";
                        PreparedStatement stmtUpdateImpuestosMov = connection.prepareStatement(queryUpdateImpuestosMov);
                        stmtUpdateImpuestosMov.setDouble(1, numeroCuentaRetirar);
                        int affectedRows2 = stmtUpdateImpuestosMov.executeUpdate();
                        stmtUpdateImpuestosMov.close();
                        if (affectedRows2 > 0) {
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
                }

                // Realizar la actualización del saldo en la base de datos
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

    // Clase para manejar la búsqueda de transacciones por mes
    // Clase para manejar la búsqueda de transacciones por mes
    static class TransaccionesHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Manejo de solicitudes OPTIONS para CORS
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleOptions(exchange);
                return;
            }

            if ("GET".equals(exchange.getRequestMethod())) {
                // Obtener el mes de la query string
                String query = exchange.getRequestURI().getQuery();
                String mes = null;
                if (query != null && query.startsWith("mes=")) {
                    mes = query.substring(4);
                }

                if (mes == null || mes.isEmpty()) {
                    sendResponse(exchange, 400, "Debe seleccionar un mes válido.");
                    return;
                }

                try {
                    // Realizar la consulta SQL para obtener las transacciones del mes
                    String queryTransacciones = "SELECT id, clienteId, valorConsignacion, fecha, tipo "
                            + "FROM consignaciones "
                            + "WHERE MONTH(fecha) = ?";
                    PreparedStatement stmt = connection.prepareStatement(queryTransacciones);
                    stmt.setString(1, obtenerNumeroMes(mes));
                    ResultSet rs = stmt.executeQuery();

                    // Convertir los resultados a JSON
                    JsonArray jsonArray = new JsonArray();
                    while (rs.next()) {
                        JsonObject jsonObject = new JsonObject();
                        jsonObject.addProperty("id", rs.getInt("id"));
                        jsonObject.addProperty("clienteId", rs.getInt("clienteId"));
                        jsonObject.addProperty("valorConsignacion", rs.getDouble("valorConsignacion"));
                        jsonObject.addProperty("fecha", rs.getString("fecha"));
                        jsonObject.addProperty("tipo", rs.getString("tipo"));
                        jsonArray.add(jsonObject);
                    }

                    // Enviar la respuesta JSON al cliente
                    String jsonResponse = jsonArray.toString();
                    sendResponse(exchange, 200, jsonResponse);

                    // Cerrar recursos
                    rs.close();
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    String response = "Error al obtener las transacciones.";
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
            exchange.sendResponseHeaders(statusCode, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }

        private String obtenerNumeroMes(String mes) {
            switch (mes) {
                case "enero":
                    return "01";
                case "febrero":
                    return "02";
                case "marzo":
                    return "03";
                case "abril":
                    return "04";
                case "mayo":
                    return "05";
                case "junio":
                    return "06";
                default:
                    return "01"; // Por defecto, enero
            }
        }
    }

}
