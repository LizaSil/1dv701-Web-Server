import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;

/**
 * The type Http server.
 */

public class HTTPServer {

  private int port;
  private String directory;
  private String serverName;

  /**
   * Instantiates a new Http server.
   *
   * @param port      the port
   * @param directory the directory
   */

  public HTTPServer(int port, String directory) {
    this.port = port;
    this.directory = directory;
    this.serverName = "Pikk_Hermann";
  }

  /**
   * Starts the server.
   */
  public void Start() {
    try (ServerSocket server = new ServerSocket(port)) {
      System.out.println("Server running on http://localhost:" + port);
      while (true) {
        try {
          Socket client = server.accept();
          new Thread(() -> {
            try {
              HandleRequest(client);
            } catch (IOException e) {
              System.out.println("Error handling request: " + e.getMessage());
            }
          }).start();
        } catch (IOException e) {
          System.out.println("Error accepting client connection: " + e.getMessage());
        }
      }
    } catch (IOException e) {
      System.out.println("Error starting server: " + e.getMessage());
    }
  }

  /**
   * Reads the request from the client.
   */
  private String ReadRequest(Socket client) throws IOException {
    InputStreamReader isr = new InputStreamReader(client.getInputStream());
    StringBuilder request = new StringBuilder();
    BufferedReader br = new BufferedReader(isr);

    String line = br.readLine();
    while (!line.isBlank()) {
      request.append(line + "\r\n");
      line = br.readLine();
    }
    return request.toString();
  }

  /**
   * Handles the request from the client.
   * 
   * @param client Socket.
   * 
   * @throws IOException
   */
  private void HandleRequest(Socket client) throws IOException {
    String request = ReadRequest(client);

    switch (request.split(" ")[0]) {
      case "GET":
        HandleGETRequest(client, request);
        break;
      case "POST":
        HandlePOSTRequest(client, request);
        break;
      default:
        System.out.println("Unknown request" + request);
        break;
    }
  }

