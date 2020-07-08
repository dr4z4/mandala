package cz.czechitas.mandala;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import javax.imageio.*;
import javax.swing.*;
import javax.swing.border.*;
import net.miginfocom.swing.*;
import net.sevecek.util.*;

public class HlavniOkno extends JFrame {

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner non-commercial license
    JButton btnOtevrit;
    JButton btnUlozit;
    JLabel labAktualniBarva;
    JRadioButton radBtnBarva1;
    JRadioButton radBtnBarva2;
    JRadioButton radBtnBarva3;
    JRadioButton radBtnBarva4;
    JRadioButton radBtnBarva5;
    JLabel labObrazek;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
    JPanel contentPane;
    MigLayout migLayoutManager;
    BufferedImage obrazek;
    JFileChooser dialog = new JFileChooser(".");
    Graphics2D stetec;




    public HlavniOkno() {
        initComponents();

        ButtonGroup group = new ButtonGroup();
        group.add(radBtnBarva1);
        group.add(radBtnBarva2);
        group.add(radBtnBarva3);
        group.add(radBtnBarva4);
        group.add(radBtnBarva5);

        obrazek = new BufferedImage(400, 300, BufferedImage.TYPE_3BYTE_BGR);
//        labObrazek.setIcon(new ImageIcon(obrazek));
//        labObrazek.setText("");
//        labObrazek.setBackground(null);
        
        stetec = (Graphics2D) obrazek.getGraphics();
        stetec.setColor(Color.WHITE);
        stetec.fillRect(0, 0, obrazek.getWidth(), obrazek.getHeight());

//        nahrajVychoziObrazek();
    }

