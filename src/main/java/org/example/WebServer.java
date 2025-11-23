package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;

public class WebServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebServer.class);
    protected String docRoot;
    protected int port;
    protected ServerSocket ss;

    static class Handler extends Thread {

        protected Socket socket;
        protected PrintWriter pw;
        protected BufferedOutputStream bos;
        protected BufferedReader br;
        protected File docRoot;

        public Handler(Socket _socket, String _docRoot) throws IOException {
            socket = _socket;
            docRoot = new File(_docRoot).getCanonicalFile();
        }

        public void run() {
            LOGGER.info("Handling request ...");
            try {
                br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                bos = new BufferedOutputStream(socket.getOutputStream());
                pw = new PrintWriter(new OutputStreamWriter(bos));

                String line = br.readLine();

                if (line == null) {
                    socket.close();
                    return;
                }

                if (line.toUpperCase().startsWith("GET")) {
                    StringTokenizer tokens = new StringTokenizer(line, " ?");
                    tokens.nextToken();
                    String req = tokens.nextToken();

                    String name;
                    if (req.startsWith("/")) {
                        name = this.docRoot + req;
                    } else {
                        name = this.docRoot + File.separator + req;
                    }

                    File file = new File(name).getCanonicalFile();

                    if (!file.getAbsolutePath().startsWith(this.docRoot.getAbsolutePath())) {
                        pw.println("HTTP/1.0 403 Forbidden");
                    } else if (!file.exists()) {
                        pw.println("HTTP/1.0 404 Not Found");
                    } else if (!file.canRead()) {
                        pw.println("HTTP/1.0 403 Forbidden");
                    } else if (file.isDirectory()) {
                        //
                    } else {
                        sendFile(bos, pw, file.getAbsolutePath());
                    }


                    pw.flush();
                    bos.flush();
                }

                socket.close();

            } catch (IOException e) {
                LOGGER.error("Error handling request", e);
            }
        }

        protected void sendFile(BufferedOutputStream bos, PrintWriter pw, String filename) throws IOException {
            try {
                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(filename));
                byte[] data = new byte[10 * 1024];
                int read = bis.read(data);

                pw.println("HTTP/1.0 200 Okay");
                pw.println();
                pw.flush();
                bos.flush();

                while (read != -1) {
                    bos.write(data, 0, read);
                    read = bis.read(data);
                }

                bos.flush();
            } catch (Exception e) {
                LOGGER.error("Error reading file", e);
                pw.flush();
                bos.flush();
            }
        }

    }

    public void parseParams(String[] args) {
        if (args.length == 2) {
            this.docRoot = args[0];
            this.port = Integer.parseInt(args[1]);
            return;
        }

        LOGGER.error("Syntax: " + this.getClass().getSimpleName() + " docRoot port");
        System.exit(1);
    }

    public WebServer(String[] args) {
        parseParams(args);

        LOGGER.info("Params verified.");
        LOGGER.info("Starting a server ...");

        try {
            this.ss = new ServerSocket(this.port);
            LOGGER.info("Server started ...");

            while (true) {
                Socket socket = ss.accept();

                new Handler(socket, docRoot).start();
            }

        } catch (IOException e) {
            LOGGER.error("Error starting server");
        }
    }

    public static void main(String[] args) {
        WebServer webServer = new WebServer(args);
    }
}
