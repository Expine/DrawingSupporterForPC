package main

import util.Util
import java.awt.CardLayout
import java.io.File
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.SwingWorker
import javax.swing.tree.DefaultTreeModel
import java.awt.Component
import javax.swing.event.TreeSelectionEvent
import javax.swing.filechooser.FileSystemView
import javax.swing.event.TreeSelectionListener
import javax.swing.JLabel
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.TreePath

/**
 * ファイルツリーを構成するクラス
 * @author yuu
 * @since 2017/03/06
 * @property root ファイルツリーの頂点(カレントディレクトリ)
 * @property tree ファイルツリー本体
 * @property listener ファイルツリーのリスナー
 */
class FileTree : JPanel(){
    private val root = DefaultMutableTreeNode(File(".").absoluteFile)
    private val tree = JTree(root)
    private val listener = FolderSelectionListener()

    init {
        //レイアウトは領域いっぱいに広がるCardLayout
        layout = CardLayout()

        //リスナーとレンダーを登録
        tree.addTreeSelectionListener(listener)
        tree.cellRenderer = FileTreeCellRenderer()

        //初期化して追加
        tree.isRootVisible = false
        update()
        add(tree)
    }

    /**
     * 選択されたときに行われるイベントを追加する
     * @param event 選択されたときに行われるイベント
     */
    fun addSelectionEvent(event : (File) -> Unit) = listener.addSelectionEvent(event)

    /**
     * ツリーを更新する。
     * いったんすべて削除した後、ルートから再展開させる
     */
    fun update() {
        root.removeAllChildren()
        listener.expand(root, tree.model as DefaultTreeModel, true)
    }

    /**
     * 指定ファイルまで展開して選択する。
     * 見つからなかった場合は、何もしない
     * @param file 展開するファイル
     */
    fun expand(file : File) {
        tree.selectionPath = searchPath(root, file) ?: tree.selectionPath
    }

    /**
     * 指定ファイルを指定ノードから再帰的に探す
     * @param node 再帰的に探す対象ノード
     * @param file 指定するファイル
     * @return 指定ファイルまでのツリーパス。見つからなければNull
     */
    fun searchPath(node : DefaultMutableTreeNode, file : File) : TreePath?{
        //葉ならば、その絶対パスを指定するファイルと比較し、一致するならばそのパスを返す
        if(node.isLeaf)
            return if((node.userObject as File).canonicalPath == file.canonicalPath) TreePath((tree.model as DefaultTreeModel).getPathToRoot(node)) else null
        //葉でないなら、そのノードを展開した後、その子供について再帰的に調べる
        else {
            listener.expand(node, tree.model as DefaultTreeModel, true)
            return node.children().toList().map { searchPath(it as DefaultMutableTreeNode, file) }.firstOrNull { it != null }
        }
    }
}

/**
 * ツリーの選択イベントを実行するリスナー。
 * 選択時には、その直下のツリーが空ならファイル情報から更新しつつ、登録されたイベントを実行する。
 * @author yuu
 * @since 2017/03/06
 * @property fileSystemView ファイル取得のためのファイルシステム
 * @property eventList 選択されたときに行われるイベントのリスト
 * @see TreeSelectionListener
 */
class FolderSelectionListener : TreeSelectionListener {
    private val fileSystemView = FileSystemView.getFileSystemView()
    private val eventList = ArrayList<(File) -> Unit>()

    /**
     * 選択されたときに行われるイベントを追加する
     * @param event 選択されたときに行われるイベント
     */
    fun addSelectionEvent(event : (File) -> Unit) = eventList.add(event)

    /**
     * 選択したノードに対して、直下のツリー情報を更新する
     * @param node 選択したノード
     * @param model ツリーのモデル
     * @param first 再帰処理が行われる前、すなわち最初に呼ばれた状態であるかを判定する
     */
    fun expand(node : DefaultMutableTreeNode, model : DefaultTreeModel, first : Boolean) {
        //選択したノードが葉でなく(既に展開済みであり)、かつそれが再帰により呼び出されたものでなければ、その中身を再帰的に呼ぶ
        if(!node.isLeaf) {
            if(first)
                node.children().toList().forEach { expand(it as DefaultMutableTreeNode, model, false) }
            //既に展開済みならば、これ以降の処理は行われない(展開後の再度の更新処理は一度すべてをリセットしない限り行われない)
            return
        }
        //選択したノードがディレクトリでないなら、展開処理は行われない
        val target = node.userObject as File
        if (!target.isDirectory) return

        //以下の処理を非同期的に行う
        val worker = object : SwingWorker<String,   File>() {
            public override fun doInBackground(): String {
                //ディレクトリ或いは画像ならばツリーへの追加処理に回す
                fileSystemView.getFiles(target, true)?.filter { it.isDirectory || Util.isImage(it) }?.forEach { publish(it) }
                return "done"
            }

            override fun process(chunks: List<File>?) {
                //ツリーに追加しつつ、最初の呼び出しであるならば再帰的調査
                chunks?.forEach { val element = DefaultMutableTreeNode(it); node.add(element); if(first) expand(element, model, false)}
                model.nodeStructureChanged(node)
            }
        }
        worker.execute()
    }

    override fun valueChanged(e: TreeSelectionEvent) {
        val node = e.path.lastPathComponent as DefaultMutableTreeNode
        val model = (e.source as JTree).model as DefaultTreeModel
        expand(node, model, true)

        eventList.forEach { it(node.userObject as File) }
    }
}

/**
 * ツリーの描画を指示するクラス
 * @author yuu
 * @since 2017/03/06
 * @property fileSystemView 描画のための情報を取得するファイルシステム
 * @property label ツリーに載せるラベル
 * @see DefaultTreeCellRenderer
 */
internal class FileTreeCellRenderer : DefaultTreeCellRenderer() {
    private val fileSystemView = FileSystemView.getFileSystemView()
    private val label: JLabel = JLabel()

    init {
        label.isOpaque = true
    }

    override fun getTreeCellRendererComponent(tree: JTree, value: Any, selected: Boolean, expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean): Component {
        val file = (value as DefaultMutableTreeNode).userObject as File
        label.icon = fileSystemView.getSystemIcon(file)
        label.text = fileSystemView.getSystemDisplayName(file)
        label.toolTipText = file.path
        if (selected) {
            label.background = backgroundSelectionColor
            label.foreground = textSelectionColor
        } else {
            label.background = backgroundNonSelectionColor
            label.foreground = textNonSelectionColor
        }
        return label
    }
}