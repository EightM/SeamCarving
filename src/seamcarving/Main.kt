package seamcarving

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.sqrt

fun main(args: Array<String>) {

    val appArgs = getArgsMap(args)

    val bufferedImage = ImageIO.read(File(appArgs["-in"].orEmpty()))
    val energyMap = calculatePixelsEnergy(bufferedImage)
    findAndPaintHorizontalSeamWithLowestEnergy(energyMap, bufferedImage )
    ImageIO.write(bufferedImage, "png", File(appArgs["-out"].orEmpty()))
}

fun findAndPaintVerticalSeamWithLowestEnergy(energyMap: Map<Pixel, Double>, bufferedImage: BufferedImage) {
    val costs = mutableMapOf<Pixel, Double>()
    val parents = mutableMapOf<Pixel, Pixel>()
    energyMap.entries.filter { it.key.y == 0 }.forEach { costs[it.key] = it.value }

    // Используем динамическое программирование:
    // Начинаем со второго ряда, для каждого пикселя ищем соседа сверху с минимальной энергией
    // В costs складываем энергию текущего пикселя + энергию соседа сверху
    // В parents отслеживаем цепочку соседей, чтобы потом вытащить весь seam
    // В конце, в самом последнем ряду, пиксель с наименьшей суммой энергией будет начальной точкой шва,
    // который мы вытащим из parents
    // См. https://en.wikipedia.org/wiki/Seam_carving#Dynamic_programming
    for (y in 1 until bufferedImage.height) {
        for (x in 0 until bufferedImage.width) {
            val currentPixel = Pixel(x, y)
            val minNeighbor = getMinEnergyTopNeighbor(currentPixel, bufferedImage.width, costs)

            costs[currentPixel] = energyMap[currentPixel]!! + costs[minNeighbor]!!
            parents[currentPixel] = minNeighbor
        }
    }

    paintSeamRed(costs, parents, bufferedImage)
}

fun findAndPaintHorizontalSeamWithLowestEnergy(energyMap: Map<Pixel, Double>, bufferedImage: BufferedImage) {
    val costs = mutableMapOf<Pixel, Double>()
    val parents = mutableMapOf<Pixel, Pixel>()
    energyMap.entries.filter { it.key.x == 0 }.forEach { costs[it.key] = it.value }

    for (x in 1 until bufferedImage.width) {
        for (y in 0 until bufferedImage.height) {
            val currentPixel = Pixel(x, y)
            val minNeighbor = getMinEnergyLeftNeighbor(currentPixel, bufferedImage.width, costs)

            costs[currentPixel] = energyMap[currentPixel]!! + costs[minNeighbor]!!
            parents[currentPixel] = minNeighbor
        }
    }

    paintSeamRed(costs, parents, bufferedImage, true)
}

fun paintSeamRed(
    costs: Map<Pixel, Double>,
    parents: Map<Pixel, Pixel>,
    bufferedImage: BufferedImage,
    isHorizontalSeam: Boolean = false
) {
    val border = if (isHorizontalSeam) bufferedImage.width - 1 else bufferedImage.height - 1
    var lastMinPixel = costs.entries.filter { it.key.x == border }.minByOrNull { it.value }?.key
    while (lastMinPixel != null) {
        paintPixelInRed(bufferedImage, lastMinPixel)
        lastMinPixel = parents[lastMinPixel]
    }
}

fun getMinEnergyTopNeighbor(currentPixel: Pixel, imageWidth: Int, costs: Map<Pixel, Double>): Pixel {
    val set = mutableSetOf<Pixel>()
    val pixelOne = Pixel(currentPixel.x, currentPixel.y - 1)
    set += pixelOne

    if (currentPixel.x != 0) {
        val pixelTwo = Pixel(currentPixel.x - 1, currentPixel.y - 1)
        set += pixelTwo
    }

    if (currentPixel.x != imageWidth - 1) {
        val pixelThree = Pixel(currentPixel.x + 1, currentPixel.y - 1)
        set += pixelThree
    }

    return set.minByOrNull { costs[it] ?: Double.POSITIVE_INFINITY } ?: Pixel(-1, -1)
}

fun getMinEnergyLeftNeighbor(currentPixel: Pixel, imageHeight: Int, costs: Map<Pixel, Double>): Pixel {
    val set = mutableSetOf<Pixel>()
    val pixelOne = Pixel(currentPixel.x - 1, currentPixel.y)
    set += pixelOne

    if (currentPixel.y != 0) {
        val pixelTwo = Pixel(currentPixel.x - 1, currentPixel.y - 1)
        set += pixelTwo
    }

    if (currentPixel.y != imageHeight - 1) {
        val pixelThree = Pixel(currentPixel.x - 1, currentPixel.y + 1)
        set += pixelThree
    }

    return set.minByOrNull { costs[it] ?: Double.POSITIVE_INFINITY } ?: Pixel(-1, -1)
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

fun paintPixelInRed(bufferedImage: BufferedImage, pixel: Pixel?) {
    val alpha = 255 shl 24
    val red = 255 shl 16
    val newColor = alpha or red
    bufferedImage.setRGB(pixel?.x ?: -1, pixel?.y ?: -1, newColor)
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
