package image

import util.Util
import java.awt.*
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JPanel

/**
 * 画像を表示する領域
 * @author yuu
 * @since 2017/03/05
 * @property image 表示する画像
 */
class ImagePane : JPanel() {
    private var image = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)

    /**
     * 指定したファイルを表示する画像に設定する
     * @param file 表示する画像ファイル
     */
    fun setImage(file: File) {
        image = Util.correctImageDirection(ImageIO.read(file))
        draw()
    }

    /**
     * グラフィクスを受け取り、画像をリサイズして描画する
     * @param g 受け取るグラフィクス
     *
     */
    fun draw(g : Graphics? = graphics) {
        val buf = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        buf.graphics?.clearRect(0, 0, width, height)
        buf.graphics?.drawImage(Util.resizeImage(image, width, height), 0, 0, null)
        g?.drawImage(buf, 0, 0, null)
    }

    override fun paint(g: Graphics?) {
        draw(g)
    }
}