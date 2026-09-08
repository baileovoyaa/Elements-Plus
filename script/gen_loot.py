import json

blocks = [
    "steel_pipe_l",
    "steel_pipe_i",
    "steel_pipe_t",
    "steel_pipe_x",
    "waxed_steel_pipe_l",
    "waxed_steel_pipe_i",
    "waxed_steel_pipe_t",
    "waxed_steel_pipe_x",
    "rust_steel_pipe_l",
    "rust_steel_pipe_i",
    "rust_steel_pipe_t",
    "rust_steel_pipe_x",
    "silver_pipe_l",
    "silver_pipe_i",
    "silver_pipe_t",
    "silver_pipe_x",
]

for b in blocks:
    data = {
        "type": "minecraft:block",
        "pools": [
            {
                "bonus_rolls": 0.0,
                "conditions": [
                    {
                        "condition": "minecraft:survives_explosion"
                    }
                ],
                "entries": [
                    {
                        "type": "minecraft:item",
                        "name": f"elements-plus:{b}"
                    }
                ],
                "rolls": 1.0
            }
        ],
        "random_sequence": f"elements-plus:blocks/{b}"
    }

    with open(f"output/{b}.json", "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=4)
