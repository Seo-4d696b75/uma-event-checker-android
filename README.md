# uma-event-checker-android
Androidでも「うまぴょい！」育成時のイベントを認識して選択肢を表示

<img src="https://user-images.githubusercontent.com/25225028/125165584-883ccd80-e1d2-11eb-8a60-60d5609f4927.gif" width="280">

[詳細はQiitaで説明があります](https://qiita.com/Seo-4d696b75/items/c5bddf239c198cca5ecf)

# 開発のセットアップ

## OCR学習モデル
オープンソフトで有名な[Tesseract](https://github.com/tesseract-ocr/tesseract)を利用。日本語を識別するため、
[GitHubリポジトリ](https://github.com/tesseract-ocr/tessdata) から日本語の学習モデルを取得すること。
データファイルは`app/src/main/assets/jpn.traineddata`に保存する。

その他、詳細は[公式のドキュメント](https://tesseract-ocr.github.io/tessdoc/)

## イベントデータの確認

[こちらのリポジトリで管理するデータ](https://github.com/Seo-4d696b75/uma-event-data) を利用します

## OpenCVの用意

適宜バージョンは読み替えてください

1. [OpenCV Releases](https://opencv.org/releases/) から目的のバージョンのAndroidをダウンロード
2. 解凍して`sdk`フォルダを移動 `mv ~/Download/opencv-4.5.2-android-sdk/sdk ${path2sdk}`
3. AndroidStudioでModuleとしてimport モジュール名は`opencv-4.5.2`とする
4. `app`モジュールに依存を追加

```diff:app/build.gradle
+    implementation project(path: 'opencv-4.5.2')
```

### Build署名の用意

1. keystoreファイル

ファイルは`app/release.jks`で保存します.
key alias は`key0`を指定します.

2. パスワードの指定

`app/gradle.properties`に記述します.

**このファイルは.gitignoreに追加されています**

```shell
release_keystore_pwd=${keystore_password}
release_key_pwd=${key_password}
```
