"""
Gera os recursos do AtomCraft (dados, traduções e os modelos/texturas iniciais).

Uso (na raiz do repositório):  python3 tools/gen_resources.py .
Requer Pillow (pip install pillow).

- data/ e os arquivos de tradução são sempre recriados.
- Modelos, blockstates, definições de itens e texturas só são criados quando
  ainda não existem: arquivos editados à mão (por exemplo no Blockbench) são
  preservados. Use --force para recriar tudo do zero.
"""
import json, os, re, shutil, sys
from PIL import Image, ImageDraw

ROOT = sys.argv[1] if len(sys.argv) > 1 and not sys.argv[1].startswith("--") else "."
FORCE = "--force" in sys.argv
R = os.path.join(ROOT, "src/main/resources")
shutil.rmtree(os.path.join(R, "data"), ignore_errors=True)
if FORCE:
    shutil.rmtree(os.path.join(R, "assets"), ignore_errors=True)

# Arquivos de aparência: nunca sobrescritos (a não ser com --force).
ART = ("assets/atomcraft/models/", "assets/atomcraft/blockstates/", "assets/atomcraft/items/", "assets/atomcraft/textures/")
kept = []

def preserved(path):
    if path.startswith(ART) and os.path.exists(os.path.join(R, path)):
        kept.append(path)
        return True
    return False

def w(path, data):
    if preserved(path):
        return
    p = os.path.join(R, path)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8") as f:
        f.write(json.dumps(data, indent=2, ensure_ascii=False) + "\n")

def parse(formula):
    """'Si1 O2' -> {'Si':1,'O':2}"""
    out = {}
    for tok in formula.split():
        m = re.fullmatch(r"([A-Z][a-z]?)(\d+)", tok)
        assert m, tok
        out[m.group(1)] = out.get(m.group(1), 0) + int(m.group(2))
    return out

def add(*fs):
    out = {}
    for f in fs:
        for k, v in parse(f).items():
            out[k] = out.get(k, 0) + v
    return " ".join(f"{k}{v}" for k, v in out.items())

SIO2 = "Si1 O2"
BUCKET = "Fe27"
MEAT = "C10 H16 N3 O4"
LAPIS = "Na3 Ca1 Al3 Si3 O12 S1"
EMERALD = "Be24 Al16 Si48 O144"

