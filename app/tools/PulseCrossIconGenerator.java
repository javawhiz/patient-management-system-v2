import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.List;
import javax.imageio.ImageIO;

public final class PulseCrossIconGenerator {
    private static final Color TEAL = Color.decode("#0F766E");
    private static final Color TEAL_LIGHT = Color.decode("#14B8A6");
    private static final Color YELLOW = Color.decode("#FDE68A");

    public static void main(String[] args) throws Exception {
        File output = new File(args[0]);
        output.mkdirs();
        int[] sizes = {16, 32, 48, 64, 128, 256, 512, 1024};
        for (int size : sizes) {
            ImageIO.write(render(size), "png", new File(output, size + ".png"));
        }
        writeIco(output, new File(output, "pulse-cross.ico"));
    }

    private static BufferedImage render(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double scale = size / 1024.0;
        graphics.scale(scale, scale);
        graphics.setColor(TEAL);
        graphics.fillRoundRect(0, 0, 1024, 1024, 448, 448);
        graphics.setColor(new Color(TEAL_LIGHT.getRed(), TEAL_LIGHT.getGreen(), TEAL_LIGHT.getBlue(), 89));
        graphics.fillRect(256, 256, 512, 512);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(424, 224, 176, 576);
        graphics.fillRect(248, 400, 528, 224);
        Path2D pulse = new Path2D.Double();
        pulse.moveTo(176, 548);
        pulse.lineTo(288, 548);
        pulse.lineTo(342, 440);
        pulse.lineTo(420, 636);
        pulse.lineTo(506, 384);
        pulse.lineTo(582, 548);
        pulse.lineTo(736, 548);
        graphics.setColor(YELLOW);
        graphics.setStroke(new BasicStroke(28, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.draw(pulse);
        graphics.dispose();
        return image;
    }

    private static void writeIco(File directory, File target) throws Exception {
        List<Integer> sizes = List.of(16, 32, 48, 64, 128, 256);
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(target))) {
            out.writeShort(0);
            out.writeShort(1);
            out.writeShort(sizes.size());
            int offset = 6 + sizes.size() * 16;
            byte[][] pngs = new byte[sizes.size()][];
            for (int i = 0; i < sizes.size(); i++) {
                pngs[i] = Files.readAllBytes(new File(directory, sizes.get(i) + ".png").toPath());
                int size = sizes.get(i);
                out.writeByte(size == 256 ? 0 : size);
                out.writeByte(size == 256 ? 0 : size);
                out.writeByte(0);
                out.writeByte(0);
                out.writeShort(1);
                out.writeShort(32);
                out.writeInt(pngs[i].length);
                out.writeInt(offset);
                offset += pngs[i].length;
            }
            for (byte[] png : pngs) {
                out.write(png);
            }
        }
    }
}
