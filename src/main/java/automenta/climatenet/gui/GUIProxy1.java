package automenta.climatenet.gui;

import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import java.util.Date;

@Theme("valo")
//@Push //https://vaadin.com/wiki/-/wiki/Main/Enabling%20server%20push
public class GUIProxy1 implements DynaGUI.UIProxy {


    @Override
    public void init(DynaGUI ui, VaadinRequest r) {
        //System.out.println(vaadinRequest);
        //                JavaScript.getCurrent().execute(textArea.getValue());

        // The root of the component hierarchy
        VerticalLayout content = new VerticalLayout();
        content.setSizeFull(); // Use entire window
        ui.setContent(content);   // Attach to the UI

        // Add some component
        final Label label;
        content.addComponent(label = new Label(this + " " + getClass().getSimpleName() + " " + new Date()));

        // Layout inside layout
        HorizontalLayout hor = new HorizontalLayout();
        hor.setSizeFull(); // Use all available space

        Button b = new Button("Click 22");


        /*b.addClickListener(new Button.ClickListener() {
            @Override public void buttonClick(Button.ClickEvent clickEvent) {
                label.setValue("Clicked");
            }
        });*/
        hor.addComponent(b);
        /*
        // Couple of horizontally laid out components
        Tree tree = new Tree("My Tree",
                TreeExample.createTreeContent());
        hor.addComponent(tree);

        Table table = new Table("My Table",
                TableExample.generateContent());
        table.setSizeFull();
        hor.addComponent(table);
        hor.setExpandRatio(table, 1); // Expand to fill
        */

        content.addComponent(hor);
        content.setExpandRatio(hor, 1); // Expand to fill
    }
}