# (id, formula, rarity or None). Itens sem entrada recebem valor pelas receitas.
BASE = [
    # Rochas e terra
    ("stone", SIO2, None), ("cobblestone", SIO2, None),
    ("deepslate", "Si1 O2 Mg1", None), ("cobbled_deepslate", "Si1 O2 Mg1", None),
    ("granite", "Si1 O2 Al1 K1", None), ("diorite", "Si1 O2 Ca1 Na1", None),
    ("andesite", "Si1 O2 Na1 Al1", None), ("tuff", "Si1 O2 Al1", None),
    ("calcite", "Ca1 C1 O3", None), ("dripstone_block", "Ca1 C1 O3", None),
    ("pointed_dripstone", "Ca1 C1 O3", None),
    ("dirt", "Si1 O2 C1 H2", None), ("grass_block", "Si1 O2 C2 H4 N1", None),
    ("podzol", "Si1 O2 C2 H4 N1", None), ("mycelium", "Si1 O2 C2 H4 N1", None),
    ("mud", "Si1 O3 C1 H4", None),
    ("sand", SIO2, None), ("red_sand", "Si1 O2 Fe1", None), ("gravel", SIO2, None),
    ("clay_ball", "Al2 Si2 O9 H4", None), ("flint", "Si2 O4", None),
    ("netherrack", "Si1 O2 S1", None), ("soul_sand", "Si1 O2 C2", None), ("soul_soil", "Si1 O2 C2", None),
    ("basalt", "Si1 O2 Mg1 Fe1", None), ("blackstone", "Si1 O2 Fe1 Mg1", None),
    ("end_stone", "Si1 O2 En1", None),
    ("obsidian", "Si2 O4 Mg1 Fe1", "uncommon"), ("crying_obsidian", "Si2 O4 Mg1 Fe1 Lm1", "rare"),
    ("snowball", "H2 O1", None), ("ice", "H8 O4", None),
    ("water_bucket", add(BUCKET, "H16 O8"), None),
    ("lava_bucket", add(BUCKET, "Si8 O16 Mg4 Fe4"), "uncommon"),
    ("milk_bucket", add(BUCKET, "C6 H12 O6 Ca1 N1 P1"), None),
    ("powder_snow_bucket", add(BUCKET, "H16 O8"), None),
    # Plantas
    ("bamboo", "C6 H10 O5", None), ("wheat", "C6 H10 O5", None), ("wheat_seeds", "C2 H3 O1 N1", None),
    ("beetroot_seeds", "C2 H3 O1 N1", None), ("melon_seeds", "C2 H3 O1 N1", None),
    ("pumpkin_seeds", "C2 H3 O1 N1", None), ("torchflower_seeds", "C2 H3 O1 N1", None),
    ("apple", "C6 H12 O6", None), ("carrot", "C6 H12 O6", None), ("potato", "C6 H10 O5", None),
    ("beetroot", "C6 H12 O6", None), ("melon_slice", "C3 H6 O3", None), ("pumpkin", "C12 H20 O10", None),
    ("sugar_cane", "C12 H22 O11", None), ("cactus", "C6 H12 O6", None), ("kelp", "C6 H10 O5 Na1", None),
    ("sweet_berries", "C3 H6 O3", None), ("glow_berries", "C3 H6 O3 Lm1", None),
    ("cocoa_beans", "C6 H6 O2 N1", None), ("vine", "C2 H4 O2", None), ("lily_pad", "C3 H6 O3", None),
    ("brown_mushroom", "C3 H6 O2 N1", None), ("red_mushroom", "C3 H6 O2 N1", None),
    ("nether_wart", "C4 H6 O2 N1 S1", None), ("chorus_fruit", "C6 H12 O6 En1", None),
    ("seagrass", "C2 H4 O2", None), ("short_grass", "C2 H4 O2", None), ("fern", "C2 H4 O2", None),
    # Animais
    ("egg", "Ca1 C2 O3 H2", None), ("feather", "C5 H8 N2 O2 S1", None), ("leather", MEAT, None),
    ("rabbit_hide", "C5 H8 N2 O2", None), ("string", "C6 H10 N2 O3", None),
    ("bone", "Ca5 P3 O13 H1", None), ("gunpowder", "K1 N1 O3 C1 S1", None),
    ("slime_ball", "C6 H12 O6 N1", None), ("ink_sac", "C4 H6 O2 N1", None),
    ("glow_ink_sac", "C4 H6 O2 N1 Lm1", None), ("honeycomb", "C15 H30 O2", None),
    ("honey_bottle", add(SIO2, "C12 H22 O11"), None),
    ("spider_eye", "C6 H12 O6 N1", None), ("rotten_flesh", MEAT, None),
    ("beef", MEAT, None), ("porkchop", MEAT, None), ("mutton", MEAT, None), ("chicken", MEAT, None),
    ("rabbit", MEAT, None), ("cod", add(MEAT, "P1"), None), ("salmon", add(MEAT, "P1"), None),
    ("tropical_fish", add(MEAT, "P1"), None), ("pufferfish", add(MEAT, "P1"), None),
    ("ender_pearl", "En4", "uncommon"), ("blaze_rod", "Lm2 S2 C2", "uncommon"),
    ("breeze_rod", "Lm2 O4 N2", "uncommon"),
    ("ghast_tear", "Lm2 H4 O2 Na1 Cl1", "rare"), ("phantom_membrane", add(MEAT, "En1"), "uncommon"),
    ("shulker_shell", "En4 Ca2 C2 O6", "rare"), ("nautilus_shell", "Ca8 C8 O24", "rare"),
    ("heart_of_the_sea", "Lm8 Ca4 C4 O12", "epic"),
    ("prismarine_shard", "Si2 O4 Mg1", "uncommon"), ("prismarine_crystals", "Si1 O2 Lm1", "uncommon"),
    ("rabbit_foot", add(MEAT, "Lm1"), "uncommon"), ("turtle_scute", add(MEAT, "Ca2"), "uncommon"),
    ("armadillo_scute", add(MEAT, "Ca2"), "uncommon"),
    ("echo_shard", "Sk4", "epic"), ("nether_star", "Sx64", "legendary"),
    ("dragon_breath", "En8", "rare"), ("dragon_egg", "Sx32 En32", "legendary"),
    ("elytra", add("En16", MEAT, MEAT), "legendary"),
    ("totem_of_undying", add("Au36", "Be3 Al2 Si6 O18", "Lm4"), "epic"),
    ("trident", add("Ca8 C8 O24", "Fe9 Lm4"), "epic"),
    ("enchanted_golden_apple", add("Au648", "C6 H12 O6", "Lm8"), "legendary"),
    # Minerais
    ("coal", "C24", None), ("charcoal", "C24", None), ("diamond", "C512", "rare"),
    ("emerald", EMERALD, "rare"), ("lapis_lazuli", LAPIS, "uncommon"), ("redstone", "Rs4", None),
    ("quartz", "Si4 O8", None), ("amethyst_shard", "Si4 O8 Fe1", "uncommon"),
    ("glowstone_dust", "Lm2 P1", "uncommon"),
    ("raw_iron", "Fe9", None), ("iron_ingot", "Fe9", None),
    ("raw_copper", "Cu9", None), ("copper_ingot", "Cu9", None),
    ("raw_gold", "Au9", "uncommon"), ("gold_ingot", "Au9", "uncommon"),
    ("netherite_scrap", "Nt8", "epic"), ("ancient_debris", add("Nt8", SIO2), "epic"),
]
ORES = [
    ("coal_ore", "C24"), ("iron_ore", "Fe9"), ("copper_ore", "Cu27"), ("gold_ore", "Au9"),
    ("diamond_ore", "C512"), ("emerald_ore", EMERALD), ("lapis_ore", add(LAPIS, LAPIS, LAPIS, LAPIS)),
    ("redstone_ore", "Rs16"),
]
for ore, content in ORES:
    rarity = {"diamond_ore": "rare", "emerald_ore": "rare", "gold_ore": "uncommon", "lapis_ore": "uncommon"}.get(ore)
    BASE.append((ore, add(content, SIO2), rarity))
    BASE.append(("deepslate_" + ore, add(content, "Si1 O2 Mg1"), rarity))
BASE += [("nether_quartz_ore", "Si5 O10 S1", None), ("nether_gold_ore", "Au9 Si1 O2 S1", "uncommon")]

TAGS = [
    ("logs", "C24 H40 O20", None), ("leaves", "C2 H4 O2 N1", None),
    ("saplings", "C4 H6 O3", None), ("wool", "C24 H40 N8 O12", None),
    ("small_flowers", "C2 H4 O2", None),
]

