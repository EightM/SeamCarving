package seamcarving

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.util.*
import javax.imageio.ImageIO

fun main() {
    val scanner = Scanner(System.`in`)
    println("Enter rectangle width:")
    val width = scanner.nextInt()
    println("Enter rectangle height:")
    val height = scanner.nextInt()
    println("Enter output image name:")
    val fileName = scanner.next()

    createImageFile(width, height, fileName)
}

private fun createImageFile(width: Int, height: Int, fileName: String) {
    val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val graphics = bufferedImage.graphics

    graphics.color = Color.RED
    graphics.drawLine(0, 0, width - 1, height - 1)
    graphics.drawLine(0, height - 1, width - 1, 0)

    ImageIO.write(bufferedImage, "png", File(fileName))
}
