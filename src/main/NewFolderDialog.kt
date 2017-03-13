package main

import java.awt.TextField
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.*

/**
 * 新規フォルダ作成ダイアログの表示、管理をするクラス
 * @author yuu
 * @since 2017/03/07
 */
object ShowNewFolderDialog {
    /**
     * 新規フォルダ作成ダイアログを表示する
     * @param frame 親フレーム
     * @param disposeFunction ダイアログが閉じられた時に行われるイベント
     */
    fun show(frame : JFrame, disposeFunction : (() -> Unit)?) {
        val dialog = NewFolderDialog(frame)
        dialog.addWindowListener(object : WindowAdapter(){ override fun windowClosed(e: WindowEvent?) { disposeFunction?.invoke() } })
    }
}

/**
 * フォルダ新規作成ダイアログのクラス
 * @param frame 親フレーム
 */
class NewFolderDialog(frame : JFrame) : JDialog(frame) {
    init {
        setItem()

        title = "新規フォルダの作成"
        defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        isResizable = false
        pack()
        setLocationRelativeTo(null)

        //モーダルモードの場合、Visibleから終わるまで処理が返ってこない
        isModal = true
        isVisible = true
    }

    /**
     * ダイアログの構成要素を設定する
     */
    fun setItem() {
        //ベースペイン
        val pane = JPanel()
        pane.layout = BoxLayout(pane, BoxLayout.Y_AXIS)

        //テキストフィールド
        val field = TextField(20)
        field.text = SimpleDateFormat("yyyy_MM_dd").format(Date())

        //ボタン群
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.X_AXIS)
        val ok = JButton("新規作成")
        ok.addActionListener { create(field.text) }
        panel.add(ok)
        val cancel = JButton("キャンセル")
        cancel.addActionListener { this.dispose() }
        panel.add(cancel)

        pane.add(field)
        pane.add(panel)
        add(pane)
    }

    /**
     * ディレクトリを作り、このダイアログを閉じる
     * @param name ディレクトリの名前
     */
    fun create(name : String) {
        //ディレクトリがすでにあるなら処理しない
        if(File("./$name").exists())
            return

        //ディレクトリを作り、ダイアログを閉じる
        File("./$name").mkdir()
        this.dispose()
    }
}