    private void otevritObrazek() {
        int vysledek = dialog.showOpenDialog(this);
        if (vysledek != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File soubor = dialog.getSelectedFile();
        nahrajObrazekZeSouboru(soubor);

        // Zvetsi okno presne na obrazek
        pack();
        setMinimumSize(getSize());
    }


    private void nahrajObrazekZeSouboru(File soubor) {
        try {
            obrazek = ImageIO.read(soubor);
            labObrazek.setIcon(new ImageIcon(obrazek));
            labObrazek.setMinimumSize(new Dimension(obrazek.getWidth(), obrazek.getHeight()));
        } catch (IOException ex) {
            throw new ApplicationPublicException(ex, "Nepodařilo se nahrát obrázek mandaly ze souboru " + soubor.getAbsolutePath());
        }
    }

    private void ulozitJako() {
        int vysledek = dialog.showSaveDialog(this);
        if (vysledek != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File soubor = dialog.getSelectedFile();

        // Dopln pripadne chybejici priponu .png
        if (!soubor.getName().contains(".") && !soubor.exists()) {
            soubor = new File(soubor.getParentFile(), soubor.getName() + ".png");
        }

        // Opravdu prepsat existujici soubor?
        if (soubor.exists()) {
            int potvrzeni = JOptionPane.showConfirmDialog(this, "Soubor " + soubor.getName() + " už existuje.\nChcete jej přepsat?", "Přepsat soubor?", JOptionPane.YES_NO_OPTION);
            if (potvrzeni == JOptionPane.NO_OPTION) {
                return;
            }
        }

        ulozObrazekDoSouboru(soubor);
    }

    private void ulozObrazekDoSouboru(File soubor) {
        try {
            ImageIO.write(obrazek, "png", soubor);
        } catch (IOException ex) {
            throw new ApplicationPublicException(ex, "Nepodařilo se uložit obrázek mandaly do souboru " + soubor.getAbsolutePath());
        }
    }
    
//    private void priStiskuLabBarva(MouseEvent e) {
//        JLabel label = (JLabel) e.getSource();
//        Color barva = label.getBackground();
//        stetec.setColor(barva);
////        label.setText("X");
//
//    }

    private void priKliknutiNaLabPlocha(MouseEvent e) {
        nakresliBod(e.getX(), e.getY());
    }

    private void nakresliBod(int x, int y) {
        vyplnObrazek(obrazek, x, y, stetec.getColor());
        labObrazek.repaint();
    }

//    private void nahrajVychoziObrazek() {
//        try {
//            InputStream zdrojObrazku = getClass().getResourceAsStream("/cz/czechitas/mandala/vychozi-mandala.png");
//            obrazek = ImageIO.read(zdrojObrazku);
//            labObrazek.setIcon(new ImageIcon(obrazek));
//            labObrazek.setMinimumSize(new Dimension(obrazek.getWidth(), obrazek.getHeight()));
//        } catch (IOException ex) {
//            throw new ApplicationPublicException(ex, "Nepodařilo se nahrát zabudovaný obrázek mandaly:\n\n" + ex.getMessage());
//        }
//    }

    /**
     * Vyplni <code>BufferedImage obrazek</code>
     * na pozicich <code>int x</code>, <code>int y</code>
     * barvou <code>Color barva</code>
     */
    public void vyplnObrazek(BufferedImage obrazek, int x, int y, Color barva) {
        if (barva == null) {
            barva = new Color(255, 255, 0);
        }

        // Zamez vyplnovani mimo rozsah
        if (x < 0 || x >= obrazek.getWidth() || y < 0 || y >= obrazek.getHeight()) {
            return;
        }

        WritableRaster pixely = obrazek.getRaster();
        int[] novyPixel = new int[] {barva.getRed(), barva.getGreen(), barva.getBlue(), barva.getAlpha()};
        int[] staryPixel = new int[] {255, 255, 255, 255};
        staryPixel = pixely.getPixel(x, y, staryPixel);

        // Pokud uz je pocatecni pixel obarven na cilovou barvu, nic nemen
        if (pixelyMajiStejnouBarvu(novyPixel, staryPixel)) {
            return;
        }

        // Zamez prebarveni cerne cary
        int[] cernyPixel = new int[] {0, 0, 0, staryPixel[3]};
        if (pixelyMajiStejnouBarvu(cernyPixel, staryPixel)) {
            return;
        }

        vyplnovaciSmycka(pixely, x, y, novyPixel, staryPixel);
    }

    /**
     * Provede skutecne vyplneni pomoci zasobniku
     */
    private void vyplnovaciSmycka(WritableRaster raster, int x, int y, int[] novaBarva, int[] nahrazovanaBarva) {
        Rectangle rozmery = raster.getBounds();
        int[] aktualniBarva = new int[] {255, 255, 255, 255};

        Deque<Point> zasobnik = new ArrayDeque<>(rozmery.width * rozmery.height);
        zasobnik.push(new Point(x, y));
        while (zasobnik.size() > 0) {
            Point point = zasobnik.pop();
            x = point.x;
            y = point.y;
            if (!pixelyMajiStejnouBarvu(raster.getPixel(x, y, aktualniBarva), nahrazovanaBarva)) {
                continue;
            }

            // Najdi levou zed, po ceste vyplnuj
            int levaZed = x;
            do {
                raster.setPixel(levaZed, y, novaBarva);
                levaZed--;
            }
            while (levaZed >= 0 && pixelyMajiStejnouBarvu(raster.getPixel(levaZed, y, aktualniBarva), nahrazovanaBarva));
            levaZed++;

            // Najdi pravou zed, po ceste vyplnuj
            int pravaZed = x;
            do {
                raster.setPixel(pravaZed, y, novaBarva);
                pravaZed++;
            }
            while (pravaZed < rozmery.width && pixelyMajiStejnouBarvu(raster.getPixel(pravaZed, y, aktualniBarva), nahrazovanaBarva));
            pravaZed--;

            // Pridej na zasobnik body nahore a dole
            for (int i = levaZed; i <= pravaZed; i++) {
                if (y > 0 && pixelyMajiStejnouBarvu(raster.getPixel(i, y - 1, aktualniBarva), nahrazovanaBarva)) {
                    if (!(i > levaZed && i < pravaZed
                            && pixelyMajiStejnouBarvu(raster.getPixel(i - 1, y - 1, aktualniBarva), nahrazovanaBarva)
                            && pixelyMajiStejnouBarvu(raster.getPixel(i + 1, y - 1, aktualniBarva), nahrazovanaBarva))) {
                        zasobnik.add(new Point(i, y - 1));
                    }
                }
                if (y < rozmery.height - 1 && pixelyMajiStejnouBarvu(raster.getPixel(i, y + 1, aktualniBarva), nahrazovanaBarva)) {
                    if (!(i > levaZed && i < pravaZed
                            && pixelyMajiStejnouBarvu(raster.getPixel(i - 1, y + 1, aktualniBarva), nahrazovanaBarva)
                            && pixelyMajiStejnouBarvu(raster.getPixel(i + 1, y + 1, aktualniBarva), nahrazovanaBarva))) {
                        zasobnik.add(new Point(i, y + 1));
                    }
                }
            }
        }
    }

    /**
     * Vrati true pokud RGB hodnoty v polich jsou stejne. Pokud jsou ruzne, vraci false
     */
    private boolean pixelyMajiStejnouBarvu(int[] barva1, int[] barva2) {
        return barva1[0] == barva2[0] && barva1[1] == barva2[1] && barva1[2] == barva2[2];
    }

    private void priKliknutiNaBtnUlozit(MouseEvent e) {
        ulozitJako();
    }

    private void priKliknutiNaBtnOtevrit(MouseEvent e) {
        otevritObrazek();
    }

    private void priStiskuRadBtnBarva(MouseEvent e) {
            JRadioButton label = (JRadioButton) e.getSource();
            Color barva = label.getBackground();
            stetec.setColor(barva);
    }


        private void initComponents() {
            // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
            // Generated using JFormDesigner non-commercial license
            btnOtevrit = new JButton();
            btnUlozit = new JButton();
            labAktualniBarva = new JLabel();
            radBtnBarva1 = new JRadioButton();
            radBtnBarva2 = new JRadioButton();
            radBtnBarva3 = new JRadioButton();
            radBtnBarva4 = new JRadioButton();
            radBtnBarva5 = new JRadioButton();
            labObrazek = new JLabel();

            //======== this ========
            setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            setTitle("Mandala");
            Container contentPane = getContentPane();
            contentPane.setLayout(new MigLayout(
                "insets rel,hidemode 3",
                // columns
                "[fill]" +
                "[fill]" +
                "[fill]" +
                "[fill]" +
                "[fill]" +
                "[fill]" +
                "[grow,fill]" +
                "[grow,fill]" +
                "[fill]" +
                "[fill]" +
                "[fill]" +
                "[fill]",
                // rows
                "[]" +
                "[]" +
                "[grow,fill]"));
            this.contentPane = (JPanel) this.getContentPane();
            this.contentPane.setBackground(this.getBackground());
            LayoutManager layout = this.contentPane.getLayout();
            if (layout instanceof MigLayout) {
                this.migLayoutManager = (MigLayout) layout;
            }

            //---- btnOtevrit ----
            btnOtevrit.setText("Otev\u0159\u00edt soubor");
            btnOtevrit.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    priKliknutiNaBtnOtevrit(e);
                }
            });
            contentPane.add(btnOtevrit, "cell 0 0 6 1,growx");

