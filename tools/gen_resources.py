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
ART = ("assets/atomcraft/models/", "assets/atomcraft/blockstates/", "assets/atomcraft/items/", "assets/atomcraft/textures/",
       "assets/atomcraft/geckolib/")
kept = []

def preserved(path):
    if path.startswith(ART) and os.path.exists(os.path.join(R, path)):
        kept.append(path)
        return True
    return False

def wf(path, data):
    """Escreve sempre (arquivos derivados da arte do usuário, ex.: drive_frame.json)."""
    p = os.path.join(R, path)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")

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
    ("titanium", "Ti9", "uncommon", True), ("tungsten", "W9", "rare", True), ("cobalt", "Co9", "uncommon", True),
    ("chromium", "Cr9", None, True), ("lithium", "Li9", None, True),
    # Ligas
    ("bronze", "Cu7 Sn2", None, False), ("brass", "Cu6 Zn3", None, False), ("steel", "Fe8 C1", None, False),
    ("invar", "Fe6 Ni3", None, False), ("constantan", "Cu5 Ni4", None, False),
    ("electrum", "Au5 Ag4", "uncommon", False), ("stainless_steel", "Fe6 Cr2 Ni1", "uncommon", False),
    ("duralumin", "Al7 Cu1 Mg1", "uncommon", False), ("titanium_alloy", "Ti7 Al1 V1", "rare", False),
    ("tungsten_carbide", "W5 C4", "rare", False), ("superalloy", "Ni5 Cr2 Co1 Ti1", "epic", False),
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
MACHINES = ["atomizer", "analyzer", "replicator", "advanced_replicator", "drive", "terminal", "combustion_generator", "carbon_generator", "fusion_reactor", "synthesizer", "alloy_forge", "quantum_replicator"]
GENERATORS = ["combustion_generator", "carbon_generator"]
for b in MACHINES:
    if b == "quantum_replicator":
        # Controlador do multibloco: a frente acende quando a estrutura forma
        w(f"assets/atomcraft/blockstates/{b}.json", {"variants": {
            f"facing={d},formed={str(f).lower()}": ({"model": f"atomcraft:block/{b}" + ("_on" if f else "")} | ({"y": y} if y else {}))
            for d, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)) for f in (False, True)}})
        for suffix in ("", "_on"):
            w(f"assets/atomcraft/models/block/{b}{suffix}.json", {"parent": "minecraft:block/orientable", "textures": {
                "top": "atomcraft:block/quantum_casing", "front": f"atomcraft:block/{b}_front{suffix}",
                "side": "atomcraft:block/quantum_casing", "particle": "atomcraft:block/quantum_casing"}})
        w(f"assets/atomcraft/items/{b}.json", {"model": {"type": "minecraft:model", "model": f"atomcraft:block/{b}"}})
        continue
    if b in GENERATORS:
        # Como a fornalha: "lit" troca a frente para <gerador>_front_on. Lados e topo comuns aos dois.
        w(f"assets/atomcraft/blockstates/{b}.json", {"variants": {
            f"facing={d},lit={str(lit).lower()}": ({"model": f"atomcraft:block/{b}" + ("_on" if lit else "")} | ({"y": y} if y else {}))
            for d, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)) for lit in (False, True)}})
        for suffix in ("", "_on"):
            w(f"assets/atomcraft/models/block/{b}{suffix}.json", {"parent": "minecraft:block/orientable", "textures": {
                "top": "atomcraft:block/generator_top", "front": f"atomcraft:block/{b}_front{suffix}",
                "side": "atomcraft:block/generator_side", "particle": "atomcraft:block/generator_side"}})
        w(f"assets/atomcraft/items/{b}.json", {"model": {"type": "minecraft:model", "model": f"atomcraft:block/{b}"}})
        continue
    w(f"assets/atomcraft/blockstates/{b}.json", {"variants": {
        f"facing={d}": ({"model": f"atomcraft:block/{b}"} | ({"y": y} if y else {}))
        for d, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
    w(f"assets/atomcraft/models/block/{b}.json", {"parent": "minecraft:block/orientable", "textures": {
        "top": f"atomcraft:block/{b}_top", "front": f"atomcraft:block/{b}_front",
        "side": f"atomcraft:block/machine_side", "particle": "atomcraft:block/machine_side"}})
    w(f"assets/atomcraft/items/{b}.json", {"model": {"type": "minecraft:model", "model": f"atomcraft:block/{b}"}})

# Peças do Reator de Fusão (cubos simples; somem quando o reator forma)
REACTOR_PARTS = ["reactor_casing", "reactor_glass", "magnetic_coil", "central_solenoid", "reactor_port",
                 "quantum_casing", "quantum_glass", "quantum_core"]
for b in REACTOR_PARTS:
    w(f"assets/atomcraft/blockstates/{b}.json", {"variants": {"": {"model": f"atomcraft:block/{b}"}}})
    model = {"parent": "minecraft:block/cube_all", "textures": {"all": f"atomcraft:block/{b}"}}
    if b == "reactor_port":
        # Um pouco maior que um bloco: fica por cima do casco do modelo grande (sem z-fighting).
        model = {"parent": "minecraft:block/block", "textures": {"all": f"atomcraft:block/{b}", "particle": f"atomcraft:block/{b}"},
                 "elements": [{"from": [-0.1, -0.1, -0.1], "to": [16.1, 16.1, 16.1],
                               "faces": {f: {"uv": [0, 0, 16, 16], "texture": "#all"} for f in ("north", "east", "south", "west", "up", "down")}}]}
    if b == "reactor_glass":
        model["render_type"] = "minecraft:cutout"
    if b == "quantum_glass":
        model["render_type"] = "minecraft:translucent"
    w(f"assets/atomcraft/models/block/{b}.json", model)
    w(f"assets/atomcraft/items/{b}.json", {"model": {"type": "minecraft:model", "model": f"atomcraft:block/{b}"}})

# Módulos do reator e suas schematics
MODULE_TYPES = {"containment": (255, 90, 190), "injector": (255, 160, 60), "recycler": (90, 220, 120), "ignition": (255, 80, 60)}
MODULE_ITEMS = []
for mtype in MODULE_TYPES:
    for tier in (1, 2, 3):
        MODULE_ITEMS += [f"{mtype}_module_{tier}", f"{mtype}_module_{tier}_schematic"]
for d in MODULE_ITEMS:
    w(f"assets/atomcraft/models/item/{d}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"atomcraft:item/{d}"}})
    w(f"assets/atomcraft/items/{d}.json", {"model": {"type": "minecraft:model", "model": f"atomcraft:item/{d}"}})

# Modelo grande do reator formado (GeckoLib). Origem = centro da base; frente (controlador) em -Z.
CAP = ["-CCCCC-", "CCCCCCC", "CCCCCCC", "CCCSCCC", "CCCCCCC", "CCCCCCC", "-CCCCC-"]
RING = ["-CCCCC-", "C.....C", "C.BBB.C", "C.BSB.C", "C.BBB.C", "C.....C", "-CCCCC-"]
MID = ["-CVKVC-", "C.....C", "V.BBB.V", "C.BSB.C", "V.BBB.V", "C.....C", "-CVCVC-"]
TILES = {"C": (0, 0), "B": (16, 0), "S": (32, 0), "K": (48, 0), "V": (16, 16)}
def tile_uv(t, faces=("north", "east", "south", "west", "up", "down"), front=None):
    uv = {}
    for f in faces:
        u, v = TILES[front if (front and f == "north") else t]
        uv[f] = {"uv": [u, v], "uv_size": [16, 16]}
    return uv
shell = []
for y, layer in enumerate([CAP, RING, MID, RING, CAP]):
    for z, row in enumerate(layer):
        for x, c in enumerate(row):
            if c not in TILES:
                continue
            origin = [(x - 3) * 16 - 8, y * 16, (z - 3) * 16 - 8]
            if c == "K":
                cube = {"origin": origin, "size": [16, 16, 16], "inflate": -0.05, "uv": tile_uv("C", front="K")}
            else:
                cube = {"origin": origin, "size": [16, 16, 16], "inflate": -0.05, "uv": tile_uv(c)}
            shell.append(cube)
def plasma_cube(origin, size):
    return {"origin": origin, "size": size, "uv": {f: {"uv": [32, 16], "uv_size": [16, 16]} for f in ("north", "east", "south", "west", "up", "down")}}
plasma = [plasma_cube([-38, 35, -38], [76, 10, 10]), plasma_cube([-38, 35, 28], [76, 10, 10]),
          plasma_cube([-38, 35, -28], [10, 10, 56]), plasma_cube([28, 35, -28], [10, 10, 56])]
w("assets/atomcraft/geckolib/models/block/fusion_reactor_formed.geo.json", {"format_version": "1.12.0", "minecraft:geometry": [{
    "description": {"identifier": "geometry.fusion_reactor_formed", "texture_width": 64, "texture_height": 64,
                    "visible_bounds_width": 8, "visible_bounds_height": 6, "visible_bounds_offset": [0, 2.5, 0]},
    "bones": [{"name": "reactor", "pivot": [0, 0, 0]},
              {"name": "shell", "parent": "reactor", "pivot": [0, 0, 0], "cubes": shell},
              {"name": "plasma", "parent": "reactor", "pivot": [0, 40, 0], "cubes": plasma}]}]})
w("assets/atomcraft/geckolib/animations/block/fusion_reactor_formed.animation.json", {"format_version": "1.8.0", "animations": {
    "plasma": {"loop": True, "animation_length": 2.0, "bones": {"plasma": {"scale": {
        "0.0": [1, 1, 1], "1.0": [1.03, 1.3, 1.03], "2.0": [1, 1, 1]}}}}}})

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
# Com um modelo 3D próprio em models/item/disk.json, todos os discos usam esse modelo e só trocam a
# textura (disk_16k.png, disk_64k.png...). Sem ele, cada disco é um ícone plano.
DISK_MODEL = os.path.exists(os.path.join(R, "assets/atomcraft/models/item/disk.json"))
for d in DISKS:
    tier = d.replace("element_", "")
    if DISK_MODEL:
        wf(f"assets/atomcraft/models/item/{d}.json", {"parent": "atomcraft:item/disk", "textures": {
            "0": f"atomcraft:item/{tier}", "particle": f"atomcraft:item/{tier}"}})
    else:
        w(f"assets/atomcraft/models/item/{d}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"atomcraft:item/{d}"}})
    w(f"assets/atomcraft/items/{d}.json", {"model": {"type": "minecraft:model", "model": f"atomcraft:item/{d}"}})

# Drive com modelo próprio: os elementos chamados "drive" marcam os slots dos discos. Eles saem do
# modelo do bloco (drive_frame.json, gerado) porque os discos de verdade são desenhados por código,
# cada um com a textura do seu nível.
DRIVE_MODEL = os.path.join(R, "assets/atomcraft/models/block/drive.json")
if os.path.exists(DRIVE_MODEL):
    with open(DRIVE_MODEL, encoding="utf-8") as f:
        drive = json.load(f)
    if any(e.get("name") == "drive" for e in drive.get("elements", [])):
        frame = {k: v for k, v in drive.items() if k not in ("groups", "elements")}
        frame["elements"] = [e for e in drive["elements"] if e.get("name") != "drive"]
        wf("assets/atomcraft/models/block/drive_frame.json", frame)
        wf("assets/atomcraft/blockstates/drive.json", {"variants": {
            f"facing={d}": ({"model": "atomcraft:block/drive_frame"} | ({"y": y} if y else {}))
            for d, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})

# Máquinas de 2 blocos animadas (GeckoLib): o item também usa o modelo animado.
# models/item/<máquina>.json só define os tamanhos ("display") em cada lugar (inventário,
# mão, chão, moldura), calculados pela altura do modelo. Ajuste à vontade no Blockbench (aba Display).
TALL_MACHINES = ["analyzer", "atomizer", "replicator", "advanced_replicator"]
# Replicador Avançado sem modelo próprio: usa uma cópia do modelo/animações do Replicador com a textura
# em outro tom. Substitua os arquivos advanced_replicator.* pelos seus quando quiser.
GECKO = os.path.join(R, "assets/atomcraft/geckolib")
for kind, ext in (("models", "geo.json"), ("animations", "animation.json")):
    src, dst = os.path.join(GECKO, kind, "block", f"replicator.{ext}"), os.path.join(GECKO, kind, "block", f"advanced_replicator.{ext}")
    if os.path.exists(src) and (not os.path.exists(dst) or FORCE):
        open(dst, "w").write(open(src).read())
ADV_TEX = os.path.join(R, "assets/atomcraft/textures/block/advanced_replicator.png")
REP_TEX = os.path.join(R, "assets/atomcraft/textures/block/replicator.png")
if os.path.exists(REP_TEX) and (not os.path.exists(ADV_TEX) or FORCE):
    import colorsys
    img = Image.open(REP_TEX).convert("RGBA"); px = img.load()
    for yy in range(img.height):
        for xx in range(img.width):
            r, g, b2, a = px[xx, yy]
            h, l, s2 = colorsys.rgb_to_hls(r / 255, g / 255, b2 / 255)
            if s2 > 0.25:  # só as partes coloridas mudam de tom (laranja → violeta)
                r, g, b2 = (int(v * 255) for v in colorsys.hls_to_rgb((h + 0.72) % 1.0, l, s2))
            px[xx, yy] = (r, g, b2, a)
    img.save(ADV_TEX)
for b in TALL_MACHINES:
    geo = os.path.join(R, f"assets/atomcraft/geckolib/models/block/{b}.geo.json")
    if not os.path.exists(geo):
        continue
    bones = json.load(open(geo))["minecraft:geometry"][0]["bones"]
    half = max(c["origin"][1] + c["size"][1] for bone in bones for c in bone.get("cubes", [])) / 16 / 2  # meia altura, em blocos
    t = lambda scale, cos=1.0: round(-half * scale * 16 * cos, 2)
    w(f"assets/atomcraft/models/item/{b}.json", {"parent": f"atomcraft:block/{b}", "display": {
        "gui": {"rotation": [30, 225, 0], "translation": [0, t(0.36, 0.87), 0], "scale": [0.36, 0.36, 0.36]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 1, 0], "scale": [0.16, 0.16, 0.16]},
        "fixed": {"rotation": [0, 0, 0], "translation": [0, t(0.3), 0], "scale": [0.3, 0.3, 0.3]},
        "head": {"rotation": [0, 0, 0], "translation": [0, 4, 0], "scale": [0.4, 0.4, 0.4]},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 1.5, 0], "scale": [0.22, 0.22, 0.22]},
        "thirdperson_lefthand": {"rotation": [75, 45, 0], "translation": [0, 1.5, 0], "scale": [0.22, 0.22, 0.22]},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, -2, 0], "scale": [0.24, 0.24, 0.24]},
        "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, -2, 0], "scale": [0.24, 0.24, 0.24]}}})
    wf(f"assets/atomcraft/items/{b}.json", {"model": {"type": "minecraft:special", "base": f"atomcraft:item/{b}",
                                                     "model": {"type": "geckolib:geckolib"}}})

# Chips e schematics
CHIPS = ["speed_upgrade", "efficiency_upgrade", "speed_upgrade_schematic", "efficiency_upgrade_schematic"]
for d in CHIPS:
    w(f"assets/atomcraft/models/item/{d}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"atomcraft:item/{d}"}})
    w(f"assets/atomcraft/items/{d}.json", {"model": {"type": "minecraft:model", "model": f"atomcraft:item/{d}"}})

# Schematics nos baús do mundo (NeoForge global loot modifiers)
def modules(tier, weight):
    return [(f"{t}_module_{tier}_schematic", weight) for t in MODULE_TYPES]
SPEED, EFF = "speed_upgrade_schematic", "efficiency_upgrade_schematic"
# (tabelas do vanilla, [(schematic, peso)], peso de "nada")
LOOT = {
    "village": (["village/village_toolsmith", "village/village_weaponsmith", "village/village_armorer",
                 "village/village_cartographer", "village/village_temple", "village/village_mason"], [(SPEED, 3), (EFF, 1)], 16),
    "fortress": (["nether_bridge"], [(SPEED, 2), (EFF, 2)] + modules(1, 1), 8),
    "bastion": (["bastion_treasure"], [(SPEED, 1), (EFF, 1)] + modules(1, 1) + modules(2, 1), 8),
    "end": (["end_city_treasure"], [(SPEED, 1), (EFF, 3)] + modules(2, 2) + modules(3, 1), 8),
    "ancient_city": (["ancient_city"], modules(2, 1) + modules(3, 1), 12),
}
for name, (tables, entries, empty) in LOOT.items():
    w(f"data/atomcraft/loot_table/chests/schematics_{name}.json", {"type": "minecraft:chest", "pools": [{"rolls": 1, "entries":
        [{"type": "minecraft:item", "name": f"atomcraft:{item}", "weight": weight} for item, weight in entries]
        + [{"type": "minecraft:empty", "weight": empty}]}]})
    w(f"data/atomcraft/loot_modifiers/schematics_{name}.json", {"type": "neoforge:add_table",
        "conditions": [{"condition": "minecraft:any_of", "terms": [
            {"condition": "neoforge:loot_table_id", "loot_table_id": f"minecraft:chests/{t}"} for t in tables]}],
        "table": f"atomcraft:chests/schematics_{name}"})
# Nesta versão do NeoForge cada arquivo em loot_modifiers/ é carregado sozinho (sem lista global).

# Loot tables e tags
for b in MACHINES + ["atomic_cable"] + REACTOR_PARTS:
    conditions = [{"condition": "minecraft:survives_explosion"}]
    if b in TALL_MACHINES:
        # 2 blocos de altura: só a metade de baixo solta o item.
        conditions.append({"condition": "minecraft:block_state_property", "block": f"atomcraft:{b}", "properties": {"half": "lower"}})
    w(f"data/atomcraft/loot_table/blocks/{b}.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
        "entries": [{"type": "minecraft:item", "name": f"atomcraft:{b}"}], "conditions": conditions}]})

# Receitas
def shaped(name, pattern, key, result, count=1):
    w(f"data/atomcraft/recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": "misc",
        "pattern": pattern, "key": key, "result": {"id": result, "count": count}})
IRON, RED, AME = "minecraft:iron_ingot", "minecraft:redstone", "minecraft:amethyst_shard"
shaped("atomizer", ["IAI", "ARA", "IRI"], {"I": IRON, "A": AME, "R": RED}, "atomcraft:atomizer")
# Progressão: atomizador, drive, terminal, gerador a combustão, disco 16k, cabo e Forja de Ligas usam só
# itens do vanilla; o resto pede ligas (aço → inox/invar → titânio/carbeto → superliga) e circuitos.
def plate(m): return f"atomcraft:{m}_plate"
CIRCUIT, WIRE = "atomcraft:circuit_board", "atomcraft:copper_wire"
shaped("alloy_forge", ["IAI", "CFC", "IRI"], {"I": IRON, "A": AME, "C": "minecraft:copper_ingot", "F": "minecraft:blast_furnace", "R": RED}, "atomcraft:alloy_forge")
shaped("analyzer", ["PCP", "AAA", "PRP"], {"P": plate("steel"), "C": CIRCUIT, "A": AME, "R": RED}, "atomcraft:analyzer")
shaped("advanced_replicator", ["PCP", "DRD", "PCP"], {"P": plate("stainless_steel"), "C": CIRCUIT, "D": "minecraft:diamond", "R": "atomcraft:replicator"}, "atomcraft:advanced_replicator")
shaped("quantum_replicator", ["SCS", "EAE", "SCS"], {"S": plate("superalloy"), "C": CIRCUIT, "E": "minecraft:echo_shard", "A": "atomcraft:advanced_replicator"}, "atomcraft:quantum_replicator")
shaped("quantum_casing", ["TDT", "DRD", "TDT"], {"T": plate("titanium_alloy"), "D": plate("duralumin"), "R": RED}, "atomcraft:quantum_casing", 8)
shaped("quantum_glass", ["CGC", "GAG", "CGC"], {"C": "atomcraft:quantum_casing", "G": "minecraft:glass", "A": AME}, "atomcraft:quantum_glass", 4)
shaped("quantum_core", ["TCT", "ENE", "TCT"], {"T": plate("tungsten_carbide"), "C": CIRCUIT, "E": "minecraft:echo_shard", "N": "minecraft:nether_star"}, "atomcraft:quantum_core")
shaped("replicator", ["PCP", "ERE", "PDP"], {"P": plate("steel"), "C": CIRCUIT, "E": "minecraft:ender_pearl", "R": "minecraft:redstone_block", "D": "minecraft:diamond"}, "atomcraft:replicator")
shaped("drive", ["IRI", "ICI", "IAI"], {"I": IRON, "R": RED, "C": "minecraft:chest", "A": AME}, "atomcraft:drive")
shaped("terminal", ["IGI", "ARA", "III"], {"I": IRON, "G": "minecraft:glass", "A": AME, "R": RED}, "atomcraft:terminal")
shaped("combustion_generator", ["III", "IFI", "CRC"], {"I": IRON, "F": "minecraft:furnace", "C": "minecraft:copper_ingot", "R": RED}, "atomcraft:combustion_generator")
shaped("carbon_generator", ["PAP", "AGA", "PBP"], {"P": plate("steel"), "A": AME, "G": "atomcraft:combustion_generator", "B": "minecraft:blast_furnace"}, "atomcraft:carbon_generator")
shaped("fusion_reactor", ["SCS", "ENE", "SCS"], {"S": plate("superalloy"), "C": CIRCUIT, "E": "minecraft:end_crystal", "N": "minecraft:nether_star"}, "atomcraft:fusion_reactor")
shaped("synthesizer", ["GCG", "RTR", "PPP"], {"G": "minecraft:gold_ingot", "C": CIRCUIT, "R": RED, "T": "minecraft:crafting_table", "P": plate("brass")}, "atomcraft:synthesizer")
shaped("reactor_casing", ["SLS", "LRL", "SLS"], {"S": plate("stainless_steel"), "L": plate("lead"), "R": RED}, "atomcraft:reactor_casing", 8)
shaped("reactor_glass", ["XGX", "G G", "XGX"], {"X": "atomcraft:reactor_casing", "G": "minecraft:glass"}, "atomcraft:reactor_glass", 4)
shaped("magnetic_coil", ["WSW", "SIS", "WSW"], {"W": WIRE, "S": plate("invar"), "I": "minecraft:iron_block"}, "atomcraft:magnetic_coil", 2)
shaped("central_solenoid", ["TMT", "MEM", "TMT"], {"T": plate("tungsten_carbide"), "M": "atomcraft:magnetic_coil", "E": "minecraft:ender_eye"}, "atomcraft:central_solenoid")
shaped("reactor_port", ["XAX", "AHA", "XAX"], {"X": "atomcraft:reactor_casing", "A": "atomcraft:atomic_cable", "H": "minecraft:hopper"}, "atomcraft:reactor_port", 2)
shaped("atomic_cable", ["NNN", "RAR", "NNN"], {"N": "minecraft:iron_nugget", "R": RED, "A": AME}, "atomcraft:atomic_cable", 8)
shaped("element_disk_16k", ["ARA", "RIR", "III"], {"A": AME, "R": RED, "I": IRON}, "atomcraft:element_disk_16k")
shaped("element_disk_64k", ["PRP", "RCR", "PPP"], {"P": plate("steel"), "R": RED, "C": CIRCUIT}, "atomcraft:element_disk_64k")
shaped("element_disk_256k", ["PRP", "DCD", "PPP"], {"P": plate("stainless_steel"), "R": RED, "D": "minecraft:diamond", "C": CIRCUIT}, "atomcraft:element_disk_256k")
shaped("element_disk_1m", ["PRP", "ECE", "PPP"], {"P": plate("titanium_alloy"), "R": RED, "E": "minecraft:ender_eye", "C": CIRCUIT}, "atomcraft:element_disk_1m")

# ---------- Materiais: metais, ligas, minérios e componentes ----------
# (id, nome en, nome pt, cor RGB, fórmula do lingote (9 átomos), raridade, tier)
MATERIALS = [
    ("tin", "Tin", "Estanho", (206, 210, 216), "Sn9", None, 1),
    ("zinc", "Zinc", "Zinco", (168, 178, 204), "Zn9", None, 1),
    ("lead", "Lead", "Chumbo", (98, 104, 130), "Pb9", None, 1),
    ("nickel", "Nickel", "Níquel", (204, 204, 162), "Ni9", None, 2),
    ("cobalt", "Cobalt", "Cobalto", (74, 112, 222), "Co9", "uncommon", 2),
    ("titanium", "Titanium", "Titânio", (192, 198, 210), "Ti9", "uncommon", 3),
    ("tungsten", "Tungsten", "Tungstênio", (86, 92, 104), "W9", "rare", 3),
    ("steel", "Steel", "Aço", (132, 140, 152), "Fe8 C1", None, 1),
    ("bronze", "Bronze", "Bronze", (205, 128, 52), "Cu7 Sn2", None, 1),
    ("brass", "Brass", "Latão", (226, 190, 92), "Cu6 Zn3", None, 1),
    ("stainless_steel", "Stainless Steel", "Aço Inoxidável", (202, 212, 222), "Fe6 Cr2 Ni1", "uncommon", 2),
    ("duralumin", "Duralumin", "Duralumínio", (218, 222, 230), "Al7 Cu1 Mg1", "uncommon", 2),
    ("invar", "Invar", "Invar", (166, 172, 150), "Fe6 Ni3", "uncommon", 2),
    ("titanium_alloy", "Titanium Alloy", "Liga de Titânio", (150, 166, 194), "Ti7 Al1 V1", "rare", 3),
    ("tungsten_carbide", "Tungsten Carbide", "Carbeto de Tungstênio", (62, 64, 76), "W5 C4", "rare", 3),
    ("superalloy", "Superalloy", "Superliga", (118, 152, 140), "Ni5 Cr2 Co1 Ti1", "epic", 4),
]
ALLOY_IDS = ["steel", "bronze", "brass", "stainless_steel", "duralumin", "invar", "titanium_alloy", "tungsten_carbide", "superalloy"]
# (id, en, pt, cor das pintas, fórmula do minério bruto, raridade, ferramenta, lingote ao fundir, (tamanho, veios/chunk, y mín, y máx))
ORE_TYPES = [
    ("tin", "Tin", "Estanho", (228, 228, 232), "Sn9", None, "stone", "tin", (9, 10, -16, 112)),
    ("zinc", "Zinc", "Zinco", (176, 188, 222), "Zn9", None, "stone", "zinc", (8, 8, -16, 80)),
    ("lead", "Lead", "Chumbo", (110, 116, 150), "Pb9", None, "stone", "lead", (8, 6, -32, 64)),
    ("lithium", "Lithium", "Lítio", (232, 168, 232), "Li6 Al1 Si2", None, "stone", None, (6, 4, -16, 64)),
    ("nickel", "Nickel", "Níquel", (220, 220, 172), "Ni9", None, "iron", "nickel", (7, 6, -48, 48)),
    ("chromium", "Chromium", "Cromo", (140, 162, 222), "Cr6 Fe3", None, "iron", None, (6, 5, -48, 32)),
    ("cobalt", "Cobalt", "Cobalto", (80, 124, 236), "Co9", "uncommon", "iron", "cobalt", (5, 4, -64, 24)),
    ("titanium", "Titanium", "Titânio", (214, 218, 228), "Ti8 V1", "uncommon", "iron", "titanium", (5, 4, -64, 16)),
    ("tungsten", "Tungsten", "Tungstênio", (250, 214, 120), "W9", "rare", "diamond", "tungsten", (4, 3, -64, -8)),
]
COMPONENTS = [("copper_wire", "Copper Wire", "Fio de Cobre"), ("silicon_wafer", "Silicon Wafer", "Wafer de Silício"),
              ("circuit_board", "Circuit Board", "Placa de Circuito")]
MAT_LANG = {}

def item_model(name, texture=None):
    w(f"assets/atomcraft/models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"atomcraft:item/{texture or name}"}})
    w(f"assets/atomcraft/items/{name}.json", {"model": {"type": "minecraft:model", "model": f"atomcraft:item/{name}"}})

def block_item(name, model=None):
    w(f"assets/atomcraft/items/{name}.json", {"model": {"type": "minecraft:model", "model": f"atomcraft:block/{model or name}"}})

def simple_block(name, texture):
    w(f"assets/atomcraft/blockstates/{name}.json", {"variants": {"": {"model": f"atomcraft:block/{name}"}}})
    w(f"assets/atomcraft/models/block/{name}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"atomcraft:block/{texture}"}})
    block_item(name)

def self_drop(name):
    w(f"data/atomcraft/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0,
        "entries": [{"type": "minecraft:item", "name": f"atomcraft:{name}"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})

def recipe(name, data):
    w(f"data/atomcraft/recipe/{name}.json", data)

def shapeless(name, ingredients, result, count=1):
    recipe(name, {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": ingredients, "result": {"id": result, "count": count}})

def stonecut(name, ingredient, result, count=1):
    recipe(name, {"type": "minecraft:stonecutting", "ingredient": ingredient, "result": {"id": result, "count": count}})

def cook(name, kind, ingredient, result, xp, time):
    recipe(name, {"type": f"minecraft:{kind}", "category": "misc", "ingredient": ingredient, "result": {"id": result}, "experience": xp, "cookingtime": time})

def stairs_states(name, tex):
    base = f"atomcraft:block/{name}"
    for suffix, parent in (("", "stairs"), ("_inner", "inner_stairs"), ("_outer", "outer_stairs")):
        w(f"assets/atomcraft/models/block/{name}{suffix}.json", {"parent": f"minecraft:block/{parent}",
            "textures": {"bottom": tex, "top": tex, "side": tex}})
    variants = {}
    facing_y = {"east": 0, "south": 90, "west": 180, "north": 270}
    for facing, fy in facing_y.items():
        for half in ("bottom", "top"):
            for shape in ("straight", "inner_left", "inner_right", "outer_left", "outer_right"):
                model = base + ("_inner" if shape.startswith("inner") else "_outer" if shape.startswith("outer") else "")
                y = fy
                if half == "bottom" and shape in ("inner_left", "outer_left"): y = fy - 90
                if half == "top" and shape in ("inner_right", "outer_right"): y = fy + 90
                y %= 360
                v = {"model": model}
                if half == "top": v["x"] = 180
                if y: v["y"] = y
                if half == "top" or y: v["uvlock"] = True
                variants[f"facing={facing},half={half},shape={shape}"] = v
    w(f"assets/atomcraft/blockstates/{name}.json", {"variants": variants})

def slab_states(name, tex, full):
    w(f"assets/atomcraft/models/block/{name}.json", {"parent": "minecraft:block/slab", "textures": {"bottom": tex, "top": tex, "side": tex}})
    w(f"assets/atomcraft/models/block/{name}_top.json", {"parent": "minecraft:block/slab_top", "textures": {"bottom": tex, "top": tex, "side": tex}})
    w(f"assets/atomcraft/blockstates/{name}.json", {"variants": {
        "type=bottom": {"model": f"atomcraft:block/{name}"}, "type=top": {"model": f"atomcraft:block/{name}_top"},
        "type=double": {"model": full}}})

def wall_states(name, tex):
    for suffix, parent in (("_post", "template_wall_post"), ("_side", "template_wall_side"), ("_side_tall", "template_wall_side_tall"),
                           ("_inventory", "wall_inventory")):
        w(f"assets/atomcraft/models/block/{name}{suffix}.json", {"parent": f"minecraft:block/{parent}", "textures": {"wall": tex}})
    base = f"atomcraft:block/{name}"
    parts = [{"when": {"up": "true"}, "apply": {"model": base + "_post"}}]
    for d, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for height, suffix in (("low", "_side"), ("tall", "_side_tall")):
            apply = {"model": base + suffix, "uvlock": True}
            if y: apply["y"] = y
            parts.append({"when": {d: height}, "apply": apply})
    w(f"assets/atomcraft/blockstates/{name}.json", {"multipart": parts})

MATERIAL_BLOCKS = []
for mid, en_name, pt_name, color, formula, rarity, tier in MATERIALS:
    ingot, plate, blk = f"{mid}_ingot", f"{mid}_plate", f"{mid}_block"
    slab, stairs, wall = f"{mid}_slab", f"{mid}_stairs", f"{mid}_wall"
    tex = f"atomcraft:block/{blk}"
    entry = {"item": f"atomcraft:{ingot}", "elements": parse(formula)}
    if rarity: entry["rarity"] = rarity
    w(f"data/atomcraft/atomcraft/compositions/{ingot}.json", entry)
    item_model(ingot); item_model(plate)
    simple_block(blk, blk)
    slab_states(slab, tex, f"atomcraft:block/{blk}"); block_item(slab)
    stairs_states(stairs, tex); block_item(stairs)
    wall_states(wall, tex); block_item(wall, wall + "_inventory")
    for b in (blk, stairs, wall):
        self_drop(b)
    w(f"data/atomcraft/loot_table/blocks/{slab}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0,
        "entries": [{"type": "minecraft:item", "name": f"atomcraft:{slab}", "functions": [
            {"function": "minecraft:set_count", "add": False, "count": 2,
             "conditions": [{"condition": "minecraft:block_state_property", "block": f"atomcraft:{slab}", "properties": {"type": "double"}}]},
            {"function": "minecraft:explosion_decay"}]}]}]})
    MATERIAL_BLOCKS += [blk, slab, stairs, wall]
    shaped(blk, ["###", "###", "###"], {"#": f"atomcraft:{ingot}"}, f"atomcraft:{blk}")
    shapeless(f"{ingot}_from_{blk}", [f"atomcraft:{blk}"], f"atomcraft:{ingot}", 9)
    shaped(plate, ["#", "#"], {"#": f"atomcraft:{ingot}"}, f"atomcraft:{plate}", 2)
    shaped(slab, ["###"], {"#": f"atomcraft:{blk}"}, f"atomcraft:{slab}", 6)
    shaped(stairs, ["#  ", "## ", "###"], {"#": f"atomcraft:{blk}"}, f"atomcraft:{stairs}", 4)
    shaped(wall, ["###", "###"], {"#": f"atomcraft:{blk}"}, f"atomcraft:{wall}", 6)
    stonecut(f"{slab}_from_stonecutting", f"atomcraft:{blk}", f"atomcraft:{slab}", 2)
    stonecut(f"{stairs}_from_stonecutting", f"atomcraft:{blk}", f"atomcraft:{stairs}")
    stonecut(f"{wall}_from_stonecutting", f"atomcraft:{blk}", f"atomcraft:{wall}")
    for key, e, p in ((ingot, f"{en_name} Ingot", f"Lingote de {pt_name}"), (plate, f"{en_name} Plate", f"Chapa de {pt_name}")):
        MAT_LANG[f"item.atomcraft.{key}"] = (e, p)
    for key, e, p in ((blk, f"Block of {en_name}", f"Bloco de {pt_name}"), (slab, f"{en_name} Slab", f"Laje de {pt_name}"),
                      (stairs, f"{en_name} Stairs", f"Escada de {pt_name}"), (wall, f"{en_name} Wall", f"Muro de {pt_name}")):
        MAT_LANG[f"block.atomcraft.{key}"] = (e, p)

ORE_BLOCKS = []
TOOL_TAGS = {"stone": [], "iron": [], "diamond": []}
for oid, en_name, pt_name, color, formula, rarity, tool, smelts, (size, count, ymin, ymax) in ORE_TYPES:
    raw, ore, deep = f"raw_{oid}", f"{oid}_ore", f"deepslate_{oid}_ore"
    for key, content in ((raw, formula), (ore, add(formula, SIO2)), (deep, add(formula, "Si1 O2 Mg1"))):
        entry = {"item": f"atomcraft:{key}", "elements": parse(content)}
        if rarity: entry["rarity"] = rarity
        w(f"data/atomcraft/atomcraft/compositions/{key}.json", entry)
    item_model(raw)
    simple_block(ore, ore); simple_block(deep, deep)
    ORE_BLOCKS += [ore, deep]
    TOOL_TAGS[tool] += [ore, deep]
    for b in (ore, deep):
        w(f"data/atomcraft/loot_table/blocks/{b}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{
            "type": "minecraft:alternatives", "children": [
                {"type": "minecraft:item", "name": f"atomcraft:{b}", "conditions": [{"condition": "minecraft:match_tool", "predicate": {
                    "predicates": {"minecraft:enchantments": [{"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}]},
                {"type": "minecraft:item", "name": f"atomcraft:{raw}", "functions": [
                    {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
                    {"function": "minecraft:explosion_decay"}]}]}]}]})
    if smelts:
        ingot = f"atomcraft:{smelts}_ingot"
        for src in (raw, ore, deep):
            cook(f"{smelts}_ingot_from_smelting_{src}", "smelting", f"atomcraft:{src}", ingot, 0.7, 200)
            cook(f"{smelts}_ingot_from_blasting_{src}", "blasting", f"atomcraft:{src}", ingot, 0.7, 100)
    # Geração no mundo (veios de pedra e deepslate em todo o Overworld)
    w(f"data/atomcraft/worldgen/configured_feature/ore_{oid}.json", {"type": "minecraft:ore", "config": {
        "size": size, "discard_chance_on_air_exposure": 0.0, "targets": [
            {"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables"}, "state": {"Name": f"atomcraft:{ore}"}},
            {"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:deepslate_ore_replaceables"}, "state": {"Name": f"atomcraft:{deep}"}}]}})
    w(f"data/atomcraft/worldgen/placed_feature/ore_{oid}.json", {"feature": f"atomcraft:ore_{oid}", "placement": [
        {"type": "minecraft:count", "count": count}, {"type": "minecraft:in_square"},
        {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform",
            "min_inclusive": {"absolute": ymin}, "max_inclusive": {"absolute": ymax}}},
        {"type": "minecraft:biome"}]})
    w(f"data/atomcraft/neoforge/biome_modifier/ore_{oid}.json", {"type": "neoforge:add_features", "biomes": "#minecraft:is_overworld",
        "features": f"atomcraft:ore_{oid}", "step": "underground_ores"})
    MAT_LANG[f"item.atomcraft.{raw}"] = (f"Raw {en_name}", f"{pt_name} Bruto" if pt_name not in ("Prata",) else f"{pt_name} Bruta")
    MAT_LANG[f"block.atomcraft.{ore}"] = (f"{en_name} Ore", f"Minério de {pt_name}")
    MAT_LANG[f"block.atomcraft.{deep}"] = (f"Deepslate {en_name} Ore", f"Minério de {pt_name} de Ardósia Profunda")
    # Tags comuns
    w(f"data/c/tags/block/ores/{oid}.json", {"values": [f"atomcraft:{ore}", f"atomcraft:{deep}"]})
    w(f"data/c/tags/item/ores/{oid}.json", {"values": [f"atomcraft:{ore}", f"atomcraft:{deep}"]})
    w(f"data/c/tags/item/raw_materials/{oid}.json", {"values": [f"atomcraft:{raw}"]})
w("data/c/tags/block/ores.json", {"values": [f"#c:ores/{o[0]}" for o in ORE_TYPES]})
w("data/c/tags/item/ores.json", {"values": [f"#c:ores/{o[0]}" for o in ORE_TYPES]})
w("data/c/tags/item/raw_materials.json", {"values": [f"#c:raw_materials/{o[0]}" for o in ORE_TYPES]})
for tag_dir in ("block", "item"):
    w(f"data/c/tags/{tag_dir}/ores_in_ground/stone.json", {"values": [f"atomcraft:{o[0]}_ore" for o in ORE_TYPES]})
    w(f"data/c/tags/{tag_dir}/ores_in_ground/deepslate.json", {"values": [f"atomcraft:deepslate_{o[0]}_ore" for o in ORE_TYPES]})
for mid, *_ in MATERIALS:
    w(f"data/c/tags/item/ingots/{mid}.json", {"values": [f"atomcraft:{mid}_ingot"]})
    w(f"data/c/tags/item/plates/{mid}.json", {"values": [f"atomcraft:{mid}_plate"]})
    w(f"data/c/tags/block/storage_blocks/{mid}.json", {"values": [f"atomcraft:{mid}_block"]})
    w(f"data/c/tags/item/storage_blocks/{mid}.json", {"values": [f"atomcraft:{mid}_block"]})
w("data/c/tags/item/ingots.json", {"values": [f"#c:ingots/{m[0]}" for m in MATERIALS]})
w("data/c/tags/item/plates.json", {"values": [f"#c:plates/{m[0]}" for m in MATERIALS]})
w("data/c/tags/block/storage_blocks.json", {"values": [f"#c:storage_blocks/{m[0]}" for m in MATERIALS]})
w("data/c/tags/item/storage_blocks.json", {"values": [f"#c:storage_blocks/{m[0]}" for m in MATERIALS]})
w("data/c/tags/item/wires/copper.json", {"values": ["atomcraft:copper_wire"]})
for tag_dir in ("block", "item"):
    w(f"data/minecraft/tags/{tag_dir}/slabs.json", {"values": [f"atomcraft:{m[0]}_slab" for m in MATERIALS]})
    w(f"data/minecraft/tags/{tag_dir}/stairs.json", {"values": [f"atomcraft:{m[0]}_stairs" for m in MATERIALS]})
    w(f"data/minecraft/tags/{tag_dir}/walls.json", {"values": [f"atomcraft:{m[0]}_wall" for m in MATERIALS]})

for cid, e, p in COMPONENTS:
    item_model(cid)
    MAT_LANG[f"item.atomcraft.{cid}"] = (e, p)
w("data/atomcraft/atomcraft/compositions/silicon_wafer.json", {"item": "atomcraft:silicon_wafer", "elements": parse("Si9")})
shaped("copper_wire", ["CCC"], {"C": "minecraft:copper_ingot"}, "atomcraft:copper_wire", 6)
shaped("circuit_board", ["WRW", "GSG", "WRW"], {"W": "atomcraft:copper_wire", "R": "minecraft:redstone", "G": "minecraft:gold_nugget",
                                                "S": "atomcraft:silicon_wafer"}, "atomcraft:circuit_board", 2)

# Receitas da Forja de Ligas (data/atomcraft/atomcraft/alloys). Os elementos = composição do resultado.
FORGE_TIER = {1: (4000, 100), 2: (12000, 160), 3: (40000, 240), 4: (120000, 400)}
order = 0
for mid, en_name, pt_name, color, formula, rarity, tier in sorted(MATERIALS, key=lambda m: (m[0] not in ALLOY_IDS, m[6])):
    order += 10
    energy, ticks = FORGE_TIER[tier] if mid in ALLOY_IDS else (2000, 60)
    w(f"data/atomcraft/atomcraft/alloys/{mid}.json", {"result": f"atomcraft:{mid}_ingot", "count": 1, "elements": parse(formula),
                                                      "energy": energy, "ticks": ticks, "order": order})
w("data/atomcraft/atomcraft/alloys/silicon_wafer.json", {"result": "atomcraft:silicon_wafer", "count": 1, "elements": parse("Si9"),
                                                         "energy": 6000, "ticks": 120, "order": order + 10})

w("data/minecraft/tags/block/mineable/pickaxe.json", {"values": [f"atomcraft:{b}" for b in MACHINES + ["atomic_cable"] + REACTOR_PARTS + MATERIAL_BLOCKS + ORE_BLOCKS]})
w("data/minecraft/tags/block/needs_iron_tool.json", {"values": [f"atomcraft:{b}" for b in MACHINES + REACTOR_PARTS + TOOL_TAGS["iron"]]
                                                     + [f"atomcraft:{m[0]}_{s}" for m in MATERIALS if m[6] >= 2 for s in ("block", "slab", "stairs", "wall")]})
w("data/minecraft/tags/block/needs_stone_tool.json", {"values": [f"atomcraft:{b}" for b in TOOL_TAGS["stone"]]
                                                      + [f"atomcraft:{m[0]}_{s}" for m in MATERIALS if m[6] < 2 for s in ("block", "slab", "stairs", "wall")]})
w("data/minecraft/tags/block/needs_diamond_tool.json", {"values": [f"atomcraft:{b}" for b in TOOL_TAGS["diamond"]]})

# ---------- Traduções ----------
MODULE_NAMES = {"containment": ("Magnetic Containment", "Confinamento Magnético"), "injector": ("Fuel Injector", "Injetor de Combustível"),
                "recycler": ("Hydrogen Recycler", "Reciclador de Hidrogênio"), "ignition": ("Plasma Ignition", "Ignição a Plasma")}
ELEMENTS = [("hydrogen", "Hydrogen", "Hidrogênio"), ("helium", "Helium", "Hélio"),
    ("lithium", "Lithium", "Lítio"), ("titanium", "Titanium", "Titânio"), ("vanadium", "Vanadium", "Vanádio"),
    ("chromium", "Chromium", "Cromo"), ("cobalt", "Cobalt", "Cobalto"), ("tungsten", "Tungsten", "Tungstênio"),
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
    "block.atomcraft.alloy_forge": ("Alloy Forge", "Forja de Ligas"),
    "block.atomcraft.advanced_replicator": ("Advanced Replicator", "Replicador Avançado"),
    "block.atomcraft.quantum_replicator": ("Quantum Replicator", "Replicador Quântico"),
    "block.atomcraft.quantum_casing": ("Quantum Casing", "Carcaça Quântica"),
    "block.atomcraft.quantum_glass": ("Quantum Glass", "Vidro Quântico"),
    "block.atomcraft.quantum_core": ("Quantum Core", "Núcleo Quântico"),
    "replicator_tier.atomcraft.basic": ("Atomic", "Atômico"),
    "replicator_tier.atomcraft.advanced": ("Advanced", "Avançado"),
    "replicator_tier.atomcraft.quantum": ("Quantum", "Quântico"),
    "gui.atomcraft.complexity": ("Complexity", "Complexidade"),
    "gui.atomcraft.complexity_max": ("up to %s", "até %s"),
    "gui.atomcraft.complexity_elements": ("Elements:", "Elementos:"),
    "gui.atomcraft.complexity_unlimited": ("no limit", "sem limite"),
    "gui.atomcraft.exotics_yes": ("Exotics: yes", "Exóticos: sim"),
    "gui.atomcraft.exotics_no": ("Exotics: no", "Exóticos: não"),
    "gui.atomcraft.complexity_researched": ("Can make:", "Pode fazer:"),
    "gui.atomcraft.quantum_incomplete": ("Structure incomplete", "Estrutura incompleta"),
    "gui.atomcraft.quantum_hint": ("3×3×3 behind it", "3×3×3 atrás dele"),
    "message.atomcraft.too_complex": ("Too complex: %s elements (max %s here)", "Complexo demais: %s elementos (máx. %s aqui)"),
    "message.atomcraft.needs_exotics": ("Has exotic elements: needs a better replicator", "Tem elementos exóticos: precisa de um replicador melhor"),
    "status.atomcraft.too_complex": ("Too complex", "Complexo demais"),
    "atomcraft.configuration.advancedReplicator": ("Advanced Replicator", "Replicador Avançado"),
    "atomcraft.configuration.quantumReplicator": ("Quantum Replicator", "Replicador Quântico"),
    "atomcraft.configuration.maxElements": ("Max distinct elements", "Máximo de elementos diferentes"),
    "atomcraft.configuration.allowExotics": ("Allow exotic elements", "Permitir elementos exóticos"),
    "itemGroup.atomcraft.materials": ("AtomCraft: Materials", "AtomCraft: Materiais"),
    "gui.atomcraft.no_alloy_recipes": ("No recipes loaded", "Nenhuma receita carregada"),
    "gui.atomcraft.select_alloy": ("Pick what to forge", "Escolha o que forjar"),
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
    "status.atomcraft.incomplete": ("Incomplete structure", "Estrutura incompleta"),
    "gui.atomcraft.partition": ("Partition", "Partição"),
    "gui.atomcraft.partition_limit": ("Limit", "Limite"),
    "gui.atomcraft.partition_limit_of": ("Limit of %s:", "Limite de %s:"),
    "gui.atomcraft.partition_none": ("No partition: accepts any element", "Sem partição: aceita qualquer elemento"),
    "gui.atomcraft.partition_count": ("%s elements allowed", "%s elementos permitidos"),
    "gui.atomcraft.partition_clear": ("Clear", "Limpar"),
    "gui.atomcraft.partition_click_add": ("Click to allow it in this disk", "Clique para permitir neste disco"),
    "gui.atomcraft.partition_click_edit": ("Click to edit its limit", "Clique para editar o limite"),
    "gui.atomcraft.partition_click_remove": ("Click again to remove it from the partition", "Clique de novo para tirar da partição"),
    "gui.atomcraft.partition_limit_hint": ("Maximum atoms of this element (empty = no limit)", "Máximo de átomos deste elemento (vazio = sem limite)"),
    "tooltip.atomcraft.partition": ("Partition: %s", "Partição: %s"),
    "tooltip.atomcraft.partition_hint": ("Right-click to edit the partition", "Botão direito para editar a partição"),
    "block.atomcraft.reactor_casing": ("Reactor Casing", "Carcaça do Reator"),
    "block.atomcraft.reactor_glass": ("Containment Glass", "Vidro de Contenção"),
    "block.atomcraft.magnetic_coil": ("Magnetic Coil", "Bobina Magnética"),
    "block.atomcraft.central_solenoid": ("Central Solenoid", "Solenoide Central"),
    "block.atomcraft.reactor_port": ("Reactor Port", "Porta do Reator"),
    "gui.atomcraft.part_air": ("Empty space (plasma channel)", "Espaço vazio (canal de plasma)"),
    "gui.atomcraft.structure_missing": ("Missing %s blocks", "Faltam %s blocos"),
    "gui.atomcraft.structure_more": ("+%s other types", "+%s outros tipos"),
    "gui.atomcraft.show_structure": ("Show structure", "Mostrar estrutura"),
    "gui.atomcraft.hide_structure": ("Hide preview", "Ocultar prévia"),
    "gui.atomcraft.modules": ("Modules", "Módulos"),
    "gui.atomcraft.modules_hint": ("Only the highest tier of each type counts", "Só o maior nível de cada tipo vale"),
    "tooltip.atomcraft.module_hint": ("Install in the Fusion Reactor (Modules tab)", "Instale no Reator de Fusão (aba Módulos)"),
    "module.atomcraft.containment.short": ("Containment", "Confinamento"),
    "module.atomcraft.injector.short": ("Injector", "Injetor"),
    "module.atomcraft.recycler.short": ("Recycler", "Reciclador"),
    "module.atomcraft.ignition.short": ("Ignition", "Ignição"),
    "module.atomcraft.containment.desc": ("Energy per fusion: %s", "Energia por fusão: %s"),
    "module.atomcraft.injector.desc": ("Fusion cycle: %s (base 10t)", "Ciclo de fusão: %s (padrão 10t)"),
    "module.atomcraft.recycler.desc": ("Returns 1 H per fusion: %s chance", "Devolve 1 H por fusão: %s de chance"),
    "module.atomcraft.ignition.desc": ("Plasma warm-up: %s (base 5min)", "Aquecimento do plasma: %s (padrão 5min)"),
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
common.update(MAT_LANG)
en = {k: v[0] for k, v in common.items()}
pt = {k: v[1] for k, v in common.items()}
for key, e, p in ELEMENTS:
    en[f"element.atomcraft.{key}"] = e
    pt[f"element.atomcraft.{key}"] = p
for mtype, (e, p) in MODULE_NAMES.items():
    for tier, roman in ((1, "I"), (2, "II"), (3, "III")):
        en[f"item.atomcraft.{mtype}_module_{tier}"] = f"{e} Module {roman}"
        pt[f"item.atomcraft.{mtype}_module_{tier}"] = f"Módulo de {p} {roman}"
        en[f"item.atomcraft.{mtype}_module_{tier}_schematic"] = f"Schematic: {e} {roman}"
        pt[f"item.atomcraft.{mtype}_module_{tier}_schematic"] = f"Schematic: {p} {roman}"
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
    "terminal": (39, 201, 181, 255), "synthesizer": (120, 230, 140, 255), "combustion_generator": (255, 120, 30, 255), "carbon_generator": (226, 50, 50, 255), "fusion_reactor": (255, 90, 190, 255), "alloy_forge": (255, 140, 40, 255), "advanced_replicator": (190, 110, 255, 255)}
img, d = frame(); d.line([4, 5, 11, 5], fill=(60, 63, 70, 255)); d.line([4, 10, 11, 10], fill=(60, 63, 70, 255)); save(img, "block/machine_side.png")
for name, c in COLORS.items():
    if name not in GENERATORS:  # os geradores usam generator_top.png / generator_side.png
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
    elif name == "alloy_forge":
        # Cadinho com metal derretido
        d.polygon([(4, 5), (11, 5), (10, 12), (5, 12)], fill=(60, 62, 70, 255), outline=(120, 124, 136, 255))
        d.rectangle([5, 6, 10, 8], fill=c); d.line([6, 7, 9, 7], fill=(255, 220, 120, 255))
    elif name == "analyzer":
        d.ellipse([4, 4, 11, 11], outline=c); d.line([6, 7, 9, 7], fill=c); d.line([7, 6, 7, 9], fill=c)
    else:
        atom(d, c)
    save(img, f"block/{name}_front.png")
img = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(img)
d.rectangle([0, 0, 15, 15], fill=(70, 74, 84, 255)); d.rectangle([5, 5, 10, 10], fill=(80, 220, 255, 255))
d.rectangle([6, 6, 9, 9], fill=(40, 140, 180, 255)); save(img, "block/atomic_cable.png")
DISK_TEX = os.path.join(R, "assets/atomcraft/textures/item/disk.png")
for name, c in DISKS.items():
    if DISK_MODEL and os.path.exists(DISK_TEX):
        # Variante por nível: troca a cor da etiqueta verde da textura base do disco.
        base = Image.open(DISK_TEX).convert("RGBA")
        px = base.load()
        for y in range(base.height):
            for x in range(base.width):
                r, g, b, a = px[x, y]
                if a and (r, g, b) == (90, 200, 120):
                    px[x, y] = c + (a,)
        save(base, f"item/{name.replace('element_', '')}.png")
        continue
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
# Peças do Reator de Fusão
def casing_tile():
    img = Image.new("RGBA", (16, 16), (64, 74, 88, 255)); d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 15], outline=(30, 34, 42, 255)); d.rectangle([1, 1, 14, 14], outline=(104, 118, 136, 255))
    d.line([2, 8, 13, 8], fill=(50, 58, 70, 255)); d.line([8, 2, 8, 13], fill=(50, 58, 70, 255))
    for (px, py) in ((3, 3), (12, 3), (3, 12), (12, 12)):
        d.point((px, py), fill=(150, 165, 185, 255))
    return img, d
img, d = casing_tile(); save(img, "block/reactor_casing.png")
casing_img = img.copy()
img = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(img)
d.rectangle([0, 0, 15, 15], outline=(30, 34, 42, 255)); d.rectangle([1, 1, 14, 14], outline=(104, 118, 136, 255))
d.line([3, 12, 6, 9], fill=(170, 235, 255, 200)); d.line([10, 5, 12, 3], fill=(170, 235, 255, 200))
save(img, "block/reactor_glass.png"); glass_img = img.copy()
img, d = casing_tile()
for y in range(3, 13, 2):
    d.line([2, y, 13, y], fill=(200, 110, 50, 255)); d.line([2, y + 1, 13, y + 1], fill=(150, 75, 35, 255))
save(img, "block/magnetic_coil.png"); coil_img = img.copy()
img, d = casing_tile(); d.rectangle([4, 1, 11, 14], fill=(60, 30, 90, 255))
for y in (3, 6, 9, 12):
    d.line([4, y, 11, y], fill=(190, 120, 255, 255))
save(img, "block/central_solenoid.png"); solenoid_img = img.copy()
img, d = casing_tile(); d.rectangle([4, 4, 11, 11], fill=(20, 24, 30, 255), outline=(92, 224, 122, 255))
d.rectangle([6, 6, 9, 9], fill=(80, 220, 255, 255)); save(img, "block/reactor_port.png")

# Textura do modelo grande (64×64): ladrilhos de 16×16 + máscara de brilho do plasma
big = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
big.paste(casing_img, (0, 0)); big.paste(coil_img, (16, 0)); big.paste(solenoid_img, (32, 0)); big.paste(glass_img, (16, 16))
ctrl, d = casing_tile(); d.ellipse([2, 2, 13, 13], fill=(24, 20, 34, 255), outline=(255, 90, 190, 255))
d.ellipse([5, 5, 10, 10], fill=(255, 200, 240, 255)); big.paste(ctrl, (48, 0))
pl = Image.new("RGBA", (16, 16), (255, 120, 220, 255)); d = ImageDraw.Draw(pl)
d.rectangle([3, 3, 12, 12], fill=(255, 190, 245, 255)); d.rectangle([6, 6, 9, 9], fill=(255, 255, 255, 255))
big.paste(pl, (32, 16))
if not preserved("assets/atomcraft/textures/block/fusion_reactor_formed.png"):
    p = os.path.join(R, "assets/atomcraft/textures/block/fusion_reactor_formed.png"); os.makedirs(os.path.dirname(p), exist_ok=True); big.save(p)
glow = Image.new("RGBA", (64, 64), (0, 0, 0, 0)); glow.paste(pl, (32, 16))
gd = ImageDraw.Draw(glow); gd.ellipse([53, 5, 58, 10], fill=(255, 200, 240, 255))
if not preserved("assets/atomcraft/textures/block/fusion_reactor_formed_glowmask.png"):
    glow.save(os.path.join(R, "assets/atomcraft/textures/block/fusion_reactor_formed_glowmask.png"))

# Módulos: placa escura com núcleo colorido e pontos do nível; schematics com a mesma cor
TIER_EDGE = {1: (170, 178, 190, 255), 2: (230, 190, 70, 255), 3: (190, 120, 255, 255)}
for mtype, c in MODULE_TYPES.items():
    for tier in (1, 2, 3):
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(img)
        d.polygon([(4, 1), (11, 1), (14, 4), (14, 11), (11, 14), (4, 14), (1, 11), (1, 4)], fill=(36, 40, 50, 255), outline=TIER_EDGE[tier])
        d.ellipse([4, 3, 11, 10], fill=c + (255,), outline=(20, 22, 28, 255)); d.point((6, 5), fill=(255, 255, 255, 255))
        for i in range(tier):
            d.rectangle([5 + i * 2, 12, 5 + i * 2, 12], fill=TIER_EDGE[tier])
        save(img, f"item/{mtype}_module_{tier}.png")
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(img)
        d.rectangle([2, 2, 13, 13], fill=(34, 78, 150, 255), outline=(18, 40, 86, 255))
        for y in (5, 8):
            d.line([4, y, 9, y], fill=(150, 190, 240, 255))
        d.ellipse([9, 3, 12, 6], fill=c + (255,))
        for i in range(tier):
            d.rectangle([4 + i * 3, 11, 5 + i * 3, 12], fill=TIER_EDGE[tier])
        save(img, f"item/{mtype}_module_{tier}_schematic.png")

# Materiais: lingotes, chapas, minério bruto, blocos e minérios (provisórios; troque à vontade, o gerador não sobrescreve)
import random
def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (255,)
def ingot_tex(c):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(img)
    d.polygon([(2, 10), (5, 6), (14, 6), (11, 10)], fill=shade(c, 1.15))
    d.polygon([(2, 10), (11, 10), (11, 13), (2, 13)], fill=shade(c, 0.85))
    d.polygon([(11, 10), (14, 6), (14, 9), (11, 13)], fill=shade(c, 0.65))
    d.line([(5, 7), (12, 7)], fill=shade(c, 1.4))
    return img
def plate_tex(c):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(img)
    d.rectangle([2, 3, 13, 12], fill=shade(c, 1.0), outline=shade(c, 0.6))
    d.line([(3, 4), (12, 4)], fill=shade(c, 1.35)); d.line([(3, 4), (3, 11)], fill=shade(c, 1.2))
    for x, y in ((4, 10), (11, 10)):
        d.point((x, y), fill=shade(c, 0.7))
    return img
def raw_tex(c, seed):
    rnd = random.Random(seed); img = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(img)
    d.ellipse([3, 5, 12, 13], fill=shade(c, 0.8)); d.ellipse([5, 3, 13, 10], fill=shade(c, 1.0))
    for _ in range(10):
        x, y = rnd.randint(5, 12), rnd.randint(4, 11)
        d.point((x, y), fill=shade(c, rnd.choice((0.65, 1.25, 1.4))))
    return img
def block_tex(c):
    img = Image.new("RGBA", (16, 16), shade(c, 0.95)); d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 15], outline=shade(c, 0.6)); d.line([(1, 1), (14, 1)], fill=shade(c, 1.3)); d.line([(1, 1), (1, 14)], fill=shade(c, 1.15))
    d.line([(1, 14), (14, 14)], fill=shade(c, 0.75)); d.line([(14, 1), (14, 14)], fill=shade(c, 0.75))
    d.rectangle([4, 4, 11, 11], outline=shade(c, 0.82))
    for x, y in ((3, 3), (12, 3), (3, 12), (12, 12)):
        d.point((x, y), fill=shade(c, 0.6))
    return img
