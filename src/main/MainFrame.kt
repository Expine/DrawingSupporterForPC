package main

import image.ImagePane
import image.ShowImageDialog
import text.TextPane
import text.TextData
import text.ShowSearchDialog
import util.Util
import java.awt.CardLayout
import java.awt.Dimension
import java.awt.Font
import java.awt.event.*
import java.io.File
import java.util.*
import javax.swing.*

/**
 * メインフレームのクラス
 * このフレームを媒介にして各ペインは接合する
 * @property TREE_WIDTH ファイルツリーの占める横幅
 * @property IMAGE_WIDTH 画像表示領域の占める横幅
 * @property TEXT_WIDTH テキスト表示領域の占める領域
 * @property FRAME_HEIGHT このフレームの縦幅
 *
 * @property panel ベースとなるパネル
 * @property originSplit 分割ペインのベースとなるペイン
 * @property tree ファイルツリー
 * @property image 画像表示領域
 * @property text テキスト表示領域
 *
 * @property targetFile 現在開いているファイル
 */
class MainFrame : JFrame() {
    private val TREE_WIDTH = 200
    private val IMAGE_WIDTH = 600
    private val TEXT_WIDTH = 400
    private val FRAME_HEIGHT = 800

    private val editorData : EditorData

    private val panel = JPanel()
    private val originSplit = JSplitPane()
    private val tree = FileTree()
    private val image = ImagePane()
    private val text = TextPane()

    private var targetFile : File? = null

    init {
        setMenuBar()

        title = "Drawing Supporter"
        defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE

        //編集領域を生成
        text.preferredSize = Dimension(TEXT_WIDTH, FRAME_HEIGHT)
        image.preferredSize = Dimension(IMAGE_WIDTH, FRAME_HEIGHT)

        //各領域の固有動作を設定
        tree.addSelectionEvent { openFile(it) }
        text.linkEvent = { ShowImageDialog.show(this, it.drop(2).trimStart()) }
        text.setSplit(arrayListOf("Exp", "Memo", "Tips", "Other"))
        ShowSearchDialog.addSelectionEvents { openFile(it) }
        ShowSearchDialog.addSelectionEvents { tree.expand(it) }

        //基本的なデータを取得、設定
        editorData = loadEditorData()
        ShowImageDialog.dialog_x = editorData.dialog_x
        ShowImageDialog.dialog_y = editorData.dialog_y

        //スプリットフレームを生成
        panel.layout = CardLayout()
        panel.preferredSize = Dimension(editorData.panel_width, editorData.panel_height)
        Util.addSplitPane(Util.addSplitPane(Util.addSplitPane(originSplit, JScrollPane(tree), editorData.tree_width), JScrollPane(image), editorData.image_width), JScrollPane(text), editorData.text_width)
        panel.add(originSplit)
        add(panel)

        //フレームにパネルを登録し、初期設定を行う
        pack()
        if(editorData.frame_x == -1 && editorData.frame_y == -1)
            setLocationRelativeTo(null)
        else
            setLocation(editorData.frame_x, editorData.frame_y)

        //セーブした情報があれば再現する
        if(editorData.targetFile != null) {
            openFile(editorData.targetFile)
            tree.expand(targetFile!!)
        }

        //ウィンドウを閉じた場合の処理を追加
        addWindowListener(object : WindowAdapter() { override fun windowClosing(e: WindowEvent?) { saveEditorData(); if(targetFile != null) text.saveFile(targetFile!!) } })

        //フレームの情報を構成し、可視化する
        isVisible = true
    }

