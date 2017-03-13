package text

import util.Util
import java.awt.*
import java.awt.event.MouseEvent
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.*
import javax.swing.event.MouseInputAdapter
import javax.swing.text.*

/**
 * テキストが表示される領域のクラス。
 * @author yuu
 * @since 2017/03/05
 * @property texts 分割ペインにあるテキストエリア群
 * @property days 作成日を書くテキストフィールド
 * @property linkEvent リンク処理の際に呼ばれるイベント
 */
class TextPane : JPanel() {
    private val texts = HashMap<String, JTextPane>()
    private val days = JTextField(20)
    var linkEvent : ((String) -> Unit)? = null

    /**
     * 指定の名前のラベルを上、下をテキストエリアとしたパネルを返す
     * @param name ラベル表示名
     * @return 上記のパネル
     */
    fun getPane(name : String) : JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)

        val text = JTextPane()
        val mouseListener = TextMouseListener(text)
        //テキストに対する各種処理を実装
        text.styledDocument = SyntaxDocument()
        text.addMouseListener(mouseListener)
        mouseListener.addPressEvent { if(linkEvent != null && it.startsWith(">>")) linkEvent?.invoke(it) }

        //パネルに載せる
        panel.add(JLabel(name))
        panel.add(JScrollPane(text))

        //テキストエリアは保存しておく
        texts[name] = text
        return panel
    }


    /**
     * 分割ペインに指定された文字群と同じテキストエリアを設置する。
     * まず、全ての子を排した後、分割ペインを再設定する。
     * @param regions この名前を持ったテキストエリアが分割ペインに登録される。
     */
    fun setSplit(regions : List<String>) {
        //いったん、全ての子を排除する
        removeAll()
        texts.clear()

        //レイアウトは縦方向への並び
        layout = BoxLayout(this, BoxLayout.Y_AXIS)

        //作成日情報
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.X_AXIS)
        panel.add(JLabel("作成日"))
        panel.add(days)

        //再帰処理で、分割ペインを設置していく
        var base = JSplitPane(JSplitPane.VERTICAL_SPLIT)
        add(base)
        base = Util.addSplitPane(base, panel, 20, JSplitPane.VERTICAL_SPLIT)
        regions.forEach {
            base = Util.addSplitPane(base, getPane(it), (preferredSize.height - 20) / regions.size - 5, JSplitPane.VERTICAL_SPLIT)
        }

        //再描画させる
        size = Dimension(size.width + 1, size.height)
    }

    /** 全テキストエリアをクリアする */
    fun clear() = texts.forEach { it.value.text = "" }

    /**
     * ファイルを開き、テキストエリアに書き込む
     * @param
     */
    fun openFile(file : File?) {
        if(file == null || !file.exists())
            return

        val data = TextData.parseFile(file)
        setSplit(data.filter { it.key != "Date" }.map { it.key })
        data.filter { it.key != "Date" }.forEach { texts[it.key]?.text = it.value}

        days.text = if(data.containsKey("Date")) data["Date"] else SimpleDateFormat("yyyy/MM/dd").format(Date())
    }

    /**
     * 指定ファイルにテキストデータをセーブする
     * @param file 指定するファイル
     */
    fun saveFile(file : File) = TextData.unparseFile(file, texts.mapValues { it.value.text }.plus(Pair("Date", days.text)))

    /**
     * テキストに対するマウス操作のリスナー
     * @param text 実装するテキストペイン
     * @property pressEventList ある行で押された時、その行の文字列を引数に行われるイベントのリスト
     */
    internal class TextMouseListener(val text : JTextPane) : MouseInputAdapter() {
        private  val pressEventList = ArrayList<(String) -> Unit>()

        /**
         * ある行で押された時、その行の文字列を引数に行われるイベントを設定する
         * @param event ある行で押された時、その行の文字列を引数に行われるイベント
         */
        fun addPressEvent(event : (String) -> Unit) = pressEventList.add(event)

        override fun mousePressed(e: MouseEvent?) {
            //クリックされた行を取得
            val line = text.document.defaultRootElement.getElementIndex(text.viewToModel(e?.point ?: Point()))
            if(line > text.styledDocument.defaultRootElement.elementCount - 1)
                return

            //クリックされた行のオフセットを取得
            val startOffset = text.styledDocument.defaultRootElement.getElement(line).startOffset
            val endOffset = text.styledDocument.defaultRootElement.getElement(line).endOffset - 1

            //クリックされた行の文字列を取得し、イベントを発生させる
            val tex = text.styledDocument.getText(startOffset, endOffset - startOffset)
            pressEventList.forEach { it(tex) }

            super.mousePressed(e)
        }
    }

    /**
     * シンタックスハイライトを実装するためのクラス
     * @property normal 普通の文字列のスタイル
     */
    internal class SyntaxDocument : DefaultStyledDocument() {
        private val normal: Style

        init {
            //通常のスタイル
            val def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE)
            normal = addStyle("normal", def)
            StyleConstants.setForeground(normal, Color.BLACK)

            //リンクのスタイル
            val link = addStyle(">>", normal)
            StyleConstants.setForeground(link, Color.BLUE)
            StyleConstants.setUnderline(link, true)
        }

        override fun insertString(offs: Int, str: String?, a: AttributeSet?) {
            super.insertString(offs, str, a)
            processChangedLines(offs, str!!.length)
        }

        override fun remove(offset: Int, length: Int) {
            super.remove(offset, length)
            processChangedLines(offset, 0)
        }

        /**
         * 何らかの変化が生じた際に呼ばれる。
         * 変化が生じた各行について、[applyHighlighting]を呼ぶ。
         * @param offset 変化が生じた開始オフセット
         * @param length 変化が生じた長さ
         * @see applyHighlighting
         */
        private fun processChangedLines(offset: Int, length: Int) =
                (defaultRootElement.getElementIndex(offset) .. defaultRootElement.getElementIndex(offset + length)).forEach { applyHighlighting(it) }

        /**
         * ある行に対してハイライトを実行する
         * @param line ハイライトを実行する行
         */
        private fun applyHighlighting(line: Int) {
            val startOffset = defaultRootElement.getElement(line).startOffset
            var endOffset = defaultRootElement.getElement(line).endOffset - 1
            val lineLength = endOffset - startOffset
            if (endOffset >= length)
                endOffset = length - 1

            //いったん普通の状態にする
            setCharacterAttributes(startOffset, lineLength, normal, true)
            //トークンを調べ、ハイライトをチェックする
            checkForLine(startOffset, endOffset, lineLength)
        }

        /**
         * 各スタイルをチェックし、初めに前方一致したものを探し、あるならばハイライトする
         * @param startOffset 開始オフセット
         * @param endOffset 終了オフセット
         * @param lineLength 文章の長さ
         */
        private fun checkForLine(startOffset: Int, endOffset: Int, lineLength : Int) {
            if(startOffset <= endOffset)
                styleNames?.toList()?.firstOrNull { getText(startOffset, lineLength).startsWith(it as String) }?.let { setCharacterAttributes(startOffset, lineLength, getStyle(it as String), false) }
        }
    }
}