def ore_tex(c, seed, deep):
    rnd = random.Random(seed); base = (70, 70, 78) if deep else (125, 125, 125)
    img = Image.new("RGBA", (16, 16), base + (255,)); d = ImageDraw.Draw(img)
    for _ in range(70):
        f = rnd.choice((0.8, 0.9, 1.1, 1.2)); d.point((rnd.randint(0, 15), rnd.randint(0, 15)), fill=shade(base, f))
    for _ in range(5):
        x, y = rnd.randint(1, 13), rnd.randint(1, 13)
        d.rectangle([x, y, x + 1, y + 1], fill=c + (255,)); d.point((x + 1, y + 1), fill=shade(c, 0.7)); d.point((x, y), fill=shade(c, 1.3))
    return img
for mid, en_name, pt_name, color, formula, rarity, tier in MATERIALS:
    save(ingot_tex(color), f"item/{mid}_ingot.png")
    save(plate_tex(color), f"item/{mid}_plate.png")
    save(block_tex(color), f"block/{mid}_block.png")
for i, (oid, en_name, pt_name, color, *_rest) in enumerate(ORE_TYPES):
    save(raw_tex(color, i), f"item/raw_{oid}.png")
    save(ore_tex(color, i, False), f"block/{oid}_ore.png")
    save(ore_tex(color, i + 50, True), f"block/deepslate_{oid}_ore.png")
img = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(img)
for k in range(5):
    d.ellipse([3 + k, 4, 7 + k, 12], outline=(201, 123, 58, 255))
d.line([(1, 8), (3, 8)], fill=(201, 123, 58, 255)); d.line([(12, 8), (14, 8)], fill=(201, 123, 58, 255)); save(img, "item/copper_wire.png")
img = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(img)
d.ellipse([2, 2, 13, 13], fill=(70, 78, 110, 255), outline=(40, 44, 64, 255))
for x in range(4, 12, 2):
    for y in range(4, 12, 2):
        d.point((x, y), fill=(150, 170, 230, 255))
save(img, "item/silicon_wafer.png")
img = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(img)
d.rectangle([1, 3, 14, 12], fill=(30, 110, 60, 255), outline=(14, 56, 30, 255))
d.rectangle([6, 6, 9, 9], fill=(40, 40, 46, 255)); d.line([(2, 5), (5, 5), (5, 7)], fill=(220, 180, 70, 255))
d.line([(10, 8), (13, 8)], fill=(220, 180, 70, 255)); d.line([(7, 10), (7, 11), (12, 11)], fill=(220, 180, 70, 255))
save(img, "item/circuit_board.png")

# Replicador Quântico: carcaça, vidro, núcleo e a frente do controlador (provisórios)
QV = (190, 110, 255, 255)
img, d = frame(); d.rectangle([3, 3, 12, 12], outline=(60, 40, 90, 255)); d.rectangle([6, 6, 9, 9], fill=QV)
for (px_, py_) in ((2, 2), (13, 2), (2, 13), (13, 13)):
    d.point((px_, py_), fill=QV)
