package text

import java.awt.Dimension
import java.awt.TextField
import java.io.File
import java.util.*
import javax.swing.*
import kotlin.collections.ArrayList

/**
 * 検索ダイアログの表示、管理を行うクラス
 * @author yuu
 * @since 2017/03/07
 * @property selectionEvents 検索結果選択時のイベントのリスト
 */
object ShowSearchDialog {
    val selectionEvents =  ArrayList<(File) -> Unit>()

    /**
     * 検索結果選択時のイベントを追加する
     * @param event 検索結果選択時のイベント
     */
    fun addSelectionEvents(event : (File) -> Unit) = selectionEvents.add(event)

    /**
     * ファイル検索ダイアログを表示する
     * @param frame 親フレーム
     */
    fun show(frame : JFrame) {
        val dialog = SearchDialog(frame)
        selectionEvents.forEach { dialog.addSelectionEvents(it) }
    }
}

/**
 * 検索ダイアログのクラス
 * @author yuu
 * @since 2017/03/07
 * @param frame 親フレーム
 * @property checkMap チェックボックスのHashMap。検索絞り込みのチェックボックスが、そのタグ名に紐づけられて格納されている
 * @property model ツリーの操作のためのモデル
 * @property searchResult 検索結果のリスト
 * @property selectionEvents 検索結果選択時のイベントのリスト
 */
class SearchDialog(frame : JFrame) : JDialog(frame) {
    private val checkMap = HashMap<String, JCheckBox>()

    private val model = DefaultListModel<ListElement>()
    private val searchResult = JList<ListElement>(model)

    private val selectionEvents =  ArrayList<(File) -> Unit>()

    init {
        setItem()

        title = "検索"
        defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        isVisible = true
        isResizable = false
        pack()
        setLocationRelativeTo(null)
    }

    /**
     * 検索結果選択時のイベントを追加する
     * @param event 検索結果選択時のイベント
     */
    fun addSelectionEvents(event : (File) -> Unit) = selectionEvents.add(event)

    /**
     * ダイアログの要素を実装する
     */
    fun setItem() {
        val pane = JPanel()
        pane.layout = BoxLayout(pane, BoxLayout.Y_AXIS)

        //検索フィールド
        val field = TextField(20)

        //検索対象チェックボックス
        val checks = JPanel()
        checks.layout = BoxLayout(checks, BoxLayout.X_AXIS)
        arrayOf("Exp", "Memo", "Tips", "Other").forEach {
            val check = JCheckBox(it)
            check.isSelected = true
            checkMap[it] = check
            checks.add(check)
        }

        //検索実行・キャンセルボタン
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.X_AXIS)
        val ok = JButton("検索開始")
        ok.addActionListener { if(field.text.isNotEmpty()) search(field.text) }
        panel.add(ok)
        val cancel = JButton("キャンセル")
        cancel.addActionListener { this.dispose() }
        panel.add(cancel)

        //検索結果表示リスト
        searchResult.addListSelectionListener {
            if(searchResult.selectedIndex > -1)
                selectionEvents.forEach { it(model.getElementAt(searchResult.selectedIndex).link) }
        }
        val searchScroll = JScrollPane(searchResult)
        searchScroll.preferredSize = Dimension(500, 500)

        pane.add(field)
        pane.add(checks)
        pane.add(panel)
        pane.add(searchScroll)
        add(pane)
    }

    /**
     * ある言葉を検索し、検索結果を結果表示領域に追加する
     * @param words 検索する言葉
     */
    fun search(words : String) {
        //検索状態をリセット
        searchResult.clearSelection()
        model.clear()

        //検索結果
        val result = ArrayList<Triple<File, String, String>>()
        //検索するファイル
        val file = File("./data/text.db")
        //各領域ごとの画像ファイル名
        var image = ""
        //各領域ごとのIni形式テキスト
        var text = ""
        if(file.exists())
            file.forEachLine {
                if(it.startsWith("[") && it.contains("]")) {
                    //次の領域に到達した場合は、それまでの内容に対して検索を実行する
                    result.addAll(searchWords(text, words).map { Triple(File(image), it.first, it.second) })
                    //画像名を更新し、テキストをクリアする
                    image = it.substring(it.indexOf('[') + 1, it.indexOf(']')).split(',')[0]
                    text = ""
                } else
                    text += (if (text.isNotEmpty()) "\n" else "") + it
            }
        //結果を、検索結果に追加する
        result.forEach { model.addElement(ListElement(it.first, it.second, it.third)) }
        pack()
    }

    /**
     * Ini形式のテキストから、ある言葉を検索し、リストとして返す
     * @param ini Ini形式のテキスト
     * @param words 検索する単語
     * @return 検索結果のリスト。そのタグ名と、検索単語から20文字以内までの一文のペアが格納されている
     */
    fun searchWords(ini : String, words : String) : ArrayList<Pair<String, String>>{
        val result = ArrayList<Pair<String, String>>()
        TextData.parseIni(ini).filter { (checkMap[it.key]?.isSelected ?: false) && it.value.contains(words) }.forEach {
            var textData = it.value
            while(textData.isNotEmpty() && textData.contains(words)) {
                val pos = textData.indexOf(words)
                val end = Math.min(pos + 20, textData.length - 1)
                result.add(Pair(it.key, textData.substring(pos .. end)))
                textData = textData.substring(end + 1)
            }
        }
        return result
    }

    /**
     * リストの要素のクラス
     * @param link リンク先のファイル
     * @param tag タグの名前
     * @param text 表示されるテキスト
     */
    class ListElement(val link : File, val tag : String, val text : String) {
        override fun toString(): String = "${link.nameWithoutExtension} -> $tag: $text"
    }
}