import requests
import json

data = json.load(open('icon.json', 'r', encoding='utf-8'))

def get_all(data):
    for d in data:
        name = d['i']
        url = f"https://img.gamewith.jp/article_tools/uma-musume/gacha/{name}"
        response = requests.get(url)
        image = response.content
        with open(f"./icon/{name}", "wb") as aaa:
            aaa.write(image)

get_all(data['support'])
get_all(data['chara'])