package image

import util.Util
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import javax.imageio.ImageIO
import javax.swing.*

/**
 * イメージダイアログを表示、管理するクラス
 * @author yuu
 * @since 2017/3/7
 * @property showedImages 表示されたイメージダイアログとファイル名の組
 * @property dialog_x ダイアログの表示位置
 * @property dialog_y ダイアログの表示位置
 * @property dialog_width ダイアログの横幅
 * @property dialog_height ダイアログの縦幅
 */
object ShowImageDialog {
    private val showedImages = ArrayList<Pair<String, ImageDialog>>()
    private val dialog_width = 600
    private val dialog_height = 800
    var dialog_x = -1
    var dialog_y = -1

    /**
     * イメージダイアログを表示する
     * @param frame 親となるフレーム
     * @param name 表示する画像のファイル名
     */
    fun show(frame : JFrame, name : String) {
        //既に同じファイル名を表示していた場合は処理しない
        if(showedImages.any { it.first == name })
            return

        //指定ファイル名を持つファイルをカレントディレクトリから再帰的に探し出し、ダイアログを表示する
        searchFile(File("./"), name).forEach {
            val dialog = ImageDialog(frame, it, dialog_width, dialog_height)
            dialog.addWindowListener(object : WindowAdapter(){ override fun windowClosing(e: WindowEvent?) {
                ShowImageDialog.dialog_x = dialog.locationOnScreen.x
                ShowImageDialog.dialog_y = dialog.locationOnScreen.y
                showedImages.removeIf { it.second == dialog }
            } })
            showedImages.add(name to dialog)
        }
    }

    /**
     * 指定ディレクトリから、指定ファイル名を持つファイルを再帰的に探し出してリストにして返す
     * @param dir 再帰検索するディレクトリ
     * @param name 表示する画像のファイル名
     * @return 指定ファイル名を持つFileクラスのリスト
     */
    private fun searchFile(dir : File, name : String) : List<File> {
        return dir.listFiles().filter { it.isDirectory }.map { searchFile(it, name) }.flatMap { it -> it}.plus(dir.listFiles().filter { it.isFile && it.nameWithoutExtension == name && Util.isImage(it)})
    }
}

/**
 * イメージダイアログのクラス
 * イメージが表示されるだけで、リサイズその他の操作は不可能
 * @author yuu
 * @since 2017/3/7
 * @param frame 親に持つフレーム
 * @param file 表示するイメージのファイル
 * @param width 表示するダイアログの横幅
 * @param height 表示するダイアログの縦幅
 */
class ImageDialog(frame : JFrame, file : File, width : Int, height : Int) : JDialog(frame){
    init {
        //表示するイメージを張り付ける
        add(JLabel(ImageIcon(Util.resizeImage(Util.correctImageDirection(ImageIO.read(file)), width, height))))

        title = file.nameWithoutExtension
        defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        isVisible = true
        isResizable = false
        pack()
        if(ShowImageDialog.dialog_x == -1 && ShowImageDialog.dialog_y == -1)
            setLocationRelativeTo(null)
        else
            setLocation(ShowImageDialog.dialog_x, ShowImageDialog.dialog_y)
    }
}