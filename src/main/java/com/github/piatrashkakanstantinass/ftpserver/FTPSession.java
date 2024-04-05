package com.github.piatrashkakanstantinass.ftpserver;

import com.github.piatrashkakanstantinass.ftpserver.common.CommandParser;
import com.github.piatrashkakanstantinass.ftpserver.common.CommandProcessingException;
import com.github.piatrashkakanstantinass.ftpserver.common.FTPSessionState;
import com.github.piatrashkakanstantinass.ftpserver.common.ReplyType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Logger;
import java.util.logging.Level;

public class FTPSession implements Runnable {
    private final Socket socket;
    private static final Logger logger = Logger.getLogger(FTPSession.class.getName());
    private static final String SERVICE_READY_MSG = "Service ready";
    private static final FTPSessionState state = new FTPSessionState();

    public FTPSession(Socket socket) {
        this.socket = socket;
    }

    private String getSocketString() {
        return String.format("%s:%d", socket.getInetAddress().getHostAddress(), socket.getPort());
    }

    private String genSocketStringLogMessage(String msg) {
        return String.format("%s %s", this.getSocketString(), msg);
    }

    private String genResponse(ReplyType replyType, String message) {
        return String.format("%d %s", replyType.getValue(), message);
    }

    public void run() {
        logger.info(this.genSocketStringLogMessage("initiated a new session"));
        try {
            var inputStream = socket.getInputStream();
            var outputStream = socket.getOutputStream();
            var reader = new BufferedReader(new InputStreamReader(inputStream));
            var writter = new PrintWriter(outputStream, true);

            writter.println(genResponse(ReplyType.SERVICE_READY, SERVICE_READY_MSG));

            while (true) {
                var line = reader.readLine();
                if (line == null) break;
                logger.info(this.genSocketStringLogMessage(String.format("Got message: %s", line)));
                try {
                    var command = CommandParser.parse(line);
                    var reply = command.process(state);
                    writter.println(genResponse(reply.getReplyType(), reply.getMessage()));
                } catch (CommandProcessingException e) {
                    writter.println(genResponse(e.getReply(), e.getMessage()));
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage());
        } finally {
            logger.info(this.genSocketStringLogMessage("closed session"));
        }
    }
}
