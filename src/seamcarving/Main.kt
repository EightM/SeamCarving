package seamcarving

import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_INT_RGB
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.sqrt

fun main(args: Array<String>) {

    val appArgs = getArgsMap(args)

    var bufferedImage = ImageIO.read(File(appArgs["-in"].orEmpty()))
    val energyMap = calculatePixelsEnergy(bufferedImage)
    val verticalSeamsCount = appArgs["-width"]?.toInt() ?: 0
    val horizontalSeamsCount = appArgs["-height"]?.toInt() ?: 0

    for (i in 0 until verticalSeamsCount) {
        bufferedImage = findAndRemoveVerticalSeamWithLowestEnergy(energyMap, bufferedImage)
    }

    for (i in 0 until horizontalSeamsCount) {
        bufferedImage = findAndRemoveHorizontalSeamWithLowestEnergy(energyMap, bufferedImage)
    }

    ImageIO.write(bufferedImage, "png", File(appArgs["-out"].orEmpty()))
}

fun findAndRemoveVerticalSeamWithLowestEnergy(
    energyMap: Map<Pixel, Double>,
    bufferedImage: BufferedImage
): BufferedImage {
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

    return deleteSeam(costs, parents, bufferedImage)
}

fun findAndRemoveHorizontalSeamWithLowestEnergy(
    energyMap: Map<Pixel, Double>,
    bufferedImage: BufferedImage
): BufferedImage {
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

    return deleteSeam(costs, parents, bufferedImage, true)
}

fun deleteSeam(
    costs: Map<Pixel, Double>,
    parents: Map<Pixel, Pixel>,
    bufferedImage: BufferedImage,
    isHorizontalSeam: Boolean = false
): BufferedImage {
    val border = if (isHorizontalSeam) bufferedImage.width - 1 else bufferedImage.height - 1
    val xShift = if (isHorizontalSeam) 0 else 1
    val yShift = if (isHorizontalSeam) 1 else 0
    var lastMinPixel = costs.entries.filter { it.key.x == border }.minByOrNull { it.value }?.key
    val pixelsForDeletion = mutableSetOf<Pixel>()
    while (lastMinPixel != null) {
        pixelsForDeletion += lastMinPixel
        lastMinPixel = parents[lastMinPixel]
    }

    return when {
        isHorizontalSeam -> deleteHorizontalSeam(bufferedImage, xShift, yShift, pixelsForDeletion)
        else -> deleteVerticalSeam(bufferedImage, xShift, yShift, pixelsForDeletion)
    }
}

private fun deleteVerticalSeam(
    bufferedImage: BufferedImage,
    xShift: Int,
    yShift: Int,
    pixelsForDeletion: MutableSet<Pixel>
): BufferedImage {
    val newImage = BufferedImage(bufferedImage.width - xShift, bufferedImage.height - yShift, TYPE_INT_RGB)
    for (y in 0 until newImage.height) {
        var shift = 0
        for (x in 0 until newImage.width) {
            if (pixelsForDeletion.contains(Pixel(x, y))) {
                shift = 1
            }
            newImage.setRGB(x, y, bufferedImage.getRGB(x + shift, y))
        }
    }
    return newImage
}

private fun deleteHorizontalSeam(
    bufferedImage: BufferedImage,
    xShift: Int,
    yShift: Int,
    pixelsForDeletion: MutableSet<Pixel>
): BufferedImage {
    val newImage = BufferedImage(bufferedImage.width - xShift, bufferedImage.height - yShift, TYPE_INT_RGB)
    for (x in 0 until newImage.width) {
        var shift = 0
        for (y in 0 until newImage.height) {
            if (pixelsForDeletion.contains(Pixel(x, y))) {
                shift = 1
            }
            newImage.setRGB(x, y, bufferedImage.getRGB(x, y + shift))
        }
    }
    return newImage
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

private fun getArgsMap(args: Array<String>): Map<String, String> {
    if (args.size < 8) {
        throw IllegalArgumentException("Wrong parameters")
    }

    val appArgs = mutableMapOf<String, String>()
    for (i in 0..args.lastIndex step 2) appArgs[args[i]] = args[i + 1]
    return appArgs
}
