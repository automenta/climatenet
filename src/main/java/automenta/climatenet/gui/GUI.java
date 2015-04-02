package automenta.climatenet.gui;

import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.*;


public class GUI extends UI {

    @Override
    protected void init(VaadinRequest vaadinRequest) {
        System.out.println(vaadinRequest);
        // The root of the component hierarchy
        VerticalLayout content = new VerticalLayout();
        content.setSizeFull(); // Use entire window
        setContent(content);   // Attach to the UI

        // Add some component
        final Label label;
        content.addComponent(label = new Label("Hello!"));

        // Layout inside layout
        HorizontalLayout hor = new HorizontalLayout();
        hor.setSizeFull(); // Use all available space

        Button b = new Button("Click This");
        b.addClickListener(new Button.ClickListener() {
            @Override public void buttonClick(Button.ClickEvent clickEvent) {
                label.setValue("Clicked");
            }
        });
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
