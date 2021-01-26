package seamcarving

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

fun main(args: Array<String>) {

    val appArgs = getArgsMap(args)

    val bufferedImage = ImageIO.read(File(appArgs["-in"].orEmpty()))
    for (i in 0 until bufferedImage.width) {
        for (j in 0 until bufferedImage.height) {
            changePixel(bufferedImage, i, j)
        }
    }

    ImageIO.write(bufferedImage, "png", File(appArgs["-out"].orEmpty()))
}

private fun changePixel(bufferedImage: BufferedImage, x: Int, y: Int) {
    val currentRgb = bufferedImage.getRGB(x, y)
    // Color in int like 11111111 11111111 00000000 00000000
    // where first eight digits are alpha (always 255), and then red, green and blue
    val red = currentRgb.shr(16).and(0xFF)
    val green = currentRgb.shr(8).and(0xFF)
    val blue = currentRgb.and(0xFF)

    val alpha = 255.shl(24)
    val newRed = (255 - red).shl(16);
    val newGreen = (255 - green).shl(8)
    val newBlue = 255 - blue
    val newRGB = alpha.or(newRed).or(newGreen).or(newBlue)
    bufferedImage.setRGB(x, y, newRGB)
}

private fun getArgsMap(args: Array<String>): Map<String, String> {
    if (args.size < 4) {
        throw IllegalArgumentException("Wrong parameters")
    }

    val appArgs = mutableMapOf<String, String>()
    for (i in 0..args.lastIndex step 2) appArgs[args[i]] = args[i + 1]
    return appArgs
}
