# uma-event-checker-android
Androidでも「うまぴょい！」育成時のイベントを認識して選択肢を表示

<img src="https://user-images.githubusercontent.com/25225028/125165584-883ccd80-e1d2-11eb-8a60-60d5609f4927.gif" width="280">

[詳細はQiitaで説明があります](https://qiita.com/Seo-4d696b75/items/c5bddf239c198cca5ecf)

# データの用意

## OCR学習モデル
オープンソフトで有名な[Tesseract](https://github.com/tesseract-ocr/tesseract)を利用。日本語を識別するため、適宜[GitHubリポジトリ](https://github.com/tesseract-ocr/tessdata)から日本語の学習モデルを取得すること。その他、詳細は[公式のドキュメント](https://tesseract-ocr.github.io/tessdoc/)  
`app/src/main/assets/jpn.traineddata`   

## イベントデータ
当然ですが、検索するイベントのデータが必要です。ここでは、参考先でも言及があったとおり[GameWith ウマ娘攻略wiki](https://gamewith.jp/uma-musume)にあるイベント選択肢チェッカーなる便利サイトから拝借。該当ページでは`js`ファイルに直書きされたデータを利用しているようで、developerツールで覗いてごにょごにょすると見つけられます。  

ちょちょっと修正してjsonで保存。  
`app/src/main/assets/event.json`  
