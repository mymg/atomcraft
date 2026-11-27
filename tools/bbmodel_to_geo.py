#!/usr/bin/env python3
"""Converte um .bbmodel (Blockbench, GeckoLib ou Bedrock com UV por face) em .geo.json do GeckoLib.

Uso: python3 tools/bbmodel_to_geo.py modelo.bbmodel saida.geo.json [textura.png]
Faz o mesmo que "File > Export > Export GeckoLib Model" do Blockbench: grupos viram bones (x invertido),
cubos com "export" desligado (os "dontTouch" do modelo de armadura) ficam de fora, e a textura embutida
pode ser salva junto.
"""
import base64, json, sys


def r(v):
    v = round(v, 4)
    return int(v) if v == int(v) else v


def convert(model):
    elements = {e["uuid"]: e for e in model["elements"]}
    groups = {g["uuid"]: g for g in model["groups"]}
    width, height = model["resolution"]["width"], model["resolution"]["height"]
    bones = []

    def cube(e):
        f, t = e["from"], e["to"]
        size = [r(t[i] - f[i]) for i in range(3)]
        c = {"origin": [r(-t[0]), r(f[1]), r(f[2])], "size": size}
        if e.get("inflate"):
            c["inflate"] = e["inflate"]
        rot = e.get("rotation")
        if rot and any(rot):
            o = e.get("origin", [0, 0, 0])
            c["pivot"] = [r(-o[0]), r(o[1]), r(o[2])]
            c["rotation"] = [r(-rot[0]), r(-rot[1]), r(rot[2])]
        uv = {}
        for key, face in e["faces"].items():
            if face.get("texture") is None:
                continue
            u = face["uv"]
            start, sz = [u[0], u[1]], [u[2] - u[0], u[3] - u[1]]
            if key in ("up", "down"):
                start = [start[0] + sz[0], start[1] + sz[1]]
                sz = [-sz[0], -sz[1]]
            entry = {"uv": [r(start[0]), r(start[1])], "uv_size": [r(sz[0]), r(sz[1])]}
            if face.get("rotation"):
                entry["uv_rotation"] = face["rotation"]
            uv[key] = entry
        c["uv"] = uv
        return c

    def walk(node, parent):
        g = groups[node["uuid"]]
        o = g.get("origin", [0, 0, 0])
        bone = {"name": g["name"], "pivot": [r(-o[0]), r(o[1]), r(o[2])]}
        if parent:
            bone["parent"] = parent
        rot = g.get("rotation")
        if rot and any(rot):
            bone["rotation"] = [r(-rot[0]), r(-rot[1]), r(rot[2])]
        cubes = []
        bones.append(bone)
        for child in node.get("children", []):
            if isinstance(child, str):
                e = elements.get(child)
                if e and e.get("export", True) and e.get("type", "cube") == "cube":
                    cubes.append(cube(e))
            else:
                walk(child, g["name"])
        if cubes:
            bone["cubes"] = cubes

    loose = []
    for node in model["outliner"]:
        if isinstance(node, str):
            e = elements.get(node)
            if e and e.get("export", True):
                loose.append(cube(e))
        else:
            walk(node, None)
    if loose:
        bones.append({"name": "root", "pivot": [0, 0, 0], "cubes": loose})
    return {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry.unknown", "texture_width": width, "texture_height": height,
                        "visible_bounds_width": 3, "visible_bounds_height": 3.5, "visible_bounds_offset": [0, 1.25, 0]},
        "bones": bones}]}


def save_texture(model, path):
    src = model["textures"][0]["source"]
    with open(path, "wb") as f:
        f.write(base64.b64decode(src.split(",", 1)[1]))


if __name__ == "__main__":
    data = json.load(open(sys.argv[1]))
    json.dump(convert(data), open(sys.argv[2], "w"), indent=1)
    if len(sys.argv) > 3:
        save_texture(data, sys.argv[3])
    print("ok")
