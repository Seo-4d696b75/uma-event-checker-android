# uma-event-checker-android

Androidでも「うまぴょい！」育成時のイベントを認識して選択肢を表示

<img src="https://user-images.githubusercontent.com/25225028/125165584-883ccd80-e1d2-11eb-8a60-60d5609f4927.gif" width="280">

[詳細はQiitaで説明があります](https://qiita.com/Seo-4d696b75/items/c5bddf239c198cca5ecf)

# Projectの構成

機能ごとにモジュールを分割しています

- :app アプリ本体
- :data ゲームのイベントデータを管理・検索
- :img キャプチャした画像からイベントタイトルを検出
- :opencv-4.5.2 画像処理に利用

モジュールに共通するライブラリのバージョンは`buildSrc`で定義した値を参照しています

# 開発のセットアップ

## OCR学習モデル

オープンソフトで有名な[Tesseract](https://github.com/tesseract-ocr/tesseract)を利用。日本語を識別するため、
[GitHubリポジトリ](https://github.com/tesseract-ocr/tessdata) から日本語の学習モデルを取得すること。
データファイルは`img/src/main/assets/jpn.traineddata`に保存する。

その他Tesseractの詳細は[公式のドキュメント](https://tesseract-ocr.github.io/tessdoc/)

## イベントデータ

検索するイベントのデータが必要です。ここでは、Qiita記事でも言及したとおり
[GameWith ウマ娘攻略wiki](https://gamewith.jp/uma-musume)にあるイベント選択肢チェッカーなる便利サイトから拝借。
該当ページでは`js`ファイルに直書きされたデータを利用しているようで、developerツールで覗いてごにょごにょすると見つけられます。

ちょちょっと修正してjsonで保存。  
`data/src/main/assets/event.json`

```json
[
  {
    "e": "今日も、明日からも",
    "n": "スペシャルウィーク",
    "c": "c",
    "l": "通常",
    "a": "257412",
    "k": "きょうもあすからも_じゃあついかとれーにんぐね_ゆっくりやすんであすにそなえよう",
    "choices": [
      {
        "n": "じゃあ追加トレーニングね",
        "t": "スピード+20[br]ランダムで『注目株』取得"
      },
      {
        "n": "ゆっくり休んで明日に備えよう",
        "t": "賢さ+20[br]ランダムで『注目株』取得"
      }
    ]
  }
]
```

## OpenCVの用意

適宜バージョンは読み替えてください

1. [OpenCV Releases](https://opencv.org/releases/) から目的のバージョンのAndroidをダウンロード
2. 解凍して`sdk`フォルダを移動 `mv ~/Download/opencv-4.5.2-android-sdk/sdk ${path2projectRoot}`
3. AndroidStudioでModuleとしてimport モジュール名は`opencv-4.5.2`とする
4. `img`モジュールに依存を追加

```diff:img/build.gradle.kts
+    implementation(project("path" to "opencv-4.5.2"))
```

