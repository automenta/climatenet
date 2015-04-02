package automenta.climatenet.gui;


import com.vaadin.server.VaadinServlet;

import javax.servlet.annotation.WebServlet;

@WebServlet(value = "/*", asyncSupported = true) //this is specified in the web server handler for now
//@VaadinServletConfiguration(productionMode = false/*, ui = GUI.class*/)
public class GUIServlet extends VaadinServlet {
}