    /**
     * メニューバーを設定する
     */
    fun setMenuBar() {
        //メニューおよびメニューバー
        val menuBar = JMenuBar()
        val menus = ArrayList<Pair<JMenu, Char>>()
        val items = HashMap<Char, ArrayList<Triple<JMenuItem?, KeyStroke?, (() -> Unit)?>>>()

        //メニューの名前を設定
        menus.add(Pair(JMenu("ファイル(F)"), 'F'))
        menus.add(Pair(JMenu("画像(I)"), 'I'))
        menus.add(Pair(JMenu("編集(E)"), 'E'))
        menus.add(Pair(JMenu("設定(C)"), 'C'))
        menus.add(Pair(JMenu("オプション(O)"), 'O'))
        menus.add(Pair(JMenu("ヘルプ(H)"), 'H'))

        //ニーモニックを設定
        //メニューバーにメニューを追加
        for (menu in menus) {
            menu.first.setMnemonic(menu.second)
            menu.first.font = Font("MS 明朝", Font.PLAIN, 12)
            menuBar.add(menu.first)
            menuBar.add(Box.createRigidArea(Dimension(5, 1)))
        }

        //メニューの項目
        menus.forEach { items[it.second] = ArrayList<Triple<JMenuItem?, KeyStroke?, (() -> Unit)?>>() }

        //以下でメニューを設定する。Triple(null, null. null)であれば、そこに線が引かれる

        //ファイル直下の項目
        items['F']?.add(Triple(JMenuItem("新規フォルダを作成"), KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK), { ShowNewFolderDialog.show(this, { tree.update() })}))
        items['F']?.add(Triple(JMenuItem("保存する"), KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), { if(targetFile != null) text.saveFile(targetFile!!) }))

        //画像直下の項目
        items['I']?.add(Triple(JMenuItem("画像の更新"), KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.CTRL_DOWN_MASK), { updateImage() }))

        //編集直下の項目
        items['E']?.add(Triple(JMenuItem("新規タグ"), KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), { addGroup() }))
        items['E']?.add(Triple(JMenuItem("検索"), KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK), { ShowSearchDialog.show(this) }))
        items['E']?.add(Triple(JMenuItem("ツリーの更新"), KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK), { tree.update() }))

        //設定直下の項目
        items['C']?.add(Triple(JMenuItem("基本設定"), KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK), { }))

        //オプション直下の項目
        items['O']?.add(Triple(JMenuItem("オプション"), KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK), { }))

        //ヘルプ直下の項目
        items['H']?.add(Triple(JMenuItem("ヘルプ"), null, { }))

        //キーアクセラレーションの設定
        //メニューのアクションを設定
        items.forEach { it.value.forEach { item -> item.first?.accelerator = item.second; item.first?.addActionListener { item.third?.invoke() } } }

        //メニューの項目をメニューに追加
        menus.forEach { items[it.second]?.forEach { i -> if (i.first != null) it.first.add(i.first) else it.first.addSeparator(); i.first?.font = Font("MS 明朝", Font.PLAIN, 12) } }

        jMenuBar = menuBar
    }

    /**
     * ファイルを開き、テキストエリアと画像エリアに表示する
     * @param file 開くファイル
     */
    fun openFile(file : File) {
        //開くことのできる画像ファイルでない場合は処理しない
        if(!file.exists() || !file.isFile || !Util.isImage(file))
            return

        //以前に何らかのファイルを開いていた場合はそれを保存する
        if(targetFile != null)
            text.saveFile(targetFile!!)

        //イメージをセットする
        image.setImage(file)

        //テキストをセットする
        text.clear()
        text.openFile(file)

        //開いているファイルを更新する
        targetFile = file
    }

    fun updateImage() {}

    fun addGroup() {}

    /**
     * Drawing Supporterの基本データを保存する。
     * 保存するデータ構造はIni形式。
     */
    fun saveEditorData() {
        val editFile = File("./data/editor.ini")
        var data = ""
        data += "frame_x=${locationOnScreen.x}\nframe_y=${locationOnScreen.y}\n"
        data += "panel_w=${panel.size.width}\npanel_h=${panel.size.height}\n"
        data += "tree_w=${originSplit.dividerLocation}\n"
        data += "image_w=${(originSplit.rightComponent as JSplitPane).dividerLocation}\n"
        data += "text_w=${((originSplit.rightComponent as JSplitPane).rightComponent as JSplitPane).dividerLocation}\n"
        data += "target=${targetFile?.path}"
        editFile.writeText(data)
    }

    /**
     * Drawing Supporterの基本設定をロードする。
     * Ini形式のファイルと見て読み込む
     * @return ロードした基本設定のデータクラス
     */
    fun loadEditorData() : EditorData{
        val adjust = UIManager.getInt("ScrollBar.width") + 5
        val data = TextData.parseIniFile(File("./data/editor.ini"))
        return EditorData(data.getOrDefault("frame_x", "-1").toInt(),
                data.getOrDefault("frame_y", "-1").toInt(),
                data.getOrDefault("panel_w", "${TREE_WIDTH + IMAGE_WIDTH + TEXT_WIDTH + adjust * 3 + 10}").toInt(),
                data.getOrDefault("panel_h", "${FRAME_HEIGHT + 10}").toInt(),
                data.getOrDefault("tree_w", "$TREE_WIDTH").toInt(),
                data.getOrDefault("image_w", "${IMAGE_WIDTH + adjust}").toInt(),
                data.getOrDefault("text_w", "${TEXT_WIDTH + adjust}").toInt(),
                data.getOrDefault("dialog_x", "-1").toInt(),
                data.getOrDefault("dialog_y", "-1").toInt(),
                if(data["target"] == "null") null else File(data["target"])
                )
    }

    /**
     * Drawing Supporter の基本情報を保持するためのデータクラス
     * @property frame_x フレームの表示座標X
     * @property frame_y フレームの表示座標Y
     * @property panel_width フレームの実際の横幅
     * @property panel_height フレームの実際の縦幅
     * @property tree_width フレーム分割のファイルツリーから画像領域までの距離
     * @property image_width フレーム分割の画像領域からテキスト領域までの距離
     * @property text_width フレーム分割のテキスト領域から空き領域までの距離
     * @property dialog_x 画像表示ダイアログの表示座標X
     * @property dialog_y 画像表示ダイアログの表示座標Y
     * @property targetFile 現在開いているファイル
     */
    data class EditorData(val frame_x : Int, val frame_y : Int,
                          val panel_width : Int, val panel_height : Int,
                          val tree_width : Int, val image_width : Int, val text_width : Int,
                          val dialog_x : Int, val dialog_y : Int,
                          val targetFile : File?)
}