for item, formula, rarity in BASE:
    entry = {"item": f"minecraft:{item}", "elements": parse(formula)}
    if rarity: entry["rarity"] = rarity
    w(f"data/atomcraft/atomcraft/compositions/{item}.json", entry)
for tag, formula, rarity in TAGS:
    entry = {"tag": f"minecraft:{tag}", "elements": parse(formula)}
    if rarity: entry["rarity"] = rarity
    w(f"data/atomcraft/atomcraft/compositions/tag_{tag}.json", entry)

# ---------- Compatibilidade com outros mods (tags comuns "c:" do NeoForge) ----------
# Cada material tem a composição de UMA barra (9 átomos, como Fe9 na barra de ferro).
# As formas derivam dela: pepita 1/9, pó/chapa/minério bruto = barra, engrenagem 4×,
# bastão 1/2, bloco 9×. Itens com entrada própria (ex.: minecraft:iron_ingot) têm prioridade,
# e itens que outros mods fabricam por receita recebem valor pela derivação normal.
METALS = [
    # (material, fórmula da barra, raridade, tem minério)
    ("iron", "Fe9", None, False), ("copper", "Cu9", None, False), ("gold", "Au9", "uncommon", False),
    ("tin", "Sn9", None, True), ("lead", "Pb9", None, True), ("nickel", "Ni9", None, True),
    ("zinc", "Zn9", None, True), ("aluminum", "Al9", None, True), ("silver", "Ag9", "uncommon", True),
    ("osmium", "Os9", "rare", True), ("platinum", "Pt9", "rare", True), ("iridium", "Ir9", "rare", True),
    ("uranium", "U9", "rare", True),
    # Ligas
    ("bronze", "Cu7 Sn2", None, False), ("brass", "Cu6 Zn3", None, False), ("steel", "Fe8 C1", None, False),
    ("invar", "Fe6 Ni3", None, False), ("constantan", "Cu5 Ni4", None, False),
    ("electrum", "Au5 Ag4", "uncommon", False),
    # All the Mods (Allthemodium): metais fictícios → elementos exóticos
    ("allthemodium", "Ad9", "epic", True), ("vibranium", "Vb9", "legendary", True),
    ("unobtainium", "Ub9", "legendary", True),
]

def scaled(formula, factor):
    out = []
    for k, v in parse(formula).items():
        n = v * factor
        out.append(f"{k}{max(1, int(round(n)))}")
    return " ".join(out)

COMPAT = []
for metal, ingot, rarity, has_ore in METALS:
    forms = [("ingots", 1), ("nuggets", 1 / 9), ("raw_materials", 1), ("dusts", 1), ("plates", 1),
             ("gears", 4), ("rods", 0.5), ("wires", 0.5), ("storage_blocks", 9)]
    for form, factor in forms:
        COMPAT.append((f"c:{form}/{metal}", scaled(ingot, factor), rarity))
    if has_ore:
        COMPAT.append((f"c:storage_blocks/raw_{metal}", scaled(ingot, 9), rarity))
        COMPAT.append((f"c:ores/{metal}", add(ingot, SIO2), rarity))

GEMS = [
    # (gema, fórmula de uma gema, raridade)
    ("ruby", "Al4 O6", "uncommon"), ("sapphire", "Al4 O6", "uncommon"), ("peridot", "Mg4 Si2 O8", "uncommon"),
    ("certus_quartz", "Si4 O8", None), ("fluix", "Si4 O8 Rs2", "uncommon"),
]
for gem, formula, rarity in GEMS:
    COMPAT.append((f"c:gems/{gem}", formula, rarity))
    COMPAT.append((f"c:dusts/{gem}", formula, rarity))
    COMPAT.append((f"c:storage_blocks/{gem}", scaled(formula, 9), rarity))
    COMPAT.append((f"c:ores/{gem}", add(formula, SIO2), rarity))
COMPAT += [
    ("c:dusts/sulfur", "S4", None), ("c:gems/sulfur", "S4", None), ("c:dusts/saltpeter", "K2 N2 O6", None),
    ("c:dusts/coal", "C24", None), ("c:dusts/charcoal", "C24", None), ("c:dusts/diamond", "C512", "rare"),
    ("c:dusts/obsidian", "Si2 O4 Mg1 Fe1", None), ("c:dusts/quartz", "Si4 O8", None),
    ("c:dusts/lapis", LAPIS, "uncommon"), ("c:dusts/emerald", EMERALD, "rare"),
    ("c:silicon", "Si4", None),
]
for tag, formula, rarity in COMPAT:
    entry = {"tag": tag, "elements": parse(formula)}
    if rarity: entry["rarity"] = rarity
    w(f"data/atomcraft/atomcraft/compositions/compat/{tag.replace(':', '_').replace('/', '_')}.json", entry)

