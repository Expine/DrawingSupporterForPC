package text

import util.Util
import java.io.File

/**
 * テキストデータを管理するクラス
 * @author yuu
 * @since 2017/03/11
 */
object TextData {
    /**
     * Iniファイル形式のファイルをHashにパースする
     * key1=value1
     * key2=value2
     * => [key1 -> value1, key2 -> value2]
     * @param file Iniファイル
     * @return Iniファイルの構造に即したHash
     */
    fun parseIniFile(file : File) : HashMap<String, String> {
        //パースした結果
        val result = HashMap<String, String>()
        //改行した内容を格納するため、前の状態でのキーを保存しておく必要がある
        var preKey : String = ""

        //各行統合チェックしながら、データにパースする
        if(file.exists())
            file.readLines().forEach {
                val eq = it.indexOf('=')
                if(eq == -1)
                    result[preKey] = result[preKey] + "\n" + it
                else {
                    preKey = it.substring(0, eq)
                    result[preKey] = (result[preKey] ?: "") + it.substring(eq + 1)
                }
            }
        return result
    }

    /**
     * Iniファイル形式のテキストをHashにパースする
     * key1=value1
     * key2=value2
     * => [key1 -> value1, key2 -> value2]
     * @param text Iniファイル形式のテキスト
     * @return Iniファイルの構造に即したHash
     */
    fun parseIni(text : String) : HashMap<String, String> {
        //パースした結果
        val result = HashMap<String, String>()
        //改行した内容を格納するため、前の状態でのキーを保存しておく必要がある
        var preKey : String = ""

        //テキストが空なら殻を返す
        if(text == "")
            return result

        //各行統合チェックしながら、データにパースする
        text.lines().forEach {
            val eq = it.indexOf('=')
            if(eq == -1)
                result[preKey] = result[preKey] + "\n" + it
            else {
                preKey = it.substring(0, eq)
                result[preKey] = (result[preKey] ?: "") + it.substring(eq + 1)
            }
        }
        return result
    }

    /**
     * 指定したイメージファイルからデータを読み込み、Hash構造のデータ型にして返す
     * 読み込むデータファイル構造は以下のようなもの
     * [ region_name ]
     * key=value...
     * [ region_name ]
     * key=value...
     * => 指定のregion_nameについて、[ key => value, ... ]
     * @param file 指定するイメージファイル
     * @return 指定したイメージファイルから読み込まれたデータのHash
     */
    fun parseFile(file : File) : HashMap<String, String>{
        //読み込むデータファイル
        val data = File("./data/text.db")
        //識別するためのSHA1値
        val sha = Util.sha256(file)
        //読み込む領域内であることを判定する
        var isWritable = false
        //ファイルから読み込んだ指定領域のIni形式のテキスト
        var text = ""

        //読み込むデータファイルが存在しない場合は空のHashを返す
        if(!data.exists())
            return HashMap()

        //データファイルから各行読み込み、指定ファイル領域のデータを取得する
        data.forEachLine {
            if(it.startsWith("[") && it.contains("]")) {
                val id = it.substring(it.indexOf('[') + 1, it.indexOf(']')).split(',')
                isWritable = id.size ==2 && (id[0] == (".\\" + file.relativeTo(File("./").absoluteFile).path) || id[1] == sha)
            } else if(isWritable) {
                text += (if(text.isNotEmpty()) "\n" else "") + it
            }
        }

        //取得したデータをIni形式からデータ形式にパースして返す
        return parseIni(text)
    }

    /**
     * 指定したファイルのデータを更新して、データファイルに書き込む
     * @param file 更新するファイル
     * @param update 更新するデータ
     */
    fun unparseFile(file : File, update : Map<String, String>) {
        //読み込むデータファイル
        val data = File("./data/text.db")
        //識別するためのSHA1値
        val sha = Util.sha256(file)
        val filePath = ".\\" + file.canonicalPath.replace(File("./").canonicalPath + "\\", "")
        //書き込む領域内であることを判定する
        var isWritable = false
        //すでに書き込んだかどうかを判定する
        var isWrote = false
        //テキストを読み出し、更新したテキストにして格納する
        var text = ""

        //データファイルから各行読み込み、指定ファイル領域だけ更新する
        if(data.exists())
            data.forEachLine {
                if(it.startsWith("[") && it.contains("]")) {
                    //IDを識別し、書き込み領域内かを判定
                    val id = it.substring(it.indexOf('[') + 1, it.indexOf(']')).split(',')
                    isWritable = id.size ==2 && (id[0] == (".\\" + file.relativeTo(File("./").absoluteFile).path) || id[1] == sha)
                    if(!isWritable)
                        text += (if (text.isNotEmpty()) "\n" else "") + it

                    //テキストに書き込み、まだ書き込んでおらず、かつ書き込み領域内であるなら、更新データに差し替える
                    if(!isWrote && isWritable) {
                        text += (if (text.isNotEmpty()) "\n" else "") + "[$filePath,$sha]"
                        update.forEach { text += "\n${it.key}=${it.value}" }
                    }

                    //書き込み終わったかどうかを更新
                    isWrote = isWritable or isWrote
                } else if(!isWritable) {
                    text += (if (text.isNotEmpty()) "\n" else "") + it
                }
            }

        //まだ書き込んでいないならば、テキスト末尾に書き込んでおく
        if(!isWrote) {
            text += (if (text.isNotEmpty()) "\n" else "") + "[$filePath,$sha]"
            update.forEach { text += "\n${it.key}=${it.value}" }
        }

        //更新されたテキストをデータファイルに書き込む
        data.writeText(text)
    }
}

