package automenta.climatenet.gui;

import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.UI;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;

/**
 * Proxies to a dynamic classloaded UI implementation, for fast reload during dev
 */
@Theme("valo")
public class DynaGUI extends UI {

    private String clss = "automenta.climatenet.gui.GUIProxy1"; //TODO this will be a parameter


    public interface UIProxy {
        void init(DynaGUI ui, VaadinRequest r);
    }



    public static class FuckThisClassLoaderShit extends ClassLoader {

        public final Class c;

        public FuckThisClassLoaderShit(String pathToClass) throws IOException {

            FileInputStream fileInputStream = new FileInputStream(pathToClass);
            // create FileInputStream object
            //InputStream fileInputStream = this.getClass().getClassLoader().getResourceAsStream(pathToClass);

		/*
		 * Create byte array large enough to hold the content of the file. Use
		 * fileInputStream.available() to determine size of the file in bytes.
		 */
            byte rawBytes[] = new byte[fileInputStream.available()];

		/*
		 * To read content of the file in byte array, use int read(byte[]
		 * byteArray) method of java FileInputStream class.
		 */
            fileInputStream.read(rawBytes);

            // Load the target class
            c = this.defineClass(rawBytes, 0, rawBytes.length);

        }
    }

    @Override
    protected void init(VaadinRequest vaadinRequest) {


        //Loading classes from different sources
        //jcl.add("myjar.jar");
        //jcl.add(new URL("http://myserver.com/myjar.jar"));
        //jcl.add(new FileInputStream("myotherjar.jar"));
        //jcl.add("myjarlib/"); //Recursively load all jar files in the folder/sub-folder(s)
        try {

            FuckThisClassLoaderShit x = new FuckThisClassLoaderShit("./target/classes/automenta/climatenet/gui/GUIProxy1.class");
            UIProxy obj = (UIProxy) (x.c).newInstance();
            obj.init(this, vaadinRequest);


        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

//    private synchronized void initFactory() {
//
//    }
}
