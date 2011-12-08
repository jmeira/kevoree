package org.kevoree.library.javase.webserver.servlet;

import org.kevoree.library.javase.webserver.KevoreeHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Locale;

/**
 * Created by IntelliJ IDEA.
 * User: duke
 * Date: 07/12/11
 * Time: 13:28
 * To change this template use File | Settings | File Templates.
 */
public class KevoreeServletResponse implements HttpServletResponse {

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    final private ByteArrayOutputStream stream = new  ByteArrayOutputStream();
    final private PrintWriter writer = new PrintWriter(stream);
    final ServletOutputStream servletStream = new ServletOutputStream(){
        @Override
        public void write(int i) throws IOException {
            stream.write(i);
        }
    };

    @Override
    public String getCharacterEncoding() {
        return "UTF-8";
    }

    @Override
    public String getContentType() {
        return "text/html";
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return servletStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return writer;
    }

    @Override
    public void setCharacterEncoding(String s) {
    }

    @Override
    public void setContentLength(int i) {
    }

    @Override
    public void setContentType(String s) {

    }

    @Override
    public void setBufferSize(int i) {
    }

    @Override
    public int getBufferSize() {
        return 0;
    }

    @Override
    public void flushBuffer() throws IOException {
       stream.flush();
    }

    @Override
    public void resetBuffer() {
        stream.reset();
    }

    @Override
    public boolean isCommitted() {
        return true;
    }

    @Override
    public void reset() {
        stream.reset();
    }

    @Override
    public void setLocale(Locale locale) {
    }

    @Override
    public Locale getLocale() {
        return null;
    }

    public void populateKevoreeResponse(KevoreeHttpResponse response){
        logger.debug("Set ContentRaw {}",stream.toByteArray());
        try {
            stream.flush();
            response.setRawContent(stream.toByteArray());
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    @Override
    public void addCookie(Cookie cookie) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean containsHeader(String name) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String encodeURL(String url) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String encodeRedirectURL(String url) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String encodeUrl(String url) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String encodeRedirectUrl(String url) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void sendError(int sc) throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setDateHeader(String name, long date) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addDateHeader(String name, long date) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setHeader(String name, String value) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addHeader(String name, String value) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setIntHeader(String name, int value) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addIntHeader(String name, int value) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setStatus(int sc) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setStatus(int sc, String sm) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getStatus() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getHeader(String name) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Collection<String> getHeaderNames() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}