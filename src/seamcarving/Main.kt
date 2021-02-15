package seamcarving

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.sqrt

fun main(args: Array<String>) {

    val appArgs = getArgsMap(args)

    val bufferedImage = ImageIO.read(File(appArgs["-in"].orEmpty()))
    val energyMap= calculatePixelsEnergy(bufferedImage)
    val maxEnergy = energyMap.values.maxOf { it }
    for (i in 0 until bufferedImage.width) {
        for (j in 0 until bufferedImage.height) {
            changePixel(bufferedImage, maxEnergy, energyMap, i, j)
        }
    }

    ImageIO.write(bufferedImage, "png", File(appArgs["-out"].orEmpty()))
}

fun calculatePixelsEnergy(bufferedImage: BufferedImage): Map<Pixel, Double> {
    val energyMap = mutableMapOf<Pixel, Double>()
    for (x in 0 until bufferedImage.width) {
        for (y in 0 until bufferedImage.height) {
            val energy: Double = calculatePixelEnergy(bufferedImage, x, y)
            energyMap[Pixel(x, y)] = energy
        }
    }
    return energyMap
}

fun calculatePixelEnergy(bufferedImage: BufferedImage, x: Int, y: Int): Double {
    val diffX = when (x) {
        0 -> getDiff(bufferedImage, x + 1, y, xShift = 1)
        bufferedImage.width - 1 -> getDiff(bufferedImage, x - 1, y, xShift = 1)
        else -> getDiff(bufferedImage, x, y, xShift = 1)
    }
    val diffY = when (y) {
        0 -> getDiff(bufferedImage, x, y + 1, yShift = 1)
        bufferedImage.height - 1 -> getDiff(bufferedImage, x, y - 1, yShift = 1)
        else -> getDiff(bufferedImage, x, y, yShift = 1)
    }
    return sqrt((diffX + diffY).toDouble())
}

fun getDiff(bufferedImage: BufferedImage, x: Int, y: Int, xShift: Int = 0, yShift: Int = 0): Int {
    val firstColor = getColorFromPixel(bufferedImage, x - xShift, y - yShift)
    val secondColor = getColorFromPixel(bufferedImage, x + xShift, y + yShift)
    val redX = abs(firstColor.r - secondColor.r)
    val greenX = abs(firstColor.g - secondColor.g)
    val blueX = abs(firstColor.b - secondColor.b)
    return redX * redX + greenX * greenX + blueX * blueX
}

fun getColorFromPixel(bufferedImage: BufferedImage, x: Int, y: Int): Color {
    val rgbNumber = bufferedImage.getRGB(x, y)
    val red = rgbNumber.shr(16).and(0xFF)
    val green = rgbNumber.shr(8).and(0xFF)
    val blue = rgbNumber.and(0xFF)
    return Color(red, green, blue)
}

private fun changePixel(
    bufferedImage: BufferedImage,
    maxEnergy: Double,
    energyMap: Map<Pixel, Double>,
    x: Int,
    y: Int
) {
    // Color in int like 11111111 11111111 00000000 00000000
    // where first eight digits are alpha (always 255), and then red, green and blue
    val pixelEnergy = energyMap[Pixel(x, y)] ?: 0.0
    val intensity = (255.0 * pixelEnergy / maxEnergy).toInt()

    val alpha = 255.shl(24)
    val newRed = (intensity).shl(16)
    val newGreen = (intensity).shl(8)
    val newRGB = alpha.or(newRed).or(newGreen).or(intensity)
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
