import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PackCapeTexture {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Usage: java scripts/PackCapeTexture.java input.png output.png");
        BufferedImage source = ImageIO.read(Path.of(args[0]).toFile());
        BufferedImage panel = new BufferedImage(20, 32, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = panel.createGraphics();
        graphics.drawImage(source.getScaledInstance(20, 32, Image.SCALE_AREA_AVERAGING), 0, 0, null);
        graphics.dispose();
        BufferedImage atlas = new BufferedImage(128, 64, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 20; x++) {
                atlas.setRGB(2 + x, 2 + y, panel.getRGB(x, y));
                atlas.setRGB(24 + x, 2 + y, panel.getRGB(19 - x, y));
            }
            for (int edge = 0; edge < 2; edge++) {
                atlas.setRGB(edge, 2 + y, panel.getRGB(0, y));
                atlas.setRGB(22 + edge, 2 + y, panel.getRGB(19, y));
            }
        }
        for (int x = 0; x < 20; x++) for (int edge = 0; edge < 2; edge++) {
            atlas.setRGB(2 + x, edge, panel.getRGB(x, 0));
            atlas.setRGB(22 + x, edge, panel.getRGB(19 - x, 31));
        }
        Path output = Path.of(args[1]);
        Files.createDirectories(output.toAbsolutePath().getParent());
        ImageIO.write(atlas, "png", output.toFile());
    }
}
