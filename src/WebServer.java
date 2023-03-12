import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The Web server.
 */
public class WebServer {

  /**
   * The entry point of application.
   *
   * @param args the input arguments
   */
  public static void main(String[] args) {
    if (args.length == 0) {
      System.out.println("How to use: java HTTPServer <port> <directory>");
      System.exit(0);
    }
    int port = Integer.parseInt(args[0]);
    String directory = args[1];
    Path path = Paths.get(directory);
    if (Files.exists(path)) {
      System.out.println("Server source file exists!");
    } else {
      System.out.println("Server source file doesn't exist!");
    }

    HTTPServer server = new HTTPServer(port, directory);
    server.Start();
  }
}