package im.tox.tox4j.gui;

public final class ToxGuiClient {

    public static void main(String[] args) throws Exception {
//        for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
//            if ("Nimbus".equals(info.getName())) {
//                UIManager.setLookAndFeel(info.getClassName());
//                break;
//            }
//        }

        ToxGui dialog = new ToxGui();
        dialog.setSize(800,600);
        //dialog.pack();
        dialog.setVisible(true);
    }

}