  /**
   * Handles the GET request from the client.
   * 
   * @param client  Socket.
   * 
   * @param request String.
   */
  private void HandleGETRequest(Socket client, String request) throws IOException {
    String firstline = request.toString().split("\r\n")[0];
    String host = request.toString().split("\r\n")[1];
    String method = firstline.split(" ")[0];
    String resource = firstline.split(" ")[1];
    String version = firstline.split(" ")[2];
    String clientIP = client.getInetAddress().toString();
    String clientPort = Integer.toString(client.getLocalPort());
    Timestamp ts = new Timestamp(System.currentTimeMillis());
    OutputStream stream = client.getOutputStream();
    Path p = getFilePath(resource);

    if (resource.endsWith("test") || resource.endsWith("test/")) { // redirect
      Redirect(client, "http://google.com", "test");
      String ServerErrorResponseInfo = String.format(
          "Client: %s:%s, Version: HTTP/1.0, Response: 302 Redirect, Date: %s, Server: %s, Connection: closed\n",
          clientIP,
          clientPort, ts, serverName);
      System.out.println(ServerErrorResponseInfo);

    } else if (resource.endsWith("error.html") || resource.endsWith("error.html/")) { // 500 error simulation
      int ten = 10;
      int zero = 0;
      int result;
      try {
        // This will throw an ArithmeticException
        result = ten / zero;
        // This will not be executed
        byte[] fileBytes = Files.readAllBytes(p);
        stream.write(fileBytes);
        stream.flush();

      } catch (ArithmeticException e) {
        stream.write("HTTP/1.1 500 Internal Server Error\r\n\r\n".getBytes());
        stream.write("<title> 500 Internal Server Error </title>  <h1> 500 Internal Server Error </h1>".getBytes());
        stream.flush();
        stream.close();
        String ServerErrorResponseInfo = String.format(
            "Client: %s:%s, Method: %s, Path: %s, Version: %s, Response: 500 Internal Server Error, Date: %s, Server: %s, Connection: closed\n",
            clientIP,
            clientPort, method, resource, version, ts, serverName);

        System.out.println(ServerErrorResponseInfo);

      }
    } else {
      if ((Files.exists(p) && !Files.isDirectory(p))) { // 200 Ok
        String contentType = Files.probeContentType(p);
        long contentLength = Files.size(p);
        ts = new Timestamp(System.currentTimeMillis());

        if (contentType != null) {
          String responseHeader = String.format(
              "HTTP/1.1 200 OK\r\nContent-Type: %s\r\nContent-Length: %s\r\nServer: %s\r\nDate: %s\r\nConnection: closed\r\n\r\n",
              contentType, contentLength, serverName, ts);
          stream.write(responseHeader.getBytes());

          String requestHeader = String.format("%s, Method: %s, Path: %s, Version: %s", host,
              method, resource,
              version);

          System.out.println(requestHeader);
          System.out.println("Server request file exists!");

          String responseInfo = String.format(
              "Client: %s:%s, Version: HTTP/1.0, Response 200 OK, Date: %s, Server: %s, Content-Length: %d, Connection: closed, Content-Type: %s\n",
              clientIP,
              clientPort, ts, serverName, contentLength, contentType);

          System.out.println(responseInfo);
        } else {
          // Content type is null
          stream.write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
          stream.write("<title> 404 Not Found </title>  <h1> 404 Not Found </h1>".getBytes());
          stream.flush();
          stream.close();
        }
        byte[] fileBytes = Files.readAllBytes(p);
        stream.write(fileBytes);
        stream.flush();
        stream.close();

      } else if ((Files.exists(p) && !Files.isDirectory(p))) { // 500 Server error
        stream.write("HTTP/1.1 500 Internal Server Error\r\n\r\n".getBytes());
        stream.write("<title> 500 Internal Server Error </title>  <h1> 500 Internal Server Error </h1>".getBytes());
        String ServerErrorResponseInfo = String.format(
            "Client: %s:%s, Method: %s, Path: %s, Version: %s, Response: 500 Internal Server Error, Date: %s, Server: , Connection: closed\n",
            clientIP,
            clientPort, method, resource, version, ts, serverName);
        System.out.println(ServerErrorResponseInfo);

      } else {
        stream.write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes()); // 404 Not found error
        stream.write("<title> 404 Not Found </title>  <h1> 404 Not Found </h1>".getBytes());
        String NotFoundErrorResponseInfo = String.format(
            "Client: %s:%s, Method: %s, Path: %s, Version: %s, Response: 500 Internal Server Error, Date: %s, Server: , Connection: closed\n",
            clientIP,
            clientPort, method, resource, version, ts, serverName);

        System.out.println(NotFoundErrorResponseInfo);
        stream.flush();
        stream.close();

      }
    }
  }

  private void HandlePOSTRequest(Socket client, String request) throws IOException {
    // Not implemented
  }

  /**
   * Redirects the client to the specified location. And sends a 302 response.
   * 
   * @param client   Socket.
   * 
   * @param location String.
   * 
   */
  private void Redirect(Socket client, String location, String source) throws IOException {
    OutputStream outputStream = client.getOutputStream();
    outputStream.write(
        String.format("HTTP/1.1 302 Redirect\r\nLocation: %s\r\n\r\n", location).getBytes());
    outputStream.flush();
    outputStream.close();
    System.out.println(String.format("%s, Method: %s, Path: %s, Version: %s", client.getInetAddress().toString(),
        "GET", source, "HTTP/1.1"));
    System.out.println("Redirecting to " + location);
  }

  /**
   * Gets the file path from the resource.
   */
  private Path getFilePath(String resource) {
    String r = directory + resource;

    if (r.endsWith("/")) {
      r += "index.html";
      System.out.println("Requested item is a directory!");
    } else if (!r.contains(".")) {
      r += "/index.html";
      System.out.println("Requested item is a directory!");
    }

    return Paths.get(r);
  }
}
