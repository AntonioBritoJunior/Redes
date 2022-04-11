import java.io.*;
import java.net.*;
import java.util.*;

public final class WebServer
{
    public static void main(String argv[]) throws Exception
    {
        //Setting the port number:
        int port = 6789;
        
        //Establish the listen socket:
        ServerSocket socket = new ServerSocket(port);
        
        //Process HTTP service requestes in an infinite loop:
        while(true){
            //Waiting for a TCP request.
            Socket sckt = socket.accept();

            //Construct an object to process the HTTP request message.
            HttpRequest request = new HttpRequest(sckt);
            
            //Create a new thread to process the request.
            Thread thread = new Thread (request);
            
            //Start the thread.
            thread.start();
        }  
    }
}

final class HttpRequest implements Runnable
{
    final static String CRLF = "\r\n";
    Socket socket;
    
    //Constructor.
    public HttpRequest (Socket socket) throws Exception
    {
        this.socket = socket;
    }
    
    public void run()
    {
        try{
            processRequest();    
        } catch (Exception e){
            System.out.println(e);
        }
    }
    
    private void processRequest() throws Exception
    {
        InputStream is = socket.getInputStream();
        DataOutputStream os = new DataOutputStream(socket.getOutputStream());
        
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String requestLine = br.readLine();
        
        // Extract the filename from the request line.
        StringTokenizer tokens = new StringTokenizer(requestLine);
        tokens.nextToken();  // skip over the method, which should be "GET"
        String fileName = tokens.nextToken();
        
        // Prepend a "." so that file request is within the current directory.
        fileName = "." + fileName;
        
        // Open the requested file.
        FileInputStream fis = null;
        boolean fileExists = true;
        try {
	    fis = new FileInputStream(fileName);
        } catch (FileNotFoundException e) {
	    fileExists = false;
        }
        
        // Construct the response message.
        String statusLine = null;
        String contentTypeLine = null;
        String entityBody = null;
        if (fileExists) {
	    statusLine = "HTTP/1.0 200 OK" + CRLF;
	    contentTypeLine = "Content-Type: " + 
		contentType(fileName) + CRLF;
        } else {
	    statusLine = "HTTP/1.0 404 Not Found" + CRLF;
	    contentTypeLine = "Content-Type: text/html" + CRLF;
	    entityBody = "<HTML>" + 
		"<HEAD><TITLE>Not Found</TITLE></HEAD>" +
		"<BODY>Not Found</BODY></HTML>";
        }
        
        // Send the status line.
        os.writeBytes(statusLine);

        // Send the content type line.
        os.writeBytes(contentTypeLine);

        // Send a blank line to indicate the end of the header lines.
        os.writeBytes(CRLF);

        // Send the entity body.
        if (fileExists) {
	    sendBytes(fis, os);
	    fis.close();
        } else {
	    os.writeBytes(entityBody) ;
        }
        
        os.close();
        br.close();
        socket.close();
    }
    
    private static void sendBytes(FileInputStream fis, 
				  OutputStream os) throws Exception {
	// Construct a 1K buffer to hold bytes on their way to the socket.
	byte[] buffer = new byte[1024];
	int bytes = 0;
	
	// Copy requested file into the socket's output stream.
	while ((bytes = fis.read(buffer)) != -1) {
	    os.write(buffer, 0, bytes);
	}
    }

    private static String contentType(String fileName) {
	if(fileName.endsWith(".htm") || fileName.endsWith(".html")) {
	    return "text/html";
	}
	if(fileName.endsWith(".ram") || fileName.endsWith(".ra")) {
	    return "audio/x-pn-realaudio";
	}
	return "application/octet-stream";
    }
}