# ---------- Blocos ----------
MACHINES = ["atomizer", "analyzer", "replicator", "drive", "terminal", "combustion_generator", "carbon_generator", "fusion_reactor", "synthesizer"]
for b in MACHINES:
    w(f"assets/atomcraft/blockstates/{b}.json", {"variants": {
        f"facing={d}": ({"model": f"atomcraft:block/{b}"} | ({"y": y} if y else {}))
        for d, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
    w(f"assets/atomcraft/models/block/{b}.json", {"parent": "minecraft:block/orientable", "textures": {
        "top": f"atomcraft:block/{b}_top", "front": f"atomcraft:block/{b}_front",
        "side": f"atomcraft:block/machine_side", "particle": "atomcraft:block/machine_side"}})
    w(f"assets/atomcraft/items/{b}.json", {"model": {"type": "minecraft:model", "model": f"atomcraft:block/{b}"}})

# Cabo
def face(uv):
    return {"uv": uv, "texture": "#cable"}
w("assets/atomcraft/models/block/cable_core.json", {"textures": {"cable": "atomcraft:block/atomic_cable", "particle": "atomcraft:block/atomic_cable"},
    "elements": [{"from": [5, 5, 5], "to": [11, 11, 11], "faces": {f: face([5, 5, 11, 11]) for f in ("north", "south", "east", "west", "up", "down")}}]})
w("assets/atomcraft/models/block/cable_side.json", {"textures": {"cable": "atomcraft:block/atomic_cable", "particle": "atomcraft:block/atomic_cable"},
    "elements": [{"from": [6, 6, 0], "to": [10, 10, 5], "faces": {
        "north": face([6, 6, 10, 10]), "east": face([0, 6, 5, 10]), "west": face([0, 6, 5, 10]),
        "up": face([6, 0, 10, 5]), "down": face([6, 0, 10, 5])}}]})
w("assets/atomcraft/models/block/cable_item.json", {"parent": "minecraft:block/block", "textures": {"cable": "atomcraft:block/atomic_cable", "particle": "atomcraft:block/atomic_cable"},
    "elements": [
        {"from": [5, 5, 5], "to": [11, 11, 11], "faces": {f: face([5, 5, 11, 11]) for f in ("north", "south", "east", "west", "up", "down")}},
        {"from": [6, 6, 0], "to": [10, 10, 16], "faces": {"north": face([6, 6, 10, 10]), "south": face([6, 6, 10, 10]),
            "east": face([0, 6, 16, 10]), "west": face([0, 6, 16, 10]), "up": face([6, 0, 10, 16]), "down": face([6, 0, 10, 16])}}]})
multipart = [{"apply": {"model": "atomcraft:block/cable_core"}}]
for d, rot in (("north", {}), ("east", {"y": 90}), ("south", {"y": 180}), ("west", {"y": 270}), ("up", {"x": 270}), ("down", {"x": 90})):
    multipart.append({"when": {d: "true"}, "apply": {"model": "atomcraft:block/cable_side", "uvlock": True} | rot})
w("assets/atomcraft/blockstates/atomic_cable.json", {"multipart": multipart})
w("assets/atomcraft/items/atomic_cable.json", {"model": {"type": "minecraft:model", "model": "atomcraft:block/cable_item"}})

# Discos
DISKS = {"element_disk_16k": (90, 200, 120), "element_disk_64k": (230, 190, 60), "element_disk_256k": (90, 220, 230), "element_disk_1m": (190, 110, 230)}
for d in DISKS:
    w(f"assets/atomcraft/models/item/{d}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"atomcraft:item/{d}"}})
    w(f"assets/atomcraft/items/{d}.json", {"model": {"type": "minecraft:model", "model": f"atomcraft:item/{d}"}})

# Chips e schematics
CHIPS = ["speed_upgrade", "efficiency_upgrade", "speed_upgrade_schematic", "efficiency_upgrade_schematic"]
for d in CHIPS:
    w(f"assets/atomcraft/models/item/{d}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"atomcraft:item/{d}"}})
    w(f"assets/atomcraft/items/{d}.json", {"model": {"type": "minecraft:model", "model": f"atomcraft:item/{d}"}})

# Schematics nos baús do mundo (NeoForge global loot modifiers)
LOOT = {
    "village": (["village/village_toolsmith", "village/village_weaponsmith", "village/village_armorer",
                 "village/village_cartographer", "village/village_temple", "village/village_mason"], 3, 1, 16),
    "nether": (["nether_bridge", "bastion_treasure"], 2, 2, 6),
    "end": (["end_city_treasure"], 1, 3, 4),
}
for name, (tables, speed, eff, empty) in LOOT.items():
    w(f"data/atomcraft/loot_table/chests/schematics_{name}.json", {"type": "minecraft:chest", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": "atomcraft:speed_upgrade_schematic", "weight": speed},
        {"type": "minecraft:item", "name": "atomcraft:efficiency_upgrade_schematic", "weight": eff},
        {"type": "minecraft:empty", "weight": empty}]}]})
    w(f"data/atomcraft/loot_modifiers/schematics_{name}.json", {"type": "neoforge:add_table",
        "conditions": [{"condition": "minecraft:any_of", "terms": [
            {"condition": "neoforge:loot_table_id", "loot_table_id": f"minecraft:chests/{t}"} for t in tables]}],
        "table": f"atomcraft:chests/schematics_{name}"})
# Nesta versão do NeoForge cada arquivo em loot_modifiers/ é carregado sozinho (sem lista global).

# Loot tables e tags
for b in MACHINES + ["atomic_cable"]:
    w(f"data/atomcraft/loot_table/blocks/{b}.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
        "entries": [{"type": "minecraft:item", "name": f"atomcraft:{b}"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
w("data/minecraft/tags/block/mineable/pickaxe.json", {"values": [f"atomcraft:{b}" for b in MACHINES + ["atomic_cable"]]})
w("data/minecraft/tags/block/needs_iron_tool.json", {"values": [f"atomcraft:{b}" for b in MACHINES]})

# Receitas
def shaped(name, pattern, key, result, count=1):
    w(f"data/atomcraft/recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": "misc",
        "pattern": pattern, "key": key, "result": {"id": result, "count": count}})
IRON, RED, AME = "minecraft:iron_ingot", "minecraft:redstone", "minecraft:amethyst_shard"
shaped("atomizer", ["IAI", "ARA", "IRI"], {"I": IRON, "A": AME, "R": RED}, "atomcraft:atomizer")
shaped("analyzer", ["IRI", "AAA", "IRI"], {"I": IRON, "A": AME, "R": RED}, "atomcraft:analyzer")
shaped("replicator", ["DED", "IRI", "DED"], {"D": "minecraft:diamond", "E": "minecraft:ender_pearl", "I": IRON, "R": "minecraft:redstone_block"}, "atomcraft:replicator")
shaped("drive", ["IRI", "ICI", "IAI"], {"I": IRON, "R": RED, "C": "minecraft:chest", "A": AME}, "atomcraft:drive")
shaped("terminal", ["IGI", "ARA", "III"], {"I": IRON, "G": "minecraft:glass", "A": AME, "R": RED}, "atomcraft:terminal")
shaped("combustion_generator", ["III", "IFI", "CRC"], {"I": IRON, "F": "minecraft:furnace", "C": "minecraft:copper_ingot", "R": RED}, "atomcraft:combustion_generator")
shaped("carbon_generator", ["IAI", "AGA", "IBI"], {"I": IRON, "A": AME, "G": "atomcraft:combustion_generator", "B": "minecraft:blast_furnace"}, "atomcraft:carbon_generator")
shaped("fusion_reactor", ["IEI", "DSD", "IEI"], {"I": "minecraft:iron_block", "E": "minecraft:end_crystal", "D": "minecraft:diamond_block", "S": "minecraft:nether_star"}, "atomcraft:fusion_reactor")
shaped("synthesizer", ["GAG", "RCR", "GAG"], {"G": "minecraft:gold_ingot", "A": AME, "R": RED, "C": "minecraft:crafting_table"}, "atomcraft:synthesizer")
shaped("atomic_cable", ["NNN", "RAR", "NNN"], {"N": "minecraft:iron_nugget", "R": RED, "A": AME}, "atomcraft:atomic_cable", 8)
shaped("element_disk_16k", ["ARA", "RIR", "III"], {"A": AME, "R": RED, "I": IRON}, "atomcraft:element_disk_16k")
shaped("element_disk_64k", ["GRG", "RAR", "GGG"], {"G": "minecraft:gold_ingot", "R": RED, "A": AME}, "atomcraft:element_disk_64k")
shaped("element_disk_256k", ["DRD", "RAR", "DDD"], {"D": "minecraft:diamond", "R": RED, "A": AME}, "atomcraft:element_disk_256k")
shaped("element_disk_1m", ["SRS", "RER", "SSS"], {"S": "minecraft:shulker_shell", "R": RED, "E": "minecraft:ender_eye"}, "atomcraft:element_disk_1m")

# ---------- Traduções ----------
ELEMENTS = [("hydrogen", "Hydrogen", "Hidrogênio"), ("helium", "Helium", "Hélio"),
    ("nickel", "Nickel", "Níquel"), ("zinc", "Zinc", "Zinco"), ("silver", "Silver", "Prata"), ("tin", "Tin", "Estanho"),
    ("osmium", "Osmium", "Ósmio"), ("iridium", "Iridium", "Irídio"), ("platinum", "Platinum", "Platina"),
    ("lead", "Lead", "Chumbo"), ("uranium", "Uranium", "Urânio"),
    ("allthemodium", "Allthemodium", "Allthemódio"), ("vibranium", "Vibranium", "Vibrânio"),
    ("unobtainium", "Unobtainium", "Unobtânio"), ("beryllium", "Beryllium", "Berílio"), ("carbon", "Carbon", "Carbono"),
    ("nitrogen", "Nitrogen", "Nitrogênio"), ("oxygen", "Oxygen", "Oxigênio"), ("sodium", "Sodium", "Sódio"),
    ("magnesium", "Magnesium", "Magnésio"), ("aluminium", "Aluminium", "Alumínio"), ("silicon", "Silicon", "Silício"),
    ("phosphorus", "Phosphorus", "Fósforo"), ("sulfur", "Sulfur", "Enxofre"), ("chlorine", "Chlorine", "Cloro"),
    ("potassium", "Potassium", "Potássio"), ("calcium", "Calcium", "Cálcio"), ("iron", "Iron", "Ferro"),
    ("copper", "Copper", "Cobre"), ("gold", "Gold", "Ouro"), ("redstonium", "Redstonium", "Redstônio"),
    ("luminium", "Luminium", "Lumínio"), ("enderium", "Enderium", "Endério"), ("netherium", "Netherium", "Netherio"),
    ("sculkium", "Sculkium", "Sculkio"), ("stellarium", "Stellarium", "Estelário")]
common = {
    "block.atomcraft.atomizer": ("Atomizer", "Atomizador"),
    "block.atomcraft.analyzer": ("Analyzer", "Analisador"),
    "block.atomcraft.replicator": ("Atomic Replicator", "Replicador Atômico"),
    "block.atomcraft.drive": ("Element Drive", "Drive de Elementos"),
    "block.atomcraft.atomic_cable": ("Atomic Cable", "Cabo Atômico"),
    "block.atomcraft.terminal": ("Element Terminal", "Terminal de Elementos"),
    "block.atomcraft.combustion_generator": ("Combustion Generator", "Gerador de Combustão"),
    "block.atomcraft.carbon_generator": ("Carbon Generator", "Gerador de Carbono"),
    "block.atomcraft.fusion_reactor": ("Fusion Reactor", "Reator de Fusão"),
    "status.atomcraft.no_fuel": ("No fuel", "Sem combustível"),
    "status.atomcraft.disabled": ("Off", "Desligado"),
    "block.atomcraft.synthesizer": ("Synthesizer", "Sintetizador"),
    "item.atomcraft.speed_upgrade": ("Speed Upgrade Chip", "Chip de Velocidade"),
    "item.atomcraft.efficiency_upgrade": ("Efficiency Upgrade Chip", "Chip de Eficiência"),
    "item.atomcraft.speed_upgrade_schematic": ("Schematic: Speed Chip", "Schematic: Chip de Velocidade"),
    "item.atomcraft.efficiency_upgrade_schematic": ("Schematic: Efficiency Chip", "Schematic: Chip de Eficiência"),
    "upgrade.atomcraft.speed": ("Speed", "Velocidade"),
    "upgrade.atomcraft.efficiency": ("Efficiency", "Eficiência"),
    "upgrade.atomcraft.speed.desc": ("Machines work faster (using more energy); generators burn faster", "Máquinas trabalham mais rápido (gastando mais energia); geradores queimam mais rápido"),
    "upgrade.atomcraft.efficiency.desc": ("Machines use less energy; generators yield more energy per fuel", "Máquinas gastam menos energia; geradores rendem mais energia por combustível"),
    "tooltip.atomcraft.upgrade_max": ("Up to %s of each type per machine", "Até %s de cada tipo por máquina"),
    "tooltip.atomcraft.schematic": ("Lost technology: %s", "Tecnologia perdida: %s"),
    "tooltip.atomcraft.schematic_hint": ("Place it in a Synthesizer's library", "Coloque na biblioteca de um Sintetizador"),
    "message.atomcraft.element_discovered": ("Element discovered: %s (%s)!", "Elemento descoberto: %s (%s)!"),
    "gui.atomcraft.unknown_element": ("Unknown element", "Elemento desconhecido"),
    "gui.atomcraft.unknown_hint": ("Analyze an item that contains it to discover it", "Analise um item que o contenha para descobri-lo"),
    "gui.atomcraft.owner": ("Owner", "Dono"),
    "gui.atomcraft.access_public": ("Access: public", "Acesso: público"),
    "gui.atomcraft.info": ("Information", "Informações"),
    "gui.atomcraft.upgrades": ("Upgrades", "Upgrades"),
    "gui.atomcraft.upgrades_hint": ("Install upgrade chips", "Instale chips de upgrade"),
    "gui.atomcraft.upgrade_speed": ("Speed: %s", "Velocidade: %s"),
    "gui.atomcraft.upgrade_efficiency": ("Efficiency: %s", "Eficiência: %s"),
    "gui.atomcraft.power_on": ("On", "Ligado"),
    "gui.atomcraft.power_off": ("Off", "Desligado"),
    "gui.atomcraft.power_hint": ("Click to turn on/off", "Clique para ligar/desligar"),
    "gui.atomcraft.insert_schematics_1": ("Insert", "Insira"),
    "gui.atomcraft.insert_schematics_2": ("schematics", "schematics"),
    "gui.atomcraft.output": ("Output", "Saída"),
    "gui.atomcraft.click_to_select": ("Click to synthesize", "Clique para sintetizar"),
    "gui.atomcraft.click_to_stop": ("Click to stop", "Clique para parar"),
    "gui.atomcraft.cost_amount": ("%s needed · %s in network", "%s necessários · %s na rede"),
    "gui.atomcraft.fuel": ("Carbon fuel", "Combustível de carbono"),
    "gui.atomcraft.fuel_value": ("Value: %s", "Valor: %s"),
    "gui.atomcraft.generating": ("%s FE/t", "%s FE/t"),
    "gui.atomcraft.burn": ("Burning: %s%%", "Queimando: %s%%"),
    "gui.atomcraft.heat": ("Plasma: %s%%", "Plasma: %s%%"),
    "gui.atomcraft.heat_hint": ("Output grows as the plasma heats up", "A geração cresce conforme o plasma aquece"),
    "gui.atomcraft.helium_produced": ("He produced: %s", "He produzido: %s"),
    "gui.atomcraft.exotic": ("Exotic", "Exóticos"),
    "gui.atomcraft.element_kinds": ("%s elements stored", "%s elementos guardados"),
    "gui.atomcraft.atomic_number": ("Z = %s · %s u", "Z = %s · %s u"),
    "gui.atomcraft.element_amount": ("%s atoms (%s%%)", "%s átomos (%s%%)"),
    "tooltip.atomcraft.fuel_value": ("Fuel: %s FE", "Combustível: %s FE"),
    "atomcraft.configuration.generators": ("Generators", "Geradores"),
    "item.atomcraft.element_disk_16k": ("16K Element Disk", "Disco de Elementos 16K"),
    "item.atomcraft.element_disk_64k": ("64K Element Disk", "Disco de Elementos 64K"),
    "item.atomcraft.element_disk_256k": ("256K Element Disk", "Disco de Elementos 256K"),
    "item.atomcraft.element_disk_1m": ("1M Element Disk", "Disco de Elementos 1M"),
    "itemGroup.atomcraft": ("AtomCraft", "AtomCraft"),
    "rarity.atomcraft.common": ("Common", "Comum"), "rarity.atomcraft.uncommon": ("Uncommon", "Incomum"),
    "rarity.atomcraft.rare": ("Rare", "Raro"), "rarity.atomcraft.epic": ("Epic", "Épico"),
    "rarity.atomcraft.legendary": ("Legendary", "Lendário"),
    "status.atomcraft.idle": ("Idle", "Parado"), "status.atomcraft.working": ("Working", "Trabalhando"),
    "status.atomcraft.no_storage": ("No storage", "Sem armazenamento"),
    "status.atomcraft.storage_full": ("Storage full", "Armazenamento cheio"),
    "status.atomcraft.no_owner": ("No owner", "Sem dono"),
    "status.atomcraft.no_energy": ("Not enough energy", "Energia insuficiente"),
    "status.atomcraft.no_elements": ("Not enough elements", "Elementos insuficientes"),
    "status.atomcraft.output_full": ("Output full", "Saída cheia"),
    "status.atomcraft.not_researched": ("Not researched", "Não pesquisado"),
    "status.atomcraft.nothing_to_research": ("Nothing to research", "Nada para pesquisar"),
    "gui.atomcraft.storage": ("%s / %s atoms", "%s / %s átomos"),
    "gui.atomcraft.no_storage": ("No storage", "Sem armazenamento"),
    "gui.atomcraft.empty": ("Empty", "Vazio"),
    "gui.atomcraft.researched_total": ("%s researched", "%s pesquisados"),
    "gui.atomcraft.search": ("Search...", "Buscar..."),
    "gui.atomcraft.quantity": ("Quantity", "Quantidade"),
    "gui.atomcraft.replicate": ("Replicate", "Replicar"),
    "gui.atomcraft.cancel": ("Cancel", "Cancelar"),
    "gui.atomcraft.energy_cost": ("Energy: %s FE", "Energia: %s FE"),
    "gui.atomcraft.time": ("Time: %s", "Tempo: %s"),
    "gui.atomcraft.available": ("Available: %s", "Disponível: %s"),
    "gui.atomcraft.select_item": ("Pick an item", "Escolha um item"),
    "gui.atomcraft.library_empty": ("Nothing researched", "Nada pesquisado"),
    "gui.atomcraft.storage_label": ("Storage", "Armazenamento"),
    "gui.atomcraft.atoms": ("%s atoms", "%s átomos"),
    "gui.atomcraft.in_network": ("In network: %s", "Na rede: %s"),
    "gui.atomcraft.insert_items": ("Insert items", "Insira itens"),
    "atomcraft.configuration.fePerAtomicMass.tooltip": ("FE per unit of atomic mass", "FE por unidade de massa atômica"),
    "gui.atomcraft.energy": ("%s / %s FE", "%s / %s FE"),
    "tooltip.atomcraft.disk_usage": ("%s / %s atoms", "%s / %s átomos"),
    "tooltip.atomcraft.researched": ("Researched", "Pesquisado"),
    "tooltip.atomcraft.research_progress": ("Research: %s/%s", "Pesquisa: %s/%s"),
    "message.atomcraft.research_complete": ("Research complete: %s", "Pesquisa concluída: %s"),
    "message.atomcraft.not_researched": ("%s has not been researched yet", "%s ainda não foi pesquisado"),
    "message.atomcraft.no_value": ("%s has no atomic value", "%s não tem valor atômico"),
    "message.atomcraft.value": ("%s = %s · %s · %s FE · %ss · research: %s", "%s = %s · %s · %s FE · %ss · pesquisa: %s"),
    "message.atomcraft.research_all": ("%s items researched", "%s itens pesquisados"),
    "atomcraft.configuration.title": ("AtomCraft", "AtomCraft"),
    "atomcraft.configuration.replicator": ("Replicator", "Replicador"),
    "atomcraft.configuration.atomizer": ("Atomizer", "Atomizador"),
    "atomcraft.configuration.analyzer": ("Analyzer", "Analisador"),
    "atomcraft.configuration.fePerAtomicMass": ("FE per atomic mass", "FE por massa atômica"),
    "atomcraft.configuration.timeMultiplier": ("Time multiplier", "Multiplicador de tempo"),
    "atomcraft.configuration.energyCapacity": ("Energy capacity", "Capacidade de energia"),
    "atomcraft.configuration.maxEnergyInput": ("Max energy input", "Entrada máxima de energia"),
    "atomcraft.configuration.ticksPerItem": ("Ticks per item", "Ticks por item"),
}
en = {k: v[0] for k, v in common.items()}
pt = {k: v[1] for k, v in common.items()}
for key, e, p in ELEMENTS:
    en[f"element.atomcraft.{key}"] = e
    pt[f"element.atomcraft.{key}"] = p
w("assets/atomcraft/lang/en_us.json", en)
w("assets/atomcraft/lang/pt_br.json", pt)

# ---------- Texturas 16x16 ----------
def save(img, path):
    if preserved("assets/atomcraft/textures/" + path):
        return
    p = os.path.join(R, "assets/atomcraft/textures", path)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)

def frame():
    img = Image.new("RGBA", (16, 16), (78, 82, 92, 255)); d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 15], outline=(42, 44, 50, 255)); d.rectangle([1, 1, 14, 14], outline=(118, 124, 136, 255))
    return img, d

def atom(d, c, cx=7.5, cy=7.5):
    d.ellipse([cx - 4.5, cy - 1.5, cx + 4.5, cy + 1.5], outline=c); d.ellipse([cx - 1.5, cy - 4.5, cx + 1.5, cy + 4.5], outline=c)
    d.point((int(cx), int(cy)), fill=(255, 255, 255, 255)); d.point((int(cx) + 1, int(cy) + 1), fill=(255, 255, 255, 255))

COLORS = {"atomizer": (80, 220, 255, 255), "analyzer": (190, 110, 240, 255), "replicator": (255, 170, 60, 255), "drive": (90, 220, 120, 255),
    "terminal": (39, 201, 181, 255), "synthesizer": (120, 230, 140, 255), "combustion_generator": (255, 120, 30, 255), "carbon_generator": (226, 50, 50, 255), "fusion_reactor": (255, 90, 190, 255)}
img, d = frame(); d.line([4, 5, 11, 5], fill=(60, 63, 70, 255)); d.line([4, 10, 11, 10], fill=(60, 63, 70, 255)); save(img, "block/machine_side.png")
for name, c in COLORS.items():
    img, d = frame(); d.rectangle([4, 4, 11, 11], fill=c); d.rectangle([6, 6, 9, 9], fill=(26, 28, 34, 255)); save(img, f"block/{name}_top.png")
    img, d = frame(); d.rectangle([2, 2, 13, 13], fill=(24, 26, 32, 255))
    if name == "drive":
        for row in range(4):
            d.rectangle([3, 3 + row * 3, 12, 4 + row * 3], fill=(60, 64, 72, 255)); d.point((11, 3 + row * 3), fill=c)
    elif name == "terminal":
        d.rectangle([3, 4, 12, 11], fill=(11, 34, 36, 255), outline=c)
        for (px, py) in ((4, 5), (11, 5), (4, 7), (6, 7), (9, 7), (11, 7), (4, 9), (6, 9), (8, 9), (10, 9)):
            d.point((px, py), fill=c)
    elif name == "combustion_generator":
        d.rectangle([4, 9, 11, 12], fill=(40, 30, 26, 255))
        d.polygon([(5, 11), (7, 5), (8, 8), (9, 4), (10, 11)], fill=c); d.polygon([(6, 11), (8, 8), (9, 11)], fill=(255, 220, 90, 255))
    elif name == "carbon_generator":
        d.ellipse([3, 3, 12, 12], fill=(30, 30, 34, 255), outline=c)
        d.text((5, 3), "C", fill=c)
    elif name == "fusion_reactor":
        d.ellipse([2, 2, 13, 13], outline=c); d.ellipse([4, 4, 11, 11], outline=(255, 170, 230, 255))
        d.rectangle([6, 6, 9, 9], fill=(255, 240, 250, 255)); d.point((7, 7), fill=(255, 255, 255, 255))
    elif name == "synthesizer":
        d.rectangle([4, 4, 11, 11], fill=(20, 40, 28, 255), outline=c)
        d.line([4, 7, 2, 7], fill=(201, 166, 70, 255)); d.line([11, 7, 13, 7], fill=(201, 166, 70, 255))
        d.line([7, 4, 7, 2], fill=(201, 166, 70, 255)); d.line([7, 11, 7, 13], fill=(201, 166, 70, 255))
        d.rectangle([6, 6, 9, 9], fill=c)
    elif name == "analyzer":
        d.ellipse([4, 4, 11, 11], outline=c); d.line([6, 7, 9, 7], fill=c); d.line([7, 6, 7, 9], fill=c)
    else:
        atom(d, c)
    save(img, f"block/{name}_front.png")
img = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(img)
d.rectangle([0, 0, 15, 15], fill=(70, 74, 84, 255)); d.rectangle([5, 5, 10, 10], fill=(80, 220, 255, 255))
d.rectangle([6, 6, 9, 9], fill=(40, 140, 180, 255)); save(img, "block/atomic_cable.png")
for name, c in DISKS.items():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(img)
    d.rectangle([3, 2, 12, 13], fill=(60, 64, 72, 255), outline=(30, 32, 38, 255))
    d.rectangle([5, 4, 10, 8], fill=c + (255,)); d.rectangle([5, 10, 10, 11], fill=(150, 156, 168, 255))
    save(img, f"item/{name}.png")
# Chips: placa verde com pinos dourados e um símbolo
def chip(symbol_color, draw_symbol, path):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(img)
    for x in range(4, 12, 2):
        d.line([x, 1, x, 2], fill=(220, 180, 70, 255)); d.line([x, 13, x, 14], fill=(220, 180, 70, 255))
    d.rectangle([2, 3, 13, 12], fill=(32, 96, 58, 255), outline=(14, 46, 28, 255))
    d.line([3, 4, 12, 4], fill=(60, 140, 90, 255))
    draw_symbol(d, symbol_color)
    save(img, path)
chip((255, 200, 60, 255), lambda d, c: (d.polygon([(5, 6), (8, 6), (8, 5), (11, 8), (8, 11), (8, 10), (5, 10)], fill=c)), "item/speed_upgrade.png")
chip((120, 230, 255, 255), lambda d, c: (d.polygon([(9, 5), (5, 9), (8, 9), (6, 12), (11, 7), (8, 7)], fill=c)), "item/efficiency_upgrade.png")
def schematic(c, path):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(img)
    d.rectangle([2, 2, 13, 13], fill=(34, 78, 150, 255), outline=(18, 40, 86, 255))
    for y in (5, 8, 11):
        d.line([4, y, 11, y], fill=(150, 190, 240, 255))
    d.rectangle([9, 4, 12, 7], fill=c); d.point((3, 3), fill=(220, 236, 255, 255))
    save(img, path)
schematic((255, 200, 60, 255), "item/speed_upgrade_schematic.png")
schematic((120, 230, 255, 255), "item/efficiency_upgrade_schematic.png")
print("ok", len(BASE), "itens base,", len(TAGS), "tags vanilla,", len(COMPAT), "tags de compatibilidade,",
      len(kept), "arquivos de aparência preservados")
