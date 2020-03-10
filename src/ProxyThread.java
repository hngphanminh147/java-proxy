import java.net.*;
import java.io.*;

public class ProxyThread extends Thread {

    private Socket clientSocket = null;
    private Socket wserverSocket = null;

    private final int BUFFER_SIZE = 32768;
    private final char CR = 13; // \r
    private final char LF = 10; // \n

    private boolean inBlackList = false;

    private BufferedReader fromBrowser = null;
    private DataOutputStream toBrowser = null;

    private DataInputStream fromServer = null;
    private DataOutputStream toServer = null;

    private String hostName = null;
    private StringBuilder requestMsg = null;

    public ProxyThread(Socket socket) {
        super("ProxyThread");
        this.clientSocket = socket;
    }

    public void getRequestMessage() throws IOException {

        System.out.println("\n[IP: " + clientSocket.getInetAddress().toString() + "]");

        fromBrowser = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        requestMsg = new StringBuilder();
        String line;
        int contentLenght = 0;

        while ((line = fromBrowser.readLine()) != null) {

            if (line.equals(""))
                break;

            if (line.startsWith("Content-Lenght:")) {
                int index = line.indexOf(":") + 1;
                String len = line.substring(index).trim();
                contentLenght = Integer.parseInt(len);
            }

            if (line.startsWith("Host:")) {
                int index = line.indexOf(":") + 1;
                hostName = line.substring(index).trim();
            }

            requestMsg.append(line);
            requestMsg.append(CR);
            requestMsg.append(LF);
            if (hostName != null)
                if (hostName.indexOf("google.com") != -1) {
                    requestMsg.append(CR);
                    requestMsg.append(LF);
                }
        }

        requestMsg.append(CR);
        requestMsg.append(LF);

        if (contentLenght > 0) {
            int read;
            StringBuilder body = new StringBuilder();

            while ((read = fromBrowser.read()) != -1) {
                body.append((char) read);
                if (body.length() == contentLenght)
                    break;
            }
            requestMsg.append(body);
        }
    }

    public void checkHost() {
        if (BlackList.isOpen)
            if (BlackList.inList(hostName))
                inBlackList = true;
    }

    public void send403Message() throws IOException {
        toBrowser = new DataOutputStream(clientSocket.getOutputStream());
        StringBuilder responseMsg = new StringBuilder();

        responseMsg.append("HTTP/1.1 403 Forbidden");
        responseMsg.append(CR);
        responseMsg.append(LF);

        responseMsg.append(CR);
        responseMsg.append(LF);

        responseMsg.append("<!DOCTYPE html>\n");
        responseMsg.append("<html>\n");
        responseMsg.append("	<head>\n");
        responseMsg.append("		<title>403 Forbidden</title>\n");
        responseMsg.append("	</head>\n");
        responseMsg.append("	<body>\n");
        responseMsg.append("		<center><h1>403 Forbidden</h1>\n");
        responseMsg.append("		<p>This host in Black list (blacklist.conf).</p></center>\n");
        responseMsg.append("	</body>\n");
        responseMsg.append("</html>");

        toBrowser.writeBytes(responseMsg.toString());
    }

    public void sendRequestToServer() throws IOException {

        wserverSocket = new Socket(hostName, 80);

        toServer = new DataOutputStream(wserverSocket.getOutputStream());
        toServer.writeBytes(requestMsg.toString());
    }

    public void getResponseToBrowser() throws IOException {
        fromServer = new DataInputStream(wserverSocket.getInputStream());
        toBrowser = new DataOutputStream(clientSocket.getOutputStream());

        int count;
        byte[] by = new byte[BUFFER_SIZE];

        while ((count = fromServer.read(by)) > 0) {
            toBrowser.write(by, 0, count);
        }

        toBrowser.flush();
    }

    public void closeStreams() throws IOException {
        if (fromBrowser != null) fromBrowser.close();
        if (toBrowser != null) toBrowser.close();
        if (fromServer != null) fromServer.close();
        if (toServer != null) toServer.close();

        if (wserverSocket != null)
            if (!wserverSocket.isClosed())
                wserverSocket.close();
    }

    public void run() {

        try {
            getRequestMessage();
            System.out.println(requestMsg);
            System.out.println("---> OK: Get request successfully.");
        } catch (IOException e1) {
            System.out.println("---> ERROR: Get request failed.");
            return;
        }

        checkHost();

        if (inBlackList) {
            try {
                send403Message();
                closeStreams();
                System.out.println("---> HOST IN BLACKLIST: Send 403 message.");
            } catch (IOException e2) {
                System.out.println("---> ERROR: Cannot send 403 message.");
                return;
            }
        } else {
            try {
                sendRequestToServer();
                System.out.println("---> HOST NOT IN BLACKLIST: Send request to web server successfully.");
            } catch (IOException e) {
                System.out.println("---> ERROR: Send request to web server failed.");
                return;
            }

            try {
                getResponseToBrowser();
                System.out.println("---> OK: Send response to browser successfully.");
            } catch (IOException e) {
                System.out.println("---> ERROR: Send response to browser failed.");
                return;
            }

            try {
                closeStreams();
            } catch (IOException e) {
                System.out.println("---> ERROR: Unidentified.");
                return;
            }
        }
    }
}
