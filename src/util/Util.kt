package util

import java.awt.Graphics2D
import java.awt.Point
import java.awt.image.BufferedImage
import java.io.File
import java.security.MessageDigest
import javax.swing.JComponent
import javax.swing.JSplitPane

/**
 * ユーティリティをまとめたシングルトンクラス
 * @author yuu
 * @since 2017/3/6
 */
object Util {
    /**
     * 二つの値の最大値を求める
     * @param a 一つ目の値
     * @param b 二つ目の値
     * @param predicate 比較関数 a >= b -> trueなる関数
     * @return 二つの値の最大値
     */
    fun <T> max(a: T, b: T, predicate : (T, T) -> Boolean) = if(predicate(a, b)) a else b
    /**
     * 二つの値の最小値を求める
     * @param a 一つ目の値
     * @param b 二つ目の値
     * @param predicate 比較関数 a >= b -> trueなる関数
     * @return 二つの値の最小値
     */
    fun <T> min(a: T, b: T, predicate : (T, T) -> Boolean) = if(predicate(a, b)) b else a

    /**
     * 二つの値の間に値を収める
     * @param x 収めたい値
     * @param max 最大の値
     * @param min 最小の値
     * @param predicate 比較関数 a >= b -> trueなる関数
     * @return 最大値と最小値の間に収まった値
     */
    fun <T> between(x : T, max : T, min : T, predicate : (T, T) -> Boolean) = min(max(x, min, predicate), max, predicate)
    /**
     * Int型の値を指定の二値間に収める
     * @see between
     */
    fun between(x : Int, max : Int, min : Int) = between(x, max, min, { a, b -> a >= b })
    /**
     * Point型の値を、各々の座標に関して指定の二値間に収める
     * @see between
     */
    fun betweenPoint(p : Point, max : Point, min : Point) = Point(between(p.x, max.x, min.x), between(p.y, max.y, min.y))

    /**
     * イメージ関連のファイルかどうかを拡張子より判定する
     * @param file 判定するファイル
     * @return イメージ関連のファイルかどうかを判定する Boolean 値
     */
    fun isImage(file : File) = arrayOf("JPG", "png", "jpeg", "jpg", "gif").any { it == file.extension }

    /**
     * SHA256を用いて、ファイルのハッシュ値を返す
     * @param file 対象ファイル
     * @return 対象ファイルのSHA256アルゴリズムによるハッシュ値
     */
    fun sha256(file : File) : String{
        val md = MessageDigest.getInstance("SHA-256")
        md.update(file.readBytes())
        val cipher_byte = md.digest()
        val sb = StringBuilder(2 * cipher_byte.size)
        cipher_byte.forEach { sb.append(String.format("%02x", it)) }
        return sb.toString()
    }

    /**
     * 画像を縦幅の方が大きいように回転する
     * 何故かスマートフォンで撮影したJPGファイルが上手く表示されないため実装
     * @param image 回転する画像
     * @return 縦幅の方が大きいように回転した画像
     */
    fun correctImageDirection(image : BufferedImage) : BufferedImage {
        if(image.width < image.height)
            return image
        val dst = BufferedImage(image.height, image.width, BufferedImage.TYPE_INT_ARGB)
        val g2 = (dst.graphics as Graphics2D?)
        g2?.rotate(Math.PI / 2, image.height.toDouble(), 0.0)
        g2?.drawImage(image, image.height, 0, null)
        return dst
    }

    /**
     * 画像を指定の大きさにリサイズする
     * ただし、縦横比は変化させず、あくまでこの範囲内に収まるようにリサイズする
     * @param image リサイズする画像
     * @param width 横幅
     * @param height 縦幅
     * @return リサイズされた画像
     */
    fun resizeImage(image : BufferedImage, width : Int, height : Int) : BufferedImage {
        val dst = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val multiple = Math.min(width.toDouble() / image.width, height.toDouble() / image.height)
        dst.graphics.drawImage(image, 0, 0, (image.width * multiple).toInt(), (image.height * multiple).toInt(), null)
        return dst
    }

    /**
     * JSplitPaneに指定コンポーネントを置き、右(下)側に置いたJSplitPaneを返す
     * @param parent 指定コンポーネントを多くJSplitPane
     * @param pane 指定コンポーネント
     * @param width 指定コンポーネントの横(縦)幅
     * @param orientation 分割ペインの分割方法 (Default : [JSplitPane.HORIZONTAL_SPLIT])
     * @return 右(下)に設置したJSplitPane
     */
    fun addSplitPane(parent : JSplitPane, pane : JComponent, width : Int, orientation : Int = JSplitPane.HORIZONTAL_SPLIT) : JSplitPane{
        val ret = JSplitPane(orientation)
        parent.leftComponent = pane
        parent.rightComponent = ret
        parent.dividerLocation = width
        return ret
    }
}