save(img, "block/quantum_casing.png")
img = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(img)
d.rectangle([0, 0, 15, 15], outline=(78, 82, 92, 255)); d.rectangle([1, 1, 14, 14], outline=(150, 110, 220, 255))
d.line([(3, 12), (7, 8)], fill=(210, 190, 255, 160)); d.line([(8, 6), (10, 4)], fill=(210, 190, 255, 160))
save(img, "block/quantum_glass.png")
img = Image.new("RGBA", (16, 16), (30, 18, 48, 255)); d = ImageDraw.Draw(img)
d.rectangle([0, 0, 15, 15], outline=(80, 60, 120, 255)); d.ellipse([3, 3, 12, 12], fill=(120, 60, 200, 255))
d.ellipse([5, 5, 10, 10], fill=(220, 160, 255, 255)); d.point((7, 7), fill=(255, 255, 255, 255))
save(img, "block/quantum_core.png")
for suffix, glow in (("", (70, 50, 100, 255)), ("_on", (230, 170, 255, 255))):
    img, d = frame(); d.rectangle([2, 2, 13, 13], fill=(24, 18, 34, 255))
    d.ellipse([3, 3, 12, 12], outline=glow); d.ellipse([6, 6, 9, 9], fill=glow)
    save(img, f"block/quantum_replicator_front{suffix}.png")

print("ok", len(BASE), "itens base,", len(TAGS), "tags vanilla,", len(COMPAT), "tags de compatibilidade,",
      len(kept), "arquivos de aparência preservados")
