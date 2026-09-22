import json

item = [
    "small_chip",
    "medium_chip",
    "large_chip",
    "huge_chip",
    "small_empty_chip",
    "medium_empty_chip",
    "large_empty_chip",
    "huge_empty_chip",
]

for b in item:
    data = {
        "parent": "item/generated",
        "textures": {
            "layer0": f"elements-plus:item/{b}"
        }
    }

    with open(f"output/models/{b}.json", "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=4)