            //---- btnUlozit ----
            btnUlozit.setText("Ulo\u017eit soubor");
            btnUlozit.setPreferredSize(new Dimension(133, 30));
            btnUlozit.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    priKliknutiNaBtnUlozit(e);
                }
            });
            contentPane.add(btnUlozit, "cell 0 0 6 1,growx");

            //---- labAktualniBarva ----
            labAktualniBarva.setText("Aktu\u00e1ln\u00ed barva:");
            contentPane.add(labAktualniBarva, "cell 0 1");

            //---- radBtnBarva1 ----
            radBtnBarva1.setPreferredSize(new Dimension(32, 32));
            radBtnBarva1.setBackground(new Color(145, 194, 20));
            radBtnBarva1.setHorizontalAlignment(SwingConstants.CENTER);
            radBtnBarva1.setBorder(new BevelBorder(BevelBorder.RAISED));
            radBtnBarva1.setForeground(Color.white);
            radBtnBarva1.setMargin(new Insets(2, 2, 2, 2));
            radBtnBarva1.setBorderPainted(true);
            radBtnBarva1.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    priStiskuRadBtnBarva(e);
                }
            });
            contentPane.add(radBtnBarva1, "cell 1 1");

            //---- radBtnBarva2 ----
            radBtnBarva2.setPreferredSize(new Dimension(32, 32));
            radBtnBarva2.setBackground(new Color(201, 179, 22));
            radBtnBarva2.setHorizontalAlignment(SwingConstants.CENTER);
            radBtnBarva2.setBorder(new BevelBorder(BevelBorder.RAISED));
            radBtnBarva2.setForeground(Color.white);
            radBtnBarva2.setMargin(new Insets(2, 2, 2, 2));
            radBtnBarva2.setBorderPainted(true);
            radBtnBarva2.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    priStiskuRadBtnBarva(e);
                }
            });
            contentPane.add(radBtnBarva2, "cell 2 1");

            //---- radBtnBarva3 ----
            radBtnBarva3.setPreferredSize(new Dimension(32, 32));
            radBtnBarva3.setBackground(new Color(179, 129, 29));
            radBtnBarva3.setHorizontalAlignment(SwingConstants.CENTER);
            radBtnBarva3.setBorder(new BevelBorder(BevelBorder.RAISED));
            radBtnBarva3.setForeground(Color.white);
            radBtnBarva3.setMargin(new Insets(2, 2, 2, 2));
            radBtnBarva3.setBorderPainted(true);
            radBtnBarva3.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    priStiskuRadBtnBarva(e);
                }
            });
            contentPane.add(radBtnBarva3, "cell 3 1");

            //---- radBtnBarva4 ----
            radBtnBarva4.setPreferredSize(new Dimension(32, 32));
            radBtnBarva4.setBackground(new Color(201, 95, 22));
            radBtnBarva4.setHorizontalAlignment(SwingConstants.CENTER);
            radBtnBarva4.setBorder(new BevelBorder(BevelBorder.RAISED));
            radBtnBarva4.setForeground(Color.white);
            radBtnBarva4.setMargin(new Insets(2, 2, 2, 2));
            radBtnBarva4.setBorderPainted(true);
            radBtnBarva4.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    priStiskuRadBtnBarva(e);
                }
            });
            contentPane.add(radBtnBarva4, "cell 4 1");

            //---- radBtnBarva5 ----
            radBtnBarva5.setPreferredSize(new Dimension(32, 32));
            radBtnBarva5.setBackground(new Color(194, 43, 25));
            radBtnBarva5.setHorizontalAlignment(SwingConstants.CENTER);
            radBtnBarva5.setBorder(new BevelBorder(BevelBorder.RAISED));
            radBtnBarva5.setForeground(Color.white);
            radBtnBarva5.setMargin(new Insets(2, 2, 2, 2));
            radBtnBarva5.setBorderPainted(true);
            radBtnBarva5.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    priStiskuRadBtnBarva(e);
                }
            });
            contentPane.add(radBtnBarva5, "cell 5 1");

            //---- labObrazek ----
            labObrazek.setBackground(Color.white);
            labObrazek.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    priKliknutiNaLabPlocha(e);
                }
            });
            contentPane.add(labObrazek, "cell 0 2 8 1,align left top,grow 0 0");
            setSize(677, 754);
            setLocationRelativeTo(null);
            // JFormDesigner - End of component initialization  //GEN-END:initComponents
        }

}
